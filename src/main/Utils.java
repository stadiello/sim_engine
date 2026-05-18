package main;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class Utils {

	private static final SoundPool LASER_POOL = new SoundPool("/sound/laser.wav", 4);
	private static final SoundPool SMG_POOL = new SoundPool("/sound/smg.wav", 10);
	private static final SoundPool PISTOL_POOL = new SoundPool("/sound/pistol.wav", 5);
	private static final SoundPool SHOTGUN_POOL = new SoundPool("/sound/shotGun.wav", 3);

	private Utils() {}

	public static void playLaserSound() {
		LASER_POOL.play();
	}

	public static void playSmgSound() {
		SMG_POOL.play();
	}

	public static void playPistolSound() {
		PISTOL_POOL.play();
	}

	public static void playShotgunSound() {
		SHOTGUN_POOL.play();
	}

	private static final class SoundPool {
		private final List<Clip> clips = new ArrayList<>();
		private int roundRobinIndex = 0;

		SoundPool(String resourcePath, int poolSize) {
			for (int i = 0; i < poolSize; i++) {
				Clip clip = createClip(resourcePath);
				if (clip != null) {
					clips.add(clip);
				}
			}
		}

		synchronized void play() {
			if (clips.isEmpty()) {
				return;
			}

			Clip clip = findAvailableClip();
			if (clip == null) {
				clip = clips.get(roundRobinIndex);
				roundRobinIndex = (roundRobinIndex + 1) % clips.size();
				clip.stop();
			}

			clip.setFramePosition(0);
			clip.start();
		}

		private Clip findAvailableClip() {
			for (Clip clip : clips) {
				if (!clip.isRunning()) {
					return clip;
				}
			}
			return null;
		}

		private Clip createClip(String resourcePath) {
			URL url = Utils.class.getResource(resourcePath);
			if (url == null) {
				return null;
			}

			try (AudioInputStream stream = AudioSystem.getAudioInputStream(url)) {
				Clip clip = AudioSystem.getClip();
				clip.open(stream);
				return clip;
			} catch (Exception e) {
				return null;
			}
		}
	}
}
