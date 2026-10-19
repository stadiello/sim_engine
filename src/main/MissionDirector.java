package main;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import object.Alien;
import object.Ennemi;
import object.Ennemi.EnemyArchetype;
import object.GameObject;
import object.Hostage;
import object.ImpactSpark;
import object.ObjectManager;
import object.Protagonist;
import object.Shockwave;
import object.ai.TacticalMovement;
import world.TileManager;

/**
 * Mode mission : choix de l'objectif, mise en place, suivi de la progression,
 * marqueurs dans le monde et lignes du HUD.
 */
final class MissionDirector {
    private static final int MISSION_DETONATION_FUSE_FRAMES = GamePanel.TARGET_UPS * 3;
    private static final int MISSION_DEFEND_HOLD_FRAMES = GamePanel.TARGET_UPS * 35;
    private static final int MISSION_SECURE_HOLD_FRAMES = GamePanel.TARGET_UPS * 8;
    private static final double MISSION_INTERACT_DISTANCE = 84.0;
    private static final double MISSION_HOSTAGE_FREE_DISTANCE = 90.0;
    private static final double MISSION_INFILTRATION_DETECT_DISTANCE = 260.0;

    private enum MissionType {
        RESCUE_HOSTAGES,
        DEFEND_POSITION,
        SECURE_ZONE,
        INFILTRATE,
        DESTROY_VEHICLE
    }

    // Issue d'un pas de mission, appliquee par GamePanel a l'ecran courant.
    enum Outcome {
        ONGOING,
        VICTORY,
        FAILED
    }

    private final TileManager tileManager;
    private final int tileSize;
    private final GameSession session;
    private final SpawnLocator spawns;
    private int missionRotationIndex = 0;
    private MissionType activeMissionType;
    private String activeMissionTitle = "";
    private Rectangle2D.Double missionPrimaryZone;
    private Rectangle2D.Double missionSecondaryZone;
    private final ArrayList<Hostage> missionHostages = new ArrayList<>();
    private final ArrayList<Hostage> missionFreedHostages = new ArrayList<>();
    private final ArrayList<Hostage> missionExtractedHostages = new ArrayList<>();
    private int missionDefendFramesRemaining = 0;
    private int missionSecureFrames = 0;
    private double missionVehicleX = -1;
    private double missionVehicleY = -1;
    private boolean missionVehicleChargeArmed = false;
    private int missionVehicleFuseFrames = 0;
    private boolean missionFailed = false;
    private String failureReason = "";

    MissionDirector(TileManager tileManager, int tileSize, GameSession session, SpawnLocator spawns) {
        this.tileManager = tileManager;
        this.tileSize = tileSize;
        this.session = session;
        this.spawns = spawns;
    }

    void reset() {
        activeMissionType = null;
        activeMissionTitle = "";
        missionPrimaryZone = null;
        missionSecondaryZone = null;
        missionHostages.clear();
        missionFreedHostages.clear();
        missionExtractedHostages.clear();
        missionDefendFramesRemaining = 0;
        missionSecureFrames = 0;
        missionVehicleX = -1;
        missionVehicleY = -1;
        missionVehicleChargeArmed = false;
        missionVehicleFuseFrames = 0;
        missionFailed = false;
    }

    // Raison du dernier echec, affichee comme titre de l'ecran de fin.
    String getFailureReason() {
        return failureReason;
    }

    String getTitle() {
        return activeMissionTitle;
    }

    private MissionType selectNextMissionType() {
        MissionType[] types = MissionType.values();
        MissionType type = types[missionRotationIndex % types.length];
        missionRotationIndex++;
        return type;
    }

    private Rectangle2D.Double zoneFromTiles(int startCol, int endCol, int startRow, int endRow) {
        int minCol = Math.max(0, Math.min(startCol, tileManager.getMapCols() - 1));
        int maxCol = Math.max(0, Math.min(endCol, tileManager.getMapCols() - 1));
        int minRow = Math.max(0, Math.min(startRow, tileManager.getMapRows() - 1));
        int maxRow = Math.max(0, Math.min(endRow, tileManager.getMapRows() - 1));

        if (minCol > maxCol || minRow > maxRow) {
            return new Rectangle2D.Double(0, 0, tileSize * 2.0, tileSize * 2.0);
        }

        double x = minCol * tileSize;
        double y = minRow * tileSize;
        double width = (maxCol - minCol + 1) * tileSize;
        double height = (maxRow - minRow + 1) * tileSize;
        return new Rectangle2D.Double(x, y, width, height);
    }

    void setup(double protagonistSpawnX, double protagonistSpawnY) {
        activeMissionType = selectNextMissionType();

        int cols = tileManager.getMapCols();
        int rows = tileManager.getMapRows();
        int centerRow = Math.max(3, rows / 2 - 2);

        switch (activeMissionType) {
            case RESCUE_HOSTAGES -> {
                activeMissionTitle = "Mission: Liberer les otages";
                int startCol = Math.max(2, cols - 13);
                int endCol = Math.max(4, cols - 8);
                int startRow = 4;
                int endRow = Math.min(rows - 6, 11);
                missionPrimaryZone = zoneFromTiles(startCol, endCol, startRow, endRow);
                missionSecondaryZone = zoneFromTiles(3, Math.min(8, cols - 2), 3, Math.min(8, rows - 2));

                for (int i = 0; i < 3; i++) {
                    double[] spawn = spawns.getFreeSpawnPositionInZone(startCol, endCol, startRow + 1, Math.max(startRow + 1, endRow - 1));
                    Hostage hostage = new Hostage(spawn[0], spawn[1]);
                    missionHostages.add(hostage);
                    ObjectManager.list.add(hostage);
                }

                spawnMissionGuards(startCol, endCol, startRow, endRow);
            }
            case DEFEND_POSITION -> {
                activeMissionTitle = "Mission: Proteger la position";
                missionPrimaryZone = new Rectangle2D.Double(
                        protagonistSpawnX - tileSize * 2.5,
                        protagonistSpawnY - tileSize * 2.5,
                        tileSize * 5.0,
                        tileSize * 5.0
                );
                missionDefendFramesRemaining = MISSION_DEFEND_HOLD_FRAMES;
            }
            case SECURE_ZONE -> {
                activeMissionTitle = "Mission: Securiser la zone";
                missionPrimaryZone = zoneFromTiles(Math.max(8, cols / 2 - 4), Math.min(cols - 4, cols / 2 + 4), centerRow, Math.min(rows - 4, centerRow + 7));
                missionSecureFrames = 0;
            }
            case INFILTRATE -> {
                activeMissionTitle = "Mission: Infiltration";
                missionPrimaryZone = zoneFromTiles(Math.max(3, cols - 8), Math.max(5, cols - 4), 3, Math.min(rows - 4, 8));
            }
            case DESTROY_VEHICLE -> {
                activeMissionTitle = "Mission: Sabotage";
                int col = Math.max(6, cols - 9);
                int row = Math.max(6, rows - 8);
                missionVehicleX = col * tileSize + tileSize * 0.5;
                missionVehicleY = row * tileSize + tileSize * 0.5;
                missionPrimaryZone = new Rectangle2D.Double(
                        missionVehicleX - tileSize * 1.5,
                        missionVehicleY - tileSize,
                        tileSize * 3.0,
                        tileSize * 2.0
                );
            }
        }
    }

    Outcome update(Protagonist protagonist, Protagonist interactionPlayer, boolean interactTriggered) {
        if (!session.isMode(GameMode.MISSION) || activeMissionType == null || protagonist == null || missionFailed) {
            return Outcome.ONGOING;
        }

        if (interactionPlayer == null) interactionPlayer = protagonist;

        return switch (activeMissionType) {
            case RESCUE_HOSTAGES -> updateRescueMission(interactionPlayer, interactTriggered);
            case DEFEND_POSITION -> updateDefendMission(protagonist);
            case SECURE_ZONE -> updateSecureMission(protagonist);
            case INFILTRATE -> updateInfiltrationMission(protagonist);
            case DESTROY_VEHICLE -> updateSabotageMission(interactionPlayer, interactTriggered);
        };
    }

    private Outcome updateRescueMission(Protagonist protagonist, boolean interactTriggered) {
        int aliveHostages = 0;
        int deadHostages = 0;

        for (Hostage hostage : missionHostages) {
            if (missionExtractedHostages.contains(hostage)) {
                continue;
            }

            if (!isObjectAlive(hostage)) {
                deadHostages++;
                continue;
            }

            aliveHostages++;
            if (!hostage.isRescued()) {
                double dx = hostage.x - protagonist.x;
                double dy = hostage.y - protagonist.y;
                if (interactTriggered && dx * dx + dy * dy <= MISSION_HOSTAGE_FREE_DISTANCE * MISSION_HOSTAGE_FREE_DISTANCE) {
                    hostage.rescue();
                    if (!missionFreedHostages.contains(hostage)) {
                        missionFreedHostages.add(hostage);
                    }
                }
            }

            if (hostage.isRescued() && missionSecondaryZone != null && missionSecondaryZone.contains(hostage.x, hostage.y)) {
                missionExtractedHostages.add(hostage);
                ObjectManager.list.remove(hostage);
            }
        }

        if (deadHostages > 0) {
            return failMission("OTAGE ABATTU");
        }

        if (!missionHostages.isEmpty() && missionExtractedHostages.size() >= missionHostages.size()) {
            return Outcome.VICTORY;
        }

        if (aliveHostages <= 0 && missionExtractedHostages.isEmpty()) {
            return failMission("OTAGES PERDUS");
        }
        return Outcome.ONGOING;
    }

    private void spawnMissionGuards(int startCol, int endCol, int startRow, int endRow) {
        int outerStartCol = Math.max(1, startCol - 2);
        int outerEndCol = Math.min(tileManager.getMapCols() - 2, endCol + 2);
        int outerStartRow = Math.max(1, startRow - 1);
        int outerEndRow = Math.min(tileManager.getMapRows() - 2, endRow + 2);

        EnemyArchetype[] guards = {
                EnemyArchetype.STANDARD,
                EnemyArchetype.STANDARD,
                EnemyArchetype.ASSAUT,
                EnemyArchetype.FLANQUEUR
        };

        int guardCount = session.getSettings().isEasyDifficulty() ? 2 : guards.length;

        for (int i = 0; i < guardCount; i++) {
            double[] spawn = spawns.getFreeSpawnPositionInZone(outerStartCol, outerEndCol, outerStartRow, outerEndRow);
            ObjectManager.list.add(new Ennemi(spawn[0], spawn[1], guards[i]));
        }
    }

    private Outcome updateDefendMission(Protagonist protagonist) {
        if (missionPrimaryZone != null && missionPrimaryZone.contains(protagonist.x, protagonist.y)) {
            missionDefendFramesRemaining = Math.max(0, missionDefendFramesRemaining - 1);
        }

        return missionDefendFramesRemaining <= 0 ? Outcome.VICTORY : Outcome.ONGOING;
    }

    private Outcome updateSecureMission(Protagonist protagonist) {
        int hostilesInZone = countHostilesInZone(missionPrimaryZone);
        boolean protagonistInZone = missionPrimaryZone != null && missionPrimaryZone.contains(protagonist.x, protagonist.y);

        if (hostilesInZone == 0 && protagonistInZone) {
            missionSecureFrames++;
        } else {
            missionSecureFrames = Math.max(0, missionSecureFrames - 2);
        }

        return missionSecureFrames >= MISSION_SECURE_HOLD_FRAMES ? Outcome.VICTORY : Outcome.ONGOING;
    }

    private Outcome updateInfiltrationMission(Protagonist protagonist) {
        if (isProtagonistDetected(protagonist)) {
            return failMission("INFILTRATION COMPROMISE");
        }

        if (missionPrimaryZone != null && missionPrimaryZone.contains(protagonist.x, protagonist.y)) {
            return Outcome.VICTORY;
        }
        return Outcome.ONGOING;
    }

    private Outcome updateSabotageMission(Protagonist protagonist, boolean interactTriggered) {
        double dx = missionVehicleX - protagonist.x;
        double dy = missionVehicleY - protagonist.y;
        boolean closeEnough = dx * dx + dy * dy <= MISSION_INTERACT_DISTANCE * MISSION_INTERACT_DISTANCE;

        if (!missionVehicleChargeArmed && closeEnough && interactTriggered) {
            missionVehicleChargeArmed = true;
            missionVehicleFuseFrames = MISSION_DETONATION_FUSE_FRAMES;
            session.triggerScreenFlash(new Color(255, 230, 140), 0.09f, 4);
        }

        if (missionVehicleChargeArmed) {
            missionVehicleFuseFrames = Math.max(0, missionVehicleFuseFrames - 1);
            if (missionVehicleFuseFrames <= 0) {
                for (int i = 0; i < 6; i++) {
                    ObjectManager.list.add(new ImpactSpark(
                            missionVehicleX,
                            missionVehicleY,
                            Math.random() * 2.0 - 1.0,
                            Math.random() * 2.0 - 1.0,
                            1.8,
                            new Color(255, 214, 138),
                            new Color(255, 118, 72)
                    ));
                }

                ObjectManager.list.add(new Shockwave(missionVehicleX, missionVehicleY, protagonist, tileSize * 3.2));
                session.triggerScreenShake(16, 7.8);
                session.triggerScreenFlash(new Color(255, 170, 96), 0.24f, 8);
                return Outcome.VICTORY;
            }
        }
        return Outcome.ONGOING;
    }

    private Outcome failMission(String reason) {
        missionFailed = true;
        failureReason = reason;
        return Outcome.FAILED;
    }

    private boolean isObjectAlive(GameObject target) {
        if (target == null) {
            return false;
        }

        for (GameObject obj : ObjectManager.list) {
            if (obj == target) {
                return true;
            }
        }
        return false;
    }

    private int countHostilesInZone(Rectangle2D zone) {
        if (zone == null) {
            return 0;
        }

        int count = 0;
        for (GameObject obj : ObjectManager.list) {
            if (obj instanceof Ennemi || obj instanceof Alien) {
                if (zone.contains(obj.x, obj.y)) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean isProtagonistDetected(Protagonist protagonist) {
        for (GameObject obj : ObjectManager.list) {
            if (!(obj instanceof Ennemi) && !(obj instanceof Alien)) {
                continue;
            }

            double dx = protagonist.x - obj.x;
            double dy = protagonist.y - obj.y;
            double distSq = dx * dx + dy * dy;
            if (distSq > MISSION_INFILTRATION_DETECT_DISTANCE * MISSION_INFILTRATION_DETECT_DISTANCE) {
                continue;
            }

            if (TacticalMovement.hasLineOfSight(obj.x, obj.y, protagonist.x, protagonist.y)) {
                return true;
            }
        }

        return false;
    }

    // Zones, otages et vehicule dessines dans le repere du monde (camera deja appliquee).
    void drawWorldMarkers(Graphics2D g2d) {
        if (activeMissionType == null) {
            return;
        }

        if (missionPrimaryZone != null) {
            g2d.setColor(new Color(110, 210, 255, 52));
            g2d.fill(missionPrimaryZone);
            g2d.setColor(new Color(140, 228, 255, 180));
            g2d.setStroke(new BasicStroke(2f));
            g2d.draw(missionPrimaryZone);

            if (activeMissionType == MissionType.RESCUE_HOSTAGES) {
                g2d.drawString("CAPTIFS", (int) Math.round(missionPrimaryZone.x + 10), (int) Math.round(missionPrimaryZone.y + 20));
            }
        }

        if (missionSecondaryZone != null) {
            g2d.setColor(new Color(240, 202, 122, 44));
            g2d.fill(missionSecondaryZone);
            g2d.setColor(new Color(255, 224, 146, 165));
            g2d.setStroke(new BasicStroke(2f));
            g2d.draw(missionSecondaryZone);

            if (activeMissionType == MissionType.RESCUE_HOSTAGES) {
                g2d.drawString("EXFIL", (int) Math.round(missionSecondaryZone.x + 10), (int) Math.round(missionSecondaryZone.y + 20));
            }
        }

        if (activeMissionType == MissionType.RESCUE_HOSTAGES) {
            for (Hostage hostage : missionHostages) {
                if (missionExtractedHostages.contains(hostage)) {
                    continue;
                }
                if (!isObjectAlive(hostage)) {
                    continue;
                }

                boolean freed = hostage.isRescued();
                int radius = 16;
                g2d.setColor(freed ? new Color(120, 255, 170, 170) : new Color(255, 190, 110, 165));
                g2d.fillOval((int) Math.round(hostage.x - radius), (int) Math.round(hostage.y - radius), radius * 2, radius * 2);
                g2d.setColor(freed ? new Color(170, 255, 200) : new Color(255, 218, 155));
                g2d.drawOval((int) Math.round(hostage.x - radius), (int) Math.round(hostage.y - radius), radius * 2, radius * 2);
            }
        }

        if (activeMissionType == MissionType.DESTROY_VEHICLE && missionVehicleX >= 0 && missionVehicleY >= 0) {
            int bodyW = (int) Math.round(tileSize * 1.3);
            int bodyH = (int) Math.round(tileSize * 0.76);
            int x = (int) Math.round(missionVehicleX - bodyW / 2.0);
            int y = (int) Math.round(missionVehicleY - bodyH / 2.0);

            g2d.setColor(new Color(82, 96, 108));
            g2d.fillRoundRect(x, y, bodyW, bodyH, 10, 10);
            g2d.setColor(new Color(125, 142, 156));
            g2d.drawRoundRect(x, y, bodyW, bodyH, 10, 10);
            g2d.setColor(new Color(34, 39, 46));
            g2d.fillRect(x + 7, y + bodyH - 5, 18, 5);
            g2d.fillRect(x + bodyW - 25, y + bodyH - 5, 18, 5);

            if (missionVehicleChargeArmed) {
                float pulse = (float) (0.55 + 0.45 * Math.abs(Math.sin(missionVehicleFuseFrames * 0.23)));
                Composite oldComposite = g2d.getComposite();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulse));
                g2d.setColor(new Color(255, 110, 86));
                g2d.fillOval((int) Math.round(missionVehicleX - 10), (int) Math.round(missionVehicleY - 10), 20, 20);
                g2d.setComposite(oldComposite);
            }
        }
    }

    String[] getHudDetails() {
        if (activeMissionType == null) {
            return new String[]{"Objectif en attente"};
        }

        return switch (activeMissionType) {
            case RESCUE_HOSTAGES -> new String[]{
                    "Gardes en zone : " + countHostilesInZone(missionPrimaryZone),
                    "Otages extraits : " + missionExtractedHostages.size() + "/" + missionHostages.size(),
                    missionFreedHostages.size() < missionHostages.size() ? "Approche et appuie sur E pour delivrer" : "Replie-les vers l'exfiltration"
            };
            case DEFEND_POSITION -> new String[]{
                    "Temps a tenir : " + Math.max(0, missionDefendFramesRemaining / GamePanel.TARGET_UPS) + " s",
                    "Reste dans le perimetre"
            };
            case SECURE_ZONE -> new String[]{
                    "Hostiles dans la zone : " + countHostilesInZone(missionPrimaryZone),
                    "Stabilisation : " + Math.min(100, (missionSecureFrames * 100) / MISSION_SECURE_HOLD_FRAMES) + " %"
            };
            case INFILTRATE -> new String[]{
                    "Aucun contact visuel autorise",
                    "Reste hors des lignes de vue"
            };
            case DESTROY_VEHICLE -> new String[]{
                    missionVehicleChargeArmed
                            ? "Charge activee : " + Math.max(0, missionVehicleFuseFrames / GamePanel.TARGET_UPS) + " s"
                            : "Approche le vehicule et appuie sur E",
                    missionVehicleChargeArmed ? "Decroche avant l'explosion" : "Prepare l'exfiltration"
            };
        };
    }
}
