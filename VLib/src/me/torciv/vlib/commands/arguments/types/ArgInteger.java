package me.torciv.vlib.commands.arguments.types;

import me.torciv.vlib.commands.arguments.AbstractArg;

import java.util.List;

public class ArgInteger extends AbstractArg<Integer> {

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
