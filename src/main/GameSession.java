package main;

import java.awt.Color;

/**
 * Etat partage d'une partie : mode, score, reglages et retours visuels.
 * Une instance par GamePanel ; les objets du jeu y accedent par ObjectManager.getSession(),
 * comme pour le TileManager.
 */
public final class GameSession {
    private final GameSettings settings = new GameSettings();
    private final ScreenFeedback screenFeedback = new ScreenFeedback();
    private GameMode mode = GameMode.FREE;
    private int score = 0;

    public GameMode getMode() {
        return mode;
    }

    public boolean isMode(GameMode candidate) {
        return mode == candidate;
    }

    void setMode(GameMode mode) {
        this.mode = mode;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int points) {
        score += points;
    }

    void setScore(int score) {
        this.score = score;
    }

    public GameSettings getSettings() {
        return settings;
    }

    public ScreenFeedback getScreenFeedback() {
        return screenFeedback;
    }

    public boolean isUnlimitedAmmoEnabled() {
        return settings.isUnlimitedAmmoEnabled();
    }

    public boolean isFireCameraFeedbackEnabled() {
        return settings.isFireCameraFeedbackEnabled();
    }

    public boolean areDeathMarkersEnabled() {
        return settings.areDeathMarkersEnabled();
    }

    public void triggerScreenShake(int frames, double amplitude) {
        screenFeedback.triggerShake(frames, amplitude);
    }

    public void triggerScreenFlash(Color color, float alpha, int frames) {
        screenFeedback.triggerFlash(color, alpha, frames);
    }
}
