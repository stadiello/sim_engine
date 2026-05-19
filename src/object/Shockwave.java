package object;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;

import main.GamePanel;

public class Shockwave extends GameObject {

    private static final double EXPANSION_PER_FRAME = 6.0;

    private final Homme source;
    private final double maxRadius;
    private double radius = 0.0;
    private double previousRadius = 0.0;

    public Shockwave(double x, double y, Homme source, double maxRadius) {
        super(x, y);
        this.source = source;
        this.maxRadius = maxRadius;
    }

    @Override
    public void update() {
        previousRadius = radius;
        radius = Math.min(maxRadius, radius + EXPANSION_PER_FRAME);

        ArrayList<Homme> victims = new ArrayList<>();
        double outerSq = radius * radius;
        double innerSq = previousRadius * previousRadius;

        for (Homme homme : ObjectManager.getLivingHumans()) {
            if (homme == source) {
                continue;
            }

            double dx = homme.x - x;
            double dy = homme.y - y;
            double distSq = dx * dx + dy * dy;
            if (distSq <= outerSq && distSq > innerSq) {
                victims.add(homme);
            }
        }

        for (Homme victim : victims) {
            ObjectManager.list.remove(victim);
            victim.onDeath();
            GamePanel.score += 10;
        }

        if (radius >= maxRadius) {
            ObjectManager.list.remove(this);
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        float alpha = (float) Math.max(0.0, 1.0 - (radius / maxRadius));
        Composite oldComposite = g2d.getComposite();
        var oldStroke = g2d.getStroke();

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.8f));
        g2d.setStroke(new BasicStroke(3f));
        g2d.setColor(new Color(255, 220, 140));

        int drawRadius = (int) Math.round(radius);
        int diameter = drawRadius * 2;
        g2d.drawOval((int) Math.round(x) - drawRadius, (int) Math.round(y) - drawRadius, diameter, diameter);

        g2d.setComposite(oldComposite);
        g2d.setStroke(oldStroke);
    }
}
