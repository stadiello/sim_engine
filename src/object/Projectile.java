package object;

import java.awt.*;
import java.io.IOException;
import javax.imageio.ImageIO;

import main.GamePanel;
import main.GameMode;
import main.Utils;
import object.ai.AiTuning;
import world.TileManager;

public class Projectile extends GameObject {

    private static final double ENEMY_SUPPRESSION_MULTIPLIER = 0.30;
    private static final double GRENADE_BOUNCE_FACTOR = 0.58;
    private static final double GRENADE_FRICTION_FACTOR = 0.97;
    private static final double GRENADE_STOP_SPEED_SQ = 0.12;
    private static final double GRENADE_RADIUS = 7.0;
    private static final double GRENADE_BLAST_RADIUS = 72.0;
    private static final int GRENADE_FUSE_FRAMES = 180;
    private static final int GRENADE_SPARK_COUNT = 3;
    private static final double ROCKET_RADIUS = 8.0;
    private static final double ROCKET_BLAST_RADIUS = 110.0;
    private static final int ROCKET_SPARK_COUNT = 10;
    private static final Color METAL_IMPACT_CORE = new Color(255, 236, 180);
    private static final Color METAL_IMPACT_EMBER = new Color(255, 140, 84);
    private static final Color FLESH_IMPACT_CORE = new Color(255, 196, 118);
    private static final Color FLESH_IMPACT_EMBER = new Color(255, 86, 68);
    private static final Color ROCKET_FIRE_CORE = new Color(255, 224, 160);
    private static final Color ROCKET_FIRE_EMBER = new Color(255, 110, 72);

    private static Image imgShot;
    private static Image imgBullet;
    private static Image imgShotgunPellet;
    private static Image imgRocket;

    public enum ProjectileType {
        DEFAULT,
        BULLET,
        SHOTGUN_PELLET,
        GRENADE,
        ROCKET,
        TESLA
    }

    static {
        try {
            imgShot = ImageIO.read(Projectile.class.getResourceAsStream("/assets/effets/shot_effect.png"));
            imgBullet = ImageIO.read(Projectile.class.getResourceAsStream("/assets/effets/bullet.png"));
            // Utilise le shot_effect pour les pellets aussi (ou créer une image spécifique)
            imgShotgunPellet = ImageIO.read(Projectile.class.getResourceAsStream("/assets/effets/bullet.png"));
            imgRocket = ImageIO.read(Projectile.class.getResourceAsStream("/assets/effets/rocket.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double angle; // Angle de rotation du projectile
    private int vie = 120; // Durée de vie du projectile en frames
    private final Homme tireur; // Le tireur à ignorer lors des collisions
    private final ProjectileType type;
    private int fuseFrames = GRENADE_FUSE_FRAMES;

    public Projectile(double x, double y, double vx, double vy, Homme tireur, ProjectileType type) {
        super(x, y);
        this.vx = vx;
        this.vy = vy;
        this.tireur = tireur;
        this.type = type;
        this.angle = Math.atan2(vy, vx) + Math.PI / 2; // Calcul de l'angle de rotation pour orienter le projectile dans la direction du mouvement  
    }

    public void update() {
        if (type == ProjectileType.GRENADE) {
            updateGrenade();
            return;
        }
        if (type == ProjectileType.ROCKET) {
            updateRocket();
            return;
        }

        double previousX = x;
        double previousY = y;
        x += vx;
        y += vy;

        if (!canMoveTo(x, y, 2)) {
            TileManager tileManager = ObjectManager.getTileManager();
            if (tileManager != null) {
                int tileDamage = switch (type) {
                    case BULLET -> 1;
                    case SHOTGUN_PELLET -> 1;
                    default -> 1;
                };
                tileManager.damageTileAtPixel(x, y, tileDamage);
            }
            ObjectManager.list.remove(this);
            ObjectManager.list.add(new ImpactSpark(x, y, vx, vy, 1.0, METAL_IMPACT_CORE, METAL_IMPACT_EMBER));
            if (GameMode.current == main.GameMode.ARCADE) {
                GamePanel.triggerScreenFlash(METAL_IMPACT_CORE, 0.025f, 2);
            }
            return;
        }

        // Vérification des collisions sans recopier toute la liste globale à chaque projectile.
        for (Homme homme : ObjectManager.getLivingHumans()) {
            if (homme == tireur) {
                continue;
            }

            boolean hostile = areHostile(tireur, homme);

            double distSq = distancePointToSegmentSquared(
                homme.x,
                homme.y,
                previousX,
                previousY,
                x,
                y
            );
            double hitRadiusSq = 18 * 18;
            double suppressionRadius = AiTuning.getSuppressionNearMissRadius();
            double suppressionRadiusSq = suppressionRadius * suppressionRadius;

            if (hostile && distSq < suppressionRadiusSq) {
                double dist = Math.sqrt(Math.max(0.0, distSq));
                double falloff = Math.max(0.0, 1.0 - (dist / suppressionRadius));
                double intensity = falloff * falloff * getSuppressionMultiplier();
                if (type == ProjectileType.SHOTGUN_PELLET) {
                    intensity *= 0.35;
                } else if (type == ProjectileType.BULLET) {
                    intensity *= 1.10;
                }
                if (intensity > 0.01) {
                    homme.onIncomingFire(tireur, intensity);
                }
            }

            if (distSq < hitRadiusSq) {
                if (homme instanceof Protagonist protagonist && protagonist.consumeArmorPlateOnHit()) {
                    ObjectManager.list.add(new ImpactSpark(x, y, vx, vy, 1.15, METAL_IMPACT_CORE, METAL_IMPACT_EMBER));
                    if (GameMode.current == main.GameMode.ARCADE) {
                        GamePanel.triggerScreenShake(4, 1.6);
                        GamePanel.triggerScreenFlash(METAL_IMPACT_CORE, 0.045f, 3);
                    }
                    ObjectManager.list.remove(this);
                    return;
                }

                if (homme instanceof Soldat soldat && soldat.consumeArmorPlateOnHit()) {
                    ObjectManager.list.add(new ImpactSpark(x, y, vx, vy, 1.10, METAL_IMPACT_CORE, METAL_IMPACT_EMBER));
                    if (GameMode.current == main.GameMode.ARCADE) {
                        GamePanel.triggerScreenShake(3, 1.4);
                        GamePanel.triggerScreenFlash(METAL_IMPACT_CORE, 0.04f, 2);
                    }
                    ObjectManager.list.remove(this);
                    return;
                }

                if (homme instanceof Ennemi ennemi && ennemi.absorbFrontHit(vx, vy)) {
                    ObjectManager.list.add(new ImpactSpark(x, y, vx, vy, 1.2, METAL_IMPACT_CORE, METAL_IMPACT_EMBER));
                    if (GameMode.current == main.GameMode.ARCADE) {
                        GamePanel.triggerScreenShake(4, 1.8);
                        GamePanel.triggerScreenFlash(METAL_IMPACT_CORE, 0.05f, 3);
                    }
                    ObjectManager.list.remove(this);
                    return;
                }

                if (homme instanceof Alien hitAlien && AiTuning.isAlienPackAggroEnabled()) {
                    ObjectManager.activateAlienHiveAggro(tireur, hitAlien);
                }

                ObjectManager.list.remove(homme);
                homme.onDeath();
                ObjectManager.list.add(new ImpactSpark(x, y, vx, vy, 1.5, FLESH_IMPACT_CORE, FLESH_IMPACT_EMBER));
                if (GameMode.current == main.GameMode.ARCADE) {
                    GamePanel.triggerScreenShake(5, 2.3);
                    GamePanel.triggerScreenFlash(FLESH_IMPACT_EMBER, 0.06f, 3);
                }
                ObjectManager.list.remove(this);
                GamePanel.score += 10;
                return;
            }
        }

        vie--;

        TileManager tileManager = ObjectManager.getTileManager();
        double maxX = tileManager != null ? tileManager.getWorldWidth() : 800;
        double maxY = tileManager != null ? tileManager.getWorldHeight() : 600;

        if (vie <= 0 || x < 0 || x > maxX || y < 0 || y > maxY) {
            ObjectManager.list.remove(this); // Supprime le projectile après sa durée de vie
        }
    }

    private void updateGrenade() {
        boolean bounced = false;

        double nextX = x + vx;
        if (canMoveTo(nextX, y, GRENADE_RADIUS)) {
            x = nextX;
        } else {
            vx = -vx * GRENADE_BOUNCE_FACTOR;
            bounced = true;
        }

        double nextY = y + vy;
        if (canMoveTo(x, nextY, GRENADE_RADIUS)) {
            y = nextY;
        } else {
            vy = -vy * GRENADE_BOUNCE_FACTOR;
            bounced = true;
        }

        double friction = bounced ? 0.92 : GRENADE_FRICTION_FACTOR;
        vx *= friction;
        vy *= friction;

        if (vx * vx + vy * vy < GRENADE_STOP_SPEED_SQ) {
            vx = 0;
            vy = 0;
        }

        angle += (vx + vy) * 0.03;
        fuseFrames--;

        TileManager tileManager = ObjectManager.getTileManager();
        double maxX = tileManager != null ? tileManager.getWorldWidth() : 800;
        double maxY = tileManager != null ? tileManager.getWorldHeight() : 600;

        if (fuseFrames <= 0 || x < 0 || x > maxX || y < 0 || y > maxY) {
            explode();
            return;
        }
    }

    private void updateRocket() {
        double previousX = x;
        double previousY = y;
        x += vx;
        y += vy;

        if (!canMoveTo(x, y, ROCKET_RADIUS)) {
            explodeRocket();
            return;
        }

        for (Homme homme : ObjectManager.getLivingHumans()) {
            if (homme == tireur) {
                continue;
            }

            if (!areHostile(tireur, homme)) {
                continue;
            }

            double distSq = distancePointToSegmentSquared(homme.x, homme.y, previousX, previousY, x, y);
            if (distSq < 20 * 20) {
                explodeRocket();
                return;
            }
        }

        vie--;
        TileManager tileManager = ObjectManager.getTileManager();
        double maxX = tileManager != null ? tileManager.getWorldWidth() : 800;
        double maxY = tileManager != null ? tileManager.getWorldHeight() : 600;
        if (vie <= 0 || x < 0 || x > maxX || y < 0 || y > maxY) {
            explodeRocket();
        }
    }

    private void explode() {
        Utils.playGrenadeSound();

        TileManager tileManager = ObjectManager.getTileManager();
        if (tileManager != null) {
            tileManager.damageTilesInRadius(x, y, GRENADE_BLAST_RADIUS, 2);
        }

        for (int i = 0; i < GRENADE_SPARK_COUNT; i++) {
            ObjectManager.list.add(new ImpactSpark(
                    x,
                    y,
                    Math.random() * 2.0 - 1.0,
                    Math.random() * 2.0 - 1.0,
                    1.8,
                    METAL_IMPACT_CORE,
                    METAL_IMPACT_EMBER));
        }

        ObjectManager.list.add(new Shockwave(x, y, tireur, GRENADE_BLAST_RADIUS));
        if (GameMode.current == main.GameMode.ARCADE) {
            GamePanel.triggerScreenShake(14, 7.0);
            GamePanel.triggerScreenFlash(new Color(255, 176, 110), 0.22f, 8);
        }

        ObjectManager.list.remove(this);
    }

    private void explodeRocket() {
        Utils.playGrenadeSound();

        TileManager tileManager = ObjectManager.getTileManager();
        if (tileManager != null) {
            tileManager.damageTilesInRadius(x, y, ROCKET_BLAST_RADIUS, 3);
        }

        for (int i = 0; i < ROCKET_SPARK_COUNT; i++) {
            ObjectManager.list.add(new ImpactSpark(
                    x,
                    y,
                    Math.random() * 2.0 - 1.0,
                    Math.random() * 2.0 - 1.0,
                    2.3,
                    ROCKET_FIRE_CORE,
                    ROCKET_FIRE_EMBER));
        }

                ObjectManager.list.add(new RocketBlastCloud(x, y, ROCKET_BLAST_RADIUS));
        ObjectManager.list.add(new Shockwave(x, y, tireur, ROCKET_BLAST_RADIUS));
        ObjectManager.list.add(new Shockwave(x, y, tireur, ROCKET_BLAST_RADIUS * 0.62));
        GamePanel.triggerScreenShake(18, 8.5);
        GamePanel.triggerScreenFlash(new Color(255, 152, 86), 0.28f, 10);
        ObjectManager.list.remove(this);
    }

    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        if (type == ProjectileType.GRENADE) {
            var old = g2d.getTransform();
            g2d.rotate(angle, x, y);
            g2d.setColor(new Color(78, 96, 52));
            g2d.fillOval((int) Math.round(x - GRENADE_RADIUS), (int) Math.round(y - GRENADE_RADIUS), 14, 14);
            g2d.setColor(new Color(22, 28, 18));
            g2d.drawOval((int) Math.round(x - GRENADE_RADIUS), (int) Math.round(y - GRENADE_RADIUS), 14, 14);
            g2d.setColor(new Color(180, 200, 120));
            g2d.drawLine((int) Math.round(x), (int) Math.round(y - 6), (int) Math.round(x + 3), (int) Math.round(y - 12));
            g2d.setTransform(old);
            return;
        }
        if (type == ProjectileType.ROCKET) {
            var old = g2d.getTransform();
            g2d.rotate(angle, x, y);
            if (imgRocket != null) {
                g2d.drawImage(imgRocket, (int) Math.round(x - 6), (int) Math.round(y - 14), 12, 28, null);
            } else {
                g2d.setColor(new Color(92, 102, 112));
                g2d.fillRoundRect((int) Math.round(x - 4), (int) Math.round(y - 12), 8, 20, 3, 3);
                g2d.setColor(new Color(210, 104, 72));
                g2d.fillOval((int) Math.round(x - 4), (int) Math.round(y - 14), 8, 8);
            }
            g2d.setColor(new Color(255, 198, 122, 210));
            g2d.fillOval((int) Math.round(x - 3), (int) Math.round(y + 8), 6, 6);
            g2d.setTransform(old);
            return;
        }

        boolean isBullet = type == ProjectileType.BULLET;
        boolean isPellet = type == ProjectileType.SHOTGUN_PELLET;
        
        Image projectileImage = isPellet ? imgShotgunPellet : (isBullet ? imgBullet : imgShot);
        int projectileWidth = isPellet ? 4 : (isBullet ? 2 : 8);
        int projectileHeight = isPellet ? 6 : (isBullet ? 12 : 30);
        int projectileX = isPellet ? (int)x + 3 : (isBullet ? (int)x + 8 : (int)x + 5);
        int projectileY = isPellet ? (int)y - 10 : (isBullet ? (int)y - 14 : (int)y - 23);

        var old = g2d.getTransform(); // Sauvegarde de la transformation actuelle
        g2d.rotate(angle, x, y);
        if (projectileImage != null) {
            g2d.drawImage(projectileImage, projectileX, projectileY, projectileWidth, projectileHeight, null);
        }
        g2d.setTransform(old); // Restauration de la transformation originale pour ne pas affecter les autres dessins
    }

    private boolean areHostile(Homme from, Homme to) {
        boolean fromHostileTeam = from instanceof Ennemi || from instanceof Alien;
        boolean toHostileTeam = to instanceof Ennemi || to instanceof Alien;
        return fromHostileTeam != toHostileTeam;
    }

    private double getSuppressionMultiplier() {
        return tireur instanceof Ennemi ? ENEMY_SUPPRESSION_MULTIPLIER : 1.0;
    }

    private static double distancePointToSegmentSquared(
            double px,
            double py,
            double x1,
            double y1,
            double x2,
            double y2
    ) {
        double segX = x2 - x1;
        double segY = y2 - y1;
        double segLenSq = segX * segX + segY * segY;

        if (segLenSq <= 0.0000001) {
            double dx = px - x1;
            double dy = py - y1;
            return dx * dx + dy * dy;
        }

        double t = ((px - x1) * segX + (py - y1) * segY) / segLenSq;
        t = Math.max(0.0, Math.min(1.0, t));

        double closestX = x1 + t * segX;
        double closestY = y1 + t * segY;
        double dx = px - closestX;
        double dy = py - closestY;
        return dx * dx + dy * dy;
    }
    
}
