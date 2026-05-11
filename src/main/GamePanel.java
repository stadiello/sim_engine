package main;

import javax.swing.*;
import java.awt.*;

import object.Alien;
import object.Homme;
import object.Soldat;
import object.ObjectManager;

public class GamePanel extends JPanel implements Runnable {
    public GamePanel() {
        // Cree une petite population initiale.
        for (int i = 0; i < 6; i++) {
            ObjectManager.list.add(new Homme(Math.random() * 780, Math.random() * 580));
        }
        for (int i = 0; i < 2; i++) {
            ObjectManager.list.add(new Soldat(Math.random() * 780, Math.random() * 580));
        }
        for (int i = 0; i < 5; i++) {
            ObjectManager.list.add(new object.Alien(Math.random() * 780, Math.random() * 580));
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

        // Affiche le compteur des entites encore en vie.
        int civils = 0;
        int soldats = 0;
        int aliens = 0;
        for (var obj : ObjectManager.list) {
            if (obj instanceof Homme && !(obj instanceof Soldat) && !(obj instanceof Alien)) civils++;
            else if (obj instanceof Soldat) soldats++;
            else if (obj instanceof Alien) aliens++;
        }

        g.setColor(Color.WHITE);
        g.drawString("Civils: " + civils, 10, 20);
        g.drawString("Soldats: " + soldats, 10, 40);
        g.drawString("Aliens: " + aliens, 10, 60);
        g.drawString("Casualties: " + (8 - civils - soldats), 10, 80);
    }
}
