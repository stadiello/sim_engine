package object;

import java.awt.*;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Soldat extends Homme {

    private static Image imgCorps;
    private static Image arme;

    static {
        try {
            imgCorps = ImageIO.read(Soldat.class.getResourceAsStream("/assets/soldats/corps.png"));
            arme = ImageIO.read(Soldat.class.getResourceAsStream("/assets/soldats/arme.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Soldat(double x, double y) {
        super(x, y);
        vx = (Math.random() - 0.5) * 3; // Soldats plus rapides que les civils
        vy = (Math.random() - 0.5) * 3;
    }

    @Override
    public void update() {
        super.update();
        // Logique de combat ou de patrouille peut être ajoutée ici
    }

    @Override
    public void draw(Graphics g) {
        g.drawImage(imgCorps, (int)x - 16, (int)y - 16, 32, 32, null);
        g.drawImage(arme, (int)x + 5, (int)y - 28, 5, 30, null); // Arme dessinée à côté du corps
    }
    
}
