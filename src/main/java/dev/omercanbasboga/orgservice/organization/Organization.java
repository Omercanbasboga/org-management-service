package dev.omercanbasboga.orgservice.organization;

import dev.omercanbasboga.orgservice.common.BaseEntity;
import dev.omercanbasboga.orgservice.reference.Category;
import dev.omercanbasboga.orgservice.reference.Location;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "organization")
public class Organization extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "short_name", length = 32)
    private String shortName;

    @Column(length = 2000)
    private String description;

    private String email;
    private String website;

    @Column(name = "founding_date")
    private LocalDate foundingDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /** Where the organization usually meets. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    /** Closed organizations keep their history; nothing is hard-deleted. */
    @Column(nullable = false)
    private boolean active = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public LocalDate getFoundingDate() { return foundingDate; }
    public void setFoundingDate(LocalDate foundingDate) { this.foundingDate = foundingDate; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
