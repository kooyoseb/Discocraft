package org.kooyoseb.discocraft.command;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kooyoseb.discocraft.Discocraft;
import org.kooyoseb.discocraft.discord.DiscordBridge;
import org.kooyoseb.discocraft.language.Messages;

public final class DiscocraftCommand implements TabExecutor {
    private static final List<String> SUBCOMMANDS = List.of("status", "reload", "reset", "send", "get", "set", "events", "bot", "channel", "language");
    private static final List<String> LIST_COMMANDS = List.of("list", "add", "remove", "clear");
    private static final List<String> LANGUAGES = List.of("ko_kr", "en_us");
    private static final List<String> DISCORD_EVENTS = List.of(
            "alls",
            "chat",
            "join",
            "quit",
            "death",
            "server-start",
            "server-stop",
            "none"
    );
    private static final List<String> CONFIG_KEYS = List.of(
            "language",
            "discord.enabled",
            "discord.bot-token",
            "discord.channel-id",
            "discord.bot-tokens",
            "discord.channel-ids",
            "discord.activity",
            "discord.console.enabled",
            "discord.console.command-prefix",
            "discord.console.allowed-prefixes",
            "discord.console.allowed-role-ids",
            "bridge.minecraft-to-discord",
            "bridge.discord-to-minecraft",
            "bridge.discord-events",
            "bridge.join-quit-messages",
            "bridge.death-messages",
            "bridge.server-start-stop-messages",
            "bridge.discord-commands",
            "format.minecraft-chat",
            "format.discord-label",
            "format.discord-chat",
            "format.join",
            "format.quit",
            "format.death",
            "format.server-start",
            "format.server-stop"
    );
    private static final List<String> BOOLEAN_VALUES = List.of("true", "false");

    private final Discocraft plugin;
    private DiscordBridge bridge;

    public DiscocraftCommand(Discocraft plugin, DiscordBridge bridge) {
        this.plugin = plugin;
        this.bridge = bridge;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(color("&bDiscocraft&7: &f" + bridge.statusLine()));
            return true;
        }

        if (args[0].equalsIgnoreCase("language")) {
            if (args.length == 1) {
                sender.sendMessage(plugin.messages().tr("language.current", "language", plugin.messages().currentLanguage()));
                return true;
            }

            String language = args[1].toLowerCase();
            if (!Messages.isSupported(language)) {
                sender.sendMessage(plugin.messages().tr("language.unsupported"));
                return true;
            }

            plugin.getConfig().set("language", language);
            plugin.saveConfig();
            plugin.reloadBridge();
            bridge = plugin.discordBridge();
            sender.sendMessage(plugin.messages().tr("language.changed", "language", language));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadBridge();
            bridge = plugin.discordBridge();
            sender.sendMessage(color("&bDiscocraft&7: &fconfig reloaded from file. Bridge is " + bridge.statusLine() + "."));
            return true;
        }

        if (args[0].equalsIgnoreCase("reset")) {
            if (args.length == 1) {
                plugin.saveResource("config.yml", true);
                plugin.reloadBridge();
                bridge = plugin.discordBridge();
                sender.sendMessage(color("&bDiscocraft&7: &fall config values reset to defaults."));
                sender.sendMessage(color("&7Bridge is now &f" + bridge.statusLine() + "&7."));
                return true;
            }

            if (args.length != 2 || !CONFIG_KEYS.contains(args[1])) {
                sender.sendMessage(color("&cUsage: /" + label + " reset [key]"));
                sender.sendMessage(color("&7Keys: &f" + String.join(", ", CONFIG_KEYS)));
                return true;
            }

            FileConfiguration defaults = defaultConfig();
            Object defaultValue = defaults.get(args[1]);
            plugin.getConfig().set(args[1], defaultValue);
            plugin.saveConfig();
            plugin.reloadBridge();
            bridge = plugin.discordBridge();

            sender.sendMessage(color("&bDiscocraft&7: &f" + args[1] + " reset to " + displayValue(args[1], defaultValue) + "."));
            sender.sendMessage(color("&7Bridge is now &f" + bridge.statusLine() + "&7."));
            return true;
        }

        if (args[0].equalsIgnoreCase("send")) {
            if (args.length < 2) {
                sender.sendMessage(color("&cUsage: /" + label + " send <message>"));
                return true;
            }
            String message = String.join(" ", List.of(args).subList(1, args.length));
            bridge.sendAdminMessage(message);
            sender.sendMessage(color("&bDiscocraft&7: &fsent."));
            return true;
        }

        if (args[0].equalsIgnoreCase("get")) {
            if (args.length != 2 || !CONFIG_KEYS.contains(args[1])) {
                sender.sendMessage(color("&cUsage: /" + label + " get <key>"));
                sender.sendMessage(color("&7Keys: &f" + String.join(", ", CONFIG_KEYS)));
                return true;
            }

            Object value = plugin.getConfig().get(args[1]);
            sender.sendMessage(color("&bDiscocraft&7: &f" + args[1] + " = " + displayValue(args[1], value)));
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 3 || !CONFIG_KEYS.contains(args[1])) {
                sender.sendMessage(color("&cUsage: /" + label + " set <key> <value>"));
                sender.sendMessage(color("&7Keys: &f" + String.join(", ", CONFIG_KEYS)));
                return true;
            }

            String key = args[1];
            String rawValue = String.join(" ", List.of(args).subList(2, args.length));
            Object value = parseConfigValue(key, rawValue);
            if (value == null) {
                if (key.equals("language")) {
                    sender.sendMessage(plugin.messages().tr("language.unsupported"));
                } else if (key.equals("bridge.discord-events")) {
                    sender.sendMessage(color("&c" + key + " must be one or more of: " + String.join(", ", DISCORD_EVENTS)));
                } else {
                    sender.sendMessage(color("&c" + key + " must be true or false."));
                }
                return true;
            }

            plugin.getConfig().set(key, value);
            plugin.saveConfig();
            plugin.reloadBridge();
            bridge = plugin.discordBridge();

            sender.sendMessage(color("&bDiscocraft&7: &f" + key + " updated to " + displayValue(key, value) + "."));
            sender.sendMessage(color("&7Bridge is now &f" + bridge.statusLine() + "&7."));
            return true;
        }

        if (args[0].equalsIgnoreCase("events")) {
            if (args.length == 1) {
                sender.sendMessage(color("&bDiscocraft&7: &fdiscord-events = "
                        + displayValue("bridge.discord-events", plugin.getConfig().get("bridge.discord-events"))));
                sender.sendMessage(color("&7Usage: &f/" + label + " events <alls|none|chat join quit death server-start server-stop>"));
                return true;
            }

            List<String> events = parseEventList(List.of(args).subList(1, args.length));
            if (events == null) {
                sender.sendMessage(color("&cUnknown event. Allowed: " + String.join(", ", DISCORD_EVENTS)));
                return true;
            }

            plugin.getConfig().set("bridge.discord-events", events);
            plugin.saveConfig();
            plugin.reloadBridge();
            bridge = plugin.discordBridge();

            sender.sendMessage(color("&bDiscocraft&7: &fdiscord-events updated to " + String.join(", ", events) + "."));
            sender.sendMessage(color("&7Bridge is now &f" + bridge.statusLine() + "&7."));
            return true;
        }

        if (args[0].equalsIgnoreCase("bot")) {
            return handleListCommand(sender, label, args, "discord.bot-tokens", "bot token", true);
        }

        if (args[0].equalsIgnoreCase("channel")) {
            return handleListCommand(sender, label, args, "discord.channel-ids", "channel id", false);
        }

        sender.sendMessage(color("&cUsage: /" + label + " <status|reload|reset|send|get|set|events|bot|channel>"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            return startsWith(SUBCOMMANDS, args[0]);
        }

        if ((args[0].equalsIgnoreCase("get")
                || args[0].equalsIgnoreCase("set")
                || args[0].equalsIgnoreCase("reset")) && args.length == 2) {
            return startsWith(CONFIG_KEYS, args[1]);
        }

        if ((args[0].equalsIgnoreCase("bot") || args[0].equalsIgnoreCase("channel")) && args.length == 2) {
            return startsWith(LIST_COMMANDS, args[1]);
        }

        if (args[0].equalsIgnoreCase("language") && args.length == 2) {
            return startsWith(LANGUAGES, args[1]);
        }

        if (args[0].equalsIgnoreCase("set") && args.length == 3 && args[1].equals("language")) {
            return startsWith(LANGUAGES, args[2]);
        }

        if (args[0].equalsIgnoreCase("set") && args.length == 3 && isBooleanKey(args[1])) {
            return startsWith(BOOLEAN_VALUES, args[2]);
        }

        if (args[0].equalsIgnoreCase("set") && args.length >= 3 && args[1].equals("bridge.discord-events")) {
            return startsWith(DISCORD_EVENTS, args[args.length - 1]);
        }

        if (args[0].equalsIgnoreCase("events") && args.length >= 2) {
            return startsWith(DISCORD_EVENTS, args[args.length - 1]);
        }

        return List.of();
    }

    private static List<String> startsWith(List<String> options, String rawPrefix) {
        String prefix = rawPrefix.toLowerCase();
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(prefix)) {
                matches.add(option);
            }
        }
        return matches;
    }

    private FileConfiguration defaultConfig() {
        try (InputStream stream = plugin.getResource("config.yml")) {
            if (stream == null) {
                return new YamlConfiguration();
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to read default config.yml: " + exception.getMessage());
            return new YamlConfiguration();
        }
    }

    private static Object parseConfigValue(String key, String rawValue) {
        if (key.equals("language")) {
            String language = rawValue.toLowerCase();
            return Messages.isSupported(language) ? language : null;
        }

        if (key.equals("discord.bot-tokens")
                || key.equals("discord.channel-ids")
                || key.equals("discord.console.allowed-prefixes")
                || key.equals("discord.console.allowed-role-ids")) {
            return parsePlainList(rawValue);
        }

        if (key.equals("bridge.discord-events")) {
            List<String> events = parseEventList(List.of(rawValue.split("[, ]+")));
            return events == null ? null : events;
        }

        if (!isBooleanKey(key)) {
            return rawValue;
        }

        if (rawValue.equalsIgnoreCase("true") || rawValue.equalsIgnoreCase("on") || rawValue.equalsIgnoreCase("yes")) {
            return true;
        }
        if (rawValue.equalsIgnoreCase("false") || rawValue.equalsIgnoreCase("off") || rawValue.equalsIgnoreCase("no")) {
            return false;
        }
        return null;
    }

    private static List<String> parseEventList(List<String> rawEvents) {
        List<String> events = rawEvents.stream()
                .map(value -> value.toLowerCase().trim())
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        if (events.isEmpty()) {
            return null;
        }

        if (events.contains("all")) {
            events = events.stream().map(value -> value.equals("all") ? "alls" : value).toList();
        }
        if (events.contains("*")) {
            events = events.stream().map(value -> value.equals("*") ? "alls" : value).toList();
        }
        if (events.contains("alls")) {
            return List.of("alls");
        }
        if (events.contains("none")) {
            return List.of("none");
        }
        if (!DISCORD_EVENTS.containsAll(events)) {
            return null;
        }
        return events;
    }

    private boolean handleListCommand(
            CommandSender sender,
            String label,
            String[] args,
            String key,
            String itemName,
            boolean hideValues
    ) {
        if (args.length == 1 || args[1].equalsIgnoreCase("list")) {
            List<String> values = plugin.getConfig().getStringList(key);
            sender.sendMessage(color("&bDiscocraft&7: &f" + key + " = " + displayList(values, hideValues)));
            sender.sendMessage(color("&7Usage: &f/" + label + " " + args[0].toLowerCase() + " <list|add|remove|clear>"));
            return true;
        }

        if (args[1].equalsIgnoreCase("add")) {
            if (args.length < 3) {
                sender.sendMessage(color("&cUsage: /" + label + " " + args[0].toLowerCase() + " add <" + itemName + ">"));
                return true;
            }

            String value = String.join(" ", List.of(args).subList(2, args.length)).trim();
            List<String> values = new ArrayList<>(plugin.getConfig().getStringList(key));
            if (!values.contains(value)) {
                values.add(value);
            }
            updateListSetting(key, values);
            sender.sendMessage(color("&bDiscocraft&7: &fadded " + itemName + " " + displaySingle(value, hideValues) + "."));
            sender.sendMessage(color("&7Bridge is now &f" + bridge.statusLine() + "&7."));
            return true;
        }

        if (args[1].equalsIgnoreCase("remove")) {
            if (args.length < 3) {
                sender.sendMessage(color("&cUsage: /" + label + " " + args[0].toLowerCase() + " remove <" + itemName + ">"));
                return true;
            }

            String value = String.join(" ", List.of(args).subList(2, args.length)).trim();
            List<String> values = new ArrayList<>(plugin.getConfig().getStringList(key));
            boolean removed = values.remove(value);
            updateListSetting(key, values);
            sender.sendMessage(color(removed
                    ? "&bDiscocraft&7: &fremoved " + itemName + " " + displaySingle(value, hideValues) + "."
                    : "&cThat " + itemName + " was not in the list."));
            sender.sendMessage(color("&7Bridge is now &f" + bridge.statusLine() + "&7."));
            return true;
        }

        if (args[1].equalsIgnoreCase("clear")) {
            updateListSetting(key, List.of());
            sender.sendMessage(color("&bDiscocraft&7: &fcleared " + key + "."));
            sender.sendMessage(color("&7Bridge is now &f" + bridge.statusLine() + "&7."));
            return true;
        }

        sender.sendMessage(color("&cUsage: /" + label + " " + args[0].toLowerCase() + " <list|add|remove|clear>"));
        return true;
    }

    private void updateListSetting(String key, List<String> values) {
        plugin.getConfig().set(key, values);
        plugin.saveConfig();
        plugin.reloadBridge();
        bridge = plugin.discordBridge();
    }

    private static List<String> parsePlainList(String rawValue) {
        return List.of(rawValue.split("[, ]+")).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private static boolean isBooleanKey(String key) {
        return key.equals("discord.enabled") || key.equals("discord.console.enabled") || key.startsWith("bridge.");
    }

    private static String displayValue(String key, Object value) {
        if (key.equals("discord.bot-token") || key.equals("discord.bot-tokens")) {
            return value == null || value.toString().isBlank() ? "<empty>" : "<hidden>";
        }
        if (value instanceof List<?> values) {
            return String.join(", ", values.stream().map(String::valueOf).toList());
        }
        return String.valueOf(value);
    }

    private static String displayList(List<String> values, boolean hideValues) {
        if (values.isEmpty()) {
            return "<empty>";
        }
        if (hideValues) {
            return values.size() + " hidden value(s)";
        }
        return String.join(", ", values);
    }

    private static String displaySingle(String value, boolean hideValue) {
        if (value.isBlank()) {
            return "<empty>";
        }
        return hideValue ? "<hidden>" : value;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
