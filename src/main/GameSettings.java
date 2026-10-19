package main;

import object.ObjectManager;
import object.ai.AiTuning;

/**
 * Reglages choisis dans le menu et l'ecran d'options : population, difficulte et options de partie.
 */
public final class GameSettings {
    public static final int OPTION_COUNT = 12;

    private int nombreCivil = 5;
    private int nombreEnnemi = 10;
    private int nombreAlien = 5;
    private int nombreSoldat = 1;
    private DifficultyProfile difficulty = DifficultyProfile.NORMAL;
    private boolean unlimitedAmmoEnabled = false;
    private boolean fireCameraFeedbackEnabled = true;
    private boolean deathMarkersEnabled = true;

    public int getCivilianCount() {
        return nombreCivil;
    }

    public int getEnemyCount() {
        return nombreEnnemi;
    }

    public int getAlienCount() {
        return nombreAlien;
    }

    public int getSoldierCount() {
        return nombreSoldat;
    }

    // Recopie la population annoncee par l'hote (affichage client uniquement).
    void setPopulation(int civilians, int enemies, int aliens, int soldiers) {
        nombreCivil = civilians;
        nombreEnnemi = enemies;
        nombreAlien = aliens;
        nombreSoldat = soldiers;
    }

    public DifficultyProfile getDifficulty() {
        return difficulty;
    }

    void setDifficulty(DifficultyProfile difficulty) {
        this.difficulty = difficulty;
    }

    public boolean isEasyDifficulty() {
        return difficulty == DifficultyProfile.EASY;
    }

    public int getAdjustedEnemyCount(int baseCount) {
        if (!isEasyDifficulty()) {
            return baseCount;
        }
        return Math.max(3, (int) Math.ceil(baseCount * 0.65));
    }

    public int getAdjustedAlienCount(int baseCount) {
        if (!isEasyDifficulty()) {
            return baseCount;
        }
        return Math.max(1, (int) Math.ceil(baseCount * 0.60));
    }

    public void applyDifficultyProfile(DifficultyProfile profile) {
        difficulty = profile;
        switch (profile) {
            case EASY -> {
                nombreSoldat = 2;
                AiTuning.applyEasyPreset();
            }
            case NORMAL -> {
                nombreSoldat = 1;
                AiTuning.applyNormalPreset();
            }
            case CUSTOM -> {
            }
        }
    }

    // rowIndex correspond a la ligne de l'ecran d'options (voir OptionsScreen).
    public void adjustOption(int rowIndex, int direction) {
        boolean affectsDifficulty = rowIndex >= 0 && rowIndex <= 8;
        if (affectsDifficulty) {
            difficulty = DifficultyProfile.CUSTOM;
        }

        switch (rowIndex) {
            case 0 -> nombreSoldat = Math.max(0, Math.min(10, nombreSoldat + direction));
            case 1 -> AiTuning.adjustEnemyReactionFrames(direction);
            case 2 -> AiTuning.adjustSoldierReactionFrames(direction);
            case 3 -> AiTuning.adjustEnemyAimStabilizationFrames(direction);
            case 4 -> AiTuning.adjustSoldierAimStabilizationFrames(direction);
            case 5 -> AiTuning.adjustSuppressionDurationFrames(direction * 5);
            case 6 -> AiTuning.adjustSuppressionCoverBoost(direction * 0.05);
            case 7 -> AiTuning.adjustSuppressionNearMissRadius(direction * 4.0);
            case 8 -> {
                AiTuning.toggleAlienPackAggro();
                if (!AiTuning.isAlienPackAggroEnabled()) {
                    ObjectManager.clearAlienAggro();
                }
            }
            case 9 -> unlimitedAmmoEnabled = !unlimitedAmmoEnabled;
            case 10 -> fireCameraFeedbackEnabled = !fireCameraFeedbackEnabled;
            case 11 -> deathMarkersEnabled = !deathMarkersEnabled;
            default -> {
            }
        }
    }

    public boolean isUnlimitedAmmoEnabled() {
        return unlimitedAmmoEnabled;
    }

    public boolean isFireCameraFeedbackEnabled() {
        return fireCameraFeedbackEnabled;
    }

    public boolean areDeathMarkersEnabled() {
        return deathMarkersEnabled;
    }

    // Recopie les options de partie annoncees par l'hote.
    void setGameOptions(boolean unlimitedAmmo, boolean fireCameraFeedback, boolean deathMarkers) {
        unlimitedAmmoEnabled = unlimitedAmmo;
        fireCameraFeedbackEnabled = fireCameraFeedback;
        deathMarkersEnabled = deathMarkers;
    }
}
