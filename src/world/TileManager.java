package world;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

import main.GamePanel;

public class TileManager {

    GamePanel gp;
    Tile[] tiles;
    int[][] map;

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

    private void loadTiles() {
        tiles[0] = new Tile(loadImage("/assets/tiles/road.png"), false, false);
        tiles[1] = new Tile(loadImage("/assets/tiles/sidewalk.png"), false, false);
        tiles[2] = new Tile(loadImage("/assets/tiles/grass.png"), false, false);
        tiles[3] = new Tile(loadImage("/assets/tiles/wall.png"), true, false);
        tiles[4] = new Tile(loadImage("/assets/tiles/building.png"), true, false);
        tiles[5] = new Tile(loadImage("/assets/tiles/door.png"), false, true);
        tiles[6] = new Tile(loadImage("/assets/tiles/decor_tree.png"), true, false);
    }

    private void loadCityMap() {

        map = new int[][]{
                {4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4},
                {4,5,1,1,1,1,0,0,0,0,1,1,1,5,4,4},
                {4,1,1,2,2,1,0,0,0,0,1,2,2,1,1,4},
                {4,1,2,6,2,1,0,0,0,0,1,2,6,2,1,4},
                {4,1,1,2,2,1,0,0,0,0,1,2,2,1,1,4},
                {4,5,1,1,1,1,0,0,0,0,1,1,1,5,4,4},
                {4,4,4,4,4,1,0,0,0,0,1,4,4,4,4,4},
                {2,2,2,2,4,1,0,0,0,0,1,4,2,2,2,2},
                {2,6,2,2,4,1,1,1,1,1,1,4,2,2,6,2},
                {2,2,2,2,4,4,4,5,5,4,4,4,2,2,2,2},
                {0,0,0,0,0,0,1,1,1,1,0,0,0,0,0,0},
                {0,0,0,0,0,0,1,2,2,1,0,0,0,0,0,0}
        };
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
