package object;

import java.util.ArrayList;
import java.awt.Graphics;

import world.TileManager;

public class ObjectManager {

    // une seule liste pour TOUTES les entités
    public static ArrayList<GameObject> list = new ArrayList<>();
    private static TileManager tileManager;

    public static void setTileManager(TileManager manager) {
        tileManager = manager;
    }

    public static TileManager getTileManager() {
        return tileManager;
    }

    public static Alien getNearestAlien(double x, double y) {
        Alien nearest = null;
        double minDist = Double.MAX_VALUE;
        for (GameObject obj : list) {
            if (obj instanceof Alien alien) {
                double dx = alien.x - x;
                double dy = alien.y - y;
                double dist = dx * dx + dy * dy; // distance au carré pour éviter la racine carrée
                if (dist < minDist) {
                    minDist = dist;
                    nearest = alien;
                }
            }
        }
        return nearest;
    }

    public static void updateAll() {
        // on crée une copie de la liste pour éviter les problèmes de modification pendant l'itération (surtout avec les projectiles qui se suppriment eux-mêmes)
        for (GameObject obj : new ArrayList<>(list)) {
            obj.update(); // appelle Homme.update() ou Animal.update() automatiquement
        }
    }

    public static void drawAll(Graphics g) {
        // la même chose pour le dessin, chaque objet sait comment se dessiner lui-même et on évite les problèmes de modification de la liste pendant le dessin
        for (GameObject obj : new ArrayList<>(list)) {
            obj.draw(g); // chaque objet sait comment se dessiner
        }
    }
}