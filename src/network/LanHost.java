package network;

import gameController.RemotePlayerInput;
import main.GamePanel;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class LanHost implements AutoCloseable {
    public static final int DEFAULT_PORT = 28765;
    private static final String MAGIC = "SIM_ENGINE_LAN_1";
    private final GamePanel panel;
    private final int port;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile ServerSocket serverSocket;
    private volatile Socket clientSocket;
    private volatile String status = "Hote LAN en attente";
    private final String localAddress = findLocalAddress();

    public LanHost(GamePanel panel, int port) {
        this.panel = panel;
        this.port = port;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) return;
        Thread acceptThread = new Thread(this::acceptLoop, "lan-host-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public String getStatus() {
        return status + " - " + localAddress + ":" + port;
    }

    private static String findLocalAddress() {
        try {
            for (NetworkInterface network : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!network.isUp() || network.isLoopback() || network.isVirtual()) continue;
                for (java.net.InetAddress address : Collections.list(network.getInetAddresses())) {
                    if (address instanceof Inet4Address && address.isSiteLocalAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (IOException ignored) {
        }
        return "IP locale";
    }

    private void acceptLoop() {
        try (ServerSocket server = new ServerSocket(port)) {
            serverSocket = server;
            status = "En attente du joueur 2";
            while (running.get()) {
                Socket socket = server.accept();
                socket.setTcpNoDelay(true);
                clientSocket = socket;
                handleClient(socket);
                clientSocket = null;
                if (running.get()) status = "Joueur 2 deconnecte - nouvelle attente";
            }
        } catch (IOException e) {
            if (running.get()) status = "Erreur reseau : " + e.getMessage();
        }
    }

    private void handleClient(Socket socket) {
        RemotePlayerInput remoteInput = new RemotePlayerInput();
        try {
            DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            if (!MAGIC.equals(input.readUTF())) {
                throw new IOException("client incompatible");
            }
            output.writeUTF(MAGIC);
            output.flush();
            status = "Joueur 2 connecte : " + socket.getInetAddress().getHostAddress();
            panel.onRemotePlayerConnected(remoteInput);

            Thread receiver = new Thread(() -> receiveInputs(input, remoteInput), "lan-host-input");
            receiver.setDaemon(true);
            receiver.start();

            while (running.get() && !socket.isClosed()) {
                BufferedImage image = capturePanel();
                byte[] jpeg = encodeJpeg(image);
                output.writeInt(jpeg.length);
                output.write(jpeg);
                output.flush();
                Thread.sleep(50);
            }
        } catch (IOException e) {
            if (running.get()) status = "Connexion perdue";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            panel.onRemotePlayerDisconnected();
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void receiveInputs(DataInputStream input, RemotePlayerInput remoteInput) {
        try {
            while (running.get()) {
                remoteInput.apply(
                        input.readBoolean(), input.readBoolean(), input.readBoolean(), input.readBoolean(),
                        input.readBoolean(), input.readBoolean(), input.readBoolean(), input.readBoolean(),
                        input.readBoolean(), input.readInt(), input.readInt(), input.readInt()
                );
            }
        } catch (IOException ignored) {
            try {
                if (clientSocket != null) clientSocket.close();
            } catch (IOException ignoredAgain) {}
        }
    }

    private BufferedImage capturePanel() throws IOException {
        AtomicReference<BufferedImage> result = new AtomicReference<>();
        Runnable capture = () -> result.set(panel.captureFrame());
        try {
            if (SwingUtilities.isEventDispatchThread()) capture.run();
            else SwingUtilities.invokeAndWait(capture);
        } catch (Exception e) {
            throw new IOException("capture impossible", e);
        }
        return result.get();
    }

    private byte[] encodeJpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(64 * 1024);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        ImageWriter writer = writers.next();
        try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(imageOutput);
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(0.68f);
            writer.write(null, new IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }

    @Override
    public void close() {
        running.set(false);
        try { if (clientSocket != null) clientSocket.close(); } catch (IOException ignored) {}
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
    }
}
