package network;

import gameController.GameKeyController;
import object.*;
import object.Ennemi.EnemyArchetype;
import object.Projectile.ProjectileType;
import object.weapon.Weapon;

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
    private static final BufferedImage BLASTER_IMAGE = load("/assets/armes/blaster.png");
    private static final BufferedImage CARABINE_IMAGE = load("/assets/armes/carabine.png");
    private static final BufferedImage GLOCK_IMAGE = load("/assets/armes/glock.png");
    private static final BufferedImage SHOTGUN_IMAGE = load("/assets/armes/shotgun.png");
    private static final BufferedImage GRENADE_IMAGE = load("/assets/armes/grenade.png");
    private static final BufferedImage ROCKET_LAUNCHER_IMAGE = load("/assets/armes/rocket_launcher.png");
    private static final BufferedImage MINIGUN_IMAGE = load("/assets/armes/minigun.png");
    private static final BufferedImage SHELL_IMAGE = load("/assets/effets/douille.png");

    private final byte type;
    private byte variant;
    private double facingX;
    private double facingY;
    private boolean localPlayer;
    private String detail;
    private int amount;
    private double[] visualState = new double[0];
    private final GameObject baseRenderer;

    public NetworkReplica(WorldSnapshot.Entity entity) {
        super(entity.x, entity.y);
        type = entity.type;
        apply(entity);
        baseRenderer = createBaseRenderer();
        syncBaseRenderer();
    }

    public byte getNetworkType() {
        return type;
    }

    public void apply(WorldSnapshot.Entity entity) {
        x = entity.x;
        y = entity.y;
        variant = entity.variant;
        vx = entity.vx;
        vy = entity.vy;
        facingX = entity.facingX;
        facingY = entity.facingY;
        localPlayer = entity.localPlayer;
        detail = entity.detail == null ? "" : entity.detail;
        amount = entity.amount;
        visualState = entity.visualState == null ? new double[0] : entity.visualState;
        syncBaseRenderer();
    }

    @Override
    public void update() {
        // Les positions proviennent exclusivement des instantanés de l'hôte.
    }

    @Override
    public void draw(Graphics g) {
        if (baseRenderer != null) {
            syncBaseRenderer();
            baseRenderer.draw(g);
            if (type == DEATH_MARKER && variant == 1 && amount == 1) {
                drawCenteredLabel((Graphics2D) g, "E - REANIMER", 35, new Color(125, 245, 170));
            }
            return;
        }
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

    private GameObject createBaseRenderer() {
        return switch (type) {
            case PLAYER -> {
                Protagonist player = new Protagonist(x, y, new GameKeyController(), localPlayer);
                player.setControlsEnabled(false);
                yield player;
            }
            case SOLDIER -> new Soldat(x, y, Weapon.fromName(detail));
            case ENEMY -> {
                EnemyArchetype[] archetypes = EnemyArchetype.values();
                int index = Math.max(0, Math.min(archetypes.length - 1, variant - 1));
                yield new Ennemi(x, y, archetypes[index], Weapon.fromName(detail));
            }
            case ALIEN -> new Alien(x, y);
            case CIVILIAN -> new Homme(x, y);
            case PROJECTILE -> {
                ProjectileType[] projectileTypes = ProjectileType.values();
                int index = Math.max(0, Math.min(projectileTypes.length - 1, variant - 1));
                yield new Projectile(x, y, vx, vy, null, projectileTypes[index]);
            }
            case AMMO_DEPOT -> new AmmoDepot(x, y);
            case DEATH_MARKER -> new DeathMarker(createMarkerVictim());
            case TURRET -> new DesertTurret(x, y);
            case PICKUP -> switch (variant) {
                case 1 -> new DroppedAmmo(x, y, Weapon.fromName(detail), amount);
                case 2 -> new DroppedWeapon(x, y, Weapon.fromName(detail));
                case 3 -> new DroppedArmor(x, y);
                default -> null;
            };
            case EFFECT -> switch (variant) {
                case 1 -> new ImpactSpark(x, y);
                case 4 -> TeslaArc.createNetworkReplica(x, y);
                default -> null;
            };
            default -> null;
        };
    }

    private Homme createMarkerVictim() {
        return switch (variant) {
            case 1 -> new Soldat(x, y, Weapon.fromName(detail));
            case 2 -> new Ennemi(x, y);
            case 3 -> new Alien(x, y);
            default -> new Homme(x, y);
        };
    }

    private void syncBaseRenderer() {
        if (baseRenderer == null) return;
        baseRenderer.x = x;
        baseRenderer.y = y;
        baseRenderer.vx = vx;
        baseRenderer.vy = vy;
        if (baseRenderer instanceof NetworkVisualState visual) {
            visual.applyNetworkVisualState(visualState);
        }
        if (baseRenderer instanceof Protagonist player) {
            player.applyNetworkPose(facingX, facingY, detail);
        } else if (baseRenderer instanceof Soldat soldier) {
            soldier.applyNetworkPose(facingX, facingY);
        } else if (baseRenderer instanceof Ennemi enemy) {
            enemy.applyNetworkPose(facingX, facingY);
        } else if (baseRenderer instanceof DesertTurret turret) {
            turret.applyNetworkPose(facingX, facingY);
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
        drawCarriedWeapon(g2d);
        g2d.setTransform(old);
    }

    private void drawCarriedWeapon(Graphics2D g2d) {
        if (detail.isBlank()) return;
        if ("Canon Tesla".equalsIgnoreCase(detail) || detail.toLowerCase().contains("tesla")) {
            g2d.setColor(new Color(92, 220, 255));
            g2d.fillRoundRect((int) x - 5, (int) y - 20, 10, 31, 5, 5);
            g2d.setColor(new Color(220, 250, 255));
            g2d.drawLine((int) x, (int) y - 30, (int) x, (int) y - 16);
            return;
        }
        BufferedImage weapon = switch (detail) {
            case "Blaster" -> BLASTER_IMAGE;
            case "Carabine" -> CARABINE_IMAGE;
            case "Glock" -> GLOCK_IMAGE;
            case "Shotgun" -> SHOTGUN_IMAGE;
            case "Grenade" -> GRENADE_IMAGE;
            case "Lance-roquettes" -> ROCKET_LAUNCHER_IMAGE;
            case "Minigun" -> MINIGUN_IMAGE;
            default -> null;
        };
        if (weapon != null) {
            int height = "Grenade".equals(detail) ? 13 : 28;
            int width = "Minigun".equals(detail) ? 11 : 8;
            g2d.drawImage(weapon, (int) x - width / 2, (int) y - 25, width, height, null);
        }
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
        drawCenteredLabel(g2d, detail, 25, new Color(150, 220, 255));
    }

    private void drawDeathMarker(Graphics2D g2d) {
        Color color = switch (variant) {
            case 1 -> new Color(90, 190, 245, 180);
            case 2 -> new Color(245, 92, 74, 180);
            case 3 -> new Color(185, 120, 255, 180);
            default -> new Color(180, 185, 190, 170);
        };
        AffineTransform old = g2d.getTransform();
        double rotation = ((Double.doubleToLongBits(x * 31.0 + y * 17.0) & 1023L) / 1023.0) * Math.PI * 2.0;
        g2d.rotate(rotation, x, y);
        g2d.setColor(new Color(10, 12, 15, 95));
        g2d.fillOval((int) x - 25, (int) y - 13, 50, 26);
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (variant == 3) {
            Polygon alien = new Polygon(
                    new int[]{(int) x - 23, (int) x - 8, (int) x + 10, (int) x + 23, (int) x + 8, (int) x - 10},
                    new int[]{(int) y, (int) y - 9, (int) y - 7, (int) y + 1, (int) y + 9, (int) y + 7}, 6);
            g2d.fillPolygon(alien);
        } else {
            g2d.fillOval((int) x - 22, (int) y - 8, 13, 13);
            g2d.fillRoundRect((int) x - 10, (int) y - 8, 25, 16, 9, 9);
            g2d.drawLine((int) x + 10, (int) y - 3, (int) x + 23, (int) y - 9);
            g2d.drawLine((int) x + 10, (int) y + 3, (int) x + 23, (int) y + 9);
        }
        g2d.setTransform(old);
        if (variant == 1 && amount == 1) drawCenteredLabel(g2d, "E - REANIMER", 35, new Color(125, 245, 170));
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
        Color halo = variant == 1 ? new Color(88, 148, 255, 95)
                : variant == 3 ? new Color(90, 170, 255, 90) : new Color(255, 220, 75, 100);
        g2d.setColor(halo);
        g2d.fillOval((int) x - 14, (int) y - 14, 28, 28);
        g2d.setColor(new Color(225, 240, 250));
        if (variant == 3) {
            g2d.setColor(new Color(68, 118, 175));
            g2d.fillRoundRect((int) x - 7, (int) y - 9, 14, 18, 4, 4);
            g2d.setColor(new Color(175, 230, 255));
            g2d.drawRoundRect((int) x - 7, (int) y - 9, 14, 18, 4, 4);
        } else if (variant == 2) {
            AffineTransform old = g2d.getTransform();
            g2d.rotate(-Math.PI / 2, x, y);
            drawPickupWeapon(g2d);
            g2d.setTransform(old);
        } else {
            g2d.setColor(new Color(40, 52, 66));
            g2d.fillRoundRect((int) x - 10, (int) y - 7, 20, 14, 4, 4);
            g2d.setColor(new Color(212, 228, 245));
            g2d.drawRoundRect((int) x - 10, (int) y - 7, 20, 14, 4, 4);
            for (int offset = -4; offset <= 4; offset += 4) {
                g2d.drawLine((int) x + offset, (int) y - 4, (int) x + offset, (int) y + 4);
            }
        }
        String label = variant == 1 ? "+" + amount + " " + detail : detail;
        drawCenteredLabel(g2d, label, 22, Color.WHITE);
    }

    private void drawPickupWeapon(Graphics2D g2d) {
        BufferedImage weapon = switch (detail) {
            case "Blaster" -> BLASTER_IMAGE;
            case "Carabine" -> CARABINE_IMAGE;
            case "Glock" -> GLOCK_IMAGE;
            case "Shotgun" -> SHOTGUN_IMAGE;
            case "Grenade" -> GRENADE_IMAGE;
            case "Lance-roquettes" -> ROCKET_LAUNCHER_IMAGE;
            case "Minigun" -> MINIGUN_IMAGE;
            default -> null;
        };
        if (weapon != null) g2d.drawImage(weapon, (int) x - 4, (int) y - 10, 8, 20, null);
        else {
            g2d.setColor(new Color(92, 220, 255));
            g2d.fillRoundRect((int) x - 4, (int) y - 11, 8, 22, 4, 4);
        }
    }

    private void drawCenteredLabel(Graphics2D g2d, String label, int yOffset, Color color) {
        if (label == null || label.isBlank()) return;
        g2d.setColor(color);
        g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 10f));
        FontMetrics metrics = g2d.getFontMetrics();
        g2d.drawString(label, (int) x - metrics.stringWidth(label) / 2, (int) y + yOffset);
    }

    private void drawEffect(Graphics2D g2d) {
        switch (variant) {
            case 1 -> {
                g2d.setColor(new Color(255, 185, 95, 210));
                g2d.setStroke(new BasicStroke(2f));
                for (int i = 0; i < 6; i++) {
                    double angle = i * Math.PI / 3.0;
                    g2d.drawLine((int) x, (int) y,
                            (int) Math.round(x + Math.cos(angle) * 13),
                            (int) Math.round(y + Math.sin(angle) * 13));
                }
            }
            case 2 -> {
                g2d.setColor(new Color(255, 150, 75, 105));
                g2d.fillOval((int) x - 31, (int) y - 31, 62, 62);
                g2d.setColor(new Color(90, 90, 96, 135));
                g2d.fillOval((int) x - 22, (int) y - 18, 44, 44);
            }
            case 3 -> {
                g2d.setColor(new Color(255, 220, 140, 175));
                g2d.setStroke(new BasicStroke(3f));
                g2d.drawOval((int) x - 35, (int) y - 35, 70, 70);
            }
            case 4 -> {
                g2d.setColor(new Color(100, 225, 255, 210));
                g2d.setStroke(new BasicStroke(3f));
                g2d.drawLine((int) x - 18, (int) y + 8, (int) x - 5, (int) y - 9);
                g2d.drawLine((int) x - 5, (int) y - 9, (int) x + 7, (int) y + 7);
                g2d.drawLine((int) x + 7, (int) y + 7, (int) x + 22, (int) y - 12);
            }
            case 5 -> {
                g2d.setColor(new Color(80, 240, 145, 95));
                g2d.setStroke(new BasicStroke(16f));
                g2d.drawLine((int) x, (int) y - 120, (int) x, (int) y);
                g2d.setColor(new Color(135, 255, 190, 210));
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawOval((int) x - 22, (int) y - 22, 44, 44);
            }
            case 6 -> {
                if (SHELL_IMAGE != null) g2d.drawImage(SHELL_IMAGE, (int) x - 4, (int) y - 2, 8, 4, null);
            }
            default -> {
                g2d.setColor(new Color(255, 185, 95, 170));
                g2d.fillOval((int) x - 7, (int) y - 7, 14, 14);
            }
        }
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
