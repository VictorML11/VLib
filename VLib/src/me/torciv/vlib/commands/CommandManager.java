package me.torciv.vlib.commands;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    static boolean GLOBAL_DEBUG = false;

    @Cmd(cmd = "debug",
            args = "[on/off]",
            argTypes = {Arg.ArgBoolean.class},
            help = "Activa/Desactiva el modo debug.",
            longhelp = "Activa/Desactiva el modo debug. Muestra información al usar un comando.",
            only = CommandOnly.OP,
            permission = "debug")
    public static CommandFinished cmdToggleDebugMode(CommandSender sender, Object[] args) {
        GLOBAL_DEBUG = (args.length != 0 ? (Boolean) args[0] : !GLOBAL_DEBUG);
        sender.sendMessage(ChatColor.YELLOW + "Modo Debug: " + (GLOBAL_DEBUG ? ChatColor.GREEN + "ACTIVADO" : ChatColor.RED + "DESACTIVADO"));
        return CommandFinished.DONE;
    }

    private static HashMap<Class<? extends AbstractArg<?>>, AbstractArg<?>> argInstances = new HashMap<Class<? extends AbstractArg<?>>, AbstractArg<?>>();

    private ArrayList<Cmd> commands = new ArrayList<Cmd>();
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
            for (Method method : commandClass.getMethods()) {
                if (method.isAnnotationPresent(Cmd.class)) {
                    Cmd cmd = method.getAnnotation(Cmd.class);
                    commands.add(cmd);
                    commandMethods.add(method);
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        return this;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> predictions = new ArrayList<String>();
        String token = (args.length == 0 ? "" : args[args.length - 1]);

        for (Cmd c : commands) {
            List<String> cmdPredictions = getPredicted(c, token, args.length - 1);
            // Prevent duplicate entries
            if (cmdPredictions != null) {
                for (String str : cmdPredictions) {
                    if (!predictions.contains(str))
                        predictions.add(str);
                }
            }
        }

        return predictions;
    }

    /**
     * Get a prediction of the next command argument.
     */
    private List<String> getPredicted(Cmd c, String token, int i) {
        String[] cmdArg = c.cmd().split(" ");
        // If no token, return all possible commands.
        if (token == "")
            return Arrays.asList(new String[]{cmdArg[0]});
        // If the amount of args is more than available, or it doesn't start with the token.
        if (i >= cmdArg.length) {
            int argNum = i - cmdArg.length;
            if (argNum >= c.argTypes().length)
                return null;
            else {
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
                    for (int i = 0; i < (args.length > fixedArgs.length ? fixedArgs.length : args.length); i++)
                        fixedArgs[i] = args[i];

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
                    } else
                        sender.sendMessage(ChatColor.GOLD + "   Querías ejecutar: " + ChatColor.GRAY + "/" + label + " " + entry.getValue().cmd() + ChatColor.GOLD + "? Estamos " + ((int) (entry.getKey() * 10) / 10D) + "% seguros.");
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

                        if (!valid)
                            continue;
                    } else
                        continue;

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
                        if (bestFit.only() == CommandOnly.CONSOLE)
                            return CommandFinished.NOPLAYER;
                    } else if (bestFit.only() == CommandOnly.PLAYER)
                        return CommandFinished.NOCONSOLE;

                    // Check for the "op" argument and permission argument
                    if ((bestFit.only() == CommandOnly.OP ? !sender.isOp() : false) ||
                            (bestFit.permission() != "" ? !sender.hasPermission(permissionScheme + "." + bestFit.permission()) : false))
                        return CommandFinished.PERMISSION;

                    // Split up the args; arguments in quotes count as a single argument.
                    List<Object> cmdArgList = new ArrayList<Object>();
                    Matcher m = Pattern.compile("(?:([^\"]\\S*)|\"(.+?)\")\\s*").matcher(StringUtils.join(args, " ").replaceFirst(bestFit.cmd(), "").trim());
                    for (int j = 0; m.find(); j++) {
                        // Apply the requested argument type.
                        Class<? extends AbstractArg<?>> requestedType = (j < bestFit.argTypes().length ? bestFit.argTypes()[j] : Arg.ArgString.class);
                        // Cache the instance.
                        if (!argInstances.containsKey(requestedType))
                            argInstances.put(requestedType, requestedType.newInstance());
                        AbstractArg<?> absArg = argInstances.get(requestedType);
                        try {
                            Object arg = absArg.parseArg(m.group(1) != null ? m.group(1) : m.group(2));
                            if (arg == null)
                                // Some argument parsers don't throw an exception. Just an extra precaution.
                                throw new CommandException(absArg.getFailure() + " (" + m.group(1) != null ? m.group(1) : m.group(2) + ")");
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
                if (page < 0) // Negative pages are bad juju.
                    page = 0;
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
            if (specific && !cmd.cmd().startsWith(cmdLabel))
                continue;

            // Should it even show?
            if (cmd.showInHelp()) {
                // If it can't be used, don't show it! Simple! :D
                boolean canUse = cmd.permission().equals("") || (sender.hasPermission(permissionScheme + "." + cmd.permission()));

                // Is it op-only?
                if (cmd.only() == CommandOnly.OP)
                    canUse = (cmd.permission().equals("") ? sender.isOp() : canUse);

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

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    /**
     * Attach to a function to denote it as a command. You must register the class it is in as a command class before it will be used.
     */
    public static @interface Cmd {
        /**
         * Takes in any amount of subcommands. The command handler always chooses the best-fit command.
         * <br><br>
         * Example:
         * <br>
         * cmd = "sub1 sub2 sub3 sub4 sub5"
         */
        String cmd() default "";

        /**
         * The arguments for the command. Required arguments must be enclosed in <>'s
         * <br><br>
         * Example:
         * <br>
         * args = "&lt;arg1&gt; &lt;arg2&gt; [arg3]"
         * <br>
         * Argument 1 and 2 are required; the third is optional.
         */
        String args() default "";

        /**
         * Specifies the type that an argument should be. Default is Arg.ArgString.
         * <br><br>
         * Example:
         * <br>
         * argTypes = { Arg.ArgInteger, Arg.ArgString, Arg.ArgPlayer }
         * <ul>
         * <li>Argument 1 <i>must</i> be an integer.</li>
         * <li>Argument 2 <i>must</i> be a string(anything).</li>
         * <li>Argument 3 <i>must</i> be an online player.</li>
         * </ul>
         */
        Class<? extends AbstractArg<?>>[] argTypes() default {};

        /**
         * Argument description
         * argDesc = {"Intruduce un numero", "Introduce un testo en comillas"}
         */
        String[] argDesc() default {};

        /**
         * The text to show next to a command when a user does /cmd help.
         */
        String help() default "Default help thingy... :(";

        /**
         * The text to show next to a command when a user does /cmd help &lt;command&gt;.
         * <br><br>
         * Use to give more information about the command.
         */
        String longhelp() default "";

        /**
         * Should the command be shown in help at all?
         */
        boolean showInHelp() default true;

        /**
         * Specifies if the command should be restricted to CONSOLE, OP, or PLAYERS. Otherwise, ALL.
         */
        CommandOnly only() default CommandOnly.ALL;

        /**
         * The permission node to use.
         * <br><br>
         * Example:
         * <br>
         * new CommandManager(plugin, "Test", "testnode", "test");
         * <br>
         * permission = "edit"
         * <br>
         * This setup would require a player to have testnode.edit to use the command.
         */
        String permission() default "";
    }

    /**
     * Used to define who is allowed to use a command.
     */
    public static enum CommandOnly {
        /**
         * Only players can use the command.
         */
        PLAYER,
        /**
         * Only op players can use the command. Can be overridden by a matched permission.
         */
        OP,
        /**
         * Only the console can use the command.
         */
        CONSOLE,
        /**
         * Anyone can use the command, given their permissions match.
         */
        ALL
    }

    /**
     * This can be used to immediately throw an error without returning a <code>CommandFinished</code>.
     * It'll display the specified error.
     */
    public static class CommandException extends Exception {
        private static final long serialVersionUID = 1L;

        public CommandException(String message) {
            super(message);
        }
    }

    /**
     * A set of command errors.
     */
    public static enum CommandFinished {
        /**
         * Finished correctly
         */
        DONE(false, "Hecho"),
        /**
         * Command does not exist
         */
        COMMAND(true, "El comando no existe. Usa /%s para ayuda."),
        /**
         * Command does not exist
         */
        BADCOMMAND(true, "Error usa: /%s "),
        /**
         * Console not allowed to use
         */
        NOCONSOLE(true, "Este comando no puede ser ejecutado desde la consola."),
        /**
         * Player not allowed to use
         */
        NOPLAYER(true, "Este comando no puede ser ejecutador por jugadores."),
        /**
         * Player does not exist
         */
        EXISTPLAYER(true, "El jugador no existe"),
        /**
         * Incorrect permissions
         */
        PERMISSION(true, "Permisos insuficientes."),

        HOLDBLOCK(true, "You must be holding a block."),
        HOLDITEM(true, "You must be holding an item."),

        LONGSTRING(true, "String cannot be longer than %s!"),

        /**
         * Custom error
         */
        CUSTOM(true, "%s"),
        EXCEPTION(true, "An exception occured. Please contact a member of staff and tell them!");

        private boolean shouldPrint;
        private String errorString;
        private String extraString;

        CommandFinished(boolean par1ShouldPrint, String par1Error) {
            shouldPrint = par1ShouldPrint;
            errorString = par1Error;
        }

        public boolean shouldPrint() {
            return shouldPrint;
        }

        public String getErrorString() {
            if (extraString != null)
                return errorString.replace("%s", extraString);
            else
                return errorString;
        }

        public CommandFinished replace(String theString) {
            extraString = theString;
            return this;
        }
    }

    /*
     * Arguments
     */
    public static interface IArgParse<T> {
        T parseArg(String arg);

        List<String> getPredictions();

        String getFailure();
    }

    public static abstract class AbstractArg<T> implements IArgParse<T> {
        public AbstractArg() {
        }
    }

    public static class Arg {
        public static class ArgArray extends AbstractArg<List<String>> {
            public List<String> parseArg(String arg) {
                List<String> list = new ArrayList<String>();
                for (String str : arg.split(","))
                    list.add(str);
                return list;
            }

            public String getFailure() {
                return "Argument failure.";
            }

            public List<String> getPredictions() {
                return null;
            }
        }

        public static class ArgBoolean extends AbstractArg<Boolean> {
            public Boolean parseArg(String arg) {
                if (arg.equalsIgnoreCase("true") || arg.equalsIgnoreCase("yes") || arg.equalsIgnoreCase("on") || arg.equalsIgnoreCase("1"))
                    return true;
                if (arg.equalsIgnoreCase("false") || arg.equalsIgnoreCase("no") || arg.equalsIgnoreCase("off") || arg.equalsIgnoreCase("0"))
                    return false;
                return null;
            }

            public String getFailure() {
                return "Argument not a valid boolean.";
            }

            public List<String> getPredictions() {
                return Arrays.asList(new String[]{"true", "false"});
            }
        }

        public static class ArgByte extends AbstractArg<Byte> {
            public Byte parseArg(String arg) {
                return Byte.valueOf(arg);
            }

            public String getFailure() {
                return "Argument not a valid byte.";
            }

            public List<String> getPredictions() {
                return Arrays.asList(new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"});
            }
        }

        public static class ArgDouble extends AbstractArg<Double> {
            public Double parseArg(String arg) {
                return Double.valueOf(arg);
            }

            public String getFailure() {
                return "Argument not a real number.";
            }

            public List<String> getPredictions() {
                return null;
            }
        }

        public static class ArgFloat extends AbstractArg<Float> {
            public Float parseArg(String arg) {
                return Float.valueOf(arg);
            }

            public String getFailure() {
                return "Argument not a floating point number.";
            }

            public List<String> getPredictions() {
                return null;
            }
        }

        public static class ArgInteger extends AbstractArg<Integer> {
            public Integer parseArg(String arg) {
                return Integer.valueOf(arg);
            }

            public String getFailure() {
                return "Argument not an integer.";
            }

            public List<String> getPredictions() {
                return null;
            }
        }

        public static class ArgPlayer extends AbstractArg<OfflinePlayer> {
            public Player parseArg(String arg) {
                return Bukkit.getPlayer(arg);
            }

            public String getFailure() {
                return CommandFinished.EXISTPLAYER.getErrorString();
            }

            public List<String> getPredictions() {
                List<String> players = new ArrayList<String>();
                for (Player p : Bukkit.getOnlinePlayers())
                    players.add(p.getName());
                return players;
            }
        }

        public static class ArgOfflinePlayer extends AbstractArg<OfflinePlayer> {
            @SuppressWarnings("deprecation")
            public OfflinePlayer parseArg(String arg) {
                return Bukkit.getOfflinePlayer(arg);
            }

            public String getFailure() {
                return CommandFinished.EXISTPLAYER.getErrorString();
            }

            public List<String> getPredictions() {
                List<String> players = new ArrayList<String>();
                for (Player p : Bukkit.getOnlinePlayers())
                    players.add(p.getName());
                return players;
            }
        }

        public static class ArgString extends AbstractArg<String> {
            public String parseArg(String arg) {
                return arg;
            }

            public String getFailure() {
                return "Could not parse string.";
            }

            public List<String> getPredictions() {
                return null;
            }
        }
    }


}