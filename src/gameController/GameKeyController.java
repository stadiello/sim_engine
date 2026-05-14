package gameController;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;

import object.ObjectManager;
import object.Projectile;

public class GameKeyController implements KeyEventDispatcher {
    
    private boolean up, down, left, right, space;

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
            case KeyEvent.VK_SPACE -> space = pressed;
        }
        return false; // Ne pas consommer l'événement
    }

    public boolean isUp() { return up; }
    public boolean isDown() { return down; }
    public boolean isLeft() { return left; }
    public boolean isRight() { return right; }
    public boolean isSpace() { return space; }
}
