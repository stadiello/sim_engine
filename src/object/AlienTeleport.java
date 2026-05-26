package object;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class AlienTeleport extends GameObject {

    private static final int TELEPORT_MIN_FRAMES = 36;
    private static final int TELEPORT_MAX_FRAMES = 58;

    private final int durationFrames;
    private int elapsedFrames;

    public AlienTeleport(double x, double y) {
        super(x, y);
        durationFrames = TELEPORT_MIN_FRAMES
                + (int) (Math.random() * (TELEPORT_MAX_FRAMES - TELEPORT_MIN_FRAMES + 1));
    }

    @Override
    public void update() {
        elapsedFrames++;
        if (elapsedFrames < durationFrames) {
            return;
        }

        ObjectManager.list.add(new Alien(x, y));
        ObjectManager.list.remove(this);
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        double progress = Math.min(1.0, elapsedFrames / (double) durationFrames);
        int beamHeight = (int) Math.round(92 + 86 * (1.0 - progress));
        int haloRadius = (int) Math.round(10 + 26 * progress);

        var oldStroke = g2d.getStroke();
        Color beamCore = new Color(102, 255, 172, 150);
        Color beamAura = new Color(48, 220, 126, 88);

        g2d.setColor(beamAura);
        g2d.setStroke(new BasicStroke(18f));
        g2d.drawLine((int) Math.round(x), (int) Math.round(y - beamHeight), (int) Math.round(x), (int) Math.round(y + 4));

        g2d.setColor(beamCore);
        g2d.setStroke(new BasicStroke(8f));
        g2d.drawLine((int) Math.round(x), (int) Math.round(y - beamHeight + 2), (int) Math.round(x), (int) Math.round(y + 3));

        g2d.setStroke(new BasicStroke(2f));
        g2d.setColor(new Color(120, 255, 186, 190));
        g2d.drawOval((int) Math.round(x - haloRadius), (int) Math.round(y - haloRadius), haloRadius * 2, haloRadius * 2);

        int outer = haloRadius + 8;
        g2d.setColor(new Color(110, 255, 160, 110));
        g2d.drawOval((int) Math.round(x - outer), (int) Math.round(y - outer), outer * 2, outer * 2);

        g2d.setStroke(oldStroke);
    }
}
