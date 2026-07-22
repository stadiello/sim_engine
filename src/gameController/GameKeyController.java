package gameController;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.concurrent.atomic.AtomicInteger;

public class GameKeyController implements KeyEventDispatcher, MouseMotionListener, MouseListener, MouseWheelListener {
    
    private boolean up, down, left, right;
    private boolean sprint;
    private volatile boolean leftClickPressed;
    private volatile boolean leftClickTriggered;
    private volatile boolean rightClickTriggered;
    private volatile boolean pauseToggleTriggered;
    private volatile boolean interactTriggered;
    private volatile boolean reloadTriggered;
    private boolean pauseKeyPressed;
    private boolean interactKeyPressed;
    private boolean reloadKeyPressed;
    private volatile int mouseX = 400;
    private volatile int mouseY = 300;
    private volatile int rightClickX = 400;
    private volatile int rightClickY = 300;
    private final AtomicInteger weaponScrollDelta = new AtomicInteger(0);

    @Override
    public boolean dispatchKeyEvent(java.awt.event.KeyEvent e) {
        if (e.getID() != KeyEvent.KEY_PRESSED && e.getID() != KeyEvent.KEY_RELEASED) {
            return false;
        }

        boolean pressed = e.getID() == KeyEvent.KEY_PRESSED;
        switch (e.getKeyCode()) {
            // Haut: fleche haut, Z (AZERTY), W (QWERTY)
            case KeyEvent.VK_UP, KeyEvent.VK_Z, KeyEvent.VK_W -> up = pressed;
            // Bas: fleche bas, S
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> down = pressed;
            // Gauche: fleche gauche, Q (AZERTY), A (QWERTY)
            case KeyEvent.VK_LEFT, KeyEvent.VK_Q, KeyEvent.VK_A -> left = pressed;
            // Droite: fleche droite, D
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> right = pressed;
            case KeyEvent.VK_SHIFT -> sprint = pressed;
            case KeyEvent.VK_P -> {
                if (pressed) {
                    if (!pauseKeyPressed) {
                        pauseToggleTriggered = true;
                    }
                    pauseKeyPressed = true;
                } else {
                    pauseKeyPressed = false;
                }
            }
            case KeyEvent.VK_E -> {
                if (pressed) {
                    if (!interactKeyPressed) {
                        interactTriggered = true;
                    }
                    interactKeyPressed = true;
                } else {
                    interactKeyPressed = false;
                }
            }
            case KeyEvent.VK_R -> {
                if (pressed) {
                    if (!reloadKeyPressed) {
                        reloadTriggered = true;
                    }
                    reloadKeyPressed = true;
                } else {
                    reloadKeyPressed = false;
                }
            }
        }
        return false; // Ne pas consommer l'événement
    }

    public boolean isUp() { return up; }
    public boolean isDown() { return down; }
    public boolean isLeft() { return left; }
    public boolean isRight() { return right; }
    public boolean isSprint() { return sprint; }
    public boolean isLeftClickPressed() { return leftClickPressed; }
    public boolean consumeLeftClickPressed() {
        boolean wasTriggered = leftClickTriggered;
        leftClickTriggered = false;
        return wasTriggered;
    }
    public boolean consumeRightClickTriggered() {
        boolean wasTriggered = rightClickTriggered;
        rightClickTriggered = false;
        return wasTriggered;
    }
    public int getRightClickX() { return rightClickX; }
    public int getRightClickY() { return rightClickY; }
    public boolean consumePauseToggleTriggered() {
        boolean wasTriggered = pauseToggleTriggered;
        pauseToggleTriggered = false;
        return wasTriggered;
    }
    public boolean consumeInteractTriggered() {
        boolean wasTriggered = interactTriggered;
        interactTriggered = false;
        return wasTriggered;
    }
    public boolean consumeReloadTriggered() {
        boolean wasTriggered = reloadTriggered;
        reloadTriggered = false;
        return wasTriggered;
    }
    public int consumeWeaponScrollDelta() { return weaponScrollDelta.getAndSet(0); }

    public int getMouseX() { return mouseX; }
    public int getMouseY() { return mouseY; }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            leftClickPressed = true;
            leftClickTriggered = true;
        } else if (e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger()) {
            rightClickX = e.getX();
            rightClickY = e.getY();
            rightClickTriggered = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            leftClickPressed = false;
        } else if (e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger()) {
            rightClickX = e.getX();
            rightClickY = e.getY();
            rightClickTriggered = true;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        weaponScrollDelta.addAndGet(e.getWheelRotation());
    }
}
