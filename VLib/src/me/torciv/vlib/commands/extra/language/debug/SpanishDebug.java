package me.torciv.vlib.commands.extra.language.debug;

import me.torciv.vlib.commands.Cmd;
import me.torciv.vlib.commands.arguments.types.ArgBoolean;
import me.torciv.vlib.commands.extra.CommandFinished;
import me.torciv.vlib.commands.extra.CommandOnly;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class SpanishDebug extends Debug{

    @Cmd(cmd = "debug",
            args = "[on/off]",
            argTypes = {ArgBoolean.class},
            help = "Activa/Desactiva el modo Debug.",
            longhelp = "Activa/Desactiva el modo Debug. Muestra el tiempo de ejecucción de un Comando.",
            only = CommandOnly.OP,
            permission = "debug")
    public static CommandFinished cmdToggleDebugMode(CommandSender sender, Object[] args) {
        GLOBAL_DEBUG = (args.length != 0 ? (Boolean) args[0] : !GLOBAL_DEBUG);
        sender.sendMessage(ChatColor.YELLOW + "Modo Debug: " + (GLOBAL_DEBUG ? ChatColor.GREEN + "ACTIVADO" : ChatColor.RED + "DESACTIVADO"));
        return CommandFinished.DONE;
    }

    @Override
    public Class<?> getClas() {
        return SpanishDebug.class;
    }
}
