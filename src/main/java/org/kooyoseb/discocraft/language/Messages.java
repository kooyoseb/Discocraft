package org.kooyoseb.discocraft.language;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.ChatColor;
import org.kooyoseb.discocraft.Discocraft;

public final class Messages {
    private final Discocraft plugin;
    private String language;

    public Messages(Discocraft plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        language = plugin.getConfig().getString("language", "ko_kr").toLowerCase(Locale.ROOT);
        if (!language.equals("ko_kr") && !language.equals("en_us")) {
            language = "ko_kr";
        }
    }

    public String currentLanguage() {
        return language;
    }

    public String tr(String key, Object... replacements) {
        String message = messages(language).getOrDefault(key, messages("en_us").getOrDefault(key, key));
        for (int index = 0; index + 1 < replacements.length; index += 2) {
            message = message.replace("%" + replacements[index] + "%", String.valueOf(replacements[index + 1]));
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static boolean isSupported(String language) {
        String normalized = language.toLowerCase(Locale.ROOT);
        return normalized.equals("ko_kr") || normalized.equals("en_us");
    }

    private static Map<String, String> messages(String language) {
        return switch (language) {
            case "en_us" -> enUs();
            default -> koKr();
        };
    }

    private static Map<String, String> koKr() {
        Map<String, String> messages = new HashMap<>();
        messages.put("language.current", "&bDiscocraft&7: &f현재 언어는 &e%language%&f 입니다.");
        messages.put("language.changed", "&bDiscocraft&7: &f언어를 &e%language%&f(으)로 변경했습니다.");
        messages.put("language.unsupported", "&c지원하지 않는 언어입니다. 사용 가능: ko_kr, en_us");
        messages.put("link.only-player", "&c이 명령어는 플레이어만 사용할 수 있습니다.");
        messages.put("link.status.linked", "&bDiscocraft&7: &e%discord%&f 계정과 연동되어 있습니다.");
        messages.put("link.status.unlinked", "&bDiscocraft&7: &f아직 연동되지 않았습니다. &e/%label% link&f 를 사용하세요.");
        messages.put("link.code", "&bDiscocraft&7: &f디스코드 연동 코드: &e%code%");
        messages.put("link.code-help", "&7연동된 디스코드 채널에서 &f!link %code% &7를 10분 안에 입력하세요.");
        messages.put("link.unlinked", "&bDiscocraft&7: &f디스코드 계정 연동을 해제했습니다.");
        messages.put("link.not-linked", "&bDiscocraft&7: &f연동된 디스코드 계정이 없습니다.");
        messages.put("link.usage", "&c사용법: /%label% <link|unlink|status>");
        return messages;
    }

    private static Map<String, String> enUs() {
        Map<String, String> messages = new HashMap<>();
        messages.put("language.current", "&bDiscocraft&7: &fCurrent language is &e%language%&f.");
        messages.put("language.changed", "&bDiscocraft&7: &fLanguage changed to &e%language%&f.");
        messages.put("language.unsupported", "&cUnsupported language. Available: ko_kr, en_us");
        messages.put("link.only-player", "&cOnly players can use this command.");
        messages.put("link.status.linked", "&bDiscocraft&7: &flinked to &e%discord%&f.");
        messages.put("link.status.unlinked", "&bDiscocraft&7: &fnot linked. Use &e/%label% link&f.");
        messages.put("link.code", "&bDiscocraft&7: &fDiscord link code: &e%code%");
        messages.put("link.code-help", "&7Type &f!link %code% &7in the configured Discord channel within 10 minutes.");
        messages.put("link.unlinked", "&bDiscocraft&7: &fyour Discord account was unlinked.");
        messages.put("link.not-linked", "&bDiscocraft&7: &fyou are not linked.");
        messages.put("link.usage", "&cUsage: /%label% <link|unlink|status>");
        return messages;
    }
}
