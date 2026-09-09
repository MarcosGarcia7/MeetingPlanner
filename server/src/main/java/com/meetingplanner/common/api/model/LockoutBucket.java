package com.meetingplanner.common.api.model;

import java.time.Instant;

public class LockoutBucket {

    private int tier;
    private Instant lockedAt;
    private String reason;

    public LockoutBucket() {}
    public LockoutBucket(int tier, Instant lockedAt, String reason) {
        this.tier = tier;
        this.lockedAt = lockedAt;
        this.reason = reason;
    }

    public int getTier() { return tier; }
    public void setTier(int tier) { this.tier = tier; }
    public Instant getLockedAt() { return lockedAt; }
    public void setLockedAt(Instant lockedAt) { this.lockedAt = lockedAt; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
