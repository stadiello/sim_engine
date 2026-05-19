package object.ai;

import object.ObjectManager;
import world.TileManager;

public final class TacticalMovement {

    private static final double EPSILON_DIST_SQ = 0.00000001;

    private TacticalMovement() {
    }

    public static boolean hasLineOfSight(double fromX, double fromY, double toX, double toY) {
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

    public static boolean canStandAt(double x, double y, double radius) {
        TileManager tileManager = ObjectManager.getTileManager();
        if (tileManager == null) {
            return true;
        }

        return !tileManager.isBlockedAtPixel(x - radius, y - radius)
                && !tileManager.isBlockedAtPixel(x + radius, y - radius)
                && !tileManager.isBlockedAtPixel(x - radius, y + radius)
                && !tileManager.isBlockedAtPixel(x + radius, y + radius);
    }

    public static double[] adjustForObstacles(double x, double y, double desiredVx, double desiredVy, double radius) {
        if (desiredVx * desiredVx + desiredVy * desiredVy < EPSILON_DIST_SQ) {
            return new double[]{0, 0};
        }

        if (canStandAt(x + desiredVx, y + desiredVy, radius)) {
            return new double[]{desiredVx, desiredVy};
        }

        double speed = Math.sqrt(desiredVx * desiredVx + desiredVy * desiredVy);
        double baseAngle = Math.atan2(desiredVy, desiredVx);
        double[] offsets = {
                Math.toRadians(28),
                -Math.toRadians(28),
                Math.toRadians(55),
                -Math.toRadians(55),
                Math.toRadians(82),
                -Math.toRadians(82)
        };

        for (double offset : offsets) {
            double angle = baseAngle + offset;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;
            if (canStandAt(x + vx, y + vy, radius)) {
                return new double[]{vx, vy};
            }
        }

        return new double[]{0, 0};
    }

    public static double[] findCoverPoint(
            double selfX,
            double selfY,
            double targetX,
            double targetY,
            double actorRadius,
            double preferredRange
    ) {
        TileManager tileManager = ObjectManager.getTileManager();
        if (tileManager == null) {
            return null;
        }

        double bestScore = Double.NEGATIVE_INFINITY;
        double bestX = 0;
        double bestY = 0;
        boolean found = false;

        double toTargetX = targetX - selfX;
        double toTargetY = targetY - selfY;
        double baseAngle = Math.atan2(toTargetY, toTargetX);

        double[] radii = {70, 100, 130, 160, 190, 230};
        int directionSamples = 20;

        for (double radius : radii) {
            for (int i = 0; i < directionSamples; i++) {
                double angle = baseAngle + i * (Math.PI * 2.0 / directionSamples);
                double candidateX = selfX + Math.cos(angle) * radius;
                double candidateY = selfY + Math.sin(angle) * radius;

                if (!canStandAt(candidateX, candidateY, actorRadius)) {
                    continue;
                }

                if (hasLineOfSight(candidateX, candidateY, targetX, targetY)) {
                    continue;
                }

                double distToTarget = Math.hypot(targetX - candidateX, targetY - candidateY);
                if (distToTarget < actorRadius * 4.0) {
                    continue;
                }

                double distToSelf = Math.hypot(candidateX - selfX, candidateY - selfY);
                double rangePenalty = Math.abs(distToTarget - preferredRange);

                double score = 1000.0 - rangePenalty * 2.2 - distToSelf * 0.75;
                if (score > bestScore) {
                    bestScore = score;
                    bestX = candidateX;
                    bestY = candidateY;
                    found = true;
                }
            }
        }

        if (!found) {
            return null;
        }

        return new double[]{bestX, bestY};
    }
}
