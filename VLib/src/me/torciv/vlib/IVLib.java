package me.torciv.vlib;

import me.torciv.vlib.commands.CommandManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public interface IVLib {

    public CommandManager generateBaseCmd(JavaPlugin hook, String cmdTag, String perm, String baseCmd, String ... aliases);
    public void loadCmdsFromClasses(CommandManager cm, ArrayList<Class<?>> classes);
    public void loadCmdsFromClass(CommandManager cm, Class<?> cls);

}
