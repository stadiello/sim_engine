package main;

import object.ObjectManager;
import world.TileManager;

/**
 * Recherche de positions d'apparition libres (ni tuile bloquante, ni humain deja present).
 */
final class SpawnLocator {
    static final double ENTITY_RADIUS = 14;
    static final double SAFE_HOSTILE_SPAWN_DISTANCE = 520;
    private static final int MAX_SPAWN_ATTEMPTS = 300;

    private final TileManager tileManager;
    private final int tileSize;

    SpawnLocator(TileManager tileManager, int tileSize) {
        this.tileManager = tileManager;
        this.tileSize = tileSize;
    }

    // Zones d'apparition ennemies : bord droit (haut et bas), centre bas et gauche du centre bas.
    int[][] buildEnemySpawnZones() {
        int maxCol = tileManager.getMapCols() - 2;
        int maxRow = tileManager.getMapRows() - 2;

        int rightStart = Math.max(1, maxCol - 10);
        int rightEnd = maxCol;
        int centerStart = Math.max(1, maxCol / 2 - 4);
        int centerEnd = Math.min(maxCol, maxCol / 2 + 4);

        int topStart = 1;
        int topEnd = Math.min(8, maxRow);
        int bottomStart = Math.max(1, maxRow - 8);
        int bottomEnd = maxRow;

        return new int[][]{
                {rightStart, rightEnd, topStart, topEnd},
            {rightStart, rightEnd, bottomStart, bottomEnd},
            {centerStart, centerEnd, bottomStart, bottomEnd},
            {Math.max(1, centerStart - 12), Math.max(1, centerEnd - 12), bottomStart, bottomEnd}
        };
    }

    double[] getFreeSpawnPositionInZone(int startCol, int endCol, int startRow, int endRow) {
        int mapCols = tileManager.getMapCols();
        int mapRows = tileManager.getMapRows();

        int minCol = Math.max(0, Math.min(startCol, mapCols - 1));
        int maxCol = Math.max(0, Math.min(endCol, mapCols - 1));
        int minRow = Math.max(0, Math.min(startRow, mapRows - 1));
        int maxRow = Math.max(0, Math.min(endRow, mapRows - 1));

        if (minCol > maxCol || minRow > maxRow) {
            return getFreeSpawnPosition();
        }

        for (int i = 0; i < MAX_SPAWN_ATTEMPTS; i++) {
            int col = minCol + (int) (Math.random() * (maxCol - minCol + 1));
            int row = minRow + (int) (Math.random() * (maxRow - minRow + 1));

            double x = col * tileSize + tileSize / 2.0;
            double y = row * tileSize + tileSize / 2.0;

            if (isSpawnAreaFree(x, y, ENTITY_RADIUS)) {
                return new double[]{x, y};
            }
        }

        for (int row = minRow; row <= maxRow; row++) {
            for (int col = minCol; col <= maxCol; col++) {
                double x = col * tileSize + tileSize / 2.0;
                double y = row * tileSize + tileSize / 2.0;

                if (isSpawnAreaFree(x, y, ENTITY_RADIUS)) {
                    return new double[]{x, y};
                }
            }
        }

        return getFreeSpawnPosition();
    }

    double[] getFreeSpawnPositionInZoneFarFrom(
            int startCol,
            int endCol,
            int startRow,
            int endRow,
            double avoidX,
            double avoidY,
            double minDistance
    ) {
        int mapCols = tileManager.getMapCols();
        int mapRows = tileManager.getMapRows();

        int minCol = Math.max(0, Math.min(startCol, mapCols - 1));
        int maxCol = Math.max(0, Math.min(endCol, mapCols - 1));
        int minRow = Math.max(0, Math.min(startRow, mapRows - 1));
        int maxRow = Math.max(0, Math.min(endRow, mapRows - 1));

        if (minCol > maxCol || minRow > maxRow) {
            return getFreeSpawnPositionFarFrom(avoidX, avoidY, minDistance);
        }

        for (int i = 0; i < MAX_SPAWN_ATTEMPTS; i++) {
            int col = minCol + (int) (Math.random() * (maxCol - minCol + 1));
            int row = minRow + (int) (Math.random() * (maxRow - minRow + 1));

            double x = col * tileSize + tileSize / 2.0;
            double y = row * tileSize + tileSize / 2.0;

            if (isSpawnAreaFree(x, y, ENTITY_RADIUS) && isFarEnoughFromPoint(x, y, avoidX, avoidY, minDistance)) {
                return new double[]{x, y};
            }
        }

        for (int row = minRow; row <= maxRow; row++) {
            for (int col = minCol; col <= maxCol; col++) {
                double x = col * tileSize + tileSize / 2.0;
                double y = row * tileSize + tileSize / 2.0;

                if (isSpawnAreaFree(x, y, ENTITY_RADIUS) && isFarEnoughFromPoint(x, y, avoidX, avoidY, minDistance)) {
                    return new double[]{x, y};
                }
            }
        }

        return getFreeSpawnPositionFarFrom(avoidX, avoidY, minDistance);
    }

    double[] getFreeSpawnPosition() {
        int mapCols = tileManager.getMapCols();
        int mapRows = tileManager.getMapRows();
        double worldWidth = tileManager.getWorldWidth();
        double worldHeight = tileManager.getWorldHeight();

        for (int i = 0; i < MAX_SPAWN_ATTEMPTS; i++) {
            double x = ENTITY_RADIUS + Math.random() * (worldWidth - ENTITY_RADIUS * 2);
            double y = ENTITY_RADIUS + Math.random() * (worldHeight - ENTITY_RADIUS * 2);

            if (isSpawnAreaFree(x, y, ENTITY_RADIUS)) {
                return new double[]{x, y};
            }
        }

        for (int row = 0; row < mapRows; row++) {
            for (int col = 0; col < mapCols; col++) {
                double x = col * tileSize + tileSize / 2.0;
                double y = row * tileSize + tileSize / 2.0;

                if (isSpawnAreaFree(x, y, ENTITY_RADIUS)) {
                    return new double[]{x, y};
                }
            }
        }

        return new double[]{tileSize / 2.0, tileSize / 2.0};
    }

    double[] getFreeSpawnPositionFarFrom(double avoidX, double avoidY, double minDistance) {
        int mapCols = tileManager.getMapCols();
        int mapRows = tileManager.getMapRows();
        double worldWidth = tileManager.getWorldWidth();
        double worldHeight = tileManager.getWorldHeight();

        for (int i = 0; i < MAX_SPAWN_ATTEMPTS; i++) {
            double x = ENTITY_RADIUS + Math.random() * (worldWidth - ENTITY_RADIUS * 2);
            double y = ENTITY_RADIUS + Math.random() * (worldHeight - ENTITY_RADIUS * 2);

            if (isSpawnAreaFree(x, y, ENTITY_RADIUS) && isFarEnoughFromPoint(x, y, avoidX, avoidY, minDistance)) {
                return new double[]{x, y};
            }
        }

        for (int row = 0; row < mapRows; row++) {
            for (int col = 0; col < mapCols; col++) {
                double x = col * tileSize + tileSize / 2.0;
                double y = row * tileSize + tileSize / 2.0;

                if (isSpawnAreaFree(x, y, ENTITY_RADIUS) && isFarEnoughFromPoint(x, y, avoidX, avoidY, minDistance)) {
                    return new double[]{x, y};
                }
            }
        }

        return getFreeSpawnPosition();
    }

    private boolean isFarEnoughFromPoint(double x, double y, double pointX, double pointY, double minDistance) {
        double dx = x - pointX;
        double dy = y - pointY;
        return dx * dx + dy * dy >= minDistance * minDistance;
    }

    boolean isSpawnAreaFree(double x, double y, double radius) {
        return !tileManager.isBlockedAtPixel(x - radius, y - radius)
                && !tileManager.isBlockedAtPixel(x + radius, y - radius)
                && !tileManager.isBlockedAtPixel(x - radius, y + radius)
                && !tileManager.isBlockedAtPixel(x + radius, y + radius)
                && ObjectManager.isHumanAreaFree(x, y, radius);
    }
}
