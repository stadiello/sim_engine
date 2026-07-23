package network;

import gameController.GameKeyController;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LanClientViewer extends JPanel implements Runnable {
    private static final String MAGIC = "SIM_ENGINE_LAN_1";
    private final String host;
    private final int port;
    private final GameKeyController controls = new GameKeyController();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private volatile BufferedImage latestFrame;
    private volatile String status = "Connexion a l'hote...";
    private volatile Socket socket;

    public LanClientViewer(String host, int port) {
        this.host = host;
        this.port = port;
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
        addMouseListener(controls);
        addMouseMotionListener(controls);
        addMouseWheelListener(controls);
    }

    public GameKeyController getControls() {
        return controls;
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
            status = "Connecte a " + host;

            Thread sender = new Thread(() -> sendInputs(output), "lan-client-input");
            sender.setDaemon(true);
            sender.start();

            while (running.get()) {
                int length = input.readInt();
                if (length <= 0 || length > 4_000_000) throw new IOException("image invalide");
                byte[] data = input.readNBytes(length);
                if (data.length != length) throw new EOFException();
                latestFrame = ImageIO.read(new ByteArrayInputStream(data));
                repaint();
            }
        } catch (IOException e) {
            status = "Connexion impossible/perdue : " + e.getMessage();
            repaint();
        }
    }

    private void sendInputs(DataOutputStream output) {
        try {
            while (running.get()) {
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        BufferedImage frame = latestFrame;
        if (frame != null) {
            g.drawImage(frame, 0, 0, getWidth(), getHeight(), null);
        } else {
            g.setColor(Color.WHITE);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 20f));
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(status, (getWidth() - metrics.stringWidth(status)) / 2, getHeight() / 2);
        }
    }
}
