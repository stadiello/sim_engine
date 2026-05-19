package object;

import java.awt.*;
import java.io.IOException;
import javax.imageio.ImageIO;

import gameController.*;
import main.GameMode;
import object.ai.TacticalMovement;
import object.weapon.Weapon;
import world.TileManager;
import java.util.ArrayList;

public class Protagonist extends Homme{

    // private static final double WALK_CYCLE_SPEED = 0.42;
    // private static final double WALK_BLEND_RATE = 0.18;

    private static Image imgCorps;
    // private static Image imgPied_d;
    // private static Image imgPied_g;
    private final ArrayList<Weapon> loadout;
    private boolean shot = false;

    private static final double MOVE_SPEED = 2.6;

    private int shootCooldown = 0;
    private int timer = 0;
    private final GameKeyController keyController;
    private double facingX;
    private double facingY;
    private int selectedWeaponIndex = 0;
    // private double walkCycle = 0;
    // private double walkBlend = 0;

    static {
        try {
            imgCorps = ImageIO.read(Soldat.class.getResourceAsStream("/assets/soldats/corps.png"));
            // imgPied_d = ImageIO.read(Soldat.class.getResourceAsStream("/assets/civils/pied_d.png"));
            // imgPied_g = ImageIO.read(Soldat.class.getResourceAsStream("/assets/civils/pied_g.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public Protagonist(double x, double y, GameKeyController keyController) {
        super(x, y);
        this.keyController = keyController;
        Weapon[] initial = GameMode.current == GameMode.STORY
                ? Weapon.storyLoadout()
                : Weapon.protagonistLoadout();
        this.loadout = new ArrayList<>();
        for (Weapon w : initial) {
            loadout.add(w);
        }
        vx = 0;
        vy = 0;
        facingX = 0;
        facingY = -1;
    }

    /** Ajoute une arme au loadout si elle n'est pas déjà présente. Retourne true si ajoutée. */
    public boolean addWeapon(Weapon weapon) {
        if (loadout.contains(weapon)) {
            return false;
        }
        loadout.add(weapon);
        return true;
    }

    public boolean hasWeapon(Weapon weapon) {
        return loadout.contains(weapon);
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
        double inputX = 0;
        double inputY = 0;
        double startX = x;
        double startY = y;

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
        if (weaponScrollDelta != 0 && loadout.size() > 0) {
            selectedWeaponIndex = Math.floorMod(selectedWeaponIndex + weaponScrollDelta, loadout.size());
        }

        if (shootCooldown > 0) {
                shootCooldown--;
        }

        if (keyController.isLeft()) inputX -= 1;
        if (keyController.isRight()) inputX += 1;
        if (keyController.isUp()) inputY -= 1;
        if (keyController.isDown()) inputY += 1;
        Weapon currentWeapon = loadout.size() > 0 ? loadout.get(Math.min(selectedWeaponIndex, loadout.size() - 1)) : null;
        boolean leftClickTriggered = keyController.consumeLeftClickPressed();
        boolean shouldFire = currentWeapon != null
                && shootCooldown == 0
                && (currentWeapon.isAutomatic() ? keyController.isLeftClickPressed() : leftClickTriggered);
        if (shouldFire) {
            currentWeapon.fire(this, facingX, facingY);
            shootCooldown = currentWeapon.getCooldownFrames();
            shot = true;
        }

        if (inputX != 0 || inputY != 0) {
            double length = Math.sqrt(inputX * inputX + inputY * inputY);
            double dirX = inputX / length;
            double dirY = inputY / length;

            vx = dirX * MOVE_SPEED;
            vy = dirY * MOVE_SPEED;
        } else {
            vx = 0;
            vy = 0;
        }

        moveWithTileCollision(14);

        // double movedDistance = Math.hypot(x - startX, y - startY);
        // double targetWalkBlend = Math.min(1.0, movedDistance / MOVE_SPEED);
        // walkBlend += (targetWalkBlend - walkBlend) * WALK_BLEND_RATE;
        // if (movedDistance > 0.001) {
        //     walkCycle += movedDistance * WALK_CYCLE_SPEED;
        // }

        timer++;

    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        
        double drawVx = facingX;
        double drawVy = facingY;
        Weapon currentWeapon = loadout.size() > 0 ? loadout.get(Math.min(selectedWeaponIndex, loadout.size() - 1)) : null;

        double angle = Math.atan2(drawVy, drawVx) + Math.PI / 2;
        var old = g2d.getTransform(); // Sauvegarde de la transformation actuelle

        // double stride = Math.sin(walkCycle);
        // double bounce = Math.cos(walkCycle);
        // int bodyBob = (int) Math.round(Math.abs(stride) * 2.0 * walkBlend);
        // int footSpreadOffset = (int) Math.round(bounce * 1.5 * walkBlend);
        // int rightFootForward = (int) Math.round(stride * 5.5 * walkBlend);
        // int leftFootForward = (int) Math.round(-stride * 5.5 * walkBlend);
        // int rightFootLift = (int) Math.round(Math.max(0.0, -bounce) * 3.0 * walkBlend);
        // int leftFootLift = (int) Math.round(Math.max(0.0, bounce) * 3.0 * walkBlend);

        g2d.rotate(angle, x, y);
        
        // g2d.drawImage(imgPied_d, (int) x +5 - footSpreadOffset, (int) y-6  - rightFootForward - rightFootLift + bodyBob, 7, 15, null);
        // g2d.drawImage(imgPied_g, (int) x -5 + footSpreadOffset, (int) y-6 - leftFootForward - leftFootLift + bodyBob, 7, 15, null);
        // g2d.drawImage(imgCorps, (int) x - 16, (int) y - 16 - bodyBob, 32, 42, null);
        
        g2d.drawImage(imgCorps, (int)x - 16, (int)y - 16, 32, 42, null);


        // gérer le recule de l'arme lors du tir
        if (currentWeapon != null) {
            currentWeapon.draw(g2d, x, y, timer, shot);
        }
        shot = false;
        g2d.setTransform(old); // Restauration de la transformation originale pour ne pas affecter les autres dessins
    
    }

}
