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
    private static final int LOS_CHECK_INTERVAL_FRAMES = 4;
    private static final double EPSILON_DIST_SQ = 0.00000001;
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
    private int losCheckCooldown = 0;
    private Homme losCachedTarget;
    private boolean losCachedVisible;


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
            if (distanceSq > EPSILON_DIST_SQ) {
                double invDistance = 1.0 / Math.sqrt(distanceSq);
                facingX = dx * invDistance;
                facingY = dy * invDistance;
            }

            if (distanceSq <= ATTACK_RANGE * ATTACK_RANGE && canSeeTarget(target)) {
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
            double distanceSq = dx * dx + dy * dy;
            double arrivalRadiusSq = ARRIVAL_RADIUS * ARRIVAL_RADIUS;

            if (distanceSq <= arrivalRadiusSq) {
                vx = 0;
                vy = 0;
                return;
            }

            double invDistance = 1.0 / Math.sqrt(distanceSq);
            vx = dx * invDistance * MOVE_SPEED;
            vy = dy * invDistance * MOVE_SPEED;
            facingX = dx * invDistance;
            facingY = dy * invDistance;
            return;
        }

        Protagonist protagonist = ObjectManager.getProtagonist();
        if (protagonist != null) {
            double dx = protagonist.x - x;
            double dy = protagonist.y - y;
            double distanceSq = dx * dx + dy * dy;
            double followDistanceSq = FOLLOW_DISTANCE * FOLLOW_DISTANCE;

            if (distanceSq > followDistanceSq && distanceSq > EPSILON_DIST_SQ) {
                double invDistance = 1.0 / Math.sqrt(distanceSq);
                vx = dx * invDistance * MOVE_SPEED;
                vy = dy * invDistance * MOVE_SPEED;
                facingX = dx * invDistance;
                facingY = dy * invDistance;
                return;
            }

            vx = 0;
            vy = 0;
            return;
        }

        vx = 0;
        vy = 0;
    }

    private boolean canSeeTarget(Homme target) {
        if (target == null) {
            return false;
        }

        if (target != losCachedTarget) {
            losCachedTarget = target;
            losCheckCooldown = 0;
        }

        if (losCheckCooldown <= 0) {
            losCachedVisible = hasLineOfSight(target.x, target.y);
            losCheckCooldown = LOS_CHECK_INTERVAL_FRAMES;
        } else {
            losCheckCooldown--;
        }

        return losCachedVisible;
    }

    private boolean hasLineOfSight(double targetX, double targetY) {
        TileManager tileManager = ObjectManager.getTileManager();
        if (tileManager == null) {
            return true;
        }

        double dx = targetX - x;
        double dy = targetY - y;
        if (dx * dx + dy * dy < EPSILON_DIST_SQ) {
            return true;
        }

        double step = 4.0;
        int steps = Math.max(1, (int) (Math.max(Math.abs(dx), Math.abs(dy)) / step));

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
