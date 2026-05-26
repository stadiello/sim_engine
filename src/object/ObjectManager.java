package object;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import object.ai.TacticalMovement;
import world.TileManager;

public class ObjectManager {

    // Liste centrale conservée pour compatibilité avec le reste du code.
    public static final ManagedObjectList list = new ManagedObjectList();
    private static final Object LOCK = new Object();
    private static TileManager tileManager;
    private static final ArrayList<Homme> livingHumans = new ArrayList<>();
    private static final ArrayList<Homme> alliedTargets = new ArrayList<>();
    private static final ArrayList<Homme> soldierHostiles = new ArrayList<>();
    private static final ArrayList<Alien> aliens = new ArrayList<>();
    private static final ArrayList<GameObject> pendingAdditions = new ArrayList<>();
    private static final ArrayList<GameObject> pendingRemovals = new ArrayList<>();
    private static final Set<GameObject> pendingRemovalSet = new HashSet<>();
    private static final int ALIEN_HIVE_BREAK_MIN_FRAMES = 120;
    private static final int ALIEN_HIVE_BREAK_MAX_FRAMES = 180;
    private static boolean updating;
    private static Protagonist protagonist;
    private static boolean alienHiveActive;
    private static Homme alienHiveAggressor;
    private static Alien alienHiveAlpha;
    private static int alienHiveNoLosMajorityFrames;
    private static int alienHiveBreakThresholdFrames = 150;

    public static void setTileManager(TileManager manager) {
        tileManager = manager;
    }

    public static TileManager getTileManager() {
        return tileManager;
    }

    public static boolean isHumanAreaFree(double x, double y, double radius, GameObject... ignoredObjects) {
        synchronized (LOCK) {
            for (GameObject obj : list) {
                if (pendingRemovalSet.contains(obj)) {
                    continue;
                }

                if (isIgnoredObject(obj, ignoredObjects)) {
                    continue;
                }

                if (!(obj instanceof Homme homme)) {
                    continue;
                }

                double otherRadius = homme.getCollisionRadius();
                double allowedDistance = radius + otherRadius;
                double dx = homme.x - x;
                double dy = homme.y - y;
                if (dx * dx + dy * dy < allowedDistance * allowedDistance) {
                    return false;
                }
            }

            return true;
        }
    }

    public static ArrayList<Homme> getOverlappingHumans(double x, double y, double radius, GameObject... ignoredObjects) {
        synchronized (LOCK) {
            ArrayList<Homme> overlapping = new ArrayList<>();

            for (GameObject obj : list) {
                if (pendingRemovalSet.contains(obj) || isIgnoredObject(obj, ignoredObjects)) {
                    continue;
                }

                if (!(obj instanceof Homme homme)) {
                    continue;
                }

                double otherRadius = homme.getCollisionRadius();
                double allowedDistance = radius + otherRadius;
                double dx = homme.x - x;
                double dy = homme.y - y;
                if (dx * dx + dy * dy < allowedDistance * allowedDistance) {
                    overlapping.add(homme);
                }
            }

            return overlapping;
        }
    }

    private static boolean isIgnoredObject(GameObject obj, GameObject... ignoredObjects) {
        if (ignoredObjects == null) {
            return false;
        }

        for (GameObject ignoredObject : ignoredObjects) {
            if (obj == ignoredObject) {
                return true;
            }
        }

        return false;
    }

    public static Soldat getSoldat() {
        synchronized (LOCK) {
            for (GameObject obj : list) {
                if (obj instanceof Soldat soldat && !pendingRemovalSet.contains(soldat)) {
                    return soldat;
                }
            }
            return null;
        }
    }

    public static int[] getUiCounts() {
        synchronized (LOCK) {
            int civils = 0;
            int soldats = 0;
            int aliensCount = 0;

            for (GameObject obj : list) {
                if (pendingRemovalSet.contains(obj)) {
                    continue;
                }

                if (obj instanceof Homme && !(obj instanceof Soldat) && !(obj instanceof Alien)) {
                    civils++;
                } else if (obj instanceof Soldat) {
                    soldats++;
                } else if (obj instanceof Alien) {
                    aliensCount++;
                }
            }

            return new int[]{civils, soldats, aliensCount};
        }
    }

    public static Iterable<Homme> getLivingHumans() {
        return livingHumans;
    }

    public static void alertAliensToAggressor(Homme aggressor) {
        if (aggressor == null) {
            return;
        }

        synchronized (LOCK) {
            for (Alien alien : aliens) {
                if (pendingRemovalSet.contains(alien)) {
                    continue;
                }
                alien.setAggroTarget(aggressor);
            }
        }
    }

    public static void activateAlienHiveAggro(Homme aggressor, Alien alphaAlien) {
        if (aggressor == null) {
            return;
        }

        synchronized (LOCK) {
            alienHiveActive = true;
            alienHiveAggressor = aggressor;
            alienHiveAlpha = alphaAlien;
            alienHiveNoLosMajorityFrames = 0;
            alienHiveBreakThresholdFrames = ALIEN_HIVE_BREAK_MIN_FRAMES
                    + (int) (Math.random() * (ALIEN_HIVE_BREAK_MAX_FRAMES - ALIEN_HIVE_BREAK_MIN_FRAMES + 1));
            for (Alien alien : aliens) {
                if (pendingRemovalSet.contains(alien)) {
                    continue;
                }
                alien.setAggroTarget(aggressor);
            }
        }
    }

    public static void clearAlienAggro() {
        clearAlienAggro(true);
    }

    private static void clearAlienAggro(boolean resetHiveState) {
        synchronized (LOCK) {
            for (Alien alien : aliens) {
                if (pendingRemovalSet.contains(alien)) {
                    continue;
                }
                alien.clearAggroTarget();
            }

            if (resetHiveState) {
                alienHiveActive = false;
                alienHiveAggressor = null;
                alienHiveAlpha = null;
                alienHiveNoLosMajorityFrames = 0;
            }
        }
    }

    public static Alien getNearestAlien(double x, double y) {
        synchronized (LOCK) {
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
    }

    public static Homme getNearestAlliedTarget(double x, double y) {
        synchronized (LOCK) {
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
    }

    public static Homme getNearestHostileForSoldat(double x, double y) {
        synchronized (LOCK) {
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
    }

    public static Protagonist getProtagonist() {
        synchronized (LOCK) {
            return protagonist != null && !pendingRemovalSet.contains(protagonist) ? protagonist : null;
        }
    }

    public static void updateAll() {
        synchronized (LOCK) {
            updating = true;
            int count = list.size();
            for (int i = 0; i < count; i++) {
                GameObject obj = list.get(i);
                if (pendingRemovalSet.contains(obj)) {
                    continue;
                }
                obj.update();
            }
            updateAlienHiveState();
            updating = false;
            flushPendingChanges();
        }
    }

    private static void updateAlienHiveState() {
        if (!alienHiveActive) {
            return;
        }

        if (!isObjectAliveUnlocked(alienHiveAggressor)) {
            clearAlienAggro(false);
            alienHiveActive = false;
            alienHiveAlpha = null;
            alienHiveAggressor = null;
            alienHiveNoLosMajorityFrames = 0;
            return;
        }

        // Si l'alpha est elimine, on tente d'en designer un nouveau pour conserver la coordination.
        if (!isObjectAliveUnlocked(alienHiveAlpha)) {
            alienHiveAlpha = selectReplacementHiveAlpha();
            if (alienHiveAlpha == null) {
                clearAlienAggro(true);
                return;
            }
        }

        int totalAliens = 0;
        int aliensWithLos = 0;
        for (Alien alien : aliens) {
            if (pendingRemovalSet.contains(alien)) {
                continue;
            }

            totalAliens++;
            if (TacticalMovement.hasLineOfSight(alien.x, alien.y, alienHiveAggressor.x, alienHiveAggressor.y)) {
                aliensWithLos++;
            }
        }

        if (totalAliens <= 0) {
            clearAlienAggro(true);
            return;
        }

        if (aliensWithLos * 2 <= totalAliens) {
            alienHiveNoLosMajorityFrames++;
        } else {
            alienHiveNoLosMajorityFrames = 0;
        }

        // Sortie de ligne de vue de la majorite des aliens pendant 2-3 secondes.
        if (alienHiveNoLosMajorityFrames >= alienHiveBreakThresholdFrames) {
            clearAlienAggro(true);
        }
    }

    private static boolean isObjectAliveUnlocked(GameObject obj) {
        return obj != null && list.contains(obj) && !pendingRemovalSet.contains(obj);
    }

    private static Alien selectReplacementHiveAlpha() {
        Alien selected = null;
        double bestDistSq = Double.MAX_VALUE;

        for (Alien alien : aliens) {
            if (pendingRemovalSet.contains(alien)) {
                continue;
            }

            double distSq;
            if (alienHiveAggressor == null) {
                distSq = 0.0;
            } else {
                double dx = alien.x - alienHiveAggressor.x;
                double dy = alien.y - alienHiveAggressor.y;
                distSq = dx * dx + dy * dy;
            }

            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                selected = alien;
            }
        }

        return selected;
    }

    public static void drawAll(Graphics g) {
        ArrayList<GameObject> snapshot;
        HashSet<GameObject> removedSnapshot;
        synchronized (LOCK) {
            snapshot = new ArrayList<>(list);
            removedSnapshot = new HashSet<>(pendingRemovalSet);
        }

        // Dessine d'abord les douilles pour qu'elles restent sous les personnages.
        for (GameObject obj : snapshot) {
            if (removedSnapshot.contains(obj)) {
                continue;
            }
            if (obj instanceof Douille) {
                obj.draw(g);
            }
        }

        // Dessine ensuite tout le reste.
        for (GameObject obj : snapshot) {
            if (removedSnapshot.contains(obj)) {
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
        alienHiveActive = false;
        alienHiveAggressor = null;
        alienHiveAlpha = null;
        alienHiveNoLosMajorityFrames = 0;
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
            if (alienHiveActive && isObjectAliveUnlocked(alienHiveAggressor)) {
                alien.setAggroTarget(alienHiveAggressor);
            }
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
            synchronized (LOCK) {
                if (updating) {
                    pendingAdditions.add(obj);
                    return true;
                }

                addInternal(obj);
                return true;
            }
        }

        @Override
        public void add(int index, GameObject element) {
            add(element);
        }

        @Override
        public boolean remove(Object obj) {
            synchronized (LOCK) {
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
        }

        @Override
        public GameObject remove(int index) {
            GameObject obj = get(index);
            remove(obj);
            return obj;
        }

        @Override
        public void clear() {
            synchronized (LOCK) {
                clearInternal();
            }
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
            synchronized (LOCK) {
                ArrayList<GameObject> toRemove = new ArrayList<>();
                for (GameObject obj : this) {
                    if (!collection.contains(obj)) {
                        toRemove.add(obj);
                    }
                }
                return removeAll(toRemove);
            }
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