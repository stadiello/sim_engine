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
    private static final double BULLET_SPEED = 8.2;

    private int shootCooldown = 0;

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
        double dist = Math.sqrt(distSq);
        boolean hasLineOfSight = hasLineOfSight(enemy.x, enemy.y, target.x, target.y);

        enemy.setFacingDirection(dx, dy);

        if (distSq <= STOP_AND_SHOOT_RANGE * STOP_AND_SHOOT_RANGE && hasLineOfSight) {
            enemy.vx = 0;
            enemy.vy = 0;
            if (shootCooldown == 0 && dist > 0.0001) {
                double dirX = dx / dist;
                double dirY = dy / dist;

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

        enemy.vx = (dx / dist) * CHASE_SPEED;
        enemy.vy = (dy / dist) * CHASE_SPEED;
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
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < 0.0001) {
            return true;
        }

        double step = 4.0;
        int steps = Math.max(1, (int) (distance / step));

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
