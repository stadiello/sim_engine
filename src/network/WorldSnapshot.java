package network;

import main.Utils;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class WorldSnapshot {
    public static final class TileMutation {
        public int row;
        public int col;
        public int tileId;
        public int damage;
    }

    public static final class Entity {
        public long id;
        public byte type;
        public byte variant;
        public double x;
        public double y;
        public double vx;
        public double vy;
        public double facingX;
        public double facingY;
        public boolean localPlayer;
        public String detail = "";
        public int amount;
        public double[] visualState = new double[0];
    }

    public int screenState;
    public int score;
    public double playerX;
    public double playerY;
    public double playerFacingX;
    public double playerFacingY;
    public String weaponName = "";
    public int ammo;
    public int reserveAmmo;
    public int armor;
    public int maxArmor;
    public boolean reloading;
    public double reloadProgress;
    public long[] soundCounters = new long[Utils.SOUND_COUNT];
    public final List<TileMutation> tileMutations = new ArrayList<>();
    public final List<Entity> entities = new ArrayList<>();

    public void write(DataOutputStream output) throws IOException {
        output.writeInt(screenState);
        output.writeInt(score);
        output.writeDouble(playerX);
        output.writeDouble(playerY);
        output.writeDouble(playerFacingX);
        output.writeDouble(playerFacingY);
        output.writeUTF(weaponName);
        output.writeInt(ammo);
        output.writeInt(reserveAmmo);
        output.writeInt(armor);
        output.writeInt(maxArmor);
        output.writeBoolean(reloading);
        output.writeDouble(reloadProgress);
        for (int i = 0; i < Utils.SOUND_COUNT; i++) output.writeLong(soundCounters[i]);
        output.writeInt(tileMutations.size());
        for (TileMutation mutation : tileMutations) {
            output.writeInt(mutation.row);
            output.writeInt(mutation.col);
            output.writeInt(mutation.tileId);
            output.writeInt(mutation.damage);
        }
        output.writeInt(entities.size());
        for (Entity entity : entities) {
            output.writeLong(entity.id);
            output.writeByte(entity.type);
            output.writeByte(entity.variant);
            output.writeDouble(entity.x);
            output.writeDouble(entity.y);
            output.writeDouble(entity.vx);
            output.writeDouble(entity.vy);
            output.writeDouble(entity.facingX);
            output.writeDouble(entity.facingY);
            output.writeBoolean(entity.localPlayer);
            output.writeUTF(entity.detail);
            output.writeInt(entity.amount);
            output.writeInt(entity.visualState.length);
            for (double value : entity.visualState) output.writeDouble(value);
        }
    }

    public static WorldSnapshot read(DataInputStream input) throws IOException {
        WorldSnapshot snapshot = new WorldSnapshot();
        snapshot.screenState = input.readInt();
        snapshot.score = input.readInt();
        snapshot.playerX = input.readDouble();
        snapshot.playerY = input.readDouble();
        snapshot.playerFacingX = input.readDouble();
        snapshot.playerFacingY = input.readDouble();
        snapshot.weaponName = input.readUTF();
        snapshot.ammo = input.readInt();
        snapshot.reserveAmmo = input.readInt();
        snapshot.armor = input.readInt();
        snapshot.maxArmor = input.readInt();
        snapshot.reloading = input.readBoolean();
        snapshot.reloadProgress = input.readDouble();
        for (int i = 0; i < Utils.SOUND_COUNT; i++) snapshot.soundCounters[i] = input.readLong();
        int mutationCount = input.readInt();
        if (mutationCount < 0 || mutationCount > 10_000) throw new IOException("mutations de carte invalides");
        for (int i = 0; i < mutationCount; i++) {
            TileMutation mutation = new TileMutation();
            mutation.row = input.readInt();
            mutation.col = input.readInt();
            mutation.tileId = input.readInt();
            mutation.damage = input.readInt();
            snapshot.tileMutations.add(mutation);
        }
        int count = input.readInt();
        if (count < 0 || count > 10_000) throw new IOException("instantane invalide");
        for (int i = 0; i < count; i++) {
            Entity entity = new Entity();
            entity.id = input.readLong();
            entity.type = input.readByte();
            entity.variant = input.readByte();
            entity.x = input.readDouble();
            entity.y = input.readDouble();
            entity.vx = input.readDouble();
            entity.vy = input.readDouble();
            entity.facingX = input.readDouble();
            entity.facingY = input.readDouble();
            entity.localPlayer = input.readBoolean();
            entity.detail = input.readUTF();
            entity.amount = input.readInt();
            int visualStateLength = input.readInt();
            if (visualStateLength < 0 || visualStateLength > 1_024) {
                throw new IOException("etat visuel invalide");
            }
            entity.visualState = new double[visualStateLength];
            for (int value = 0; value < visualStateLength; value++) {
                entity.visualState[value] = input.readDouble();
            }
            snapshot.entities.add(entity);
        }
        return snapshot;
    }
}
