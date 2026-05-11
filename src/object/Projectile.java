package object;

import java.awt.*;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Projectile extends GameObject {

    private static Image imgShot;

    static {
        try {
            imgShot = ImageIO.read(Projectile.class.getResourceAsStream("/assets/effets/shot_effect.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double angle; // Angle de rotation du projectile
    private int vie = 120; // Durée de vie du projectile en frames
    private final Homme tireur; // Le tireur à ignorer lors des collisions

    // on utilise toujours homme car toutes les entité héritent de homme.
    public Projectile(double x, double y, double vx, double vy, Homme tireur) {
        super(x, y);
        this.vx = vx;
        this.vy = vy;
        this.tireur = tireur;
        this.angle = Math.atan2(vy, vx) + Math.PI / 2; // Calcul de l'angle de rotation pour orienter le projectile dans la direction du mouvement  
    }

    public void update() {
        x += vx;
        y += vy;

        // Vérification des collisions avec les hommes (ignorer le tireur lui-même)
        for (GameObject obj : new java.util.ArrayList<>(ObjectManager.list)) {
            if (obj instanceof Homme homme && homme != tireur) {
                double hx = homme.x - x;
                double hy = homme.y - y;
                if (hx * hx + hy * hy < 18 * 18) {
                    ObjectManager.list.remove(homme);
                    ObjectManager.list.remove(this);
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

        var old = g2d.getTransform(); // Sauvegarde de la transformation actuelle
        g2d.rotate(angle, x, y);
        g2d.drawImage(imgShot, (int)x-4, (int)y-8, 8, 30, null);
        g2d.setTransform(old); // Restauration de la transformation originale pour ne pas affecter les autres dessins
    }
    
}
