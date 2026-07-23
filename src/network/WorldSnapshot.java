package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class WorldSnapshot {
    public static final class Entity {
        public byte type;
        public byte variant;
        public double x;
        public double y;
        public double vx;
        public double vy;
        public double facingX;
        public double facingY;
        public boolean localPlayer;
    }

    public int mapType;
    public int gameMode;
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
    public final List<Entity> entities = new ArrayList<>();

    public void write(DataOutputStream output) throws IOException {
        output.writeInt(mapType);
        output.writeInt(gameMode);
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
        output.writeInt(entities.size());
        for (Entity entity : entities) {
            output.writeByte(entity.type);
            output.writeByte(entity.variant);
            output.writeDouble(entity.x);
            output.writeDouble(entity.y);
            output.writeDouble(entity.vx);
            output.writeDouble(entity.vy);
            output.writeDouble(entity.facingX);
            output.writeDouble(entity.facingY);
            output.writeBoolean(entity.localPlayer);
        }
    }

    public static WorldSnapshot read(DataInputStream input) throws IOException {
        WorldSnapshot snapshot = new WorldSnapshot();
        snapshot.mapType = input.readInt();
        snapshot.gameMode = input.readInt();
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
        int count = input.readInt();
        if (count < 0 || count > 10_000) throw new IOException("instantane invalide");
        for (int i = 0; i < count; i++) {
            Entity entity = new Entity();
            entity.type = input.readByte();
            entity.variant = input.readByte();
            entity.x = input.readDouble();
            entity.y = input.readDouble();
            entity.vx = input.readDouble();
            entity.vy = input.readDouble();
            entity.facingX = input.readDouble();
            entity.facingY = input.readDouble();
            entity.localPlayer = input.readBoolean();
            snapshot.entities.add(entity);
        }
        return snapshot;
    }
}
