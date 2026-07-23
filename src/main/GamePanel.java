package main;

import javax.swing.*;
import java.awt.*;
import java.awt.KeyboardFocusManager;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import gameController.GameKeyController;
import gameController.RemotePlayerInput;
import network.LanClientViewer;
import network.LanHost;
import network.NetworkReplica;
import network.WorldSnapshot;
import object.GameObject;
import object.ai.AiTuning;
import object.ai.TacticalMovement;
import object.Alien;
import object.AlienTeleport;
import object.AmmoDepot;
import object.DesertTurret;
import object.DeathMarker;
import object.Ennemi;
import object.Ennemi.EnemyArchetype;
import object.Homme;
import object.Hostage;
import object.ImpactSpark;
import object.Projectile;
import object.Protagonist;
import object.DroppedAmmo;
import object.DroppedArmor;
import object.DroppedWeapon;
import object.RocketBlastCloud;
import object.TeslaArc;
import object.Douille;
import object.Shockwave;
import object.Soldat;
import object.ObjectManager;
import world.TileManager;
import world.TileManager.MapType;

public class GamePanel extends JPanel implements Runnable {

    private static final int TARGET_UPS = 60;
    private static final long NANOS_PER_UPDATE = 1_000_000_000L / TARGET_UPS;
    private static final int ARCADE_BASE_WAVE_INTERVAL_FRAMES = TARGET_UPS * 14;
    private static final int ARCADE_MIN_WAVE_INTERVAL_FRAMES = TARGET_UPS * 7;
    private static final int ARCADE_WAVE_BANNER_FRAMES = TARGET_UPS * 2;
    private static final int MISSION_DETONATION_FUSE_FRAMES = TARGET_UPS * 3;
    private static final int MISSION_DEFEND_HOLD_FRAMES = TARGET_UPS * 35;
    private static final int MISSION_SECURE_HOLD_FRAMES = TARGET_UPS * 8;
    private static final double MISSION_INTERACT_DISTANCE = 84.0;
    private static final double MISSION_HOSTAGE_FREE_DISTANCE = 90.0;
    private static final double MISSION_INFILTRATION_DETECT_DISTANCE = 260.0;

    private enum MissionType {
        RESCUE_HOSTAGES,
        DEFEND_POSITION,
        SECURE_ZONE,
        INFILTRATE,
        DESTROY_VEHICLE
    }

    private enum ScreenState {
        MENU,
        OPTIONS,
        PLAYING,
        GAME_OVER,
        VICTORY
    }

    private enum DifficultyProfile {
        EASY("Facile"),
        NORMAL("Normal"),
        CUSTOM("Personnaliser");

        private final String label;

        DifficultyProfile(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private int nombreCivil = 5;
    private int nombreEnnemi = 10;
    private int nombreAlien = 5;
    private int nombreSoldat = 1;

    // 50 correspond aux dimensions de la carte (800÷16=50, 600÷12=50), ce qui permet un affichage correct des tiles sur l'écran 800×600.
    public int tileSize = 50;
    private static final double ENTITY_RADIUS = 14;
    private static final int MAX_SPAWN_ATTEMPTS = 300;
    private static final double SAFE_HOSTILE_SPAWN_DISTANCE = 520;
    private static final int ALIEN_RESPAWN_COOLDOWN_FRAMES = TARGET_UPS * 2;
    private static final int AI_OPTIONS_BASE_Y = 190;
    private static final int AI_OPTION_ROW_GAP = 26;
    private static final int AI_OPTION_COUNT = 12;
    private static final int MENU_MAP_CARD_GAP = 26;
    private static final int DIFFICULTY_BUTTON_GAP = 14;
    private static final float NIGHT_DARKNESS_ALPHA = 0.86f;
    private static final double PLAYER_FLASHLIGHT_RANGE = 320;
    private static final double PLAYER_FLASHLIGHT_HALF_ANGLE = Math.toRadians(35);
    private static final double ENEMY_FLASHLIGHT_RANGE = 230;
    private static final double ENEMY_FLASHLIGHT_HALF_ANGLE = Math.toRadians(25);
    private static final double EXTRACTION_MARGIN_TILES = 1.5;
    private static final double FLASHLIGHT_RAY_STEP = 8.0;
    private static final int HUD_PANEL_ARC = 22;
    private static GamePanel activePanel;

    TileManager tileManager = new TileManager(this);
    private final GameKeyController keyController = new GameKeyController();
    private volatile RemotePlayerInput remotePlayerInput;
    private volatile Protagonist remoteProtagonist;
    private volatile LanHost lanHost;
    private final ThreadLocal<Protagonist> renderedPlayerOverride = new ThreadLocal<>();
    private volatile WorldSnapshot networkSnapshot;
    private volatile boolean networkReplicaView;
    private volatile String networkConnectionStatus = "Connexion a l'hote...";
    private final long[] networkSoundCounters = new long[Utils.SOUND_COUNT];
    private boolean networkSoundCountersInitialized;
    private final Map<Long, NetworkReplica> networkReplicas = new LinkedHashMap<>();
    private final IdentityHashMap<GameObject, Long> networkObjectIds = new IdentityHashMap<>();
    private long nextNetworkObjectId = 1;
    private ScreenState screenState = ScreenState.MENU;
    private boolean paused = false;
    private String gameOverTitle = "VOUS ETES MORT";
    private int arcadeWaveIndex = 0;
    private int arcadeWaveTimer = ARCADE_BASE_WAVE_INTERVAL_FRAMES;
    private int arcadeWaveBannerTimer = 0;
    private int screenShakeFrames = 0;
    private double screenShakeAmplitude = 0.0;
    private int screenFlashFrames = 0;
    private Color screenFlashColor = new Color(255, 236, 160);
    private float screenFlashAlpha = 0f;
    private int alienRespawnCooldownFrames = 0;
    private static boolean unlimitedAmmoEnabled = false;
    private static boolean fireCameraFeedbackEnabled = true;
    private static boolean deathMarkersEnabled = true;
    private DifficultyProfile currentDifficulty = DifficultyProfile.NORMAL;
    private int missionRotationIndex = 0;
    private MissionType activeMissionType;
    private String activeMissionTitle = "";
    private Rectangle2D.Double missionPrimaryZone;
    private Rectangle2D.Double missionSecondaryZone;
    private final ArrayList<Hostage> missionHostages = new ArrayList<>();
    private final ArrayList<Hostage> missionFreedHostages = new ArrayList<>();
    private final ArrayList<Hostage> missionExtractedHostages = new ArrayList<>();
    private int missionDefendFramesRemaining = 0;
    private int missionSecureFrames = 0;
    private double missionVehicleX = -1;
    private double missionVehicleY = -1;
    private boolean missionVehicleChargeArmed = false;
    private int missionVehicleFuseFrames = 0;
    private boolean missionFailed = false;

    public static int score = 0;

    public GamePanel() {
        activePanel = this;
        ObjectManager.setTileManager(tileManager);
        addMouseMotionListener(keyController);
        addMouseListener(keyController);
        addMouseWheelListener(keyController);

        initializeWorld();
        updateCamera();
    }

    public void startLanHost(int port) {
        if (lanHost != null) return;
        lanHost = new LanHost(this, port);
        lanHost.start();
    }

    public GameKeyController getKeyController() {
        return keyController;
    }

    public void configureNetworkReplicaView() {
        networkReplicaView = true;
        screenState = ScreenState.MENU;
        ObjectManager.list.clear();
    }

    public int getNetworkMapType() {
        return tileManager.getCurrentMapType().ordinal();
    }

    public int getNetworkGameMode() {
        return GameMode.current.ordinal();
    }

    public void configureNetworkWorld(int mapType, int gameMode) {
        MapType[] maps = MapType.values();
        if (mapType >= 0 && mapType < maps.length) tileManager.setCurrentMapType(maps[mapType]);
        GameMode[] modes = GameMode.values();
        if (gameMode >= 0 && gameMode < modes.length) GameMode.current = modes[gameMode];
    }

    public void setNetworkConnectionStatus(String status) {
        networkConnectionStatus = status == null ? "" : status;
        repaint();
    }

    public void showNetworkConnectionError(String status) {
        networkSnapshot = null;
        setNetworkConnectionStatus(status);
    }

    public void applyNetworkSnapshot(WorldSnapshot snapshot) {
        if (snapshot == null) return;
        applyNetworkSounds(snapshot);
        networkSnapshot = snapshot;
        ScreenState[] states = ScreenState.values();
        if (snapshot.screenState >= 0 && snapshot.screenState < states.length) {
            screenState = states[snapshot.screenState];
        }
        score = snapshot.score;
        for (WorldSnapshot.TileMutation mutation : snapshot.tileMutations) {
            tileManager.applyNetworkTileMutation(
                    mutation.row, mutation.col, mutation.tileId, mutation.damage);
        }
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
        tileManager.centerCameraOn(snapshot.playerX, snapshot.playerY, getViewWidth(), getViewHeight());
        repaint();
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

    public WorldSnapshot createSnapshotForRemote() {
        WorldSnapshot snapshot = new WorldSnapshot();
        snapshot.screenState = screenState.ordinal();
        snapshot.score = score;
        snapshot.soundCounters = Utils.getSoundCounters();
        for (int[] tile : tileManager.getNetworkTileMutations()) {
            WorldSnapshot.TileMutation mutation = new WorldSnapshot.TileMutation();
            mutation.row = tile[0];
            mutation.col = tile[1];
            mutation.tileId = tile[2];
            mutation.damage = tile[3];
            snapshot.tileMutations.add(mutation);
        }
        Protagonist remote = remoteProtagonist;
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

    private WorldSnapshot.Entity toNetworkEntity(GameObject object, Protagonist remote) {
        WorldSnapshot.Entity entity = new WorldSnapshot.Entity();
        entity.id = networkObjectIds.computeIfAbsent(object, ignored -> nextNetworkObjectId++);
        entity.x = object.x;
        entity.y = object.y;
        entity.vx = object.vx;
        entity.vy = object.vy;
        if (object instanceof Protagonist player) {
            entity.type = NetworkReplica.PLAYER;
            entity.facingX = player.getFacingX();
            entity.facingY = player.getFacingY();
            entity.localPlayer = player == remote;
            if (player.getCurrentWeapon() != null) entity.detail = player.getCurrentWeapon().getName();
        } else if (object instanceof Soldat soldier) {
            entity.type = NetworkReplica.SOLDIER;
            entity.detail = soldier.getCarriedWeapon().getName();
        } else if (object instanceof Ennemi enemy) {
            entity.type = NetworkReplica.ENEMY;
            entity.variant = (byte) (enemy.getArchetype().ordinal() + 1);
            entity.facingX = enemy.getFacingX();
            entity.facingY = enemy.getFacingY();
            entity.detail = enemy.getCarriedWeapon().getName();
        } else if (object instanceof Alien) {
            entity.type = NetworkReplica.ALIEN;
        } else if (object instanceof Homme) {
            entity.type = NetworkReplica.CIVILIAN;
        } else if (object instanceof Projectile projectile) {
            entity.type = NetworkReplica.PROJECTILE;
            entity.variant = (byte) (projectile.getProjectileType().ordinal() + 1);
        } else if (object instanceof AmmoDepot) {
            entity.type = NetworkReplica.AMMO_DEPOT;
            entity.detail = "DEPOT MUNITIONS";
        } else if (object instanceof DeathMarker marker) {
            entity.type = NetworkReplica.DEATH_MARKER;
            entity.variant = (byte) marker.getNetworkVariant();
            if (entity.variant == 1 && remote != null) {
                double dx = remote.x - marker.x;
                double dy = remote.y - marker.y;
                entity.amount = dx * dx + dy * dy <= 68.0 * 68.0 ? 1 : 0;
            }
        } else if (object instanceof DesertTurret turret) {
            entity.type = NetworkReplica.TURRET;
            entity.facingX = turret.getFacingX();
            entity.facingY = turret.getFacingY();
        } else if (object instanceof DroppedAmmo ammo) {
            entity.type = NetworkReplica.PICKUP;
            entity.variant = 1;
            entity.detail = ammo.getWeapon().getName();
            entity.amount = ammo.getReserveAmmo();
        } else if (object instanceof DroppedWeapon weapon) {
            entity.type = NetworkReplica.PICKUP;
            entity.variant = 2;
            entity.detail = weapon.getWeapon().getName();
        } else if (object instanceof DroppedArmor) {
            entity.type = NetworkReplica.PICKUP;
            entity.variant = 3;
            entity.detail = "Gilet";
        } else if (object instanceof ImpactSpark) {
            entity.type = NetworkReplica.EFFECT;
            entity.variant = 1;
        } else if (object instanceof RocketBlastCloud) {
            entity.type = NetworkReplica.EFFECT;
            entity.variant = 2;
        } else if (object instanceof Shockwave) {
            entity.type = NetworkReplica.EFFECT;
            entity.variant = 3;
        } else if (object instanceof TeslaArc) {
            entity.type = NetworkReplica.EFFECT;
            entity.variant = 4;
        } else if (object instanceof AlienTeleport) {
            entity.type = NetworkReplica.EFFECT;
            entity.variant = 5;
        } else if (object instanceof Douille) {
            entity.type = NetworkReplica.EFFECT;
            entity.variant = 6;
        } else {
            return null;
        }
        return entity;
    }

    public void onRemotePlayerConnected(RemotePlayerInput input) {
        remotePlayerInput = input;
        addRemoteProtagonistIfNeeded();
    }

    public void onRemotePlayerDisconnected() {
        Protagonist disconnected = remoteProtagonist;
        remoteProtagonist = null;
        remotePlayerInput = null;
        if (disconnected != null) ObjectManager.list.remove(disconnected);
    }

    private void addRemoteProtagonistIfNeeded() {
        if (remotePlayerInput == null || remoteProtagonist != null) return;
        Protagonist primary = ObjectManager.getProtagonist();
        double x = primary != null ? primary.x + tileSize : tileSize * 7.0;
        double y = primary != null ? primary.y : tileSize * 6.0;
        if (!isSpawnAreaFree(x, y, ENTITY_RADIUS)) {
            double[] spawn = getFreeSpawnPosition();
            x = spawn[0];
            y = spawn[1];
        }
        remoteProtagonist = new Protagonist(x, y, remotePlayerInput, false);
        ObjectManager.list.add(remoteProtagonist);
    }

    private void initializeWorld() {
        ObjectManager.list.clear();
        score = 0;
        gameOverTitle = "VOUS ETES MORT";
        resetArcadeState();
        resetMissionState();

        int civilsToSpawn = nombreCivil;
        int soldatsToSpawn = nombreSoldat;
        if (GameMode.current == GameMode.PROTECTION) {
            civilsToSpawn = Math.max(4, nombreCivil);
            soldatsToSpawn = Math.max(1, nombreSoldat);
        } else if (GameMode.current == GameMode.ARCADE) {
            civilsToSpawn = Math.max(1, Math.min(2, nombreCivil));
            soldatsToSpawn = Math.max(1, nombreSoldat);
        } else if (GameMode.current == GameMode.MISSION) {
            civilsToSpawn = Math.max(1, Math.min(2, nombreCivil));
            soldatsToSpawn = Math.max(1, nombreSoldat);
        }

        if (isEasyDifficulty()) {
            civilsToSpawn = Math.max(civilsToSpawn, nombreCivil + 1);
            soldatsToSpawn = Math.min(10, soldatsToSpawn + 1);
        }

        // Cree une petite population initiale.
        for (int i = 0; i < civilsToSpawn; i++) {
            double[] spawn = getFreeSpawnPosition();
            ObjectManager.list.add(new Homme(spawn[0], spawn[1]));
        }
        double[] protagonistSpawn = getFreeSpawnPositionInZone(5, Math.min(8, tileManager.getMapCols() - 2), 4, Math.min(7, tileManager.getMapRows() - 2));
        double protagonistSpawnX = protagonistSpawn[0];
        double protagonistSpawnY = protagonistSpawn[1];
        ObjectManager.list.add(new Protagonist(protagonistSpawn[0], protagonistSpawn[1], keyController));
        remoteProtagonist = null;
        addRemoteProtagonistIfNeeded();

        // Garde les soldats alliés près du protagoniste dans la même zone sécurisée.
        for (int i = 0; i < soldatsToSpawn; i++) {
            double[] soldatSpawn = getFreeSpawnPositionInZone(
                5,
                Math.min(8, tileManager.getMapCols() - 2),
                4,
                Math.min(7, tileManager.getMapRows() - 2)
            );
            ObjectManager.list.add(new Soldat(soldatSpawn[0], soldatSpawn[1]));
        }
        spawnAmmoDepots();
        spawnMapSpecificObjects();

        int aliensToSpawn = getAdjustedAlienCount(GameMode.current == GameMode.ARCADE ? Math.max(2, nombreAlien - 1) : nombreAlien);
        for (int i = 0; i < aliensToSpawn; i++) {
            double[] spawn = getFreeSpawnPositionFarFrom(
                    protagonistSpawnX,
                    protagonistSpawnY,
                    SAFE_HOSTILE_SPAWN_DISTANCE
            );
            ObjectManager.list.add(new AlienTeleport(spawn[0], spawn[1]));
        }

        int enemyCount = getAdjustedEnemyCount(GameMode.current == GameMode.ARCADE ? Math.max(8, nombreEnnemi - 1) : nombreEnnemi);
        int[][] enemySpawnZones = buildEnemySpawnZones();
        for (int i = 0; i < enemyCount; i++) {
            int[] zone = enemySpawnZones[i % enemySpawnZones.length];
            double[] spawn = getFreeSpawnPositionInZoneFarFrom(
                    zone[0],
                    zone[1],
                    zone[2],
                    zone[3],
                    protagonistSpawnX,
                    protagonistSpawnY,
                    SAFE_HOSTILE_SPAWN_DISTANCE
            );
            ObjectManager.list.add(new Ennemi(spawn[0], spawn[1], chooseEnemyArchetype(i, enemyCount)));
        }

        if (GameMode.current == GameMode.MISSION) {
            setupMission(protagonistSpawnX, protagonistSpawnY);
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
            double[] position = getFreeSpawnPositionInZone(zone[0], zone[1], zone[2], zone[3]);
            ObjectManager.list.add(new AmmoDepot(position[0], position[1]));
        }

        if (isEasyDifficulty()) {
            double[] bonusDepot = getFreeSpawnPositionInZone(
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

    private void resetArcadeState() {
        arcadeWaveIndex = 0;
        arcadeWaveTimer = ARCADE_BASE_WAVE_INTERVAL_FRAMES;
        arcadeWaveBannerTimer = 0;
        screenShakeFrames = 0;
        screenShakeAmplitude = 0.0;
        screenFlashFrames = 0;
        screenFlashAlpha = 0f;
        screenFlashColor = new Color(255, 236, 160);
        alienRespawnCooldownFrames = 0;
    }

    private void resetMissionState() {
        activeMissionType = null;
        activeMissionTitle = "";
        missionPrimaryZone = null;
        missionSecondaryZone = null;
        missionHostages.clear();
        missionFreedHostages.clear();
        missionExtractedHostages.clear();
        missionDefendFramesRemaining = 0;
        missionSecureFrames = 0;
        missionVehicleX = -1;
        missionVehicleY = -1;
        missionVehicleChargeArmed = false;
        missionVehicleFuseFrames = 0;
        missionFailed = false;
    }

    private EnemyArchetype chooseEnemyArchetype(int index, int total) {
        if (isEasyDifficulty()) {
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

    private int[][] buildEnemySpawnZones() {
        int maxCol = tileManager.getMapCols() - 2;
        int maxRow = tileManager.getMapRows() - 2;

        int rightStart = Math.max(1, maxCol - 10);
        int rightEnd = maxCol;
        int centerStart = Math.max(1, maxCol / 2 - 4);
        int centerEnd = Math.min(maxCol, maxCol / 2 + 4);

        int topStart = 1;
        int topEnd = Math.min(8, maxRow);
        int bottomStart = Math.max(1, maxRow - 8);
        int bottomEnd = maxRow;

        return new int[][]{
                {rightStart, rightEnd, topStart, topEnd},
            {rightStart, rightEnd, bottomStart, bottomEnd},
            {centerStart, centerEnd, bottomStart, bottomEnd},
            {Math.max(1, centerStart - 12), Math.max(1, centerEnd - 12), bottomStart, bottomEnd}
        };
    }

    private MissionType selectNextMissionType() {
        MissionType[] types = MissionType.values();
        MissionType type = types[missionRotationIndex % types.length];
        missionRotationIndex++;
        return type;
    }

    private Rectangle2D.Double zoneFromTiles(int startCol, int endCol, int startRow, int endRow) {
        int minCol = Math.max(0, Math.min(startCol, tileManager.getMapCols() - 1));
        int maxCol = Math.max(0, Math.min(endCol, tileManager.getMapCols() - 1));
        int minRow = Math.max(0, Math.min(startRow, tileManager.getMapRows() - 1));
        int maxRow = Math.max(0, Math.min(endRow, tileManager.getMapRows() - 1));

        if (minCol > maxCol || minRow > maxRow) {
            return new Rectangle2D.Double(0, 0, tileSize * 2.0, tileSize * 2.0);
        }

        double x = minCol * tileSize;
        double y = minRow * tileSize;
        double width = (maxCol - minCol + 1) * tileSize;
        double height = (maxRow - minRow + 1) * tileSize;
        return new Rectangle2D.Double(x, y, width, height);
    }

    private void setupMission(double protagonistSpawnX, double protagonistSpawnY) {
        activeMissionType = selectNextMissionType();

        int cols = tileManager.getMapCols();
        int rows = tileManager.getMapRows();
        int centerRow = Math.max(3, rows / 2 - 2);

        switch (activeMissionType) {
            case RESCUE_HOSTAGES -> {
                activeMissionTitle = "Mission: Liberer les otages";
                int startCol = Math.max(2, cols - 13);
                int endCol = Math.max(4, cols - 8);
                int startRow = 4;
                int endRow = Math.min(rows - 6, 11);
                missionPrimaryZone = zoneFromTiles(startCol, endCol, startRow, endRow);
                missionSecondaryZone = zoneFromTiles(3, Math.min(8, cols - 2), 3, Math.min(8, rows - 2));

                for (int i = 0; i < 3; i++) {
                    double[] spawn = getFreeSpawnPositionInZone(startCol, endCol, startRow + 1, Math.max(startRow + 1, endRow - 1));
                    Hostage hostage = new Hostage(spawn[0], spawn[1]);
                    missionHostages.add(hostage);
                    ObjectManager.list.add(hostage);
                }

                spawnMissionGuards(startCol, endCol, startRow, endRow);
            }
            case DEFEND_POSITION -> {
                activeMissionTitle = "Mission: Proteger la position";
                missionPrimaryZone = new Rectangle2D.Double(
                        protagonistSpawnX - tileSize * 2.5,
                        protagonistSpawnY - tileSize * 2.5,
                        tileSize * 5.0,
                        tileSize * 5.0
                );
                missionDefendFramesRemaining = MISSION_DEFEND_HOLD_FRAMES;
            }
            case SECURE_ZONE -> {
                activeMissionTitle = "Mission: Securiser la zone";
                missionPrimaryZone = zoneFromTiles(Math.max(8, cols / 2 - 4), Math.min(cols - 4, cols / 2 + 4), centerRow, Math.min(rows - 4, centerRow + 7));
                missionSecureFrames = 0;
            }
            case INFILTRATE -> {
                activeMissionTitle = "Mission: Infiltration";
                missionPrimaryZone = zoneFromTiles(Math.max(3, cols - 8), Math.max(5, cols - 4), 3, Math.min(rows - 4, 8));
            }
            case DESTROY_VEHICLE -> {
                activeMissionTitle = "Mission: Sabotage";
                int col = Math.max(6, cols - 9);
                int row = Math.max(6, rows - 8);
                missionVehicleX = col * tileSize + tileSize * 0.5;
                missionVehicleY = row * tileSize + tileSize * 0.5;
                missionPrimaryZone = new Rectangle2D.Double(
                        missionVehicleX - tileSize * 1.5,
                        missionVehicleY - tileSize,
                        tileSize * 3.0,
                        tileSize * 2.0
                );
            }
        }
    }

    private void updateMissionMode(Protagonist protagonist, Protagonist interactionPlayer, boolean interactTriggered) {
        if (GameMode.current != GameMode.MISSION || activeMissionType == null || protagonist == null || missionFailed) {
            return;
        }

        if (interactionPlayer == null) interactionPlayer = protagonist;

        switch (activeMissionType) {
            case RESCUE_HOSTAGES -> updateRescueMission(interactionPlayer, interactTriggered);
            case DEFEND_POSITION -> updateDefendMission(protagonist);
            case SECURE_ZONE -> updateSecureMission(protagonist);
            case INFILTRATE -> updateInfiltrationMission(protagonist);
            case DESTROY_VEHICLE -> updateSabotageMission(interactionPlayer, interactTriggered);
        }
    }

    private void updateRescueMission(Protagonist protagonist, boolean interactTriggered) {
        int aliveHostages = 0;
        int deadHostages = 0;

        for (Hostage hostage : missionHostages) {
            if (missionExtractedHostages.contains(hostage)) {
                continue;
            }

            if (!isObjectAlive(hostage)) {
                deadHostages++;
                continue;
            }

            aliveHostages++;
            if (!hostage.isRescued()) {
                double dx = hostage.x - protagonist.x;
                double dy = hostage.y - protagonist.y;
                if (interactTriggered && dx * dx + dy * dy <= MISSION_HOSTAGE_FREE_DISTANCE * MISSION_HOSTAGE_FREE_DISTANCE) {
                    hostage.rescue();
                    if (!missionFreedHostages.contains(hostage)) {
                        missionFreedHostages.add(hostage);
                    }
                }
            }

            if (hostage.isRescued() && missionSecondaryZone != null && missionSecondaryZone.contains(hostage.x, hostage.y)) {
                missionExtractedHostages.add(hostage);
                ObjectManager.list.remove(hostage);
            }
        }

        if (deadHostages > 0) {
            failMission("OTAGE ABATTU");
            return;
        }

        if (!missionHostages.isEmpty() && missionExtractedHostages.size() >= missionHostages.size()) {
            screenState = ScreenState.VICTORY;
            paused = false;
            return;
        }

        if (aliveHostages <= 0 && missionExtractedHostages.isEmpty()) {
            failMission("OTAGES PERDUS");
        }
    }

    private void spawnMissionGuards(int startCol, int endCol, int startRow, int endRow) {
        int outerStartCol = Math.max(1, startCol - 2);
        int outerEndCol = Math.min(tileManager.getMapCols() - 2, endCol + 2);
        int outerStartRow = Math.max(1, startRow - 1);
        int outerEndRow = Math.min(tileManager.getMapRows() - 2, endRow + 2);

        EnemyArchetype[] guards = {
                EnemyArchetype.STANDARD,
                EnemyArchetype.STANDARD,
                EnemyArchetype.ASSAUT,
                EnemyArchetype.FLANQUEUR
        };

        int guardCount = isEasyDifficulty() ? 2 : guards.length;

        for (int i = 0; i < guardCount; i++) {
            double[] spawn = getFreeSpawnPositionInZone(outerStartCol, outerEndCol, outerStartRow, outerEndRow);
            ObjectManager.list.add(new Ennemi(spawn[0], spawn[1], guards[i]));
        }
    }

    private void updateDefendMission(Protagonist protagonist) {
        if (missionPrimaryZone != null && missionPrimaryZone.contains(protagonist.x, protagonist.y)) {
            missionDefendFramesRemaining = Math.max(0, missionDefendFramesRemaining - 1);
        }

        if (missionDefendFramesRemaining <= 0) {
            screenState = ScreenState.VICTORY;
            paused = false;
        }
    }

    private void updateSecureMission(Protagonist protagonist) {
        int hostilesInZone = countHostilesInZone(missionPrimaryZone);
        boolean protagonistInZone = missionPrimaryZone != null && missionPrimaryZone.contains(protagonist.x, protagonist.y);

        if (hostilesInZone == 0 && protagonistInZone) {
            missionSecureFrames++;
        } else {
            missionSecureFrames = Math.max(0, missionSecureFrames - 2);
        }

        if (missionSecureFrames >= MISSION_SECURE_HOLD_FRAMES) {
            screenState = ScreenState.VICTORY;
            paused = false;
        }
    }

    private void updateInfiltrationMission(Protagonist protagonist) {
        if (isProtagonistDetected(protagonist)) {
            failMission("INFILTRATION COMPROMISE");
            return;
        }

        if (missionPrimaryZone != null && missionPrimaryZone.contains(protagonist.x, protagonist.y)) {
            screenState = ScreenState.VICTORY;
            paused = false;
        }
    }

    private void updateSabotageMission(Protagonist protagonist, boolean interactTriggered) {
        double dx = missionVehicleX - protagonist.x;
        double dy = missionVehicleY - protagonist.y;
        boolean closeEnough = dx * dx + dy * dy <= MISSION_INTERACT_DISTANCE * MISSION_INTERACT_DISTANCE;

        if (!missionVehicleChargeArmed && closeEnough && interactTriggered) {
            missionVehicleChargeArmed = true;
            missionVehicleFuseFrames = MISSION_DETONATION_FUSE_FRAMES;
            GamePanel.triggerScreenFlash(new Color(255, 230, 140), 0.09f, 4);
        }

        if (missionVehicleChargeArmed) {
            missionVehicleFuseFrames = Math.max(0, missionVehicleFuseFrames - 1);
            if (missionVehicleFuseFrames <= 0) {
                for (int i = 0; i < 6; i++) {
                    ObjectManager.list.add(new ImpactSpark(
                            missionVehicleX,
                            missionVehicleY,
                            Math.random() * 2.0 - 1.0,
                            Math.random() * 2.0 - 1.0,
                            1.8,
                            new Color(255, 214, 138),
                            new Color(255, 118, 72)
                    ));
                }

                ObjectManager.list.add(new Shockwave(missionVehicleX, missionVehicleY, protagonist, tileSize * 3.2));
                triggerScreenShake(16, 7.8);
                triggerScreenFlash(new Color(255, 170, 96), 0.24f, 8);
                screenState = ScreenState.VICTORY;
                paused = false;
            }
        }
    }

    private void failMission(String reason) {
        missionFailed = true;
        gameOverTitle = reason;
        screenState = ScreenState.GAME_OVER;
        paused = false;
    }

    private boolean isObjectAlive(GameObject target) {
        if (target == null) {
            return false;
        }

        for (GameObject obj : ObjectManager.list) {
            if (obj == target) {
                return true;
            }
        }
        return false;
    }

    private int countHostilesInZone(Rectangle2D zone) {
        if (zone == null) {
            return 0;
        }

        int count = 0;
        for (GameObject obj : ObjectManager.list) {
            if (obj instanceof Ennemi || obj instanceof Alien) {
                if (zone.contains(obj.x, obj.y)) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean isProtagonistDetected(Protagonist protagonist) {
        for (GameObject obj : ObjectManager.list) {
            if (!(obj instanceof Ennemi) && !(obj instanceof Alien)) {
                continue;
            }

            double dx = protagonist.x - obj.x;
            double dy = protagonist.y - obj.y;
            double distSq = dx * dx + dy * dy;
            if (distSq > MISSION_INFILTRATION_DETECT_DISTANCE * MISSION_INFILTRATION_DETECT_DISTANCE) {
                continue;
            }

            if (TacticalMovement.hasLineOfSight(obj.x, obj.y, protagonist.x, protagonist.y)) {
                return true;
            }
        }

        return false;
    }

    private double[] getFreeSpawnPositionInZone(int startCol, int endCol, int startRow, int endRow) {
        int mapCols = tileManager.getMapCols();
        int mapRows = tileManager.getMapRows();

        int minCol = Math.max(0, Math.min(startCol, mapCols - 1));
        int maxCol = Math.max(0, Math.min(endCol, mapCols - 1));
        int minRow = Math.max(0, Math.min(startRow, mapRows - 1));
        int maxRow = Math.max(0, Math.min(endRow, mapRows - 1));

        if (minCol > maxCol || minRow > maxRow) {
            return getFreeSpawnPosition();
        }

        for (int i = 0; i < MAX_SPAWN_ATTEMPTS; i++) {
            int col = minCol + (int) (Math.random() * (maxCol - minCol + 1));
            int row = minRow + (int) (Math.random() * (maxRow - minRow + 1));

            double x = col * tileSize + tileSize / 2.0;
            double y = row * tileSize + tileSize / 2.0;

            if (isSpawnAreaFree(x, y, ENTITY_RADIUS)) {
                return new double[]{x, y};
            }
        }

        for (int row = minRow; row <= maxRow; row++) {
            for (int col = minCol; col <= maxCol; col++) {
                double x = col * tileSize + tileSize / 2.0;
                double y = row * tileSize + tileSize / 2.0;

                if (isSpawnAreaFree(x, y, ENTITY_RADIUS)) {
                    return new double[]{x, y};
                }
            }
        }

        return getFreeSpawnPosition();
    }

    private double[] getFreeSpawnPositionInZoneFarFrom(
            int startCol,
            int endCol,
            int startRow,
            int endRow,
            double avoidX,
            double avoidY,
            double minDistance
    ) {
        int mapCols = tileManager.getMapCols();
        int mapRows = tileManager.getMapRows();

        int minCol = Math.max(0, Math.min(startCol, mapCols - 1));
        int maxCol = Math.max(0, Math.min(endCol, mapCols - 1));
        int minRow = Math.max(0, Math.min(startRow, mapRows - 1));
        int maxRow = Math.max(0, Math.min(endRow, mapRows - 1));

        if (minCol > maxCol || minRow > maxRow) {
            return getFreeSpawnPositionFarFrom(avoidX, avoidY, minDistance);
        }

        for (int i = 0; i < MAX_SPAWN_ATTEMPTS; i++) {
            int col = minCol + (int) (Math.random() * (maxCol - minCol + 1));
            int row = minRow + (int) (Math.random() * (maxRow - minRow + 1));

            double x = col * tileSize + tileSize / 2.0;
            double y = row * tileSize + tileSize / 2.0;

            if (isSpawnAreaFree(x, y, ENTITY_RADIUS) && isFarEnoughFromPoint(x, y, avoidX, avoidY, minDistance)) {
                return new double[]{x, y};
            }
        }

        for (int row = minRow; row <= maxRow; row++) {
            for (int col = minCol; col <= maxCol; col++) {
                double x = col * tileSize + tileSize / 2.0;
                double y = row * tileSize + tileSize / 2.0;

                if (isSpawnAreaFree(x, y, ENTITY_RADIUS) && isFarEnoughFromPoint(x, y, avoidX, avoidY, minDistance)) {
                    return new double[]{x, y};
                }
            }
        }

        return getFreeSpawnPositionFarFrom(avoidX, avoidY, minDistance);
    }

    private double[] getFreeSpawnPosition() {
        int mapCols = tileManager.getMapCols();
        int mapRows = tileManager.getMapRows();
        double worldWidth = tileManager.getWorldWidth();
        double worldHeight = tileManager.getWorldHeight();

        for (int i = 0; i < MAX_SPAWN_ATTEMPTS; i++) {
            double x = ENTITY_RADIUS + Math.random() * (worldWidth - ENTITY_RADIUS * 2);
            double y = ENTITY_RADIUS + Math.random() * (worldHeight - ENTITY_RADIUS * 2);

            if (isSpawnAreaFree(x, y, ENTITY_RADIUS)) {
                return new double[]{x, y};
            }
        }

        for (int row = 0; row < mapRows; row++) {
            for (int col = 0; col < mapCols; col++) {
                double x = col * tileSize + tileSize / 2.0;
                double y = row * tileSize + tileSize / 2.0;

                if (isSpawnAreaFree(x, y, ENTITY_RADIUS)) {
                    return new double[]{x, y};
                }
            }
        }

        return new double[]{tileSize / 2.0, tileSize / 2.0};
    }

    private double[] getFreeSpawnPositionFarFrom(double avoidX, double avoidY, double minDistance) {
        int mapCols = tileManager.getMapCols();
        int mapRows = tileManager.getMapRows();
        double worldWidth = tileManager.getWorldWidth();
        double worldHeight = tileManager.getWorldHeight();

        for (int i = 0; i < MAX_SPAWN_ATTEMPTS; i++) {
            double x = ENTITY_RADIUS + Math.random() * (worldWidth - ENTITY_RADIUS * 2);
            double y = ENTITY_RADIUS + Math.random() * (worldHeight - ENTITY_RADIUS * 2);

            if (isSpawnAreaFree(x, y, ENTITY_RADIUS) && isFarEnoughFromPoint(x, y, avoidX, avoidY, minDistance)) {
                return new double[]{x, y};
            }
        }

        for (int row = 0; row < mapRows; row++) {
            for (int col = 0; col < mapCols; col++) {
                double x = col * tileSize + tileSize / 2.0;
                double y = row * tileSize + tileSize / 2.0;

                if (isSpawnAreaFree(x, y, ENTITY_RADIUS) && isFarEnoughFromPoint(x, y, avoidX, avoidY, minDistance)) {
                    return new double[]{x, y};
                }
            }
        }

        return getFreeSpawnPosition();
    }

    private boolean isFarEnoughFromPoint(double x, double y, double pointX, double pointY, double minDistance) {
        double dx = x - pointX;
        double dy = y - pointY;
        return dx * dx + dy * dy >= minDistance * minDistance;
    }

    private boolean isSpawnAreaFree(double x, double y, double radius) {
        return !tileManager.isBlockedAtPixel(x - radius, y - radius)
                && !tileManager.isBlockedAtPixel(x + radius, y - radius)
                && !tileManager.isBlockedAtPixel(x - radius, y + radius)
                && !tileManager.isBlockedAtPixel(x + radius, y + radius)
                && ObjectManager.isHumanAreaFree(x, y, radius);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> launch(args));
    }

    private static void launch(String[] args) {
        String action = args.length > 0 ? args[0] : "";
        if (action.isEmpty()) {
            Object[] choices = {"Solo", "Heberger en LAN", "Rejoindre"};
            int selected = JOptionPane.showOptionDialog(
                    null, "Comment veux-tu jouer ?", "SIM ENGINE",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, choices, choices[0]
            );
            if (selected < 0) return;
            action = selected == 1 ? "--host" : selected == 2 ? "--join" : "--solo";
        }

        if ("--join".equals(action)) {
            String defaultHost = args.length > 1 ? args[1] : "192.168.1.2";
            String host = args.length > 1 ? defaultHost : JOptionPane.showInputDialog(
                    null, "Adresse IP de l'hote :", defaultHost
            );
            if (host == null || host.isBlank()) return;
            launchClient(host.trim());
            return;
        }

        JFrame frame = new JFrame("--host".equals(action) ? "SIM ENGINE - Hote LAN" : "Simulation");
        GamePanel gamePanel = new GamePanel();
        frame.add(gamePanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(gamePanel.keyController);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        if ("--host".equals(action)) gamePanel.startLanHost(LanHost.DEFAULT_PORT);
        new Thread(gamePanel, "game-loop").start();
    }

    private static void launchClient(String host) {
        JFrame frame = new JFrame("SIM ENGINE - Joueur 2");
        LanClientViewer viewer = new LanClientViewer(host, LanHost.DEFAULT_PORT);
        frame.add(viewer);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(viewer.getControls());
        frame.setVisible(true);
        new Thread(viewer, "lan-client").start();
    }

    public void run() {
        long previousTime = System.nanoTime();
        long accumulator = 0L;

        while (true) {
            long currentTime = System.nanoTime();
            long elapsed = currentTime - previousTime;
            previousTime = currentTime;
            accumulator += elapsed;

            int updates = 0;
            while (accumulator >= NANOS_PER_UPDATE && updates < 5) {
                updateGameStep();
                accumulator -= NANOS_PER_UPDATE;
                updates++;
            }

            handleMenuInput();

            repaint();

            long sleepNanos = NANOS_PER_UPDATE - accumulator;
            if (sleepNanos <= 0) {
                Thread.yield();
                continue;
            }

            try {
                long sleepMillis = sleepNanos / 1_000_000L;
                int sleepNanoPart = (int) (sleepNanos % 1_000_000L);
                if (sleepMillis > 0 || sleepNanoPart > 0) {
                    Thread.sleep(sleepMillis, sleepNanoPart);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void updateGameStep() {
        if (screenState == ScreenState.PLAYING && keyController.consumePauseToggleTriggered()) {
            paused = !paused;
        }

        tickScreenFeedback();

        if (screenState == ScreenState.PLAYING && !paused) {
            Protagonist protagonist = ObjectManager.getProtagonist();
            Protagonist interactionPlayer = consumeInteractionPlayer(protagonist);
            boolean interactionUsedByTurret = tryToggleDesertTurret(interactionPlayer);
            boolean interactionUsedByRevive = !interactionUsedByTurret && tryReviveAlly(interactionPlayer);
            updateRemoteInputCamera();
            applySoldierMoveCommand();
            ObjectManager.updateAll();
            maintainAlienPresence();
            updateArcadeMode();
            protagonist = ObjectManager.getProtagonist();
            if (protagonist == null) {
                gameOverTitle = "VOUS ETES MORT";
                screenState = ScreenState.GAME_OVER;
                paused = false;
                return;
            }

            if (GameMode.current == GameMode.MISSION) {
                updateMissionMode(protagonist, interactionPlayer,
                        interactionPlayer != null && !interactionUsedByTurret && !interactionUsedByRevive);
                if (screenState != ScreenState.PLAYING) {
                    return;
                }
            }

            if (GameMode.current == GameMode.PROTECTION) {
                int[] counts = ObjectManager.getUiCounts();
                int alliesAlive = counts[0] + counts[1];
                if (alliesAlive <= 0) {
                    gameOverTitle = "MISSION ECHOUEE";
                    screenState = ScreenState.GAME_OVER;
                    paused = false;
                    return;
                }

                if (protagonist.x >= getExtractionX()) {
                    screenState = ScreenState.VICTORY;
                    paused = false;
                    return;
                }
            }

            updateCamera();
        }
    }

    private Protagonist consumeInteractionPlayer(Protagonist primaryPlayer) {
        if (keyController.consumeInteractTriggered()) {
            return primaryPlayer;
        }
        RemotePlayerInput remoteInput = remotePlayerInput;
        if (remoteInput != null && remoteInput.consumeInteractTriggered()) {
            return remoteProtagonist;
        }
        return null;
    }

    private boolean tryToggleDesertTurret(Protagonist player) {
        if (player == null) return false;
        for (GameObject object : new ArrayList<>(ObjectManager.list)) {
            if (object instanceof DesertTurret turret && turret.tryToggleOperator(player)) {
                return true;
            }
        }
        return false;
    }

    private boolean tryReviveAlly(Protagonist player) {
        if (player == null) return false;
        for (GameObject object : new ArrayList<>(ObjectManager.list)) {
            if (object instanceof DeathMarker marker && marker.tryRevive(player)) {
                return true;
            }
        }
        return false;
    }

    private void updateRemoteInputCamera() {
        Protagonist remote = remoteProtagonist;
        if (remote == null) return;
        RemotePlayerInput input = remotePlayerInput;
        int viewWidth = input != null ? input.getViewWidth() : getViewWidth();
        int viewHeight = input != null ? input.getViewHeight() : getViewHeight();
        int[] camera = tileManager.calculateCameraFor(remote.x, remote.y, viewWidth, viewHeight);
        remote.setIndependentCamera(camera[0], camera[1]);
    }

    private void maintainAlienPresence() {
        if (alienRespawnCooldownFrames > 0) {
            alienRespawnCooldownFrames--;
            return;
        }

        int aliveAliens = ObjectManager.getUiCounts()[2];
        int baseDesiredAliens = GameMode.current == GameMode.ARCADE ? Math.max(2, nombreAlien) : Math.max(1, nombreAlien);
        int desiredAliens = getAdjustedAlienCount(baseDesiredAliens);
        if (aliveAliens >= desiredAliens) {
            return;
        }

        Protagonist protagonist = ObjectManager.getProtagonist();
        if (protagonist == null) {
            return;
        }

        double[] spawn = getFreeSpawnPositionFarFrom(
                protagonist.x,
                protagonist.y,
                SAFE_HOSTILE_SPAWN_DISTANCE * 0.72
        );
        ObjectManager.list.add(new AlienTeleport(spawn[0], spawn[1]));
        alienRespawnCooldownFrames = ALIEN_RESPAWN_COOLDOWN_FRAMES;
    }

    private void tickScreenFeedback() {
        if (screenShakeFrames > 0) {
            screenShakeFrames--;
            screenShakeAmplitude *= 0.82;
            if (screenShakeFrames == 0) {
                screenShakeAmplitude = 0.0;
            }
        }

        if (screenFlashFrames > 0) {
            screenFlashFrames--;
            screenFlashAlpha *= 0.72f;
            if (screenFlashFrames == 0) {
                screenFlashAlpha = 0f;
            }
        }
    }

    private void updateArcadeMode() {
        if (GameMode.current != GameMode.ARCADE || screenState != ScreenState.PLAYING) {
            return;
        }

        if (arcadeWaveBannerTimer > 0) {
            arcadeWaveBannerTimer--;
        }

        if (arcadeWaveTimer > 0) {
            arcadeWaveTimer--;
            return;
        }

        spawnArcadeWave();
    }

    private void spawnArcadeWave() {
        Protagonist protagonist = ObjectManager.getProtagonist();
        if (protagonist == null) {
            return;
        }

        arcadeWaveIndex++;
        arcadeWaveBannerTimer = ARCADE_WAVE_BANNER_FRAMES;
        int enemyReinforcements = Math.min(6, 2 + arcadeWaveIndex / 2);
        int alienReinforcements = arcadeWaveIndex >= 2 && arcadeWaveIndex % 2 == 0 ? 1 : 0;
        if (isEasyDifficulty()) {
            enemyReinforcements = Math.max(1, enemyReinforcements - 1);
            if (arcadeWaveIndex % 3 == 0) {
                alienReinforcements = 0;
            }
        }
        int[][] enemySpawnZones = buildEnemySpawnZones();

        for (int i = 0; i < enemyReinforcements; i++) {
            int[] zone = enemySpawnZones[(arcadeWaveIndex + i) % enemySpawnZones.length];
            double[] spawn = getFreeSpawnPositionInZoneFarFrom(
                    zone[0],
                    zone[1],
                    zone[2],
                    zone[3],
                    protagonist.x,
                    protagonist.y,
                    SAFE_HOSTILE_SPAWN_DISTANCE * 0.8
            );
            ObjectManager.list.add(new Ennemi(spawn[0], spawn[1], chooseArcadeArchetype(i, enemyReinforcements)));
        }

        for (int i = 0; i < alienReinforcements; i++) {
            double[] spawn = getFreeSpawnPositionFarFrom(protagonist.x, protagonist.y, SAFE_HOSTILE_SPAWN_DISTANCE * 0.75);
            ObjectManager.list.add(new AlienTeleport(spawn[0], spawn[1]));
        }

        arcadeWaveTimer = Math.max(
                ARCADE_MIN_WAVE_INTERVAL_FRAMES,
                ARCADE_BASE_WAVE_INTERVAL_FRAMES - arcadeWaveIndex * TARGET_UPS / 2
        );
        triggerScreenShake(6 + arcadeWaveIndex, 4.8);
        triggerScreenFlash(new Color(255, 154, 88), 0.16f, 6);
    }

    private EnemyArchetype chooseArcadeArchetype(int index, int waveSize) {
        if (isEasyDifficulty()) {
            double pressure = arcadeWaveIndex + index / (double) Math.max(1, waveSize);
            if (pressure < 3.5) {
                return EnemyArchetype.STANDARD;
            }
            return index % 2 == 0 ? EnemyArchetype.FLANQUEUR : EnemyArchetype.ASSAUT;
        }

        double pressure = arcadeWaveIndex + index / (double) Math.max(1, waveSize);
        if (pressure < 1.5) {
            return EnemyArchetype.STANDARD;
        }
        if (pressure < 3.2) {
            return index % 3 == 0 ? EnemyArchetype.ASSAUT : EnemyArchetype.FLANQUEUR;
        }
        if (pressure < 5.0) {
            return index % 5 == 0 ? EnemyArchetype.ROQUETTE : (index % 4 == 0 ? EnemyArchetype.LOURD : EnemyArchetype.ASSAUT);
        }
        if (index % 5 == 0) {
            return EnemyArchetype.LOURD;
        }
        if (index % 7 == 0) {
            return EnemyArchetype.ROQUETTE;
        }
        return index % 2 == 0 ? EnemyArchetype.FLANQUEUR : EnemyArchetype.ASSAUT;
    }

    private void handleMenuInput() {
        if (screenState == ScreenState.PLAYING && !paused) {
            return;
        }

        if (!keyController.consumeLeftClickPressed()) {
            return;
        }

        int mouseX = keyController.getMouseX();
        int mouseY = keyController.getMouseY();

        if (screenState == ScreenState.PLAYING && paused) {
            if (getPauseMenuButtonBounds().contains(mouseX, mouseY)) {
                paused = false;
                screenState = ScreenState.MENU;
            }
            return;
        }

        if (screenState == ScreenState.MENU) {
            if (handleMapSelectionClick(mouseX, mouseY)) {
                return;
            }

            if (handleDifficultySelectionClick(mouseX, mouseY)) {
                return;
            }

            if (getFreeModeButtonBounds().contains(mouseX, mouseY)) {
                GameMode.current = GameMode.FREE;
                initializeWorld();
                screenState = ScreenState.PLAYING;
                paused = false;
            } else if (getArcadeModeButtonBounds().contains(mouseX, mouseY)) {
                GameMode.current = GameMode.ARCADE;
                initializeWorld();
                screenState = ScreenState.PLAYING;
                paused = false;
            } else if (getStoryModeButtonBounds().contains(mouseX, mouseY)) {
                GameMode.current = GameMode.STORY;
                initializeWorld();
                screenState = ScreenState.PLAYING;
                paused = false;
            } else if (getMissionModeButtonBounds().contains(mouseX, mouseY)) {
                GameMode.current = GameMode.MISSION;
                initializeWorld();
                screenState = ScreenState.PLAYING;
                paused = false;
            } else if (getProtectionModeButtonBounds().contains(mouseX, mouseY)) {
                GameMode.current = GameMode.PROTECTION;
                initializeWorld();
                screenState = ScreenState.PLAYING;
                paused = false;
            } else if (getOptionsButtonBounds().contains(mouseX, mouseY)) {
                screenState = ScreenState.OPTIONS;
            }
            return;
        }

        if (screenState == ScreenState.OPTIONS && getBackButtonBounds().contains(mouseX, mouseY)) {
            screenState = ScreenState.MENU;
            return;
        }

        if (screenState == ScreenState.OPTIONS) {
            if (handleAiOptionsClick(mouseX, mouseY)) {
                return;
            }
        }

        if ((screenState == ScreenState.GAME_OVER || screenState == ScreenState.VICTORY)
                && getReplayButtonBounds().contains(mouseX, mouseY)) {
            initializeWorld();
            screenState = ScreenState.PLAYING;
            paused = false;
        }
    }

    private Rectangle getPauseMenuButtonBounds() {
        int panelWidth = getViewWidth();
        int panelHeight = getViewHeight();
        int buttonWidth = 220;
        int buttonHeight = 48;
        int x = (panelWidth - buttonWidth) / 2;
        int y = panelHeight / 2 + 70;
        return new Rectangle(x, y, buttonWidth, buttonHeight);
    }

    private Rectangle getFreeModeButtonBounds() {
        int panelWidth = getViewWidth();
        int panelHeight = getViewHeight();
        int buttonWidth = 220;
        int buttonHeight = 48;
        int x = (panelWidth - buttonWidth) / 2;
        int y = panelHeight / 2 + 10;
        return new Rectangle(x, y, buttonWidth, buttonHeight);
    }

    private Rectangle getStoryModeButtonBounds() {
        Rectangle free = getArcadeModeButtonBounds();
        return new Rectangle(free.x, free.y + 62, free.width, free.height);
    }

    private Rectangle getMissionModeButtonBounds() {
        Rectangle story = getStoryModeButtonBounds();
        return new Rectangle(story.x, story.y + 62, story.width, story.height);
    }

    private Rectangle getArcadeModeButtonBounds() {
        Rectangle free = getFreeModeButtonBounds();
        return new Rectangle(free.x, free.y + 62, free.width, free.height);
    }

    private Rectangle getProtectionModeButtonBounds() {
        Rectangle mission = getMissionModeButtonBounds();
        return new Rectangle(mission.x, mission.y + 62, mission.width, mission.height);
    }

    private Rectangle getOptionsButtonBounds() {
        Rectangle protection = getProtectionModeButtonBounds();
        return new Rectangle(protection.x, protection.y + 62, protection.width, protection.height);
    }

    private Rectangle getDifficultyButtonBounds(int index) {
        int panelWidth = getViewWidth();
        int buttonWidth = 130;
        int buttonHeight = 34;
        int totalWidth = buttonWidth * 3 + DIFFICULTY_BUTTON_GAP * 2;
        int startX = (panelWidth - totalWidth) / 2;
        int y = 282;
        return new Rectangle(startX + index * (buttonWidth + DIFFICULTY_BUTTON_GAP), y, buttonWidth, buttonHeight);
    }

    private boolean handleDifficultySelectionClick(int mouseX, int mouseY) {
        DifficultyProfile[] profiles = DifficultyProfile.values();
        for (int i = 0; i < profiles.length; i++) {
            if (getDifficultyButtonBounds(i).contains(mouseX, mouseY)) {
                applyDifficultyProfile(profiles[i]);
                return true;
            }
        }
        return false;
    }

    private Rectangle getMapCardBounds(int index, int total) {
        int panelWidth = getViewWidth();
        int cardHeight = 130;
        int maxCardWidth = 250;
        int availableWidth = panelWidth - 120 - (total - 1) * MENU_MAP_CARD_GAP;
        int cardWidth = Math.max(150, Math.min(maxCardWidth, availableWidth / total));
        int totalWidth = total * cardWidth + (total - 1) * MENU_MAP_CARD_GAP;
        int startX = (panelWidth - totalWidth) / 2;
        int y = 140;
        return new Rectangle(startX + index * (cardWidth + MENU_MAP_CARD_GAP), y, cardWidth, cardHeight);
    }

    private Rectangle getMapPreviewImageBounds(Rectangle cardBounds) {
        int margin = 8;
        return new Rectangle(
                cardBounds.x + margin,
                cardBounds.y + margin,
                cardBounds.width - margin * 2,
                cardBounds.height - margin * 2 - 26
        );
    }

    private boolean handleMapSelectionClick(int mouseX, int mouseY) {
        MapType[] mapTypes = tileManager.getAvailableMapTypes();
        for (int i = 0; i < mapTypes.length; i++) {
            Rectangle card = getMapCardBounds(i, mapTypes.length);
            if (card.contains(mouseX, mouseY)) {
                tileManager.setCurrentMapType(mapTypes[i]);
                return true;
            }
        }

        return false;
    }

    private Rectangle getBackButtonBounds() {
        int panelWidth = getViewWidth();
        int panelHeight = getViewHeight();
        int buttonWidth = 220;
        int buttonHeight = 52;
        int x = (panelWidth - buttonWidth) / 2;
        int y = panelHeight - 62;
        return new Rectangle(x, y, buttonWidth, buttonHeight);
    }

    private Rectangle getAiMinusButtonBounds(int rowIndex) {
        int panelWidth = getViewWidth();
        int x = panelWidth - 210;
        int y = AI_OPTIONS_BASE_Y + rowIndex * AI_OPTION_ROW_GAP - 22;
        return new Rectangle(x, y, 34, 26);
    }

    private Rectangle getAiPlusButtonBounds(int rowIndex) {
        int panelWidth = getViewWidth();
        int x = panelWidth - 168;
        int y = AI_OPTIONS_BASE_Y + rowIndex * AI_OPTION_ROW_GAP - 22;
        return new Rectangle(x, y, 34, 26);
    }

    private boolean handleAiOptionsClick(int mouseX, int mouseY) {
        for (int i = 0; i < AI_OPTION_COUNT; i++) {
            if (getAiMinusButtonBounds(i).contains(mouseX, mouseY)) {
                adjustAiOption(i, -1);
                return true;
            }
            if (getAiPlusButtonBounds(i).contains(mouseX, mouseY)) {
                adjustAiOption(i, 1);
                return true;
            }
        }

        return false;
    }

    private void adjustAiOption(int rowIndex, int direction) {
        boolean affectsDifficulty = rowIndex >= 0 && rowIndex <= 8;
        if (affectsDifficulty) {
            currentDifficulty = DifficultyProfile.CUSTOM;
        }

        switch (rowIndex) {
            case 0 -> nombreSoldat = Math.max(0, Math.min(10, nombreSoldat + direction));
            case 1 -> AiTuning.adjustEnemyReactionFrames(direction);
            case 2 -> AiTuning.adjustSoldierReactionFrames(direction);
            case 3 -> AiTuning.adjustEnemyAimStabilizationFrames(direction);
            case 4 -> AiTuning.adjustSoldierAimStabilizationFrames(direction);
            case 5 -> AiTuning.adjustSuppressionDurationFrames(direction * 5);
            case 6 -> AiTuning.adjustSuppressionCoverBoost(direction * 0.05);
            case 7 -> AiTuning.adjustSuppressionNearMissRadius(direction * 4.0);
            case 8 -> {
                AiTuning.toggleAlienPackAggro();
                if (!AiTuning.isAlienPackAggroEnabled()) {
                    ObjectManager.clearAlienAggro();
                }
            }
            case 9 -> unlimitedAmmoEnabled = !unlimitedAmmoEnabled;
            case 10 -> fireCameraFeedbackEnabled = !fireCameraFeedbackEnabled;
            case 11 -> deathMarkersEnabled = !deathMarkersEnabled;
            default -> {
            }
        }
    }

    public static boolean isUnlimitedAmmoEnabled() {
        return unlimitedAmmoEnabled;
    }

    public static boolean isFireCameraFeedbackEnabled() {
        return fireCameraFeedbackEnabled;
    }

    public static boolean areDeathMarkersEnabled() {
        return deathMarkersEnabled;
    }

    private Rectangle getReplayButtonBounds() {
        int panelWidth = getViewWidth();
        int panelHeight = getViewHeight();
        int buttonWidth = 240;
        int buttonHeight = 56;
        int x = (panelWidth - buttonWidth) / 2;
        int y = panelHeight / 2 + 30;
        return new Rectangle(x, y, buttonWidth, buttonHeight);
    }

    private Soldat findSoldat() {
        return ObjectManager.getSoldat();
    }

    private void applySoldierMoveCommand() {
        if (!keyController.consumeRightClickTriggered()) {
            return;
        }

        Soldat soldat = findSoldat();
        if (soldat == null) {
            return;
        }

        double worldX = keyController.getRightClickX() + tileManager.getCameraX();
        double worldY = keyController.getRightClickY() + tileManager.getCameraY();
        soldat.moveTo(worldX, worldY);
    }

    private void updateCamera() {
        Protagonist protagonist = ObjectManager.getProtagonist();
        if (protagonist == null) {
            return;
        }

        int viewWidth = getViewWidth();
        int viewHeight = getViewHeight();
        tileManager.centerCameraOn(protagonist.x, protagonist.y, viewWidth, viewHeight);
    }

    private Protagonist getRenderedPlayer() {
        Protagonist override = renderedPlayerOverride.get();
        return override != null ? override : ObjectManager.getProtagonist();
    }

    private int getViewWidth() {
        return getWidth() > 0 ? getWidth() : 800;
    }

    private int getViewHeight() {
        return getHeight() > 0 ? getHeight() : 600;
    }

    private double getExtractionX() {
        return tileManager.getWorldWidth() - tileSize * EXTRACTION_MARGIN_TILES;
    }

    private void drawExtractionZone(Graphics2D g2d) {
        if (GameMode.current != GameMode.PROTECTION || screenState != ScreenState.PLAYING) {
            return;
        }

        int zoneX = (int) Math.round(getExtractionX());
        int zoneWidth = (int) Math.round(tileSize * 1.5);
        int zoneHeight = (int) Math.round(tileManager.getWorldHeight());

        g2d.setColor(new Color(90, 210, 110, 70));
        g2d.fillRect(zoneX, 0, zoneWidth, zoneHeight);
        g2d.setColor(new Color(170, 255, 190, 180));
        g2d.drawRect(zoneX, 0, zoneWidth, zoneHeight);
        g2d.drawString("EXTRACTION", zoneX + 8, 22);
    }

    private void drawMissionWorldMarkers(Graphics2D g2d) {
        if (GameMode.current != GameMode.MISSION || screenState != ScreenState.PLAYING || activeMissionType == null) {
            return;
        }

        if (missionPrimaryZone != null) {
            g2d.setColor(new Color(110, 210, 255, 52));
            g2d.fill(missionPrimaryZone);
            g2d.setColor(new Color(140, 228, 255, 180));
            g2d.setStroke(new BasicStroke(2f));
            g2d.draw(missionPrimaryZone);

            if (activeMissionType == MissionType.RESCUE_HOSTAGES) {
                g2d.drawString("CAPTIFS", (int) Math.round(missionPrimaryZone.x + 10), (int) Math.round(missionPrimaryZone.y + 20));
            }
        }

        if (missionSecondaryZone != null) {
            g2d.setColor(new Color(240, 202, 122, 44));
            g2d.fill(missionSecondaryZone);
            g2d.setColor(new Color(255, 224, 146, 165));
            g2d.setStroke(new BasicStroke(2f));
            g2d.draw(missionSecondaryZone);

            if (activeMissionType == MissionType.RESCUE_HOSTAGES) {
                g2d.drawString("EXFIL", (int) Math.round(missionSecondaryZone.x + 10), (int) Math.round(missionSecondaryZone.y + 20));
            }
        }

        if (activeMissionType == MissionType.RESCUE_HOSTAGES) {
            for (Hostage hostage : missionHostages) {
                if (missionExtractedHostages.contains(hostage)) {
                    continue;
                }
                if (!isObjectAlive(hostage)) {
                    continue;
                }

                boolean freed = hostage.isRescued();
                int radius = 16;
                g2d.setColor(freed ? new Color(120, 255, 170, 170) : new Color(255, 190, 110, 165));
                g2d.fillOval((int) Math.round(hostage.x - radius), (int) Math.round(hostage.y - radius), radius * 2, radius * 2);
                g2d.setColor(freed ? new Color(170, 255, 200) : new Color(255, 218, 155));
                g2d.drawOval((int) Math.round(hostage.x - radius), (int) Math.round(hostage.y - radius), radius * 2, radius * 2);
            }
        }

        if (activeMissionType == MissionType.DESTROY_VEHICLE && missionVehicleX >= 0 && missionVehicleY >= 0) {
            int bodyW = (int) Math.round(tileSize * 1.3);
            int bodyH = (int) Math.round(tileSize * 0.76);
            int x = (int) Math.round(missionVehicleX - bodyW / 2.0);
            int y = (int) Math.round(missionVehicleY - bodyH / 2.0);

            g2d.setColor(new Color(82, 96, 108));
            g2d.fillRoundRect(x, y, bodyW, bodyH, 10, 10);
            g2d.setColor(new Color(125, 142, 156));
            g2d.drawRoundRect(x, y, bodyW, bodyH, 10, 10);
            g2d.setColor(new Color(34, 39, 46));
            g2d.fillRect(x + 7, y + bodyH - 5, 18, 5);
            g2d.fillRect(x + bodyW - 25, y + bodyH - 5, 18, 5);

            if (missionVehicleChargeArmed) {
                float pulse = (float) (0.55 + 0.45 * Math.abs(Math.sin(missionVehicleFuseFrames * 0.23)));
                Composite oldComposite = g2d.getComposite();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulse));
                g2d.setColor(new Color(255, 110, 86));
                g2d.fillOval((int) Math.round(missionVehicleX - 10), (int) Math.round(missionVehicleY - 10), 20, 20);
                g2d.setComposite(oldComposite);
            }
        }
    }

    private boolean isNightMap() {
        return tileManager.getCurrentMapType() == MapType.NIGHT_BLACKOUT;
    }

    private void drawNightOverlay(Graphics2D g2d) {
        if (!isNightMap() || screenState != ScreenState.PLAYING) {
            return;
        }

        int viewWidth = getWidth() > 0 ? getWidth() : 800;
        int viewHeight = getHeight() > 0 ? getHeight() : 600;

        BufferedImage darkness = new BufferedImage(viewWidth, viewHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D darknessG = darkness.createGraphics();
        darknessG.setColor(new Color(0f, 0f, 0f, NIGHT_DARKNESS_ALPHA));
        darknessG.fillRect(0, 0, viewWidth, viewHeight);

        darknessG.setComposite(AlphaComposite.Clear);

        Protagonist protagonist = getRenderedPlayer();
        WorldSnapshot replicated = networkReplicaView ? networkSnapshot : null;
        if (protagonist != null) {
            drawFlashlightCone(
                    darknessG,
                    protagonist.x,
                    protagonist.y,
                    protagonist.getFacingX(),
                    protagonist.getFacingY(),
                    PLAYER_FLASHLIGHT_RANGE,
                    PLAYER_FLASHLIGHT_HALF_ANGLE,
                    42
            );
        } else if (replicated != null) {
            drawFlashlightCone(
                    darknessG,
                    replicated.playerX,
                    replicated.playerY,
                    replicated.playerFacingX,
                    replicated.playerFacingY,
                    PLAYER_FLASHLIGHT_RANGE,
                    PLAYER_FLASHLIGHT_HALF_ANGLE,
                    42
            );
        }

        for (GameObject obj : ObjectManager.list) {
            if (obj instanceof Ennemi ennemi) {
                drawFlashlightCone(
                        darknessG,
                        ennemi.x,
                        ennemi.y,
                        ennemi.getFacingX(),
                        ennemi.getFacingY(),
                        ENEMY_FLASHLIGHT_RANGE,
                        ENEMY_FLASHLIGHT_HALF_ANGLE,
                        20
                );
            } else if (obj instanceof Alien alien) {
                drawFlashlightCone(
                        darknessG,
                        alien.x,
                        alien.y,
                        alien.vx,
                        alien.vy,
                        ENEMY_FLASHLIGHT_RANGE * 0.85,
                        ENEMY_FLASHLIGHT_HALF_ANGLE,
                        16
                );
            }
        }

        darknessG.dispose();
        g2d.drawImage(darkness, 0, 0, null);
    }

    private void drawFlashlightCone(
            Graphics2D g2d,
            double worldX,
            double worldY,
            double dirX,
            double dirY,
            double range,
            double halfAngle,
            int rays
    ) {
        double lenSq = dirX * dirX + dirY * dirY;
        if (lenSq <= 0.00001) {
            dirX = 0;
            dirY = -1;
            lenSq = 1;
        }

        double invLen = 1.0 / Math.sqrt(lenSq);
        dirX *= invLen;
        dirY *= invLen;

        double baseAngle = Math.atan2(dirY, dirX);
        int cameraX = tileManager.getCameraX();
        int cameraY = tileManager.getCameraY();
        double screenX = worldX - cameraX;
        double screenY = worldY - cameraY;

        Path2D.Double cone = new Path2D.Double();
        cone.moveTo(screenX, screenY);
        for (int i = 0; i <= rays; i++) {
            double t = i / (double) rays;
            double angle = baseAngle - halfAngle + t * halfAngle * 2.0;
            double rayDirX = Math.cos(angle);
            double rayDirY = Math.sin(angle);
            double hitDistance = range;

            for (double rayDistance = FLASHLIGHT_RAY_STEP; rayDistance <= range; rayDistance += FLASHLIGHT_RAY_STEP) {
                double sampleWorldX = worldX + rayDirX * rayDistance;
                double sampleWorldY = worldY + rayDirY * rayDistance;
                if (tileManager.isBlockedAtPixel(sampleWorldX, sampleWorldY)) {
                    hitDistance = Math.max(FLASHLIGHT_RAY_STEP, rayDistance - FLASHLIGHT_RAY_STEP * 0.55);
                    break;
                }
            }

            double px = screenX + rayDirX * hitDistance;
            double py = screenY + rayDirY * hitDistance;
            cone.lineTo(px, py);
        }
        cone.closePath();
        g2d.fill(cone);

        int centerRadius = 40;
        g2d.fillOval(
                (int) Math.round(screenX - centerRadius),
                (int) Math.round(screenY - centerRadius),
                centerRadius * 2,
                centerRadius * 2
        );
    }

    private void drawPanelBackground(Graphics2D g2d, int x, int y, int width, int height, Color fill, Color border) {
        g2d.setColor(fill);
        g2d.fillRoundRect(x, y, width, height, HUD_PANEL_ARC, HUD_PANEL_ARC);
        g2d.setColor(border);
        g2d.setStroke(new BasicStroke(1.8f));
        g2d.drawRoundRect(x, y, width, height, HUD_PANEL_ARC, HUD_PANEL_ARC);
    }

    private void drawTopStatusBar(Graphics2D g2d) {
        int x = 16;
        int y = 14;
        int width = 330;
        int height = GameMode.current == GameMode.MISSION ? 118 : 86;

        drawPanelBackground(g2d, x, y, width, height, new Color(8, 14, 24, 190), new Color(108, 160, 220, 170));

        Font oldFont = g2d.getFont();
        g2d.setColor(new Color(136, 195, 255));
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 14f));
        g2d.drawString(getModeDisplayLabel(), x + 16, y + 24);

        g2d.setColor(Color.WHITE);
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 20f));
        g2d.drawString(getTopHudTitle(), x + 16, y + 50);

        g2d.setFont(oldFont.deriveFont(Font.PLAIN, 14f));
        g2d.setColor(new Color(214, 228, 244));
        int infoY = y + 72;
        for (String line : getTopHudDetails()) {
            g2d.drawString(line, x + 16, infoY);
            infoY += 18;
        }

        g2d.setFont(oldFont);
    }

    private String getModeDisplayLabel() {
        String modeLabel = switch (GameMode.current) {
            case FREE -> "MODE LIBRE";
            case ARCADE -> "MODE ARCADE";
            case STORY -> "MODE HISTOIRE";
            case PROTECTION -> "MODE PROTECTION";
            case MISSION -> "MODE MISSION";
        };
        return modeLabel + " - " + currentDifficulty.getLabel().toUpperCase();
    }

    private String getTopHudTitle() {
        if (GameMode.current == GameMode.MISSION && activeMissionTitle != null && !activeMissionTitle.isEmpty()) {
            return activeMissionTitle;
        }
        if (GameMode.current == GameMode.PROTECTION) {
            return "Escorter jusqu'a extraction";
        }
        if (GameMode.current == GameMode.ARCADE) {
            return "Pression continue";
        }
        if (GameMode.current == GameMode.STORY) {
            return "Recuperation d'armement";
        }
        return "Survie tactique";
    }

    private String[] getTopHudDetails() {
        Protagonist protagonist = getRenderedPlayer();
        if (GameMode.current == GameMode.MISSION) {
            return getMissionTopHudDetails();
        }
        if (GameMode.current == GameMode.PROTECTION && protagonist != null) {
            int[] counts = ObjectManager.getUiCounts();
            double distanceToExtraction = Math.max(0, getExtractionX() - protagonist.x);
            return new String[]{
                    "Distance extraction : " + (int) Math.round(distanceToExtraction) + " px",
                    "Allies encore en vie : " + (counts[0] + counts[1]),
                    "Maintiens la colonne en mouvement"
            };
        }
        if (GameMode.current == GameMode.ARCADE) {
            return new String[]{
                    "Vague : " + (arcadeWaveIndex + 1),
                    "Renforts dans : " + Math.max(0, arcadeWaveTimer / TARGET_UPS) + " s"
            };
        }
        if (GameMode.current == GameMode.STORY) {
            return new String[]{
                    "Ramasse les armes ennemies",
                    "Conserve tes chargeurs pour progresser"
            };
        }
        return new String[]{
                "Sprint : Shift",
                "Pause : P"
        };
    }

    private String[] getMissionTopHudDetails() {
        if (activeMissionType == null) {
            return new String[]{"Objectif en attente"};
        }

        return switch (activeMissionType) {
            case RESCUE_HOSTAGES -> new String[]{
                    "Gardes en zone : " + countHostilesInZone(missionPrimaryZone),
                    "Otages extraits : " + missionExtractedHostages.size() + "/" + missionHostages.size(),
                    missionFreedHostages.size() < missionHostages.size() ? "Approche et appuie sur E pour delivrer" : "Replie-les vers l'exfiltration"
            };
            case DEFEND_POSITION -> new String[]{
                    "Temps a tenir : " + Math.max(0, missionDefendFramesRemaining / TARGET_UPS) + " s",
                    "Reste dans le perimetre"
            };
            case SECURE_ZONE -> new String[]{
                    "Hostiles dans la zone : " + countHostilesInZone(missionPrimaryZone),
                    "Stabilisation : " + Math.min(100, (missionSecureFrames * 100) / MISSION_SECURE_HOLD_FRAMES) + " %"
            };
            case INFILTRATE -> new String[]{
                    "Aucun contact visuel autorise",
                    "Reste hors des lignes de vue"
            };
            case DESTROY_VEHICLE -> new String[]{
                    missionVehicleChargeArmed
                            ? "Charge activee : " + Math.max(0, missionVehicleFuseFrames / TARGET_UPS) + " s"
                            : "Approche le vehicule et appuie sur E",
                    missionVehicleChargeArmed ? "Decroche avant l'explosion" : "Prepare l'exfiltration"
            };
        };
    }

    private void drawBottomRightHud(Graphics2D g2d) {
        if (networkReplicaView && networkSnapshot != null) {
            drawNetworkPlayerHud(g2d, networkSnapshot);
            return;
        }
        Protagonist protagonist = getRenderedPlayer();
        if (protagonist == null) {
            return;
        }

        int panelWidth = 250;
        int panelHeight = protagonist.isReloading() ? 154 : 136;
        int x = getWidth() - panelWidth - 18;
        int y = getHeight() - panelHeight - 18;
        drawPanelBackground(g2d, x, y, panelWidth, panelHeight, new Color(10, 12, 20, 205), new Color(255, 208, 120, 165));

        Font oldFont = g2d.getFont();
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 13f));
        g2d.setColor(new Color(143, 188, 255));
        g2d.drawString("OPERATEUR", x + 16, y + 22);

        if (protagonist.getCurrentWeapon() != null) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(oldFont.deriveFont(Font.BOLD, 20f));
            g2d.drawString(protagonist.getCurrentWeapon().getName(), x + 16, y + 50);

            g2d.setColor(new Color(255, 222, 140));
            g2d.setFont(oldFont.deriveFont(Font.BOLD, 30f));
            String ammo = protagonist.getCurrentWeapon().getAmmoInMagazine() + " / " + protagonist.getCurrentWeapon().getReserveAmmo();
            g2d.drawString(ammo, x + 16, y + 84);

            g2d.setFont(oldFont.deriveFont(Font.PLAIN, 12f));
            g2d.setColor(new Color(188, 200, 218));
            g2d.drawString("CHARGEUR / RESERVE", x + 18, y + 100);
        }

        int plateBaseX = x + 16;
        int plateY = y + panelHeight - 34;
        for (int i = 0; i < protagonist.getMaxArmorPlates(); i++) {
            int plateX = plateBaseX + i * 24;
            Color fill = i < protagonist.getArmorPlates() ? new Color(108, 214, 255) : new Color(36, 50, 68);
            Color border = i < protagonist.getArmorPlates() ? new Color(184, 243, 255) : new Color(88, 102, 120);
            g2d.setColor(fill);
            g2d.fillRoundRect(plateX, plateY, 18, 18, 5, 5);
            g2d.setColor(border);
            g2d.drawRoundRect(plateX, plateY, 18, 18, 5, 5);
        }
        g2d.setColor(new Color(206, 220, 236));
        g2d.setFont(oldFont.deriveFont(Font.PLAIN, 12f));
        g2d.drawString("PLAQUES", plateBaseX + protagonist.getMaxArmorPlates() * 24 + 4, plateY + 14);

        if (protagonist.isReloading()) {
            int barX = x + 16;
            int barY = y + panelHeight - 60;
            int barWidth = panelWidth - 32;
            int barHeight = 10;
            double progress = protagonist.getReloadProgress();
            g2d.setColor(new Color(32, 40, 52));
            g2d.fillRoundRect(barX, barY, barWidth, barHeight, 8, 8);
            g2d.setColor(new Color(255, 176, 92));
            g2d.fillRoundRect(barX, barY, (int) Math.round(barWidth * progress), barHeight, 8, 8);
            g2d.setColor(new Color(255, 226, 188));
            g2d.drawRoundRect(barX, barY, barWidth, barHeight, 8, 8);
            g2d.drawString("RECHARGEMENT", barX, barY - 4);
        }

        g2d.setFont(oldFont);
    }

    private void drawNetworkPlayerHud(Graphics2D g2d, WorldSnapshot snapshot) {
        int panelWidth = 250;
        int panelHeight = snapshot.reloading ? 154 : 136;
        int x = getWidth() - panelWidth - 18;
        int y = getHeight() - panelHeight - 18;
        drawPanelBackground(g2d, x, y, panelWidth, panelHeight,
                new Color(10, 12, 20, 205), new Color(105, 225, 160, 180));
        Font oldFont = g2d.getFont();
        g2d.setColor(new Color(105, 235, 165));
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 13f));
        g2d.drawString("JOUEUR 2", x + 16, y + 22);
        g2d.setColor(Color.WHITE);
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 20f));
        g2d.drawString(snapshot.weaponName, x + 16, y + 50);
        g2d.setColor(new Color(255, 222, 140));
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 30f));
        g2d.drawString(snapshot.ammo + " / " + snapshot.reserveAmmo, x + 16, y + 84);
        int plateY = y + panelHeight - 34;
        for (int i = 0; i < snapshot.maxArmor; i++) {
            g2d.setColor(i < snapshot.armor ? new Color(108, 214, 255) : new Color(36, 50, 68));
            g2d.fillRoundRect(x + 16 + i * 24, plateY, 18, 18, 5, 5);
        }
        if (snapshot.reloading) {
            g2d.setColor(new Color(32, 40, 52));
            g2d.fillRoundRect(x + 16, y + panelHeight - 60, panelWidth - 32, 10, 8, 8);
            g2d.setColor(new Color(255, 176, 92));
            g2d.fillRoundRect(x + 16, y + panelHeight - 60,
                    (int) Math.round((panelWidth - 32) * snapshot.reloadProgress), 10, 8, 8);
        }
        g2d.setFont(oldFont);
    }

    private void drawReloadAnimation(Graphics2D g2d) {
        if (networkReplicaView && networkSnapshot != null) {
            drawNetworkReloadAnimation(g2d, networkSnapshot);
            return;
        }
        Protagonist protagonist = getRenderedPlayer();
        if (protagonist == null || !protagonist.isReloading()) {
            return;
        }

        int cameraX = tileManager.getCameraX();
        int cameraY = tileManager.getCameraY();
        int screenX = (int) Math.round(protagonist.x - cameraX);
        int screenY = (int) Math.round(protagonist.y - cameraY - 34);
        int radius = 18;
        double progress = protagonist.getReloadProgress();

        Composite oldComposite = g2d.getComposite();
        Stroke oldStroke = g2d.getStroke();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.92f));
        g2d.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(new Color(34, 40, 50, 190));
        g2d.drawOval(screenX - radius, screenY - radius, radius * 2, radius * 2);
        g2d.setColor(new Color(255, 194, 108));
        g2d.drawArc(screenX - radius, screenY - radius, radius * 2, radius * 2, 90, (int) Math.round(-360 * progress));
        g2d.setComposite(oldComposite);
        g2d.setStroke(oldStroke);
    }

    private void drawNetworkReloadAnimation(Graphics2D g2d, WorldSnapshot snapshot) {
        if (!snapshot.reloading) return;
        int screenX = (int) Math.round(snapshot.playerX - tileManager.getCameraX());
        int screenY = (int) Math.round(snapshot.playerY - tileManager.getCameraY() - 34);
        g2d.setColor(new Color(255, 194, 108));
        g2d.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawArc(screenX - 18, screenY - 18, 36, 36, 90,
                (int) Math.round(-360 * snapshot.reloadProgress));
    }

    private void drawUI(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        drawTopStatusBar(g2d);
        drawBottomRightHud(g2d);
        if (lanHost != null) {
            Font oldFont = g2d.getFont();
            g2d.setFont(oldFont.deriveFont(Font.BOLD, 13f));
            String status = lanHost.getStatus();
            FontMetrics metrics = g2d.getFontMetrics();
            int width = metrics.stringWidth(status) + 22;
            int x = (getViewWidth() - width) / 2;
            g2d.setColor(new Color(8, 14, 24, 205));
            g2d.fillRoundRect(x, 10, width, 28, 12, 12);
            g2d.setColor(remoteProtagonist != null ? new Color(120, 255, 160) : new Color(255, 214, 120));
            g2d.drawString(status, x + 11, 29);
            g2d.setFont(oldFont);
        }
    }

    private void drawArcadeOverlay(Graphics2D g2d) {
        if (screenFlashAlpha > 0f) {
            Composite oldComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, screenFlashAlpha));
            g2d.setColor(screenFlashColor);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setComposite(oldComposite);
        }

        if (GameMode.current == GameMode.ARCADE && screenState == ScreenState.PLAYING && arcadeWaveBannerTimer > 0) {
            Font oldFont = g2d.getFont();
            float alpha = Math.min(1f, arcadeWaveBannerTimer / (float) ARCADE_WAVE_BANNER_FRAMES);
            Composite oldComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.28f + alpha * 0.42f));
            g2d.setColor(new Color(255, 142, 84));
            g2d.fillRoundRect(getWidth() / 2 - 160, 28, 320, 48, 18, 18);
            g2d.setComposite(oldComposite);
            g2d.setColor(Color.WHITE);
            g2d.setFont(oldFont.deriveFont(Font.BOLD, 24f));
            String label = "VAGUE " + arcadeWaveIndex;
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(label, (getWidth() - fm.stringWidth(label)) / 2, 59);
            g2d.setFont(oldFont);
        }
    }

    private void drawButton(Graphics2D g2d, Rectangle rect, String label) {
        g2d.setColor(new Color(25, 25, 25, 210));
        g2d.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 14, 14);
        g2d.setColor(new Color(235, 235, 235));
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 14, 14);

        Font oldFont = g2d.getFont();
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 24f));
        FontMetrics fm = g2d.getFontMetrics();
        int textX = rect.x + (rect.width - fm.stringWidth(label)) / 2;
        int textY = rect.y + (rect.height - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(label, textX, textY);
        g2d.setFont(oldFont);
    }

    private void drawSmallButton(Graphics2D g2d, Rectangle rect, String label) {
        g2d.setColor(new Color(30, 30, 30, 220));
        g2d.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 8, 8);
        g2d.setColor(new Color(235, 235, 235));
        g2d.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 8, 8);

        Font oldFont = g2d.getFont();
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 16f));
        FontMetrics fm = g2d.getFontMetrics();
        int textX = rect.x + (rect.width - fm.stringWidth(label)) / 2;
        int textY = rect.y + (rect.height - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(label, textX, textY);
        g2d.setFont(oldFont);
    }

    private void drawAiOptionRow(Graphics2D g2d, int rowIndex, String label, String value) {
        int y = AI_OPTIONS_BASE_Y + rowIndex * AI_OPTION_ROW_GAP;
        g2d.drawString(label, 130, y);
        g2d.drawString(value, 470, y);

        drawSmallButton(g2d, getAiMinusButtonBounds(rowIndex), "-");
        drawSmallButton(g2d, getAiPlusButtonBounds(rowIndex), "+");
    }

    private void drawMenu(Graphics2D g2d) {
        int panelWidth = getWidth() > 0 ? getWidth() : 800;
        int panelHeight = getHeight() > 0 ? getHeight() : 600;

        g2d.setColor(new Color(0, 0, 0, 170));
        g2d.fillRect(0, 0, panelWidth, panelHeight);

        Font oldFont = g2d.getFont();
        g2d.setColor(Color.WHITE);
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 44f));
        String title = "SIM ENGINE";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (panelWidth - fm.stringWidth(title)) / 2, 88);

        g2d.setFont(oldFont.deriveFont(Font.BOLD, 18f));
        String mapTitle = "Choisis ta carte";
        FontMetrics mapFm = g2d.getFontMetrics();
        g2d.drawString(mapTitle, (panelWidth - mapFm.stringWidth(mapTitle)) / 2, 118);

        MapType[] mapTypes = tileManager.getAvailableMapTypes();
        for (int i = 0; i < mapTypes.length; i++) {
            Rectangle card = getMapCardBounds(i, mapTypes.length);
            Rectangle preview = getMapPreviewImageBounds(card);
            boolean selected = mapTypes[i] == tileManager.getCurrentMapType();

            g2d.setColor(new Color(24, 24, 24, 225));
            g2d.fillRoundRect(card.x, card.y, card.width, card.height, 16, 16);
            tileManager.drawPreview(g2d, preview, mapTypes[i]);

            g2d.setColor(selected ? new Color(255, 216, 92) : new Color(225, 225, 225));
            g2d.setStroke(new BasicStroke(selected ? 3f : 2f));
            g2d.drawRoundRect(card.x, card.y, card.width, card.height, 16, 16);

            g2d.setFont(oldFont.deriveFont(Font.BOLD, 15f));
            FontMetrics cardFm = g2d.getFontMetrics();
            String label = mapTypes[i].getDisplayName();
            int labelX = card.x + (card.width - cardFm.stringWidth(label)) / 2;
            int labelY = card.y + card.height - 7;
            g2d.drawString(label, labelX, labelY);
        }

        Font oldDifficultyFont = g2d.getFont();
        g2d.setFont(oldDifficultyFont.deriveFont(Font.BOLD, 16f));
        String difficultyTitle = "Difficulte";
        FontMetrics diffTitleMetrics = g2d.getFontMetrics();
        g2d.setColor(new Color(238, 238, 238));
        g2d.drawString(difficultyTitle, (panelWidth - diffTitleMetrics.stringWidth(difficultyTitle)) / 2, 272);
        drawDifficultyButton(g2d, getDifficultyButtonBounds(0), DifficultyProfile.EASY);
        drawDifficultyButton(g2d, getDifficultyButtonBounds(1), DifficultyProfile.NORMAL);
        drawDifficultyButton(g2d, getDifficultyButtonBounds(2), DifficultyProfile.CUSTOM);
        g2d.setFont(oldDifficultyFont);

        drawButton(g2d, getFreeModeButtonBounds(), "Mode Libre");
        drawButton(g2d, getArcadeModeButtonBounds(), "Mode Arcade");
        drawButton(g2d, getStoryModeButtonBounds(), "Mode Histoire");
        drawButton(g2d, getMissionModeButtonBounds(), "Mode Mission");
        drawButton(g2d, getProtectionModeButtonBounds(), "Mode Protection");
        drawButton(g2d, getOptionsButtonBounds(), "Options");

        g2d.setFont(oldFont.deriveFont(Font.PLAIN, 16f));
        String hint = "Clique sur une carte puis sur un mode";
        FontMetrics hintFm = g2d.getFontMetrics();
        g2d.drawString(hint, (panelWidth - hintFm.stringWidth(hint)) / 2, panelHeight - 26);
        g2d.setFont(oldFont);
    }

    private void drawDifficultyButton(Graphics2D g2d, Rectangle rect, DifficultyProfile profile) {
        boolean selected = currentDifficulty == profile;
        g2d.setColor(selected ? new Color(180, 132, 54, 220) : new Color(32, 32, 32, 218));
        g2d.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 11, 11);
        g2d.setColor(selected ? new Color(255, 226, 148) : new Color(214, 214, 214));
        g2d.setStroke(new BasicStroke(selected ? 2.4f : 1.6f));
        g2d.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 11, 11);

        Font oldFont = g2d.getFont();
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 15f));
        FontMetrics fm = g2d.getFontMetrics();
        String label = profile.getLabel();
        int textX = rect.x + (rect.width - fm.stringWidth(label)) / 2;
        int textY = rect.y + (rect.height - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(label, textX, textY);
        g2d.setFont(oldFont);
    }

    private boolean isEasyDifficulty() {
        return currentDifficulty == DifficultyProfile.EASY;
    }

    private int getAdjustedEnemyCount(int baseCount) {
        if (!isEasyDifficulty()) {
            return baseCount;
        }
        return Math.max(3, (int) Math.ceil(baseCount * 0.65));
    }

    private int getAdjustedAlienCount(int baseCount) {
        if (!isEasyDifficulty()) {
            return baseCount;
        }
        return Math.max(1, (int) Math.ceil(baseCount * 0.60));
    }

    private void applyDifficultyProfile(DifficultyProfile profile) {
        currentDifficulty = profile;
        switch (profile) {
            case EASY -> {
                nombreSoldat = 2;
                AiTuning.applyEasyPreset();
            }
            case NORMAL -> {
                nombreSoldat = 1;
                AiTuning.applyNormalPreset();
            }
            case CUSTOM -> {
            }
        }
    }

    private void drawOptions(Graphics2D g2d) {
        int panelWidth = getViewWidth();
        int panelHeight = getViewHeight();

        g2d.setColor(new Color(0, 0, 0, 185));
        g2d.fillRect(0, 0, panelWidth, panelHeight);

        Font oldFont = g2d.getFont();
        g2d.setColor(Color.WHITE);
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 40f));
        String title = "Options";
        FontMetrics titleFm = g2d.getFontMetrics();
        g2d.drawString(title, (panelWidth - titleFm.stringWidth(title)) / 2, 130);

        g2d.setFont(oldFont.deriveFont(Font.BOLD, 22f));
        g2d.drawString("Options de partie et IA", 130, AI_OPTIONS_BASE_Y - 34);

        g2d.setFont(oldFont.deriveFont(Font.PLAIN, 18f));
        drawAiOptionRow(g2d, 0, "Soldats allies", Integer.toString(nombreSoldat));
        drawAiOptionRow(g2d, 1, "Reaction ennemis (frames)", Integer.toString(AiTuning.getEnemyReactionFrames()));
        drawAiOptionRow(g2d, 2, "Reaction soldats (frames)", Integer.toString(AiTuning.getSoldierReactionFrames()));
        drawAiOptionRow(g2d, 3, "Visee ennemis (frames)", Integer.toString(AiTuning.getEnemyAimStabilizationFrames()));
        drawAiOptionRow(g2d, 4, "Visee soldats (frames)", Integer.toString(AiTuning.getSoldierAimStabilizationFrames()));
        drawAiOptionRow(g2d, 5, "Suppression duree (frames)", Integer.toString(AiTuning.getSuppressionDurationFrames()));
        drawAiOptionRow(g2d, 6, "Suppression bonus couverture", String.format("%.2f", AiTuning.getSuppressionCoverBoost()));
        drawAiOptionRow(g2d, 7, "Suppression rayon (px)", Integer.toString((int) Math.round(AiTuning.getSuppressionNearMissRadius())));
        drawAiOptionRow(g2d, 8, "Aliens meute agressive", AiTuning.isAlienPackAggroEnabled() ? "ON" : "OFF");
        drawAiOptionRow(g2d, 9, "Munitions illimitees", unlimitedAmmoEnabled ? "ON" : "OFF");
        drawAiOptionRow(g2d, 10, "Feedback camera des tirs", fireCameraFeedbackEnabled ? "ON" : "OFF");
        drawAiOptionRow(g2d, 11, "Marqueurs des morts", deathMarkersEnabled ? "ON" : "OFF");

        drawButton(g2d, getBackButtonBounds(), "Retour");
        g2d.setFont(oldFont);
    }

    private void drawPauseOverlay(Graphics2D g2d) {
        int panelWidth = getWidth() > 0 ? getWidth() : 800;
        int panelHeight = getHeight() > 0 ? getHeight() : 600;

        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.fillRect(0, 0, panelWidth, panelHeight);
        g2d.setColor(Color.WHITE);
        Font oldFont = g2d.getFont();
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 48f));
        String pausedText = "PAUSE";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(pausedText, (panelWidth - fm.stringWidth(pausedText)) / 2, panelHeight / 2);
        g2d.setFont(oldFont.deriveFont(Font.PLAIN, 18f));
        String hint = "Appuie sur P pour reprendre";
        FontMetrics hintFm = g2d.getFontMetrics();
        g2d.drawString(hint, (panelWidth - hintFm.stringWidth(hint)) / 2, panelHeight / 2 + 36);
        g2d.setFont(oldFont);

        drawButton(g2d, getPauseMenuButtonBounds(), "Menu principal");
    }

    private void drawGameOverOverlay(Graphics2D g2d) {
        int panelWidth = getWidth() > 0 ? getWidth() : 800;
        int panelHeight = getHeight() > 0 ? getHeight() : 600;

        g2d.setColor(new Color(0, 0, 0, 170));
        g2d.fillRect(0, 0, panelWidth, panelHeight);

        Font oldFont = g2d.getFont();
        g2d.setColor(new Color(255, 80, 80));
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 54f));
        String title = gameOverTitle;
        FontMetrics titleFm = g2d.getFontMetrics();
        g2d.drawString(title, (panelWidth - titleFm.stringWidth(title)) / 2, panelHeight / 2 - 40);

        g2d.setColor(Color.WHITE);
        g2d.setFont(oldFont.deriveFont(Font.PLAIN, 22f));
        String scoreText = "Score final : " + score;
        FontMetrics scoreFm = g2d.getFontMetrics();
        g2d.drawString(scoreText, (panelWidth - scoreFm.stringWidth(scoreText)) / 2, panelHeight / 2);

        drawButton(g2d, getReplayButtonBounds(), "Rejouer");
        g2d.setFont(oldFont);
    }

    private void drawVictoryOverlay(Graphics2D g2d) {
        int panelWidth = getWidth() > 0 ? getWidth() : 800;
        int panelHeight = getHeight() > 0 ? getHeight() : 600;

        g2d.setColor(new Color(0, 0, 0, 165));
        g2d.fillRect(0, 0, panelWidth, panelHeight);

        Font oldFont = g2d.getFont();
        g2d.setColor(new Color(120, 255, 160));
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 52f));
        String title = "MISSION REUSSIE";
        FontMetrics titleFm = g2d.getFontMetrics();
        g2d.drawString(title, (panelWidth - titleFm.stringWidth(title)) / 2, panelHeight / 2 - 44);

        g2d.setColor(Color.WHITE);
        g2d.setFont(oldFont.deriveFont(Font.PLAIN, 22f));
        String scoreText = "Score final : " + score;
        FontMetrics scoreFm = g2d.getFontMetrics();
        g2d.drawString(scoreText, (panelWidth - scoreFm.stringWidth(scoreText)) / 2, panelHeight / 2);

        drawButton(g2d, getReplayButtonBounds(), "Rejouer");
        g2d.setFont(oldFont);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        var old = g2d.getTransform();
        int shakeX = 0;
        int shakeY = 0;
        if (screenShakeFrames > 0 && screenState == ScreenState.PLAYING) {
            shakeX = (int) Math.round((Math.random() * 2.0 - 1.0) * screenShakeAmplitude);
            shakeY = (int) Math.round((Math.random() * 2.0 - 1.0) * screenShakeAmplitude);
        }
        g2d.translate(-tileManager.getCameraX() + shakeX, -tileManager.getCameraY() + shakeY);
        tileManager.draw(g2d);
        drawExtractionZone(g2d);
        drawMissionWorldMarkers(g2d);
        ObjectManager.drawAll(g2d);
        g2d.setTransform(old);

        drawNightOverlay(g2d);
        drawArcadeOverlay(g2d);
        drawReloadAnimation(g2d);

        if (screenState == ScreenState.PLAYING) {
            drawUI(g);
            if (paused) {
                drawPauseOverlay(g2d);
            }
        } else if (screenState == ScreenState.GAME_OVER) {
            drawGameOverOverlay(g2d);
        } else if (screenState == ScreenState.VICTORY) {
            drawVictoryOverlay(g2d);
        } else if (screenState == ScreenState.MENU) {
            drawMenu(g2d);
        } else if (screenState == ScreenState.OPTIONS) {
            drawOptions(g2d);
        }
        if (networkReplicaView && networkSnapshot == null) {
            drawNetworkConnectionOverlay(g2d);
        }
    }

    private void drawNetworkConnectionOverlay(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 205));
        g2d.fillRect(0, 0, getViewWidth(), getViewHeight());
        g2d.setColor(Color.WHITE);
        g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 20f));
        FontMetrics metrics = g2d.getFontMetrics();
        g2d.drawString(networkConnectionStatus,
                (getViewWidth() - metrics.stringWidth(networkConnectionStatus)) / 2,
                getViewHeight() / 2);
    }

    public BufferedImage captureFrame() {
        int width = Math.max(1, getViewWidth());
        int height = Math.max(1, getViewHeight());
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    public BufferedImage captureRemoteFrame() {
        Protagonist remote = remoteProtagonist;
        if (remote == null) return captureFrame();

        int[] camera = tileManager.calculateCameraFor(remote.x, remote.y, getViewWidth(), getViewHeight());
        tileManager.setRenderCameraOverride(camera[0], camera[1]);
        renderedPlayerOverride.set(remote);
        try {
            return captureFrame();
        } finally {
            renderedPlayerOverride.remove();
            tileManager.clearRenderCameraOverride();
        }
    }

    public void paintScore(Graphics g, int scoreHit) {
        super.paintComponent(g);
        g.setColor(Color.RED);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.WHITE);
        g.drawString("Score: " + score, 10, 20);
    }

    public static void triggerScreenShake(int frames, double amplitude) {
        if (activePanel == null) {
            return;
        }

        activePanel.screenShakeFrames = Math.max(activePanel.screenShakeFrames, frames);
        activePanel.screenShakeAmplitude = Math.max(activePanel.screenShakeAmplitude, amplitude);
    }

    public static void triggerScreenFlash(Color color, float alpha, int frames) {
        if (activePanel == null) {
            return;
        }

        activePanel.screenFlashColor = color;
        activePanel.screenFlashAlpha = Math.max(activePanel.screenFlashAlpha, alpha);
        activePanel.screenFlashFrames = Math.max(activePanel.screenFlashFrames, frames);
    }

}
