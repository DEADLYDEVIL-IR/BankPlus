package me.pulsi_.bankplus.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TabCompletion implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender s, Command command, String alias, String[] args) {

        List<String> args1 = new ArrayList<>();
        if (args1.isEmpty()) {
            if (s.hasPermission("bankplus.withdraw")) {
                args1.add("withdraw");
            }
            if (s.hasPermission("bankplus.deposit")) {
                args1.add("deposit");
            }
            if (s.hasPermission("bankplus.view")) {
                args1.add("view");
            }
            if (s.hasPermission("bankplus.help")) {
                args1.add("help");
            }
            if (s.hasPermission("bankplus.reload")) {
                args1.add("reload");
            }
            if (s.hasPermission("bankplus.set")) {
                args1.add("set");
            }
            if (s.hasPermission("bankplus.add")) {
                args1.add("add");
            }
            if (s.hasPermission("bankplus.remove")) {
                args1.add("remove");
            }
            if (s.hasPermission("bankplus.interest.restart")) {
                args1.add("interest");
            }
        }

        List<String> args2 = new ArrayList<>();
        if (args2.isEmpty()) {
            if (s.hasPermission("bankplus.interest.restart")) {
                args2.add("restart");
            }
        }

        List<String> resultArgs1 = new ArrayList<>();
        if (args.length == 1) {
            for (String a : args1) {
                if (a.toLowerCase().startsWith(args[0].toLowerCase()))
                    resultArgs1.add(a);
            }
            return resultArgs1;
        }

        List<String> resultArgs2 = new ArrayList<>();
        if (args.length == 2) {
            for (String a : args2) {
                if (args[1].equalsIgnoreCase("restart")) {
                    resultArgs2.add(a);
                } else {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        resultArgs2.add(p.getName());
                    }
                }
            }
            return resultArgs2;
        }

        return null;
    }
}