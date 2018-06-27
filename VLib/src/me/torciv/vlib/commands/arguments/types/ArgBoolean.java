package me.torciv.vlib.commands.arguments.types;

import me.torciv.vlib.commands.arguments.AbstractArg;

import java.util.Arrays;
import java.util.List;

public class ArgBoolean extends AbstractArg<Boolean> {

    public Boolean parseArg(String arg) {
        if (arg.equalsIgnoreCase("true") || arg.equalsIgnoreCase("yes") || arg.equalsIgnoreCase("on") || arg.equalsIgnoreCase("1"))
            return true;
        if (arg.equalsIgnoreCase("false") || arg.equalsIgnoreCase("no") || arg.equalsIgnoreCase("off") || arg.equalsIgnoreCase("0"))
            return false;
        return null;
    }

    public String getFailure() {
        return "Argument not a valid boolean.";
    }

    public List<String> getPredictions() {
        return Arrays.asList(new String[]{"true", "false"});
    }
}
