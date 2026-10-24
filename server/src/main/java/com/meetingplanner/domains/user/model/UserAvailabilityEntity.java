package com.meetingplanner.domains.user.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_availability")
public class UserAvailabilityEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sunday", nullable = false, columnDefinition = "jsonb")
    private int[][] sunday;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "monday", nullable = false, columnDefinition = "jsonb")
    private int[][] monday;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tuesday", nullable = false, columnDefinition = "jsonb")
    private int[][] tuesday;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "wednesday", nullable = false, columnDefinition = "jsonb")
    private int[][] wednesday;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "thursday", nullable = false, columnDefinition = "jsonb")
    private int[][] thursday;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "friday", nullable = false, columnDefinition = "jsonb")
    private int[][] friday;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "saturday", nullable = false, columnDefinition = "jsonb")
    private int[][] saturday;

    @Column(name = "availability_set", nullable = false)
    private boolean availabilitySet;

    public UserAvailabilityEntity() {
    }

    public Long getUserId() {
        return userId;
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
