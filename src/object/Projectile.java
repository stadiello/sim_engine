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

    public Projectile(double x, double y, double vx, double vy) {
        super(x, y);
        this.vx = vx;
        this.vy = vy;
        this.angle = Math.atan2(vy, vx) + Math.PI / 2; // Calcul de l'angle de rotation pour orienter le projectile dans la direction du mouvement  
    }

    public void update() {
        x += vx;
        y += vy;

        for (GameObject obj : new java.util.ArrayList<>(ObjectManager.list)) {
            if (obj instanceof Homme homme) {
                double hx = homme.x - x;
                double hy = homme.y - y;
                if (hx * hx + hy * hy < 18 * 18) {
                    ObjectManager.list.remove(homme);
                    ObjectManager.list.remove(this);
                    return;
                }
            // if (obj instanceof Alien alien) {
            //     double dx = alien.x - x;
            //     double dy = alien.y - y;
            //     if (dx * dx + dy * dy < 18 * 18) {
            //         ObjectManager.list.remove(alien);
            //         ObjectManager.list.remove(this);
            //         return;
            //     }
            // }
            // else if (obj instanceof Soldat soldat) {
            //     double sx = soldat.x - x;
            //     double sy = soldat.y - y;
            //     if (sx * sx + sy * sy < 18 * 18) {
            //         ObjectManager.list.remove(soldat);
            //         ObjectManager.list.remove(this);
            //         return;
            //     }
            // } 
            // else if (obj instanceof Homme homme) {
            //     double hx = homme.x - x;
            //     double hy = homme.y - y;
            //     if (hx * hx + hy * hy < 18 * 18) {
            //         ObjectManager.list.remove(homme);
            //         ObjectManager.list.remove(this);
            //         return;
            //     }
            // }
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
