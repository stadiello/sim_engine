package world;

import java.awt.image.BufferedImage;

public class Tile {
    public BufferedImage image;
    public boolean collision;
    public boolean interactif;
    public boolean destructible;
    public int hitPoints;

    public Tile(BufferedImage image, boolean collision, boolean interactif) {
        this(image, collision, interactif, false, 0);
    }

    public Tile(BufferedImage image, boolean collision, boolean interactif, boolean destructible, int hitPoints) {
        this.image = image;
        this.collision = collision;
        this.interactif = interactif;
        this.destructible = destructible;
        this.hitPoints = Math.max(0, hitPoints);
    }
    
}
