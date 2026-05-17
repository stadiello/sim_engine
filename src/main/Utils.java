package main;

import java.net.URI;
import java.net.URL;

public class Utils {

	private Utils() {}

	public static void playLaserSound() {
		playSoundResource("/sound/laser.mp3");
	}

	private static void playSoundResource(String resourcePath) {
		URL soundUrl = Utils.class.getResource(resourcePath);
		if (soundUrl == null) {
			return;
		}

		Thread playerThread = new Thread(() -> {
			try {
				String osName = System.getProperty("os.name", "").toLowerCase();
				if (!osName.contains("mac")) {
					return;
				}

				URI uri = soundUrl.toURI();
				ProcessBuilder processBuilder = new ProcessBuilder("afplay", uri.getPath());
				processBuilder.start();
			} catch (Exception e) {
				// Ignore les erreurs audio pour ne jamais interrompre la boucle de jeu.
			}
		}, "laser-sound-player");

		playerThread.setDaemon(true);
		playerThread.start();
	}
}
