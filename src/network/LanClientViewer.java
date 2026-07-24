package network;

import gameController.GameKeyController;
import main.GamePanel;

import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;

public final class LanClientViewer extends GamePanel implements Runnable {
    private static final String MAGIC = "SIM_ENGINE_LAN_6";
    private final String host;
    private final int port;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private volatile Socket socket;

    public LanClientViewer(String host, int port) {
        this.host = host;
        this.port = port;
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
        configureNetworkReplicaView();
    }

    public GameKeyController getControls() {
        return getKeyController();
    }

    @Override
    public void run() {
        try (Socket connected = new Socket()) {
            socket = connected;
            connected.connect(new InetSocketAddress(host, port), 5000);
            connected.setTcpNoDelay(true);
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(connected.getOutputStream()));
            DataInputStream input = new DataInputStream(new BufferedInputStream(connected.getInputStream()));
            output.writeUTF(MAGIC);
            output.flush();
            if (!MAGIC.equals(input.readUTF())) throw new IOException("hote incompatible");
            setNetworkConnectionStatus("Connecte a " + host + " - synchronisation...");

            Thread sender = new Thread(() -> sendInputs(output), "lan-client-input");
            sender.setDaemon(true);
            sender.start();

            while (running.get()) {
                WorldSnapshot snapshot = WorldSnapshot.read(input);
                SwingUtilities.invokeLater(() -> applyNetworkSnapshot(snapshot));
            }
        } catch (IOException e) {
            showNetworkConnectionError("Connexion impossible/perdue : " + e.getMessage());
        }
    }

    private void sendInputs(DataOutputStream output) {
        try {
            while (running.get()) {
                GameKeyController controls = getControls();
                output.writeBoolean(controls.isUp());
                output.writeBoolean(controls.isDown());
                output.writeBoolean(controls.isLeft());
                output.writeBoolean(controls.isRight());
                output.writeBoolean(controls.isSprint());
                output.writeBoolean(controls.isLeftClickPressed());
                output.writeBoolean(controls.consumeLeftClickPressed());
                output.writeBoolean(controls.consumeReloadTriggered());
                output.writeBoolean(controls.consumeInteractTriggered());
                output.writeInt(controls.getMouseX());
                output.writeInt(controls.getMouseY());
                output.writeInt(controls.consumeWeaponScrollDelta());
                output.writeInt(Math.max(1, getWidth()));
                output.writeInt(Math.max(1, getHeight()));
                output.flush();
                Thread.sleep(16);
            }
        } catch (IOException ignored) {
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void close() {
        running.set(false);
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

}
