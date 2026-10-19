package main;

import java.awt.*;

/**
 * Elements graphiques communs aux menus et a la HUD (boutons, panneaux, texte centre).
 */
final class UiPainter {
    private static final int HUD_PANEL_ARC = 22;

    private UiPainter() {
    }

    static void drawButton(Graphics2D g2d, Rectangle rect, String label) {
        g2d.setColor(new Color(25, 25, 25, 210));
        g2d.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 14, 14);
        g2d.setColor(new Color(235, 235, 235));
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 14, 14);

        Font oldFont = g2d.getFont();
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 24f));
        FontMetrics fm = g2d.getFontMetrics();
        int textX = rect.x + (rect.width - fm.stringWidth(label)) / 2;
        int textY = rect.y + (rect.height - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(label, textX, textY);
        g2d.setFont(oldFont);
    }

    static void drawSmallButton(Graphics2D g2d, Rectangle rect, String label) {
        g2d.setColor(new Color(30, 30, 30, 220));
        g2d.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 8, 8);
        g2d.setColor(new Color(235, 235, 235));
        g2d.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 8, 8);

        Font oldFont = g2d.getFont();
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 16f));
        FontMetrics fm = g2d.getFontMetrics();
        int textX = rect.x + (rect.width - fm.stringWidth(label)) / 2;
        int textY = rect.y + (rect.height - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(label, textX, textY);
        g2d.setFont(oldFont);
    }

    static void drawPanelBackground(Graphics2D g2d, int x, int y, int width, int height, Color fill, Color border) {
        g2d.setColor(fill);
        g2d.fillRoundRect(x, y, width, height, HUD_PANEL_ARC, HUD_PANEL_ARC);
        g2d.setColor(border);
        g2d.setStroke(new BasicStroke(1.8f));
        g2d.drawRoundRect(x, y, width, height, HUD_PANEL_ARC, HUD_PANEL_ARC);
    }

    // Bouton centre horizontalement, utilise par les ecrans pause, fin de partie et options.
    static Rectangle centeredButton(int viewWidth, int y, int width, int height) {
        return new Rectangle((viewWidth - width) / 2, y, width, height);
    }
}
