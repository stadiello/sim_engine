package network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

// Verifie que write() et read() restent symetriques : un champ ajoute d'un seul cote casse ce test.
public final class WorldSnapshotTest {

    public static void main(String[] args) throws IOException {
        WorldSnapshot original = new WorldSnapshot();
        original.screenState = 2;
        original.mapType = 3;
        original.score = 1234;
        original.suppressionCoverBoost = 0.25;
        original.playerX = 812.5;
        original.playerY = 410.25;
        original.weaponName = "Carabine";
        original.ammo = 17;
        original.reloading = true;
        original.soundCounters[1] = 42;

        WorldSnapshot.TileMutation mutation = new WorldSnapshot.TileMutation();
        mutation.row = 4;
        mutation.col = 9;
        mutation.tileId = 6;
        mutation.damage = 3;
        original.tileMutations.add(mutation);

        WorldSnapshot.Entity entity = new WorldSnapshot.Entity();
        entity.id = 77;
        entity.type = NetworkReplica.ENEMY;
        entity.variant = 2;
        entity.x = 1500.5;
        entity.y = 320.75;
        entity.facingX = 0.6;
        entity.facingY = -0.8;
        entity.localPlayer = true;
        entity.detail = "Shotgun";
        entity.amount = 5;
        entity.visualState = new double[]{1.5, 2.25};
        original.entities.add(entity);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        original.write(new DataOutputStream(bytes));
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        WorldSnapshot copy = WorldSnapshot.read(input);

        check(input.available() == 0, "octets non lus apres read()");
        check(copy.screenState == 2 && copy.mapType == 3 && copy.score == 1234, "en-tete");
        check(copy.suppressionCoverBoost == 0.25, "reglages IA");
        check(copy.playerX == 812.5 && copy.playerY == 410.25, "position du joueur");
        check(copy.weaponName.equals("Carabine") && copy.ammo == 17 && copy.reloading, "HUD");
        check(copy.soundCounters[1] == 42, "compteurs de sons");
        check(copy.tileMutations.size() == 1, "nombre de tuiles");
        WorldSnapshot.TileMutation tile = copy.tileMutations.get(0);
        check(tile.row == 4 && tile.col == 9 && tile.tileId == 6 && tile.damage == 3, "tuile");
        check(copy.entities.size() == 1, "nombre d'entites");
        WorldSnapshot.Entity read = copy.entities.get(0);
        check(read.id == 77 && read.type == NetworkReplica.ENEMY && read.variant == 2, "identite");
        check(close(read.x, 1500.5) && close(read.y, 320.75), "position de l'entite");
        check(close(read.facingX, 0.6) && close(read.facingY, -0.8), "orientation");
        check(read.localPlayer && read.detail.equals("Shotgun") && read.amount == 5, "details");
        check(read.visualState.length == 2 && close(read.visualState[1], 2.25), "etat visuel");

        System.out.println("WorldSnapshotTest OK (" + bytes.size() + " octets)");
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) < 1e-3;
    }

    private static void check(boolean condition, String what) {
        if (!condition) throw new AssertionError("WorldSnapshot : " + what);
    }
}
