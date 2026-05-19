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

    private static final double MAX_TURN_PER_FRAME_RAD = Math.toRadians(10.0);

    private static Image imgEnnemi;

    static {
        try {
            imgEnnemi = ImageIO.read(Ennemi.class.getResourceAsStream("/assets/badGuys/ennemis.png"));
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
        if (len <= 0.0001) {
            return;
        }

        double targetX = dx / len;
        double targetY = dy / len;
        double currentAngle = Math.atan2(facingY, facingX);
        double targetAngle = Math.atan2(targetY, targetX);
        double delta = normalizeAngle(targetAngle - currentAngle);

        if (Math.abs(delta) <= MAX_TURN_PER_FRAME_RAD) {
            facingX = targetX;
            facingY = targetY;
            return;
        }

        double nextAngle = currentAngle + Math.copySign(MAX_TURN_PER_FRAME_RAD, delta);
        facingX = Math.cos(nextAngle);
        facingY = Math.sin(nextAngle);
    }

    private static double normalizeAngle(double angle) {
        while (angle > Math.PI) {
            angle -= Math.PI * 2;
        }
        while (angle < -Math.PI) {
            angle += Math.PI * 2;
        }
        return angle;
    }

    public double getFacingX() {
        return facingX;
    }

    public double getFacingY() {
        return facingY;
    }

    public Weapon getCarriedWeapon() {
        return carriedWeapon;
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
        var old = g2d.getTransform();

        g2d.rotate(angle, x, y);
        g2d.drawImage(imgEnnemi, (int) x - 16, (int) y - 16, 32, 42, null);
        carriedWeapon.draw(g2d, x, y, shotAnimTimer, shotAnimTimer > 0);
        g2d.setTransform(old);
    }
}
