package object;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Color;
import java.io.IOException;
import javax.imageio.ImageIO;

import object.ai.BotBrain;
import object.weapon.Weapon;
import main.GameMode;

public class Ennemi extends Homme {

    public enum EnemyArchetype {
        STANDARD,
        FLANQUEUR,
        ASSAUT,
        LOURD
    }

    private static final double MAX_TURN_PER_FRAME_RAD = Math.toRadians(10.0);

    private static Image imgEnnemi;
    private static Image imgEnnemiLourd;

    static {
        try {
            var defaultStream = Ennemi.class.getResourceAsStream("/assets/badGuys/ennemis.png");
            if (defaultStream != null) {
                imgEnnemi = ImageIO.read(defaultStream);
            }
            var heavyStream = Ennemi.class.getResourceAsStream("/assets/badGuys/ennemi_lourd.png");
            if (heavyStream != null) {
                imgEnnemiLourd = ImageIO.read(heavyStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final BotBrain brain;
    private final Weapon carriedWeapon;
    private final EnemyArchetype archetype;
    private double facingX = 0;
    private double facingY = -1;
    private int shotAnimTimer = 0;
    private static final int FRONT_ARMOR_MAX = 8;
    private int frontArmor = FRONT_ARMOR_MAX;
    private int frontArmorRegenTimer = 0;

    public Ennemi(double x, double y) {
        this(x, y, EnemyArchetype.STANDARD);
    }

    public Ennemi(double x, double y, EnemyArchetype archetype) {
        super(x, y);
        this.brain = new BotBrain();
        this.archetype = archetype;
        this.carriedWeapon = pickWeaponForArchetype(archetype);
        this.vx = 0;
        this.vy = 0;
    }

    private static Weapon pickWeaponForArchetype(EnemyArchetype archetype) {
        double r = Math.random();
        return switch (archetype) {
            case FLANQUEUR -> r < 0.70 ? Weapon.glock() : Weapon.carabine();
            case ASSAUT -> r < 0.72 ? Weapon.shotgun() : Weapon.carabine();
            case LOURD -> Weapon.minigun();
            case STANDARD -> {
                if (r < 0.50) {
                    yield Weapon.glock();
                }
                if (r < 0.80) {
                    yield Weapon.carabine();
                }
                yield Weapon.shotgun();
            }
        };
    }

    @Override
    public void onDeath() {
        if (GameMode.current == GameMode.STORY) {
            ObjectManager.list.add(new DroppedWeapon(x, y, carriedWeapon));
        }

        double armorDropChance = GameMode.current == GameMode.ARCADE
                ? 0.28
                : GameMode.current == GameMode.PROTECTION ? 0.22 : 0.16;
        if (Math.random() < armorDropChance) {
            ObjectManager.list.add(new DroppedArmor(x, y));
        }
    }

    @Override
    public void update() {
        brain.update(this);

        if (vx * vx + vy * vy > 0.0001) {
            setFacingDirection(vx, vy);
        }

        if (shotAnimTimer > 0) {
            shotAnimTimer--;
        }

        if (isHeavy()) {
            if (frontArmor < FRONT_ARMOR_MAX) {
                if (frontArmorRegenTimer > 0) {
                    frontArmorRegenTimer--;
                } else {
                    frontArmor++;
                    frontArmorRegenTimer = 140;
                }
            } else {
                frontArmorRegenTimer = 0;
            }
        }

        moveWithTileCollision(14);
    }

    public void setFacingDirection(double dx, double dy) {
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len <= 0.0001) {
            return;
        }

        double targetX = dx / len;
        double targetY = dy / len;
        double currentAngle = Math.atan2(facingY, facingX);
        double targetAngle = Math.atan2(targetY, targetX);
        double delta = normalizeAngle(targetAngle - currentAngle);

        if (Math.abs(delta) <= MAX_TURN_PER_FRAME_RAD) {
            facingX = targetX;
            facingY = targetY;
            return;
        }

        double nextAngle = currentAngle + Math.copySign(MAX_TURN_PER_FRAME_RAD, delta);
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

    public double getFacingX() {
        return facingX;
    }

    public double getFacingY() {
        return facingY;
    }

    public Weapon getCarriedWeapon() {
        return carriedWeapon;
    }

    public EnemyArchetype getArchetype() {
        return archetype;
    }

    public boolean isFlanker() {
        return archetype == EnemyArchetype.FLANQUEUR;
    }

    public boolean isBreacher() {
        return archetype == EnemyArchetype.ASSAUT;
    }

    public boolean isHeavy() {
        return archetype == EnemyArchetype.LOURD;
    }

    public boolean absorbFrontHit(double projectileVx, double projectileVy) {
        if (!isHeavy() || frontArmor <= 0) {
            return false;
        }

        double speedSq = projectileVx * projectileVx + projectileVy * projectileVy;
        if (speedSq <= 0.0001) {
            return false;
        }

        double invSpeed = 1.0 / Math.sqrt(speedSq);
        double dirX = projectileVx * invSpeed;
        double dirY = projectileVy * invSpeed;
        double frontalDot = dirX * facingX + dirY * facingY;

        // Tir arrivant de face: direction du projectile opposee au regard de l'ennemi.
        if (frontalDot > -0.42) {
            return false;
        }

        frontArmor--;
        frontArmorRegenTimer = 220;
        return true;
    }

    public void onShot() {
        shotAnimTimer = 4;
    }

    @Override
    public void draw(Graphics g) {
        if (imgEnnemi == null) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        double angle = Math.atan2(facingY, facingX) + Math.PI / 2;
        var old = g2d.getTransform();

        g2d.rotate(angle, x, y);
        Image bodyImage = isHeavy() && imgEnnemiLourd != null ? imgEnnemiLourd : imgEnnemi;
        g2d.drawImage(bodyImage, (int) x - 16, (int) y - 16, 32, 42, null);

        if (archetype != EnemyArchetype.STANDARD) {
            Color tagColor = switch (archetype) {
                case FLANQUEUR -> new Color(226, 196, 110);
                case ASSAUT -> new Color(206, 92, 92);
                case LOURD -> new Color(120, 180, 205);
                default -> new Color(220, 220, 220);
            };
            g2d.setColor(tagColor);
            g2d.fillRect((int) x - 4, (int) y - 19, 8, 3);
        }

        if (isHeavy()) {
            g2d.setColor(new Color(56, 70, 84));
            g2d.fillRoundRect((int) x - 14, (int) y - 15, 28, 24, 6, 6);
            g2d.setColor(new Color(96, 117, 140));
            g2d.fillRect((int) x - 8, (int) y - 18, 16, 6);
            g2d.setColor(new Color(42, 52, 64));
            g2d.fillRect((int) x - 4, (int) y + 10, 8, 4);

            int armorBar = (int) Math.round(12.0 * frontArmor / Math.max(1, FRONT_ARMOR_MAX));
            g2d.setColor(new Color(35, 35, 35, 170));
            g2d.fillRect((int) x - 6, (int) y - 24, 12, 2);
            g2d.setColor(new Color(134, 198, 224));
            g2d.fillRect((int) x - 6, (int) y - 24, armorBar, 2);
        }

        carriedWeapon.draw(g2d, x, y, shotAnimTimer, shotAnimTimer > 0);
        g2d.setTransform(old);
    }
}
