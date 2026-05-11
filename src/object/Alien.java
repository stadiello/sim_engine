package object;

import java.awt.*;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Alien extends Homme {

    private static Image imgAlien;

    static {
        try {
            imgAlien = ImageIO.read(Alien.class.getResourceAsStream("/assets/aliens/alien.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private int timer = 0;

    public Alien(double x, double y) {
        super(x, y);
        vx = (Math.random() - 0.5) * 4;
        vy = (Math.random() - 0.5) * 4;
    }

    public void update() {
        x += vx;
        y += vy;
        timer++;


        if (x < 0 || x > 800) vx = -vx;
        if (y < 0 || y > 600) vy = -vy;
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        double angle = Math.atan2(vy, vx) + Math.PI / 2; // Calcul de l'angle de rotation
        // Ajouter une osilation droite / gauche pour rendre l'alien plus vivant
        double wobble = Math.sin(timer*0.15) * 0.12; // Oscillation de 0.12 radians (environ 7 degrés) à une fréquence de 0.15
        var old = g2d.getTransform(); // Sauvegarde de la transformation actuelle

        g2d.rotate(angle + wobble, x, y);
        g2d.drawImage(imgAlien, (int)x - 16, (int)y - 16, 32, 32, null);
        g2d.setTransform(old); // Restauration de la transformation originale pour ne pas affecter les autres dessins
    }
    
}
