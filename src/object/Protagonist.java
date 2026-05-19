package object;

import java.awt.*;
import java.io.IOException;
import javax.imageio.ImageIO;

import gameController.*;
import main.GameMode;
import object.weapon.Weapon;
import world.TileManager;
import java.util.ArrayList;

public class Protagonist extends Homme{

    private static Image imgCorps;
    private final ArrayList<Weapon> loadout;
    private boolean shot = false;

    private static final double MOVE_SPEED = 2.6;

    private int shootCooldown = 0;
    private int timer = 0;
    private final GameKeyController keyController;
    private double facingX;
    private double facingY;
    private int selectedWeaponIndex = 0;

    static {
        try {
            imgCorps = ImageIO.read(Soldat.class.getResourceAsStream("/assets/soldats/corps.png"));
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
    public void update() {
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

        g2d.rotate(angle, x, y);
        g2d.drawImage(imgCorps, (int)x - 16, (int)y - 16, 32, 42, null);
        
        // gérer le recule de l'arme lors du tir
        if (currentWeapon != null) {
            currentWeapon.draw(g2d, x, y, timer, shot);
        }
        shot = false;
        g2d.setTransform(old); // Restauration de la transformation originale pour ne pas affecter les autres dessins
    
    }

}
