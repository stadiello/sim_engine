package main;

import javax.swing.*;
import java.awt.*;
import java.awt.KeyboardFocusManager;

import gameController.GameKeyController;
import object.Alien;
import object.Ennemi;
import object.Homme;
import object.Protagonist;
import object.Soldat;
import object.ObjectManager;
import world.TileManager;

public class GamePanel extends JPanel implements Runnable {

    // 50 correspond aux dimensions de la carte (800÷16=50, 600÷12=50), ce qui permet un affichage correct des tiles sur l'écran 800×600.
    public int tileSize = 50;
    private static final double ENTITY_RADIUS = 14;
    private static final int MAP_COLS = 16;
    private static final int MAP_ROWS = 12;
    private static final int MAX_SPAWN_ATTEMPTS = 300;
        private static final int[][] ENEMY_SPAWN_ZONES = {
            {7, 9, 1, 2},
            {13, 14, 1, 2},
            {1, 3, 8, 9},
            {13, 14, 8, 9}
        };
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
        double[] protagonistSpawn = getFreeSpawnPositionInZone(1, 3, 1, 2);
        ObjectManager.list.add(new Protagonist(protagonistSpawn[0], protagonistSpawn[1], keyController));

        // Garde un soldat allié près du protagoniste mais dans la même zone sécurisée.
        double[] soldatSpawn = getFreeSpawnPositionInZone(1, 3, 1, 2);
        ObjectManager.list.add(new Soldat(soldatSpawn[0], soldatSpawn[1]));
        for (int i = 0; i < 5; i++) {
            double[] spawn = getFreeSpawnPosition();
            ObjectManager.list.add(new Alien(spawn[0], spawn[1]));
        }
        for (int i = 0; i < 4; i++) {
            int[] zone = ENEMY_SPAWN_ZONES[i % ENEMY_SPAWN_ZONES.length];
            double[] spawn = getFreeSpawnPositionInZone(zone[0], zone[1], zone[2], zone[3]);
            ObjectManager.list.add(new Ennemi(spawn[0], spawn[1]));
        }
    }

    private double[] getFreeSpawnPositionInZone(int startCol, int endCol, int startRow, int endRow) {
        int minCol = Math.max(0, Math.min(startCol, MAP_COLS - 1));
        int maxCol = Math.max(0, Math.min(endCol, MAP_COLS - 1));
        int minRow = Math.max(0, Math.min(startRow, MAP_ROWS - 1));
        int maxRow = Math.max(0, Math.min(endRow, MAP_ROWS - 1));

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

    private double[] getFreeSpawnPosition() {
        double worldWidth = MAP_COLS * tileSize;
        double worldHeight = MAP_ROWS * tileSize;

        for (int i = 0; i < MAX_SPAWN_ATTEMPTS; i++) {
            double x = ENTITY_RADIUS + Math.random() * (worldWidth - ENTITY_RADIUS * 2);
            double y = ENTITY_RADIUS + Math.random() * (worldHeight - ENTITY_RADIUS * 2);

            if (isSpawnAreaFree(x, y, ENTITY_RADIUS)) {
                return new double[]{x, y};
            }
        }

        for (int row = 0; row < MAP_ROWS; row++) {
            for (int col = 0; col < MAP_COLS; col++) {
                double x = col * tileSize + tileSize / 2.0;
                double y = row * tileSize + tileSize / 2.0;

                if (isSpawnAreaFree(x, y, ENTITY_RADIUS)) {
                    return new double[]{x, y};
                }
            }
        }

        return new double[]{tileSize / 2.0, tileSize / 2.0};
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
        // g.setColor(Color.BLACK);
        // g.fillRect(0, 0, getWidth(), getHeight());
        tileManager.draw(g);    
        ObjectManager.drawAll(g);
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
