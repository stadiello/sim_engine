package object.ai;

import object.Ennemi;
import object.Homme;
import object.ObjectManager;
import object.weapon.Weapon;

public class BotBrain {

    private static final double CHASE_SPEED = 1.9; // Vitesse de déplacement de base de l'ennemi lorsqu'il poursuit une cible, utilisée pour calculer la vélocité désirée. La vitesse réelle peut être réduite par les obstacles et les ajustements de mouvement tactique.
    private static final double AGGRESSIVE_PUSH_FACTOR = 1.03; // Facteur de poussée pour les armes à courte portée comme le shotgun, pour les rendre un peu plus agressifs et éviter qu'ils ne restent à distance optimale sans jamais s'approcher. Un nombre légèrement supérieur à 1.0 encourage les ennemis à se rapprocher un peu plus que la distance optimale, ce qui est souvent nécessaire pour les armes à courte portée afin d'être efficaces.
    private static final double COVER_SEARCH_MIN_COOLDOWN_RATIO = 0.45; // Ratio de cooldown minimum pour que les ennemis commencent à chercher une couverture après avoir tiré. Un nombre plus élevé rend les ennemis plus susceptibles de chercher une couverture après chaque tir, tandis qu'un nombre plus bas les rend plus agressifs et moins enclins à se mettre à couvert.
    private static final double ENTITY_RADIUS = 14.0; // Rayon utilisé pour les vérifications de collision et de ligne de vue, pas forcément égal à la moitié de la taille du sprite. Un nombre plus élevé rend les ennemis plus prudents et moins susceptibles de se coller aux murs ou de tirer à travers des obstacles étroits, tandis qu'un nombre plus bas les rend plus agressifs mais peut entraîner des comportements étranges comme tirer à travers des murs très fins ou se coincer dans des espaces étroits.
    private static final int LOS_CHECK_INTERVAL_FRAMES = 4; // Combien de frames attendre avant de refaire un check de ligne de vue, pour économiser du CPU. Un nombre plus élevé rend les ennemis moins réactifs à l'apparition soudaine d'une cible, mais réduit les saccades quand il y a beaucoup d'ennemis à l'écran.
    private static final double EPSILON_DIST_SQ = 0.00000001; // Seuil pour éviter les divisions par zéro et les calculs inutiles de direction quand la cible est très proche. Un nombre plus élevé rend les ennemis plus susceptibles de rester immobiles quand ils sont très proches de leur cible, tandis qu'un nombre plus bas les rend plus réactifs mais peut entraîner des comportements erratiques quand ils sont presque sur la même position que la cible.
    private static final int COVER_MEMORY_FRAMES = 20; // Combien de temps le soldat "se souvient" d'un point de couverture avant de chercher un nouveau (en frames). Un nombre plus élevé rend les ennemis plus susceptibles de rester à couvert une fois qu'ils en ont trouvé un, tandis qu'un nombre plus bas les rend plus dynamiques et moins prévisibles dans leur utilisation de la couverture.
    private static final double VELOCITY_BLEND = 0.35; // Plus élevé = mouvements plus réactifs mais plus saccadés, plus bas = mouvements plus fluides mais plus lents à réagir. Un nombre autour de 0.3 à 0.4 est souvent un bon compromis pour les jeux d'action en 2D, offrant une bonne réactivité tout en gardant les mouvements relativement fluides.
    private static final double ENEMY_AIM_ERROR_CLOSE_DEG = 2.2; // Erreur d'aim pour les ennemis à courte distance, en degrés. Un nombre plus élevé rend les ennemis moins précis à courte portée, ce qui peut être utile pour équilibrer les armes très puissantes ou à tir rapide, tandis qu'un nombre plus bas les rend plus précis et plus dangereux à courte portée.
    private static final double ENEMY_AIM_ERROR_FAR_DEG = 8.5; // Erreur d'aim pour les ennemis à longue distance, en degrés. Un nombre plus élevé rend les ennemis moins précis à longue portée, ce qui peut être utile pour équilibrer les armes à longue portée ou pour rendre les combats à distance plus dynamiques, tandis qu'un nombre plus bas les rend plus précis et plus dangereux à longue portée.

    private int shootCooldown = 0;
    private int losCheckCooldown = 0;
    private Homme losCachedTarget;
    private boolean losCachedVisible;
    private int reactionDelayTimer = 0;
    private int aimSettleTimer = 0;
    private Homme focusedTarget;
    private int strafeSign = Math.random() < 0.5 ? -1 : 1;
    private int strafeSwitchTimer = 0;
    private int coverMemoryTimer = 0;
    private double coverX;
    private double coverY;

    public void update(Ennemi enemy) {
        enemy.tickSuppression();

        if (shootCooldown > 0) {
            shootCooldown--;
        }

        Homme target = ObjectManager.getNearestAlliedTarget(enemy.x, enemy.y);
        if (target == null) {
            applySmoothedVelocity(enemy, 0, 0);
            focusedTarget = null;
            clearCover();
            return;
        }

        if (focusedTarget != target) {
            focusedTarget = target;
            reactionDelayTimer = AiTuning.getEnemyReactionFrames();
            aimSettleTimer = AiTuning.getEnemyAimStabilizationFrames();
        }

        double dx = target.x - enemy.x;
        double dy = target.y - enemy.y;
        double distSq = dx * dx + dy * dy;
        if (distSq <= EPSILON_DIST_SQ) {
            applySmoothedVelocity(enemy, 0, 0);
            clearCover();
            return;
        }

        double invDist = 1.0 / Math.sqrt(distSq);
        double dist = 1.0 / invDist;
        Weapon weapon = enemy.getCarriedWeapon();
        double retreatRange = weapon.getAiRetreatRange();
        double optimalRange = weapon.getAiOptimalRange();
        double engageRange = weapon.getAiEngageRange();
        boolean hasLineOfSight = canSeeTarget(enemy, target, distSq);

        enemy.setFacingDirection(dx, dy);

        if (reactionDelayTimer > 0) {
            reactionDelayTimer--;
            applySmoothedVelocity(enemy, 0, 0);
            return;
        }

        if (hasLineOfSight) {
            if (aimSettleTimer > 0) {
                aimSettleTimer--;
            }
        } else {
            aimSettleTimer = AiTuning.getEnemyAimStabilizationFrames();
        }

        if (shouldSeekCover(enemy, weapon, dist, hasLineOfSight)) {
            if (moveToCover(enemy, target, optimalRange)) {
                return;
            }
        }

        if (coverMemoryTimer > 0) {
            coverMemoryTimer--;
        } else {
            clearCover();
        }

        if (hasLineOfSight && dist <= engageRange) {
            applyCombatMovement(enemy, dx, dy, invDist, dist, retreatRange, optimalRange);

            if (shootCooldown == 0 && aimSettleTimer <= 0) {
                double[] shotDir = applyAimError(enemy, dx * invDist, dy * invDist, dist);
                enemy.getCarriedWeapon().fire(enemy, shotDir[0], shotDir[1]);
                enemy.onShot();
                shootCooldown = enemy.getCarriedWeapon().getCooldownFrames();
                aimSettleTimer = AiTuning.getEnemyAimStabilizationFrames();
            }
            return;
        }

        clearCover();
        double pushFactor = weapon.isShotgun() ? AGGRESSIVE_PUSH_FACTOR : 1.0;
        double desiredVx = dx * invDist * CHASE_SPEED * pushFactor;
        double desiredVy = dy * invDist * CHASE_SPEED * pushFactor;
        double[] adjusted = TacticalMovement.adjustForObstacles(enemy.x, enemy.y, desiredVx, desiredVy, ENTITY_RADIUS);
        applySmoothedVelocity(enemy, adjusted[0], adjusted[1]);
    }

    private boolean canSeeTarget(Ennemi enemy, Homme target, double distSq) {
        double maxVisionRange = enemy.getCarriedWeapon().getAiEngageRange() * 1.25;
        if (distSq > maxVisionRange * maxVisionRange) {
            losCachedTarget = null;
            losCheckCooldown = 0;
            return false;
        }

        if (target != losCachedTarget) {
            losCachedTarget = target;
            losCheckCooldown = 0;
        }

        if (losCheckCooldown <= 0) {
            losCachedVisible = TacticalMovement.hasLineOfSight(enemy.x, enemy.y, target.x, target.y);
            losCheckCooldown = LOS_CHECK_INTERVAL_FRAMES;
        } else {
            losCheckCooldown--;
        }

        return losCachedVisible;
    }

    private boolean shouldSeekCover(Ennemi enemy, Weapon weapon, double dist, boolean hasLineOfSight) {
        double suppressionBonus = enemy.isSuppressed() ? AiTuning.getSuppressionCoverBoost() * enemy.getSuppressionLevel() : 0.0;

        if (!hasLineOfSight) {
            return dist <= weapon.getAiEngageRange() * (1.15 + suppressionBonus * 0.4);
        }

        int cooldownFrames = Math.max(1, weapon.getCooldownFrames());
        double ratio = shootCooldown / (double) cooldownFrames;
        double threshold = Math.max(0.12, COVER_SEARCH_MIN_COOLDOWN_RATIO - suppressionBonus * 0.22);
        return ratio >= threshold && dist <= weapon.getAiEngageRange();
    }

    private boolean moveToCover(Ennemi enemy, Homme target, double preferredRange) {
        if (coverMemoryTimer <= 0 || !TacticalMovement.hasLineOfSight(enemy.x, enemy.y, coverX, coverY)) {
            double[] cover = TacticalMovement.findCoverPoint(
                    enemy.x,
                    enemy.y,
                    target.x,
                    target.y,
                    ENTITY_RADIUS,
                    preferredRange
            );

            if (cover == null) {
                clearCover();
                return false;
            }

            coverX = cover[0];
            coverY = cover[1];
            coverMemoryTimer = COVER_MEMORY_FRAMES;
        }

        double toCoverX = coverX - enemy.x;
        double toCoverY = coverY - enemy.y;
        double coverDistSq = toCoverX * toCoverX + toCoverY * toCoverY;
        if (coverDistSq <= 12.0 * 12.0) {
            applySmoothedVelocity(enemy, 0, 0);
            return true;
        }

        double invCoverDist = 1.0 / Math.sqrt(coverDistSq);
        double desiredVx = toCoverX * invCoverDist * CHASE_SPEED;
        double desiredVy = toCoverY * invCoverDist * CHASE_SPEED;
        double[] adjusted = TacticalMovement.adjustForObstacles(enemy.x, enemy.y, desiredVx, desiredVy, ENTITY_RADIUS);
        applySmoothedVelocity(enemy, adjusted[0], adjusted[1]);
        enemy.setFacingDirection(target.x - enemy.x, target.y - enemy.y);
        return true;
    }

    private void applyCombatMovement(
            Ennemi enemy,
            double dx,
            double dy,
            double invDist,
            double dist,
            double retreatRange,
            double optimalRange
    ) {
        if (strafeSwitchTimer <= 0) {
            strafeSign = Math.random() < 0.5 ? -1 : 1;
            strafeSwitchTimer = 24 + (int) (Math.random() * 24);
        } else {
            strafeSwitchTimer--;
        }

        double dirX = dx * invDist;
        double dirY = dy * invDist;

        double forwardFactor = 0;
        if (dist < retreatRange) {
            forwardFactor = -0.95;
        } else if (dist < optimalRange * 0.90) {
            forwardFactor = -0.35;
        } else if (dist > optimalRange) {
            forwardFactor = enemy.getCarriedWeapon().isShotgun() ? 0.30 : 0.18;
        }

        double strafeFactor = enemy.getCarriedWeapon().isShotgun() ? 0.45 : 0.72;
        double perpX = -dirY * strafeSign;
        double perpY = dirX * strafeSign;

        double rawVx = (dirX * forwardFactor + perpX * strafeFactor) * CHASE_SPEED;
        double rawVy = (dirY * forwardFactor + perpY * strafeFactor) * CHASE_SPEED;
        double[] adjusted = TacticalMovement.adjustForObstacles(enemy.x, enemy.y, rawVx, rawVy, ENTITY_RADIUS);
        applySmoothedVelocity(enemy, adjusted[0], adjusted[1]);
    }

    private void clearCover() {
        coverMemoryTimer = 0;
    }

    private void applySmoothedVelocity(Ennemi enemy, double desiredVx, double desiredVy) {
        enemy.vx = enemy.vx * (1.0 - VELOCITY_BLEND) + desiredVx * VELOCITY_BLEND;
        enemy.vy = enemy.vy * (1.0 - VELOCITY_BLEND) + desiredVy * VELOCITY_BLEND;
        if (Math.abs(enemy.vx) < 0.01) {
            enemy.vx = 0;
        }
        if (Math.abs(enemy.vy) < 0.01) {
            enemy.vy = 0;
        }
    }

    private double[] applyAimError(Ennemi enemy, double dirX, double dirY, double dist) {
        double engageRange = Math.max(1.0, enemy.getCarriedWeapon().getAiEngageRange());
        double t = Math.max(0.0, Math.min(1.0, dist / engageRange));
        double baseErrorDeg = ENEMY_AIM_ERROR_CLOSE_DEG + (ENEMY_AIM_ERROR_FAR_DEG - ENEMY_AIM_ERROR_CLOSE_DEG) * t;
        double movementPenalty = Math.hypot(enemy.vx, enemy.vy) > 0.2 ? 1.5 : 0.0;
        double suppressionPenalty = enemy.isSuppressed() ? 2.5 * enemy.getSuppressionLevel() : 0.0;
        double maxErrorRad = Math.toRadians(baseErrorDeg + movementPenalty + suppressionPenalty);

        double randomAngle = (Math.random() * 2.0 - 1.0) * maxErrorRad;
        double cos = Math.cos(randomAngle);
        double sin = Math.sin(randomAngle);
        double outX = dirX * cos - dirY * sin;
        double outY = dirX * sin + dirY * cos;
        double len = Math.sqrt(outX * outX + outY * outY);

        if (len <= 0.0001) {
            return new double[]{dirX, dirY};
        }

        return new double[]{outX / len, outY / len};
    }
}
