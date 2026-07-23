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
    private int suppressionFlinchFrames = 0;
    
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

        double clamped = Math.max(0.08, Math.min(1.0, intensity));
        int add = (int) Math.round(AiTuning.getSuppressionDurationFrames() * clamped);
        suppressionFrames = Math.min(420, suppressionFrames + add);
        suppressionFlinchFrames = Math.min(12, suppressionFlinchFrames + (int) Math.round(6 * clamped));
    }

    public void tickSuppression() {
        if (suppressionFrames > 0) {
            suppressionFrames--;
        }
        if (suppressionFlinchFrames > 0) {
            suppressionFlinchFrames--;
        }
    }

    public boolean isSuppressed() {
        return suppressionFrames > 0;
    }

    public double getSuppressionLevel() {
        int duration = Math.max(1, AiTuning.getSuppressionDurationFrames());
        return Math.min(1.0, suppressionFrames / (double) duration);
    }

    public int getSuppressionStage() {
        double level = getSuppressionLevel();
        if (level >= 0.75) {
            return 3;
        }
        if (level >= 0.45) {
            return 2;
        }
        if (level >= 0.15) {
            return 1;
        }
        return 0;
    }

    public double getSuppressionMoveMultiplier() {
        return 1.0 - 0.22 * getSuppressionLevel();
    }

    public double getSuppressionFlinchLevel() {
        return Math.min(1.0, suppressionFlinchFrames / 12.0);
    }

    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        double angle = Math.atan2(vy, vx) + Math.PI / 2; // Calcul de l'angle de rotation
        double suppressionLevel = getSuppressionLevel();
        double flinchLevel = getSuppressionFlinchLevel();
        double swayX = Math.sin(timer * 0.12) * 0.8 * suppressionLevel + Math.sin(timer * 0.45) * 1.4 * flinchLevel;
        double swayY = Math.cos(timer * 0.18) * 0.4 * flinchLevel;
        int offsetBras = (int) (Math.sin(timer * 0.15) * 6 + Math.sin(timer * 0.8) * 3 * flinchLevel);
        var old = g2d.getTransform(); // Sauvegarde de la transformation actuelle

        g2d.rotate(angle, x + swayX, y + swayY);
        g2d.drawImage(imgBrasG, (int) Math.round(x - 20 + swayX), (int) Math.round(y - 16 - offsetBras + swayY), 10, 20, null);
        g2d.drawImage(imgBrasD, (int) Math.round(x + 10 + swayX), (int) Math.round(y - 16 + offsetBras + swayY), 10, 20, null);
        g2d.drawImage(imgCorps, (int) Math.round(x - 16 + swayX), (int) Math.round(y - 16 + swayY), 32, 32, null);
        g2d.setTransform(old); // Restauration de la transformation originale pour ne pas affecter les autres dessins
        
    }

    protected double getCollisionRadius() {
        return COLLISION_RADIUS;
    }

    @Override
    public void onDeath() {
        ObjectManager.list.add(new DeathMarker(this));
    }

}
