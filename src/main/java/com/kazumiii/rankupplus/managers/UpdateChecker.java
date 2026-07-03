package com.kazumiii.rankupplus.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.kazumiii.rankupplus.RankUpPlus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Checks SpigotMC and/or Modrinth for a newer published version than the one
 * currently running, and logs a console notice if one is found. Never blocks
 * the main thread, never throws past its own boundary (a failed check is
 * silently skipped — this is a courtesy notice, not something that should
 * ever affect the plugin's own startup or operation).
 *
 * SPIGOT_RESOURCE_ID and MODRINTH_PROJECT_SLUG are hardcoded constants, not
 * config.yml options — deliberately, and consistent with how
 * MetricsManager.PLUGIN_ID is also a hardcoded constant rather than a config
 * value. These identify WHICH plugin/resource to check against, which is a
 * property of this specific build, the same for every server that installs
 * it — not something an individual server owner should ever need to fill in
 * themselves. Only whether the check runs at all (settings.update-checker.enabled
 * in config.yml) is a legitimate per-server preference.
 */
public class UpdateChecker {

    // TODO: replace with your real Spigot resource id once published
    // (from your resource's URL, e.g. .../resources/rankupplus.12345 -> 12345).
    // Leave at 0 to skip the Spigot check entirely.
    private static final int SPIGOT_RESOURCE_ID = 0;

    // TODO: replace with your real Modrinth project slug once published
    // (from your project's URL, e.g. modrinth.com/plugin/rankupplus -> "rankupplus").
    // Leave blank to skip the Modrinth check entirely.
    private static final String MODRINTH_PROJECT_SLUG = "";

    private final RankUpPlus plugin;
    private final HttpClient httpClient;

    public UpdateChecker(RankUpPlus plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    /** Kicks off both checks (whichever are configured) asynchronously. */
    public void checkAsync() {
        if (!plugin.getConfigManager().isUpdateCheckerEnabled()) return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String currentVersion = plugin.getDescription().getVersion();

            if (SPIGOT_RESOURCE_ID > 0) {
                checkSpigot(SPIGOT_RESOURCE_ID, currentVersion);
            }

            if (MODRINTH_PROJECT_SLUG != null && !MODRINTH_PROJECT_SLUG.isBlank()) {
                checkModrinth(MODRINTH_PROJECT_SLUG, currentVersion);
            }
        });
    }

    private void checkSpigot(int resourceId, String currentVersion) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return;

            String latest = response.body().trim();
            // The Spigot API returns a bare error-ish body for an unpublished/unknown
            // resource id rather than a clean HTTP error — a sanity check on shape
            // avoids ever comparing garbage against the real version string.
            if (latest.isEmpty() || latest.length() > 32) return;

            if (!latest.equalsIgnoreCase(currentVersion)) {
                announce("SpigotMC", latest, currentVersion,
                    "https://www.spigotmc.org/resources/" + resourceId);
            }
        } catch (Exception e) {
            plugin.getLogger().fine("Spigot update check failed: " + e.getMessage());
        }
    }

    private void checkModrinth(String slug, String currentVersion) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.modrinth.com/v2/project/" + slug + "/version"))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "Kazumiii/RankUpPlus/" + currentVersion + " (update-checker)")
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return;

            JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();
            if (versions.isEmpty()) return;

            // Modrinth returns versions newest-first by default.
            String latest = versions.get(0).getAsJsonObject().get("version_number").getAsString();
            if (!latest.equalsIgnoreCase(currentVersion)) {
                announce("Modrinth", latest, currentVersion,
                    "https://modrinth.com/plugin/" + slug + "/versions");
            }
        } catch (Exception e) {
            plugin.getLogger().fine("Modrinth update check failed: " + e.getMessage());
        }
    }

    private void announce(String source, String latest, String current, String url) {
        plugin.getLogger().warning("========================================");
        plugin.getLogger().warning("A new version of RankUpPlus is available on " + source + "!");
        plugin.getLogger().warning("Current version: " + current + "  |  Latest version: " + latest);
        plugin.getLogger().warning("Download: " + url);
        plugin.getLogger().warning("========================================");
    }
}
