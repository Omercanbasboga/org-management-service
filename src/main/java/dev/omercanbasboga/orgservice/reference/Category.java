package dev.omercanbasboga.orgservice.reference;

import dev.omercanbasboga.orgservice.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "category", uniqueConstraints = @UniqueConstraint(name = "uq_category_name", columnNames = "name"))
public class Category extends BaseEntity {

    @Column(nullable = false)
    private String name;

    protected Category() {}

    public Category(String name) {
        this.name = name;
    }

    public String getName() { return name; }
}
