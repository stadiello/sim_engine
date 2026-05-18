package world;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

import main.GamePanel;

public class TileManager {

    GamePanel gp;
    Tile[] tiles;
    int[][] map;
    private double cameraX;
    private double cameraY;

    public TileManager(GamePanel gp) {
        this.gp = gp;
        tiles = new Tile[10];
        
        loadTiles();
        loadCityMap();
    }
    private BufferedImage loadImage(String path) {
        try {
            var stream = getClass().getResourceAsStream(path);
            if (stream == null) {
                throw new RuntimeException("Image introuvable : " + path);
            }

            return ImageIO.read(stream);
        } catch (Exception e) {
            throw new RuntimeException(e);

        }

    }

    // private void loadTiles() {
    //     tiles[0] = new Tile(loadImage("/assets/tiles/road.png"), false, false);
    //     tiles[1] = new Tile(loadImage("/assets/tiles/sidewalk.png"), false, false);
    //     tiles[2] = new Tile(loadImage("/assets/tiles/grass.png"), false, false);
    //     tiles[3] = new Tile(loadImage("/assets/tiles/wall.png"), true, false);
    //     tiles[4] = new Tile(loadImage("/assets/tiles/building.png"), true, false);
    //     tiles[5] = new Tile(loadImage("/assets/tiles/door.png"), false, true);
    //     tiles[6] = new Tile(loadImage("/assets/tiles/decor_tree.png"), true, false);
    // }

    private void loadTiles() {
        tiles[0] = new Tile(TileGenerator.generateRoad(), false, false);
        tiles[1] = new Tile(TileGenerator.generateSidewalk(), false, false);
        tiles[2] = new Tile(TileGenerator.generateGrass(), false, false);
        tiles[3] = new Tile(TileGenerator.generateWall(), true, false);
        tiles[4] = new Tile(TileGenerator.generateBuilding(), true, false);
        tiles[5] = new Tile(TileGenerator.generateDoor(), false, true);
        tiles[6] = new Tile(TileGenerator.generateTree(), true, false);
    }

    private void loadCityMap() {
        int rows = 36;
        int cols = 48;
        map = new int[rows][cols];

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                map[row][col] = 2; // herbe par defaut
            }
        }

        // Routes principales pour garder une lecture claire du monde.
        for (int row = 0; row < rows; row++) {
            map[row][6] = 0;
            map[row][18] = 0;
            map[row][30] = 0;
            map[row][42] = 0;
        }
        for (int col = 0; col < cols; col++) {
            map[5][col] = 0;
            map[14][col] = 0;
            map[23][col] = 0;
            map[31][col] = 0;
        }

        // Blocs de batiments simples bordes de murs.
        addBuildingBlock(1, 1, 4, 3);
        addBuildingBlock(8, 1, 8, 3);
        addBuildingBlock(20, 1, 8, 3);
        addBuildingBlock(32, 1, 8, 3);

        addBuildingBlock(1, 7, 4, 5);
        addBuildingBlock(8, 7, 8, 5);
        addBuildingBlock(20, 7, 8, 5);
        addBuildingBlock(32, 7, 8, 5);

        addBuildingBlock(1, 16, 4, 6);
        addBuildingBlock(8, 16, 8, 6);
        addBuildingBlock(20, 16, 8, 6);
        addBuildingBlock(32, 16, 8, 6);

        addBuildingBlock(1, 25, 4, 5);
        addBuildingBlock(8, 25, 8, 5);
        addBuildingBlock(20, 25, 8, 5);
        addBuildingBlock(32, 25, 8, 5);

        // Quelques arbres decoratifs bloquants dans les espaces verts.
        for (int row = 2; row < rows; row += 7) {
            for (int col = 3; col < cols; col += 9) {
                if (map[row][col] == 2) {
                    map[row][col] = 6;
                }
            }
        }

        // Bord du monde bloque pour garder les entites dans la carte.
        for (int col = 0; col < cols; col++) {
            map[0][col] = 3;
            map[rows - 1][col] = 3;
        }
        for (int row = 0; row < rows; row++) {
            map[row][0] = 3;
            map[row][cols - 1] = 3;
        }
    }

    private void addBuildingBlock(int startCol, int startRow, int width, int height) {
        int endRow = Math.min(map.length, startRow + height);
        int endCol = Math.min(map[0].length, startCol + width);

        for (int row = startRow; row < endRow; row++) {
            for (int col = startCol; col < endCol; col++) {
                boolean border = row == startRow || row == endRow - 1 || col == startCol || col == endCol - 1;
                map[row][col] = border ? 3 : 4;
            }
        }

        int doorCol = Math.min(endCol - 1, startCol + width / 2);
        int doorRow = endRow - 1;
        if (doorRow >= 0 && doorRow < map.length && doorCol >= 0 && doorCol < map[0].length) {
            map[doorRow][doorCol] = 5;
        }
    }

    public void draw(Graphics g) {

        for (int row = 0; row < map.length; row++) {

            for (int col = 0; col < map[row].length; col++) {
                int tileId = map[row][col];
                int x = col * gp.tileSize;
                int y = row * gp.tileSize;
                g.drawImage(tiles[tileId].image, x, y, gp.tileSize, gp.tileSize, null);

            }
        }
    }

    public int getMapCols() {
        return map[0].length;
    }

    public int getMapRows() {
        return map.length;
    }

    public double getWorldWidth() {
        return getMapCols() * gp.tileSize;
    }

    public double getWorldHeight() {
        return getMapRows() * gp.tileSize;
    }

    public void centerCameraOn(double worldX, double worldY, int viewWidth, int viewHeight) {
        double targetCameraX = worldX - viewWidth / 2.0;
        double targetCameraY = worldY - viewHeight / 2.0;

        double maxX = Math.max(0, getWorldWidth() - viewWidth);
        double maxY = Math.max(0, getWorldHeight() - viewHeight);

        cameraX = Math.max(0, Math.min(targetCameraX, maxX));
        cameraY = Math.max(0, Math.min(targetCameraY, maxY));
    }

    public int getCameraX() {
        return (int) Math.round(cameraX);
    }

    public int getCameraY() {
        return (int) Math.round(cameraY);
    }

    public Tile getTileAtPixel(double x, double y) {

        int col = (int) x / gp.tileSize;
        int row = (int) y / gp.tileSize;

        if (row < 0 || row >= map.length || col < 0 || col >= map[0].length) {

            return null;

        }

        return tiles[map[row][col]];

    }

    public boolean isBlockedAtPixel(double x, double y) {
        Tile tile = getTileAtPixel(x, y);
        return tile == null || tile.collision;
    }


}
