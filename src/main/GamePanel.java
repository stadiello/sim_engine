package main;

import javax.swing.*;
import java.awt.*;
import java.awt.KeyboardFocusManager;
import java.util.ArrayList;

import gameController.GameKeyController;
import gameController.RemotePlayerInput;
import network.LanClientViewer;
import network.LanHost;
import network.WorldSnapshot;
import object.GameObject;
import object.DesertTurret;
import object.DeathMarker;
import object.Protagonist;
import object.Soldat;
import object.ObjectManager;
import world.TileManager;
import world.TileManager.MapType;

/**
 * Fenetre de jeu : boucle de simulation, enchainement des ecrans, joueur LAN distant et rendu.
 * Les regles propres a chaque mode, les menus, la HUD et la synchronisation reseau sont
 * deleguees aux classes du paquet main (MissionDirector, ArcadeWaves, MenuScreen, HudRenderer...).
 */
public class GamePanel extends JPanel implements Runnable {

    static final int TARGET_UPS = 60;
    private static final long NANOS_PER_UPDATE = 1_000_000_000L / TARGET_UPS;

    // 50 correspond aux dimensions de la carte (800÷16=50, 600÷12=50), ce qui permet un affichage correct des tiles sur l'écran 800×600.
    public int tileSize = 50;

    TileManager tileManager = new TileManager(this);
    private final GameSession session = new GameSession();
    private final SpawnLocator spawnLocator = new SpawnLocator(tileManager, tileSize);
    private final WorldSpawner worldSpawner = new WorldSpawner(tileManager, tileSize, session, spawnLocator);
    private final MissionDirector missionDirector = new MissionDirector(tileManager, tileSize, session, spawnLocator);
    private final ArcadeWaves arcadeWaves = new ArcadeWaves(session, spawnLocator);
    private final ProtectionEscort protectionEscort = new ProtectionEscort(tileManager, tileSize);
    private final NightOverlay nightOverlay = new NightOverlay(tileManager);
    private final MenuScreen menuScreen = new MenuScreen(tileManager, session.getSettings());
    private final OptionsScreen optionsScreen = new OptionsScreen(session.getSettings());
    private final HudRenderer hud = new HudRenderer(tileManager, session, missionDirector, arcadeWaves, protectionEscort);
    private final NetworkSync networkSync = new NetworkSync(tileManager, session);
    private final GameKeyController keyController = new GameKeyController();
    private volatile RemotePlayerInput remotePlayerInput;
    private volatile Protagonist remoteProtagonist;
    private volatile LanHost lanHost;
    private volatile WorldSnapshot networkSnapshot;
    private volatile boolean networkReplicaView;
    private volatile String networkConnectionStatus = "Connexion a l'hote...";
    private ScreenState screenState = ScreenState.MENU;
    private boolean paused = false;
    private String gameOverTitle = "VOUS ETES MORT";

    public GamePanel() {
        ObjectManager.setSession(session);
        ObjectManager.setTileManager(tileManager);
        addMouseMotionListener(keyController);
        addMouseListener(keyController);
        addMouseWheelListener(keyController);

        initializeWorld();
        updateCamera();
    }

    public void startLanHost(int port) {
        if (lanHost != null) return;
        lanHost = new LanHost(this, port);
        lanHost.start();
    }

    public GameKeyController getKeyController() {
        return keyController;
    }

    public void configureNetworkReplicaView() {
        networkReplicaView = true;
        screenState = ScreenState.MENU;
        ObjectManager.list.clear();
    }

    public void setNetworkConnectionStatus(String status) {
        networkConnectionStatus = status == null ? "" : status;
        repaint();
    }

    public void showNetworkConnectionError(String status) {
        networkSnapshot = null;
        setNetworkConnectionStatus(status);
    }

    public void applyNetworkSnapshot(WorldSnapshot snapshot) {
        if (snapshot == null) return;
        networkSync.applySnapshot(snapshot);
        networkSnapshot = snapshot;
        ScreenState[] states = ScreenState.values();
        if (snapshot.screenState >= 0 && snapshot.screenState < states.length) {
            screenState = states[snapshot.screenState];
        }
        tileManager.centerCameraOn(snapshot.playerX, snapshot.playerY, getViewWidth(), getViewHeight());
        repaint();
    }

    private void publishRemoteSnapshot() {
        LanHost host = lanHost;
        if (host != null && host.hasClient()) {
            host.publishSnapshot(networkSync.createSnapshot(screenState, remoteProtagonist));
        }
    }

    public void onRemotePlayerConnected(RemotePlayerInput input) {
        remotePlayerInput = input;
        addRemoteProtagonistIfNeeded();
    }

    public void onRemotePlayerDisconnected() {
        Protagonist disconnected = remoteProtagonist;
        remoteProtagonist = null;
        remotePlayerInput = null;
        if (disconnected != null) ObjectManager.list.remove(disconnected);
    }

    private void addRemoteProtagonistIfNeeded() {
        if (remotePlayerInput == null || remoteProtagonist != null) return;
        Protagonist primary = ObjectManager.getProtagonist();
        double x = primary != null ? primary.x + tileSize : tileSize * 7.0;
        double y = primary != null ? primary.y : tileSize * 6.0;
        if (!spawnLocator.isSpawnAreaFree(x, y, SpawnLocator.ENTITY_RADIUS)) {
            double[] spawn = spawnLocator.getFreeSpawnPosition();
            x = spawn[0];
            y = spawn[1];
        }
        remoteProtagonist = new Protagonist(x, y, remotePlayerInput, false);
        ObjectManager.list.add(remoteProtagonist);
    }

    private void initializeWorld() {
        ObjectManager.list.clear();
        session.setScore(0);
        gameOverTitle = "VOUS ETES MORT";
        arcadeWaves.reset();
        session.getScreenFeedback().reset();
        worldSpawner.resetAlienRespawnCooldown();
        missionDirector.reset();

        double[] protagonistSpawn = worldSpawner.spawnCiviliansAndPlayer(keyController);
        double protagonistSpawnX = protagonistSpawn[0];
        double protagonistSpawnY = protagonistSpawn[1];
        remoteProtagonist = null;
        addRemoteProtagonistIfNeeded();

        worldSpawner.spawnSquadAndHostiles(protagonistSpawnX, protagonistSpawnY);

        if (session.isMode(GameMode.MISSION)) {
            missionDirector.setup(protagonistSpawnX, protagonistSpawnY);
        }
    }

    private void startGame(GameMode mode) {
        session.setMode(mode);
        initializeWorld();
        screenState = ScreenState.PLAYING;
        paused = false;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> launch(args));
    }

    private static void launch(String[] args) {
        String action = args.length > 0 ? args[0] : "";
        if (action.isEmpty()) {
            Object[] choices = {"Solo", "Heberger en LAN", "Rejoindre"};
            int selected = JOptionPane.showOptionDialog(
                    null, "Comment veux-tu jouer ?", "SIM ENGINE",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, choices, choices[0]
            );
            if (selected < 0) return;
            action = selected == 1 ? "--host" : selected == 2 ? "--join" : "--solo";
        }

        if ("--join".equals(action)) {
            String defaultHost = args.length > 1 ? args[1] : "192.168.1.2";
            String host = args.length > 1 ? defaultHost : JOptionPane.showInputDialog(
                    null, "Adresse IP de l'hote :", defaultHost
            );
            if (host == null || host.isBlank()) return;
            launchClient(host.trim());
            return;
        }

        JFrame frame = new JFrame("--host".equals(action) ? "SIM ENGINE - Hote LAN" : "Simulation");
        GamePanel gamePanel = new GamePanel();
        frame.add(gamePanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(gamePanel.keyController);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        if ("--host".equals(action)) gamePanel.startLanHost(LanHost.DEFAULT_PORT);
        new Thread(gamePanel, "game-loop").start();
    }

    private static void launchClient(String host) {
        JFrame frame = new JFrame("SIM ENGINE - Joueur 2");
        LanClientViewer viewer = new LanClientViewer(host, LanHost.DEFAULT_PORT);
        frame.add(viewer);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(viewer.getControls());
        frame.setVisible(true);
        new Thread(viewer, "lan-client").start();
    }

    public void run() {
        long previousTime = System.nanoTime();
        long accumulator = 0L;

        while (true) {
            long currentTime = System.nanoTime();
            long elapsed = currentTime - previousTime;
            previousTime = currentTime;
            accumulator += elapsed;

            int updates = 0;
            while (accumulator >= NANOS_PER_UPDATE && updates < 5) {
                updateGameStep();
                accumulator -= NANOS_PER_UPDATE;
                updates++;
            }

            // Apres un blocage (GC, fenetre deplacee...), on abandonne le retard au lieu d'accelerer le jeu.
            if (accumulator >= NANOS_PER_UPDATE) {
                accumulator = NANOS_PER_UPDATE - 1;
            }

            handleMenuInput();
            publishRemoteSnapshot();

            repaint();

            long sleepNanos = NANOS_PER_UPDATE - accumulator;
            if (sleepNanos <= 0) {
                Thread.yield();
                continue;
            }

            try {
                long sleepMillis = sleepNanos / 1_000_000L;
                int sleepNanoPart = (int) (sleepNanos % 1_000_000L);
                if (sleepMillis > 0 || sleepNanoPart > 0) {
                    Thread.sleep(sleepMillis, sleepNanoPart);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void updateGameStep() {
        if (screenState == ScreenState.PLAYING && keyController.consumePauseToggleTriggered()) {
            paused = !paused;
        }

        session.getScreenFeedback().tick();

        if (screenState == ScreenState.PLAYING && !paused) {
            Protagonist protagonist = ObjectManager.getProtagonist();
            Protagonist interactionPlayer = consumeInteractionPlayer(protagonist);
            boolean interactionUsedByTurret = tryToggleDesertTurret(interactionPlayer);
            boolean interactionUsedByRevive = !interactionUsedByTurret && tryReviveAlly(interactionPlayer);
            updateRemoteInputCamera();
            applySoldierMoveCommand();
            ObjectManager.updateAll();
            worldSpawner.maintainAlienPresence();
            arcadeWaves.update();
            protagonist = ObjectManager.getProtagonist();
            if (protagonist == null) {
                endGame(MissionDirector.Outcome.FAILED, "VOUS ETES MORT");
                return;
            }

            if (session.isMode(GameMode.MISSION)) {
                MissionDirector.Outcome outcome = missionDirector.update(protagonist, interactionPlayer,
                        interactionPlayer != null && !interactionUsedByTurret && !interactionUsedByRevive);
                if (endGame(outcome, missionDirector.getFailureReason())) {
                    return;
                }
            }

            if (session.isMode(GameMode.PROTECTION)) {
                if (endGame(protectionEscort.update(protagonist), "MISSION ECHOUEE")) {
                    return;
                }
            }

            updateCamera();
        }
    }

    // Passe a l'ecran de victoire ou de defaite ; renvoie false si la partie continue.
    private boolean endGame(MissionDirector.Outcome outcome, String failureTitle) {
        switch (outcome) {
            case VICTORY -> screenState = ScreenState.VICTORY;
            case FAILED -> {
                gameOverTitle = failureTitle;
                screenState = ScreenState.GAME_OVER;
            }
            case ONGOING -> {
                return false;
            }
        }
        paused = false;
        return true;
    }

    private Protagonist consumeInteractionPlayer(Protagonist primaryPlayer) {
        if (keyController.consumeInteractTriggered()) {
            return primaryPlayer;
        }
        RemotePlayerInput remoteInput = remotePlayerInput;
        if (remoteInput != null && remoteInput.consumeInteractTriggered()) {
            return remoteProtagonist;
        }
        return null;
    }

    private boolean tryToggleDesertTurret(Protagonist player) {
        if (player == null) return false;
        for (GameObject object : new ArrayList<>(ObjectManager.list)) {
            if (object instanceof DesertTurret turret && turret.tryToggleOperator(player)) {
                return true;
            }
        }
        return false;
    }

    private boolean tryReviveAlly(Protagonist player) {
        if (player == null) return false;
        for (GameObject object : new ArrayList<>(ObjectManager.list)) {
            if (object instanceof DeathMarker marker && marker.tryRevive(player)) {
                return true;
            }
        }
        return false;
    }

    private void updateRemoteInputCamera() {
        Protagonist remote = remoteProtagonist;
        if (remote == null) return;
        RemotePlayerInput input = remotePlayerInput;
        int viewWidth = input != null ? input.getViewWidth() : getViewWidth();
        int viewHeight = input != null ? input.getViewHeight() : getViewHeight();
        int[] camera = tileManager.calculateCameraFor(remote.x, remote.y, viewWidth, viewHeight);
        remote.setIndependentCamera(camera[0], camera[1]);
    }

    private void handleMenuInput() {
        if (networkReplicaView) {
            return;
        }
        if (screenState == ScreenState.PLAYING && !paused) {
            return;
        }

        if (!keyController.consumeLeftClickPressed()) {
            return;
        }

        int mouseX = keyController.getMouseX();
        int mouseY = keyController.getMouseY();
        int viewWidth = getViewWidth();
        int viewHeight = getViewHeight();

        if (screenState == ScreenState.PLAYING && paused) {
            if (hud.getPauseMenuButtonBounds(viewWidth, viewHeight).contains(mouseX, mouseY)) {
                paused = false;
                screenState = ScreenState.MENU;
            }
            return;
        }

        if (screenState == ScreenState.MENU) {
            if (menuScreen.handleSettingsClick(mouseX, mouseY, viewWidth)) {
                return;
            }
            GameMode mode = menuScreen.getModeAt(mouseX, mouseY, viewWidth, viewHeight);
            if (mode != null) {
                startGame(mode);
            } else if (menuScreen.isOptionsButton(mouseX, mouseY, viewWidth, viewHeight)) {
                screenState = ScreenState.OPTIONS;
            }
            return;
        }

        if (screenState == ScreenState.OPTIONS) {
            if (optionsScreen.isBackButton(mouseX, mouseY, viewWidth, viewHeight)) {
                screenState = ScreenState.MENU;
                return;
            }
            if (optionsScreen.handleClick(mouseX, mouseY, viewWidth)) {
                return;
            }
        }

        if ((screenState == ScreenState.GAME_OVER || screenState == ScreenState.VICTORY)
                && hud.getReplayButtonBounds(viewWidth, viewHeight).contains(mouseX, mouseY)) {
            initializeWorld();
            screenState = ScreenState.PLAYING;
            paused = false;
        }
    }

    private void applySoldierMoveCommand() {
        if (!keyController.consumeRightClickTriggered()) {
            return;
        }

        Soldat soldat = ObjectManager.getSoldat();
        if (soldat == null) {
            return;
        }

        double worldX = keyController.getRightClickX() + tileManager.getCameraX();
        double worldY = keyController.getRightClickY() + tileManager.getCameraY();
        soldat.moveTo(worldX, worldY);
    }

    private void updateCamera() {
        Protagonist protagonist = ObjectManager.getProtagonist();
        if (protagonist == null) {
            return;
        }

        int viewWidth = getViewWidth();
        int viewHeight = getViewHeight();
        tileManager.centerCameraOn(protagonist.x, protagonist.y, viewWidth, viewHeight);
    }

    private Protagonist getRenderedPlayer() {
        return ObjectManager.getProtagonist();
    }

    private int getViewWidth() {
        return getWidth() > 0 ? getWidth() : 800;
    }

    private int getViewHeight() {
        return getHeight() > 0 ? getHeight() : 600;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        boolean playing = screenState == ScreenState.PLAYING;
        ScreenFeedback feedback = session.getScreenFeedback();
        var old = g2d.getTransform();
        int shakeX = 0;
        int shakeY = 0;
        if (feedback.isShaking() && playing) {
            shakeX = feedback.randomShakeOffset();
            shakeY = feedback.randomShakeOffset();
        }
        g2d.translate(-tileManager.getCameraX() + shakeX, -tileManager.getCameraY() + shakeY);
        tileManager.draw(g2d);
        if (playing && session.isMode(GameMode.PROTECTION)) {
            protectionEscort.drawExtractionZone(g2d);
        }
        if (playing && session.isMode(GameMode.MISSION)) {
            missionDirector.drawWorldMarkers(g2d);
        }
        ObjectManager.drawAll(g2d);
        g2d.setTransform(old);

        int viewWidth = getViewWidth();
        int viewHeight = getViewHeight();
        Protagonist player = getRenderedPlayer();
        WorldSnapshot replicated = networkReplicaView ? networkSnapshot : null;
        if (playing && tileManager.getCurrentMapType() == MapType.NIGHT_BLACKOUT) {
            nightOverlay.draw(g2d, viewWidth, viewHeight, player, replicated);
        }
        feedback.drawFlash(g2d, getWidth(), getHeight());
        if (playing && session.isMode(GameMode.ARCADE)) {
            arcadeWaves.drawBanner(g2d, getWidth());
        }
        hud.drawReloadAnimation(g2d, player, replicated);

        if (screenState == ScreenState.PLAYING) {
            LanHost host = lanHost;
            hud.drawUI(g2d, viewWidth, viewHeight, player, replicated,
                    host != null ? host.getStatus() : null, remoteProtagonist != null);
            if (paused) {
                hud.drawPauseOverlay(g2d, viewWidth, viewHeight);
            }
        } else if (screenState == ScreenState.GAME_OVER) {
            hud.drawGameOverOverlay(g2d, viewWidth, viewHeight, gameOverTitle);
        } else if (screenState == ScreenState.VICTORY) {
            hud.drawVictoryOverlay(g2d, viewWidth, viewHeight);
        } else if (screenState == ScreenState.MENU) {
            menuScreen.draw(g2d, viewWidth, viewHeight, networkReplicaView);
        } else if (screenState == ScreenState.OPTIONS) {
            optionsScreen.draw(g2d, viewWidth, viewHeight);
        }
        if (networkReplicaView && networkSnapshot == null) {
            hud.drawNetworkConnectionOverlay(g2d, viewWidth, viewHeight, networkConnectionStatus);
        }
    }

}
