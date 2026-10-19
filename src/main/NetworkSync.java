package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import network.NetworkReplica;
import network.WorldSnapshot;
import object.GameObject;
import object.NetworkVisualState;
import object.ObjectManager;
import object.Protagonist;
import object.ai.AiTuning;
import world.TileManager;
import world.TileManager.MapType;

/**
 * Synchronisation LAN de l'etat du monde.
 * Cote hote : construit l'instantane envoye au client (identifiants stables par objet).
 * Cote client : applique l'instantane recu (reglages, tuiles, sons) et tient a jour les NetworkReplica.
 */
final class NetworkSync {
    private final TileManager tileManager;
    private final GameSession session;
    private final long[] networkSoundCounters = new long[Utils.SOUND_COUNT];
    private boolean networkSoundCountersInitialized;
    private final Map<Long, NetworkReplica> networkReplicas = new LinkedHashMap<>();
    private final IdentityHashMap<GameObject, Long> networkObjectIds = new IdentityHashMap<>();
    private long nextNetworkObjectId = 1;

    NetworkSync(TileManager tileManager, GameSession session) {
        this.tileManager = tileManager;
        this.session = session;
    }

    // Appele par le thread de jeu entre deux pas de simulation (voir LanHost.publishSnapshot).
    WorldSnapshot createSnapshot(ScreenState screenState, Protagonist remote) {
        GameSettings settings = session.getSettings();
        WorldSnapshot snapshot = new WorldSnapshot();
        snapshot.screenState = screenState.ordinal();
        snapshot.mapType = tileManager.getCurrentMapType().ordinal();
        snapshot.gameMode = session.getMode().ordinal();
        snapshot.difficulty = settings.getDifficulty().ordinal();
        snapshot.civilianCount = settings.getCivilianCount();
        snapshot.enemyCount = settings.getEnemyCount();
        snapshot.alienCount = settings.getAlienCount();
        snapshot.soldierCount = settings.getSoldierCount();
        snapshot.enemyReactionFrames = AiTuning.getEnemyReactionFrames();
        snapshot.soldierReactionFrames = AiTuning.getSoldierReactionFrames();
        snapshot.enemyAimFrames = AiTuning.getEnemyAimStabilizationFrames();
        snapshot.soldierAimFrames = AiTuning.getSoldierAimStabilizationFrames();
        snapshot.suppressionFrames = AiTuning.getSuppressionDurationFrames();
        snapshot.suppressionCoverBoost = AiTuning.getSuppressionCoverBoost();
        snapshot.suppressionRadius = AiTuning.getSuppressionNearMissRadius();
        snapshot.alienPackAggro = AiTuning.isAlienPackAggroEnabled();
        snapshot.unlimitedAmmo = settings.isUnlimitedAmmoEnabled();
        snapshot.fireCameraFeedback = settings.isFireCameraFeedbackEnabled();
        snapshot.deathMarkers = settings.areDeathMarkersEnabled();
        snapshot.score = session.getScore();
        snapshot.soundCounters = Utils.getSoundCounters();
        for (int[] tile : tileManager.getNetworkTileMutations()) {
            WorldSnapshot.TileMutation mutation = new WorldSnapshot.TileMutation();
            mutation.row = tile[0];
            mutation.col = tile[1];
            mutation.tileId = tile[2];
            mutation.damage = tile[3];
            snapshot.tileMutations.add(mutation);
        }
        if (remote != null) {
            snapshot.playerX = remote.x;
            snapshot.playerY = remote.y;
            snapshot.playerFacingX = remote.getFacingX();
            snapshot.playerFacingY = remote.getFacingY();
            snapshot.armor = remote.getArmorPlates();
            snapshot.maxArmor = remote.getMaxArmorPlates();
            snapshot.reloading = remote.isReloading();
            snapshot.reloadProgress = remote.getReloadProgress();
            if (remote.getCurrentWeapon() != null) {
                snapshot.weaponName = remote.getCurrentWeapon().getName();
                snapshot.ammo = remote.getCurrentWeapon().getAmmoInMagazine();
                snapshot.reserveAmmo = remote.getCurrentWeapon().getReserveAmmo();
            }
        }
        ArrayList<GameObject> currentObjects = ObjectManager.getObjectSnapshot();
        Set<GameObject> currentObjectSet = Collections.newSetFromMap(new IdentityHashMap<>());
        currentObjectSet.addAll(currentObjects);
        networkObjectIds.keySet().removeIf(object -> !currentObjectSet.contains(object));
        for (GameObject object : currentObjects) {
            WorldSnapshot.Entity entity = toNetworkEntity(object, remote);
            if (entity != null) snapshot.entities.add(entity);
        }
        return snapshot;
    }

    // Chaque GameObject decrit lui-meme son type reseau (GameObject.describeForNetwork).
    private WorldSnapshot.Entity toNetworkEntity(GameObject object, Protagonist remote) {
        WorldSnapshot.Entity entity = new WorldSnapshot.Entity();
        entity.id = networkObjectIds.computeIfAbsent(object, ignored -> nextNetworkObjectId++);
        entity.x = object.x;
        entity.y = object.y;
        entity.vx = object.vx;
        entity.vy = object.vy;
        if (object instanceof NetworkVisualState visual) {
            entity.visualState = visual.getNetworkVisualState();
        }
        return object.describeForNetwork(entity, remote) ? entity : null;
    }

    // Applique l'instantane recu par le client ; l'ecran courant et la camera restent geres par GamePanel.
    void applySnapshot(WorldSnapshot snapshot) {
        applyNetworkSounds(snapshot);
        MapType[] maps = MapType.values();
        if (snapshot.mapType >= 0 && snapshot.mapType < maps.length) {
            tileManager.setCurrentMapType(maps[snapshot.mapType]);
        }
        GameMode[] modes = GameMode.values();
        if (snapshot.gameMode >= 0 && snapshot.gameMode < modes.length) {
            session.setMode(modes[snapshot.gameMode]);
        }
        GameSettings settings = session.getSettings();
        DifficultyProfile[] difficulties = DifficultyProfile.values();
        if (snapshot.difficulty >= 0 && snapshot.difficulty < difficulties.length) {
            settings.setDifficulty(difficulties[snapshot.difficulty]);
        }
        settings.setPopulation(snapshot.civilianCount, snapshot.enemyCount, snapshot.alienCount, snapshot.soldierCount);
        AiTuning.applyNetworkSettings(
                snapshot.enemyReactionFrames,
                snapshot.soldierReactionFrames,
                snapshot.enemyAimFrames,
                snapshot.soldierAimFrames,
                snapshot.suppressionFrames,
                snapshot.suppressionCoverBoost,
                snapshot.suppressionRadius,
                snapshot.alienPackAggro
        );
        settings.setGameOptions(snapshot.unlimitedAmmo, snapshot.fireCameraFeedback, snapshot.deathMarkers);
        session.setScore(snapshot.score);
        for (WorldSnapshot.TileMutation mutation : snapshot.tileMutations) {
            tileManager.applyNetworkTileMutation(
                    mutation.row, mutation.col, mutation.tileId, mutation.damage);
        }
        applyReplicas(snapshot);
    }

    // Cree, met a jour ou retire les NetworkReplica pour refleter les entites de l'instantane.
    private void applyReplicas(WorldSnapshot snapshot) {
        Set<Long> presentIds = new HashSet<>();
        for (WorldSnapshot.Entity entity : snapshot.entities) {
            presentIds.add(entity.id);
            NetworkReplica replica = networkReplicas.get(entity.id);
            if (replica == null || replica.getNetworkType() != entity.type) {
                if (replica != null) ObjectManager.list.remove(replica);
                replica = new NetworkReplica(entity);
                networkReplicas.put(entity.id, replica);
                ObjectManager.list.add(replica);
            } else {
                replica.apply(entity);
            }
        }
        networkReplicas.entrySet().removeIf(entry -> {
            if (presentIds.contains(entry.getKey())) return false;
            ObjectManager.list.remove(entry.getValue());
            return true;
        });
    }

    private void applyNetworkSounds(WorldSnapshot snapshot) {
        if (!networkSoundCountersInitialized) {
            System.arraycopy(snapshot.soundCounters, 0, networkSoundCounters, 0, Utils.SOUND_COUNT);
            networkSoundCountersInitialized = true;
            return;
        }
        for (int sound = 0; sound < Utils.SOUND_COUNT; sound++) {
            long delta = snapshot.soundCounters[sound] - networkSoundCounters[sound];
            for (int repeat = 0; repeat < Math.min(3L, Math.max(0L, delta)); repeat++) {
                Utils.playReplicatedSound(sound);
            }
            networkSoundCounters[sound] = snapshot.soundCounters[sound];
        }
    }
}
