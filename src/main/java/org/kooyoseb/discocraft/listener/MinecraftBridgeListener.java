package org.kooyoseb.discocraft.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.kooyoseb.discocraft.Discocraft;

public final class MinecraftBridgeListener implements Listener {
    private final Discocraft plugin;

    public MinecraftBridgeListener(Discocraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        plugin.discordBridge().sendMinecraftChat(player, message);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.discordBridge().sendJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.discordBridge().sendQuit(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        plugin.discordBridge().sendDeath(event.getDeathMessage());
    }
}
