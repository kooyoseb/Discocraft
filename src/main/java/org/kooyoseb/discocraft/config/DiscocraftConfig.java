package org.kooyoseb.discocraft.config;

import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;

public record DiscocraftConfig(
        boolean discordEnabled,
        List<String> botTokens,
        List<String> channelIds,
        String activity,
        boolean minecraftToDiscord,
        boolean discordToMinecraft,
        List<String> discordEvents,
        boolean joinQuitMessages,
        boolean deathMessages,
        boolean serverStartStopMessages,
        boolean discordCommands,
        boolean discordConsoleEnabled,
        String discordConsoleCommandPrefix,
        List<String> discordConsoleAllowedPrefixes,
        List<String> discordConsoleAllowedRoleIds,
        String minecraftChatFormat,
        String discordLabel,
        String discordChatFormat,
        String joinFormat,
        String quitFormat,
        String deathFormat,
        String serverStartFormat,
        String serverStopFormat
) {
    public static DiscocraftConfig from(FileConfiguration config) {
        return new DiscocraftConfig(
                config.getBoolean("discord.enabled", false),
                readConfiguredList(config, "discord.bot-tokens", "discord.bot-token", "PUT_YOUR_DISCORD_BOT_TOKEN_HERE"),
                readConfiguredList(config, "discord.channel-ids", "discord.channel-id", "PUT_CHANNEL_ID_HERE"),
                config.getString("discord.activity", "Minecraft"),
                config.getBoolean("bridge.minecraft-to-discord", true),
                config.getBoolean("bridge.discord-to-minecraft", true),
                readDiscordEvents(config),
                config.getBoolean("bridge.join-quit-messages", true),
                config.getBoolean("bridge.death-messages", true),
                config.getBoolean("bridge.server-start-stop-messages", true),
                config.getBoolean("bridge.discord-commands", true),
                config.getBoolean("discord.console.enabled", false),
                config.getString("discord.console.command-prefix", "!mc"),
                readList(config, "discord.console.allowed-prefixes"),
                readList(config, "discord.console.allowed-role-ids"),
                config.getString("format.minecraft-chat", "**%player%**: %message%"),
                config.getString("format.discord-label", "&9[Discord]&r"),
                config.getString("format.discord-chat", "%label% %user%: %message%"),
                config.getString("format.join", ":green_circle: **%player% joined the server**"),
                config.getString("format.quit", ":red_circle: **%player% left the server**"),
                config.getString("format.death", ":skull: %message%"),
                config.getString("format.server-start", ":white_check_mark: **Server online**"),
                config.getString("format.server-stop", ":octagonal_sign: **Server offline**")
        );
    }

    public boolean isConfigured() {
        return discordEnabled
                && !validBotTokens().isEmpty()
                && !validChannelIds().isEmpty();
    }

    public List<String> validBotTokens() {
        return botTokens.stream()
                .filter(value -> !value.isBlank())
                .filter(value -> !value.equals("PUT_YOUR_DISCORD_BOT_TOKEN_HERE"))
                .toList();
    }

    public List<String> validChannelIds() {
        return channelIds.stream()
                .filter(value -> !value.isBlank())
                .filter(value -> !value.equals("PUT_CHANNEL_ID_HERE"))
                .toList();
    }

    public boolean sendsDiscordEvent(String event) {
        return discordEvents.stream().anyMatch(value ->
                value.equals("alls") || value.equals("all") || value.equals("*") || value.equalsIgnoreCase(event));
    }

    public boolean isConsoleCommandAllowed(String command) {
        String normalizedCommand = command.toLowerCase().trim();
        if (normalizedCommand.isBlank()) {
            return false;
        }

        return discordConsoleAllowedPrefixes.stream()
                .map(value -> value.toLowerCase().trim())
                .filter(value -> !value.isBlank())
                .anyMatch(prefix -> normalizedCommand.equals(prefix) || normalizedCommand.startsWith(prefix + " "));
    }

    private static List<String> readDiscordEvents(FileConfiguration config) {
        List<String> events = config.getStringList("bridge.discord-events").stream()
                .map(value -> value.toLowerCase().trim())
                .filter(value -> !value.isBlank())
                .toList();
        if (!events.isEmpty()) {
            return events;
        }

        String singleValue = config.getString("bridge.discord-events", "alls");
        if (singleValue == null || singleValue.isBlank()) {
            return List.of("alls");
        }
        return List.of(singleValue.toLowerCase().trim());
    }

    private static List<String> readConfiguredList(
            FileConfiguration config,
            String listPath,
            String legacyPath,
            String placeholder
    ) {
        List<String> values = config.getStringList(listPath).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        if (!values.isEmpty()) {
            return values;
        }

        String legacyValue = config.getString(legacyPath, placeholder);
        if (legacyValue == null || legacyValue.isBlank()) {
            return List.of();
        }
        return List.of(legacyValue.trim());
    }

    private static List<String> readList(FileConfiguration config, String path) {
        return config.getStringList(path).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
