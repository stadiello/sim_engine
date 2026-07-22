package object.ai;

import main.GameMode;
import main.Utils;
import object.Ennemi;
import object.Homme;
import object.ObjectManager;
import object.weapon.Weapon;

public class BotBrain {

    private static final double ARCADE_CHASE_SPEED_MULTIPLIER = 1.16;
    private static final double ARCADE_PUSH_BONUS = 0.10;
    private static final double ARCADE_COVER_THRESHOLD_DELTA = 0.14;
    private static final double ARCADE_VELOCITY_BLEND_BONUS = 0.10;
    private static final double ARCADE_AIM_ERROR_SCALE = 0.90;

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
    private static final int BURST_PAUSE_MIN_FRAMES = 6;
    private static final int BURST_PAUSE_MAX_FRAMES = 18;
    private static final int SUPPRESSIVE_FIRE_MIN_FRAMES = 10;
    private static final int SUPPRESSIVE_FIRE_MAX_FRAMES = 24;
    private static final int SUPPRESSIVE_REARM_MIN_FRAMES = 90;
    private static final int SUPPRESSIVE_REARM_MAX_FRAMES = 170;
    private static final int FLANK_MEMORY_FRAMES = 46;
    private static final double FLANK_MIN_RANGE = 120.0;
    private static final double FLANK_SIDE_OFFSET = 132.0;
    private static final double FLANK_BACK_OFFSET = 56.0;
    private static final double COVER_PEEK_OFFSET = 15.0;
    private static final int COVER_PEEK_MEMORY_FRAMES = 26;

    private int shootCooldown = 0;
    private int reloadTimer = 0;
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
    private int burstShotsRemaining = 0;
    private int burstPauseTimer = 0;
    private int suppressiveFireTimer = 0;
    private int suppressiveRearminTimer = 0;
    private int flankMemoryTimer = 0;
    private double flankX;
    private double flankY;
    private int flankDirectionSign = 0;
    private int coverPeekSideSign = 0;
    private int coverPeekMemoryTimer = 0;

    public void update(Ennemi enemy) {
        enemy.tickSuppression();

        if (shootCooldown > 0) {
            shootCooldown--;
        }
        if (reloadTimer > 0) {
            reloadTimer--;
            if (reloadTimer == 0) {
                enemy.getCarriedWeapon().reload();
            }
        }
        if (burstPauseTimer > 0) {
            burstPauseTimer--;
        }
        if (suppressiveFireTimer > 0) {
            suppressiveFireTimer--;
        }
        if (suppressiveRearminTimer > 0) {
            suppressiveRearminTimer--;
        }

        Homme target = ObjectManager.getNearestAlliedTarget(enemy.x, enemy.y);
        if (target == null) {
            applySmoothedVelocity(enemy, 0, 0);
            focusedTarget = null;
            clearCover();
            clearFlank();
            return;
        }

        if (focusedTarget != target) {
            focusedTarget = target;
            reactionDelayTimer = getEnemyReactionFrames();
            aimSettleTimer = getEnemyAimStabilizationFrames();
            burstShotsRemaining = 0;
            burstPauseTimer = 0;
            suppressiveFireTimer = 0;
            suppressiveRearminTimer = 0;
            reloadTimer = 0;
            clearFlank();
            coverPeekMemoryTimer = 0;
            coverPeekSideSign = 0;
        }

        double dx = target.x - enemy.x;
        double dy = target.y - enemy.y;
        double distSq = dx * dx + dy * dy;
        if (distSq <= EPSILON_DIST_SQ) {
            applySmoothedVelocity(enemy, 0, 0);
            clearCover();
            clearFlank();
            return;
        }

        double invDist = 1.0 / Math.sqrt(distSq);
        double dist = 1.0 / invDist;
        Weapon weapon = enemy.getCarriedWeapon();
        if (reloadTimer == 0 && weapon.getAmmoInMagazine() <= 0 && weapon.canReload()) {
            startReload(weapon);
        }

        double retreatRange = weapon.getAiRetreatRange();
        double optimalRange = weapon.getAiOptimalRange();
        double engageRange = weapon.getAiEngageRange();
        int suppressionStage = enemy.getSuppressionStage();
        double suppressionLevel = enemy.getSuppressionLevel();
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
            aimSettleTimer = getEnemyAimStabilizationFrames();
            double suppressiveChance = enemy.isHeavy() ? 0.075 : enemy.isBreacher() ? 0.055 : enemy.isFlanker() ? 0.030 : 0.035;
            if (suppressionStage <= 2
                    && dist <= engageRange * 1.1
                    && suppressiveFireTimer <= 0
                    && suppressiveRearminTimer <= 0
                    && Math.random() < suppressiveChance) {
                suppressiveFireTimer = Math.max(
                        suppressiveFireTimer,
                        SUPPRESSIVE_FIRE_MIN_FRAMES + (int) (Math.random() * (SUPPRESSIVE_FIRE_MAX_FRAMES - SUPPRESSIVE_FIRE_MIN_FRAMES + 1))
                );
                int rearminBase = enemy.isHeavy()
                    ? SUPPRESSIVE_REARM_MIN_FRAMES - 35
                    : enemy.isBreacher() ? SUPPRESSIVE_REARM_MIN_FRAMES - 18 : SUPPRESSIVE_REARM_MIN_FRAMES;
                suppressiveRearminTimer = Math.max(42, rearminBase)
                        + (int) (Math.random() * (SUPPRESSIVE_REARM_MAX_FRAMES - SUPPRESSIVE_REARM_MIN_FRAMES + 1));
            }
            if (suppressiveFireTimer > 0 && shootCooldown == 0 && burstPauseTimer == 0 && reloadTimer == 0) {
                double[] shotDir = applySuppressiveSpread(dx * invDist, dy * invDist, suppressionStage, suppressionLevel);
                if (fireEnemyWeapon(enemy, shotDir[0], shotDir[1])) {
                    int rolePauseDelta = enemy.isHeavy() ? -3 : enemy.isBreacher() ? -2 : 0;
                    burstPauseTimer = Math.max(
                            1,
                            BURST_PAUSE_MIN_FRAMES + 2 + rolePauseDelta + (int) (Math.random() * (BURST_PAUSE_MAX_FRAMES - BURST_PAUSE_MIN_FRAMES + 1))
                    );
                    if (isArcadeMode()) {
                        burstPauseTimer = Math.max(1, burstPauseTimer - 2);
                    }
                }
            }
        }

        if (enemy.isFlanker() && tryMoveToFlank(enemy, target, dist, engageRange, optimalRange, hasLineOfSight)) {
            return;
        }

        if (shouldSeekCover(enemy, weapon, dist, hasLineOfSight, suppressionStage)) {
            if (moveToCover(enemy, target, optimalRange)) {
                return;
            }
        }

        if (suppressionStage >= 3 && hasLineOfSight) {
            applySuppressedFallbackMovement(enemy, dx, dy, invDist, dist, retreatRange, suppressionLevel);
            return;
        }

        if (coverMemoryTimer > 0) {
            coverMemoryTimer--;
        } else {
            clearCover();
        }

        if (coverPeekMemoryTimer > 0) {
            coverPeekMemoryTimer--;
        } else {
            coverPeekSideSign = 0;
        }

        if (hasLineOfSight && dist <= engageRange) {
            applyCombatMovement(enemy, dx, dy, invDist, dist, retreatRange, optimalRange);

            if (shootCooldown == 0 && reloadTimer == 0 && aimSettleTimer <= 0 && canFireInCurrentBurst(enemy, enemy.getCarriedWeapon(), suppressionStage)) {
                double[] shotDir = applyAimError(enemy, dx * invDist, dy * invDist, dist, suppressionStage, suppressionLevel);
                if (fireEnemyWeapon(enemy, shotDir[0], shotDir[1])) {
                    aimSettleTimer = getEnemyAimStabilizationFrames();
                    consumeBurstShot(enemy, enemy.getCarriedWeapon(), suppressionStage);
                }
            }
            return;
        }

        clearCover();
        double moveSpeed = getChaseSpeed() * enemy.getSuppressionMoveMultiplier() * getRoleSpeedMultiplier(enemy);
        double pushFactor = weapon.isShotgun() ? AGGRESSIVE_PUSH_FACTOR : 1.0;
        if (isArcadeMode()) {
            pushFactor += ARCADE_PUSH_BONUS;
        }
        if (enemy.isHeavy()) {
            pushFactor += 0.18;
        } else if (enemy.isBreacher()) {
            pushFactor += 0.10;
        } else if (enemy.isFlanker()) {
            pushFactor *= 0.95;
        }
        double desiredVx = dx * invDist * moveSpeed * pushFactor;
        double desiredVy = dy * invDist * moveSpeed * pushFactor;
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

    private boolean shouldSeekCover(Ennemi enemy, Weapon weapon, double dist, boolean hasLineOfSight, int suppressionStage) {
        double suppressionBonus = enemy.isSuppressed() ? AiTuning.getSuppressionCoverBoost() * enemy.getSuppressionLevel() : 0.0;
        double roleCoverBias = enemy.isHeavy() ? 0.52 : enemy.isBreacher() ? 0.62 : enemy.isFlanker() ? 0.88 : 1.0;

        if (!hasLineOfSight) {
            double chaseWindow = suppressionStage >= 2 ? 1.35 : 1.15;
            if (enemy.isFlanker() && flankMemoryTimer > 0) {
                return false;
            }
            return dist <= weapon.getAiEngageRange() * (chaseWindow + suppressionBonus * 0.4 * roleCoverBias);
        }

        int cooldownFrames = Math.max(1, weapon.getCooldownFrames());
        double ratio = shootCooldown / (double) cooldownFrames;
        double threshold = Math.max(0.08, COVER_SEARCH_MIN_COOLDOWN_RATIO - suppressionBonus * 0.22 - suppressionStage * 0.06);
        if (isArcadeMode()) {
            threshold = Math.max(0.08, threshold - ARCADE_COVER_THRESHOLD_DELTA);
        }
        threshold += enemy.isHeavy() ? 0.30 : enemy.isBreacher() ? 0.20 : enemy.isFlanker() ? 0.08 : 0.0;
        double engageWindow = suppressionStage >= 3 ? weapon.getAiEngageRange() * 1.15 : weapon.getAiEngageRange();
        return ratio >= threshold && dist <= engageWindow * roleCoverBias;
    }

    private boolean moveToCover(Ennemi enemy, Homme target, double preferredRange) {
        double moveSpeed = getChaseSpeed() * enemy.getSuppressionMoveMultiplier();

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
            enemy.setFacingDirection(target.x - enemy.x, target.y - enemy.y);
            tryFireFromCoverPeek(enemy, target);
            return true;
        }

        double invCoverDist = 1.0 / Math.sqrt(coverDistSq);
        double desiredVx = toCoverX * invCoverDist * moveSpeed;
        double desiredVy = toCoverY * invCoverDist * moveSpeed;
        double[] adjusted = TacticalMovement.adjustForObstacles(enemy.x, enemy.y, desiredVx, desiredVy, ENTITY_RADIUS);
        applySmoothedVelocity(enemy, adjusted[0], adjusted[1]);
        enemy.setFacingDirection(target.x - enemy.x, target.y - enemy.y);
        return true;
    }

    private void tryFireFromCoverPeek(Ennemi enemy, Homme target) {
        if (shootCooldown > 0 || burstPauseTimer > 0 || reloadTimer > 0) {
            return;
        }

        double toTargetX = target.x - enemy.x;
        double toTargetY = target.y - enemy.y;
        double distSq = toTargetX * toTargetX + toTargetY * toTargetY;
        if (distSq <= EPSILON_DIST_SQ) {
            return;
        }

        double invDist = 1.0 / Math.sqrt(distSq);
        double dirX = toTargetX * invDist;
        double dirY = toTargetY * invDist;
        double perpX = -dirY;
        double perpY = dirX;

        int preferredSide = coverPeekSideSign == 0 ? (Math.random() < 0.5 ? -1 : 1) : coverPeekSideSign;
        int[] sideOrder = {preferredSide, -preferredSide};

        double bestOriginX = 0;
        double bestOriginY = 0;
        int bestSide = 0;

        for (int sideSign : sideOrder) {
            double candidateX = enemy.x + perpX * sideSign * COVER_PEEK_OFFSET;
            double candidateY = enemy.y + perpY * sideSign * COVER_PEEK_OFFSET;
            if (!TacticalMovement.hasLineOfSight(enemy.x, enemy.y, candidateX, candidateY)) {
                continue;
            }
            if (!TacticalMovement.hasLineOfSight(candidateX, candidateY, target.x, target.y)) {
                continue;
            }

            bestOriginX = candidateX;
            bestOriginY = candidateY;
            bestSide = sideSign;
            break;
        }

        if (bestSide == 0) {
            return;
        }

        coverPeekSideSign = bestSide;
        coverPeekMemoryTimer = COVER_PEEK_MEMORY_FRAMES;

        int suppressionStage = enemy.getSuppressionStage();
        if (!canFireInCurrentBurst(enemy, enemy.getCarriedWeapon(), suppressionStage)) {
            return;
        }

        double shotDx = target.x - bestOriginX;
        double shotDy = target.y - bestOriginY;
        double shotDist = Math.hypot(shotDx, shotDy);
        if (shotDist <= 0.0001) {
            return;
        }

        double[] shotDir = applyAimError(
                enemy,
                shotDx / shotDist,
                shotDy / shotDist,
                shotDist,
                suppressionStage,
                enemy.getSuppressionLevel()
        );

        if (fireEnemyWeapon(enemy, bestOriginX, bestOriginY, shotDir[0], shotDir[1])) {
            aimSettleTimer = Math.max(1, AiTuning.getEnemyAimStabilizationFrames() / 2);
            consumeBurstShot(enemy, enemy.getCarriedWeapon(), suppressionStage);
        }
    }

    private boolean fireEnemyWeapon(Ennemi enemy, double dirX, double dirY) {
        return fireEnemyWeapon(enemy, enemy.x, enemy.y, dirX, dirY);
    }

    private boolean fireEnemyWeapon(Ennemi enemy, double originX, double originY, double dirX, double dirY) {
        Weapon weapon = enemy.getCarriedWeapon();
        if (reloadTimer > 0) {
            return false;
        }

        if (weapon.getAmmoInMagazine() <= 0) {
            startReload(weapon);
            return false;
        }

        if (!weapon.fire(enemy, originX, originY, dirX, dirY)) {
            startReload(weapon);
            return false;
        }

        enemy.onShot();
        shootCooldown = weapon.getCooldownFrames();
        if (weapon.getAmmoInMagazine() <= 0 && weapon.canReload()) {
            startReload(weapon);
        }
        return true;
    }

    private void startReload(Weapon weapon) {
        if (weapon == null || reloadTimer > 0 || !weapon.canReload()) {
            return;
        }

        reloadTimer = weapon.getReloadFrames();
        Utils.playReloadSound();
    }

    private boolean tryMoveToFlank(
            Ennemi enemy,
            Homme target,
            double dist,
            double engageRange,
            double preferredRange,
            boolean hasLineOfSight
    ) {
        if (dist < FLANK_MIN_RANGE || dist > engageRange * 1.35) {
            clearFlank();
            return false;
        }

        if (flankMemoryTimer <= 0 || !isFlankPointUsable(target, preferredRange)) {
            if (!selectFlankPoint(enemy, target, preferredRange, hasLineOfSight)) {
                clearFlank();
                return false;
            }
        } else {
            flankMemoryTimer--;
        }

        double toFlankX = flankX - enemy.x;
        double toFlankY = flankY - enemy.y;
        double flankDistSq = toFlankX * toFlankX + toFlankY * toFlankY;
        if (flankDistSq <= 14.0 * 14.0) {
            applySmoothedVelocity(enemy, 0, 0);
            enemy.setFacingDirection(target.x - enemy.x, target.y - enemy.y);
            return true;
        }

        double moveSpeed = CHASE_SPEED * enemy.getSuppressionMoveMultiplier() * 1.08;
        if (isArcadeMode()) {
            moveSpeed *= ARCADE_CHASE_SPEED_MULTIPLIER;
        }
        double invDist = 1.0 / Math.sqrt(flankDistSq);
        double desiredVx = toFlankX * invDist * moveSpeed;
        double desiredVy = toFlankY * invDist * moveSpeed;
        double[] adjusted = TacticalMovement.adjustForObstacles(enemy.x, enemy.y, desiredVx, desiredVy, ENTITY_RADIUS);
        applySmoothedVelocity(enemy, adjusted[0], adjusted[1]);
        enemy.setFacingDirection(target.x - enemy.x, target.y - enemy.y);
        return true;
    }

    private boolean isFlankPointUsable(Homme target, double preferredRange) {
        if (!TacticalMovement.canStandAt(flankX, flankY, ENTITY_RADIUS)) {
            return false;
        }

        double distToTarget = Math.hypot(target.x - flankX, target.y - flankY);
        return distToTarget >= preferredRange * 0.45 && distToTarget <= preferredRange * 1.50;
    }

    private boolean selectFlankPoint(Ennemi enemy, Homme target, double preferredRange, boolean hasLineOfSight) {
        double toTargetX = target.x - enemy.x;
        double toTargetY = target.y - enemy.y;
        double toTargetLenSq = toTargetX * toTargetX + toTargetY * toTargetY;
        if (toTargetLenSq <= EPSILON_DIST_SQ) {
            return false;
        }

        double invLen = 1.0 / Math.sqrt(toTargetLenSq);
        double dirX = toTargetX * invLen;
        double dirY = toTargetY * invLen;
        int preferredSide = flankDirectionSign == 0 ? (Math.random() < 0.5 ? -1 : 1) : flankDirectionSign;

        double bestScore = Double.NEGATIVE_INFINITY;
        double bestX = 0;
        double bestY = 0;
        int bestSide = 0;

        int[] sideOrder = {preferredSide, -preferredSide};
        for (int sideSign : sideOrder) {
            double perpX = -dirY * sideSign;
            double perpY = dirX * sideSign;

            double candidateX = target.x + perpX * FLANK_SIDE_OFFSET - dirX * FLANK_BACK_OFFSET;
            double candidateY = target.y + perpY * FLANK_SIDE_OFFSET - dirY * FLANK_BACK_OFFSET;

            if (!TacticalMovement.canStandAt(candidateX, candidateY, ENTITY_RADIUS)) {
                continue;
            }
            if (!ObjectManager.isHumanAreaFree(candidateX, candidateY, ENTITY_RADIUS, enemy)) {
                continue;
            }

            double selfTravel = Math.hypot(candidateX - enemy.x, candidateY - enemy.y);
            double distToTarget = Math.hypot(target.x - candidateX, target.y - candidateY);
            double rangePenalty = Math.abs(distToTarget - preferredRange);
            boolean hasFlankSight = TacticalMovement.hasLineOfSight(candidateX, candidateY, target.x, target.y);

            double score = 1000.0 - selfTravel * 1.05 - rangePenalty * 0.90;
            if (hasFlankSight) {
                score += 150.0;
            }
            if (!hasLineOfSight) {
                score += 65.0;
            }

            if (score > bestScore) {
                bestScore = score;
                bestX = candidateX;
                bestY = candidateY;
                bestSide = sideSign;
            }
        }

        if (bestSide == 0) {
            return false;
        }

        flankX = bestX;
        flankY = bestY;
        flankDirectionSign = bestSide;
        flankMemoryTimer = FLANK_MEMORY_FRAMES;
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
        int suppressionStage = enemy.getSuppressionStage();
        double suppressionLevel = enemy.getSuppressionLevel();
        double moveSpeed = getChaseSpeed() * enemy.getSuppressionMoveMultiplier() * getRoleSpeedMultiplier(enemy);

        if (strafeSwitchTimer <= 0) {
            int baseSwitch = suppressionStage >= 2 ? 16 : 24;
            int variance = suppressionStage >= 2 ? 16 : 24;
            if (enemy.isFlanker()) {
                baseSwitch += 10;
                variance += 8;
            } else if (enemy.isHeavy()) {
                baseSwitch = Math.max(14, baseSwitch - 8);
                variance = Math.max(8, variance - 10);
            } else if (enemy.isBreacher()) {
                baseSwitch = Math.max(10, baseSwitch - 5);
            }

            if (!enemy.isFlanker() || Math.random() < 0.45) {
                strafeSign = Math.random() < 0.5 ? -1 : 1;
            }

            strafeSwitchTimer = baseSwitch + (int) (Math.random() * Math.max(1, variance));
        } else {
            strafeSwitchTimer--;
        }

        double dirX = dx * invDist;
        double dirY = dy * invDist;

        double forwardFactor = 0;
        if (suppressionStage >= 3) {
            forwardFactor = dist < retreatRange * 1.15 ? -1.10 : -0.55;
        } else if (suppressionStage == 2) {
            if (dist < retreatRange) {
                forwardFactor = -1.0;
            } else if (dist < optimalRange) {
                forwardFactor = -0.50;
            } else {
                forwardFactor = 0.10;
            }
        } else {
            if (dist < retreatRange) {
                forwardFactor = -0.95;
            } else if (dist < optimalRange * 0.90) {
                forwardFactor = -0.35;
            } else if (dist > optimalRange) {
                forwardFactor = enemy.getCarriedWeapon().isShotgun() ? 0.30 : 0.18;
            }
        }

        double strafeFactor = enemy.getCarriedWeapon().isShotgun() ? 0.45 : 0.72;
        if (suppressionStage >= 2) {
            strafeFactor *= 0.60;
        } else if (suppressionStage == 1) {
            strafeFactor *= 1.10;
        }

        if (enemy.isFlanker()) {
            forwardFactor += 0.12;
            strafeFactor *= 1.42;
        } else if (enemy.isBreacher()) {
            forwardFactor += 0.22;
            strafeFactor *= 0.62;
        }

        strafeFactor *= 1.0 - 0.12 * suppressionLevel;
        double perpX = -dirY * strafeSign;
        double perpY = dirX * strafeSign;

        double rawVx = (dirX * forwardFactor + perpX * strafeFactor) * moveSpeed;
        double rawVy = (dirY * forwardFactor + perpY * strafeFactor) * moveSpeed;
        double[] adjusted = TacticalMovement.adjustForObstacles(enemy.x, enemy.y, rawVx, rawVy, ENTITY_RADIUS);
        applySmoothedVelocity(enemy, adjusted[0], adjusted[1]);
    }

    private void applySuppressedFallbackMovement(
            Ennemi enemy,
            double dx,
            double dy,
            double invDist,
            double dist,
            double retreatRange,
            double suppressionLevel
    ) {
        double dirX = dx * invDist;
        double dirY = dy * invDist;
        double panicFactor = dist < retreatRange ? -1.15 : -0.75;
        double jitterFactor = 0.18 + suppressionLevel * 0.28;
        double sideSign = Math.random() < 0.5 ? -1 : 1;
        double perpX = -dirY * sideSign;
        double perpY = dirX * sideSign;
        double moveSpeed = getChaseSpeed() * enemy.getSuppressionMoveMultiplier() * getRoleSpeedMultiplier(enemy) * 0.85;

        double rawVx = (dirX * panicFactor + perpX * jitterFactor) * moveSpeed;
        double rawVy = (dirY * panicFactor + perpY * jitterFactor) * moveSpeed;
        double[] adjusted = TacticalMovement.adjustForObstacles(enemy.x, enemy.y, rawVx, rawVy, ENTITY_RADIUS);
        applySmoothedVelocity(enemy, adjusted[0], adjusted[1]);
    }

    private void clearCover() {
        coverMemoryTimer = 0;
        coverPeekMemoryTimer = 0;
        coverPeekSideSign = 0;
    }

    private void clearFlank() {
        flankMemoryTimer = 0;
    }

    private double getRoleSpeedMultiplier(Ennemi enemy) {
        if (enemy.isFlanker()) {
            return 1.16;
        }
        if (enemy.isHeavy()) {
            return 0.62;
        }
        if (enemy.isBreacher()) {
            return 1.08;
        }
        return 1.0;
    }

    private boolean canFireInCurrentBurst(Ennemi enemy, Weapon weapon, int suppressionStage) {
        if (burstPauseTimer > 0) {
            return false;
        }
        if (burstShotsRemaining > 0) {
            return true;
        }

        int burstSize = chooseBurstSize(enemy, weapon, suppressionStage);
        burstShotsRemaining = Math.max(1, burstSize);
        return true;
    }

    private void consumeBurstShot(Ennemi enemy, Weapon weapon, int suppressionStage) {
        if (burstShotsRemaining > 0) {
            burstShotsRemaining--;
        }
        if (burstShotsRemaining <= 0) {
            burstPauseTimer = BURST_PAUSE_MIN_FRAMES + (int) (Math.random() * (BURST_PAUSE_MAX_FRAMES - BURST_PAUSE_MIN_FRAMES + 1));
            if (!weapon.isAutomatic()) {
                burstPauseTimer += 3;
            }
            if (suppressionStage >= 2) {
                burstPauseTimer += 2;
            }
            if (enemy.isHeavy()) {
                burstPauseTimer = Math.max(1, burstPauseTimer - 3);
            } else if (enemy.isBreacher()) {
                burstPauseTimer = Math.max(1, burstPauseTimer - 2);
            } else if (enemy.isFlanker()) {
                burstPauseTimer += 1;
            }
        }
    }

    private int chooseBurstSize(Ennemi enemy, Weapon weapon, int suppressionStage) {
        int minBurst;
        int maxBurst;

        if (weapon.isShotgun()) {
            minBurst = 1;
            maxBurst = 2;
        } else if (weapon.isAutomatic()) {
            minBurst = 3;
            maxBurst = 5;
        } else {
            minBurst = 2;
            maxBurst = 3;
        }

        if (suppressionStage >= 2) {
            maxBurst = Math.max(minBurst, maxBurst - 1);
        }

        if (enemy.isHeavy()) {
            minBurst += 1;
            maxBurst += 2;
        } else if (enemy.isBreacher()) {
            maxBurst += 1;
        } else if (enemy.isFlanker() && weapon.isAutomatic()) {
            maxBurst = Math.max(minBurst, maxBurst - 1);
        }

        return minBurst + (int) (Math.random() * (maxBurst - minBurst + 1));
    }

    private double[] applySuppressiveSpread(double dirX, double dirY, int suppressionStage, double suppressionLevel) {
        double extraErrorDeg = 10.0 + suppressionStage * 2.5 + suppressionLevel * 3.0;
        double randomAngle = (Math.random() * 2.0 - 1.0) * Math.toRadians(extraErrorDeg);
        double cos = Math.cos(randomAngle);
        double sin = Math.sin(randomAngle);
        double outX = dirX * cos - dirY * sin;
        double outY = dirX * sin + dirY * cos;
        double len = Math.hypot(outX, outY);
        if (len <= 0.0001) {
            return new double[]{dirX, dirY};
        }
        return new double[]{outX / len, outY / len};
    }

    private void applySmoothedVelocity(Ennemi enemy, double desiredVx, double desiredVy) {
        double blend = getVelocityBlend();
        enemy.vx = enemy.vx * (1.0 - blend) + desiredVx * blend;
        enemy.vy = enemy.vy * (1.0 - blend) + desiredVy * blend;
        if (Math.abs(enemy.vx) < 0.01) {
            enemy.vx = 0;
        }
        if (Math.abs(enemy.vy) < 0.01) {
            enemy.vy = 0;
        }
    }

    private double[] applyAimError(Ennemi enemy, double dirX, double dirY, double dist, int suppressionStage, double suppressionLevel) {
        double engageRange = Math.max(1.0, enemy.getCarriedWeapon().getAiEngageRange());
        double t = Math.max(0.0, Math.min(1.0, dist / engageRange));
        double baseErrorDeg = ENEMY_AIM_ERROR_CLOSE_DEG + (ENEMY_AIM_ERROR_FAR_DEG - ENEMY_AIM_ERROR_CLOSE_DEG) * t;
        double movementPenalty = Math.hypot(enemy.vx, enemy.vy) > 0.2 ? 1.5 : 0.0;
        double suppressionPenalty = enemy.isSuppressed()
                ? (suppressionStage >= 3 ? 4.0 : suppressionStage == 2 ? 2.8 : 1.6) * suppressionLevel
                : 0.0;
        double maxErrorRad = Math.toRadians((baseErrorDeg + movementPenalty + suppressionPenalty) * (isArcadeMode() ? ARCADE_AIM_ERROR_SCALE : 1.0));

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

    private boolean isArcadeMode() {
        return GameMode.current == GameMode.ARCADE;
    }

    private int getEnemyReactionFrames() {
        int base = AiTuning.getEnemyReactionFrames();
        return isArcadeMode() ? Math.max(2, base - 4) : base;
    }

    private int getEnemyAimStabilizationFrames() {
        int base = AiTuning.getEnemyAimStabilizationFrames();
        return isArcadeMode() ? Math.max(1, base - 2) : base;
    }

    private double getChaseSpeed() {
        return CHASE_SPEED * (isArcadeMode() ? ARCADE_CHASE_SPEED_MULTIPLIER : 1.0);
    }

    private double getVelocityBlend() {
        return Math.min(0.7, VELOCITY_BLEND + (isArcadeMode() ? ARCADE_VELOCITY_BLEND_BONUS : 0.0));
    }
}
