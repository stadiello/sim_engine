package object;

import java.awt.*;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Soldat extends Homme {

    private static Image imgCorps;
    private static Image arme;
    private boolean shot = false;

    private int shootCooldown = 0;

    private int timer = 0;


    static {
        try {
            imgCorps = ImageIO.read(Soldat.class.getResourceAsStream("/assets/soldats/corps.png"));
            arme = ImageIO.read(Soldat.class.getResourceAsStream("/assets/armes/blaster.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Soldat(double x, double y) {
        super(x, y);
        vx = (Math.random() - 0.5) * 3; // Soldats plus rapides que les civils
        vy = (Math.random() - 0.5) * 3;
    }

    @Override
    public void update() {
        Alien target = ObjectManager.getNearestAlien(x, y);

        if (target != null) {
            double dx = target.x - x;
            double dy = target.y - y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance > 0) {
                double speed = 2.4;
                vx = (dx / distance) * speed;
                vy = (dy / distance) * speed;
            }

            if (shootCooldown > 0) {
                shootCooldown--;
            }

            if (distance > 0 && distance < 260 && shootCooldown == 0) {
                double projectileSpeed = 6.5;
                double px = x + (dx / distance) * 18;
                double py = y + (dy / distance) * 18;
                // le this est passé pour que le projectile ignore les collisions avec le soldat qui l'a tiré
                ObjectManager.list.add(new Projectile(px, py, (dx / distance) * projectileSpeed, (dy / distance) * projectileSpeed, this));
                shootCooldown = 25;
                shot = true;
            }
        }

        moveWithTileCollision(14);
        timer++;
    }

    // @Override
    // public void draw(Graphics g) {
    //     g.drawImage(imgCorps, (int)x - 16, (int)y - 16, 32, 32, null);
    //     g.drawImage(arme, (int)x + 5, (int)y - 28, 5, 30, null); // Arme dessinée à côté du corps
    // }
    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        double angle = Math.atan2(vy, vx) + Math.PI / 2; // Calcul de l'angle de rotation
        int offsetArme = (int)(Math.sin(timer * 0.15) * 6);
        var old = g2d.getTransform(); // Sauvegarde de la transformation actuelle

        g2d.rotate(angle, x, y);
        g2d.drawImage(imgCorps, (int)x - 16, (int)y - 16, 32, 42, null);
        if (shot == true) {
            g2d.drawImage(arme, (int)x + 5, (int)y - 23 + offsetArme, 5, 30, null); // Arme dessinée à côté du corps avec un léger mouvement
            shot = false;
        } else {
                    g2d.drawImage(arme, (int)x + 5, (int)y - 23, 5, 30, null); 
        }

        g2d.setTransform(old); // Restauration de la transformation originale pour ne pas affecter les autres dessins
        
    }
}
