package object;

import java.awt.*;

public class ImpactSpark extends GameObject implements NetworkVisualState {

    private static final int PARTICLE_COUNT = 8;
    private static final int MAX_LIFETIME = 12;

    private final double[] px = new double[PARTICLE_COUNT];
    private final double[] py = new double[PARTICLE_COUNT];
    private final double[] pvx = new double[PARTICLE_COUNT];
    private final double[] pvy = new double[PARTICLE_COUNT];
    private Color coreColor;
    private Color emberColor;
    private double particleScale;
    private int lifetime = MAX_LIFETIME;

    public ImpactSpark(double x, double y) {
        this(x, y, 0.0, -1.0, 1.0, new Color(255, 210, 90), new Color(255, 160, 64));
    }

    public ImpactSpark(double x, double y, double dirX, double dirY, double intensity, Color coreColor, Color emberColor) {
        super(x, y);
        this.coreColor = coreColor;
        this.emberColor = emberColor;
        this.particleScale = Math.max(0.75, intensity);

        double dirLength = Math.hypot(dirX, dirY);
        double normDirX = dirLength <= 0.0001 ? 0.0 : dirX / dirLength;
        double normDirY = dirLength <= 0.0001 ? -1.0 : dirY / dirLength;

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double spread = (Math.random() - 0.5) * Math.PI * 0.9;
            double baseAngle = Math.atan2(normDirY, normDirX) + Math.PI + spread;
            double s = (1.6 + Math.random() * 2.8) * particleScale;
            px[i] = x;
            py[i] = y;
            pvx[i] = Math.cos(baseAngle) * s;
            pvy[i] = Math.sin(baseAngle) * s;
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
        g2d.setStroke(new BasicStroke((float) Math.max(1.4, particleScale * 1.3), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double tailX = px[i] - pvx[i] * 1.4;
            double tailY = py[i] - pvy[i] * 1.4;
            g2d.setColor(emberColor);
            g2d.drawLine((int) Math.round(tailX), (int) Math.round(tailY), (int) Math.round(px[i]), (int) Math.round(py[i]));
            g2d.setColor(coreColor);
            int size = (int) Math.max(3, Math.round(3 * particleScale));
            g2d.fillOval((int) Math.round(px[i]) - size / 2, (int) Math.round(py[i]) - size / 2, size, size);
        }

        g2d.setComposite(oldComposite);
    }

    @Override
    public double[] getNetworkVisualState() {
        double[] state = new double[4 + PARTICLE_COUNT * 4];
        state[0] = lifetime;
        state[1] = particleScale;
        state[2] = coreColor.getRGB();
        state[3] = emberColor.getRGB();
        int offset = 4;
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            state[offset++] = px[i];
            state[offset++] = py[i];
            state[offset++] = pvx[i];
            state[offset++] = pvy[i];
        }
        return state;
    }

    @Override
    public void applyNetworkVisualState(double[] state) {
        if (state == null || state.length != 4 + PARTICLE_COUNT * 4) return;
        lifetime = Math.max(0, Math.min(MAX_LIFETIME, (int) Math.round(state[0])));
        particleScale = Math.max(0.1, state[1]);
        coreColor = new Color((int) state[2], true);
        emberColor = new Color((int) state[3], true);
        int offset = 4;
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            px[i] = state[offset++];
            py[i] = state[offset++];
            pvx[i] = state[offset++];
            pvy[i] = state[offset++];
        }
    }
    
}
