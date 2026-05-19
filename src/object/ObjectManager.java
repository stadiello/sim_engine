package object;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import world.TileManager;

public class ObjectManager {

    // Liste centrale conservée pour compatibilité avec le reste du code.
    public static final ManagedObjectList list = new ManagedObjectList();
    private static TileManager tileManager;
    private static final ArrayList<Homme> livingHumans = new ArrayList<>();
    private static final ArrayList<Homme> alliedTargets = new ArrayList<>();
    private static final ArrayList<Homme> soldierHostiles = new ArrayList<>();
    private static final ArrayList<Alien> aliens = new ArrayList<>();
    private static final ArrayList<GameObject> pendingAdditions = new ArrayList<>();
    private static final ArrayList<GameObject> pendingRemovals = new ArrayList<>();
    private static final Set<GameObject> pendingRemovalSet = new HashSet<>();
    private static boolean updating;
    private static Protagonist protagonist;

    public static void setTileManager(TileManager manager) {
        tileManager = manager;
    }

    public static TileManager getTileManager() {
        return tileManager;
    }

    public static Iterable<Homme> getLivingHumans() {
        return livingHumans;
    }

    public static Alien getNearestAlien(double x, double y) {
        Alien nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Alien alien : aliens) {
            if (pendingRemovalSet.contains(alien)) {
                continue;
            }

            double dx = alien.x - x;
            double dy = alien.y - y;
            double dist = dx * dx + dy * dy; // distance au carré pour éviter la racine carrée
            if (dist < minDist) {
                minDist = dist;
                nearest = alien;
            }
        }
        return nearest;
    }

    public static Homme getNearestAlliedTarget(double x, double y) {
        Homme nearest = null;
        double minDist = Double.MAX_VALUE;

        for (Homme ally : alliedTargets) {
            if (pendingRemovalSet.contains(ally)) {
                continue;
            }

            double dx = ally.x - x;
            double dy = ally.y - y;
            double dist = dx * dx + dy * dy;
            if (dist < minDist) {
                minDist = dist;
                nearest = ally;
            }
        }

        return nearest;
    }

    public static Homme getNearestHostileForSoldat(double x, double y) {
        Homme nearest = null;
        double minDist = Double.MAX_VALUE;

        for (Homme hostile : soldierHostiles) {
            if (pendingRemovalSet.contains(hostile)) {
                continue;
            }

            double dx = hostile.x - x;
            double dy = hostile.y - y;
            double dist = dx * dx + dy * dy;
            if (dist < minDist) {
                minDist = dist;
                nearest = hostile;
            }
        }

        return nearest;
    }

    public static Protagonist getProtagonist() {
        return protagonist != null && !pendingRemovalSet.contains(protagonist) ? protagonist : null;
    }

    public static void updateAll() {
        updating = true;
        int count = list.size();
        for (int i = 0; i < count; i++) {
            GameObject obj = list.get(i);
            if (pendingRemovalSet.contains(obj)) {
                continue;
            }
            obj.update();
        }
        updating = false;
        flushPendingChanges();
    }

    public static void drawAll(Graphics g) {
        // Dessine d'abord les douilles pour qu'elles restent sous les personnages.
        for (GameObject obj : list) {
            if (pendingRemovalSet.contains(obj)) {
                continue;
            }
            if (obj instanceof Douille) {
                obj.draw(g);
            }
        }

        // Dessine ensuite tout le reste.
        for (GameObject obj : list) {
            if (pendingRemovalSet.contains(obj)) {
                continue;
            }
            if (!(obj instanceof Douille)) {
                obj.draw(g);
            }
        }
    }

    private static void addInternal(GameObject obj) {
        if (obj == null) {
            return;
        }

        list.directAdd(obj);
        registerObject(obj);
    }

    private static boolean removeInternal(GameObject obj) {
        if (obj == null) {
            return false;
        }

        boolean removed = list.directRemove(obj);
        if (removed) {
            unregisterObject(obj);
        }
        return removed;
    }

    private static void clearInternal() {
        list.directClear();
        livingHumans.clear();
        alliedTargets.clear();
        soldierHostiles.clear();
        aliens.clear();
        pendingAdditions.clear();
        pendingRemovals.clear();
        pendingRemovalSet.clear();
        protagonist = null;
    }

    private static void flushPendingChanges() {
        if (!pendingRemovals.isEmpty()) {
            for (GameObject obj : pendingRemovals) {
                removeInternal(obj);
            }
            pendingRemovals.clear();
            pendingRemovalSet.clear();
        }

        if (!pendingAdditions.isEmpty()) {
            for (GameObject obj : pendingAdditions) {
                addInternal(obj);
            }
            pendingAdditions.clear();
        }
    }

    private static void registerObject(GameObject obj) {
        if (obj instanceof Homme homme) {
            livingHumans.add(homme);

            if (obj instanceof Protagonist newProtagonist) {
                protagonist = newProtagonist;
                alliedTargets.add(homme);
            } else if (obj instanceof Soldat) {
                alliedTargets.add(homme);
            } else if (obj instanceof Ennemi) {
                soldierHostiles.add(homme);
            }
        }

        if (obj instanceof Alien alien) {
            aliens.add(alien);
        }
    }

    private static void unregisterObject(GameObject obj) {
        if (obj instanceof Homme homme) {
            livingHumans.remove(homme);

            if (obj instanceof Protagonist && protagonist == obj) {
                protagonist = null;
            }

            alliedTargets.remove(homme);
            soldierHostiles.remove(homme);
        }

        if (obj instanceof Alien alien) {
            aliens.remove(alien);
        }
    }

    public static final class ManagedObjectList extends ArrayList<GameObject> {
        @Override
        public boolean add(GameObject obj) {
            if (updating) {
                pendingAdditions.add(obj);
                return true;
            }

            addInternal(obj);
            return true;
        }

        @Override
        public void add(int index, GameObject element) {
            add(element);
        }

        @Override
        public boolean remove(Object obj) {
            if (!(obj instanceof GameObject gameObject)) {
                return false;
            }

            if (updating) {
                if (pendingRemovalSet.add(gameObject)) {
                    pendingRemovals.add(gameObject);
                }
                return true;
            }

            return removeInternal(gameObject);
        }

        @Override
        public GameObject remove(int index) {
            GameObject obj = get(index);
            remove(obj);
            return obj;
        }

        @Override
        public void clear() {
            clearInternal();
        }

        @Override
        public boolean addAll(java.util.Collection<? extends GameObject> collection) {
            boolean changed = false;
            for (GameObject obj : collection) {
                changed |= add(obj);
            }
            return changed;
        }

        @Override
        public boolean addAll(int index, java.util.Collection<? extends GameObject> collection) {
            return addAll(collection);
        }

        @Override
        public boolean removeAll(java.util.Collection<?> collection) {
            boolean changed = false;
            for (Object obj : collection) {
                changed |= remove(obj);
            }
            return changed;
        }

        @Override
        public boolean retainAll(java.util.Collection<?> collection) {
            ArrayList<GameObject> toRemove = new ArrayList<>();
            for (GameObject obj : this) {
                if (!collection.contains(obj)) {
                    toRemove.add(obj);
                }
            }
            return removeAll(toRemove);
        }

        private boolean directAdd(GameObject obj) {
            return super.add(obj);
        }

        private boolean directRemove(GameObject obj) {
            return super.remove(obj);
        }

        private void directClear() {
            super.clear();
        }
    }
}