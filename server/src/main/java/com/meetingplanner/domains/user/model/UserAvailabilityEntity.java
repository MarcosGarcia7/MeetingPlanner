package com.meetingplanner.domains.user.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_availability")
public class UserAvailabilityEntity {

    @Id
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "sunday", nullable = false)
    private int[][] sunday;

    @Column(name = "monday", nullable = false)
    private int[][] monday;

    @Column(name = "tuesday", nullable = false)
    private int[][] tuesday;

    @Column(name = "wednesday", nullable = false)
    private int[][] wednesday;

    @Column(name = "thursday", nullable = false)
    private int[][] thursday;

    @Column(name = "friday", nullable = false)
    private int[][] friday;

    @Column(name = "saturday", nullable = false)
    private int[][] saturday;

    @Column(name = "availability_set", nullable = false)
    private boolean availabilitySet;

    public UserAvailabilityEntity() {
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public int[][] getSunAvailability() { return sunday; }

    public void setSunAvailability(int[][] sunday) { this.sunday = sunday; }

    public int[][] getMonAvailability() { return monday; }

    public void setMonAvailability(int[][] monday) { this.monday = monday; }

    public int[][] getTueAvailability() { return tuesday; }

    public void setTueAvailability(int[][] tuesday) { this.tuesday = tuesday; }

    public int[][] getWedAvailability() { return wednesday; }

    public void setWedAvailability(int[][] wednesday) { this.wednesday = wednesday; }

    public int[][] getThuAvailability() { return thursday; }

    public void setThuAvailability(int[][] thursday) { this.thursday = thursday; }

    public int[][] getFriAvailability() { return friday; }

    public void setFriAvailability(int[][] friday) { this.friday = friday; }

    public int[][] getSatAvailability() { return saturday; }

    public void setSatAvailability(int[][] saturday) { this.saturday = saturday; }

    public boolean isAvailabilitySet() { return availabilitySet; }

    public void setAvailabilitySet(boolean availabilitySet) { this.availabilitySet = availabilitySet; }
}
