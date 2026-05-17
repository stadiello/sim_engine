package gameController;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class GameKeyController implements KeyEventDispatcher, MouseMotionListener, MouseListener {
    
    private boolean up, down, left, right;
    private volatile boolean leftClickPressed;
    private volatile int mouseX = 400;
    private volatile int mouseY = 300;

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
        }
        return false; // Ne pas consommer l'événement
    }

    public boolean isUp() { return up; }
    public boolean isDown() { return down; }
    public boolean isLeft() { return left; }
    public boolean isRight() { return right; }
    public boolean isLeftClickPressed() { return leftClickPressed; }

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
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            leftClickPressed = false;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}
}
