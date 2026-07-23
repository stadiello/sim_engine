package object;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.IdentityHashMap;
import java.util.Map;

/** Caisse permanente qui recharge les réserves, avec un délai propre à chaque joueur. */
public final class AmmoDepot extends GameObject {
    private static final double USE_RADIUS = 48.0;
    private static final int PLAYER_COOLDOWN_FRAMES = 60 * 6;
    private final Map<Protagonist, Integer> cooldowns = new IdentityHashMap<>();
    private int pulseFrames;

    public AmmoDepot(double x, double y) {
        super(x, y);
    }

    @Override
    public void update() {
        cooldowns.replaceAll((player, frames) -> Math.max(0, frames - 1));
        cooldowns.entrySet().removeIf(entry -> !ObjectManager.list.contains(entry.getKey()));
        if (pulseFrames > 0) pulseFrames--;

        for (GameObject object : new java.util.ArrayList<>(ObjectManager.list)) {
            if (!(object instanceof Protagonist player)) continue;
            if (cooldowns.getOrDefault(player, 0) > 0) continue;
            double dx = player.x - x;
            double dy = player.y - y;
            if (dx * dx + dy * dy > USE_RADIUS * USE_RADIUS) continue;
            if (player.restockAmmunition()) {
                cooldowns.put(player, PLAYER_COOLDOWN_FRAMES);
                pulseFrames = 18;
            }
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        Composite oldComposite = g2d.getComposite();
        var oldStroke = g2d.getStroke();
        double pulse = pulseFrames > 0 ? pulseFrames / 18.0 : 0.0;

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.18 + pulse * 0.35)));
        g2d.setColor(new Color(84, 180, 255));
        int halo = 36 + (int) Math.round(pulse * 10);
        g2d.fillOval((int) x - halo / 2, (int) y - halo / 2, halo, halo);
        g2d.setComposite(oldComposite);

        g2d.setColor(new Color(54, 66, 74));
        g2d.fillRoundRect((int) x - 18, (int) y - 14, 36, 28, 6, 6);
        g2d.setColor(new Color(118, 151, 166));
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRoundRect((int) x - 18, (int) y - 14, 36, 28, 6, 6);
        g2d.drawLine((int) x - 15, (int) y - 5, (int) x + 15, (int) y - 5);
        g2d.drawLine((int) x - 12, (int) y + 8, (int) x + 12, (int) y + 8);

        g2d.setColor(new Color(235, 198, 84));
        for (int i = -1; i <= 1; i++) {
            int bulletX = (int) x + i * 8;
            g2d.fillRoundRect(bulletX - 2, (int) y - 3, 4, 11, 3, 3);
        }

        g2d.setColor(new Color(220, 240, 255));
        g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 10f));
        FontMetrics metrics = g2d.getFontMetrics();
        String label = "DEPOT MUNITIONS";
        g2d.drawString(label, (int) x - metrics.stringWidth(label) / 2, (int) y + 28);
        g2d.setStroke(oldStroke);
    }
}
