package object;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

import gameController.PlayerInput;
import object.weapon.Weapon;

/** Tourelle alliée fixe réservée à la position fortifiée du Désert tactique. */
public final class DesertTurret extends GameObject {
    private static final double USE_RADIUS = 76.0;
    private static final int FIRE_INTERVAL_FRAMES = 7;
    private static final int RELOAD_FRAMES = 75;

    private final Weapon weapon = Weapon.carabine();
    private Protagonist operator;
    private double facingX = -1;
    private double facingY = 0;
    private int fireCooldown;
    private int reloadTimer;
    private int muzzleFlash;

    public DesertTurret(double x, double y) {
        super(x, y);
    }

    @Override
    public void update() {
        if (fireCooldown > 0) fireCooldown--;
        if (muzzleFlash > 0) muzzleFlash--;
        if (reloadTimer > 0 && --reloadTimer == 0) weapon.reload();

        if (operator == null || !ObjectManager.list.contains(operator)) {
            releaseOperator();
            return;
        }

        PlayerInput input = operator.getPlayerInput();
        double aimX = input.getMouseX() + operator.getInputCameraX();
        double aimY = input.getMouseY() + operator.getInputCameraY();
        double dx = aimX - x;
        double dy = aimY - y;
        double distance = Math.hypot(dx, dy);
        if (distance > 0.001) {
            facingX = dx / distance;
            facingY = dy / distance;
        }

        boolean fireTriggered = input.consumeLeftClickPressed();
        boolean fireRequested = input.isLeftClickPressed() || fireTriggered;
        if (!fireRequested || reloadTimer > 0 || fireCooldown > 0) return;
        if (weapon.getAmmoInMagazine() <= 0) {
            if (weapon.canReload()) reloadTimer = RELOAD_FRAMES;
            return;
        }

        if (weapon.fire(operator, x, y, facingX, facingY)) {
            fireCooldown = FIRE_INTERVAL_FRAMES;
            muzzleFlash = 3;
        }
    }

    public boolean tryToggleOperator(Protagonist player) {
        if (player == null) return false;
        if (operator == player) {
            releaseOperator();
            return true;
        }
        if (operator != null) return false;
        double dx = player.x - x;
        double dy = player.y - y;
        if (dx * dx + dy * dy > USE_RADIUS * USE_RADIUS) return false;
        operator = player;
        operator.setControlsEnabled(false);
        return true;
    }

    public boolean isOperatedBy(Protagonist player) {
        return operator == player;
    }

    public double getFacingX() {
        return facingX;
    }

    public double getFacingY() {
        return facingY;
    }

    public void applyNetworkPose(double networkFacingX, double networkFacingY) {
        facingX = networkFacingX;
        facingY = networkFacingY;
    }

    private void releaseOperator() {
        if (operator != null) operator.setControlsEnabled(true);
        operator = null;
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        Object oldAntialias = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        AffineTransform oldTransform = g2d.getTransform();
        var oldStroke = g2d.getStroke();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(new Color(30, 25, 20, 95));
        g2d.fillOval((int) x - 31, (int) y - 22, 62, 44);
        g2d.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(new Color(76, 70, 59));
        g2d.drawLine((int) x, (int) y + 2, (int) x - 25, (int) y + 20);
        g2d.drawLine((int) x, (int) y + 2, (int) x + 25, (int) y + 20);
        g2d.drawLine((int) x, (int) y + 2, (int) x, (int) y - 24);
        g2d.setColor(new Color(115, 108, 91));
        g2d.fillOval((int) x - 18, (int) y - 14, 36, 28);
        g2d.setColor(new Color(183, 165, 125));
        g2d.drawOval((int) x - 18, (int) y - 14, 36, 28);

        g2d.rotate(Math.atan2(facingY, facingX), x, y);
        g2d.setColor(new Color(49, 55, 53));
        g2d.fillRoundRect((int) x - 8, (int) y - 9, 35, 18, 7, 7);
        g2d.setColor(new Color(103, 116, 105));
        g2d.fillRoundRect((int) x + 12, (int) y - 4, 38, 8, 4, 4);
        g2d.setColor(new Color(31, 35, 34));
        g2d.fillRect((int) x + 42, (int) y - 3, 13, 6);

        Polygon shield = new Polygon(
                new int[]{(int) x - 5, (int) x + 13, (int) x + 13, (int) x - 5},
                new int[]{(int) y - 18, (int) y - 12, (int) y + 12, (int) y + 18},
                4
        );
        g2d.setColor(new Color(93, 91, 76));
        g2d.fillPolygon(shield);
        g2d.setColor(new Color(197, 171, 112));
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawPolygon(shield);
        g2d.setColor(new Color(56, 172, 105));
        g2d.fillOval((int) x + 1, (int) y - 3, 6, 6);

        if (muzzleFlash > 0) {
            g2d.setColor(new Color(255, 232, 160, 220));
            g2d.fillOval((int) x + 49, (int) y - 7, 17, 14);
            g2d.setColor(new Color(255, 151, 70));
            g2d.fillOval((int) x + 52, (int) y - 4, 10, 8);
        }

        g2d.setTransform(oldTransform);
        g2d.setStroke(oldStroke);
        if (oldAntialias != null) g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialias);

        if (operator == null && !hasNearbyPlayer()) return;
        g2d.setColor(operator == null ? new Color(235, 225, 185) : new Color(110, 235, 155));
        g2d.setFont(g2d.getFont().deriveFont(java.awt.Font.BOLD, 11f));
        String label = operator == null ? "E - UTILISER" : "TOURELLE ACTIVE - E POUR QUITTER";
        java.awt.FontMetrics metrics = g2d.getFontMetrics();
        g2d.drawString(label, (int) x - metrics.stringWidth(label) / 2, (int) y + 42);
    }

    private boolean hasNearbyPlayer() {
        for (Homme human : ObjectManager.getLivingHumans()) {
            if (!(human instanceof Protagonist player)) continue;
            double dx = player.x - x;
            double dy = player.y - y;
            if (dx * dx + dy * dy <= USE_RADIUS * USE_RADIUS) return true;
        }
        return false;
    }
}
