package object;

import java.awt.Graphics;

public abstract class GameObject {

    public double x, y;
    public double vx, vy;

    protected GameObject(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public abstract void update();
    public abstract void draw(Graphics g);
    
}
