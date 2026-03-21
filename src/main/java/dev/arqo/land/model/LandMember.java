package dev.arqo.land.model;

import java.util.UUID;

public class LandMember {
    private final UUID uuid;
    private String role;
    private int totalContributed;

    public LandMember(UUID uuid, String role) {
        this.uuid = uuid;
        this.role = role;
        this.totalContributed = 0;
    }

    public LandMember(UUID uuid, String role, int contributed) {
        this.uuid = uuid;
        this.role = role;
        this.totalContributed = contributed;
    }

    public UUID getUuid() { return uuid; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public int getTotalContributed() { return totalContributed; }
    public void addContribution(int amount) { this.totalContributed += amount; }
}
