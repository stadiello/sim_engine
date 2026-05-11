package object;

import java.util.ArrayList;
import java.awt.Graphics;

public class ObjectManager {

    // une seule liste pour TOUTES les entités
    public static ArrayList<GameObject> list = new ArrayList<>();

    public static void updateAll() {
        for (GameObject obj : list) {
            obj.update(); // appelle Homme.update() ou Animal.update() automatiquement
        }
    }

    public static void drawAll(Graphics g) {
        for (GameObject obj : list) {
            obj.draw(g); // chaque objet sait comment se dessiner
        }
    }
}