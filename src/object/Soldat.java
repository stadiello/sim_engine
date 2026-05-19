package object;

import java.awt.*;
import java.io.IOException;
import javax.imageio.ImageIO;

import object.ai.AiTuning;
import object.ai.TacticalMovement;
import object.weapon.Weapon;

public class Soldat extends Homme {

    private static final double MOVE_SPEED = 2.4; // Vitesse de déplacement de base du soldat, utilisée pour calculer la vélocité désirée. La vitesse réelle peut être réduite par les obstacles et les ajustements de mouvement tactique.
    private static final double FOLLOW_DISTANCE = 90.0; // Distance à laquelle le soldat commence à suivre le protagoniste s'il n'a pas de cible, pour éviter qu'il ne reste trop loin ou qu'il ne se rapproche trop quand le joueur est à proximité
    private static final double ARRIVAL_RADIUS = 12.0; // Distance à laquelle le soldat considère être arrivé à destination, pour éviter les oscillations et les calculs inutiles de direction quand il est très proche de la cible
    private static final int LOS_CHECK_INTERVAL_FRAMES = 4; // Combien de frames attendre avant de refaire un check de ligne de vue, pour économiser du CPU. Un nombre plus élevé rend les ennemis moins réactifs à l'apparition soudaine d'une cible, mais réduit les saccades quand il y a beaucoup d'ennemis à l'écran.
    private static final double EPSILON_DIST_SQ = 0.00000001; // Seuil pour éviter les divisions par zéro et les calculs inutiles de direction quand la cible est très proche
    private static final double ENTITY_RADIUS = 14.0; // Rayon utilisé pour les vérifications de collision et de ligne de vue, pas forcément égal à la moitié de la taille du sprite
    private static final int COVER_MEMORY_FRAMES = 22; // Combien de temps le soldat "se souvient" d'un point de couverture avant de chercher un nouveau (en frames)
    private static final double VELOCITY_BLEND = 0.34; // Plus élevé = mouvements plus réactifs mais plus saccadés, plus bas = mouvements plus fluides mais plus lents à réagir
    private static Image imgCorps;
    private int shotAnimTimer = 0;

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
    private int reactionDelayTimer = 0;
    private int aimSettleTimer = 0;
    private Homme focusedTarget;
    private final Weapon carriedWeapon;
    private int strafeSign = Math.random() < 0.5 ? -1 : 1;
    private int strafeSwitchTimer = 0;
    private int coverMemoryTimer = 0;
    private double coverX;
    private double coverY;
    private int commandMovePriorityFrames = 0;


    static {
        try {
            imgCorps = ImageIO.read(Soldat.class.getResourceAsStream("/assets/soldats/corps.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Soldat(double x, double y) {
        super(x, y);
        carriedWeapon = pickSoldierWeapon();
        vx = (Math.random() - 0.5) * 3; // Soldats plus rapides que les civils
        vy = (Math.random() - 0.5) * 3;
    }

    private static Weapon pickSoldierWeapon() {
        double r = Math.random();
        if (r < 0.45) {
            return Weapon.blaster();
        }
        if (r < 0.82) {
            return Weapon.carabine();
        }
        if (r < 0.95) {
            return Weapon.glock();
        }
        return Weapon.shotgun();
    }

    public void moveTo(double targetX, double targetY) {
        destinationX = targetX;
        destinationY = targetY;
        hasDestination = true;
        commandMovePriorityFrames = 120;
    }

    @Override
    public void update() {
        tickSuppression();

        if (commandMovePriorityFrames > 0) {
            commandMovePriorityFrames--;
        }

        if (shootCooldown > 0) {
            shootCooldown--;
        }
        if (shotAnimTimer > 0) {
            shotAnimTimer--;
        }

        Homme target = ObjectManager.getNearestHostileForSoldat(x, y);
        boolean hasAttackTarget = false;

        if (target != null) {
            if (focusedTarget != target) {
                focusedTarget = target;
                reactionDelayTimer = AiTuning.getSoldierReactionFrames();
                aimSettleTimer = AiTuning.getSoldierAimStabilizationFrames();
            }

            double dx = target.x - x;
            double dy = target.y - y;
            double distanceSq = dx * dx + dy * dy;
            double engageRange = carriedWeapon.getAiEngageRange();
            double retreatRange = carriedWeapon.getAiRetreatRange();
            double optimalRange = carriedWeapon.getAiOptimalRange();
            int suppressionStage = getSuppressionStage();
            if (distanceSq > EPSILON_DIST_SQ) {
                double invDistance = 1.0 / Math.sqrt(distanceSq);
                setFacingDirection(dx * invDistance, dy * invDistance);
            }

            boolean hasLos = canSeeTarget(target);
            double distance = distanceSq > EPSILON_DIST_SQ ? Math.sqrt(distanceSq) : 0.0;

            if (reactionDelayTimer > 0 && !hasDestination) {
                reactionDelayTimer--;
                vx = 0;
                vy = 0;
                timer++;
                return;
            }

            if (hasLos) {
                if (suppressionStage >= 2) {
                    aimSettleTimer = Math.min(aimSettleTimer + 1, AiTuning.getSoldierAimStabilizationFrames() + 4);
                } else if (aimSettleTimer > 0) {
                    aimSettleTimer--;
                }
            } else {
                aimSettleTimer = AiTuning.getSoldierAimStabilizationFrames();
            }

            if (!hasDestination && shouldSeekCover(hasLos, distance, optimalRange, suppressionStage)) {
                if (moveToCover(target, optimalRange)) {
                    moveWithTileCollision(14);
                    timer++;
                    return;
                }
            }

            if (suppressionStage >= 3 && hasLos) {
                applySuppressedFallbackMovement(dx, dy, distance, retreatRange);
                moveWithTileCollision(14);
                timer++;
                return;
            }

            if (!hasDestination && hasLos && distanceSq <= engageRange * engageRange) {
                applyCombatMovement(dx, dy, distance, retreatRange, optimalRange, suppressionStage);
                hasAttackTarget = true;
                if (shootCooldown == 0 && aimSettleTimer <= 0 && suppressionStage < 3) {
                    carriedWeapon.fire(this, facingX, facingY);
                    shootCooldown = carriedWeapon.getCooldownFrames();
                    shotAnimTimer = 4;
                    aimSettleTimer = AiTuning.getSoldierAimStabilizationFrames();
                } else if (shootCooldown == 0 && aimSettleTimer <= 0 && Math.random() < 0.35) {
                    carriedWeapon.fire(this, facingX, facingY);
                    shootCooldown = carriedWeapon.getCooldownFrames();
                    shotAnimTimer = 4;
                    aimSettleTimer = AiTuning.getSoldierAimStabilizationFrames();
                }
            }
        } else {
            focusedTarget = null;
        }

        if (!hasAttackTarget) {
            if (coverMemoryTimer > 0) {
                coverMemoryTimer--;
            } else {
                clearCover();
            }
            updateMovement();
            moveWithTileCollision(14);
        }

        timer++;
    }

    private void updateMovement() {
        double moveSpeed = MOVE_SPEED * getSuppressionMoveMultiplier();

        if (hasDestination) {
            double dx = destinationX - x;
            double dy = destinationY - y;
            double distanceSq = dx * dx + dy * dy;
            double arrivalRadiusSq = ARRIVAL_RADIUS * ARRIVAL_RADIUS;

            if (distanceSq <= arrivalRadiusSq) {
                vx = 0;
                vy = 0;
                hasDestination = false;
                commandMovePriorityFrames = 0;
                return;
            }

            double invDistance = 1.0 / Math.sqrt(distanceSq);
            double desiredVx = dx * invDistance * moveSpeed;
            double desiredVy = dy * invDistance * moveSpeed;
            double[] adjusted = TacticalMovement.adjustForObstacles(x, y, desiredVx, desiredVy, ENTITY_RADIUS);
            applySmoothedVelocity(adjusted[0], adjusted[1]);  // Applique la vélocité ajustée pour éviter les obstacles
            setFacingDirection(dx * invDistance, dy * invDistance); // Oriente le soldat vers sa destination
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
                double desiredVx = dx * invDistance * moveSpeed;
                double desiredVy = dy * invDistance * moveSpeed;
                double[] adjusted = TacticalMovement.adjustForObstacles(x, y, desiredVx, desiredVy, ENTITY_RADIUS);
                applySmoothedVelocity(adjusted[0], adjusted[1]);
                setFacingDirection(dx * invDistance, dy * invDistance);
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
            losCachedVisible = TacticalMovement.hasLineOfSight(x, y, target.x, target.y);
            losCheckCooldown = LOS_CHECK_INTERVAL_FRAMES;
        } else {
            losCheckCooldown--;
        }

        return losCachedVisible;
    }

    private boolean shouldSeekCover(boolean hasLos, double distance, double optimalRange, int suppressionStage) {
        double suppressionBonus = isSuppressed() ? AiTuning.getSuppressionCoverBoost() * getSuppressionLevel() : 0.0;

        if (!hasLos) {
            double chaseWindow = suppressionStage >= 2 ? 1.35 : 1.20;
            return distance <= carriedWeapon.getAiEngageRange() * (chaseWindow + suppressionBonus * 0.4);
        }

        int cooldownFrames = Math.max(1, carriedWeapon.getCooldownFrames());
        double ratio = shootCooldown / (double) cooldownFrames;
        double threshold = Math.max(0.10, 0.45 - suppressionBonus * 0.22 - suppressionStage * 0.05);
        double engageWindow = suppressionStage >= 3 ? optimalRange * 1.15 : optimalRange * 1.35;
        return ratio >= threshold && distance <= engageWindow;
    }

    private boolean moveToCover(Homme target, double preferredRange) {
        double moveSpeed = MOVE_SPEED * getSuppressionMoveMultiplier();

        if (coverMemoryTimer <= 0 || !TacticalMovement.hasLineOfSight(x, y, coverX, coverY)) {
            double[] cover = TacticalMovement.findCoverPoint(x, y, target.x, target.y, ENTITY_RADIUS, preferredRange);
            if (cover == null) {
                clearCover();
                return false;
            }

            coverX = cover[0];
            coverY = cover[1];
            coverMemoryTimer = COVER_MEMORY_FRAMES;
        }

        double dx = coverX - x;
        double dy = coverY - y;
        double distSq = dx * dx + dy * dy;
        if (distSq <= ARRIVAL_RADIUS * ARRIVAL_RADIUS) {
            applySmoothedVelocity(0, 0);
            return true;
        }

        double invDistance = 1.0 / Math.sqrt(distSq);
        double desiredVx = dx * invDistance * moveSpeed;
        double desiredVy = dy * invDistance * moveSpeed;
        double[] adjusted = TacticalMovement.adjustForObstacles(x, y, desiredVx, desiredVy, ENTITY_RADIUS);
        applySmoothedVelocity(adjusted[0], adjusted[1]);

        double toTargetX = target.x - x;
        double toTargetY = target.y - y;
        double toTargetLen = Math.sqrt(toTargetX * toTargetX + toTargetY * toTargetY);
        if (toTargetLen > 0.0001) {
            setFacingDirection(toTargetX / toTargetLen, toTargetY / toTargetLen);
        }

        return true;
    }

    private void applyCombatMovement(double dx, double dy, double distance, double retreatRange, double optimalRange, int suppressionStage) {
        double moveSpeed = MOVE_SPEED * getSuppressionMoveMultiplier();

        if (strafeSwitchTimer <= 0) {
            strafeSign = Math.random() < 0.5 ? -1 : 1;
            int baseSwitch = suppressionStage >= 2 ? 16 : 24;
            strafeSwitchTimer = baseSwitch + (int) (Math.random() * (suppressionStage >= 2 ? 16 : 28));
        } else {
            strafeSwitchTimer--;
        }

        if (distance <= 0.0001) {
            applySmoothedVelocity(0, 0);
            return;
        }

        double invDistance = 1.0 / distance;
        double dirX = dx * invDistance;
        double dirY = dy * invDistance;

        double forwardFactor = 0;
        if (suppressionStage >= 3) {
            forwardFactor = distance < retreatRange * 1.15 ? -1.15 : -0.65;
        } else if (suppressionStage == 2) {
            if (distance < retreatRange) {
                forwardFactor = -1.0;
            } else if (distance < optimalRange) {
                forwardFactor = -0.55;
            } else {
                forwardFactor = 0.10;
            }
        } else if (distance < retreatRange) {
            forwardFactor = -1.0;
        } else if (distance > optimalRange) {
            forwardFactor = carriedWeapon.isShotgun() ? 0.70 : 0.30;
        }

        double strafeFactor = carriedWeapon.isShotgun() ? 0.30 : 0.80;
        if (suppressionStage >= 2) {
            strafeFactor *= 0.60;
        } else if (suppressionStage == 1) {
            strafeFactor *= 1.10;
        }
        double perpX = -dirY * strafeSign;
        double perpY = dirX * strafeSign;

        double desiredVx = (dirX * forwardFactor + perpX * strafeFactor) * moveSpeed;
        double desiredVy = (dirY * forwardFactor + perpY * strafeFactor) * moveSpeed;
        double[] adjusted = TacticalMovement.adjustForObstacles(x, y, desiredVx, desiredVy, ENTITY_RADIUS);
        applySmoothedVelocity(adjusted[0], adjusted[1]);
    }

    private void applySuppressedFallbackMovement(double dx, double dy, double distance, double retreatRange) {
        double invDistance = 1.0 / Math.max(distance, EPSILON_DIST_SQ);
        double dirX = dx * invDistance;
        double dirY = dy * invDistance;
        double sideSign = Math.random() < 0.5 ? -1 : 1;
        double perpX = -dirY * sideSign;
        double perpY = dirX * sideSign;
        double panicFactor = distance < retreatRange ? -1.05 : -0.70;
        double jitterFactor = 0.18 + getSuppressionLevel() * 0.25;
        double moveSpeed = MOVE_SPEED * getSuppressionMoveMultiplier() * 0.85;

        double desiredVx = (dirX * panicFactor + perpX * jitterFactor) * moveSpeed;
        double desiredVy = (dirY * panicFactor + perpY * jitterFactor) * moveSpeed;
        double[] adjusted = TacticalMovement.adjustForObstacles(x, y, desiredVx, desiredVy, ENTITY_RADIUS);
        applySmoothedVelocity(adjusted[0], adjusted[1]);
    }

    private void clearCover() {
        coverMemoryTimer = 0;
    }

    private void setFacingDirection(double targetX, double targetY) {
        double targetLen = Math.sqrt(targetX * targetX + targetY * targetY);
        if (targetLen <= 0.0001) {
            return;
        }

        double nx = targetX / targetLen;
        double ny = targetY / targetLen;
        double currentAngle = Math.atan2(facingY, facingX);
        double targetAngle = Math.atan2(ny, nx);
        double delta = normalizeAngle(targetAngle - currentAngle);

        double maxTurn = Math.toRadians(8.5);
        if (Math.abs(delta) <= maxTurn) {
            facingX = nx;
            facingY = ny;
            return;
        }

        double nextAngle = currentAngle + Math.copySign(maxTurn, delta);
        facingX = Math.cos(nextAngle);
        facingY = Math.sin(nextAngle);
    }

    private static double normalizeAngle(double angle) {
        while (angle > Math.PI) {
            angle -= Math.PI * 2;
        }
        while (angle < -Math.PI) {
            angle += Math.PI * 2;
        }
        return angle;
    }

    // Applique une vélocité lissée pour éviter les mouvements saccadés tout en restant réactif
    private void applySmoothedVelocity(double desiredVx, double desiredVy) {
        vx = vx * (1.0 - VELOCITY_BLEND) + desiredVx * VELOCITY_BLEND;
        vy = vy * (1.0 - VELOCITY_BLEND) + desiredVy * VELOCITY_BLEND;
        if (Math.abs(vx) < 0.01) {
            vx = 0;
        }
        if (Math.abs(vy) < 0.01) {
            vy = 0;
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        double angle = Math.atan2(facingY, facingX) + Math.PI / 2; // Calcul de l'angle de rotation
        var old = g2d.getTransform(); // Sauvegarde de la transformation actuelle

        g2d.rotate(angle, x, y);
        g2d.drawImage(imgCorps, (int)x - 16, (int)y - 16, 32, 42, null);
        carriedWeapon.draw(g2d, x, y, timer, shotAnimTimer > 0);
        g2d.setTransform(old); // Restauration de la transformation originale pour ne pas affecter les autres dessins
        
    }
}