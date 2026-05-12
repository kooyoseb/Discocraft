package org.kooyoseb.discocraft.discord;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.kooyoseb.discocraft.Discocraft;
import org.kooyoseb.discocraft.config.DiscocraftConfig;

public final class DiscordBridge {
    private final Discocraft plugin;
    private final DiscocraftConfig config;
    private final List<ConnectedBot> connectedBots = new CopyOnWriteArrayList<>();
    private volatile boolean connecting;
    private volatile String lastStatus = "not started";

    public DiscordBridge(Discocraft plugin, DiscocraftConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        if (!config.isConfigured()) {
            lastStatus = config.discordEnabled() ? "missing token(s) or channel id(s)" : "disabled";
            plugin.getLogger().info("Discord bridge is disabled or not configured.");
            return;
        }

        connecting = true;
        lastStatus = "connecting";
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::connect);
    }

    private void connect() {
        int failedBots = 0;
        for (String token : config.validBotTokens()) {
            try {
                JDA jda = JDABuilder.createDefault(token)
                        .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                        .setStatus(OnlineStatus.ONLINE)
                        .setActivity(Activity.playing(config.activity()))
                        .addEventListeners(new DiscordMessageListener(plugin, this))
                        .build();

                jda.awaitReady();
                List<TextChannel> channels = config.validChannelIds().stream()
                        .map(jda::getTextChannelById)
                        .filter(channel -> channel != null)
                        .toList();

                if (channels.isEmpty()) {
                    lastStatus = "bot connected, but configured channel id(s) were not found or bot cannot see them";
                    plugin.getLogger().warning("Discord bot connected, but none of the configured channels were found.");
                    shutdownJda(jda);
                    continue;
                }

                connectedBots.add(new ConnectedBot(jda, channels));
                lastStatus = "connected";
                plugin.getLogger().info("Discord bot connected to " + channels.size() + " channel(s).");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                lastStatus = "startup interrupted";
                plugin.getLogger().warning("Discord bridge startup was interrupted.");
                break;
            } catch (Exception exception) {
                failedBots++;
                lastStatus = "bot startup failed: " + exception.getMessage();
                plugin.getLogger().warning("Discord bot failed to start: " + exception.getMessage());
            }
        }

        connecting = false;
        if (connectedBots.isEmpty() && failedBots > 0) {
            lastStatus = "all bot connection attempts failed; check token, intents, and network";
        }
        if (!connectedBots.isEmpty()) {
            sendServerStart();
        }
    }

    public void shutdown() {
        for (ConnectedBot bot : connectedBots) {
            shutdownJda(bot.jda());
        }
        connectedBots.clear();
        connecting = false;
        lastStatus = "stopped";
    }

    public boolean isConnected() {
        return connectedBots.stream().anyMatch(bot -> bot.jda().getStatus() == JDA.Status.CONNECTED);
    }

    public boolean shouldProcessIncoming(JDA sourceJda, String channelId) {
        for (ConnectedBot bot : connectedBots) {
            boolean hasChannel = bot.channels().stream().anyMatch(channel -> channel.getId().equals(channelId));
            if (!hasChannel) {
                continue;
            }
            return bot.jda().equals(sourceJda);
        }
        return false;
    }

    public void sendMinecraftChat(Player player, String message) {
        if (!config.minecraftToDiscord() || !config.sendsDiscordEvent("chat")) {
            return;
        }

        sendToDiscord(config.minecraftChatFormat()
                .replace("%player%", player.getName())
                .replace("%message%", sanitizeDiscord(message)));
    }

    public void sendJoin(Player player) {
        if (config.joinQuitMessages() && config.sendsDiscordEvent("join")) {
            sendToDiscord(config.joinFormat().replace("%player%", player.getName()));
        }
    }

    public void sendQuit(Player player) {
        if (config.joinQuitMessages() && config.sendsDiscordEvent("quit")) {
            sendToDiscord(config.quitFormat().replace("%player%", player.getName()));
        }
    }

    public void sendDeath(String message) {
        if (config.deathMessages() && config.sendsDiscordEvent("death") && message != null && !message.isBlank()) {
            sendToDiscord(config.deathFormat().replace("%message%", sanitizeDiscord(message)));
        }
    }

    public void sendServerStart() {
        if (config.serverStartStopMessages() && config.sendsDiscordEvent("server-start")) {
            sendToDiscord(config.serverStartFormat());
        }
    }

    public void sendServerStop() {
        if (config.serverStartStopMessages() && config.sendsDiscordEvent("server-stop")) {
            sendToDiscord(config.serverStopFormat());
        }
    }

    public void sendAdminMessage(String message) {
        sendToDiscord(message);
    }

    public void broadcastDiscordMessage(String user, String message) {
        if (!config.discordToMinecraft()) {
            return;
        }

        String formatted = config.discordChatFormat()
                .replace("%label%", config.discordLabel())
                .replace("%user%", user)
                .replace("%message%", message);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.broadcastMessage(color(formatted)));
    }

    public String statusLine() {
        if (!config.discordEnabled()) {
            return "disabled";
        }
        if (!config.isConfigured()) {
            return "missing token(s) or channel id(s)";
        }
        if (connecting) {
            return "connecting";
        }
        return isConnected()
                ? "connected (" + connectedBots.size() + " bot(s), " + connectedChannelCount() + " channel route(s))"
                : "not connected: " + lastStatus;
    }

    public String onlinePlayersLine() {
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        if (online == 0) {
            return "No players online. (" + online + "/" + max + ")";
        }

        String players = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.joining(", "));
        return "Online players (" + online + "/" + max + "): " + players;
    }

    public DiscocraftConfig config() {
        return config;
    }

    private void sendToDiscord(String message) {
        if (!isConnected() || message.isBlank()) {
            return;
        }

        for (ConnectedBot bot : connectedBots) {
            for (TextChannel channel : bot.channels()) {
                channel.sendMessage(message)
                        .setAllowedMentions(EnumSet.of(MentionType.USER))
                        .queue(null, error ->
                                plugin.getLogger().warning("Failed to send Discord message: " + error.getMessage()));
            }
        }
    }

    private int connectedChannelCount() {
        return connectedBots.stream().mapToInt(bot -> bot.channels().size()).sum();
    }

    private void shutdownJda(JDA jda) {
        jda.shutdown();
        try {
            if (!jda.awaitShutdown(5, TimeUnit.SECONDS)) {
                jda.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            jda.shutdownNow();
        }
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static String sanitizeDiscord(String message) {
        return message
                .replace("@everyone", "@\u200beveryone")
                .replace("@here", "@\u200bhere");
    }

    private record ConnectedBot(JDA jda, List<TextChannel> channels) {
    }
}
