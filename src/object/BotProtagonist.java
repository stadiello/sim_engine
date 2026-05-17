package object;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;

import gameController.*;
import object.ObjectManager;
import object.ai.BotBrain;


public class BotProtagonist extends Homme{

    private static Image imgCorps;
    private static Image arme;
    private static boolean shot = false;
    private static ArrayList<GameObject> listEntite;
    private BotBrain action;

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
    
    public BotProtagonist(double x, double y, GameKeyController keyController) {
        super(x, y);
        this.keyController = keyController;
        vx = 0;
        vy = 0;
        facingX = 0;
        facingY = -1;
    }

    @Override
    public void update() {

        listEntite = ObjectManager.list;
        action = BotBrain(listEntite);
    

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
