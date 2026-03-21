package dev.arqo.land.core;

import dev.arqo.land.model.ClaimData;

public class FlagManager {

    public boolean canMobSpawn(ClaimData claimData) {
        if (claimData == null) return true;
        return claimData.isFlagMobSpawn();
    }

    public boolean canFireSpread(ClaimData claimData) {
        if (claimData == null) return true;
        return claimData.isFlagFireSpread();
    }

    public boolean canInteract(ClaimData claimData) {
        if (claimData == null) return true;
        return claimData.isFlagInteract();
    }

    public boolean canPistonMove(ClaimData claimData) {
        if (claimData == null) return true;
        return claimData.isFlagPiston();
    }
}
