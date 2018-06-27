package me.torciv.vlib.commands.arguments.types;

import me.torciv.vlib.commands.arguments.AbstractArg;

import java.util.ArrayList;
import java.util.List;

public class ArgArray extends AbstractArg<List<String>> {

    public List<String> parseArg(String arg) {
        List<String> list = new ArrayList<String>();
        for (String str : arg.split(","))
            list.add(str);
        return list;
    }

    public String getFailure() {
        return "Argument failure.";
    }

    public List<String> getPredictions() {
        return null;
    }
}
