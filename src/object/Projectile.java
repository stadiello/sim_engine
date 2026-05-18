package object;

import java.awt.*;
import java.io.IOException;
import javax.imageio.ImageIO;

import main.GamePanel;
import world.TileManager;

public class Projectile extends GameObject {

    private static Image imgShot;
    private static Image imgBullet;
    private static Image imgShotgunPellet;

    public enum ProjectileType {
        DEFAULT,
        BULLET,
        SHOTGUN_PELLET
    }

    static {
        try {
            imgShot = ImageIO.read(Projectile.class.getResourceAsStream("/assets/effets/shot_effect.png"));
            imgBullet = ImageIO.read(Projectile.class.getResourceAsStream("/assets/effets/bullet.png"));
            // Utilise le shot_effect pour les pellets aussi (ou créer une image spécifique)
            imgShotgunPellet = ImageIO.read(Projectile.class.getResourceAsStream("/assets/effets/bullet.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double angle; // Angle de rotation du projectile
    private int vie = 120; // Durée de vie du projectile en frames
    private final Homme tireur; // Le tireur à ignorer lors des collisions
    private final ProjectileType type;

    public Projectile(double x, double y, double vx, double vy, Homme tireur, ProjectileType type) {
        super(x, y);
        this.vx = vx;
        this.vy = vy;
        this.tireur = tireur;
        this.type = type;
        this.angle = Math.atan2(vy, vx) + Math.PI / 2; // Calcul de l'angle de rotation pour orienter le projectile dans la direction du mouvement  
    }

    public void update() {
        x += vx;
        y += vy;

        if (!canMoveTo(x, y, 2)) {
            ObjectManager.list.remove(this);
            ObjectManager.list.add(new ImpactSpark(x, y));
            return;
        }

        // Vérification des collisions avec les hommes (ignorer le tireur lui-même)
        for (GameObject obj : new java.util.ArrayList<>(ObjectManager.list)) {
            if (obj instanceof Homme homme && homme != tireur) {
                double hx = homme.x - x;
                double hy = homme.y - y;
                if (hx * hx + hy * hy < 18 * 18) {
                    ObjectManager.list.remove(homme);
                    ObjectManager.list.add(new ImpactSpark(x, y));
                    ObjectManager.list.remove(this);
                    GamePanel.score += 10;
                    return;
                }
            }
        }

        vie--;

        TileManager tileManager = ObjectManager.getTileManager();
        double maxX = tileManager != null ? tileManager.getWorldWidth() : 800;
        double maxY = tileManager != null ? tileManager.getWorldHeight() : 600;

        if (vie <= 0 || x < 0 || x > maxX || y < 0 || y > maxY) {
            ObjectManager.list.remove(this); // Supprime le projectile après sa durée de vie
        }
    }

    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        boolean isBullet = type == ProjectileType.BULLET;
        boolean isPellet = type == ProjectileType.SHOTGUN_PELLET;
        
        Image projectileImage = isPellet ? imgShotgunPellet : (isBullet ? imgBullet : imgShot);
        int projectileWidth = isPellet ? 4 : (isBullet ? 2 : 8);
        int projectileHeight = isPellet ? 6 : (isBullet ? 12 : 30);
        int projectileX = isPellet ? (int)x + 3 : (isBullet ? (int)x + 8 : (int)x + 5);
        int projectileY = isPellet ? (int)y - 10 : (isBullet ? (int)y - 14 : (int)y - 23);

        var old = g2d.getTransform(); // Sauvegarde de la transformation actuelle
        g2d.rotate(angle, x, y);
        if (projectileImage != null) {
            g2d.drawImage(projectileImage, projectileX, projectileY, projectileWidth, projectileHeight, null);
        }
        g2d.setTransform(old); // Restauration de la transformation originale pour ne pas affecter les autres dessins
    }
    
}
