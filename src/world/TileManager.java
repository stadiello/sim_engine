package world;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

import main.GamePanel;

public class TileManager {

    public enum MapType {
        CITY("Ville"),
        DESERT_OUTPOST("Avant-poste desert");

        private final String displayName;

        MapType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    GamePanel gp;
    Tile[] tiles;
    int[][] map;
    private double cameraX;
    private double cameraY;
    private MapType currentMapType = MapType.CITY;

    public TileManager(GamePanel gp) {
        this.gp = gp;
        tiles = new Tile[10];
        
        loadTiles();
        loadMap(currentMapType);
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
        tiles[7] = new Tile(TileGenerator.generateSand(), false, false);
        tiles[8] = new Tile(TileGenerator.generateWoodenCrate(), true, false);
    }

    public void setCurrentMapType(MapType mapType) {
        if (mapType == null || mapType == currentMapType) {
            return;
        }

        currentMapType = mapType;
        loadMap(currentMapType);
        cameraX = 0;
        cameraY = 0;
    }

    public MapType getCurrentMapType() {
        return currentMapType;
    }

    public MapType[] getAvailableMapTypes() {
        return MapType.values();
    }

    private void loadMap(MapType mapType) {
        map = switch (mapType) {
            case CITY -> buildCityMapData();
            case DESERT_OUTPOST -> buildDesertMapData();
        };
    }

    private int[][] buildCityMapData() {
        int rows = 36;
        int cols = 48;
        int[][] mapData = new int[rows][cols];

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                mapData[row][col] = 2; // herbe par defaut
            }
        }

        // Routes principales pour garder une lecture claire du monde.
        for (int row = 0; row < rows; row++) {
            mapData[row][6] = 0;
            mapData[row][18] = 0;
            mapData[row][30] = 0;
            mapData[row][42] = 0;
        }
        for (int col = 0; col < cols; col++) {
            mapData[5][col] = 0;
            mapData[14][col] = 0;
            mapData[23][col] = 0;
            mapData[31][col] = 0;
        }

        // Blocs de batiments simples bordes de murs.
        addBuildingBlock(mapData, 1, 1, 4, 3);
        addBuildingBlock(mapData, 8, 1, 8, 3);
        addBuildingBlock(mapData, 20, 1, 8, 3);
        addBuildingBlock(mapData, 32, 1, 8, 3);

        addBuildingBlock(mapData, 1, 7, 4, 5);
        addBuildingBlock(mapData, 8, 7, 8, 5);
        addBuildingBlock(mapData, 20, 7, 8, 5);
        addBuildingBlock(mapData, 32, 7, 8, 5);

        addBuildingBlock(mapData, 1, 16, 4, 6);
        addBuildingBlock(mapData, 8, 16, 8, 6);
        addBuildingBlock(mapData, 20, 16, 8, 6);
        addBuildingBlock(mapData, 32, 16, 8, 6);

        addBuildingBlock(mapData, 1, 25, 4, 5);
        addBuildingBlock(mapData, 8, 25, 8, 5);
        addBuildingBlock(mapData, 20, 25, 8, 5);
        addBuildingBlock(mapData, 32, 25, 8, 5);

        // Quelques arbres decoratifs bloquants dans les espaces verts.
        for (int row = 2; row < rows; row += 7) {
            for (int col = 3; col < cols; col += 9) {
                if (mapData[row][col] == 2) {
                    mapData[row][col] = 6;
                }
            }
        }

        // Bord du monde bloque pour garder les entites dans la carte.
        for (int col = 0; col < cols; col++) {
            mapData[0][col] = 3;
            mapData[rows - 1][col] = 3;
        }
        for (int row = 0; row < rows; row++) {
            mapData[row][0] = 3;
            mapData[row][cols - 1] = 3;
        }

        return mapData;
    }

    private int[][] buildDesertMapData() {
        int rows = 36;
        int cols = 48;
        int[][] mapData = new int[rows][cols];

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                mapData[row][col] = 7;
            }
        }

        // Dunes sous forme de bandes de sable plus fonce (trottoir recolore).
        for (int row = 2; row < rows - 2; row += 7) {
            for (int col = 1; col < cols - 1; col++) {
                if ((col + row) % 3 != 0) {
                    mapData[row][col] = 1;
                }
            }
        }

        // Zone de campement centrale avec route compactee.
        for (int row = 10; row <= 25; row++) {
            for (int col = 16; col <= 31; col++) {
                mapData[row][col] = 0;
            }
        }

        // Couvertures en caisses de bois.
        addCrateCluster(mapData, 12, 8, 3, 2);
        addCrateCluster(mapData, 24, 7, 2, 3);
        addCrateCluster(mapData, 34, 11, 3, 2);
        addCrateCluster(mapData, 18, 17, 4, 2);
        addCrateCluster(mapData, 29, 20, 2, 4);
        addCrateCluster(mapData, 8, 23, 3, 3);
        addCrateCluster(mapData, 39, 26, 3, 2);

        // Couloir de deplacement a gauche pour assurer un spawn initial libre.
        for (int row = 1; row < rows - 1; row++) {
            mapData[row][4] = 7;
            mapData[row][5] = 7;
            mapData[row][6] = 7;
        }

        for (int col = 0; col < cols; col++) {
            mapData[0][col] = 3;
            mapData[rows - 1][col] = 3;
        }
        for (int row = 0; row < rows; row++) {
            mapData[row][0] = 3;
            mapData[row][cols - 1] = 3;
        }

        return mapData;
    }

    private void addBuildingBlock(int[][] mapData, int startCol, int startRow, int width, int height) {
        int endRow = Math.min(mapData.length, startRow + height);
        int endCol = Math.min(mapData[0].length, startCol + width);

        for (int row = startRow; row < endRow; row++) {
            for (int col = startCol; col < endCol; col++) {
                boolean border = row == startRow || row == endRow - 1 || col == startCol || col == endCol - 1;
                mapData[row][col] = border ? 3 : 4;
            }
        }

        int doorCol = Math.min(endCol - 1, startCol + width / 2);
        int doorRow = endRow - 1;
        if (doorRow >= 0 && doorRow < mapData.length && doorCol >= 0 && doorCol < mapData[0].length) {
            mapData[doorRow][doorCol] = 5;
        }
    }

    private void addCrateCluster(int[][] mapData, int startCol, int startRow, int width, int height) {
        int endRow = Math.min(mapData.length - 1, startRow + height);
        int endCol = Math.min(mapData[0].length - 1, startCol + width);
        for (int row = Math.max(1, startRow); row < endRow; row++) {
            for (int col = Math.max(1, startCol); col < endCol; col++) {
                mapData[row][col] = 8;
            }
        }
    }

    public void drawPreview(Graphics2D g2d, Rectangle rect, MapType mapType) {
        int[][] previewMap = mapType == MapType.CITY ? buildCityMapData() : buildDesertMapData();
        int rows = previewMap.length;
        int cols = previewMap[0].length;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int tileId = previewMap[row][col];
                int x = rect.x + col * rect.width / cols;
                int y = rect.y + row * rect.height / rows;
                int w = (col + 1) * rect.width / cols - col * rect.width / cols;
                int h = (row + 1) * rect.height / rows - row * rect.height / rows;
                g2d.drawImage(tiles[tileId].image, x, y, w, h, null);
            }
        }
    }

    public void draw(Graphics g) {
        Rectangle clip = g.getClipBounds();
        int viewWidth = clip != null ? clip.width : 800;
        int viewHeight = clip != null ? clip.height : 600;
        int margin = 1;

        int startCol = Math.max(0, getCameraX() / gp.tileSize - margin);
        int endCol = Math.min(map[0].length - 1, (getCameraX() + viewWidth) / gp.tileSize + margin);
        int startRow = Math.max(0, getCameraY() / gp.tileSize - margin);
        int endRow = Math.min(map.length - 1, (getCameraY() + viewHeight) / gp.tileSize + margin);

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
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
