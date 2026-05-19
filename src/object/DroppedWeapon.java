package object;

import java.awt.*;
import java.awt.geom.AffineTransform;

import object.weapon.Weapon;

public class DroppedWeapon extends GameObject {

    private static final double PICKUP_RADIUS = 35.0;
    private final Weapon weapon;

    public DroppedWeapon(double x, double y, Weapon weapon) {
        super(x, y);
        this.weapon = weapon;
        this.vx = 0;
        this.vy = 0;
    }

    @Override
    public void update() {
        for (GameObject obj : new java.util.ArrayList<>(ObjectManager.list)) {
            if (obj instanceof Protagonist protagonist) {
                double dx = protagonist.x - x;
                double dy = protagonist.y - y;
                if (dx * dx + dy * dy <= PICKUP_RADIUS * PICKUP_RADIUS) {
                    if (protagonist.addWeapon(weapon)) {
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

        // Halo jaune au sol pour indiquer le ramassage
        g2d.setColor(new Color(255, 220, 50, 90));
        g2d.fillOval((int) x - 14, (int) y - 14, 28, 28);
        g2d.setColor(new Color(255, 200, 30, 160));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawOval((int) x - 14, (int) y - 14, 28, 28);

        // Sprite de l'arme couché à plat (rotation -90°)
        Image sprite = weapon.getSprite();
        if (sprite != null) {
            AffineTransform old = g2d.getTransform();
            g2d.rotate(-Math.PI / 2, x, y);
            int w = 5;
            int h = 18;
            g2d.drawImage(sprite, (int) x - w / 2, (int) y - h / 2, w, h, null);
            g2d.setTransform(old);
        }

        // Nom de l'arme
        g2d.setColor(Color.WHITE);
        g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 10f));
        FontMetrics fm = g2d.getFontMetrics();
        String label = weapon.getName();
        g2d.drawString(label, (int) x - fm.stringWidth(label) / 2, (int) y + 22);
    }
}
