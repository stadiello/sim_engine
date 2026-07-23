package main;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLongArray;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class Utils {
	public static final int SOUND_ELECTRIC = 0;
	public static final int SOUND_LASER = 1;
	public static final int SOUND_SMG = 2;
	public static final int SOUND_PISTOL = 3;
	public static final int SOUND_GRENADE = 4;
	public static final int SOUND_SHOTGUN = 5;
	public static final int SOUND_RELOAD = 6;
	public static final int SOUND_COUNT = 7;
	private static final AtomicLongArray SOUND_COUNTERS = new AtomicLongArray(SOUND_COUNT);

	private static final SoundPool ELECTRIC_POOL = new SoundPool("/sound/electric.wav", 4, 8_000_000L);
	private static final SoundPool LASER_POOL = new SoundPool("/sound/laser.wav", 4, 8_000_000L);
	private static final SoundPool SMG_POOL = new SoundPool("/sound/smg.wav", 10, 12_000_000L);
	private static final SoundPool PISTOL_POOL = new SoundPool("/sound/pistol.wav", 5, 20_000_000L);
	private static final SoundPool SHOTGUN_POOL = new SoundPool("/sound/shotGun.wav", 3, 45_000_000L);
	private static final SoundPool RELOAD_POOL = new SoundPool("/sound/shotGunReload.wav", 4, 55_000_000L);
	private static final SoundPool GRENADE_POOL = new SoundPool("/sound/explosion.wav", 2, 30_000_000L);
	private static final ExecutorService AUDIO_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "sim-engine-audio");
		thread.setDaemon(true);
		return thread;
	});

	private Utils() {}

	public static void playElectricSound() {
		playAndCount(SOUND_ELECTRIC, ELECTRIC_POOL);
	}

	public static void playLaserSound() {
		playAndCount(SOUND_LASER, LASER_POOL);
	}

	public static void playSmgSound() {
		playAndCount(SOUND_SMG, SMG_POOL);
	}

	public static void playPistolSound() {
		playAndCount(SOUND_PISTOL, PISTOL_POOL);
	}

	public static void playGrenadeSound() {
		playAndCount(SOUND_GRENADE, GRENADE_POOL);
	}

	public static void playShotgunSound() {
		playAndCount(SOUND_SHOTGUN, SHOTGUN_POOL);
	}

	public static void playReloadSound() {
		playAndCount(SOUND_RELOAD, RELOAD_POOL);
	}

	public static long[] getSoundCounters() {
		long[] counters = new long[SOUND_COUNT];
		for (int i = 0; i < SOUND_COUNT; i++) counters[i] = SOUND_COUNTERS.get(i);
		return counters;
	}

	public static void playReplicatedSound(int sound) {
		SoundPool pool = switch (sound) {
			case SOUND_ELECTRIC -> ELECTRIC_POOL;
			case SOUND_LASER -> LASER_POOL;
			case SOUND_SMG -> SMG_POOL;
			case SOUND_PISTOL -> PISTOL_POOL;
			case SOUND_GRENADE -> GRENADE_POOL;
			case SOUND_SHOTGUN -> SHOTGUN_POOL;
			case SOUND_RELOAD -> RELOAD_POOL;
			default -> null;
		};
		if (pool != null) dispatch(pool);
	}

	private static void playAndCount(int sound, SoundPool pool) {
		SOUND_COUNTERS.incrementAndGet(sound);
		dispatch(pool);
	}

	private static void dispatch(SoundPool pool) {
		try {
			AUDIO_EXECUTOR.execute(pool::play);
		} catch (RejectedExecutionException ignored) {
			// Ignore: en cas de saturation/extinction, on prefere dropper un son.
		}
	}

	private static final class SoundPool {
		private final List<Clip> clips = new ArrayList<>();
		private final long minReplayIntervalNanos;
		private int roundRobinIndex = 0;
		private long lastPlayNanos = 0L;
		private boolean hasPlayed = false;

		SoundPool(String resourcePath, int poolSize, long minReplayIntervalNanos) {
			this.minReplayIntervalNanos = Math.max(0L, minReplayIntervalNanos);
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

			long now = System.nanoTime();
			if (hasPlayed && now - lastPlayNanos < minReplayIntervalNanos) {
				return;
			}
			lastPlayNanos = now;
			hasPlayed = true;

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
			int count = clips.size();
			for (int i = 0; i < count; i++) {
				int idx = (roundRobinIndex + i) % count;
				Clip clip = clips.get(idx);
				if (!clip.isRunning()) {
					roundRobinIndex = (idx + 1) % count;
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
