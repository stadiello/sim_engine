package object;

import java.awt.*;
import java.io.IOException;
import javax.imageio.ImageIO;

import gameController.*;
import main.GameMode;
import main.Utils;
import object.ai.TacticalMovement;
import object.weapon.Weapon;
import world.TileManager;
import java.util.ArrayList;

public class Protagonist extends Homme{

    private static final double ARCADE_MOVE_SPEED_MULTIPLIER = 1.12;
    private static final double SPRINT_MOVE_SPEED_MULTIPLIER = 1.45;
    private static final int MAX_ARMOR_PLATES = 3;

    private static Image imgCorps;
    private final ArrayList<Weapon> loadout;
    private boolean shot = false;

    private static final double MOVE_SPEED = 2.6;

    private int shootCooldown = 0;
    private int reloadTimer = 0;
    private int timer = 0;
    private final PlayerInput keyController;
    private final boolean primaryPlayer;
    private double facingX;
    private double facingY;
    private int selectedWeaponIndex = 0;
    private int armorPlates = 0;
    private boolean controlsEnabled = true;

    static {
        try {
            imgCorps = ImageIO.read(Soldat.class.getResourceAsStream("/assets/soldats/corps.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public Protagonist(double x, double y, PlayerInput keyController) {
        this(x, y, keyController, true);
    }

    public Protagonist(double x, double y, PlayerInput keyController, boolean primaryPlayer) {
        super(x, y);
        this.keyController = keyController;
        this.primaryPlayer = primaryPlayer;
        this.loadout = new ArrayList<>();
        for (Weapon weapon : getInitialLoadout()) {
            loadout.add(weapon);
        }
        vx = 0;
        vy = 0;
        facingX = 0;
        facingY = -1;
    }

    public boolean isPrimaryPlayer() {
        return primaryPlayer;
    }

    public PlayerInput getPlayerInput() {
        return keyController;
    }

    public void setControlsEnabled(boolean enabled) {
        controlsEnabled = enabled;
        if (!enabled) {
            vx = 0;
            vy = 0;
        }
    }

    private static Weapon[] getInitialLoadout() {
        return isMissionStyleMode() ? Weapon.storyLoadout() : Weapon.protagonistLoadout();
    }

    private static boolean isMissionStyleMode() {
        return GameMode.current == GameMode.STORY
                || GameMode.current == GameMode.PROTECTION
                || GameMode.current == GameMode.MISSION;
    }

    private Weapon findMatchingWeapon(Weapon weapon) {
        if (weapon == null) {
            return null;
        }

        for (Weapon current : loadout) {
            if (current.isSameModel(weapon)) {
                return current;
            }
        }

        return null;
    }

    /** Ajoute une arme au loadout si elle n'est pas déjà présente. Retourne true si ajoutée. */
    public boolean addWeapon(Weapon weapon) {
        Weapon matchingWeapon = findMatchingWeapon(weapon);
        if (matchingWeapon != null) {
            matchingWeapon.mergeAmmoFrom(weapon);
            return true;
        }

        loadout.add(weapon);
        return true;
    }

    public boolean hasWeapon(Weapon weapon) {
        return findMatchingWeapon(weapon) != null;
    }

    public boolean addAmmoForWeapon(Weapon weapon, int reserveAmmo) {
        if (weapon == null || reserveAmmo <= 0) {
            return false;
        }

        Weapon matchingWeapon = findMatchingWeapon(weapon);
        return matchingWeapon != null && matchingWeapon.addReserveAmmo(reserveAmmo) > 0;
    }

    public boolean addArmorPlate() {
        if (armorPlates >= MAX_ARMOR_PLATES) {
            return false;
        }
        armorPlates++;
        return true;
    }

    public boolean consumeArmorPlateOnHit() {
        if (armorPlates <= 0) {
            return false;
        }
        armorPlates--;
        return true;
    }

    public int getArmorPlates() {
        return armorPlates;
    }

    public int getMaxArmorPlates() {
        return MAX_ARMOR_PLATES;
    }

    public boolean restockAmmunition() {
        boolean restocked = false;
        for (Weapon weapon : loadout) {
            restocked |= weapon.refillReserveAmmo() > 0;
        }
        return restocked;
    }

    @Override
    protected boolean canOccupyHumanSpace(double nextX, double nextY, double radius) {
        ArrayList<Homme> overlappingHumans = ObjectManager.getOverlappingHumans(nextX, nextY, radius, this);
        if (overlappingHumans.isEmpty()) {
            return true;
        }

        double minSeparation = radius + getCollisionRadius() + 0.5;
        double moveDx = nextX - x;
        double moveDy = nextY - y;
        double moveLenSq = moveDx * moveDx + moveDy * moveDy;
        if (moveLenSq <= 0.0000001) {
            return false;
        }

        double moveLen = Math.sqrt(moveLenSq);
        double fallbackDirX = moveDx / moveLen;
        double fallbackDirY = moveDy / moveLen;

        GameObject[] ignoredObjects = new GameObject[overlappingHumans.size() + 1];
        ignoredObjects[0] = this;
        for (int i = 0; i < overlappingHumans.size(); i++) {
            ignoredObjects[i + 1] = overlappingHumans.get(i);
        }

        ArrayList<double[]> candidatePositions = new ArrayList<>(overlappingHumans.size());
        for (Homme other : overlappingHumans) {
            double awayX = other.x - nextX;
            double awayY = other.y - nextY;
            double awayLenSq = awayX * awayX + awayY * awayY;

            double dirX;
            double dirY;
            if (awayLenSq <= 0.0000001) {
                dirX = fallbackDirX;
                dirY = fallbackDirY;
            } else {
                double awayLen = Math.sqrt(awayLenSq);
                dirX = awayX / awayLen;
                dirY = awayY / awayLen;
            }

            double candidateX = nextX + dirX * minSeparation;
            double candidateY = nextY + dirY * minSeparation;

            if (!TacticalMovement.canStandAt(candidateX, candidateY, other.getCollisionRadius())
                    || !ObjectManager.isHumanAreaFree(candidateX, candidateY, other.getCollisionRadius(), ignoredObjects)) {
                return false;
            }

            candidatePositions.add(new double[]{candidateX, candidateY});
        }

        for (int i = 0; i < candidatePositions.size(); i++) {
            double[] first = candidatePositions.get(i);
            for (int j = i + 1; j < candidatePositions.size(); j++) {
                double[] second = candidatePositions.get(j);
                double dx = first[0] - second[0];
                double dy = first[1] - second[1];
                if (dx * dx + dy * dy < minSeparation * minSeparation) {
                    return false;
                }
            }
        }

        for (int i = 0; i < overlappingHumans.size(); i++) {
            Homme other = overlappingHumans.get(i);
            double[] candidate = candidatePositions.get(i);
            other.x = candidate[0];
            other.y = candidate[1];
        }

        return true;
    }

    @Override
    public void update() {
        tickSuppression();

        if (!controlsEnabled) {
            vx = 0;
            vy = 0;
            if (shootCooldown > 0) shootCooldown--;
            tickReload();
            timer++;
            return;
        }

        double inputX = 0;
        double inputY = 0;

        TileManager tileManager = ObjectManager.getTileManager();
        int cameraX = tileManager != null ? tileManager.getCameraX() : 0;
        int cameraY = tileManager != null ? tileManager.getCameraY() : 0;

        double toMouseX = (keyController.getMouseX() + cameraX) - x;
        double toMouseY = (keyController.getMouseY() + cameraY) - y;
        double mouseLength = Math.sqrt(toMouseX * toMouseX + toMouseY * toMouseY);

        if (mouseLength > 0.0001) {
            facingX = toMouseX / mouseLength;
            facingY = toMouseY / mouseLength;
        }

        int weaponScrollDelta = keyController.consumeWeaponScrollDelta();
        if (weaponScrollDelta != 0 && !loadout.isEmpty() && reloadTimer == 0) {
            selectedWeaponIndex = Math.floorMod(selectedWeaponIndex + weaponScrollDelta, loadout.size());
        }

        if (shootCooldown > 0) {
            shootCooldown--;
        }
        tickReload();

        double moveSpeed = MOVE_SPEED * getSuppressionMoveMultiplier();
        if (GameMode.current == GameMode.ARCADE) {
            moveSpeed *= ARCADE_MOVE_SPEED_MULTIPLIER;
        }
        if (keyController.isSprint()) {
            moveSpeed *= SPRINT_MOVE_SPEED_MULTIPLIER;
        }

        if (keyController.isLeft()) inputX -= 1;
        if (keyController.isRight()) inputX += 1;
        if (keyController.isUp()) inputY -= 1;
        if (keyController.isDown()) inputY += 1;
        Weapon currentWeapon = getCurrentWeapon();
        if (keyController.consumeReloadTriggered()) {
            startReloadIfPossible(currentWeapon);
        }
        boolean leftClickTriggered = keyController.consumeLeftClickPressed();
        boolean shouldFire = currentWeapon != null
                && shootCooldown == 0
                && reloadTimer == 0
                && (currentWeapon.isAutomatic() ? keyController.isLeftClickPressed() : leftClickTriggered);
        if (shouldFire) {
            tryFireCurrentWeapon(currentWeapon);
        }

        if (inputX != 0 || inputY != 0) {
            double length = Math.sqrt(inputX * inputX + inputY * inputY);
            double dirX = inputX / length;
            double dirY = inputY / length;

            vx = dirX * moveSpeed;
            vy = dirY * moveSpeed;
        } else {
            vx = 0;
            vy = 0;
        }

        moveWithTileCollision(14);
        timer++;

    }

    private void tickReload() {
        if (reloadTimer <= 0) {
            return;
        }

        reloadTimer--;
        if (reloadTimer == 0) {
            Weapon weapon = getCurrentWeapon();
            if (weapon != null) {
                weapon.reload();
            }
        }
    }

    private void startReloadIfPossible(Weapon weapon) {
        if (weapon != null && reloadTimer == 0 && weapon.canReload()) {
            if (weapon.isGrenade()) {
                weapon.reload();
                return;
            }
            reloadTimer = weapon.getReloadFrames();
            Utils.playReloadSound();
        }
    }

    private void tryFireCurrentWeapon(Weapon weapon) {
        if (weapon.fire(this, facingX, facingY)) {
            shootCooldown = weapon.getCooldownFrames();
            shot = true;
            if (weapon.isGrenade() && weapon.canReload()) {
                weapon.reload();
            }
            return;
        }

        startReloadIfPossible(weapon);
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        Weapon currentWeapon = getCurrentWeapon();

        double angle = Math.atan2(facingY, facingX) + Math.PI / 2;
        var old = g2d.getTransform(); // Sauvegarde de la transformation actuelle

        g2d.setColor(primaryPlayer ? new Color(90, 180, 255, 210) : new Color(105, 255, 145, 220));
        g2d.setStroke(new BasicStroke(3f));
        g2d.drawOval((int) Math.round(x) - 21, (int) Math.round(y) - 20, 42, 42);

        g2d.rotate(angle, x, y);

        g2d.drawImage(imgCorps, (int) x - 16, (int) y - 16, 32, 42, null);

        if (currentWeapon != null) {
            currentWeapon.draw(g2d, x, y, timer, shot);
        }
        shot = false;
        g2d.setTransform(old); // Restauration de la transformation originale pour ne pas affecter les autres dessins
    
    }

    public double getFacingX() {
        return facingX;
    }

    public double getFacingY() {
        return facingY;
    }

    public Weapon getCurrentWeapon() {
        if (loadout.isEmpty()) {
            return null;
        }
        return loadout.get(Math.min(selectedWeaponIndex, loadout.size() - 1));
    }

    public boolean isReloading() {
        return reloadTimer > 0;
    }

    public int getReloadTimer() {
        return reloadTimer;
    }

    public double getReloadProgress() {
        Weapon currentWeapon = getCurrentWeapon();
        if (currentWeapon == null || reloadTimer <= 0) {
            return 0.0;
        }

        int reloadFrames = Math.max(1, currentWeapon.getReloadFrames());
        return 1.0 - Math.max(0.0, Math.min(1.0, reloadTimer / (double) reloadFrames));
    }

}
