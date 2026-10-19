package main;

import gameController.PlayerInput;
import object.AlienTeleport;
import object.AmmoDepot;
import object.DesertTurret;
import object.Ennemi;
import object.Ennemi.EnemyArchetype;
import object.Homme;
import object.ObjectManager;
import object.Protagonist;
import object.Soldat;
import world.TileManager;
import world.TileManager.MapType;

/**
 * Population initiale d'une partie et maintien du nombre d'aliens en jeu.
 */
final class WorldSpawner {
    private static final int ALIEN_RESPAWN_COOLDOWN_FRAMES = GamePanel.TARGET_UPS * 2;

    private final TileManager tileManager;
    private final int tileSize;
    private final GameSession session;
    private final SpawnLocator spawns;
    private int alienRespawnCooldownFrames = 0;

    WorldSpawner(TileManager tileManager, int tileSize, GameSession session, SpawnLocator spawns) {
        this.tileManager = tileManager;
        this.tileSize = tileSize;
        this.session = session;
        this.spawns = spawns;
    }

    void resetAlienRespawnCooldown() {
        alienRespawnCooldownFrames = 0;
    }

    // Civils puis protagoniste ; renvoie la position du protagoniste.
    double[] spawnCiviliansAndPlayer(PlayerInput playerInput) {
        GameSettings settings = session.getSettings();
        int civilsToSpawn = settings.getCivilianCount();
        if (session.isMode(GameMode.PROTECTION)) {
            civilsToSpawn = Math.max(4, settings.getCivilianCount());
        } else if (session.isMode(GameMode.ARCADE)) {
            civilsToSpawn = Math.max(1, Math.min(2, settings.getCivilianCount()));
        } else if (session.isMode(GameMode.MISSION)) {
            civilsToSpawn = Math.max(1, Math.min(2, settings.getCivilianCount()));
        }

        if (settings.isEasyDifficulty()) {
            civilsToSpawn = Math.max(civilsToSpawn, settings.getCivilianCount() + 1);
        }

        // Cree une petite population initiale.
        for (int i = 0; i < civilsToSpawn; i++) {
            double[] spawn = spawns.getFreeSpawnPosition();
            ObjectManager.list.add(new Homme(spawn[0], spawn[1]));
        }
        double[] protagonistSpawn = spawns.getFreeSpawnPositionInZone(5, Math.min(8, tileManager.getMapCols() - 2), 4, Math.min(7, tileManager.getMapRows() - 2));
        ObjectManager.list.add(new Protagonist(protagonistSpawn[0], protagonistSpawn[1], playerInput));
        return protagonistSpawn;
    }

    // Soldats allies, depots, objets propres a la carte puis hostiles loin du protagoniste.
    void spawnSquadAndHostiles(double protagonistSpawnX, double protagonistSpawnY) {
        GameSettings settings = session.getSettings();
        int soldatsToSpawn = settings.getSoldierCount();
        if (session.isMode(GameMode.PROTECTION) || session.isMode(GameMode.ARCADE) || session.isMode(GameMode.MISSION)) {
            soldatsToSpawn = Math.max(1, settings.getSoldierCount());
        }
        if (settings.isEasyDifficulty()) {
            soldatsToSpawn = Math.min(10, soldatsToSpawn + 1);
        }

        // Garde les soldats alliés près du protagoniste dans la même zone sécurisée.
        for (int i = 0; i < soldatsToSpawn; i++) {
            double[] soldatSpawn = spawns.getFreeSpawnPositionInZone(
                5,
                Math.min(8, tileManager.getMapCols() - 2),
                4,
                Math.min(7, tileManager.getMapRows() - 2)
            );
            ObjectManager.list.add(new Soldat(soldatSpawn[0], soldatSpawn[1]));
        }
        spawnAmmoDepots();
        spawnMapSpecificObjects();

        int nombreAlien = settings.getAlienCount();
        int aliensToSpawn = settings.getAdjustedAlienCount(session.isMode(GameMode.ARCADE) ? Math.max(2, nombreAlien - 1) : nombreAlien);
        for (int i = 0; i < aliensToSpawn; i++) {
            double[] spawn = spawns.getFreeSpawnPositionFarFrom(
                    protagonistSpawnX,
                    protagonistSpawnY,
                    SpawnLocator.SAFE_HOSTILE_SPAWN_DISTANCE
            );
            ObjectManager.list.add(new AlienTeleport(spawn[0], spawn[1]));
        }

        int nombreEnnemi = settings.getEnemyCount();
        int enemyCount = settings.getAdjustedEnemyCount(session.isMode(GameMode.ARCADE) ? Math.max(8, nombreEnnemi - 1) : nombreEnnemi);
        int[][] enemySpawnZones = spawns.buildEnemySpawnZones();
        for (int i = 0; i < enemyCount; i++) {
            int[] zone = enemySpawnZones[i % enemySpawnZones.length];
            double[] spawn = spawns.getFreeSpawnPositionInZoneFarFrom(
                    zone[0],
                    zone[1],
                    zone[2],
                    zone[3],
                    protagonistSpawnX,
                    protagonistSpawnY,
                    SpawnLocator.SAFE_HOSTILE_SPAWN_DISTANCE
            );
            ObjectManager.list.add(new Ennemi(spawn[0], spawn[1], chooseEnemyArchetype(i, enemyCount)));
        }
    }

    private void spawnAmmoDepots() {
        int maxCol = tileManager.getMapCols() - 2;
        int maxRow = tileManager.getMapRows() - 2;
        int centerCol = maxCol / 2;
        int centerRow = maxRow / 2;
        boolean desertTactical = tileManager.getCurrentMapType() == MapType.DESERT_TACTICAL;
        int[][] zones = {
                {4, Math.min(10, maxCol), 3, Math.min(9, maxRow)},
                {Math.max(2, centerCol - 3), Math.min(maxCol, centerCol + 3),
                        Math.max(2, centerRow - 3), Math.min(maxRow, centerRow + 3)},
                desertTactical
                        ? new int[]{Math.max(2, maxCol - 17), Math.max(2, maxCol - 11),
                                Math.max(2, maxRow - 9), maxRow}
                        : new int[]{Math.max(2, maxCol - 9), maxCol, Math.max(2, maxRow - 9), maxRow}
        };
        for (int[] zone : zones) {
            double[] position = spawns.getFreeSpawnPositionInZone(zone[0], zone[1], zone[2], zone[3]);
            ObjectManager.list.add(new AmmoDepot(position[0], position[1]));
        }

        if (session.getSettings().isEasyDifficulty()) {
            double[] bonusDepot = spawns.getFreeSpawnPositionInZone(
                    Math.max(2, centerCol - 5),
                    Math.min(maxCol, centerCol + 5),
                    Math.max(2, centerRow - 5),
                    Math.min(maxRow, centerRow + 5)
            );
            ObjectManager.list.add(new AmmoDepot(bonusDepot[0], bonusDepot[1]));
        }
    }

    private void spawnMapSpecificObjects() {
        if (tileManager.getCurrentMapType() == MapType.DESERT_TACTICAL) {
            ObjectManager.list.add(new DesertTurret(43.5 * tileSize, 31.5 * tileSize));
        }
    }

    private EnemyArchetype chooseEnemyArchetype(int index, int total) {
        if (session.getSettings().isEasyDifficulty()) {
            double ratio = total > 1 ? index / (double) (total - 1) : 0.0;
            if (ratio < 0.72) {
                return EnemyArchetype.STANDARD;
            }
            if (ratio < 0.94) {
                return EnemyArchetype.FLANQUEUR;
            }
            return EnemyArchetype.ASSAUT;
        }

        if (total <= 2) {
            return EnemyArchetype.STANDARD;
        }

        double ratio = total > 1 ? index / (double) (total - 1) : 0.0;
        if (ratio < 0.58) {
            return EnemyArchetype.STANDARD;
        }
        if (ratio < 0.85) {
            return EnemyArchetype.FLANQUEUR;
        }
        if (ratio < 0.94) {
            return EnemyArchetype.ASSAUT;
        }
        if (ratio < 0.98) {
            return EnemyArchetype.ROQUETTE;
        }
        return EnemyArchetype.LOURD;
    }

    // Fait reapparaitre un alien loin du protagoniste quand la population descend sous la cible.
    void maintainAlienPresence() {
        if (alienRespawnCooldownFrames > 0) {
            alienRespawnCooldownFrames--;
            return;
        }

        int aliveAliens = ObjectManager.getUiCounts()[2];
        int nombreAlien = session.getSettings().getAlienCount();
        int baseDesiredAliens = session.isMode(GameMode.ARCADE) ? Math.max(2, nombreAlien) : Math.max(1, nombreAlien);
        int desiredAliens = session.getSettings().getAdjustedAlienCount(baseDesiredAliens);
        if (aliveAliens >= desiredAliens) {
            return;
        }

        Protagonist protagonist = ObjectManager.getProtagonist();
        if (protagonist == null) {
            return;
        }

        double[] spawn = spawns.getFreeSpawnPositionFarFrom(
                protagonist.x,
                protagonist.y,
                SpawnLocator.SAFE_HOSTILE_SPAWN_DISTANCE * 0.72
        );
        ObjectManager.list.add(new AlienTeleport(spawn[0], spawn[1]));
        alienRespawnCooldownFrames = ALIEN_RESPAWN_COOLDOWN_FRAMES;
    }
}
