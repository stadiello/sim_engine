package object;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class DroppedArmor extends GameObject {

    private static final double PICKUP_RADIUS = 34.0;
    private static final int LIFETIME_FRAMES = 60 * 20;
    private static final int BLINK_START_FRAMES = 60 * 4;

    private int remainingFrames = LIFETIME_FRAMES;

    public DroppedArmor(double x, double y) {
        super(x, y);
    }

    @Override
    public void update() {
        remainingFrames--;
        if (remainingFrames <= 0) {
            ObjectManager.list.remove(this);
            return;
        }

        for (GameObject obj : new java.util.ArrayList<>(ObjectManager.list)) {
            if (!(obj instanceof Protagonist protagonist)) {
                continue;
            }

            double dx = protagonist.x - x;
            double dy = protagonist.y - y;
            if (dx * dx + dy * dy > PICKUP_RADIUS * PICKUP_RADIUS) {
                continue;
            }

            if (protagonist.addArmorPlate()) {
                ObjectManager.list.remove(this);
            }
            return;
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        boolean blinking = remainingFrames <= BLINK_START_FRAMES && (remainingFrames / 8) % 2 == 0;
        float alpha = blinking ? 0.42f : 0.85f;

        java.awt.Composite oldComposite = g2d.getComposite();
        g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));

        g2d.setColor(new Color(90, 170, 255, 75));
        g2d.fillOval((int) x - 14, (int) y - 14, 28, 28);
        g2d.setColor(new Color(130, 210, 255, 180));
        g2d.setStroke(new BasicStroke(1.6f));
        g2d.drawOval((int) x - 14, (int) y - 14, 28, 28);

        g2d.setColor(new Color(68, 118, 175));
        g2d.fillRoundRect((int) x - 7, (int) y - 9, 14, 18, 4, 4);
        g2d.setColor(new Color(175, 230, 255));
        g2d.drawRoundRect((int) x - 7, (int) y - 9, 14, 18, 4, 4);
        g2d.setColor(new Color(210, 240, 255));
        g2d.drawLine((int) x, (int) y - 8, (int) x, (int) y + 8);

        g2d.setComposite(oldComposite);

        g2d.setColor(Color.WHITE);
        g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 10f));
        FontMetrics fm = g2d.getFontMetrics();
        String label = "Gilet";
        g2d.drawString(label, (int) x - fm.stringWidth(label) / 2, (int) y + 22);
    }
}
