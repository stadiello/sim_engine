package object.ai;

public final class AiTuning {

    private static int enemyReactionFrames = 12;
    private static int soldierReactionFrames = 10;
    private static int enemyAimStabilizationFrames = 6;
    private static int soldierAimStabilizationFrames = 5;
    private static int suppressionDurationFrames = 90;
    private static double suppressionCoverBoost = 0.65;
    private static double suppressionNearMissRadius = 92.0;
    private static boolean alienPackAggroEnabled = true;

    private AiTuning() {
    }

    public static int getEnemyReactionFrames() {
        return enemyReactionFrames;
    }

    public static int getSoldierReactionFrames() {
        return soldierReactionFrames;
    }

    public static int getEnemyAimStabilizationFrames() {
        return enemyAimStabilizationFrames;
    }

    public static int getSoldierAimStabilizationFrames() {
        return soldierAimStabilizationFrames;
    }

    public static int getSuppressionDurationFrames() {
        return suppressionDurationFrames;
    }

    public static double getSuppressionCoverBoost() {
        return suppressionCoverBoost;
    }

    public static double getSuppressionNearMissRadius() {
        return suppressionNearMissRadius;
    }

    public static boolean isAlienPackAggroEnabled() {
        return alienPackAggroEnabled;
    }

    public static void adjustEnemyReactionFrames(int delta) {
        enemyReactionFrames = clampInt(enemyReactionFrames + delta, 2, 40);
    }

    public static void adjustSoldierReactionFrames(int delta) {
        soldierReactionFrames = clampInt(soldierReactionFrames + delta, 2, 40);
    }

    public static void adjustEnemyAimStabilizationFrames(int delta) {
        enemyAimStabilizationFrames = clampInt(enemyAimStabilizationFrames + delta, 0, 30);
    }

    public static void adjustSoldierAimStabilizationFrames(int delta) {
        soldierAimStabilizationFrames = clampInt(soldierAimStabilizationFrames + delta, 0, 30);
    }

    public static void adjustSuppressionDurationFrames(int delta) {
        suppressionDurationFrames = clampInt(suppressionDurationFrames + delta, 20, 220);
    }

    public static void adjustSuppressionCoverBoost(double delta) {
        suppressionCoverBoost = clampDouble(suppressionCoverBoost + delta, 0.0, 2.0);
    }

    public static void adjustSuppressionNearMissRadius(double delta) {
        suppressionNearMissRadius = clampDouble(suppressionNearMissRadius + delta, 30.0, 180.0);
    }

    public static void toggleAlienPackAggro() {
        alienPackAggroEnabled = !alienPackAggroEnabled;
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
