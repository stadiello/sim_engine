package object;

import java.awt.*;
import java.io.IOException;
import javax.imageio.ImageIO;

import gameController.*;
import object.weapon.Weapon;

public class Protagonist extends Homme{

    private static Image imgCorps;
    private static final Weapon[] LOADOUT = Weapon.protagonistLoadout();
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
        vx = 0;
        vy = 0;
        facingX = 0;
        facingY = -1;
    }

    @Override
    public void update() {
        double inputX = 0;
        double inputY = 0;

        double toMouseX = keyController.getMouseX() - x;
        double toMouseY = keyController.getMouseY() - y;
        double mouseLength = Math.sqrt(toMouseX * toMouseX + toMouseY * toMouseY);

        if (mouseLength > 0.0001) {
            facingX = toMouseX / mouseLength;
            facingY = toMouseY / mouseLength;
        }

        int weaponScrollDelta = keyController.consumeWeaponScrollDelta();
        if (weaponScrollDelta != 0 && LOADOUT.length > 0) {
            selectedWeaponIndex = Math.floorMod(selectedWeaponIndex + weaponScrollDelta, LOADOUT.length);
        }

        if (shootCooldown > 0) {
                shootCooldown--;
        }

        if (keyController.isLeft()) inputX -= 1;
        if (keyController.isRight()) inputX += 1;
        if (keyController.isUp()) inputY -= 1;
        if (keyController.isDown()) inputY += 1;
        Weapon currentWeapon = LOADOUT.length > 0 ? LOADOUT[selectedWeaponIndex] : null;
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
        Weapon currentWeapon = LOADOUT.length > 0 ? LOADOUT[selectedWeaponIndex] : null;

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
