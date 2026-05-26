package object;

import java.awt.*;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Alien extends Homme {

    private static final double AGGRO_CHASE_SPEED = 2.8;
    private static final double AGGRO_RELEASE_DISTANCE = 22.0;
    private static final double ATTACK_RADIUS = 18.0;
    private static final int ATTACK_COOLDOWN_FRAMES = 26;

    private static Image imgAlien;

    static {
        try {
            imgAlien = ImageIO.read(Alien.class.getResourceAsStream("/assets/aliens/alien.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private int timer = 0;
    private Homme aggroTarget;
    private int attackCooldownFrames = 0;

    public Alien(double x, double y) {
        super(x, y);
        vx = (Math.random() - 0.5) * 4;
        vy = (Math.random() - 0.5) * 4;
    }

    public void update() {
        if (attackCooldownFrames > 0) {
            attackCooldownFrames--;
        }

        if (aggroTarget != null) {
            if (!ObjectManager.list.contains(aggroTarget)) {
                aggroTarget = null;
            } else {
                double dx = aggroTarget.x - x;
                double dy = aggroTarget.y - y;
                double distSq = dx * dx + dy * dy;
                if (distSq > AGGRO_RELEASE_DISTANCE * AGGRO_RELEASE_DISTANCE) {
                    double invDist = 1.0 / Math.sqrt(distSq);
                    double moveSpeed = AGGRO_CHASE_SPEED * getSuppressionMoveMultiplier();
                    vx = dx * invDist * moveSpeed;
                    vy = dy * invDist * moveSpeed;
                } else {
                    vx = 0;
                    vy = 0;
                }
            }
        }

        moveWithTileCollision(14);
        tryMeleeAttack();
        timer++;
    }

    private void tryMeleeAttack() {
        if (attackCooldownFrames > 0) {
            return;
        }

        for (Homme target : ObjectManager.getOverlappingHumans(x, y, ATTACK_RADIUS, this)) {
            if (target instanceof Alien || target instanceof Ennemi) {
                continue;
            }

            attackCooldownFrames = ATTACK_COOLDOWN_FRAMES;

            if (target instanceof Protagonist protagonist && protagonist.consumeArmorPlateOnHit()) {
                return;
            }

            if (target instanceof Soldat soldat && soldat.consumeArmorPlateOnHit()) {
                return;
            }

            ObjectManager.list.remove(target);
            target.onDeath();
            return;
        }
    }

    public void setAggroTarget(Homme target) {
        if (target == null || target == this) {
            return;
        }
        aggroTarget = target;
    }

    public void clearAggroTarget() {
        aggroTarget = null;
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        double angle = Math.atan2(vy, vx) + Math.PI / 2; // Calcul de l'angle de rotation
        // Ajouter une osilation droite / gauche pour rendre l'alien plus vivant
        double wobble = Math.sin(timer*0.15) * 0.12; // Oscillation de 0.12 radians (environ 7 degrés) à une fréquence de 0.15
        var old = g2d.getTransform(); // Sauvegarde de la transformation actuelle

        g2d.rotate(angle + wobble, x, y);
        g2d.drawImage(imgAlien, (int)x - 16, (int)y - 16, 32, 32, null);
        g2d.setTransform(old); // Restauration de la transformation originale pour ne pas affecter les autres dessins
    }
    
}
