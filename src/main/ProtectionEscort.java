package main;

import java.awt.*;

import object.ObjectManager;
import object.Protagonist;
import world.TileManager;

/**
 * Mode protection : escorter les allies jusqu'a la zone d'extraction a droite de la carte.
 */
final class ProtectionEscort {
    private static final double EXTRACTION_MARGIN_TILES = 1.5;

    private final TileManager tileManager;
    private final int tileSize;

    ProtectionEscort(TileManager tileManager, int tileSize) {
        this.tileManager = tileManager;
        this.tileSize = tileSize;
    }

    double getExtractionX() {
        return tileManager.getWorldWidth() - tileSize * EXTRACTION_MARGIN_TILES;
    }

    // Echec si plus aucun allie, victoire quand le protagoniste atteint l'extraction.
    MissionDirector.Outcome update(Protagonist protagonist) {
        int[] counts = ObjectManager.getUiCounts();
        int alliesAlive = counts[0] + counts[1];
        if (alliesAlive <= 0) {
            return MissionDirector.Outcome.FAILED;
        }

        if (protagonist.x >= getExtractionX()) {
            return MissionDirector.Outcome.VICTORY;
        }
        return MissionDirector.Outcome.ONGOING;
    }

    void drawExtractionZone(Graphics2D g2d) {
        int zoneX = (int) Math.round(getExtractionX());
        int zoneWidth = (int) Math.round(tileSize * 1.5);
        int zoneHeight = (int) Math.round(tileManager.getWorldHeight());

        g2d.setColor(new Color(90, 210, 110, 70));
        g2d.fillRect(zoneX, 0, zoneWidth, zoneHeight);
        g2d.setColor(new Color(170, 255, 190, 180));
        g2d.drawRect(zoneX, 0, zoneWidth, zoneHeight);
        g2d.drawString("EXTRACTION", zoneX + 8, 22);
    }

    String[] getHudDetails(Protagonist protagonist) {
        int[] counts = ObjectManager.getUiCounts();
        double distanceToExtraction = Math.max(0, getExtractionX() - protagonist.x);
        return new String[]{
                "Distance extraction : " + (int) Math.round(distanceToExtraction) + " px",
                "Allies encore en vie : " + (counts[0] + counts[1]),
                "Maintiens la colonne en mouvement"
        };
    }
}
