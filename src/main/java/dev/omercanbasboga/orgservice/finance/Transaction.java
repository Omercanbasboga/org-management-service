package dev.omercanbasboga.orgservice.finance;

import dev.omercanbasboga.orgservice.common.BaseEntity;
import dev.omercanbasboga.orgservice.organization.Organization;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One line in an organization's books. Amounts are always positive and the
 * type says which way the money went, so a sum never depends on someone
 * remembering a minus sign.
 */
@Entity
@Table(name = "finance_transaction", indexes = @Index(name = "idx_tx_org_date", columnList = "organization_id, booked_on"))
public class Transaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private TransactionType type;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 64)
    private String category;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "booked_on", nullable = false)
    private LocalDate bookedOn;

    @Column(name = "recorded_by")
    private String recordedBy;

    protected Transaction() {}

    public Transaction(Organization organization, TransactionType type, BigDecimal amount, String currency,
                       String category, String description, LocalDate bookedOn, String recordedBy) {
        this.organization = organization;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.category = category;
        this.description = description;
        this.bookedOn = bookedOn;
        this.recordedBy = recordedBy;
    }

    public Organization getOrganization() { return organization; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public LocalDate getBookedOn() { return bookedOn; }
    public String getRecordedBy() { return recordedBy; }
}
