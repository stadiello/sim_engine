package main;

import javax.swing.*;
import java.awt.*;
import java.awt.KeyboardFocusManager;

import gameController.GameKeyController;
import object.ai.AiTuning;
import object.Alien;
import object.Ennemi;
import object.Homme;
import object.Protagonist;
import object.Soldat;
import object.ObjectManager;
import world.TileManager;

public class GamePanel extends JPanel implements Runnable {

    private static final int TARGET_UPS = 60;
    private static final long NANOS_PER_UPDATE = 1_000_000_000L / TARGET_UPS;

    private enum ScreenState {
        MENU,
        OPTIONS,
        PLAYING,
        GAME_OVER
    }

    private int nombreCivil = 5;
    private int nombreEnnemi = 10;
    private int nombreAlien = 5;

    // 50 correspond aux dimensions de la carte (800÷16=50, 600÷12=50), ce qui permet un affichage correct des tiles sur l'écran 800×600.
    public int tileSize = 50;
    private static final double ENTITY_RADIUS = 14;
    private static final int MAX_SPAWN_ATTEMPTS = 300;
    private static final double SAFE_HOSTILE_SPAWN_DISTANCE = 520;
    private static final int AI_OPTIONS_BASE_Y = 292;
    private static final int AI_OPTION_ROW_GAP = 34;
    private static final int AI_OPTION_COUNT = 7;

    TileManager tileManager = new TileManager(this);
    private final GameKeyController keyController = new GameKeyController();
    private ScreenState screenState = ScreenState.MENU;
    private boolean paused = false;

    public static int score = 0;

    public GamePanel() {
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

        // Cree une petite population initiale.
        for (int i = 0; i < nombreCivil; i++) {
            double[] spawn = getFreeSpawnPosition();
            ObjectManager.list.add(new Homme(spawn[0], spawn[1]));
        }
        double[] protagonistSpawn = getFreeSpawnPositionInZone(5, Math.min(8, tileManager.getMapCols() - 2), 4, Math.min(7, tileManager.getMapRows() - 2));
        double protagonistSpawnX = protagonistSpawn[0];
        double protagonistSpawnY = protagonistSpawn[1];
        ObjectManager.list.add(new Protagonist(protagonistSpawn[0], protagonistSpawn[1], keyController));

        // Garde un soldat allié près du protagoniste mais dans la même zone sécurisée.
        double[] soldatSpawn = getFreeSpawnPositionInZone(5, Math.min(8, tileManager.getMapCols() - 2), 4, Math.min(7, tileManager.getMapRows() - 2));
        ObjectManager.list.add(new Soldat(soldatSpawn[0], soldatSpawn[1]));

        for (int i = 0; i < nombreAlien; i++) {
            double[] spawn = getFreeSpawnPositionFarFrom(
                    protagonistSpawnX,
                    protagonistSpawnY,
                    SAFE_HOSTILE_SPAWN_DISTANCE
            );
            ObjectManager.list.add(new Alien(spawn[0], spawn[1]));
        }

        int[][] enemySpawnZones = buildEnemySpawnZones();
        for (int i = 0; i < nombreEnnemi; i++) {
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
            ObjectManager.list.add(new Ennemi(spawn[0], spawn[1]));
        }
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
                && !tileManager.isBlockedAtPixel(x + radius, y + radius);
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

        if (screenState == ScreenState.PLAYING && !paused) {
            applySoldierMoveCommand();
            ObjectManager.updateAll();
            if (ObjectManager.getProtagonist() == null) {
                screenState = ScreenState.GAME_OVER;
                paused = false;
            }
            updateCamera();
        }
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
            if (getFreeModeButtonBounds().contains(mouseX, mouseY)) {
                GameMode.current = GameMode.FREE;
                initializeWorld();
                screenState = ScreenState.PLAYING;
                paused = false;
            } else if (getStoryModeButtonBounds().contains(mouseX, mouseY)) {
                GameMode.current = GameMode.STORY;
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

        if (screenState == ScreenState.GAME_OVER && getReplayButtonBounds().contains(mouseX, mouseY)) {
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
        int buttonHeight = 52;
        int x = (panelWidth - buttonWidth) / 2;
        int y = panelHeight / 2 - 60;
        return new Rectangle(x, y, buttonWidth, buttonHeight);
    }

    private Rectangle getStoryModeButtonBounds() {
        Rectangle free = getFreeModeButtonBounds();
        return new Rectangle(free.x, free.y + 72, free.width, free.height);
    }

    private Rectangle getOptionsButtonBounds() {
        Rectangle story = getStoryModeButtonBounds();
        return new Rectangle(story.x, story.y + 72, story.width, story.height);
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
            case 0 -> AiTuning.adjustEnemyReactionFrames(direction);
            case 1 -> AiTuning.adjustSoldierReactionFrames(direction);
            case 2 -> AiTuning.adjustEnemyAimStabilizationFrames(direction);
            case 3 -> AiTuning.adjustSoldierAimStabilizationFrames(direction);
            case 4 -> AiTuning.adjustSuppressionDurationFrames(direction * 5);
            case 5 -> AiTuning.adjustSuppressionCoverBoost(direction * 0.05);
            case 6 -> AiTuning.adjustSuppressionNearMissRadius(direction * 4.0);
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

    private void drawUI(Graphics g) {
        // Affiche le compteur des entites encore en vie.
        int[] counts = ObjectManager.getUiCounts();
        int civils = counts[0];
        int soldats = counts[1];
        int aliens = counts[2];

        g.setColor(Color.WHITE);
        g.drawString("Civils: " + civils, 10, 20);
        g.drawString("Soldats: " + soldats, 10, 40);
        g.drawString("Aliens: " + aliens, 10, 60);
        g.drawString("Casualties: " + (8 - civils - soldats), 10, 80);
        g.drawString("P: Pause", 10, 100);
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
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 48f));
        String title = "SIM ENGINE";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (panelWidth - fm.stringWidth(title)) / 2, panelHeight / 2 - 100);

        drawButton(g2d, getFreeModeButtonBounds(), "Mode Libre");
        drawButton(g2d, getStoryModeButtonBounds(), "Mode Histoire");
        drawButton(g2d, getOptionsButtonBounds(), "Options");

        g2d.setFont(oldFont.deriveFont(Font.PLAIN, 16f));
        String hint = "Clique sur un bouton pour continuer";
        FontMetrics hintFm = g2d.getFontMetrics();
        g2d.drawString(hint, (panelWidth - hintFm.stringWidth(hint)) / 2, panelHeight - 40);
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

        g2d.setFont(oldFont.deriveFont(Font.PLAIN, 20f));
        g2d.drawString("- P : mettre le jeu en pause/reprendre", 130, 190);
        g2d.drawString("- Clic droit : envoyer le soldat", 130, 220);
        g2d.drawString("- Clic gauche : tirer", 130, 250);

        g2d.setFont(oldFont.deriveFont(Font.BOLD, 22f));
        g2d.drawString("Constantes IA", 130, AI_OPTIONS_BASE_Y - 34);

        g2d.setFont(oldFont.deriveFont(Font.PLAIN, 18f));
        drawAiOptionRow(g2d, 0, "Reaction ennemis (frames)", Integer.toString(AiTuning.getEnemyReactionFrames()));
        drawAiOptionRow(g2d, 1, "Reaction soldats (frames)", Integer.toString(AiTuning.getSoldierReactionFrames()));
        drawAiOptionRow(g2d, 2, "Visee ennemis (frames)", Integer.toString(AiTuning.getEnemyAimStabilizationFrames()));
        drawAiOptionRow(g2d, 3, "Visee soldats (frames)", Integer.toString(AiTuning.getSoldierAimStabilizationFrames()));
        drawAiOptionRow(g2d, 4, "Suppression duree (frames)", Integer.toString(AiTuning.getSuppressionDurationFrames()));
        drawAiOptionRow(g2d, 5, "Suppression bonus couverture", String.format("%.2f", AiTuning.getSuppressionCoverBoost()));
        drawAiOptionRow(g2d, 6, "Suppression rayon (px)", Integer.toString((int) Math.round(AiTuning.getSuppressionNearMissRadius())));

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
        String title = "VOUS ETES MORT";
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

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        var old = g2d.getTransform();
        g2d.translate(-tileManager.getCameraX(), -tileManager.getCameraY());
        tileManager.draw(g2d);
        ObjectManager.drawAll(g2d);
        g2d.setTransform(old);

        if (screenState == ScreenState.PLAYING) {
            drawUI(g);
            if (paused) {
                drawPauseOverlay(g2d);
            }
        } else if (screenState == ScreenState.GAME_OVER) {
            drawGameOverOverlay(g2d);
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

}
