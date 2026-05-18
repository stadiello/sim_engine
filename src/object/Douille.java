package object;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Douille extends GameObject {

    private static Image imgDouille;

    private static final double RADIUS = 2.0;
    private static final double BOUNCE_FACTOR = 0.45;
    private static final double FRICTION_FACTOR = 0.9;
    private static final double STOP_SPEED_SQ = 0.02;
    private static final int REST_FRAMES = 200; // 200 frames à 60 FPS = 3.33 secondes de repos avant disparition, 90 frames à 60 FPS = 1.5 secondes de repos avant disparition

    static {
        try {
            imgDouille = ImageIO.read(Douille.class.getResourceAsStream("/assets/effets/douille.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double angle;
    private double rotationSpeed;
    private int restTimer = REST_FRAMES;
    private boolean resting;

    public Douille(double x, double y, double vx, double vy, double angle, double rotationSpeed) {
        super(x, y);
        this.vx = vx;
        this.vy = vy;
        this.angle = angle;
        this.rotationSpeed = rotationSpeed;
        this.resting = false;
    }

    @Override
    public void update() {
        if (resting) {
            restTimer--;
            if (restTimer <= 0) {
                ObjectManager.list.remove(this);
            }
            return;
        }

        double nextX = x + vx;
        if (canMoveTo(nextX, y, RADIUS)) {
            x = nextX;
        } else {
            vx = -vx * BOUNCE_FACTOR;
        }

        double nextY = y + vy;
        if (canMoveTo(x, nextY, RADIUS)) {
            y = nextY;
        } else {
            vy = -vy * BOUNCE_FACTOR;
        }

        vx *= FRICTION_FACTOR;
        vy *= FRICTION_FACTOR;

        angle += rotationSpeed;
        rotationSpeed *= 0.95;

        if (vx * vx + vy * vy < STOP_SPEED_SQ) {
            vx = 0;
            vy = 0;
            rotationSpeed = 0;
            resting = true;
        }
    }

    @Override
    public void draw(Graphics g) {
        if (imgDouille == null) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        var old = g2d.getTransform();

        g2d.rotate(angle, x, y);
        g2d.drawImage(imgDouille, (int) x - 4, (int) y - 2, 8, 4, null);
        g2d.setTransform(old);
    }
}