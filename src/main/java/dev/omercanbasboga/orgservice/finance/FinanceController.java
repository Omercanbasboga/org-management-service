package dev.omercanbasboga.orgservice.finance;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/finance")
@Tag(name = "Budget")
public class FinanceController {

    private final FinanceService service;

    public FinanceController(FinanceService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public FinanceService.Summary summary(@PathVariable UUID orgId) {
        return service.summary(orgId);
    }

    @GetMapping("/transactions")
    @Operation(summary = "The ledger, newest first (officers and advisor)")
    public List<FinanceService.TransactionDto> transactions(
            @PathVariable UUID orgId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.transactions(orgId, type, from, to);
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record income or an expense (president, vice president or treasurer)")
    public FinanceService.TransactionDto record(@PathVariable UUID orgId,
                                                @Valid @RequestBody FinanceService.TransactionRequest req) {
        return service.record(orgId, req);
    }
}
