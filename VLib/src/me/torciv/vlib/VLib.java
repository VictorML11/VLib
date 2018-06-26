package me.torciv.vlib;

import me.torciv.vlib.commands.CommandManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public class VLib implements IVLib{

    private static VLib instance;

    public VLib() {
        instance = this;
    }

    /**
     * Generate a new CommandManager to register Commands
     * @param hook
     * @param cmdTag
     * @param perm
     * @param baseCmd
     * @param aliases
     * @return CommandManager
     */
    @Override
    public CommandManager generateBaseCmd(JavaPlugin hook, String cmdTag, String perm, String baseCmd, String ... aliases){
        return new CommandManager(hook, cmdTag, perm, baseCmd, aliases);
    }

    @Override
    public void loadCmdsFromClasses(CommandManager cm, ArrayList<Class<?>> classes) {
        for(Class<?> cls : classes){
            this.loadCmdsFromClass(cm, cls);
        }
    }

    @Override
    public void loadCmdsFromClass(CommandManager cm, Class<?> cls) {
        cm.loadCommandClass(cls);
    }


    public static VLib getInstance() {
        return instance;
    }

    public static void setInstance(VLib instance) {
        VLib.instance = instance;
    }
}
