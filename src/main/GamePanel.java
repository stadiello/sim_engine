package main;

import javax.swing.*;
import java.awt.*;
import java.awt.KeyboardFocusManager;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

import gameController.GameKeyController;
import object.GameObject;
import object.ai.AiTuning;
import object.Alien;
import object.AlienTeleport;
import object.Ennemi;
import object.Ennemi.EnemyArchetype;
import object.Homme;
import object.Protagonist;
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

    private enum ScreenState {
        MENU,
        OPTIONS,
        PLAYING,
        GAME_OVER,
        VICTORY
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
    private static final int AI_OPTIONS_BASE_Y = 292;
    private static final int AI_OPTION_ROW_GAP = 34;
    private static final int AI_OPTION_COUNT = 9;
    private static final int MENU_MAP_CARD_GAP = 26;
    private static final float NIGHT_DARKNESS_ALPHA = 0.86f;
    private static final double PLAYER_FLASHLIGHT_RANGE = 320;
    private static final double PLAYER_FLASHLIGHT_HALF_ANGLE = Math.toRadians(35);
    private static final double ENEMY_FLASHLIGHT_RANGE = 230;
    private static final double ENEMY_FLASHLIGHT_HALF_ANGLE = Math.toRadians(25);
    private static final double EXTRACTION_MARGIN_TILES = 1.5;
    private static final double FLASHLIGHT_RAY_STEP = 8.0;
    private static GamePanel activePanel;

    TileManager tileManager = new TileManager(this);
    private final GameKeyController keyController = new GameKeyController();
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

    private void initializeWorld() {
        ObjectManager.list.clear();
        score = 0;
        gameOverTitle = "VOUS ETES MORT";
        resetArcadeState();

        int civilsToSpawn = nombreCivil;
        int soldatsToSpawn = nombreSoldat;
        if (GameMode.current == GameMode.PROTECTION) {
            civilsToSpawn = Math.max(4, nombreCivil);
            soldatsToSpawn = Math.max(1, nombreSoldat);
        } else if (GameMode.current == GameMode.ARCADE) {
            civilsToSpawn = Math.max(1, Math.min(2, nombreCivil));
            soldatsToSpawn = Math.max(1, nombreSoldat);
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

        int aliensToSpawn = GameMode.current == GameMode.ARCADE ? Math.max(2, nombreAlien - 1) : nombreAlien;
        for (int i = 0; i < aliensToSpawn; i++) {
            double[] spawn = getFreeSpawnPositionFarFrom(
                    protagonistSpawnX,
                    protagonistSpawnY,
                    SAFE_HOSTILE_SPAWN_DISTANCE
            );
            ObjectManager.list.add(new AlienTeleport(spawn[0], spawn[1]));
        }

        int enemyCount = GameMode.current == GameMode.ARCADE ? Math.max(8, nombreEnnemi - 1) : nombreEnnemi;
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

    private EnemyArchetype chooseEnemyArchetype(int index, int total) {
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
        JFrame frame = new JFrame("Simulation");
        GamePanel gamePanel = new GamePanel();
        frame.add(gamePanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(gamePanel.keyController);
        frame.setVisible(true);
        new Thread(gamePanel).start();
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
            applySoldierMoveCommand();
            ObjectManager.updateAll();
            maintainAlienPresence();
            updateArcadeMode();
            Protagonist protagonist = ObjectManager.getProtagonist();
            if (protagonist == null) {
                gameOverTitle = "VOUS ETES MORT";
                screenState = ScreenState.GAME_OVER;
                paused = false;
                return;
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

    private void maintainAlienPresence() {
        if (alienRespawnCooldownFrames > 0) {
            alienRespawnCooldownFrames--;
            return;
        }

        int aliveAliens = ObjectManager.getUiCounts()[2];
        int desiredAliens = GameMode.current == GameMode.ARCADE ? Math.max(2, nombreAlien) : Math.max(1, nombreAlien);
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
        double pressure = arcadeWaveIndex + index / (double) Math.max(1, waveSize);
        if (pressure < 1.5) {
            return EnemyArchetype.STANDARD;
        }
        if (pressure < 3.2) {
            return index % 3 == 0 ? EnemyArchetype.ASSAUT : EnemyArchetype.FLANQUEUR;
        }
        if (pressure < 5.0) {
            return index % 4 == 0 ? EnemyArchetype.LOURD : EnemyArchetype.ASSAUT;
        }
        if (index % 5 == 0) {
            return EnemyArchetype.LOURD;
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
        int panelWidth = getWidth() > 0 ? getWidth() : 800;
        int panelHeight = getHeight() > 0 ? getHeight() : 600;
        int buttonWidth = 220;
        int buttonHeight = 48;
        int x = (panelWidth - buttonWidth) / 2;
        int y = panelHeight / 2 + 70;
        return new Rectangle(x, y, buttonWidth, buttonHeight);
    }

    private Rectangle getFreeModeButtonBounds() {
        int panelWidth = getWidth() > 0 ? getWidth() : 800;
        int panelHeight = getHeight() > 0 ? getHeight() : 600;
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

    private Rectangle getArcadeModeButtonBounds() {
        Rectangle free = getFreeModeButtonBounds();
        return new Rectangle(free.x, free.y + 62, free.width, free.height);
    }

    private Rectangle getProtectionModeButtonBounds() {
        Rectangle story = getStoryModeButtonBounds();
        return new Rectangle(story.x, story.y + 62, story.width, story.height);
    }

    private Rectangle getOptionsButtonBounds() {
        Rectangle protection = getProtectionModeButtonBounds();
        return new Rectangle(protection.x, protection.y + 62, protection.width, protection.height);
    }

    private Rectangle getMapCardBounds(int index, int total) {
        int panelWidth = getWidth() > 0 ? getWidth() : 800;
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
        int panelWidth = getWidth() > 0 ? getWidth() : 800;
        int panelHeight = getHeight() > 0 ? getHeight() : 600;
        int buttonWidth = 220;
        int buttonHeight = 52;
        int x = (panelWidth - buttonWidth) / 2;
        int y = panelHeight - 120;
        return new Rectangle(x, y, buttonWidth, buttonHeight);
    }

    private Rectangle getAiMinusButtonBounds(int rowIndex) {
        int panelWidth = getWidth() > 0 ? getWidth() : 800;
        int x = panelWidth - 210;
        int y = AI_OPTIONS_BASE_Y + rowIndex * AI_OPTION_ROW_GAP - 22;
        return new Rectangle(x, y, 34, 26);
    }

    private Rectangle getAiPlusButtonBounds(int rowIndex) {
        int panelWidth = getWidth() > 0 ? getWidth() : 800;
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
            default -> {
            }
        }
    }

    private Rectangle getReplayButtonBounds() {
        int panelWidth = getWidth() > 0 ? getWidth() : 800;
        int panelHeight = getHeight() > 0 ? getHeight() : 600;
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

        int viewWidth = getWidth() > 0 ? getWidth() : 800;
        int viewHeight = getHeight() > 0 ? getHeight() : 600;
        tileManager.centerCameraOn(protagonist.x, protagonist.y, viewWidth, viewHeight);
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

        Protagonist protagonist = ObjectManager.getProtagonist();
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

    private void drawUI(Graphics g) {
        // Affiche le compteur des entites encore en vie.
        int[] counts = ObjectManager.getUiCounts();
        int civils = counts[0];
        int soldats = counts[1];
        int aliens = counts[2];
        int initialAllies = Math.max(1, nombreCivil + nombreSoldat);

        g.setColor(Color.WHITE);
        g.drawString("Civils: " + civils, 10, 20);
        g.drawString("Soldats: " + soldats, 10, 40);
        g.drawString("Aliens: " + aliens, 10, 60);
        g.drawString("Casualties: " + Math.max(0, initialAllies - civils - soldats), 10, 80);
        g.drawString("P: Pause", 10, 100);
        g.drawString("Shift: Sprint", 10, 120);

        Protagonist protagonist = ObjectManager.getProtagonist();
        if (protagonist != null) {
            g.setColor(new Color(145, 220, 255));
            g.drawString("Plaques: " + protagonist.getArmorPlates(), 10, 140);
            g.setColor(Color.WHITE);
        }

        if (GameMode.current == GameMode.ARCADE) {
            g.setColor(new Color(255, 215, 110));
            g.drawString("Arcade: vague " + (arcadeWaveIndex + 1), 10, 165);
            g.drawString("Renforts dans: " + Math.max(0, arcadeWaveTimer / TARGET_UPS) + "s", 10, 185);
            g.setColor(Color.WHITE);
        }

        if (GameMode.current == GameMode.PROTECTION) {
            if (protagonist != null) {
                double distanceToExtraction = Math.max(0, getExtractionX() - protagonist.x);
                g.setColor(new Color(255, 231, 133));
                int baseY = GameMode.current == GameMode.ARCADE ? 210 : 165;
                g.drawString("Protection: escorte jusqu'a la zone d'extraction", 10, baseY);
                g.drawString("Distance extraction: " + (int) Math.round(distanceToExtraction) + " px", 10, baseY + 20);
                g.drawString("Allies a proteger: " + (civils + soldats), 10, baseY + 40);
            }
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

        drawButton(g2d, getFreeModeButtonBounds(), "Mode Libre");
        drawButton(g2d, getArcadeModeButtonBounds(), "Mode Arcade");
        drawButton(g2d, getStoryModeButtonBounds(), "Mode Histoire");
        drawButton(g2d, getProtectionModeButtonBounds(), "Mode Protection");
        drawButton(g2d, getOptionsButtonBounds(), "Options");

        g2d.setFont(oldFont.deriveFont(Font.PLAIN, 16f));
        String hint = "Clique sur une carte puis sur un mode";
        FontMetrics hintFm = g2d.getFontMetrics();
        g2d.drawString(hint, (panelWidth - hintFm.stringWidth(hint)) / 2, panelHeight - 26);
        g2d.setFont(oldFont);
    }

    private void drawOptions(Graphics2D g2d) {
        int panelWidth = getWidth() > 0 ? getWidth() : 800;
        int panelHeight = getHeight() > 0 ? getHeight() : 600;

        g2d.setColor(new Color(0, 0, 0, 185));
        g2d.fillRect(0, 0, panelWidth, panelHeight);

        Font oldFont = g2d.getFont();
        g2d.setColor(Color.WHITE);
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 40f));
        String title = "Options";
        FontMetrics titleFm = g2d.getFontMetrics();
        g2d.drawString(title, (panelWidth - titleFm.stringWidth(title)) / 2, 130);

        // g2d.setFont(oldFont.deriveFont(Font.PLAIN, 20f));
        // g2d.drawString("- P : mettre le jeu en pause/reprendre", 130, 190);
        // g2d.drawString("- Clic droit : envoyer le soldat", 130, 220);
        // g2d.drawString("- Clic gauche : tirer", 130, 250);

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
        ObjectManager.drawAll(g2d);
        g2d.setTransform(old);

        drawNightOverlay(g2d);
        drawArcadeOverlay(g2d);

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
