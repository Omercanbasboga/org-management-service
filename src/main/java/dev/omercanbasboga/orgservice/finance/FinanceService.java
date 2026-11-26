package dev.omercanbasboga.orgservice.finance;

import dev.omercanbasboga.orgservice.membership.OrgAccess;
import dev.omercanbasboga.orgservice.membership.OrgRole;
import dev.omercanbasboga.orgservice.organization.Organization;
import dev.omercanbasboga.orgservice.organization.OrganizationService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class FinanceService {

    public record TransactionDto(UUID id, TransactionType type, BigDecimal amount, String currency, String category,
                                 String description, LocalDate date, String recordedBy) {
        static TransactionDto of(Transaction t) {
            return new TransactionDto(t.getId(), t.getType(), t.getAmount(), t.getCurrency(), t.getCategory(),
                    t.getDescription(), t.getBookedOn(), t.getRecordedBy());
        }
    }

    public record TransactionRequest(@NotNull TransactionType type,
                                     @NotNull @DecimalMin(value = "0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
                                     @Size(max = 64) String category,
                                     @NotBlank @Size(max = 500) String description,
                                     LocalDate date) {}

    /** Field names match the console's budget cards. */
    public record Summary(String currency, BigDecimal income, BigDecimal expenses, BigDecimal balance) {}

    private final TransactionRepository transactions;
    private final OrganizationService orgs;
    private final OrgAccess access;
    private final String currency;

    public FinanceService(TransactionRepository transactions, OrganizationService orgs, OrgAccess access,
                          @Value("${app.finance.currency:TRY}") String currency) {
        this.transactions = transactions;
        this.orgs = orgs;
        this.access = access;
        this.currency = currency;
    }

    /** Officers and the advisor can see the books; members see the summary only. */
    @Transactional(readOnly = true)
    public List<TransactionDto> transactions(UUID orgId, TransactionType type, LocalDate from, LocalDate to) {
        orgs.require(orgId);
        access.require(orgId, r -> r.isOfficer() || r == OrgRole.ADVISOR, "see the transactions");
        if (from != null && to != null && from.isAfter(to)) throw new IllegalArgumentException("from is after to.");
        return transactions.search(orgId, type, from, to).stream().map(TransactionDto::of).toList();
    }

    @Transactional(readOnly = true)
    public Summary summary(UUID orgId) {
        orgs.require(orgId);
        access.requireMember(orgId, "see this organization's budget");
        return totals(orgId);
    }

    private Summary totals(UUID orgId) {
        BigDecimal in = transactions.total(orgId, TransactionType.INCOME, currency);
        BigDecimal out = transactions.total(orgId, TransactionType.EXPENSE, currency);
        return new Summary(currency, scale(in), scale(out), scale(in.subtract(out)));
    }

    /**
     * Records income or an expense. An expense that would take the balance
     * below zero is refused: these organizations have no credit line, and the
     * check is cheaper here than in an audit.
     */
    @Transactional
    public TransactionDto record(UUID orgId, TransactionRequest req) {
        // lock the organization row: two expenses at once must not both pass the balance check
        Organization org = orgs.requireForUpdate(orgId);
        if (!org.isActive()) throw new IllegalStateException("This organization is closed.");
        String by = access.require(orgId, OrgRole::canManageFinances, "record transactions").email();
        BigDecimal amount = req.amount().setScale(2, RoundingMode.UNNECESSARY);
        if (req.type() == TransactionType.EXPENSE) {
            BigDecimal balance = totals(orgId).balance();
            if (balance.compareTo(amount) < 0) {
                throw new IllegalStateException("Not enough money: balance is " + balance + " " + currency + ".");
            }
        }
        LocalDate date = req.date() != null ? req.date() : LocalDate.now();
        if (date.isAfter(LocalDate.now())) throw new IllegalArgumentException("Transactions can't be dated in the future.");
        Transaction t = transactions.save(new Transaction(org, req.type(), amount, currency,
                req.category(), req.description().trim(), date, by));
        return TransactionDto.of(t);
    }

    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
