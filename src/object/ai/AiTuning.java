package object.ai;

public final class AiTuning {

    private static final int NORMAL_ENEMY_REACTION_FRAMES = 12;
    private static final int NORMAL_SOLDIER_REACTION_FRAMES = 10;
    private static final int NORMAL_ENEMY_AIM_STABILIZATION_FRAMES = 6;
    private static final int NORMAL_SOLDIER_AIM_STABILIZATION_FRAMES = 5;
    private static final int NORMAL_SUPPRESSION_DURATION_FRAMES = 90;
    private static final double NORMAL_SUPPRESSION_COVER_BOOST = 0.65;
    private static final double NORMAL_SUPPRESSION_NEAR_MISS_RADIUS = 92.0;
    private static final boolean NORMAL_ALIEN_PACK_AGGRO = true;

    private static int enemyReactionFrames = NORMAL_ENEMY_REACTION_FRAMES;
    private static int soldierReactionFrames = NORMAL_SOLDIER_REACTION_FRAMES;
    private static int enemyAimStabilizationFrames = NORMAL_ENEMY_AIM_STABILIZATION_FRAMES;
    private static int soldierAimStabilizationFrames = NORMAL_SOLDIER_AIM_STABILIZATION_FRAMES;
    private static int suppressionDurationFrames = NORMAL_SUPPRESSION_DURATION_FRAMES;
    private static double suppressionCoverBoost = NORMAL_SUPPRESSION_COVER_BOOST;
    private static double suppressionNearMissRadius = NORMAL_SUPPRESSION_NEAR_MISS_RADIUS;
    private static boolean alienPackAggroEnabled = NORMAL_ALIEN_PACK_AGGRO;

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

    public static void applyNormalPreset() {
        enemyReactionFrames = NORMAL_ENEMY_REACTION_FRAMES;
        soldierReactionFrames = NORMAL_SOLDIER_REACTION_FRAMES;
        enemyAimStabilizationFrames = NORMAL_ENEMY_AIM_STABILIZATION_FRAMES;
        soldierAimStabilizationFrames = NORMAL_SOLDIER_AIM_STABILIZATION_FRAMES;
        suppressionDurationFrames = NORMAL_SUPPRESSION_DURATION_FRAMES;
        suppressionCoverBoost = NORMAL_SUPPRESSION_COVER_BOOST;
        suppressionNearMissRadius = NORMAL_SUPPRESSION_NEAR_MISS_RADIUS;
        alienPackAggroEnabled = NORMAL_ALIEN_PACK_AGGRO;
    }

    public static void applyEasyPreset() {
        enemyReactionFrames = 18;
        soldierReactionFrames = 8;
        enemyAimStabilizationFrames = 12;
        soldierAimStabilizationFrames = 3;
        suppressionDurationFrames = 70;
        suppressionCoverBoost = 0.45;
        suppressionNearMissRadius = 76.0;
        alienPackAggroEnabled = false;
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
