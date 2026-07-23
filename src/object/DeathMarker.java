package object;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import main.GamePanel;

/** Silhouette persistante au sol indiquant le type et la faction d'un mort. */
public final class DeathMarker extends GameObject {
    private static final double REVIVE_RADIUS = 68.0;
    private enum MarkerType {
        ALLY,
        HOSTILE,
        ALIEN,
        CIVILIAN
    }

    private final MarkerType type;
    private final double rotation;
    private final Homme fallenAlly;

    public DeathMarker(Homme victim) {
        super(victim.x, victim.y);
        if (victim instanceof Alien) {
            type = MarkerType.ALIEN;
            fallenAlly = null;
        } else if (victim instanceof Ennemi) {
            type = MarkerType.HOSTILE;
            fallenAlly = null;
        } else if (victim instanceof Soldat || victim instanceof Protagonist) {
            type = MarkerType.ALLY;
            fallenAlly = victim;
        } else {
            type = MarkerType.CIVILIAN;
            fallenAlly = null;
        }
        rotation = ((Double.doubleToLongBits(x * 31.0 + y * 17.0) & 1023L) / 1023.0) * Math.PI * 2.0;
    }

    @Override
    public void update() {
        // Le marqueur reste présent jusqu'à la prochaine partie.
    }

    @Override
    public void draw(Graphics g) {
        if (!GamePanel.areDeathMarkersEnabled()) return;

        Graphics2D g2d = (Graphics2D) g;
        AffineTransform oldTransform = g2d.getTransform();
        Composite oldComposite = g2d.getComposite();
        var oldStroke = g2d.getStroke();
        g2d.rotate(rotation, x, y);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.72f));

        if (type == MarkerType.ALIEN) {
            drawAlienMarker(g2d);
        } else {
            drawHumanMarker(g2d);
        }

        g2d.setStroke(oldStroke);
        g2d.setComposite(oldComposite);
        g2d.setTransform(oldTransform);

        if (fallenAlly != null && hasNearbyPlayer()) {
            g2d.setColor(new Color(125, 245, 170));
            g2d.setFont(g2d.getFont().deriveFont(java.awt.Font.BOLD, 11f));
            String label = "E - REANIMER";
            java.awt.FontMetrics metrics = g2d.getFontMetrics();
            g2d.drawString(label, (int) x - metrics.stringWidth(label) / 2, (int) y + 35);
        }
    }

    public boolean tryRevive(Protagonist rescuer) {
        if (fallenAlly == null || rescuer == null) return false;
        double dx = rescuer.x - x;
        double dy = rescuer.y - y;
        if (dx * dx + dy * dy > REVIVE_RADIUS * REVIVE_RADIUS) return false;

        if (fallenAlly instanceof Protagonist player) {
            player.x = x;
            player.y = y;
            player.vx = 0;
            player.vy = 0;
            player.setControlsEnabled(true);
            ObjectManager.list.add(player);
        } else {
            ObjectManager.list.add(new Soldat(x, y));
        }
        ObjectManager.list.remove(this);
        return true;
    }

    private boolean hasNearbyPlayer() {
        for (Homme human : ObjectManager.getLivingHumans()) {
            if (!(human instanceof Protagonist player)) continue;
            double dx = player.x - x;
            double dy = player.y - y;
            if (dx * dx + dy * dy <= REVIVE_RADIUS * REVIVE_RADIUS) return true;
        }
        return false;
    }

    private void drawHumanMarker(Graphics2D g2d) {
        Color fill = switch (type) {
            case ALLY -> new Color(46, 116, 168);
            case HOSTILE -> new Color(145, 52, 48);
            case CIVILIAN -> new Color(105, 112, 120);
            default -> Color.GRAY;
        };
        Color edge = switch (type) {
            case ALLY -> new Color(115, 206, 255);
            case HOSTILE -> new Color(255, 116, 92);
            case CIVILIAN -> new Color(188, 198, 207);
            default -> Color.LIGHT_GRAY;
        };

        g2d.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(new Color(10, 12, 15, 95));
        g2d.fillOval((int) x - 25, (int) y - 13, 50, 26);
        g2d.setColor(fill);
        g2d.fillOval((int) x - 22, (int) y - 8, 13, 13);
        g2d.fillRoundRect((int) x - 10, (int) y - 8, 25, 16, 9, 9);
        g2d.setColor(edge);
        g2d.drawOval((int) x - 22, (int) y - 8, 13, 13);
        g2d.drawRoundRect((int) x - 10, (int) y - 8, 25, 16, 9, 9);
        g2d.drawLine((int) x + 12, (int) y - 5, (int) x + 23, (int) y - 12);
        g2d.drawLine((int) x + 12, (int) y + 5, (int) x + 24, (int) y + 12);
        g2d.drawLine((int) x - 3, (int) y - 6, (int) x + 7, (int) y - 17);

        if (type == MarkerType.HOSTILE) {
            g2d.drawLine((int) x - 2, (int) y - 5, (int) x + 5, (int) y);
            g2d.drawLine((int) x + 5, (int) y, (int) x - 2, (int) y + 5);
        } else if (type == MarkerType.ALLY) {
            g2d.fillRect((int) x, (int) y - 2, 9, 4);
            g2d.fillRect((int) x + 3, (int) y - 5, 3, 10);
        }
    }

    private void drawAlienMarker(Graphics2D g2d) {
        g2d.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(new Color(8, 10, 14, 100));
        g2d.fillOval((int) x - 23, (int) y - 15, 46, 30);
        g2d.setColor(new Color(79, 53, 126));
        g2d.fillOval((int) x - 13, (int) y - 10, 26, 20);
        g2d.setColor(new Color(178, 126, 255));
        g2d.drawOval((int) x - 13, (int) y - 10, 26, 20);
        for (int side : new int[]{-1, 1}) {
            g2d.drawLine((int) x + side * 8, (int) y - 6, (int) x + side * 20, (int) y - 15);
            g2d.drawLine((int) x + side * 10, (int) y, (int) x + side * 23, (int) y);
            g2d.drawLine((int) x + side * 8, (int) y + 6, (int) x + side * 20, (int) y + 15);
        }
        g2d.setColor(new Color(220, 187, 255));
        g2d.fillOval((int) x - 7, (int) y - 3, 4, 4);
        g2d.fillOval((int) x + 3, (int) y - 3, 4, 4);
    }
}
