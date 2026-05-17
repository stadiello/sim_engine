package object.ai;

import object.Douille;
import object.Ennemi;
import object.ObjectManager;
import object.Projectile;
import object.Protagonist;

public class BotBrain {

    private static final double CHASE_SPEED = 1.9;
    private static final double STOP_AND_SHOOT_RANGE = 220.0;
    private static final int SHOOT_COOLDOWN_FRAMES = 34;
    private static final double BULLET_SPEED = 5.6;

    private int shootCooldown = 0;

    public void update(Ennemi enemy) {
        if (shootCooldown > 0) {
            shootCooldown--;
        }

        Protagonist target = getNearestProtagonist(enemy.x, enemy.y);
        if (target == null) {
            enemy.vx = 0;
            enemy.vy = 0;
            return;
        }

        double dx = target.x - enemy.x;
        double dy = target.y - enemy.y;
        double distSq = dx * dx + dy * dy;
        double dist = Math.sqrt(distSq);

        enemy.setFacingDirection(dx, dy);

        if (distSq <= STOP_AND_SHOOT_RANGE * STOP_AND_SHOOT_RANGE) {
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

    private Protagonist getNearestProtagonist(double x, double y) {
        Protagonist nearest = null;
        double bestDist = Double.MAX_VALUE;

        for (var obj : ObjectManager.list) {
            if (obj instanceof Protagonist protagonist) {
                double dx = protagonist.x - x;
                double dy = protagonist.y - y;
                double dist = dx * dx + dy * dy;
                if (dist < bestDist) {
                    bestDist = dist;
                    nearest = protagonist;
                }
            }
        }

        return nearest;
    }
}
