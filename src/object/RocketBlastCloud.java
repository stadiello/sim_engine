package object;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class RocketBlastCloud extends GameObject {

    private static final int MAX_LIFETIME = 24;
    private static final int PUFF_COUNT = 7;

    private final double[] offsetX = new double[PUFF_COUNT];
    private final double[] offsetY = new double[PUFF_COUNT];
    private final double[] puffRadius = new double[PUFF_COUNT];
    private int lifetime = MAX_LIFETIME;

    public RocketBlastCloud(double x, double y, double maxRadius) {
        super(x, y);

        for (int i = 0; i < PUFF_COUNT; i++) {
            double angle = (Math.PI * 2.0 * i) / PUFF_COUNT + (Math.random() - 0.5) * 0.35;
            double distance = maxRadius * (0.08 + Math.random() * 0.22);
            offsetX[i] = Math.cos(angle) * distance;
            offsetY[i] = Math.sin(angle) * distance;
            puffRadius[i] = maxRadius * (0.18 + Math.random() * 0.16);
        }
    }

    @Override
    public void update() {
        lifetime--;
        if (lifetime <= 0) {
            ObjectManager.list.remove(this);
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        double progress = 1.0 - lifetime / (double) MAX_LIFETIME;
        float alpha = (float) Math.max(0.0, 1.0 - progress);
        Composite oldComposite = g2d.getComposite();

        for (int i = 0; i < PUFF_COUNT; i++) {
            double growth = 0.65 + progress * 0.9;
            int radius = (int) Math.round(puffRadius[i] * growth);
            int px = (int) Math.round(x + offsetX[i] * (0.45 + progress * 0.85) - radius);
            int py = (int) Math.round(y + offsetY[i] * (0.45 + progress * 0.85) - radius);
            int size = radius * 2;

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.40f));
            g2d.setColor(new Color(255, 170, 106));
            g2d.fillOval(px, py, size, size);

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.28f));
            g2d.setColor(new Color(82, 82, 88));
            g2d.fillOval(px - 3, py + 4, size + 6, size + 6);

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.18f));
            g2d.setColor(new Color(210, 214, 224));
            g2d.fillOval(px + size / 5, py + size / 5, (int) Math.round(size * 0.55), (int) Math.round(size * 0.55));
        }

        g2d.setComposite(oldComposite);
    }
}
