package me.torciv.vlib.commands.arguments.types;

import me.torciv.vlib.commands.arguments.AbstractArg;

import java.util.List;

public class ArgFloat extends AbstractArg<Float> {

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
