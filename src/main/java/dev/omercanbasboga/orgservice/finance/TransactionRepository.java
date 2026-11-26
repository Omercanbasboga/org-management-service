package dev.omercanbasboga.orgservice.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("select t from Transaction t where t.organization.id = :orgId "
            + "and (:type is null or t.type = :type) "
            + "and (cast(:from as date) is null or t.bookedOn >= :from) "
            + "and (cast(:to as date) is null or t.bookedOn <= :to) "
            + "order by t.bookedOn desc, t.createdAt desc")
    List<Transaction> search(@Param("orgId") UUID orgId, @Param("type") TransactionType type,
                             @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select coalesce(sum(t.amount), 0) from Transaction t where t.organization.id = :orgId "
            + "and t.type = :type and t.currency = :currency")
    BigDecimal total(@Param("orgId") UUID orgId, @Param("type") TransactionType type,
                     @Param("currency") String currency);
}
