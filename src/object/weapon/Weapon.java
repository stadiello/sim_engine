package object.weapon;

import java.awt.Graphics2D;
import java.awt.Image;
import java.io.IOException;
import javax.imageio.ImageIO;

import main.Utils;
import object.Douille;
import object.Homme;
import object.ObjectManager;
import object.Projectile;

public final class Weapon {

    public enum FireSound {
        LASER,
        SMG,
        PISTOL,
        SHOTGUN,
        NONE
    }

    private static final Weapon BLASTER = new Weapon(
            "Blaster",
            "/assets/armes/blaster.png",
            Projectile.ProjectileType.DEFAULT,
            15,
            6.5,
            5,
            30,
            5,
            -23,
            12,
            true,
            false,
            FireSound.LASER);

    private static final Weapon CARABINE = new Weapon(
            "Carabine",
            "/assets/armes/carabine.png",
            Projectile.ProjectileType.BULLET,
            4,
            30, //6.5
            5,
            30,
            5,
            -23,
            12,
            true,
            true,
            FireSound.SMG);

    private static final Weapon GLOCK = new Weapon(
            "Glock",
            "/assets/armes/glock.png",
            Projectile.ProjectileType.BULLET,
            8,
            70,
            6,
            16,
            6,
            -15,
            10,
            false,
            true,
            FireSound.PISTOL);

    private static final Weapon SHOTGUN = new Weapon(
            "Shotgun",
            "/assets/armes/shotgun.png",
            Projectile.ProjectileType.SHOTGUN_PELLET,
            40,
            12,
            8,
            32,
            8,
            -25,
            18,
            false,
            true,
            FireSound.SHOTGUN);

    public static Weapon blaster() {
        return BLASTER;
    }

    public static Weapon carabine() {
        return CARABINE;
    }

    public static Weapon glock() {
        return GLOCK;
    }

    public static Weapon shotgun() {
        return SHOTGUN;
    }

    public static Weapon[] protagonistLoadout() {
        return new Weapon[]{blaster(), carabine(), glock(), shotgun()};
    }

    public static Weapon[] storyLoadout() {
        return new Weapon[]{glock()};
    }

    private final String name;
    private final Image sprite;
    private final Projectile.ProjectileType projectileType;
    private final int cooldownFrames;
    private final double projectileSpeed;
    private final int drawWidth;
    private final int drawHeight;
    private final int drawOffsetX;
    private final int drawOffsetY;
    private final int recoilAmplitude;
    private final boolean automatic;
    private final boolean ejectShell;
    private final FireSound fireSound;

    private Weapon(
            String name,
            String spritePath,
            Projectile.ProjectileType projectileType,
            int cooldownFrames,
            double projectileSpeed,
            int drawWidth,
            int drawHeight,
            int drawOffsetX,
            int drawOffsetY,
            int recoilAmplitude,
            boolean automatic,
            boolean ejectShell,
            FireSound fireSound) {
        this.name = name;
        this.sprite = loadImage(spritePath);
        this.projectileType = projectileType;
        this.cooldownFrames = cooldownFrames;
        this.projectileSpeed = projectileSpeed;
        this.drawWidth = drawWidth;
        this.drawHeight = drawHeight;
        this.drawOffsetX = drawOffsetX;
        this.drawOffsetY = drawOffsetY;
        this.recoilAmplitude = recoilAmplitude;
        this.automatic = automatic;
        this.ejectShell = ejectShell;
        this.fireSound = fireSound;
    }

    public String getName() {
        return name;
    }

    public Image getSprite() {
        return sprite;
    }

    public int getCooldownFrames() {
        return cooldownFrames;
    }

    public double getProjectileSpeed() {
        return projectileSpeed;
    }

    public boolean isShotgun() {
        return projectileType == Projectile.ProjectileType.SHOTGUN_PELLET;
    }

    public boolean isLongRange() {
        return projectileSpeed >= 25.0;
    }

    public double getAiOptimalRange() {
        if (isShotgun()) {
            return 120.0;
        }
        if (isLongRange()) {
            return 280.0;
        }
        if ("Glock".equals(name)) {
            return 200.0;
        }
        return 220.0;
    }

    public double getAiRetreatRange() {
        return getAiOptimalRange() * 0.60;
    }

    public double getAiEngageRange() {
        return getAiOptimalRange() * 1.35;
    }

    public boolean isAutomatic() {
        return automatic;
    }

    public void fire(Homme shooter, double facingX, double facingY) {
        // Pour le shotgun, créer plusieurs pellets en éventail
        if (projectileType == Projectile.ProjectileType.SHOTGUN_PELLET) {
            int pelletCount = 8; // Nombre de pellets
            double spreadAngle = Math.PI / 6; // pi/3 = 60 degrés d'angle de dispersion pi/4 = 45 degrés, pi/6 = 30 degrés
            double baseAngle = Math.atan2(facingY, facingX);
            
            for (int i = 0; i < pelletCount; i++) {
                double angle = baseAngle + (spreadAngle / (pelletCount - 1)) * i - spreadAngle / 2;
                double spreadX = Math.cos(angle);
                double spreadY = Math.sin(angle);
                
                ObjectManager.list.add(new Projectile(
                        shooter.x,
                        shooter.y,
                        spreadX * projectileSpeed,
                        spreadY * projectileSpeed,
                        shooter,
                        projectileType));
            }
        } else {
            // Comportement normal pour les autres armes
            ObjectManager.list.add(new Projectile(
                    shooter.x,
                    shooter.y,
                    facingX * projectileSpeed,
                    facingY * projectileSpeed,
                    shooter,
                    projectileType));
        }

        if (ejectShell) {
            spawnDouille(shooter, facingX, facingY);
        }

        playSound();
    }

    public void draw(Graphics2D g2d, double x, double y, int timer, boolean shot) {
        if (sprite == null) {
            return;
        }

        int offsetArme = shot ? (int) (Math.sin(timer * 0.15) * recoilAmplitude) : 0;
        g2d.drawImage(sprite, (int) x + drawOffsetX, (int) y + drawOffsetY + offsetArme, drawWidth, drawHeight, null);
    }

    private void playSound() {
        switch (fireSound) {
            case LASER -> Utils.playLaserSound();
            case SMG -> Utils.playSmgSound();
            case PISTOL -> Utils.playPistolSound();
            case SHOTGUN -> Utils.playShotgunSound();
            case NONE -> {
            }
        }
    }

    private void spawnDouille(Homme shooter, double facingX, double facingY) {
        double sideX = -facingY;
        double sideY = facingX;

        double shellX = shooter.x + sideX * 10 - facingX * 4;
        double shellY = shooter.y + sideY * 10 - facingY * 4;

        double shellSpeed = 2.0 + Math.random() * 1.3;
        double shellVx = sideX * shellSpeed - facingX * 0.7 + (Math.random() - 0.5) * 0.8;
        double shellVy = sideY * shellSpeed - facingY * 0.7 + (Math.random() - 0.5) * 0.8;
        double shellAngle = Math.atan2(shellVy, shellVx);
        double shellRotationSpeed = (Math.random() - 0.5) * 0.35;

        ObjectManager.list.add(new Douille(shellX, shellY, shellVx, shellVy, shellAngle, shellRotationSpeed));
    }

    private static Image loadImage(String assetPath) {
        try {
            var stream = Weapon.class.getResourceAsStream(assetPath);
            if (stream == null) {
                System.err.println("Missing weapon asset: " + assetPath);
                return null;
            }
            return ImageIO.read(stream);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}