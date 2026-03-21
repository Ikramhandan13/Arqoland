package dev.arqo.land.model;

public class LandTurret {
    private final int id;
    private final String world;
    private final int x;
    private final int y;
    private final int z;

    public LandTurret(int id, String world, int x, int y, int z) {
        this.id = id;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getId() { return id; }
    public String getWorld() { return world; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
}
