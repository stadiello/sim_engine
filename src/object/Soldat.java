package object;

import java.awt.*;
import java.io.IOException;
import javax.imageio.ImageIO;

import object.weapon.Weapon;
import world.TileManager;

public class Soldat extends Homme {

    private static final double MOVE_SPEED = 2.4;
    private static final double ATTACK_RANGE = 260.0;
    private static final double FOLLOW_DISTANCE = 90.0;
    private static final double ARRIVAL_RADIUS = 12.0;
    private static final Weapon WEAPON = Weapon.blaster();
    private static Image imgCorps;
    private static Image arme;
    private boolean shot = false;

    private int shootCooldown = 0;

    private int timer = 0;
    private boolean hasDestination = false;
    private double destinationX;
    private double destinationY;
    private double facingX = 0;
    private double facingY = -1;


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

    public void moveTo(double targetX, double targetY) {
        destinationX = targetX;
        destinationY = targetY;
        hasDestination = true;
    }

    @Override
    public void update() {
        if (shootCooldown > 0) {
            shootCooldown--;
        }

        Homme target = ObjectManager.getNearestHostileForSoldat(x, y);
        boolean hasAttackTarget = false;

        if (target != null) {
            double dx = target.x - x;
            double dy = target.y - y;
            double distanceSq = dx * dx + dy * dy;
            double distance = Math.sqrt(distanceSq);
            if (distance > 0.0001) {
                facingX = dx / distance;
                facingY = dy / distance;
            }

            if (distanceSq <= ATTACK_RANGE * ATTACK_RANGE && hasLineOfSight(target.x, target.y)) {
                vx = 0;
                vy = 0;
                hasAttackTarget = true;
                if (shootCooldown == 0) {
                    WEAPON.fire(this, facingX, facingY);
                    shootCooldown = WEAPON.getCooldownFrames();
                    shot = true;
                }
            }
        }

        if (!hasAttackTarget) {
            updateMovement();
            moveWithTileCollision(14);
        }

        timer++;
    }

    private void updateMovement() {
        if (hasDestination) {
            double dx = destinationX - x;
            double dy = destinationY - y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance <= ARRIVAL_RADIUS) {
                vx = 0;
                vy = 0;
                return;
            }

            vx = dx / distance * MOVE_SPEED;
            vy = dy / distance * MOVE_SPEED;
            facingX = vx / MOVE_SPEED;
            facingY = vy / MOVE_SPEED;
            return;
        }

        Protagonist protagonist = ObjectManager.getProtagonist();
        if (protagonist != null) {
            double dx = protagonist.x - x;
            double dy = protagonist.y - y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance > FOLLOW_DISTANCE) {
                vx = dx / distance * MOVE_SPEED;
                vy = dy / distance * MOVE_SPEED;
                facingX = dx / distance;
                facingY = dy / distance;
                return;
            }

            vx = 0;
            vy = 0;
            return;
        }

        vx = 0;
        vy = 0;
    }

    private boolean hasLineOfSight(double targetX, double targetY) {
        TileManager tileManager = ObjectManager.getTileManager();
        if (tileManager == null) {
            return true;
        }

        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < 0.0001) {
            return true;
        }

        double step = 4.0;
        int steps = Math.max(1, (int) (distance / step));

        for (int i = 1; i < steps; i++) {
            double t = i / (double) steps;
            double sampleX = x + dx * t;
            double sampleY = y + dy * t;
            if (tileManager.isBlockedAtPixel(sampleX, sampleY)) {
                return false;
            }
        }

        return true;
    }

    // @Override
    // public void draw(Graphics g) {
    //     g.drawImage(imgCorps, (int)x - 16, (int)y - 16, 32, 32, null);
    //     g.drawImage(arme, (int)x + 5, (int)y - 28, 5, 30, null); // Arme dessinée à côté du corps
    // }
    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        double angle = Math.atan2(facingY, facingX) + Math.PI / 2; // Calcul de l'angle de rotation
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
