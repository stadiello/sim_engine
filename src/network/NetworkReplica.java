package network;

import object.GameObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;

public final class NetworkReplica extends GameObject {
    public static final byte PLAYER = 1;
    public static final byte SOLDIER = 2;
    public static final byte ENEMY = 3;
    public static final byte ALIEN = 4;
    public static final byte CIVILIAN = 5;
    public static final byte PROJECTILE = 6;
    public static final byte AMMO_DEPOT = 7;
    public static final byte DEATH_MARKER = 8;
    public static final byte TURRET = 9;
    public static final byte PICKUP = 10;
    public static final byte EFFECT = 11;

    private static final BufferedImage SOLDIER_IMAGE = load("/assets/soldats/corps.png");
    private static final BufferedImage ENEMY_IMAGE = load("/assets/badGuys/ennemis.png");
    private static final BufferedImage ROCKET_ENEMY_IMAGE = load("/assets/badGuys/ennemi_roquette.png");
    private static final BufferedImage ALIEN_IMAGE = load("/assets/aliens/alien.png");
    private static final BufferedImage CIVILIAN_IMAGE = load("/assets/civils/corps.png");
    private static final BufferedImage ROCKET_IMAGE = load("/assets/effets/rocket.png");

    private final byte type;
    private final byte variant;
    private final double facingX;
    private final double facingY;
    private final boolean localPlayer;

    public NetworkReplica(WorldSnapshot.Entity entity) {
        super(entity.x, entity.y);
        type = entity.type;
        variant = entity.variant;
        vx = entity.vx;
        vy = entity.vy;
        facingX = entity.facingX;
        facingY = entity.facingY;
        localPlayer = entity.localPlayer;
    }

    @Override
    public void update() {
        // Les positions proviennent exclusivement des instantanés de l'hôte.
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        switch (type) {
            case PLAYER -> drawCharacter(g2d, SOLDIER_IMAGE, localPlayer ? new Color(105, 255, 145) : new Color(90, 180, 255));
            case SOLDIER -> drawCharacter(g2d, SOLDIER_IMAGE, new Color(75, 160, 235));
            case ENEMY -> drawCharacter(g2d, variant == 2 ? ROCKET_ENEMY_IMAGE : ENEMY_IMAGE, new Color(235, 82, 68));
            case ALIEN -> drawCharacter(g2d, ALIEN_IMAGE, new Color(180, 115, 255));
            case CIVILIAN -> drawCharacter(g2d, CIVILIAN_IMAGE, new Color(190, 195, 205));
            case PROJECTILE -> drawProjectile(g2d);
            case AMMO_DEPOT -> drawAmmoDepot(g2d);
            case DEATH_MARKER -> drawDeathMarker(g2d);
            case TURRET -> drawTurret(g2d);
            case PICKUP -> drawPickup(g2d);
            case EFFECT -> drawEffect(g2d);
            default -> {
            }
        }
    }

    private void drawCharacter(Graphics2D g2d, BufferedImage image, Color ring) {
        double dx = Math.abs(facingX) + Math.abs(facingY) > 0.001 ? facingX : vx;
        double dy = Math.abs(facingX) + Math.abs(facingY) > 0.001 ? facingY : vy;
        double angle = Math.atan2(dy, dx) + Math.PI / 2;
        AffineTransform old = g2d.getTransform();
        g2d.setColor(new Color(ring.getRed(), ring.getGreen(), ring.getBlue(), 190));
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.drawOval((int) x - 20, (int) y - 20, 40, 40);
        g2d.rotate(angle, x, y);
        if (image != null) g2d.drawImage(image, (int) x - 16, (int) y - 16, 32, type == PLAYER || type == SOLDIER ? 42 : 32, null);
        else {
            g2d.setColor(ring);
            g2d.fillOval((int) x - 14, (int) y - 14, 28, 28);
        }
        g2d.setTransform(old);
    }

    private void drawProjectile(Graphics2D g2d) {
        if (variant == 5 && ROCKET_IMAGE != null) {
            AffineTransform old = g2d.getTransform();
            g2d.rotate(Math.atan2(vy, vx) + Math.PI / 2, x, y);
            g2d.drawImage(ROCKET_IMAGE, (int) x - 6, (int) y - 14, 12, 28, null);
            g2d.setTransform(old);
            return;
        }
        if (variant == 4) {
            g2d.setColor(new Color(78, 96, 52));
            g2d.fillOval((int) x - 7, (int) y - 7, 14, 14);
            return;
        }
        Color color = variant == 6 ? new Color(125, 225, 255) : new Color(255, 224, 150);
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(variant == 6 ? 4f : 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine((int) x, (int) y, (int) Math.round(x - vx * 1.8), (int) Math.round(y - vy * 1.8));
    }

    private void drawAmmoDepot(Graphics2D g2d) {
        g2d.setColor(new Color(54, 66, 74));
        g2d.fillRoundRect((int) x - 18, (int) y - 14, 36, 28, 6, 6);
        g2d.setColor(new Color(118, 190, 230));
        g2d.drawRoundRect((int) x - 18, (int) y - 14, 36, 28, 6, 6);
        g2d.setColor(new Color(235, 198, 84));
        for (int i = -1; i <= 1; i++) g2d.fillRoundRect((int) x + i * 8 - 2, (int) y - 3, 4, 11, 3, 3);
    }

    private void drawDeathMarker(Graphics2D g2d) {
        Color color = switch (variant) {
            case 1 -> new Color(90, 190, 245, 180);
            case 2 -> new Color(245, 92, 74, 180);
            case 3 -> new Color(185, 120, 255, 180);
            default -> new Color(180, 185, 190, 170);
        };
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine((int) x - 20, (int) y, (int) x + 20, (int) y);
        g2d.fillOval((int) x - 25, (int) y - 6, 12, 12);
    }

    private void drawTurret(Graphics2D g2d) {
        g2d.setColor(new Color(110, 108, 91));
        g2d.fillOval((int) x - 20, (int) y - 15, 40, 30);
        AffineTransform old = g2d.getTransform();
        g2d.rotate(Math.atan2(facingY, facingX), x, y);
        g2d.setColor(new Color(75, 84, 78));
        g2d.fillRoundRect((int) x - 7, (int) y - 8, 34, 16, 7, 7);
        g2d.fillRect((int) x + 15, (int) y - 3, 42, 6);
        g2d.setTransform(old);
    }

    private void drawPickup(Graphics2D g2d) {
        g2d.setColor(new Color(255, 220, 75, 100));
        g2d.fillOval((int) x - 14, (int) y - 14, 28, 28);
        g2d.setColor(new Color(235, 240, 250));
        g2d.fillRoundRect((int) x - 8, (int) y - 5, 16, 10, 4, 4);
    }

    private void drawEffect(Graphics2D g2d) {
        g2d.setColor(new Color(255, 185, 95, 170));
        g2d.fillOval((int) x - 7, (int) y - 7, 14, 14);
    }

    private static BufferedImage load(String path) {
        try {
            var stream = NetworkReplica.class.getResourceAsStream(path);
            return stream == null ? null : ImageIO.read(stream);
        } catch (IOException ignored) {
            return null;
        }
    }
}
