package main;

import java.awt.*;

import object.AlienTeleport;
import object.Ennemi;
import object.Ennemi.EnemyArchetype;
import object.ObjectManager;
import object.Protagonist;

/**
 * Mode arcade : vagues de renforts de plus en plus rapprochees et banniere d'annonce.
 */
final class ArcadeWaves {
    private static final int ARCADE_BASE_WAVE_INTERVAL_FRAMES = GamePanel.TARGET_UPS * 14;
    private static final int ARCADE_MIN_WAVE_INTERVAL_FRAMES = GamePanel.TARGET_UPS * 7;
    private static final int ARCADE_WAVE_BANNER_FRAMES = GamePanel.TARGET_UPS * 2;

    private final GameSession session;
    private final SpawnLocator spawns;
    private int arcadeWaveIndex = 0;
    private int arcadeWaveTimer = ARCADE_BASE_WAVE_INTERVAL_FRAMES;
    private int arcadeWaveBannerTimer = 0;

    ArcadeWaves(GameSession session, SpawnLocator spawns) {
        this.session = session;
        this.spawns = spawns;
    }

    void reset() {
        arcadeWaveIndex = 0;
        arcadeWaveTimer = ARCADE_BASE_WAVE_INTERVAL_FRAMES;
        arcadeWaveBannerTimer = 0;
    }

    // Appele a chaque pas de jeu actif ; ne fait rien hors du mode arcade.
    void update() {
        if (!session.isMode(GameMode.ARCADE)) {
            return;
        }

        if (arcadeWaveBannerTimer > 0) {
            arcadeWaveBannerTimer--;
        }

        if (arcadeWaveTimer > 0) {
            arcadeWaveTimer--;
            return;
        }

        spawnArcadeWave();
    }

    private void spawnArcadeWave() {
        Protagonist protagonist = ObjectManager.getProtagonist();
        if (protagonist == null) {
            return;
        }

        boolean easy = session.getSettings().isEasyDifficulty();
        arcadeWaveIndex++;
        arcadeWaveBannerTimer = ARCADE_WAVE_BANNER_FRAMES;
        int enemyReinforcements = Math.min(6, 2 + arcadeWaveIndex / 2);
        int alienReinforcements = arcadeWaveIndex >= 2 && arcadeWaveIndex % 2 == 0 ? 1 : 0;
        if (easy) {
            enemyReinforcements = Math.max(1, enemyReinforcements - 1);
            if (arcadeWaveIndex % 3 == 0) {
                alienReinforcements = 0;
            }
        }
        int[][] enemySpawnZones = spawns.buildEnemySpawnZones();

        for (int i = 0; i < enemyReinforcements; i++) {
            int[] zone = enemySpawnZones[(arcadeWaveIndex + i) % enemySpawnZones.length];
            double[] spawn = spawns.getFreeSpawnPositionInZoneFarFrom(
                    zone[0],
                    zone[1],
                    zone[2],
                    zone[3],
                    protagonist.x,
                    protagonist.y,
                    SpawnLocator.SAFE_HOSTILE_SPAWN_DISTANCE * 0.8
            );
            ObjectManager.list.add(new Ennemi(spawn[0], spawn[1], chooseArcadeArchetype(i, enemyReinforcements)));
        }

        for (int i = 0; i < alienReinforcements; i++) {
            double[] spawn = spawns.getFreeSpawnPositionFarFrom(protagonist.x, protagonist.y, SpawnLocator.SAFE_HOSTILE_SPAWN_DISTANCE * 0.75);
            ObjectManager.list.add(new AlienTeleport(spawn[0], spawn[1]));
        }

        arcadeWaveTimer = Math.max(
                ARCADE_MIN_WAVE_INTERVAL_FRAMES,
                ARCADE_BASE_WAVE_INTERVAL_FRAMES - arcadeWaveIndex * GamePanel.TARGET_UPS / 2
        );
        session.triggerScreenShake(6 + arcadeWaveIndex, 4.8);
        session.triggerScreenFlash(new Color(255, 154, 88), 0.16f, 6);
    }

    private EnemyArchetype chooseArcadeArchetype(int index, int waveSize) {
        if (session.getSettings().isEasyDifficulty()) {
            double pressure = arcadeWaveIndex + index / (double) Math.max(1, waveSize);
            if (pressure < 3.5) {
                return EnemyArchetype.STANDARD;
            }
            return index % 2 == 0 ? EnemyArchetype.FLANQUEUR : EnemyArchetype.ASSAUT;
        }

        double pressure = arcadeWaveIndex + index / (double) Math.max(1, waveSize);
        if (pressure < 1.5) {
            return EnemyArchetype.STANDARD;
        }
        if (pressure < 3.2) {
            return index % 3 == 0 ? EnemyArchetype.ASSAUT : EnemyArchetype.FLANQUEUR;
        }
        if (pressure < 5.0) {
            return index % 5 == 0 ? EnemyArchetype.ROQUETTE : (index % 4 == 0 ? EnemyArchetype.LOURD : EnemyArchetype.ASSAUT);
        }
        if (index % 5 == 0) {
            return EnemyArchetype.LOURD;
        }
        if (index % 7 == 0) {
            return EnemyArchetype.ROQUETTE;
        }
        return index % 2 == 0 ? EnemyArchetype.FLANQUEUR : EnemyArchetype.ASSAUT;
    }

    // Banniere "VAGUE n" affichee quelques secondes apres l'arrivee des renforts.
    void drawBanner(Graphics2D g2d, int viewWidth) {
        if (arcadeWaveBannerTimer <= 0) {
            return;
        }
        Font oldFont = g2d.getFont();
        float alpha = Math.min(1f, arcadeWaveBannerTimer / (float) ARCADE_WAVE_BANNER_FRAMES);
        Composite oldComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.28f + alpha * 0.42f));
        g2d.setColor(new Color(255, 142, 84));
        g2d.fillRoundRect(viewWidth / 2 - 160, 28, 320, 48, 18, 18);
        g2d.setComposite(oldComposite);
        g2d.setColor(Color.WHITE);
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 24f));
        String label = "VAGUE " + arcadeWaveIndex;
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(label, (viewWidth - fm.stringWidth(label)) / 2, 59);
        g2d.setFont(oldFont);
    }

    String[] getHudDetails() {
        return new String[]{
                "Vague : " + (arcadeWaveIndex + 1),
                "Renforts dans : " + Math.max(0, arcadeWaveTimer / GamePanel.TARGET_UPS) + " s"
        };
    }
}
