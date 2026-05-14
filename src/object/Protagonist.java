package object;

import java.awt.*;
import java.io.IOException;
import javax.imageio.ImageIO;

import gameController.*;

public class Protagonist extends Homme{

    private static Image imgCorps;
    private static Image arme;
    private static boolean shot = false;

    private static final double MOVE_SPEED = 2.6;

    private int shootCooldown = 0;
    private int timer = 0;
    private final GameKeyController keyController;
    private double facingX;
    private double facingY;

    static {
        try {
            imgCorps = ImageIO.read(Soldat.class.getResourceAsStream("/assets/soldats/corps.png"));
            arme = ImageIO.read(Soldat.class.getResourceAsStream("/assets/soldats/arme.png"));
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

        if (shootCooldown > 0) {
                shootCooldown--;
        }

        if (keyController.isLeft()) inputX -= 1;
        if (keyController.isRight()) inputX += 1;
        if (keyController.isUp()) inputY -= 1;
        if (keyController.isDown()) inputY += 1;
        if (keyController.isSpace() && shootCooldown == 0) {
            double projectileSpeed = 6.5;
            ObjectManager.list.add(new Projectile((int)x, (int)y, facingX * projectileSpeed, facingY * projectileSpeed, this));
            shootCooldown = 15;
            shot = true;
        }

        if (inputX != 0 || inputY != 0) {
            double length = Math.sqrt(inputX * inputX + inputY * inputY);
            double dirX = inputX / length;
            double dirY = inputY / length;

            facingX = dirX;
            facingY = dirY;
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

        double angle = Math.atan2(drawVy, drawVx) + Math.PI / 2;
        int offsetArme = (int)(Math.sin(timer * 0.15) * 12);
        var old = g2d.getTransform(); // Sauvegarde de la transformation actuelle

        g2d.rotate(angle, x, y);
        g2d.drawImage(imgCorps, (int)x - 16, (int)y - 16, 32, 42, null);
        
        // gérer le recule de l'arme lors du tir
        if (shot == true) {
            g2d.drawImage(arme, (int)x + 5, (int)y - 23 + offsetArme, 5, 30, null); // Arme dessinée à côté du corps avec un léger mouvement
            shot = false;
        } else {
            g2d.drawImage(arme, (int)x + 5, (int)y - 23, 5, 30, null); // Arme dessinée à côté du corps avec un léger mouvement
        }
        g2d.setTransform(old); // Restauration de la transformation originale pour ne pas affecter les autres dessins
    
    }

}
