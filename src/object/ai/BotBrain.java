package object.ai;

import object.Douille;
import object.Ennemi;
import object.Homme;
import object.ObjectManager;
import object.Projectile;
import world.TileManager;

public class BotBrain {

    private static final double CHASE_SPEED = 1.9;
    private static final double STOP_AND_SHOOT_RANGE = 220.0;
    private static final int SHOOT_COOLDOWN_FRAMES = 34;
    private static final int LOS_CHECK_INTERVAL_FRAMES = 4;
    private static final double EPSILON_DIST_SQ = 0.00000001;
    private static final double BULLET_SPEED = 8.2;

    private int shootCooldown = 0;
    private int losCheckCooldown = 0;
    private Homme losCachedTarget;
    private boolean losCachedVisible;

    public void update(Ennemi enemy) {
        if (shootCooldown > 0) {
            shootCooldown--;
        }

        Homme target = ObjectManager.getNearestAlliedTarget(enemy.x, enemy.y);
        if (target == null) {
            enemy.vx = 0;
            enemy.vy = 0;
            return;
        }

        double dx = target.x - enemy.x;
        double dy = target.y - enemy.y;
        double distSq = dx * dx + dy * dy;
        if (distSq <= EPSILON_DIST_SQ) {
            enemy.vx = 0;
            enemy.vy = 0;
            return;
        }

        double invDist = 1.0 / Math.sqrt(distSq);
        boolean hasLineOfSight = canSeeTarget(enemy, target, distSq);

        enemy.setFacingDirection(dx, dy);

        if (distSq <= STOP_AND_SHOOT_RANGE * STOP_AND_SHOOT_RANGE && hasLineOfSight) {
            enemy.vx = 0;
            enemy.vy = 0;
            if (shootCooldown == 0) {
                double dirX = dx * invDist;
                double dirY = dy * invDist;

                ObjectManager.list.add(new Projectile(
                        enemy.x + dirX * 12,
                        enemy.y + dirY * 12,
                        dirX * BULLET_SPEED,
                        dirY * BULLET_SPEED,
                        enemy,
                        Projectile.ProjectileType.BULLET));

                spawnDouille(enemy, dirX, dirY);
                enemy.onShot();
                shootCooldown = SHOOT_COOLDOWN_FRAMES;
            }
            return;
        }

        enemy.vx = dx * invDist * CHASE_SPEED;
        enemy.vy = dy * invDist * CHASE_SPEED;
    }

    private boolean canSeeTarget(Ennemi enemy, Homme target, double distSq) {
        if (distSq > STOP_AND_SHOOT_RANGE * STOP_AND_SHOOT_RANGE) {
            losCachedTarget = null;
            losCheckCooldown = 0;
            return false;
        }

        if (target != losCachedTarget) {
            losCachedTarget = target;
            losCheckCooldown = 0;
        }

        if (losCheckCooldown <= 0) {
            losCachedVisible = hasLineOfSight(enemy.x, enemy.y, target.x, target.y);
            losCheckCooldown = LOS_CHECK_INTERVAL_FRAMES;
        } else {
            losCheckCooldown--;
        }

        return losCachedVisible;
    }

    private void spawnDouille(Ennemi enemy, double dirX, double dirY) {
        double sideX = -dirY;
        double sideY = dirX;

        double shellX = enemy.x + sideX * 9 - dirX * 4;
        double shellY = enemy.y + sideY * 9 - dirY * 4;

        double shellSpeed = 1.6 + Math.random() * 1.0;
        double shellVx = sideX * shellSpeed - dirX * 0.5 + (Math.random() - 0.5) * 0.6;
        double shellVy = sideY * shellSpeed - dirY * 0.5 + (Math.random() - 0.5) * 0.6;
        double shellAngle = Math.atan2(shellVy, shellVx);
        double shellRotationSpeed = (Math.random() - 0.5) * 0.28;

        ObjectManager.list.add(new Douille(shellX, shellY, shellVx, shellVy, shellAngle, shellRotationSpeed));
    }

    private boolean hasLineOfSight(double fromX, double fromY, double toX, double toY) {
        TileManager tileManager = ObjectManager.getTileManager();
        if (tileManager == null) {
            return true;
        }

        double dx = toX - fromX;
        double dy = toY - fromY;
        if (dx * dx + dy * dy < EPSILON_DIST_SQ) {
            return true;
        }

        double step = 4.0;
        int steps = Math.max(1, (int) (Math.max(Math.abs(dx), Math.abs(dy)) / step));

        for (int i = 1; i < steps; i++) {
            double t = i / (double) steps;
            double sampleX = fromX + dx * t;
            double sampleY = fromY + dy * t;
            if (tileManager.isBlockedAtPixel(sampleX, sampleY)) {
                return false;
            }
        }

        return true;
    }
}
