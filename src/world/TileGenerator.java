package world;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class TileGenerator {
    private static final int TILE_SIZE = 50;

    // Tile : herbe verte avec variation
    public static BufferedImage generateGrass() {
        BufferedImage img = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // Fond vert herbe
        g.setColor(new Color(76, 140, 50));
        g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);

        // Variation : taches d'herbe plus foncée
        Random rand = new Random(0); // Seed fixe pour cohérence
        g.setColor(new Color(60, 110, 40));
        for (int i = 0; i < 15; i++) {
            int x = rand.nextInt(TILE_SIZE);
            int y = rand.nextInt(TILE_SIZE);
            g.fillOval(x, y, rand.nextInt(8) + 4, rand.nextInt(8) + 4);
        }

        g.dispose();
        return img;
    }

    // Tile : route grise avec marquage blanc
    public static BufferedImage generateRoad() {
        BufferedImage img = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // Fond route
        g.setColor(new Color(88, 88, 88));
        g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);

        // Ligne de séparation blanche
        g.setColor(new Color(255, 255, 220));
        g.setStroke(new BasicStroke(2));
        g.drawLine(0, TILE_SIZE / 2, TILE_SIZE, TILE_SIZE / 2);

        // Détail : petit bruit asphalte
        Random rand = new Random(0);
        g.setColor(new Color(70, 70, 70));
        for (int i = 0; i < 20; i++) {
            g.fillRect(rand.nextInt(TILE_SIZE), rand.nextInt(TILE_SIZE), 2, 2);
        }

        g.dispose();
        return img;
    }

    // Tile : mur avec briques
    public static BufferedImage generateWall() {
        BufferedImage img = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // Fond
        g.setColor(new Color(160, 120, 100));
        g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);

        // Grille de briques
        g.setColor(new Color(120, 85, 70));
        g.setStroke(new BasicStroke(1));
        int brickWidth = 10;
        int brickHeight = 8;

        for (int y = 0; y < TILE_SIZE; y += brickHeight) {
            for (int x = 0; x < TILE_SIZE; x += brickWidth) {
                g.drawRect(x, y, brickWidth, brickHeight);
            }
        }

        g.dispose();
        return img;
    }

    // Tile : bâtiment en verre/béton
    public static BufferedImage generateBuilding() {
        BufferedImage img = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // Fond béton
        g.setColor(new Color(140, 140, 140));
        g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);

        // Fenêtres jaunes
        g.setColor(new Color(255, 240, 100));
        int windowSize = 8;
        int spacing = 3;
        int cols = TILE_SIZE / (windowSize + spacing);
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < cols; j++) {
                int x = spacing + i * (windowSize + spacing);
                int y = spacing + j * (windowSize + spacing);
                g.fillRect(x, y, windowSize, windowSize);
            }
        }

        g.dispose();
        return img;
    }

    // Tile : porte
    public static BufferedImage generateDoor() {
        BufferedImage img = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // Fond mur
        g.setColor(new Color(160, 120, 100));
        g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);

        // Porte marron
        g.setColor(new Color(101, 67, 33));
        g.fillRect(10, 5, 30, 40);

        // Poignée
        g.setColor(new Color(200, 160, 100));
        g.fillOval(35, 22, 3, 3);

        g.dispose();
        return img;
    }

    // Tile : arbre/décor
    public static BufferedImage generateTree() {
        BufferedImage img = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Base herbe, proche du tile de pelouse pour garder une palette cohérente.
        g.setColor(new Color(76, 140, 50));
        g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);

        Random rand = new Random(11);
        g.setColor(new Color(60, 110, 40));
        for (int i = 0; i < 12; i++) {
            int x = rand.nextInt(TILE_SIZE);
            int y = rand.nextInt(TILE_SIZE);
            g.fillOval(x, y, 3 + rand.nextInt(6), 3 + rand.nextInt(6));
        }

        // Ombre portée du feuillage pour mieux asseoir l'arbre sur le sol.
        g.setColor(new Color(46, 85, 36));
        g.fillOval(12, 30, 27, 11);

        // Tronc avec légère variation verticale.
        GradientPaint trunkGradient = new GradientPaint(
                0, 20, new Color(124, 83, 45),
                0, 47, new Color(86, 55, 30)
        );
        g.setPaint(trunkGradient);
        g.fillRoundRect(21, 23, 8, 24, 3, 3);

        g.setColor(new Color(150, 102, 58));
        g.drawLine(23, 26, 23, 43);
        g.drawLine(26, 28, 26, 44);

        // Feuillage en plusieurs masses pour casser l'effet "ovale unique".
        g.setColor(new Color(38, 116, 45));
        g.fillOval(10, 10, 18, 16);
        g.fillOval(22, 9, 17, 16);
        g.fillOval(14, 4, 22, 18);

        g.setColor(new Color(24, 90, 34));
        g.fillOval(12, 15, 13, 11);
        g.fillOval(26, 14, 11, 11);

        g.setColor(new Color(68, 150, 67));
        for (int i = 0; i < 6; i++) {
            int x = 14 + rand.nextInt(20);
            int y = 7 + rand.nextInt(15);
            g.fillOval(x, y, 3, 3);
        }

        // Contour léger pour garder la lisibilité sur différents fonds.
        g.setColor(new Color(20, 75, 30));
        g.drawOval(10, 10, 18, 16);
        g.drawOval(22, 9, 17, 16);
        g.drawOval(14, 4, 22, 18);

        g.dispose();
        return img;
    }

    // Tile : trottoir clair
    public static BufferedImage generateSidewalk() {
        BufferedImage img = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // Fond clair
        g.setColor(new Color(200, 200, 200));
        g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);

        // Grille de carrelage
        g.setColor(new Color(180, 180, 180));
        g.setStroke(new BasicStroke(1));
        int gridSize = 10;
        for (int x = 0; x <= TILE_SIZE; x += gridSize) {
            g.drawLine(x, 0, x, TILE_SIZE);
        }
        for (int y = 0; y <= TILE_SIZE; y += gridSize) {
            g.drawLine(0, y, TILE_SIZE, y);
        }

        g.dispose();
        return img;
    }

    public static BufferedImage generateSand() {
        BufferedImage img = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        g.setColor(new Color(209, 181, 126));
        g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);

        Random rand = new Random(0);
        g.setColor(new Color(190, 162, 110));
        for (int i = 0; i < 35; i++) {
            int x = rand.nextInt(TILE_SIZE);
            int y = rand.nextInt(TILE_SIZE);
            int w = 2 + rand.nextInt(4);
            int h = 2 + rand.nextInt(4);
            g.fillOval(x, y, w, h);
        }

        g.dispose();
        return img;
    }

    public static BufferedImage generateWoodenCrate() {
        BufferedImage img = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        g.setColor(new Color(209, 181, 126));
        g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);

        g.setColor(new Color(123, 83, 45));
        g.fillRect(6, 6, TILE_SIZE - 12, TILE_SIZE - 12);

        g.setColor(new Color(96, 63, 33));
        g.setStroke(new BasicStroke(3f));
        g.drawRect(6, 6, TILE_SIZE - 12, TILE_SIZE - 12);
        g.drawLine(6, 6, TILE_SIZE - 6, TILE_SIZE - 6);
        g.drawLine(TILE_SIZE - 6, 6, 6, TILE_SIZE - 6);

        g.setColor(new Color(146, 99, 56));
        g.setStroke(new BasicStroke(2f));
        g.drawLine(8, TILE_SIZE / 2, TILE_SIZE - 8, TILE_SIZE / 2);
        g.drawLine(TILE_SIZE / 2, 8, TILE_SIZE / 2, TILE_SIZE - 8);

        g.dispose();
        return img;
    }
}