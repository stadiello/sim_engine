package object;

import java.awt.Graphics;

import world.TileManager;

public abstract class GameObject {

    public double x, y;
    public double vx, vy;

    protected GameObject(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public abstract void update();
    public abstract void draw(Graphics g);

    public void onDeath() {}

    protected boolean canMoveTo(double nextX, double nextY, double radius) {
        TileManager tileManager = ObjectManager.getTileManager();
        if (tileManager == null) {
            return true;
        }

        return !tileManager.isBlockedAtPixel(nextX - radius, nextY - radius)
                && !tileManager.isBlockedAtPixel(nextX + radius, nextY - radius)
                && !tileManager.isBlockedAtPixel(nextX - radius, nextY + radius)
                && !tileManager.isBlockedAtPixel(nextX + radius, nextY + radius);
    }

    protected void moveWithTileCollision(double radius) {
        double nextX = x + vx;
        if (canMoveTo(nextX, y, radius)) {
            x = nextX;
        } else {
            vx = -vx;
        }

        double nextY = y + vy;
        if (canMoveTo(x, nextY, radius)) {
            y = nextY;
        } else {
            vy = -vy;
        }
    }
    
}
