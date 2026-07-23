package gameController;

import java.util.concurrent.atomic.AtomicInteger;

public final class RemotePlayerInput implements PlayerInput {
    private volatile boolean up;
    private volatile boolean down;
    private volatile boolean left;
    private volatile boolean right;
    private volatile boolean sprint;
    private volatile boolean fireHeld;
    private volatile int mouseX = 400;
    private volatile int mouseY = 300;
    private volatile int viewWidth = 800;
    private volatile int viewHeight = 600;
    private final AtomicInteger fireTriggers = new AtomicInteger();
    private final AtomicInteger reloadTriggers = new AtomicInteger();
    private final AtomicInteger interactTriggers = new AtomicInteger();
    private final AtomicInteger weaponScroll = new AtomicInteger();

    public void apply(boolean up, boolean down, boolean left, boolean right, boolean sprint,
                      boolean fireHeld, boolean fireTriggered, boolean reloadTriggered,
                      boolean interactTriggered, int mouseX, int mouseY, int scrollDelta,
                      int viewWidth, int viewHeight) {
        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;
        this.sprint = sprint;
        this.fireHeld = fireHeld;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.viewWidth = Math.max(1, viewWidth);
        this.viewHeight = Math.max(1, viewHeight);
        if (fireTriggered) fireTriggers.incrementAndGet();
        if (reloadTriggered) reloadTriggers.incrementAndGet();
        if (interactTriggered) interactTriggers.incrementAndGet();
        if (scrollDelta != 0) weaponScroll.addAndGet(scrollDelta);
    }

    private boolean consume(AtomicInteger counter) {
        while (true) {
            int value = counter.get();
            if (value <= 0) return false;
            if (counter.compareAndSet(value, value - 1)) return true;
        }
    }

    public boolean isUp() { return up; }
    public boolean isDown() { return down; }
    public boolean isLeft() { return left; }
    public boolean isRight() { return right; }
    public boolean isSprint() { return sprint; }
    public boolean isLeftClickPressed() { return fireHeld; }
    public boolean consumeLeftClickPressed() { return consume(fireTriggers); }
    public boolean consumeRightClickTriggered() { return false; }
    public int getRightClickX() { return mouseX; }
    public int getRightClickY() { return mouseY; }
    public boolean consumePauseToggleTriggered() { return false; }
    public boolean consumeInteractTriggered() { return consume(interactTriggers); }
    public boolean consumeReloadTriggered() { return consume(reloadTriggers); }
    public int consumeWeaponScrollDelta() { return weaponScroll.getAndSet(0); }
    public int getMouseX() { return mouseX; }
    public int getMouseY() { return mouseY; }
    public int getViewWidth() { return viewWidth; }
    public int getViewHeight() { return viewHeight; }
}
