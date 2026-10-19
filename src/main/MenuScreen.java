package main;

import java.awt.*;

import world.TileManager;
import world.TileManager.MapType;

/**
 * Menu principal : choix de la carte, de la difficulte et du mode de jeu.
 */
final class MenuScreen {
    private static final int MENU_MAP_CARD_GAP = 26;
    private static final int DIFFICULTY_BUTTON_GAP = 14;
    // Les boutons de difficulte sont dessines 100 px plus bas que leurs bornes de reference.
    private static final int DIFFICULTY_DRAW_OFFSET_Y = 100;

    private final TileManager tileManager;
    private final GameSettings settings;

    MenuScreen(TileManager tileManager, GameSettings settings) {
        this.tileManager = tileManager;
        this.settings = settings;
    }

    // Selection de carte ou de difficulte ; renvoie true si le clic a ete consomme.
    boolean handleSettingsClick(int mouseX, int mouseY, int viewWidth) {
        if (handleMapSelectionClick(mouseX, mouseY, viewWidth)) {
            return true;
        }
        // TODO: Modification à la main de la position de la selection de la diff mais pas très propre, à corriger plus tard
        return handleDifficultySelectionClick(mouseX, mouseY - DIFFICULTY_DRAW_OFFSET_Y, viewWidth);
    }

    // Mode correspondant au bouton clique, ou null.
    GameMode getModeAt(int mouseX, int mouseY, int viewWidth, int viewHeight) {
        if (getFreeModeButtonBounds(viewWidth, viewHeight).contains(mouseX, mouseY)) {
            return GameMode.FREE;
        } else if (getArcadeModeButtonBounds(viewWidth, viewHeight).contains(mouseX, mouseY)) {
            return GameMode.ARCADE;
        } else if (getStoryModeButtonBounds(viewWidth, viewHeight).contains(mouseX, mouseY)) {
            return GameMode.STORY;
        } else if (getMissionModeButtonBounds(viewWidth, viewHeight).contains(mouseX, mouseY)) {
            return GameMode.MISSION;
        } else if (getProtectionModeButtonBounds(viewWidth, viewHeight).contains(mouseX, mouseY)) {
            return GameMode.PROTECTION;
        }
        return null;
    }

    boolean isOptionsButton(int mouseX, int mouseY, int viewWidth, int viewHeight) {
        return getOptionsButtonBounds(viewWidth, viewHeight).contains(mouseX, mouseY);
    }

    private Rectangle getFreeModeButtonBounds(int viewWidth, int viewHeight) {
        return UiPainter.centeredButton(viewWidth, viewHeight / 2 + 10, 220, 48);
    }

    private Rectangle getArcadeModeButtonBounds(int viewWidth, int viewHeight) {
        return below(getFreeModeButtonBounds(viewWidth, viewHeight));
    }

    private Rectangle getStoryModeButtonBounds(int viewWidth, int viewHeight) {
        return below(getArcadeModeButtonBounds(viewWidth, viewHeight));
    }

    private Rectangle getMissionModeButtonBounds(int viewWidth, int viewHeight) {
        return below(getStoryModeButtonBounds(viewWidth, viewHeight));
    }

    private Rectangle getProtectionModeButtonBounds(int viewWidth, int viewHeight) {
        return below(getMissionModeButtonBounds(viewWidth, viewHeight));
    }

    private Rectangle getOptionsButtonBounds(int viewWidth, int viewHeight) {
        return below(getProtectionModeButtonBounds(viewWidth, viewHeight));
    }

    private static Rectangle below(Rectangle previous) {
        return new Rectangle(previous.x, previous.y + 62, previous.width, previous.height);
    }

    private Rectangle getDifficultyButtonBounds(int index, int viewWidth) {
        int buttonWidth = 130;
        int buttonHeight = 34;
        int totalWidth = buttonWidth * 3 + DIFFICULTY_BUTTON_GAP * 2;
        int startX = (viewWidth - totalWidth) / 2;
        int y = 282;
        return new Rectangle(startX + index * (buttonWidth + DIFFICULTY_BUTTON_GAP), y, buttonWidth, buttonHeight);
    }

    private boolean handleDifficultySelectionClick(int mouseX, int mouseY, int viewWidth) {
        DifficultyProfile[] profiles = DifficultyProfile.values();
        for (int i = 0; i < profiles.length; i++) {
            if (getDifficultyButtonBounds(i, viewWidth).contains(mouseX, mouseY)) {
                settings.applyDifficultyProfile(profiles[i]);
                return true;
            }
        }
        return false;
    }

    private Rectangle getMapCardBounds(int index, int total, int viewWidth) {
        int cardHeight = 130;
        int maxCardWidth = 250;
        int availableWidth = viewWidth - 120 - (total - 1) * MENU_MAP_CARD_GAP;
        int cardWidth = Math.max(150, Math.min(maxCardWidth, availableWidth / total));
        int totalWidth = total * cardWidth + (total - 1) * MENU_MAP_CARD_GAP;
        int startX = (viewWidth - totalWidth) / 2;
        int y = 140;
        return new Rectangle(startX + index * (cardWidth + MENU_MAP_CARD_GAP), y, cardWidth, cardHeight);
    }

    private Rectangle getMapPreviewImageBounds(Rectangle cardBounds) {
        int margin = 8;
        return new Rectangle(
                cardBounds.x + margin,
                cardBounds.y + margin,
                cardBounds.width - margin * 2,
                cardBounds.height - margin * 2 - 26
        );
    }

    private boolean handleMapSelectionClick(int mouseX, int mouseY, int viewWidth) {
        MapType[] mapTypes = tileManager.getAvailableMapTypes();
        for (int i = 0; i < mapTypes.length; i++) {
            Rectangle card = getMapCardBounds(i, mapTypes.length, viewWidth);
            if (card.contains(mouseX, mouseY)) {
                tileManager.setCurrentMapType(mapTypes[i]);
                return true;
            }
        }

        return false;
    }

    // hostControlled : affichage du client LAN, dont la configuration est choisie par l'hote.
    void draw(Graphics2D g2d, int panelWidth, int panelHeight, boolean hostControlled) {
        g2d.setColor(new Color(0, 0, 0, 170));
        g2d.fillRect(0, 0, panelWidth, panelHeight);

        Font oldFont = g2d.getFont();
        g2d.setColor(Color.WHITE);
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 44f));
        String title = "SIM ENGINE";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (panelWidth - fm.stringWidth(title)) / 2, 88);

        g2d.setFont(oldFont.deriveFont(Font.BOLD, 18f));
        String mapTitle = hostControlled ? "Carte choisie par l'hote" : "Choisis ta carte";
        FontMetrics mapFm = g2d.getFontMetrics();
        g2d.drawString(mapTitle, (panelWidth - mapFm.stringWidth(mapTitle)) / 2, 118);

        MapType[] mapTypes = tileManager.getAvailableMapTypes();
        for (int i = 0; i < mapTypes.length; i++) {
            Rectangle card = getMapCardBounds(i, mapTypes.length, panelWidth);
            Rectangle preview = getMapPreviewImageBounds(card);
            boolean selected = mapTypes[i] == tileManager.getCurrentMapType();

            g2d.setColor(new Color(24, 24, 24, 225));
            g2d.fillRoundRect(card.x, card.y, card.width, card.height, 16, 16);
            tileManager.drawPreview(g2d, preview, mapTypes[i]);

            g2d.setColor(selected ? new Color(255, 216, 92) : new Color(225, 225, 225));
            g2d.setStroke(new BasicStroke(selected ? 3f : 2f));
            g2d.drawRoundRect(card.x, card.y, card.width, card.height, 16, 16);

            g2d.setFont(oldFont.deriveFont(Font.BOLD, 15f));
            FontMetrics cardFm = g2d.getFontMetrics();
            String label = mapTypes[i].getDisplayName();
            int labelX = card.x + (card.width - cardFm.stringWidth(label)) / 2;
            int labelY = card.y + card.height - 7;
            g2d.drawString(label, labelX, labelY);
        }

        Font oldDifficultyFont = g2d.getFont();
        g2d.setFont(oldDifficultyFont.deriveFont(Font.BOLD, 16f));
        String difficultyTitle = "Difficulte";
        FontMetrics diffTitleMetrics = g2d.getFontMetrics();
        g2d.setColor(new Color(238, 238, 238));
        g2d.drawString(difficultyTitle, (panelWidth - diffTitleMetrics.stringWidth(difficultyTitle)) / 2, 372);
        drawDifficultyButton(g2d, getDifficultyButtonBounds(0, panelWidth), DifficultyProfile.EASY);
        drawDifficultyButton(g2d, getDifficultyButtonBounds(1, panelWidth), DifficultyProfile.NORMAL);
        drawDifficultyButton(g2d, getDifficultyButtonBounds(2, panelWidth), DifficultyProfile.CUSTOM);
        g2d.setFont(oldDifficultyFont);

        UiPainter.drawButton(g2d, getFreeModeButtonBounds(panelWidth, panelHeight), "Mode Libre");
        UiPainter.drawButton(g2d, getArcadeModeButtonBounds(panelWidth, panelHeight), "Mode Arcade");
        UiPainter.drawButton(g2d, getStoryModeButtonBounds(panelWidth, panelHeight), "Mode Histoire");
        UiPainter.drawButton(g2d, getMissionModeButtonBounds(panelWidth, panelHeight), "Mode Mission");
        UiPainter.drawButton(g2d, getProtectionModeButtonBounds(panelWidth, panelHeight), "Mode Protection");
        UiPainter.drawButton(g2d, getOptionsButtonBounds(panelWidth, panelHeight), "Options");

        g2d.setFont(oldFont.deriveFont(Font.PLAIN, 16f));
        String hint = hostControlled
                ? "Configuration controlee par l'hote"
                : "Clique sur une carte puis sur un mode";
        FontMetrics hintFm = g2d.getFontMetrics();
        g2d.drawString(hint, (panelWidth - hintFm.stringWidth(hint)) / 2, panelHeight - 26);
        g2d.setFont(oldFont);
    }

    private void drawDifficultyButton(Graphics2D g2d, Rectangle rect, DifficultyProfile profile) {
        boolean selected = settings.getDifficulty() == profile;
        int y = rect.y + DIFFICULTY_DRAW_OFFSET_Y;
        g2d.setColor(selected ? new Color(180, 132, 54, 220) : new Color(32, 32, 32, 218));
        g2d.fillRoundRect(rect.x, y, rect.width, rect.height, 11, 11);
        g2d.setColor(selected ? new Color(255, 226, 148) : new Color(214, 214, 214));
        g2d.setStroke(new BasicStroke(selected ? 2.4f : 1.6f));
        g2d.drawRoundRect(rect.x, y, rect.width, rect.height, 11, 11);

        Font oldFont = g2d.getFont();
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 15f));
        FontMetrics fm = g2d.getFontMetrics();
        String label = profile.getLabel();
        int textX = rect.x + (rect.width - fm.stringWidth(label)) / 2;
        int textY = y + (rect.height - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(label, textX, textY);
        g2d.setFont(oldFont);
    }
}
