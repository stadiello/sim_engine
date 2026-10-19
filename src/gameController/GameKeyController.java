package gameController;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GameKeyController implements PlayerInput, KeyEventDispatcher, MouseMotionListener, MouseListener, MouseWheelListener {
    
    // Ecrits par l'EDT, lus par le thread de jeu et le thread reseau du client.
    private volatile boolean up, down, left, right;
    private volatile boolean sprint;
    private volatile boolean leftClickPressed;
    // Evenements ponctuels : getAndSet(false) evite de perdre un appui arrive entre la lecture et la remise a zero.
    private final AtomicBoolean leftClickTriggered = new AtomicBoolean();
    private final AtomicBoolean rightClickTriggered = new AtomicBoolean();
    private final AtomicBoolean pauseToggleTriggered = new AtomicBoolean();
    private final AtomicBoolean interactTriggered = new AtomicBoolean();
    private final AtomicBoolean reloadTriggered = new AtomicBoolean();
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
                        pauseToggleTriggered.set(true);
                    }
                    pauseKeyPressed = true;
                } else {
                    pauseKeyPressed = false;
                }
            }
            case KeyEvent.VK_E -> {
                if (pressed) {
                    if (!interactKeyPressed) {
                        interactTriggered.set(true);
                    }
                    interactKeyPressed = true;
                } else {
                    interactKeyPressed = false;
                }
            }
            case KeyEvent.VK_R -> {
                if (pressed) {
                    if (!reloadKeyPressed) {
                        reloadTriggered.set(true);
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
    public boolean consumeLeftClickPressed() { return leftClickTriggered.getAndSet(false); }
    public boolean consumeRightClickTriggered() { return rightClickTriggered.getAndSet(false); }
    public int getRightClickX() { return rightClickX; }
    public int getRightClickY() { return rightClickY; }
    public boolean consumePauseToggleTriggered() { return pauseToggleTriggered.getAndSet(false); }
    public boolean consumeInteractTriggered() { return interactTriggered.getAndSet(false); }
    public boolean consumeReloadTriggered() { return reloadTriggered.getAndSet(false); }
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
            leftClickTriggered.set(true);
        } else if (e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger()) {
            rightClickX = e.getX();
            rightClickY = e.getY();
            rightClickTriggered.set(true);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            leftClickPressed = false;
        } else if (e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger()) {
            rightClickX = e.getX();
            rightClickY = e.getY();
            rightClickTriggered.set(true);
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
