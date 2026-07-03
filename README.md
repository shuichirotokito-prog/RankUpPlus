# RankUpPlus

Advanced rankup plugin for Paper, Spigot, and Purpur — built for Survival, SkyBlock, Factions, Prison, and similar servers.

Progress players through a configurable rank ladder with costs, requirements, item rewards, and console/player commands per rank; prestige resets with their own rewards; a personalized scoreboard, tab list rank-sorting, per-rank chat formatting, leaderboards, temporary boosters, an in-game admin rank editor, and both YAML and MySQL storage.

## Features

- **Rank ladder** — any number of ranks, each with its own cost, color, requirements, item rewards, and console/player commands run on reaching it.
- **Requirement types** — playtime, kills, deaths, XP level, blocks broken/placed, a Vault balance floor on top of rank cost, an arbitrary permission node, or any unqualified Bukkit statistic (e.g. `PLAYER_KILLS`, `JUMP`, `FISH_CAUGHT`).
- **Prestige** — configurable max prestige, cost multiplier per prestige, optional stat reset, and per-prestige-level rewards.
- **GUIs** — rankup menu with live requirement progress, a confirmation menu, an all-ranks overview, and an in-game admin rank editor (cost, color, prestige flag, and full requirements add/remove).
- **Scoreboard** — personalized sidebar (rank, prestige, balance, next-rank cost, stats, active booster, server online count), toggleable per player with `/rankup scoreboard`.
- **Tab list** — colored rank prefixes and rank-based sorting.
- **Chat formatting** — a default format plus per-rank overrides, safe against players injecting color codes or PlaceholderAPI placeholders through their own messages. Works on both Paper (modern Adventure-based chat) and Spigot (legacy chat event) automatically.
- **Leaderboards** — `/ranktop` for rank, prestige, playtime, kills, deaths, or blocks broken/placed.
- **Boosters** — admin-granted temporary cost discounts or reward multipliers that persist across relogs and expire on their own.
- **LuckPerms integration** — uses the real LuckPerms API (not console commands), works correctly for offline players too.
- **PlaceholderAPI** — provides 24 of its own placeholders, and any other installed plugin's placeholders also work inside `scoreboard.yml` and `chat.yml`.
- **Vault economy**, with an XP-level fallback mode if Vault isn't installed.
- **YAML or MySQL storage**, switchable in config.

## Supported platforms

| | |
|---|---|
| **Server software** | Paper, Spigot, Purpur |
| **Minecraft version** | 1.21+ |
| **Folia** | **Not supported yet.** The plugin detects Folia at startup and disables itself with a clear message rather than partially working or crashing unpredictably — its scheduling (playtime tracking, scoreboard refresh, booster expiry, leaderboard refresh, auto-rankup) uses Bukkit's global scheduler throughout, which Folia's regionized threading model doesn't allow. |

## Dependencies

| Plugin | Required? | What it's used for |
|---|---|---|
| [Vault](https://www.spigotmc.org/resources/vault.34315/) | Optional | Economy (money) costs. Without it, rankup costs are charged in XP levels instead. |
| [LuckPerms](https://luckperms.net/) | Optional | Automatic permission group sync on rankup/prestige. **This is the only permission plugin supported for automatic sync.** Other permission plugins (PermissionsEx, GroupManager, zPermissions, etc.) are not integrated — ranks will still track correctly in RankUpPlus, but their associated permission group won't be assigned automatically. You can still assign permissions manually per rank if you use a different permission plugin. |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Optional | Placeholders in scoreboard/chat/other plugins, and lets RankUpPlus consume other plugins' placeholders too. |

Nothing is required to run the plugin at all — everything above degrades gracefully if absent.

## Installation

1. Drop the jar into your server's `plugins/` folder.
2. Start the server once to generate the default config files.
3. Edit `plugins/RankUpPlus/ranks.yml` to set up your rank ladder (a sample 9-rank ladder ships by default).
4. Edit `config.yml`, `scoreboard.yml`, `tablist.yml`, and `chat.yml` as needed.
5. Run `/rankadmin reload` to apply changes without restarting.

## Configuration files

| File | Contains |
|---|---|
| `config.yml` | General settings, storage backend, leaderboard settings, GUI appearance, messages |
| `ranks.yml` | The rank ladder and prestige settings — heavily commented with every requirement type documented inline |
| `scoreboard.yml` | The personalized sidebar's content and appearance |
| `tablist.yml` | Tab list sorting and rank prefix format |
| `chat.yml` | Default and per-rank chat formats |

## Commands

| Command | Aliases | Description | Permission |
|---|---|---|---|
| `/rankup [scoreboard]` | `/ru` | Rank up, or toggle your personal scoreboard | `rankupplus.use` |
| `/rankupgui` | `/rug`, `/rankgui` | Open the rankup GUI | `rankupplus.use` |
| `/ranks` | | List all ranks and open the ranks overview GUI | `rankupplus.use` |
| `/ranktop [stat]` | `/rtop`, `/leaderboard` | View a leaderboard. `stat` is one of `rank`, `prestige`, `playtime`, `kills`, `deaths`, `blocksbroken`, `blocksplaced` (defaults to `rank`) | `rankupplus.use` |
| `/rankadmin set <player> <rank>` | `/radmin`, `/ra` | Set a player's rank | `rankupplus.admin` |
| `/rankadmin reset <player>` | | Reset a player's rank and prestige | `rankupplus.admin` |
| `/rankadmin give <player> <rank>` | | Give a rank's item rewards to an online player | `rankupplus.admin` |
| `/rankadmin info <player>` | | View a player's rank, prestige, and stats | `rankupplus.admin` |
| `/rankadmin reload` | | Reload all config files | `rankupplus.admin` |
| `/rankadmin prestige <player> set\|reset [level]` | | Manage a player's prestige level | `rankupplus.admin` |
| `/rankadmin booster <player> <cost\|rewards> <multiplier> <minutes>` | | Grant a temporary booster | `rankupplus.admin` |
| `/rankadmin edit <rank>` | | Open the in-game rank editor GUI | `rankupplus.admin` |

## Permissions

| Permission | Default | Description |
|---|---|---|
| `rankupplus.use` | `true` | Use the player-facing commands |
| `rankupplus.admin` | `op` | Use `/rankadmin` |
| `rankupplus.bypass.cost` | `op` | Skip rankup cost (also skips cooldown) |
| `rankupplus.bypass.cooldown` | `op` | Skip rankup cooldown specifically |
| `rankupplus.bypass.requirements` | `op` | Skip rank requirements |
| `rankupplus.notify.admin` | `op` | Get notified in chat when players rank up |

## Placeholders

Requires PlaceholderAPI. All placeholders are `%rankupplus_<name>%`.

| Placeholder | Returns |
|---|---|
| `rank` | Current rank's colored display name |
| `rank_id` | Current rank's raw id |
| `rank_display` | Current rank's display name, uncolored |
| `rank_color` | Current rank's color code (e.g. `&a`) |
| `rank_index` | Current rank's position in the ladder (0-based) |
| `total_ranks` | Total number of ranks configured |
| `next_rank` | Next rank's colored display name, or `MAX` |
| `next_rank_id` | Next rank's raw id, or `MAX` |
| `next_rank_cost` | Cost to reach the next rank (booster-adjusted) |
| `prestige` | Prestige level (integer) |
| `prestige_display` | Formatted prestige tag, empty if 0 |
| `kills` / `deaths` | Tracked player kills / deaths |
| `blocks_broken` / `blocks_placed` | Tracked totals since install |
| `balance` | Formatted economy balance (online players only) |
| `booster` | Human-readable active booster status |
| `booster_active` | `true`/`false` |
| `online` / `max_players` | Server online count / max players |
| `playtime` | Tracked playtime in minutes |
| `playtime_formatted` | Tracked playtime as `1h 23m`-style text |
| `is_max_rank` | `true`/`false` |
| `progress_percent` | Progress through the rank ladder, 0–100 |

Any other installed plugin's placeholders also resolve inside `scoreboard.yml` lines and `chat.yml` formats — you're not limited to RankUpPlus's own set there.

## Known limitations

- **Only LuckPerms gets automatic permission group sync.** Other permission plugins aren't integrated.
- **YAML storage's leaderboard refresh scans every player file** on each refresh cycle. Fine for small-to-mid servers; if you're running a large, long-established server with a very large accumulated player base, MySQL storage avoids this (a single query instead).
- **MySQL storage uses a single auto-reconnecting connection, not a connection pool.** Adequate for this plugin's actual traffic pattern (joins/quits/rankups/periodic saves), but worth knowing if you're running a very high-population server.
- **English only.** No built-in multi-language support yet.
- **One linear rank ladder and one prestige counter.** No support for multiple independent rank tracks.
- **Folia is not supported** (see above).

## Building from source

Requires JDK 21 and Gradle:

```
gradle clean build
```

The shaded jar is output to `output/RankUpPlus-<version>.jar`.

## Metrics

This plugin uses [bStats](https://bstats.org/) to collect anonymous usage statistics (server count, storage backend used, which integrations are active). This can be disabled server-wide via `plugins/bStats/config.yml`, independent of this plugin.

## License

[MIT](LICENSE) — free to use, modify, and redistribute, including commercially, as long as the copyright notice is kept.

## Links

*(Resource page / source repository links go here once published.)*
