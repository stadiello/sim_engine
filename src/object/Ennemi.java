package object;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.IOException;
import javax.imageio.ImageIO;

import object.ai.BotBrain;
import object.weapon.Weapon;
import main.GameMode;

public class Ennemi extends Homme {

    private static final int GLOCK_DRAW_WIDTH = 6;
    private static final int GLOCK_DRAW_HEIGHT = 16;

    private static Image imgEnnemi;
    private static Image imgArme;

    static {
        try {
            imgEnnemi = ImageIO.read(Ennemi.class.getResourceAsStream("/assets/badGuys/ennemis.png"));
            imgArme = ImageIO.read(Ennemi.class.getResourceAsStream("/assets/armes/glock.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final BotBrain brain;
    private final Weapon carriedWeapon;
    private double facingX = 0;
    private double facingY = -1;
    private int shotAnimTimer = 0;

    public Ennemi(double x, double y) {
        super(x, y);
        this.brain = new BotBrain();
        this.carriedWeapon = pickRandomWeapon();
        this.vx = 0;
        this.vy = 0;
    }

    private static Weapon pickRandomWeapon() {
        double r = Math.random();
        if (r < 0.50) return Weapon.glock();
        if (r < 0.80) return Weapon.carabine();
        return Weapon.shotgun();
    }

    @Override
    public void onDeath() {
        if (GameMode.current == GameMode.STORY) {
            ObjectManager.list.add(new DroppedWeapon(x, y, carriedWeapon));
        }
    }

    @Override
    public void update() {
        brain.update(this);

        if (vx * vx + vy * vy > 0.0001) {
            setFacingDirection(vx, vy);
        }

        if (shotAnimTimer > 0) {
            shotAnimTimer--;
        }

        moveWithTileCollision(14);
    }

    public void setFacingDirection(double dx, double dy) {
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len > 0.0001) {
            facingX = dx / len;
            facingY = dy / len;
        }
    }

    public double getFacingX() {
        return facingX;
    }

    public double getFacingY() {
        return facingY;
    }

    public void onShot() {
        shotAnimTimer = 4;
    }

    @Override
    public void draw(Graphics g) {
        if (imgEnnemi == null) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        double angle = Math.atan2(facingY, facingX) + Math.PI / 2;
        int weaponKickback = shotAnimTimer > 0 ? 3 : 0;
        var old = g2d.getTransform();

        g2d.rotate(angle, x, y);
        g2d.drawImage(imgEnnemi, (int) x - 16, (int) y - 16, 32, 32, null);
        if (imgArme != null) {
            g2d.drawImage(imgArme, (int) x + 5, (int) y - 20 + weaponKickback, GLOCK_DRAW_WIDTH, GLOCK_DRAW_HEIGHT, null);
        }
        g2d.setTransform(old);
    }
}
