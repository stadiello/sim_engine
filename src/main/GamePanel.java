package main;

import javax.swing.*;
import java.awt.*;

import object.Homme;
import object.Soldat;
import object.ObjectManager;

public class GamePanel extends JPanel implements Runnable {
    public GamePanel() {
        // Cree une petite population initiale.
        for (int i = 0; i < 10; i++) {
            ObjectManager.list.add(new Homme(Math.random() * 780, Math.random() * 580));
        }
        for (int i = 0; i < 5; i++) {
            ObjectManager.list.add(new Soldat(Math.random() * 780, Math.random() * 580));
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Simulation");
        GamePanel gamePanel = new GamePanel();
        frame.add(gamePanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setVisible(true);
        new Thread(gamePanel).start();
    }

    public void run() {
        while (true) {
            ObjectManager.updateAll();
            repaint();
            try {
                Thread.sleep(16);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        ObjectManager.drawAll(g);
    }
}
