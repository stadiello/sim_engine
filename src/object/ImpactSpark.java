package object;

import java.awt.*;

public class ImpactSpark extends GameObject {

    private static final int PARTICLE_COUNT = 8;
    private static final int MAX_LIFETIME = 12;

    private final double[] px = new double[PARTICLE_COUNT];
    private final double[] py = new double[PARTICLE_COUNT];
    private final double[] pvx = new double[PARTICLE_COUNT];
    private final double[] pvy = new double[PARTICLE_COUNT];
    private int lifetime = MAX_LIFETIME;

    public ImpactSpark(double x, double y) {
        super(x, y);

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double a = Math.random() * Math.PI * 2;
            double s = 1.2 + Math.random() * 2.2;
            px[i] = x;
            py[i] = y;
            pvx[i] = Math.cos(a) * s;
            pvy[i] = Math.sin(a) * s;
        }
    }

    @Override
    public void update() {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            px[i] += pvx[i];
            py[i] += pvy[i];
            pvy[i] += 0.04;
        }

        lifetime--;
        if (lifetime <= 0) {
            ObjectManager.list.remove(this);
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        Composite oldComposite = g2d.getComposite();
        float alpha = Math.max(0f, (float) lifetime / MAX_LIFETIME);

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.setColor(new Color(255, 210, 90));
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            g2d.fillOval((int) px[i] - 2, (int) py[i] - 2, 4, 4);
        }

        g2d.setComposite(oldComposite);
    }
    
}
