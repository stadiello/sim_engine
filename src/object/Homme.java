package object;

import java.awt.*;
import java.io.IOException;
import javax.imageio.ImageIO;

import object.ai.AiTuning;

public class Homme extends GameObject {

    private static final double COLLISION_RADIUS = 14.0;

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
    private int suppressionFrames = 0;
    
    public Homme(double x, double y) {
        super(x, y);
        vx = (Math.random() - 0.5) * 2;
        vy = (Math.random() - 0.5) * 2;
    }

    @Override
    protected boolean canMoveTo(double nextX, double nextY, double radius) {
        return super.canMoveTo(nextX, nextY, radius)
                && canOccupyHumanSpace(nextX, nextY, Math.max(radius, COLLISION_RADIUS));
    }

    protected boolean canOccupyHumanSpace(double nextX, double nextY, double radius) {
        return ObjectManager.isHumanAreaFree(nextX, nextY, radius, this);
    }

    public void update() {
        tickSuppression();
        moveWithTileCollision(14);
        timer++;
    }

    public void onIncomingFire(Homme attacker, double intensity) {
        if (intensity <= 0) {
            return;
        }

        double clamped = Math.max(0.15, Math.min(1.0, intensity));
        int add = (int) Math.round(AiTuning.getSuppressionDurationFrames() * clamped);
        suppressionFrames = Math.min(500, suppressionFrames + add);
    }

    public void tickSuppression() {
        if (suppressionFrames > 0) {
            suppressionFrames--;
        }
    }

    public boolean isSuppressed() {
        return suppressionFrames > 0;
    }

    public double getSuppressionLevel() {
        int duration = Math.max(1, AiTuning.getSuppressionDurationFrames());
        return Math.min(1.0, suppressionFrames / (double) duration);
    }

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

    protected double getCollisionRadius() {
        return COLLISION_RADIUS;
    }

}
