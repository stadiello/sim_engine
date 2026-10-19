package main;

import java.awt.*;

/**
 * Tremblement d'ecran et flash colore declenches par les tirs, explosions et vagues.
 */
public final class ScreenFeedback {
    private int shakeFrames = 0;
    private double shakeAmplitude = 0.0;
    private int flashFrames = 0;
    private Color flashColor = new Color(255, 236, 160);
    private float flashAlpha = 0f;

    public void reset() {
        shakeFrames = 0;
        shakeAmplitude = 0.0;
        flashFrames = 0;
        flashAlpha = 0f;
        flashColor = new Color(255, 236, 160);
    }

    public void triggerShake(int frames, double amplitude) {
        shakeFrames = Math.max(shakeFrames, frames);
        shakeAmplitude = Math.max(shakeAmplitude, amplitude);
    }

    public void triggerFlash(Color color, float alpha, int frames) {
        flashColor = color;
        flashAlpha = Math.max(flashAlpha, alpha);
        flashFrames = Math.max(flashFrames, frames);
    }

    // Appele une fois par pas de simulation : les effets s'estompent progressivement.
    public void tick() {
        if (shakeFrames > 0) {
            shakeFrames--;
            shakeAmplitude *= 0.82;
            if (shakeFrames == 0) {
                shakeAmplitude = 0.0;
            }
        }

        if (flashFrames > 0) {
            flashFrames--;
            flashAlpha *= 0.72f;
            if (flashFrames == 0) {
                flashAlpha = 0f;
            }
        }
    }

    public boolean isShaking() {
        return shakeFrames > 0;
    }

    public int randomShakeOffset() {
        return (int) Math.round((Math.random() * 2.0 - 1.0) * shakeAmplitude);
    }

    public void drawFlash(Graphics2D g2d, int width, int height) {
        if (flashAlpha <= 0f) {
            return;
        }
        Composite oldComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, flashAlpha));
        g2d.setColor(flashColor);
        g2d.fillRect(0, 0, width, height);
        g2d.setComposite(oldComposite);
    }
}
