package object;

import java.awt.*;
import java.io.IOException;
import javax.imageio.ImageIO;

import main.GamePanel;
import object.ai.AiTuning;
import world.TileManager;

public class Projectile extends GameObject {

    private static final double ENEMY_SUPPRESSION_MULTIPLIER = 0.30;

    private static Image imgShot;
    private static Image imgBullet;
    private static Image imgShotgunPellet;

    public enum ProjectileType {
        DEFAULT,
        BULLET,
        SHOTGUN_PELLET
    }

    static {
        try {
            imgShot = ImageIO.read(Projectile.class.getResourceAsStream("/assets/effets/shot_effect.png"));
            imgBullet = ImageIO.read(Projectile.class.getResourceAsStream("/assets/effets/bullet.png"));
            // Utilise le shot_effect pour les pellets aussi (ou créer une image spécifique)
            imgShotgunPellet = ImageIO.read(Projectile.class.getResourceAsStream("/assets/effets/bullet.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double angle; // Angle de rotation du projectile
    private int vie = 120; // Durée de vie du projectile en frames
    private final Homme tireur; // Le tireur à ignorer lors des collisions
    private final ProjectileType type;

    public Projectile(double x, double y, double vx, double vy, Homme tireur, ProjectileType type) {
        super(x, y);
        this.vx = vx;
        this.vy = vy;
        this.tireur = tireur;
        this.type = type;
        this.angle = Math.atan2(vy, vx) + Math.PI / 2; // Calcul de l'angle de rotation pour orienter le projectile dans la direction du mouvement  
    }

    public void update() {
        double previousX = x;
        double previousY = y;
        x += vx;
        y += vy;

        if (!canMoveTo(x, y, 2)) {
            ObjectManager.list.remove(this);
            ObjectManager.list.add(new ImpactSpark(x, y));
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
                ObjectManager.list.remove(homme);
                homme.onDeath();
                ObjectManager.list.add(new ImpactSpark(x, y));
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

    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
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
