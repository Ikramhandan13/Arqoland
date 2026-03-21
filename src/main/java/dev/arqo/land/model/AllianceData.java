package dev.arqo.land.model;

public class AllianceData {
    private final int landId;
    private final int allyLandId;

    public AllianceData(int landId, int allyLandId) {
        this.landId = landId;
        this.allyLandId = allyLandId;
    }

    public int getLandId() { return landId; }
    public int getAllyLandId() { return allyLandId; }
}
