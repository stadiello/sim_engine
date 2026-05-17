package object;

import java.awt.*;
import java.io.IOException;
import javax.imageio.ImageIO;

import gameController.*;
import main.Utils;

public class Protagonist extends Homme{

    private static Image imgCorps;
    private static Image[] armes;
    private boolean shot = false;

    private static final double MOVE_SPEED = 2.6;
    private static final int WEAPON_CARABINE_INDEX = 1;
    private static final int COOLDOWN_BLASTER = 15;
    private static final int COOLDOWN_CARABINE = 4;

    private int shootCooldown = 0;
    private int timer = 0;
    private final GameKeyController keyController;
    private double facingX;
    private double facingY;
    private int selectedWeaponIndex = 0;

    static {
        try {
            imgCorps = ImageIO.read(Soldat.class.getResourceAsStream("/assets/soldats/corps.png"));
            armes = new Image[]{
                ImageIO.read(Soldat.class.getResourceAsStream("/assets/armes/blaster.png")),
                ImageIO.read(Soldat.class.getResourceAsStream("/assets/armes/carabine.png"))
            };
        } catch (IOException e) {
            e.printStackTrace();
            armes = new Image[0];
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
        if (weaponScrollDelta != 0 && armes.length > 0) {
            selectedWeaponIndex = Math.floorMod(selectedWeaponIndex + weaponScrollDelta, armes.length);
        }

        if (shootCooldown > 0) {
                shootCooldown--;
        }

        if (keyController.isLeft()) inputX -= 1;
        if (keyController.isRight()) inputX += 1;
        if (keyController.isUp()) inputY -= 1;
        if (keyController.isDown()) inputY += 1;
        if (keyController.isLeftClickPressed() && shootCooldown == 0) {
            boolean isCarabine = selectedWeaponIndex == WEAPON_CARABINE_INDEX;
            double projectileSpeed = 6.5;
            Projectile.ProjectileType projectileType = isCarabine
                    ? Projectile.ProjectileType.BULLET
                    : Projectile.ProjectileType.DEFAULT;
            ObjectManager.list.add(new Projectile((int)x, (int)y, facingX * projectileSpeed, facingY * projectileSpeed, this, projectileType));
            if (isCarabine) {
                spawnDouille();
                Utils.playSmgSound();
            } else {
                Utils.playLaserSound();
            }
            shootCooldown = isCarabine ? COOLDOWN_CARABINE : COOLDOWN_BLASTER;
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

    private void spawnDouille() {
        double sideX = -facingY;
        double sideY = facingX;

        double shellX = x + sideX * 10 - facingX * 4;
        double shellY = y + sideY * 10 - facingY * 4;

        double shellSpeed = 2.0 + Math.random() * 1.3;
        double shellVx = sideX * shellSpeed - facingX * 0.7 + (Math.random() - 0.5) * 0.8;
        double shellVy = sideY * shellSpeed - facingY * 0.7 + (Math.random() - 0.5) * 0.8;
        double shellAngle = Math.atan2(shellVy, shellVx);
        double shellRotationSpeed = (Math.random() - 0.5) * 0.35;

        ObjectManager.list.add(new Douille(shellX, shellY, shellVx, shellVy, shellAngle, shellRotationSpeed));
    }
    
    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        
        double drawVx = facingX;
        double drawVy = facingY;
        Image currentWeapon = armes.length > 0 ? armes[selectedWeaponIndex] : null;

        double angle = Math.atan2(drawVy, drawVx) + Math.PI / 2;
        int offsetArme = (int)(Math.sin(timer * 0.15) * 12);
        var old = g2d.getTransform(); // Sauvegarde de la transformation actuelle

        g2d.rotate(angle, x, y);
        g2d.drawImage(imgCorps, (int)x - 16, (int)y - 16, 32, 42, null);
        
        // gérer le recule de l'arme lors du tir
        if (shot == true) {
            if (currentWeapon != null) {
                g2d.drawImage(currentWeapon, (int)x + 5, (int)y - 23 + offsetArme, 5, 30, null); // Arme dessinée à côté du corps avec un léger mouvement
            }
            shot = false;
        } else {
            if (currentWeapon != null) {
                g2d.drawImage(currentWeapon, (int)x + 5, (int)y - 23, 5, 30, null); // Arme dessinée à côté du corps avec un léger mouvement
            }
        }
        g2d.setTransform(old); // Restauration de la transformation originale pour ne pas affecter les autres dessins
    
    }

}
