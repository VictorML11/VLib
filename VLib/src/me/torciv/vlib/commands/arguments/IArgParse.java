package me.torciv.vlib.commands.arguments;

import java.util.List;

public interface IArgParse<T> {

    T parseArg(String arg);

    List<String> getPredictions();

    String getFailure();

}
