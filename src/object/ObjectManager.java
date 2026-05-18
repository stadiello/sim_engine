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

    public static Homme getNearestAlliedTarget(double x, double y) {
        Homme nearest = null;
        double minDist = Double.MAX_VALUE;

        for (GameObject obj : list) {
            if (obj instanceof Protagonist || obj instanceof Soldat) {
                Homme ally = (Homme) obj;
                double dx = ally.x - x;
                double dy = ally.y - y;
                double dist = dx * dx + dy * dy;
                if (dist < minDist) {
                    minDist = dist;
                    nearest = ally;
                }
            }
        }

        return nearest;
    }

    public static Homme getNearestHostileForSoldat(double x, double y) {
        Homme nearest = null;
        double minDist = Double.MAX_VALUE;

        for (GameObject obj : list) {
            if (obj instanceof Ennemi) {
                Homme hostile = (Homme) obj;
                double dx = hostile.x - x;
                double dy = hostile.y - y;
                double dist = dx * dx + dy * dy;
                if (dist < minDist) {
                    minDist = dist;
                    nearest = hostile;
                }
            }
        }

        return nearest;
    }

    public static Protagonist getProtagonist() {
        for (GameObject obj : list) {
            if (obj instanceof Protagonist protagonist) {
                return protagonist;
            }
        }

        return null;
    }

    public static void updateAll() {
        // on crée une copie de la liste pour éviter les problèmes de modification pendant l'itération (surtout avec les projectiles qui se suppriment eux-mêmes)
        for (GameObject obj : new ArrayList<>(list)) {
            obj.update(); // appelle Homme.update() ou Animal.update() automatiquement
        }
    }

    public static void drawAll(Graphics g) {
        // Dessine d'abord les douilles pour qu'elles restent sous les personnages.
        ArrayList<GameObject> snapshot = new ArrayList<>(list);
        for (GameObject obj : snapshot) {
            if (obj instanceof Douille) {
                obj.draw(g);
            }
        }

        // Dessine ensuite tout le reste.
        for (GameObject obj : snapshot) {
            if (!(obj instanceof Douille)) {
                obj.draw(g);
            }
        }
    }
}