package object;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

import object.weapon.Weapon;

public class DroppedAmmo extends GameObject {

    private static final double PICKUP_RADIUS = 32.0;

    private final Weapon weapon;
    private final int reserveAmmo;

    public DroppedAmmo(double x, double y, Weapon weapon, int reserveAmmo) {
        super(x, y);
        this.weapon = weapon;
        this.reserveAmmo = Math.max(1, reserveAmmo);
    }

    @Override
    public void update() {
        for (GameObject obj : new java.util.ArrayList<>(ObjectManager.list)) {
            if (obj instanceof Protagonist protagonist) {
                double dx = protagonist.x - x;
                double dy = protagonist.y - y;
                if (dx * dx + dy * dy <= PICKUP_RADIUS * PICKUP_RADIUS) {
                    if (protagonist.addAmmoForWeapon(weapon, reserveAmmo)) {
                        ObjectManager.list.remove(this);
                    }
                    return;
                }
            }
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(new Color(88, 148, 255, 88));
        g2d.fillOval((int) x - 14, (int) y - 14, 28, 28);
        g2d.setColor(new Color(138, 196, 255, 180));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawOval((int) x - 14, (int) y - 14, 28, 28);

        g2d.setColor(new Color(40, 52, 66));
        g2d.fillRoundRect((int) x - 10, (int) y - 7, 20, 14, 4, 4);
        g2d.setColor(new Color(212, 228, 245));
        g2d.drawRoundRect((int) x - 10, (int) y - 7, 20, 14, 4, 4);
        g2d.drawLine((int) x - 4, (int) y - 4, (int) x - 4, (int) y + 4);
        g2d.drawLine((int) x, (int) y - 4, (int) x, (int) y + 4);
        g2d.drawLine((int) x + 4, (int) y - 4, (int) x + 4, (int) y + 4);

        g2d.setColor(Color.WHITE);
        g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 10f));
        FontMetrics fm = g2d.getFontMetrics();
        String label = "+" + reserveAmmo + " " + weapon.getName();
        g2d.drawString(label, (int) x - fm.stringWidth(label) / 2, (int) y + 22);
    }
}