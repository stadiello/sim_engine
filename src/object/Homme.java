package object;

import java.awt.*;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Homme extends GameObject {

    private static Image imgCorps;
    private static Image imgBrasD;
    private static Image imgBrasG;

    static {
        try {
            imgCorps = ImageIO.read(Homme.class.getResourceAsStream("/assets/civils/corps.png"));
            imgBrasD = ImageIO.read(Homme.class.getResourceAsStream("/assets/civils/bras_d.png"));
            imgBrasG = ImageIO.read(Homme.class.getResourceAsStream("/assets/civils/bras_g.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int timer = 0;
    
    public Homme(double x, double y) {
        super(x, y);
        vx = (Math.random() - 0.5) * 2;
        vy = (Math.random() - 0.5) * 2;
    }

    public void update() {
        x += vx;
        y += vy;
        timer++;

        if (x < 0 || x > 800) vx = -vx;
        if (y < 0 || y > 600) vy = -vy;
    }
    
    // public void draw(Graphics g) {
    //     int offsetBras = (int)(Math.sin(timer * 0.15) * 6);

    //     // bras gauche monte quand bras droit descend
    //     g.drawImage(imgBrasG, (int)x - 20, (int)y - 16 - offsetBras, 10, 20, null);
    //     g.drawImage(imgBrasD, (int)x + 10, (int)y - 16 + offsetBras, 10, 20, null);
    //     // corps par dessus les bras
    //     g.drawImage(imgCorps, (int)x - 16, (int)y - 16, 32, 32, null);
    // }

    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        double angle = Math.atan2(vy, vx) + Math.PI / 2; // Calcul de l'angle de rotation
        int offsetBras = (int)(Math.sin(timer * 0.15) * 6);
        var old = g2d.getTransform(); // Sauvegarde de la transformation actuelle

        g2d.rotate(angle, x, y);
        g2d.drawImage(imgBrasG, (int)x - 20, (int)y - 16 - offsetBras, 10, 20, null);
        g2d.drawImage(imgBrasD, (int)x + 10, (int)y - 16 + offsetBras, 10, 20, null);
        g2d.drawImage(imgCorps, (int)x - 16, (int)y - 16, 32, 32, null);
        g2d.setTransform(old); // Restauration de la transformation originale pour ne pas affecter les autres dessins
        
    }

}
