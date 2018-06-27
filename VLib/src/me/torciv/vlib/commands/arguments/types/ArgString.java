package me.torciv.vlib.commands.arguments.types;

import me.torciv.vlib.commands.arguments.AbstractArg;

import java.util.List;

public class ArgString extends AbstractArg<String> {

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
