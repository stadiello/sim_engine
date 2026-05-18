package main;

import javax.swing.*;
import java.awt.*;
import java.awt.KeyboardFocusManager;

import gameController.GameKeyController;
import object.Alien;
import object.Ennemi;
import object.GameObject;
import object.Homme;
import object.Protagonist;
import object.Soldat;
import object.ObjectManager;
import world.TileManager;

public class GamePanel extends JPanel implements Runnable {

    // 50 correspond aux dimensions de la carte (800÷16=50, 600÷12=50), ce qui permet un affichage correct des tiles sur l'écran 800×600.
    public int tileSize = 50;
    private static final double ENTITY_RADIUS = 14;
    private static final int MAX_SPAWN_ATTEMPTS = 300;
    private static final double SAFE_HOSTILE_SPAWN_DISTANCE = 520;

    TileManager tileManager = new TileManager(this);
    private final GameKeyController keyController = new GameKeyController();

    public static int score = 0;

    public GamePanel() {
        ObjectManager.setTileManager(tileManager);
        addMouseMotionListener(keyController);
        addMouseListener(keyController);
        addMouseWheelListener(keyController);

        // Cree une petite population initiale.
        for (int i = 0; i < 6; i++) {
            double[] spawn = getFreeSpawnPosition();
            ObjectManager.list.add(new Homme(spawn[0], spawn[1]));
        }
        double[] protagonistSpawn = getFreeSpawnPositionInZone(5, Math.min(8, tileManager.getMapCols() - 2), 4, Math.min(7, tileManager.getMapRows() - 2));
        double protagonistSpawnX = protagonistSpawn[0];
        double protagonistSpawnY = protagonistSpawn[1];
        ObjectManager.list.add(new Protagonist(protagonistSpawn[0], protagonistSpawn[1], keyController));

        // Garde un soldat allié près du protagoniste mais dans la même zone sécurisée.
        double[] soldatSpawn = getFreeSpawnPositionInZone(5, Math.min(8, tileManager.getMapCols() - 2), 4, Math.min(7, tileManager.getMapRows() - 2));
        // ObjectManager.list.add(new Soldat(soldatSpawn[0], soldatSpawn[1]));
        for (int i = 0; i < 5; i++) {
            double[] spawn = getFreeSpawnPositionFarFrom(
                    protagonistSpawnX,
                    protagonistSpawnY,
                    SAFE_HOSTILE_SPAWN_DISTANCE
            );
            ObjectManager.list.add(new Alien(spawn[0], spawn[1]));
        }

        int[][] enemySpawnZones = buildEnemySpawnZones();
        for (int i = 0; i < 4; i++) {
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

        updateCamera();
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
        while (true) {
            ObjectManager.updateAll();
            updateCamera();
            repaint();
            try {
                Thread.sleep(16);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private Protagonist findProtagonist() {
        for (GameObject obj : ObjectManager.list) {
            if (obj instanceof Protagonist protagonist) {
                return protagonist;
            }
        }
        return null;
    }

    private void updateCamera() {
        Protagonist protagonist = findProtagonist();
        if (protagonist == null) {
            return;
        }

        int viewWidth = getWidth() > 0 ? getWidth() : 800;
        int viewHeight = getHeight() > 0 ? getHeight() : 600;
        tileManager.centerCameraOn(protagonist.x, protagonist.y, viewWidth, viewHeight);
    }

    private void drawUI(Graphics g) {
        // Affiche le compteur des entites encore en vie.
        int civils = 0;
        int soldats = 0;
        int aliens = 0;

        for (var obj : ObjectManager.list) {
            if (obj instanceof Homme && !(obj instanceof Soldat) && !(obj instanceof Alien)) civils++;
            else if (obj instanceof Soldat) soldats++;
            else if (obj instanceof Alien) aliens++;
        }

        g.setColor(Color.WHITE);
        g.drawString("Civils: " + civils, 10, 20);
        g.drawString("Soldats: " + soldats, 10, 40);
        g.drawString("Aliens: " + aliens, 10, 60);
        g.drawString("Casualties: " + (8 - civils - soldats), 10, 80);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        var old = g2d.getTransform();
        g2d.translate(-tileManager.getCameraX(), -tileManager.getCameraY());
        tileManager.draw(g2d);
        ObjectManager.drawAll(g2d);
        g2d.setTransform(old);
        drawUI(g);
    }

    public void paintScore(Graphics g, int scoreHit) {
        super.paintComponent(g);
        g.setColor(Color.RED);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.WHITE);
        g.drawString("Score: " + score, 10, 20);
    }

}
