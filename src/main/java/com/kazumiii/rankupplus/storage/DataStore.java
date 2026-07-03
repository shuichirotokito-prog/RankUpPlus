package com.kazumiii.rankupplus.storage;

import com.kazumiii.rankupplus.models.PlayerData;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Abstraction over where player data is persisted (YAML files or a MySQL database).
 * All methods that touch disk/network are designed to be safe to call from any thread;
 * callers that need the result on the main server thread must hop back via the
 * Bukkit scheduler themselves (see PlayerDataManager for the join-flow example).
 */
public interface DataStore {

    /** Loads one player's data synchronously. Safe to call off the main thread. */
    PlayerData load(UUID uuid, String name);

    /** Loads one player's data asynchronously. */
    CompletableFuture<PlayerData> loadAsync(UUID uuid, String name);

    /** Saves one player's data synchronously. */
    void save(PlayerData data);

    /** Saves one player's data asynchronously (fire-and-forget). */
    void saveAsync(PlayerData data);

    /** Saves several players' data synchronously (blocking) — used on shutdown. */
    void saveAllSync(List<PlayerData> data);

    /** Loads every known player's data — used for leaderboards. May be slow; call async. */
    List<PlayerData> loadAll();

    /** Finds a player's data by their last-known stored name (case-insensitive). */
    PlayerData findByName(String name);

    /** Called once on plugin enable, after config is loaded. */
    void init();

    /** Called once on plugin disable to release any held resources (DB connections, etc). */
    void close();
}
