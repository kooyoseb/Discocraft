# Discocraft

Minecraft and Discord bridge. One plugin to end it.

Discocraft links a Paper Minecraft server with Discord chat, server events, account verification, and controlled Discord-side console commands.

## Features

- Minecraft chat to Discord
- Discord chat to Minecraft with a visible configurable label
- Join, quit, death, server start, and server stop event forwarding
- Custom event selection with `alls`, `none`, `chat`, `join`, `quit`, `death`, `server-start`, `server-stop`
- Multiple Discord bot tokens
- Multiple Discord channel IDs
- Minecraft account to Discord account linking
- Discord-side Minecraft console command execution with allowlists
- Runtime config editing with commands
- Korean and English language setting

## Requirements

- Paper server matching the plugin API version
- Java 25 or compatible server runtime
- A Discord bot token
- Discord bot `MESSAGE CONTENT INTENT` enabled in the Discord Developer Portal

## Installation

1. Build the plugin with `build.bat` or `.\gradlew.bat build`.
2. Copy `build/libs/Discocraft-1.0-SNAPSHOT.jar` into your server `plugins` folder.
3. Start the server once to generate the plugin folder and config.
4. Configure Discord settings in `plugins/Discocraft/config.yml` or with commands.
5. Run `/discocraft reload`.

## Basic Setup

```yaml
language: ko_kr

discord:
  enabled: true
  bot-tokens:
    - "YOUR_BOT_TOKEN"
  channel-ids:
    - "YOUR_CHANNEL_ID"
```

## Main Commands

```text
/discocraft status
/discocraft reload
/discocraft reset
/discocraft reset <key>
/discocraft get <key>
/discocraft set <key> <value>
/discocraft events <alls|none|chat|join|quit|death|server-start|server-stop>
```

## Bot And Channel Commands

```text
/discocraft bot list
/discocraft bot add <token>
/discocraft bot remove <token>
/discocraft bot clear

/discocraft channel list
/discocraft channel add <channel-id>
/discocraft channel remove <channel-id>
/discocraft channel clear
```

Bot tokens are hidden in command output.

## Account Linking

In Minecraft:

```text
/discord link
```

Then in Discord:

```text
!link <code>
```

Other commands:

```text
/discord status
/discord unlink
!unlink
```

Linked account data is stored separately in `linked-accounts.yml`.

## Discord Console Commands

Discord console execution is disabled by default.

```yaml
discord:
  console:
    enabled: false
    command-prefix: "!mc"
    allowed-prefixes:
      - "list"
      - "say"
      - "time"
      - "weather"
    allowed-role-ids: []
```

Example:

```text
!mc list
!mc say hello
!mc time set day
```

For safety, only commands matching `allowed-prefixes` can run. If `allowed-role-ids` is empty, any Discord user in a configured bridge channel can use allowed console commands while the feature is enabled.

## Language

```text
/discocraft language ko_kr
/discocraft language en_us
```

Supported languages:

- `ko_kr`
- `en_us`

## Build

Windows:

```bat
build.bat
```

Manual:

```text
.\gradlew.bat clean build
```

Output:

```text
build/libs/Discocraft-1.0-SNAPSHOT.jar
```

## Upload Assets

Platform upload icon:

```text
assets/discocraft-icon.png
```

Source vector:

```text
assets/discocraft-icon.svg
```
