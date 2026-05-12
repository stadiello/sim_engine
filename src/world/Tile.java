package world;

import java.awt.image.BufferedImage;

public class Tile {
    public BufferedImage image;
    public boolean collision;
    public boolean interactif;

    public Tile(BufferedImage image, boolean collision, boolean interactif) {
        this.image = image;
        this.collision = collision;
        this.interactif = interactif;
    }
    
}
