package main;

import java.awt.*;

import object.ai.AiTuning;

/**
 * Ecran d'options : reglages de l'IA et options de partie, modifies par des boutons -/+.
 */
final class OptionsScreen {
    private static final int AI_OPTIONS_BASE_Y = 190;
    private static final int AI_OPTION_ROW_GAP = 26;

    private final GameSettings settings;

    OptionsScreen(GameSettings settings) {
        this.settings = settings;
    }

    boolean isBackButton(int mouseX, int mouseY, int viewWidth, int viewHeight) {
        return getBackButtonBounds(viewWidth, viewHeight).contains(mouseX, mouseY);
    }

    // Applique le clic sur un bouton -/+ ; renvoie true si le clic a ete consomme.
    boolean handleClick(int mouseX, int mouseY, int viewWidth) {
        for (int i = 0; i < GameSettings.OPTION_COUNT; i++) {
            if (getAiMinusButtonBounds(i, viewWidth).contains(mouseX, mouseY)) {
                settings.adjustOption(i, -1);
                return true;
            }
            if (getAiPlusButtonBounds(i, viewWidth).contains(mouseX, mouseY)) {
                settings.adjustOption(i, 1);
                return true;
            }
        }

        return false;
    }

    private Rectangle getBackButtonBounds(int viewWidth, int viewHeight) {
        return UiPainter.centeredButton(viewWidth, viewHeight - 62, 220, 52);
    }

    private Rectangle getAiMinusButtonBounds(int rowIndex, int viewWidth) {
        int x = viewWidth - 210;
        int y = AI_OPTIONS_BASE_Y + rowIndex * AI_OPTION_ROW_GAP - 22;
        return new Rectangle(x, y, 34, 26);
    }

    private Rectangle getAiPlusButtonBounds(int rowIndex, int viewWidth) {
        int x = viewWidth - 168;
        int y = AI_OPTIONS_BASE_Y + rowIndex * AI_OPTION_ROW_GAP - 22;
        return new Rectangle(x, y, 34, 26);
    }

    void draw(Graphics2D g2d, int panelWidth, int panelHeight) {
        g2d.setColor(new Color(0, 0, 0, 185));
        g2d.fillRect(0, 0, panelWidth, panelHeight);

        Font oldFont = g2d.getFont();
        g2d.setColor(Color.WHITE);
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 40f));
        String title = "Options";
        FontMetrics titleFm = g2d.getFontMetrics();
        g2d.drawString(title, (panelWidth - titleFm.stringWidth(title)) / 2, 130);

        g2d.setFont(oldFont.deriveFont(Font.BOLD, 22f));
        g2d.drawString("Options de partie et IA", 130, AI_OPTIONS_BASE_Y - 34);

        g2d.setFont(oldFont.deriveFont(Font.PLAIN, 18f));
        drawAiOptionRow(g2d, panelWidth, 0, "Soldats allies", Integer.toString(settings.getSoldierCount()));
        drawAiOptionRow(g2d, panelWidth, 1, "Reaction ennemis (frames)", Integer.toString(AiTuning.getEnemyReactionFrames()));
        drawAiOptionRow(g2d, panelWidth, 2, "Reaction soldats (frames)", Integer.toString(AiTuning.getSoldierReactionFrames()));
        drawAiOptionRow(g2d, panelWidth, 3, "Visee ennemis (frames)", Integer.toString(AiTuning.getEnemyAimStabilizationFrames()));
        drawAiOptionRow(g2d, panelWidth, 4, "Visee soldats (frames)", Integer.toString(AiTuning.getSoldierAimStabilizationFrames()));
        drawAiOptionRow(g2d, panelWidth, 5, "Suppression duree (frames)", Integer.toString(AiTuning.getSuppressionDurationFrames()));
        drawAiOptionRow(g2d, panelWidth, 6, "Suppression bonus couverture", String.format("%.2f", AiTuning.getSuppressionCoverBoost()));
        drawAiOptionRow(g2d, panelWidth, 7, "Suppression rayon (px)", Integer.toString((int) Math.round(AiTuning.getSuppressionNearMissRadius())));
        drawAiOptionRow(g2d, panelWidth, 8, "Aliens meute agressive", AiTuning.isAlienPackAggroEnabled() ? "ON" : "OFF");
        drawAiOptionRow(g2d, panelWidth, 9, "Munitions illimitees", settings.isUnlimitedAmmoEnabled() ? "ON" : "OFF");
        drawAiOptionRow(g2d, panelWidth, 10, "Feedback camera des tirs", settings.isFireCameraFeedbackEnabled() ? "ON" : "OFF");
        drawAiOptionRow(g2d, panelWidth, 11, "Marqueurs des morts", settings.areDeathMarkersEnabled() ? "ON" : "OFF");

        UiPainter.drawButton(g2d, getBackButtonBounds(panelWidth, panelHeight), "Retour");
        g2d.setFont(oldFont);
    }

    private void drawAiOptionRow(Graphics2D g2d, int panelWidth, int rowIndex, String label, String value) {
        int y = AI_OPTIONS_BASE_Y + rowIndex * AI_OPTION_ROW_GAP;
        g2d.drawString(label, 130, y);
        g2d.drawString(value, 470, y);

        UiPainter.drawSmallButton(g2d, getAiMinusButtonBounds(rowIndex, panelWidth), "-");
        UiPainter.drawSmallButton(g2d, getAiPlusButtonBounds(rowIndex, panelWidth), "+");
    }
}
