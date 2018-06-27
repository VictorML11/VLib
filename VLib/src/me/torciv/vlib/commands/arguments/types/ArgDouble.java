package me.torciv.vlib.commands.arguments.types;

import me.torciv.vlib.commands.arguments.AbstractArg;

import java.util.List;

public class ArgDouble extends AbstractArg<Double> {

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
