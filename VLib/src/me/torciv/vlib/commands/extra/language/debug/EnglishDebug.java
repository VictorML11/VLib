package me.torciv.vlib.commands.extra.language.debug;

import me.torciv.vlib.commands.Cmd;
import me.torciv.vlib.commands.arguments.types.ArgBoolean;
import me.torciv.vlib.commands.extra.CommandFinished;
import me.torciv.vlib.commands.extra.CommandOnly;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class EnglishDebug extends Debug {

    @Cmd(cmd = "debug",
            args = "[on/off]",
            argTypes = {ArgBoolean.class},
            help = "Enable/Disable Debug Mode.",
            longhelp = "Enable/Disable Debug Mode. Shows the time taken when a Command is executed",
            only = CommandOnly.OP,
            permission = "debug")
    public static CommandFinished cmdToggleDebugMode(CommandSender sender, Object[] args) {
        GLOBAL_DEBUG = (args.length != 0 ? (Boolean) args[0] : !GLOBAL_DEBUG);
        sender.sendMessage(ChatColor.YELLOW + "Debug Mode: " + (GLOBAL_DEBUG ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
        return CommandFinished.DONE;
    }


    @Override
    public Class<?> getClas() {
        return EnglishDebug.class;
    }
}
