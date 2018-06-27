package me.torciv.vlib.commands.arguments.types;

import me.torciv.vlib.commands.arguments.AbstractArg;

import java.util.Arrays;
import java.util.List;

public class ArgByte extends AbstractArg<Byte> {

    public Byte parseArg(String arg) {
        return Byte.valueOf(arg);
    }

    public String getFailure() {
        return "Argument not a valid byte.";
    }

    public List<String> getPredictions() {
        return Arrays.asList(new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"});
    }
}
