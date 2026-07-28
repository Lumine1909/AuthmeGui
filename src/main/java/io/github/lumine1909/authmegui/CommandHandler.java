package io.github.lumine1909.authmegui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CommandHandler implements TabExecutor {

    public CommandHandler(AuthmeGui plugin) {
        Objects.requireNonNull(plugin.getCommand("authmegui")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("authmegui")).setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player) || args.length != 1) {
            return true;
        }
        if (args[0].equalsIgnoreCase("enable")) {
            AuthmeGui.enabledPlayers.add(player.getName());
            player.sendMessage(Component.text("[AuthmeGui] 已启用Gui登录模式, 下次登录时会自动唤起Gui", NamedTextColor.GREEN));
        } else if (args[0].equalsIgnoreCase("disable")) {
            AuthmeGui.enabledPlayers.remove(player.getName());
            player.sendMessage(Component.text("[AuthmeGui] 已禁用Gui登录模式, 下次登录时会使用命令", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        return args.length == 1 ? List.of("enable", "disable") : Collections.emptyList();
    }
}
