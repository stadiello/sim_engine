package object.weapon;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.IOException;
import javax.imageio.ImageIO;

import main.GameMode;
import main.GamePanel;
import main.Utils;
import object.Douille;
import object.Homme;
import object.ObjectManager;
import object.Projectile;
import object.Protagonist;

public final class Weapon {

    private static final double MUZZLE_LEFT_BIAS = 10.0;

    public enum FireSound {
        LASER,
        SMG,
        PISTOL,
        SHOTGUN,
        GRENADE,
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
            FireSound.LASER,
            24,
            120,
            75);

    private static final Weapon CARABINE = new Weapon(
            "Carabine",
            "/assets/armes/carabine.png",
            Projectile.ProjectileType.BULLET,
            4,
            30,
            5,
            30,
            5,
            -23,
            12,
            true,
            true,
            FireSound.SMG,
            30,
            120,
            100);

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
            FireSound.PISTOL,
            17,
            51,
            80);

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
            FireSound.SHOTGUN,
            8,
            24,
            105);

    private static final Weapon GRENADE = new Weapon(
            "Grenade",
            "/assets/armes/grenade.png",
            Projectile.ProjectileType.GRENADE,
            45,
            11,
            10,
            10,
            4,
            -12,
            0,
            false,
            false,
            FireSound.NONE,
            1,
            2,
            90);

    private static final Weapon MINIGUN = new Weapon(
            "Minigun",
            "/assets/armes/minigun.png",
            Projectile.ProjectileType.BULLET,
            2,
            24,
            10,
            34,
            -26,
            12,
            14,
            true,
            true,
            FireSound.SMG,
            80,
            240,
            140);

    public static Weapon blaster() {
        return BLASTER.copy();
    }

    public static Weapon carabine() {
        return CARABINE.copy();
    }

    public static Weapon glock() {
        return GLOCK.copy();
    }

    public static Weapon shotgun() {
        return SHOTGUN.copy();
    }

    public static Weapon grenade() {
        return GRENADE.copy();
    }

    public static Weapon minigun() {
        return MINIGUN.copy();
    }

    public static Weapon[] protagonistLoadout() {
        return new Weapon[]{blaster(), carabine(), glock(), shotgun(), grenade()};
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
    private final int magazineCapacity;
    private final int maxReserveAmmo;
    private final int reloadFrames;
    private int ammoInMagazine;
    private int reserveAmmo;

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
            FireSound fireSound,
            int magazineCapacity,
            int maxReserveAmmo,
            int reloadFrames) {
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
        this.magazineCapacity = Math.max(1, magazineCapacity);
        this.maxReserveAmmo = Math.max(0, maxReserveAmmo);
        this.reloadFrames = Math.max(1, reloadFrames);
        this.ammoInMagazine = this.magazineCapacity;
        this.reserveAmmo = this.maxReserveAmmo;
    }

    private Weapon(Weapon other) {
        this.name = other.name;
        this.sprite = other.sprite;
        this.projectileType = other.projectileType;
        this.cooldownFrames = other.cooldownFrames;
        this.projectileSpeed = other.projectileSpeed;
        this.drawWidth = other.drawWidth;
        this.drawHeight = other.drawHeight;
        this.drawOffsetX = other.drawOffsetX;
        this.drawOffsetY = other.drawOffsetY;
        this.recoilAmplitude = other.recoilAmplitude;
        this.automatic = other.automatic;
        this.ejectShell = other.ejectShell;
        this.fireSound = other.fireSound;
        this.magazineCapacity = other.magazineCapacity;
        this.maxReserveAmmo = other.maxReserveAmmo;
        this.reloadFrames = other.reloadFrames;
        this.ammoInMagazine = other.ammoInMagazine;
        this.reserveAmmo = other.reserveAmmo;
    }

    public Weapon copy() {
        return new Weapon(this);
    }

    public Weapon withAmmo(int ammoInMagazine, int reserveAmmo) {
        Weapon copy = copy();
        copy.ammoInMagazine = Math.max(0, Math.min(copy.magazineCapacity, ammoInMagazine));
        copy.reserveAmmo = Math.max(0, Math.min(copy.maxReserveAmmo, reserveAmmo));
        return copy;
    }

    public String getName() {
        return name;
    }

    public Image getSprite() {
        return sprite;
    }

    public int getCooldownFrames() {
        if (GameMode.current != GameMode.ARCADE) {
            return cooldownFrames;
        }

        if (projectileType == Projectile.ProjectileType.GRENADE) {
            return Math.max(30, cooldownFrames - 8);
        }
        if (isShotgun()) {
            return Math.max(26, cooldownFrames - 8);
        }
        return Math.max(2, (int) Math.round(cooldownFrames * 0.82));
    }

    public double getProjectileSpeed() {
        return GameMode.current == GameMode.ARCADE && projectileType != Projectile.ProjectileType.GRENADE
                ? projectileSpeed * 1.06
                : projectileSpeed;
    }

    public boolean isShotgun() {
        return projectileType == Projectile.ProjectileType.SHOTGUN_PELLET;
    }

    public boolean isLongRange() {
        return projectileSpeed >= 25.0;
    }

    public boolean isMinigun() {
        return "Minigun".equals(name);
    }

    public boolean isAutomatic() {
        return automatic;
    }

    public boolean isSameModel(Weapon other) {
        return other != null && name.equals(other.name);
    }

    public int getMagazineCapacity() {
        return magazineCapacity;
    }

    public int getAmmoInMagazine() {
        return ammoInMagazine;
    }

    public int getReserveAmmo() {
        return reserveAmmo;
    }

    public int getReloadFrames() {
        return reloadFrames;
    }

    public boolean canReload() {
        return ammoInMagazine < magazineCapacity && reserveAmmo > 0;
    }

    public boolean reload() {
        if (!canReload()) {
            return false;
        }

        int needed = magazineCapacity - ammoInMagazine;
        int transferred = Math.min(needed, reserveAmmo);
        ammoInMagazine += transferred;
        reserveAmmo -= transferred;
        return transferred > 0;
    }

    public int addReserveAmmo(int amount) {
        if (amount <= 0) {
            return 0;
        }

        int previous = reserveAmmo;
        reserveAmmo = Math.min(maxReserveAmmo, reserveAmmo + amount);
        return reserveAmmo - previous;
    }

    public int mergeAmmoFrom(Weapon other) {
        if (!isSameModel(other)) {
            return 0;
        }

        int gained = 0;
        if (ammoInMagazine < magazineCapacity) {
            int needed = magazineCapacity - ammoInMagazine;
            int movedFromMagazine = Math.min(needed, other.ammoInMagazine);
            ammoInMagazine += movedFromMagazine;
            gained += movedFromMagazine;
            needed -= movedFromMagazine;

            if (needed > 0) {
                int movedFromReserve = Math.min(needed, other.reserveAmmo);
                ammoInMagazine += movedFromReserve;
                gained += movedFromReserve;
            }
        }

        gained += addReserveAmmo(other.ammoInMagazine);
        gained += addReserveAmmo(other.reserveAmmo);
        return gained;
    }

    public double getAiOptimalRange() {
        if (isMinigun()) {
            return 255.0;
        }
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

    public boolean fire(Homme shooter, double facingX, double facingY) {
        return fire(shooter, shooter.x, shooter.y, facingX, facingY);
    }

    public boolean fire(Homme shooter, double originX, double originY, double facingX, double facingY) {
        double len = Math.hypot(facingX, facingY);
        if (len <= 0.0001) {
            return false;
        }

        if (shooter instanceof Protagonist) {
            if (ammoInMagazine <= 0) {
                return false;
            }
            ammoInMagazine--;
        }

        double dirX = facingX / len;
        double dirY = facingY / len;
        double projectileSpeedValue = getProjectileSpeed();
        double[] muzzleOrigin = getMuzzleOrigin(originX, originY, dirX, dirY);

        if (GameMode.current == GameMode.ARCADE) {
            triggerArcadeFireFeedback();
        }

        if (projectileType == Projectile.ProjectileType.SHOTGUN_PELLET) {
            int pelletCount = 8;
            double spreadAngle = Math.PI / 6;
            double baseAngle = Math.atan2(dirY, dirX);

            for (int i = 0; i < pelletCount; i++) {
                double angle = baseAngle + (spreadAngle / (pelletCount - 1)) * i - spreadAngle / 2;
                double spreadX = Math.cos(angle);
                double spreadY = Math.sin(angle);

                ObjectManager.list.add(new Projectile(
                        muzzleOrigin[0],
                        muzzleOrigin[1],
                        spreadX * projectileSpeedValue,
                        spreadY * projectileSpeedValue,
                        shooter,
                        projectileType));
            }
        } else {
            ObjectManager.list.add(new Projectile(
                    muzzleOrigin[0],
                    muzzleOrigin[1],
                    dirX * projectileSpeedValue,
                    dirY * projectileSpeedValue,
                    shooter,
                    projectileType));
        }

        if (ejectShell) {
            spawnDouille(shooter, dirX, dirY);
        }

        playSound();
        return true;
    }

    public void draw(Graphics2D g2d, double x, double y, int timer, boolean shot) {
        if (sprite == null && !isMinigun()) {
            return;
        }

        double recoilScale = GameMode.current == GameMode.ARCADE ? 1.35 : 1.0;
        int offsetArme = shot ? (int) (Math.sin(timer * 0.15) * recoilAmplitude * recoilScale) : 0;
        if (sprite != null) {
            g2d.drawImage(sprite, (int) x + drawOffsetX, (int) y + drawOffsetY + offsetArme, drawWidth, drawHeight, null);
            return;
        }

        int baseX = (int) x + drawOffsetX;
        int baseY = (int) y + drawOffsetY + offsetArme;
        g2d.setColor(new Color(36, 42, 48));
        g2d.fillRoundRect(baseX, baseY, drawWidth, drawHeight, 4, 4);
        g2d.setColor(new Color(88, 99, 112));
        g2d.fillRect(baseX + 3, baseY + 4, drawWidth - 5, drawHeight - 12);
        g2d.setColor(new Color(122, 132, 144));
        g2d.fillRect(baseX + drawWidth - 3, baseY + 6, 8, 16);
        g2d.setColor(new Color(24, 26, 30));
        g2d.fillRect(baseX - 2, baseY + drawHeight - 8, 6, 10);
    }

    private void playSound() {
        switch (fireSound) {
            case LASER -> Utils.playLaserSound();
            case SMG -> Utils.playSmgSound();
            case PISTOL -> Utils.playPistolSound();
            case SHOTGUN -> Utils.playShotgunSound();
            case GRENADE -> Utils.playGrenadeSound();
            case NONE -> {
            }
        }
    }

    private void triggerArcadeFireFeedback() {
        if (projectileType == Projectile.ProjectileType.GRENADE) {
            GamePanel.triggerScreenShake(10, 5.0);
            GamePanel.triggerScreenFlash(new Color(255, 226, 180), 0.12f, 5);
            return;
        }
        if (isShotgun()) {
            GamePanel.triggerScreenShake(6, 3.0);
            GamePanel.triggerScreenFlash(new Color(255, 232, 190), 0.08f, 4);
            return;
        }
        if (isAutomatic()) {
            GamePanel.triggerScreenShake(3, 1.2);
            GamePanel.triggerScreenFlash(new Color(255, 241, 210), 0.03f, 2);
        }
    }

    private double[] getMuzzleOrigin(double originX, double originY, double dirX, double dirY) {
        double perpX = -dirY;
        double perpY = dirX;

        double forwardOffset = getMuzzleForwardOffset();
        double lateralOffset = getMuzzleLateralOffset() + MUZZLE_LEFT_BIAS;
        double muzzleX = originX + dirX * forwardOffset - perpX * lateralOffset;
        double muzzleY = originY + dirY * forwardOffset - perpY * lateralOffset;
        return new double[]{muzzleX, muzzleY};
    }

    private double getMuzzleForwardOffset() {
        double base = drawOffsetX + drawWidth - 1;
        if (isShotgun()) {
            return base + 4.0;
        }
        if (isMinigun()) {
            return base + 2.0;
        }
        if (projectileType == Projectile.ProjectileType.GRENADE) {
            return base + 1.0;
        }
        return base + 3.0;
    }

    private double getMuzzleLateralOffset() {
        double centerY = drawOffsetY + drawHeight * 0.5;
        if (isShotgun()) {
            return centerY + 1.5;
        }
        if (isMinigun()) {
            return centerY - 1.0;
        }
        if (projectileType == Projectile.ProjectileType.GRENADE) {
            return centerY + 0.5;
        }
        return centerY;
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