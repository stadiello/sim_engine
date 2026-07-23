package object;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import main.GamePanel;
import object.ai.TacticalMovement;
import world.TileManager;

/**
 * Décharge instantanée qui accroche une cible dans l'axe, puis saute vers les
 * hostiles voisins. L'entité reste quelques frames uniquement pour le rendu.
 */
public final class TeslaArc extends GameObject {
    private static final double PRIMARY_RANGE = 520.0;
    private static final double PRIMARY_LOCK_WIDTH = 78.0;
    private static final double CHAIN_RANGE = 220.0;
    private static final int MAX_TARGETS = 5;
    private static final int MAX_LIFETIME = 14;

    private final ArrayList<double[]> points = new ArrayList<>();
    private int lifetime = MAX_LIFETIME;

    public TeslaArc(double originX, double originY, double dirX, double dirY, Homme shooter) {
        super(originX, originY);
        double length = Math.max(0.0001, Math.hypot(dirX, dirY));
        double normX = dirX / length;
        double normY = dirY / length;
        points.add(new double[]{originX, originY});

        Set<Homme> struck = new HashSet<>();
        Homme first = findPrimaryTarget(originX, originY, normX, normY, shooter);
        if (first == null) {
            points.add(findBeamEnd(originX, originY, normX, normY));
            return;
        }

        Homme current = first;
        while (current != null && struck.size() < MAX_TARGETS) {
            points.add(new double[]{current.x, current.y});
            struck.add(current);
            eliminate(current);
            current = findChainTarget(current.x, current.y, shooter, struck);
        }
    }

    private Homme findPrimaryTarget(double originX, double originY, double dirX, double dirY, Homme shooter) {
        Homme best = null;
        double bestScore = Double.MAX_VALUE;
        for (Homme candidate : ObjectManager.getLivingHumans()) {
            if (!isHostile(shooter, candidate)) continue;
            double dx = candidate.x - originX;
            double dy = candidate.y - originY;
            double forward = dx * dirX + dy * dirY;
            if (forward <= 0 || forward > PRIMARY_RANGE) continue;
            double side = Math.abs(dx * -dirY + dy * dirX);
            if (side > PRIMARY_LOCK_WIDTH) continue;
            if (!TacticalMovement.hasLineOfSight(originX, originY, candidate.x, candidate.y)) continue;
            double score = forward + side * 3.5;
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private Homme findChainTarget(double fromX, double fromY, Homme shooter, Set<Homme> struck) {
        Homme best = null;
        double bestDistanceSq = CHAIN_RANGE * CHAIN_RANGE;
        for (Homme candidate : ObjectManager.getLivingHumans()) {
            if (struck.contains(candidate) || !isHostile(shooter, candidate)) continue;
            double dx = candidate.x - fromX;
            double dy = candidate.y - fromY;
            double distanceSq = dx * dx + dy * dy;
            if (distanceSq >= bestDistanceSq) continue;
            if (!TacticalMovement.hasLineOfSight(fromX, fromY, candidate.x, candidate.y)) continue;
            bestDistanceSq = distanceSq;
            best = candidate;
        }
        return best;
    }

    private double[] findBeamEnd(double originX, double originY, double dirX, double dirY) {
        TileManager tiles = ObjectManager.getTileManager();
        double distance = PRIMARY_RANGE;
        if (tiles != null) {
            for (double step = 18; step <= PRIMARY_RANGE; step += 18) {
                if (tiles.isBlockedAtPixel(originX + dirX * step, originY + dirY * step)) {
                    distance = Math.max(18, step - 18);
                    break;
                }
            }
        }
        return new double[]{originX + dirX * distance, originY + dirY * distance};
    }

    private boolean isHostile(Homme shooter, Homme candidate) {
        if (candidate == shooter) return false;
        boolean shooterHostile = shooter instanceof Ennemi || shooter instanceof Alien;
        boolean candidateHostile = candidate instanceof Ennemi || candidate instanceof Alien;
        return shooterHostile != candidateHostile;
    }

    private void eliminate(Homme target) {
        ObjectManager.list.remove(target);
        target.onDeath();
        ObjectManager.list.add(new ImpactSpark(
                target.x, target.y, 0, -1, 2.2,
                new Color(225, 255, 255), new Color(66, 154, 255)
        ));
        GamePanel.score += 15;
    }

    @Override
    public void update() {
        lifetime--;
        if (lifetime <= 0) ObjectManager.list.remove(this);
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        Object oldAntialias = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Composite oldComposite = g2d.getComposite();
        var oldStroke = g2d.getStroke();
        float alpha = Math.max(0f, lifetime / (float) MAX_LIFETIME);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int i = 1; i < points.size(); i++) {
            double[] start = points.get(i - 1);
            double[] end = points.get(i);
            drawBolt(g2d, start[0], start[1], end[0], end[1], i, alpha);
        }

        g2d.setStroke(oldStroke);
        g2d.setComposite(oldComposite);
        if (oldAntialias != null) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialias);
        }
    }

    private void drawBolt(Graphics2D g2d, double startX, double startY, double endX, double endY,
                          int segmentIndex, float alpha) {
        int pieces = Math.max(5, (int) (Math.hypot(endX - startX, endY - startY) / 34.0));
        double dx = endX - startX;
        double dy = endY - startY;
        double length = Math.max(1.0, Math.hypot(dx, dy));
        double perpX = -dy / length;
        double perpY = dx / length;

        int[] xs = new int[pieces + 1];
        int[] ys = new int[pieces + 1];
        for (int i = 0; i <= pieces; i++) {
            double t = i / (double) pieces;
            double jitter = (i == 0 || i == pieces) ? 0
                    : Math.sin(i * 7.13 + lifetime * 2.7 + segmentIndex * 3.1) * (7 + segmentIndex);
            xs[i] = (int) Math.round(startX + dx * t + perpX * jitter);
            ys[i] = (int) Math.round(startY + dy * t + perpY * jitter);
        }

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.28f));
        g2d.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(new Color(42, 118, 255));
        g2d.drawPolyline(xs, ys, xs.length);

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(new Color(90, 205, 255));
        g2d.drawPolyline(xs, ys, xs.length);
        g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(Color.WHITE);
        g2d.drawPolyline(xs, ys, xs.length);
    }
}
