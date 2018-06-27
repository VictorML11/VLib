package me.torciv.vlib;

import me.torciv.vlib.commands.CommandManager;
import me.torciv.vlib.commands.extra.language.LangType;

import java.util.ArrayList;

public interface IVLib {

    public CommandManager generateBaseCmd(String cmdTag, LangType langType, String perm, String baseCmd, String ... aliases);
    public void loadCmdsFromClasses(CommandManager cm, ArrayList<Class<?>> classes);
    public void loadCmdsFromClass(CommandManager cm, Class<?> cls);

}
