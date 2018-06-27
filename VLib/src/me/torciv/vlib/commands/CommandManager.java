package me.torciv.vlib.commands;

import me.torciv.vlib.commands.arguments.AbstractArg;
import me.torciv.vlib.commands.arguments.types.ArgBoolean;
import me.torciv.vlib.commands.arguments.types.ArgString;
import me.torciv.vlib.commands.extra.CommandFinished;
import me.torciv.vlib.commands.extra.CommandOnly;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Register a command with the command system!
 * <br><br>
 * Handles arguments, argument type handling, predictions, tab completion, sub-sub-sub-commands, error handling, help pages, and more!
 * <br><br>
 * CommandManager manager = new CommandManager(plugin, "Help Tag", "command", "permission node");
 *
 * @author VictorML11
 */
public class CommandManager implements TabCompleter, CommandExecutor {

    private boolean GLOBAL_DEBUG = false;

    @Cmd(cmd = "debug",
            args = "[on/off]",
            argTypes = {ArgBoolean.class},
            help = "Activa/Desactiva el modo debug.",
            longhelp = "Activa/Desactiva el modo debug. Muestra información al usar un comando.",
            only = CommandOnly.OP,
            permission = "debug")
    public CommandFinished cmdToggleDebugMode(CommandSender sender, Object[] args) {
        GLOBAL_DEBUG = (args.length != 0 ? (Boolean) args[0] : !GLOBAL_DEBUG);
        sender.sendMessage(ChatColor.YELLOW + "Modo Debug: " + (GLOBAL_DEBUG ? ChatColor.GREEN + "ACTIVADO" : ChatColor.RED + "DESACTIVADO"));
        return CommandFinished.DONE;
    }

    private static HashMap<Class<? extends AbstractArg<?>>, AbstractArg<?>> argInstances = new HashMap<Class<? extends AbstractArg<?>>, AbstractArg<?>>();

    private ArrayList<Cmd> commands = new ArrayList<>();
    private ArrayList<Method> commandMethods = new ArrayList<Method>();

    String tag;
    String command;
    String permissionScheme;

    public CommandManager(JavaPlugin plugin, String tag, String permissionScheme, String command, String... aliases) {
        this.tag = tag;
        this.command = command;
        this.permissionScheme = permissionScheme;

        // Used to inject the command without using plugin.yml
        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

            Constructor<PluginCommand> c = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            c.setAccessible(true);

            PluginCommand pluginCommand = c.newInstance(command, plugin);
            pluginCommand.setTabCompleter(this);
            pluginCommand.setExecutor(this);
            if (aliases.length > 0)
                pluginCommand.setAliases(Arrays.asList(aliases));
            commandMap.register(command, pluginCommand);

            loadCommandClass(this.getClass());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses classes for @Cmd annotations.
     */
    public CommandManager loadCommandClass(Class<?> commandClass) {
        try {
            Arrays.stream(commandClass.getMethods()).filter(method -> method.isAnnotationPresent(Cmd.class)).forEach(method -> {
                Cmd cmd = method.getAnnotation(Cmd.class);
                commands.add(cmd);
                commandMethods.add(method);
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        return this;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> predictions;
        String token = (args.length == 0 ? "" : args[args.length - 1]);

        // Prevent duplicate entries
        predictions = commands.stream().map(c -> getPredicted(c, token, args.length - 1)).filter(Objects::nonNull).flatMap(Collection::stream).distinct().collect(Collectors.toList());

        return predictions;
    }

    /**
     * Get a prediction of the next command argument.
     */
    private List<String> getPredicted(Cmd c, String token, int i) {
        String[] cmdArg = c.cmd().split(" ");
        // If no token, return all possible commands.
        if (token.equals("")){
            return Arrays.asList(new String[]{cmdArg[0]});
        }
        // If the amount of args is more than available, or it doesn't start with the token.
        if (i >= cmdArg.length) {
            int argNum = i - cmdArg.length;
            if (argNum >= c.argTypes().length) {
                return null;
            } else {
                if (!argInstances.containsKey(c.argTypes()[argNum]))
                    try {
                        argInstances.put(c.argTypes()[argNum], c.argTypes()[argNum].newInstance());
                    } catch (Exception e) {
                    }
                AbstractArg<?> absArg = argInstances.get(c.argTypes()[argNum]);
                return absArg.getPredictions();
            }
            // If it doesn't start with the token.
        } else if (!cmdArg[i].startsWith(token))
            return null;
        // It must be a match!
        return Arrays.asList(new String[]{cmdArg[i]});
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        StopWatch sw = null;
        if (GLOBAL_DEBUG) {
            sw = new StopWatch();
            sw.start();
        }

        CommandFinished finishedType = runCommand(sender, args);
        if (finishedType.shouldPrint()) {
            sender.sendMessage(ChatColor.RED + finishedType.getErrorString());

            // Do our best to predict which command was going to be used.
            if (finishedType == CommandFinished.COMMAND) {
                // TreeMaps automatically sort by numbers.
                TreeMap<Double, Cmd> possible = new TreeMap<Double, Cmd>();
                for (Cmd c : commands) {
                    // Reduce arg array to the shortest one.
                    String[] fixedArgs = new String[c.cmd().split(" ").length];
                    for (int i = 0; i < (args.length > fixedArgs.length ? fixedArgs.length : args.length); i++) {
                        fixedArgs[i] = args[i];
                    }

                    // Combine the arguments.
                    String cmdArgs = StringUtils.join(fixedArgs, " ");

                    // Use Levenshtein Distance to get how similar the two strings are to each other. Calculate percentage with the value returned.
                    possible.put((1D - (StringUtils.getLevenshteinDistance(cmdArgs, c.cmd()) / (Math.max(cmdArgs.length(), c.cmd().length()) * 1D))) * 100D, c);
                }

                // Are there even any predictions?
                if (possible.size() > 0) {
                    // Get the last entry. (The one with the highest possibility)
                    Entry<Double, Cmd> entry = possible.pollLastEntry();
                    sender.sendMessage("");
                    // Allow players to click the command in chat to add it to their chat input.
                    if (sender instanceof Player) {
                        ((Player) sender).spigot().sendMessage(new ComponentBuilder("   Querías ejecutar: ").color(net.md_5.bungee.api.ChatColor.GOLD)
                                .append("/" + label + " " + entry.getValue().cmd()).color(net.md_5.bungee.api.ChatColor.GRAY)
                                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(StringUtils.join((entry.getValue().longhelp().equals("") ? entry.getValue().help() : entry.getValue().longhelp()).split("(?<=\\G.........................)"), "\n")).create()))
                                .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + label + " " + entry.getValue().cmd()))
                                .append("? Estamos " + ((int) (entry.getKey() * 10) / 10D) + "% seguros.").reset().color(net.md_5.bungee.api.ChatColor.GOLD).create());
                    } else {
                        sender.sendMessage(ChatColor.GOLD + "   Querías ejecutar: " + ChatColor.GRAY + "/" + label + " " + entry.getValue().cmd() + ChatColor.GOLD + "? Estamos " + ((int) (entry.getKey() * 10) / 10D) + "% seguros.");
                    }
                    sender.sendMessage("");
                }
            }
        }

        if (GLOBAL_DEBUG && sw != null) {
            sw.stop();
            sender.sendMessage(ChatColor.YELLOW + "Ha tomado " + sw.getTime() + " ms para ejectuar el comando.");
        }
        return true;
    }

    /**
     * Find the best command to run and do so.
     */
    public CommandFinished runCommand(CommandSender sender, String[] args) {
        try {
            // Display help if no args, or they use the help command
            if (args.length == 0 || args[0].equalsIgnoreCase("help"))
                return displayHelp(sender, args.length == 0 ? null : args);
            else {
                Cmd bestFit = null;
                int bestFit_i = 0;
                int bestFit_args = -1;

                // Loop through commands until a suitable one is found
                for (int i = 0; i < commands.size(); i++) {
                    Cmd cmd = commands.get(i);

                    // Split the base command and check for a match
                    String[] cmds = cmd.cmd().split(" ");
                    if (args.length >= cmds.length) {
                        boolean valid = true;
                        for (int j = 0; j < cmds.length; j++) {
                            if (!cmds[j].equalsIgnoreCase(args[j])) {
                                valid = false;
                                break;
                            }
                        }

                        if (!valid) {
                            continue;
                        }
                    } else {
                        continue;
                    }

                    // Check if it's better than the best fit.
                    if (cmd.cmd().split(" ").length > bestFit_args) {
                        bestFit = cmd;
                        bestFit_i = i;
                        bestFit_args = cmd.cmd().split(" ").length;
                    } else
                        continue;
                }

                if (bestFit != null) {
                    // Check the "only" argument
                    if (sender instanceof Player) {
                        if (bestFit.only() == CommandOnly.CONSOLE) {
                            return CommandFinished.NOPLAYER;
                        }
                    } else if (bestFit.only() == CommandOnly.PLAYER) {
                        return CommandFinished.NOCONSOLE;
                    }

                    // Check for the "op" argument and permission argument
                    if ((bestFit.only() == CommandOnly.OP && !sender.isOp()) ||
                            (!bestFit.permission().equals("") && !sender.hasPermission(permissionScheme + "." + bestFit.permission())))
                        return CommandFinished.PERMISSION;

                    // Split up the args; arguments in quotes count as a single argument.
                    List<Object> cmdArgList = new ArrayList<Object>();
                    Matcher m = Pattern.compile("(?:([^\"]\\S*)|\"(.+?)\")\\s*").matcher(StringUtils.join(args, " ").replaceFirst(bestFit.cmd(), "").trim());
                    for (int j = 0; m.find(); j++) {
                        // Apply the requested argument type.
                        Class<? extends AbstractArg<?>> requestedType = (j < bestFit.argTypes().length ? bestFit.argTypes()[j] : ArgString.class);
                        // Cache the instance.
                        if (!argInstances.containsKey(requestedType))
                            argInstances.put(requestedType, requestedType.newInstance());
                        AbstractArg<?> absArg = argInstances.get(requestedType);
                        try {
                            Object arg = absArg.parseArg(m.group(1) != null ? m.group(1) : m.group(2));
                            if (arg == null) {// Some argument parsers don't throw an exception. Just an extra precaution.
                                throw new CommandException(absArg.getFailure() + " (" + m.group(1) != null ? m.group(1) : m.group(2) + ")");
                            }
                            cmdArgList.add(arg);
                        } catch (Exception e) {
                            return CommandFinished.CUSTOM.replace(absArg.getFailure() + " (" + (m.group(1) != null ? m.group(1) : m.group(2)) + ")");
                        }
                    }

                    // Check that all the required arguments have been fulfilled.
                    Object[] cmdArgsPassed = cmdArgList.toArray(new Object[cmdArgList.size()]);
                    if (StringUtils.countMatches(bestFit.args(), "<") > cmdArgsPassed.length)
                        return CommandFinished.BADCOMMAND.replace(command + " " + bestFit.cmd() + " " + bestFit.args());

                    // Run the command :D
                    return (CommandFinished) commandMethods.get(bestFit_i).invoke(null, new Object[]{sender, (cmdArgsPassed != null ? cmdArgsPassed : null)});
                }
            }
        } catch (InvocationTargetException e) {
            e.getCause().printStackTrace();
            if (e.getCause() instanceof CommandException)
                return CommandFinished.CUSTOM.replace(e.getCause().getMessage());
            if (GLOBAL_DEBUG)
                Bukkit.broadcastMessage(ChatColor.RED + "Error: " + getTrace(e));
            return CommandFinished.EXCEPTION;
        } catch (Exception e) {
            e.printStackTrace();
            if (GLOBAL_DEBUG)
                Bukkit.broadcastMessage(ChatColor.RED + "Error: " + getTrace(e));
            return CommandFinished.EXCEPTION;
        }

        return CommandFinished.COMMAND.replace(command);
    }

    /**
     * Display the help menu.
     */
    public CommandFinished displayHelp(CommandSender sender, String[] args) {
        ArrayList<String> helpList = new ArrayList<>(); // The help message buffer
        ArrayList<ComponentBuilder> helpList2 = new ArrayList<>();

        boolean specific = false; // If "help <command>"
        int perPage = 5; // How many commands to show per page
        int page = 0; // Which page
        Player psender = null;
        boolean isPlayer = false;

        if (sender instanceof Player) {
            psender = (Player) sender;
            isPlayer = true;
        }


        if (args != null && args.length != 1) {
            try {
                page = Integer.parseInt(args[1]) - 1;
                if (page < 0) { // Negative pages are bad juju.
                    page = 0;
                }
            } catch (Exception e) {
                specific = true;
            } // If this fails, it's probably a string. Check for a specific command.
        }
        System.out.println(specific);

        String cmdLabel = null; // The label of the specific command.

        if (specific && args.length != 1) {
            perPage = 4; // Reduce the amount to show per page.
            cmdLabel = StringUtils.join(args, " ").split(" ", 2)[1]; // Because args = "help <command>" cut out "help".
        }

        for (Cmd cmd : commands) {
            // If looking for specific commands and it isn't the one we're looking for
            if (specific && !cmd.cmd().startsWith(cmdLabel)) {
                continue;
            }

            // Should it even show?
            if (cmd.showInHelp()) {
                // If it can't be used, don't show it! Simple! :D
                boolean canUse = cmd.permission().equals("") || (sender.hasPermission(permissionScheme + "." + cmd.permission()));

                // Is it op-only?
                if (cmd.only() == CommandOnly.OP) {
                    canUse = (cmd.permission().equals("") ? sender.isOp() : canUse);
                }

                if (canUse) {
                    String baseCmd = "/" + command + " " + cmd.cmd();
                    String onlyArgs = (!cmd.args().equals("") ? " " + cmd.args() : "");
                    String baseCmdWithArgs = baseCmd + onlyArgs;
                    String descbase = specific ? (cmd.longhelp().equals("") ? cmd.help() : cmd.longhelp()) : cmd.help();
                    String descguion = " - " + descbase;

                    String[] descriptions = cmd.argDesc();
                    String[] splited = cmd.args().split(" ");

                    String baseCmdHelp = "/" + command + " help " + cmd.cmd();

                    if (isPlayer) {
                        ComponentBuilder pasteCommand = new ComponentBuilder(baseCmdWithArgs + "\n")
                                .color(net.md_5.bungee.api.ChatColor.YELLOW)
                                .append("Click para pegar el comando\n")
                                .color(net.md_5.bungee.api.ChatColor.GRAY)
                                .append("en el chat.", ComponentBuilder.FormatRetention.ALL);

                        ComponentBuilder masInfo = new ComponentBuilder(baseCmdHelp + "\n")
                                .color(net.md_5.bungee.api.ChatColor.YELLOW)
                                .append("Click para mostrar información\n")
                                .color(net.md_5.bungee.api.ChatColor.GRAY)
                                .append("más detallada del comando.", ComponentBuilder.FormatRetention.ALL);


                        ComponentBuilder a = new ComponentBuilder(" " + baseCmd)
                                .color(net.md_5.bungee.api.ChatColor.YELLOW)
                                .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, baseCmdWithArgs))
                                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, pasteCommand.create()));



                        int i = 0;
                        for (String argumento : splited) {
                            ComponentBuilder cdesc = null;
                            if(i < descriptions.length){
                                cdesc = new ComponentBuilder(descriptions[i])
                                .color(net.md_5.bungee.api.ChatColor.GRAY);
                            }else{
                                cdesc = new ComponentBuilder("default").color(net.md_5.bungee.api.ChatColor.GRAY);
                            }
                            a.append(" " + argumento, ComponentBuilder.FormatRetention.FORMATTING).color(net.md_5.bungee.api.ChatColor.WHITE)
                                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, cdesc.create()));
                            i++;
                        }

                        //a.append(onlyArgs, ComponentBuilder.FormatRetention.NONE).color(net.md_5.bungee.api.ChatColor.WHITE);


                        a.append(" \u25b8 ", ComponentBuilder.FormatRetention.NONE)
                                .color(net.md_5.bungee.api.ChatColor.RED)
                                .append(descbase, ComponentBuilder.FormatRetention.NONE)
                                .color(net.md_5.bungee.api.ChatColor.AQUA);

                        if (!specific) {
                            a.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, baseCmdHelp))
                                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, masInfo.create()));
                        }
                        helpList2.add(a);

                    } else {
                        helpList.add(colorize("&e" + baseCmd + "&f" + onlyArgs + "&b" + descguion));
                    }
                }
            }
        }

        // Make sure there's something to show.
        boolean badPage = true;
        int size = isPlayer ? helpList2.size() : helpList.size();
        if (size >= page * perPage) {
            for (int j = 0; j < perPage; j++) {
                if (size > (j + page * perPage)) {
                    if (j == 0) {
                        //"\u25c2 < - \u25b8 >
                        sender.sendMessage(" ");
                        sender.sendMessage(colorize("&7&m-------------&r &6&l" + tag + " Help &f(" + (page + 1) + "/" + (int) Math.ceil(size / (perPage * 1F)) + ") " + "&7&m---------------"));
                        sender.sendMessage(" ");
                    }

                    if (isPlayer) {
                        psender.spigot().sendMessage(helpList2.get(j + page * perPage).create());
                    } else {
                        sender.sendMessage(helpList.get((j + page * perPage)));
                    }

                    badPage = false;
                }
            }
        }

        if (badPage) {
            if (specific) {
                return CommandFinished.CUSTOM.replace("Comando desconocido.");
            } else {
                return CommandFinished.CUSTOM.replace("Página " + (page + 1) + " no existe en la ayuda.");
            }
        } else if (size > (page + 1) * perPage) {
            String next = "/" + command + " help " + (page + 2);
            if (!isPlayer) {
                sender.sendMessage(" ");
                sender.sendMessage(ChatColor.WHITE + " Usa " + ChatColor.YELLOW + next + ChatColor.WHITE + " para mostrar mas ayuda.");
            } else {
                psender.sendMessage(" ");

                ComponentBuilder inf = new ComponentBuilder(next + "\n")
                        .color(net.md_5.bungee.api.ChatColor.YELLOW)
                        .append("Click para ir a la\n")
                        .color(net.md_5.bungee.api.ChatColor.GRAY)
                        .append("siguiente página.", ComponentBuilder.FormatRetention.ALL);

                ComponentBuilder cb = new ComponentBuilder("Usa ")
                        .color(net.md_5.bungee.api.ChatColor.WHITE)
                        .append(next, ComponentBuilder.FormatRetention.NONE)
                        .color(net.md_5.bungee.api.ChatColor.YELLOW)
                        .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, next))
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, inf.create()))
                        .append(" para mostrar más ayuda.", ComponentBuilder.FormatRetention.NONE)
                        .color(net.md_5.bungee.api.ChatColor.WHITE);

                psender.spigot().sendMessage(cb.create());
            }

        } else {
            sender.sendMessage(" ");
        }

        return CommandFinished.DONE;
    }

    private String colorize(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    /**
     * Returns the string version of an exception. Helps with in-game error checking.
     */
    private String getTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

}