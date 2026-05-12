package org.kooyoseb.discocraft.command;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kooyoseb.discocraft.Discocraft;

public final class DiscordLinkCommand implements TabExecutor {
    private static final List<String> SUBCOMMANDS = List.of("link", "unlink", "status");

    private final Discocraft plugin;

    public DiscordLinkCommand(Discocraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.messages().tr("link.only-player"));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            plugin.linkManager().linkedDiscordName(player.getUniqueId()).ifPresentOrElse(
                    discordName -> player.sendMessage(plugin.messages().tr("link.status.linked", "discord", discordName)),
                    () -> player.sendMessage(plugin.messages().tr("link.status.unlinked", "label", label))
            );
            return true;
        }

        if (args[0].equalsIgnoreCase("link")) {
            String code = plugin.linkManager().createCode(player);
            player.sendMessage(plugin.messages().tr("link.code", "code", code));
            player.sendMessage(plugin.messages().tr("link.code-help", "code", code));
            return true;
        }

        if (args[0].equalsIgnoreCase("unlink")) {
            plugin.linkManager().unlinkByMinecraftId(player.getUniqueId()).ifPresentOrElse(
                    ignored -> player.sendMessage(plugin.messages().tr("link.unlinked")),
                    () -> player.sendMessage(plugin.messages().tr("link.not-linked"))
            );
            return true;
        }

        player.sendMessage(plugin.messages().tr("link.usage", "label", label));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length != 1) {
            return List.of();
        }

        String prefix = args[0].toLowerCase();
        List<String> matches = new ArrayList<>();
        for (String subcommand : SUBCOMMANDS) {
            if (subcommand.startsWith(prefix)) {
                matches.add(subcommand);
            }
        }
        return matches;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
