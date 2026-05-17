package object;

import java.awt.*;
import java.io.IOException;
import javax.imageio.ImageIO;

import main.GamePanel;

public class Projectile extends GameObject {

    private static Image imgShot;
    private static Image imgBullet;

    public enum ProjectileType {
        DEFAULT,
        BULLET
    }

    static {
        try {
            imgShot = ImageIO.read(Projectile.class.getResourceAsStream("/assets/effets/shot_effect.png"));
            imgBullet = ImageIO.read(Projectile.class.getResourceAsStream("/assets/effets/bullet.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double angle; // Angle de rotation du projectile
    private int vie = 120; // Durée de vie du projectile en frames
    private final Homme tireur; // Le tireur à ignorer lors des collisions
    private final ProjectileType type;

    // on utilise toujours homme car toutes les entité héritent de homme.
    public Projectile(double x, double y, double vx, double vy, Homme tireur) {
        this(x, y, vx, vy, tireur, ProjectileType.DEFAULT);
    }

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

        if (vie <= 0 || x < 0 || x > 800 || y < 0 || y > 600) {
            ObjectManager.list.remove(this); // Supprime le projectile après sa durée de vie
        }
    }

    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        boolean isBullet = type == ProjectileType.BULLET;
        Image projectileImage = isBullet ? imgBullet : imgShot;
        int projectileWidth = isBullet ? 2 : 8;
        int projectileHeight = isBullet ? 12 : 30;
        int projectileX = isBullet ? (int)x + 8 : (int)x + 5;
        int projectileY = isBullet ? (int)y - 14 : (int)y - 23;

        var old = g2d.getTransform(); // Sauvegarde de la transformation actuelle
        g2d.rotate(angle, x, y);
        if (projectileImage != null) {
            g2d.drawImage(projectileImage, projectileX, projectileY, projectileWidth, projectileHeight, null);
        }
        g2d.setTransform(old); // Restauration de la transformation originale pour ne pas affecter les autres dessins
    }
    
}
