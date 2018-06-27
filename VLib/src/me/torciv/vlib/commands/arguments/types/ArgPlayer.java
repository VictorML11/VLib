package me.torciv.vlib.commands.arguments.types;

import me.torciv.vlib.commands.arguments.AbstractArg;
import me.torciv.vlib.commands.extra.CommandFinished;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ArgPlayer extends AbstractArg<OfflinePlayer> {

    public Player parseArg(String arg) {
        return Bukkit.getPlayer(arg);
    }

    public String getFailure() {
        return CommandFinished.EXISTPLAYER.getErrorString();
    }

    public List<String> getPredictions() {
        List<String> players = new ArrayList<String>();
        for (Player p : Bukkit.getOnlinePlayers())
            players.add(p.getName());
        return players;
    }
}
