package com.example.telecom.controller;

import com.example.telecom.api.Times;
import com.example.telecom.audit.AuditAction;
import com.example.telecom.audit.AuditService;
import com.example.telecom.error.BadRequestException;
import com.example.telecom.error.NotFoundException;
import com.example.telecom.model.Account;
import com.example.telecom.model.Customer;
import com.example.telecom.repository.AccountRepository;
import com.example.telecom.repository.CustomerRepository;
import com.example.telecom.repository.MobileLineRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Billing accounts. A customer can hold several, for example a postpaid and a
 * prepaid account.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private static final int ACCOUNT_DIGITS = 1_000_000_000;
    private static final BigDecimal CONSUMER_CREDIT_LIMIT = new BigDecimal("500.00");

    private final AccountRepository repository;
    private final CustomerRepository customerRepository;
    private final MobileLineRepository lineRepository;
    private final AuditService audit;
    private final SecureRandom random = new SecureRandom();

    public AccountController(AccountRepository repository, CustomerRepository customerRepository,
            MobileLineRepository lineRepository, AuditService audit) {
        this.repository = repository;
        this.customerRepository = customerRepository;
        this.lineRepository = lineRepository;
        this.audit = audit;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccounts(@RequestParam Long customerId) {
        return repository.findByCustomerIdOrderById(customerId).stream()
                .map(a -> AccountResponse.from(a, lineRepository.findByAccountIdOrderById(a.getId()).size())).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public AccountResponse getAccount(@PathVariable Long id) {
        Account account = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Account " + id + " does not exist."));
        return AccountResponse.from(account, lineRepository.findByAccountIdOrderById(id).size());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public AccountResponse createAccount(@Valid @RequestBody AccountCreateRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new BadRequestException("Customer " + request.customerId() + " does not exist."));
        if (customer.getStatus() == Customer.Status.CLOSED) {
            throw new BadRequestException("Customer " + customer.getId() + " is closed; reopen it first.");
        }
        boolean prepaid = request.type() == Account.Type.PREPAID;
        Account.PaymentMethod payment = request.paymentMethod();
        if (prepaid != (payment == Account.PaymentMethod.TOP_UP)) {
            throw new BadRequestException("Prepaid accounts are paid by TOP_UP and postpaid accounts cannot be.");
        }
        Account account = new Account();
        account.setCustomer(customer);
        account.setType(request.type());
        account.setName(CustomerController.trimToNull(request.name()));
        account.setPaymentMethod(payment);
        account.setBillCycleDay(prepaid ? null : request.billCycleDay() != null ? request.billCycleDay() : 1);
        account.setCreditLimit(prepaid ? null : CONSUMER_CREDIT_LIMIT);
        account.setAccountNumber(newAccountNumber());
        Account saved = repository.save(account);
        audit.record(AuditAction.ACCOUNT_CREATED, saved.getId(), "Opened " + saved.getType().name().toLowerCase(Locale.ROOT) + " account " + saved.getAccountNumber() + " for customer "
                + customer.getCustomerNumber());
        return AccountResponse.from(saved, 0);
    }

    private String newAccountNumber() {
        String number;
        do {
            number = "1" + String.format("%09d", random.nextInt(ACCOUNT_DIGITS));
        } while (repository.existsByAccountNumber(number));
        return number;
    }

    public record AccountCreateRequest(@NotNull Long customerId, @NotNull Account.Type type,
            @NotNull Account.PaymentMethod paymentMethod, @Size(max = 100) String name,
            @Min(1) @Max(28) Integer billCycleDay) {
    }

    public record AccountResponse(Long id, String account_number, Long customer_id, String customer_name, String type,
            String name, Integer bill_cycle_day, String payment_method, BigDecimal balance, BigDecimal credit_limit,
            String status, long line_count, String created_at) {
        static AccountResponse from(Account a, long lineCount) {
            return new AccountResponse(a.getId(), a.getAccountNumber(), a.getCustomer().getId(),
                    a.getCustomer().displayName(), a.getType().name(), a.getName(), a.getBillCycleDay(),
                    a.getPaymentMethod().name(), CustomerController.toMoney(a.getBalance()),
                    a.getCreditLimit() != null ? CustomerController.toMoney(a.getCreditLimit()) : null,
                    a.getStatus().name(), lineCount, Times.iso(a.getCreatedAt()));
        }
    }
}
