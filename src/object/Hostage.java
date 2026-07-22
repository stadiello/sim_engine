package object;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import object.ai.TacticalMovement;

public class Hostage extends Homme {

    private static final double FOLLOW_SPEED = 2.1;
    private static final double FOLLOW_DISTANCE = 84.0;
    private static final double ARRIVAL_RADIUS = 18.0;
    private static final double ENTITY_RADIUS = 14.0;

    private boolean rescued;

    public Hostage(double x, double y) {
        super(x, y);
        vx = 0;
        vy = 0;
    }

    public boolean isRescued() {
        return rescued;
    }

    public void rescue() {
        rescued = true;
    }

    @Override
    public void update() {
        tickSuppression();

        if (!rescued) {
            vx = 0;
            vy = 0;
            return;
        }

        Protagonist protagonist = ObjectManager.getProtagonist();
        if (protagonist == null) {
            vx = 0;
            vy = 0;
            return;
        }

        double dx = protagonist.x - x;
        double dy = protagonist.y - y;
        double distanceSq = dx * dx + dy * dy;
        if (distanceSq <= ARRIVAL_RADIUS * ARRIVAL_RADIUS) {
            vx = 0;
            vy = 0;
            return;
        }

        double moveSpeed = FOLLOW_SPEED * getSuppressionMoveMultiplier();
        if (distanceSq > FOLLOW_DISTANCE * FOLLOW_DISTANCE) {
            moveSpeed *= 1.12;
        }

        double invDistance = 1.0 / Math.sqrt(distanceSq);
        double desiredVx = dx * invDistance * moveSpeed;
        double desiredVy = dy * invDistance * moveSpeed;
        double[] adjusted = TacticalMovement.adjustForObstacles(x, y, desiredVx, desiredVy, ENTITY_RADIUS);
        vx = adjusted[0];
        vy = adjusted[1];
        moveWithTileCollision(ENTITY_RADIUS);
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);

        Graphics2D g2d = (Graphics2D) g;
        var oldStroke = g2d.getStroke();

        if (!rescued) {
            g2d.setColor(new Color(210, 80, 80, 190));
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawLine((int) Math.round(x - 8), (int) Math.round(y - 2), (int) Math.round(x + 8), (int) Math.round(y - 2));
            g2d.drawLine((int) Math.round(x - 8), (int) Math.round(y + 3), (int) Math.round(x + 8), (int) Math.round(y + 3));
            g2d.drawOval((int) Math.round(x - 17), (int) Math.round(y - 17), 34, 34);
        } else {
            g2d.setColor(new Color(120, 255, 168, 190));
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawOval((int) Math.round(x - 16), (int) Math.round(y - 16), 32, 32);
        }

        g2d.setStroke(oldStroke);
    }
}