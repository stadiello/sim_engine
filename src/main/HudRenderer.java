package main;

import java.awt.*;

import network.WorldSnapshot;
import object.Protagonist;
import world.TileManager;

/**
 * HUD en jeu (bandeau d'objectif, armes et plaques, rechargement, statut LAN)
 * et ecrans superposes : pause, fin de partie, victoire, connexion du client.
 */
final class HudRenderer {
    private final TileManager tileManager;
    private final GameSession session;
    private final MissionDirector missionDirector;
    private final ArcadeWaves arcadeWaves;
    private final ProtectionEscort protectionEscort;

    HudRenderer(TileManager tileManager, GameSession session, MissionDirector missionDirector,
                ArcadeWaves arcadeWaves, ProtectionEscort protectionEscort) {
        this.tileManager = tileManager;
        this.session = session;
        this.missionDirector = missionDirector;
        this.arcadeWaves = arcadeWaves;
        this.protectionEscort = protectionEscort;
    }

    /**
     * replicated : instantane recu par le client LAN (null cote hote ou solo).
     * lanStatus : statut de l'hote LAN a afficher (null hors hebergement).
     */
    void drawUI(Graphics2D g2d, int viewWidth, int viewHeight, Protagonist player, WorldSnapshot replicated,
                String lanStatus, boolean remoteConnected) {
        drawTopStatusBar(g2d, player);
        drawBottomRightHud(g2d, viewWidth, viewHeight, player, replicated);
        if (lanStatus != null) {
            Font oldFont = g2d.getFont();
            g2d.setFont(oldFont.deriveFont(Font.BOLD, 13f));
            FontMetrics metrics = g2d.getFontMetrics();
            int width = metrics.stringWidth(lanStatus) + 22;
            int x = (viewWidth - width) / 2;
            g2d.setColor(new Color(8, 14, 24, 205));
            g2d.fillRoundRect(x, 10, width, 28, 12, 12);
            g2d.setColor(remoteConnected ? new Color(120, 255, 160) : new Color(255, 214, 120));
            g2d.drawString(lanStatus, x + 11, 29);
            g2d.setFont(oldFont);
        }
    }

    private void drawTopStatusBar(Graphics2D g2d, Protagonist player) {
        int x = 16;
        int y = 14;
        int width = 330;
        int height = session.isMode(GameMode.MISSION) ? 118 : 86;

        UiPainter.drawPanelBackground(g2d, x, y, width, height, new Color(8, 14, 24, 190), new Color(108, 160, 220, 170));

        Font oldFont = g2d.getFont();
        g2d.setColor(new Color(136, 195, 255));
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 14f));
        g2d.drawString(getModeDisplayLabel(), x + 16, y + 24);

        g2d.setColor(Color.WHITE);
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 20f));
        g2d.drawString(getTopHudTitle(), x + 16, y + 50);

        g2d.setFont(oldFont.deriveFont(Font.PLAIN, 14f));
        g2d.setColor(new Color(214, 228, 244));
        int infoY = y + 72;
        for (String line : getTopHudDetails(player)) {
            g2d.drawString(line, x + 16, infoY);
            infoY += 18;
        }

        g2d.setFont(oldFont);
    }

    private String getModeDisplayLabel() {
        String modeLabel = switch (session.getMode()) {
            case FREE -> "MODE LIBRE";
            case ARCADE -> "MODE ARCADE";
            case STORY -> "MODE HISTOIRE";
            case PROTECTION -> "MODE PROTECTION";
            case MISSION -> "MODE MISSION";
        };
        return modeLabel + " - " + session.getSettings().getDifficulty().getLabel().toUpperCase();
    }

    private String getTopHudTitle() {
        String missionTitle = missionDirector.getTitle();
        if (session.isMode(GameMode.MISSION) && missionTitle != null && !missionTitle.isEmpty()) {
            return missionTitle;
        }
        if (session.isMode(GameMode.PROTECTION)) {
            return "Escorter jusqu'a extraction";
        }
        if (session.isMode(GameMode.ARCADE)) {
            return "Pression continue";
        }
        if (session.isMode(GameMode.STORY)) {
            return "Recuperation d'armement";
        }
        return "Survie tactique";
    }

    private String[] getTopHudDetails(Protagonist protagonist) {
        if (session.isMode(GameMode.MISSION)) {
            return missionDirector.getHudDetails();
        }
        if (session.isMode(GameMode.PROTECTION) && protagonist != null) {
            return protectionEscort.getHudDetails(protagonist);
        }
        if (session.isMode(GameMode.ARCADE)) {
            return arcadeWaves.getHudDetails();
        }
        if (session.isMode(GameMode.STORY)) {
            return new String[]{
                    "Ramasse les armes ennemies",
                    "Conserve tes chargeurs pour progresser"
            };
        }
        return new String[]{
                "Sprint : Shift",
                "Pause : P"
        };
    }

    private void drawBottomRightHud(Graphics2D g2d, int viewWidth, int viewHeight, Protagonist protagonist, WorldSnapshot replicated) {
        if (replicated != null) {
            drawNetworkPlayerHud(g2d, viewWidth, viewHeight, replicated);
            return;
        }
        if (protagonist == null) {
            return;
        }

        int panelWidth = 250;
        int panelHeight = protagonist.isReloading() ? 154 : 136;
        int x = viewWidth - panelWidth - 18;
        int y = viewHeight - panelHeight - 18;
        UiPainter.drawPanelBackground(g2d, x, y, panelWidth, panelHeight, new Color(10, 12, 20, 205), new Color(255, 208, 120, 165));

        Font oldFont = g2d.getFont();
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 13f));
        g2d.setColor(new Color(143, 188, 255));
        g2d.drawString("OPERATEUR", x + 16, y + 22);

        if (protagonist.getCurrentWeapon() != null) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(oldFont.deriveFont(Font.BOLD, 20f));
            g2d.drawString(protagonist.getCurrentWeapon().getName(), x + 16, y + 50);

            g2d.setColor(new Color(255, 222, 140));
            g2d.setFont(oldFont.deriveFont(Font.BOLD, 30f));
            String ammo = protagonist.getCurrentWeapon().getAmmoInMagazine() + " / " + protagonist.getCurrentWeapon().getReserveAmmo();
            g2d.drawString(ammo, x + 16, y + 84);

            g2d.setFont(oldFont.deriveFont(Font.PLAIN, 12f));
            g2d.setColor(new Color(188, 200, 218));
            g2d.drawString("CHARGEUR / RESERVE", x + 18, y + 100);
        }

        int plateBaseX = x + 16;
        int plateY = y + panelHeight - 34;
        for (int i = 0; i < protagonist.getMaxArmorPlates(); i++) {
            int plateX = plateBaseX + i * 24;
            Color fill = i < protagonist.getArmorPlates() ? new Color(108, 214, 255) : new Color(36, 50, 68);
            Color border = i < protagonist.getArmorPlates() ? new Color(184, 243, 255) : new Color(88, 102, 120);
            g2d.setColor(fill);
            g2d.fillRoundRect(plateX, plateY, 18, 18, 5, 5);
            g2d.setColor(border);
            g2d.drawRoundRect(plateX, plateY, 18, 18, 5, 5);
        }
        g2d.setColor(new Color(206, 220, 236));
        g2d.setFont(oldFont.deriveFont(Font.PLAIN, 12f));
        g2d.drawString("PLAQUES", plateBaseX + protagonist.getMaxArmorPlates() * 24 + 4, plateY + 14);

        if (protagonist.isReloading()) {
            int barX = x + 16;
            int barY = y + panelHeight - 60;
            int barWidth = panelWidth - 32;
            int barHeight = 10;
            double progress = protagonist.getReloadProgress();
            g2d.setColor(new Color(32, 40, 52));
            g2d.fillRoundRect(barX, barY, barWidth, barHeight, 8, 8);
            g2d.setColor(new Color(255, 176, 92));
            g2d.fillRoundRect(barX, barY, (int) Math.round(barWidth * progress), barHeight, 8, 8);
            g2d.setColor(new Color(255, 226, 188));
            g2d.drawRoundRect(barX, barY, barWidth, barHeight, 8, 8);
            g2d.drawString("RECHARGEMENT", barX, barY - 4);
        }

        g2d.setFont(oldFont);
    }

    private void drawNetworkPlayerHud(Graphics2D g2d, int viewWidth, int viewHeight, WorldSnapshot snapshot) {
        int panelWidth = 250;
        int panelHeight = snapshot.reloading ? 154 : 136;
        int x = viewWidth - panelWidth - 18;
        int y = viewHeight - panelHeight - 18;
        UiPainter.drawPanelBackground(g2d, x, y, panelWidth, panelHeight,
                new Color(10, 12, 20, 205), new Color(105, 225, 160, 180));
        Font oldFont = g2d.getFont();
        g2d.setColor(new Color(105, 235, 165));
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 13f));
        g2d.drawString("JOUEUR 2", x + 16, y + 22);
        g2d.setColor(Color.WHITE);
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 20f));
        g2d.drawString(snapshot.weaponName, x + 16, y + 50);
        g2d.setColor(new Color(255, 222, 140));
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 30f));
        g2d.drawString(snapshot.ammo + " / " + snapshot.reserveAmmo, x + 16, y + 84);
        int plateY = y + panelHeight - 34;
        for (int i = 0; i < snapshot.maxArmor; i++) {
            g2d.setColor(i < snapshot.armor ? new Color(108, 214, 255) : new Color(36, 50, 68));
            g2d.fillRoundRect(x + 16 + i * 24, plateY, 18, 18, 5, 5);
        }
        if (snapshot.reloading) {
            g2d.setColor(new Color(32, 40, 52));
            g2d.fillRoundRect(x + 16, y + panelHeight - 60, panelWidth - 32, 10, 8, 8);
            g2d.setColor(new Color(255, 176, 92));
            g2d.fillRoundRect(x + 16, y + panelHeight - 60,
                    (int) Math.round((panelWidth - 32) * snapshot.reloadProgress), 10, 8, 8);
        }
        g2d.setFont(oldFont);
    }

    // Anneau de progression au-dessus du joueur pendant le rechargement.
    void drawReloadAnimation(Graphics2D g2d, Protagonist protagonist, WorldSnapshot replicated) {
        if (replicated != null) {
            drawNetworkReloadAnimation(g2d, replicated);
            return;
        }
        if (protagonist == null || !protagonist.isReloading()) {
            return;
        }

        int cameraX = tileManager.getCameraX();
        int cameraY = tileManager.getCameraY();
        int screenX = (int) Math.round(protagonist.x - cameraX);
        int screenY = (int) Math.round(protagonist.y - cameraY - 34);
        int radius = 18;
        double progress = protagonist.getReloadProgress();

        Composite oldComposite = g2d.getComposite();
        Stroke oldStroke = g2d.getStroke();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.92f));
        g2d.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(new Color(34, 40, 50, 190));
        g2d.drawOval(screenX - radius, screenY - radius, radius * 2, radius * 2);
        g2d.setColor(new Color(255, 194, 108));
        g2d.drawArc(screenX - radius, screenY - radius, radius * 2, radius * 2, 90, (int) Math.round(-360 * progress));
        g2d.setComposite(oldComposite);
        g2d.setStroke(oldStroke);
    }

    private void drawNetworkReloadAnimation(Graphics2D g2d, WorldSnapshot snapshot) {
        if (!snapshot.reloading) return;
        int screenX = (int) Math.round(snapshot.playerX - tileManager.getCameraX());
        int screenY = (int) Math.round(snapshot.playerY - tileManager.getCameraY() - 34);
        g2d.setColor(new Color(255, 194, 108));
        g2d.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawArc(screenX - 18, screenY - 18, 36, 36, 90,
                (int) Math.round(-360 * snapshot.reloadProgress));
    }

    Rectangle getPauseMenuButtonBounds(int viewWidth, int viewHeight) {
        return UiPainter.centeredButton(viewWidth, viewHeight / 2 + 70, 220, 48);
    }

    Rectangle getReplayButtonBounds(int viewWidth, int viewHeight) {
        return UiPainter.centeredButton(viewWidth, viewHeight / 2 + 30, 240, 56);
    }

    void drawPauseOverlay(Graphics2D g2d, int panelWidth, int panelHeight) {
        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.fillRect(0, 0, panelWidth, panelHeight);
        g2d.setColor(Color.WHITE);
        Font oldFont = g2d.getFont();
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 48f));
        String pausedText = "PAUSE";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(pausedText, (panelWidth - fm.stringWidth(pausedText)) / 2, panelHeight / 2);
        g2d.setFont(oldFont.deriveFont(Font.PLAIN, 18f));
        String hint = "Appuie sur P pour reprendre";
        FontMetrics hintFm = g2d.getFontMetrics();
        g2d.drawString(hint, (panelWidth - hintFm.stringWidth(hint)) / 2, panelHeight / 2 + 36);
        g2d.setFont(oldFont);

        UiPainter.drawButton(g2d, getPauseMenuButtonBounds(panelWidth, panelHeight), "Menu principal");
    }

    void drawGameOverOverlay(Graphics2D g2d, int panelWidth, int panelHeight, String title) {
        g2d.setColor(new Color(0, 0, 0, 170));
        g2d.fillRect(0, 0, panelWidth, panelHeight);

        Font oldFont = g2d.getFont();
        g2d.setColor(new Color(255, 80, 80));
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 54f));
        FontMetrics titleFm = g2d.getFontMetrics();
        g2d.drawString(title, (panelWidth - titleFm.stringWidth(title)) / 2, panelHeight / 2 - 40);

        g2d.setColor(Color.WHITE);
        g2d.setFont(oldFont.deriveFont(Font.PLAIN, 22f));
        String scoreText = "Score final : " + session.getScore();
        FontMetrics scoreFm = g2d.getFontMetrics();
        g2d.drawString(scoreText, (panelWidth - scoreFm.stringWidth(scoreText)) / 2, panelHeight / 2);

        UiPainter.drawButton(g2d, getReplayButtonBounds(panelWidth, panelHeight), "Rejouer");
        g2d.setFont(oldFont);
    }

    void drawVictoryOverlay(Graphics2D g2d, int panelWidth, int panelHeight) {
        g2d.setColor(new Color(0, 0, 0, 165));
        g2d.fillRect(0, 0, panelWidth, panelHeight);

        Font oldFont = g2d.getFont();
        g2d.setColor(new Color(120, 255, 160));
        g2d.setFont(oldFont.deriveFont(Font.BOLD, 52f));
        String title = "MISSION REUSSIE";
        FontMetrics titleFm = g2d.getFontMetrics();
        g2d.drawString(title, (panelWidth - titleFm.stringWidth(title)) / 2, panelHeight / 2 - 44);

        g2d.setColor(Color.WHITE);
        g2d.setFont(oldFont.deriveFont(Font.PLAIN, 22f));
        String scoreText = "Score final : " + session.getScore();
        FontMetrics scoreFm = g2d.getFontMetrics();
        g2d.drawString(scoreText, (panelWidth - scoreFm.stringWidth(scoreText)) / 2, panelHeight / 2);

        UiPainter.drawButton(g2d, getReplayButtonBounds(panelWidth, panelHeight), "Rejouer");
        g2d.setFont(oldFont);
    }

    // Ecran noir du client LAN tant qu'aucun instantane n'a ete recu.
    void drawNetworkConnectionOverlay(Graphics2D g2d, int viewWidth, int viewHeight, String status) {
        g2d.setColor(new Color(0, 0, 0, 205));
        g2d.fillRect(0, 0, viewWidth, viewHeight);
        g2d.setColor(Color.WHITE);
        g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 20f));
        FontMetrics metrics = g2d.getFontMetrics();
        g2d.drawString(status,
                (viewWidth - metrics.stringWidth(status)) / 2,
                viewHeight / 2);
    }
}
