package dev.omercanbasboga.orgservice.reference;

import dev.omercanbasboga.orgservice.common.BaseEntity;
import jakarta.persistence.*;

/** A room or hall meetings can be booked into. */
@Entity
@Table(name = "location")
public class Location extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private Integer capacity;

    protected Location() {}

    public Location(String name, Integer capacity) {
        this.name = name;
        this.capacity = capacity;
    }

    public String getName() { return name; }
    public Integer getCapacity() { return capacity; }
}
