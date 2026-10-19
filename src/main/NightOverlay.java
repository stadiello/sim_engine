package main;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

import network.WorldSnapshot;
import object.Alien;
import object.Ennemi;
import object.GameObject;
import object.ObjectManager;
import object.Protagonist;
import world.TileManager;

/**
 * Carte de nuit : obscurite plein ecran percee par les lampes du joueur, des ennemis et des aliens.
 */
final class NightOverlay {
    private static final float NIGHT_DARKNESS_ALPHA = 0.86f;
    private static final double PLAYER_FLASHLIGHT_RANGE = 320;
    private static final double PLAYER_FLASHLIGHT_HALF_ANGLE = Math.toRadians(35);
    private static final double ENEMY_FLASHLIGHT_RANGE = 230;
    private static final double ENEMY_FLASHLIGHT_HALF_ANGLE = Math.toRadians(25);
    private static final double FLASHLIGHT_RAY_STEP = 8.0;

    private final TileManager tileManager;

    NightOverlay(TileManager tileManager) {
        this.tileManager = tileManager;
    }

    // replicated : instantane du client LAN, utilise quand aucun protagoniste local n'existe.
    void draw(Graphics2D g2d, int viewWidth, int viewHeight, Protagonist protagonist, WorldSnapshot replicated) {
        BufferedImage darkness = new BufferedImage(viewWidth, viewHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D darknessG = darkness.createGraphics();
        darknessG.setColor(new Color(0f, 0f, 0f, NIGHT_DARKNESS_ALPHA));
        darknessG.fillRect(0, 0, viewWidth, viewHeight);

        darknessG.setComposite(AlphaComposite.Clear);

        if (protagonist != null) {
            drawFlashlightCone(
                    darknessG,
                    protagonist.x,
                    protagonist.y,
                    protagonist.getFacingX(),
                    protagonist.getFacingY(),
                    PLAYER_FLASHLIGHT_RANGE,
                    PLAYER_FLASHLIGHT_HALF_ANGLE,
                    42
            );
        } else if (replicated != null) {
            drawFlashlightCone(
                    darknessG,
                    replicated.playerX,
                    replicated.playerY,
                    replicated.playerFacingX,
                    replicated.playerFacingY,
                    PLAYER_FLASHLIGHT_RANGE,
                    PLAYER_FLASHLIGHT_HALF_ANGLE,
                    42
            );
        }

        for (GameObject obj : ObjectManager.list) {
            if (obj instanceof Ennemi ennemi) {
                drawFlashlightCone(
                        darknessG,
                        ennemi.x,
                        ennemi.y,
                        ennemi.getFacingX(),
                        ennemi.getFacingY(),
                        ENEMY_FLASHLIGHT_RANGE,
                        ENEMY_FLASHLIGHT_HALF_ANGLE,
                        20
                );
            } else if (obj instanceof Alien alien) {
                drawFlashlightCone(
                        darknessG,
                        alien.x,
                        alien.y,
                        alien.vx,
                        alien.vy,
                        ENEMY_FLASHLIGHT_RANGE * 0.85,
                        ENEMY_FLASHLIGHT_HALF_ANGLE,
                        16
                );
            }
        }

        darknessG.dispose();
        g2d.drawImage(darkness, 0, 0, null);
    }

    private void drawFlashlightCone(
            Graphics2D g2d,
            double worldX,
            double worldY,
            double dirX,
            double dirY,
            double range,
            double halfAngle,
            int rays
    ) {
        double lenSq = dirX * dirX + dirY * dirY;
        if (lenSq <= 0.00001) {
            dirX = 0;
            dirY = -1;
            lenSq = 1;
        }

        double invLen = 1.0 / Math.sqrt(lenSq);
        dirX *= invLen;
        dirY *= invLen;

        double baseAngle = Math.atan2(dirY, dirX);
        int cameraX = tileManager.getCameraX();
        int cameraY = tileManager.getCameraY();
        double screenX = worldX - cameraX;
        double screenY = worldY - cameraY;

        Path2D.Double cone = new Path2D.Double();
        cone.moveTo(screenX, screenY);
        for (int i = 0; i <= rays; i++) {
            double t = i / (double) rays;
            double angle = baseAngle - halfAngle + t * halfAngle * 2.0;
            double rayDirX = Math.cos(angle);
            double rayDirY = Math.sin(angle);
            double hitDistance = range;

            for (double rayDistance = FLASHLIGHT_RAY_STEP; rayDistance <= range; rayDistance += FLASHLIGHT_RAY_STEP) {
                double sampleWorldX = worldX + rayDirX * rayDistance;
                double sampleWorldY = worldY + rayDirY * rayDistance;
                if (tileManager.isBlockedAtPixel(sampleWorldX, sampleWorldY)) {
                    hitDistance = Math.max(FLASHLIGHT_RAY_STEP, rayDistance - FLASHLIGHT_RAY_STEP * 0.55);
                    break;
                }
            }

            double px = screenX + rayDirX * hitDistance;
            double py = screenY + rayDirY * hitDistance;
            cone.lineTo(px, py);
        }
        cone.closePath();
        g2d.fill(cone);

        int centerRadius = 40;
        g2d.fillOval(
                (int) Math.round(screenX - centerRadius),
                (int) Math.round(screenY - centerRadius),
                centerRadius * 2,
                centerRadius * 2
        );
    }
}
