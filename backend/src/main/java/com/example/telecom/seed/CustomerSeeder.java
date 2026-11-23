package com.example.telecom.seed;

import com.example.telecom.model.IdentityNumbers;
import com.example.telecom.seed.CatalogSeeder.Catalog;
import com.example.telecom.seed.CatalogSeeder.SeedPlan;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Seeds individual and business customers with their billing accounts, mobile
 * lines and add-ons.
 */
@SuppressWarnings({"PMD.AvoidLiteralsInIfCondition", "PMD.AvoidInstantiatingObjectsInLoops", "PMD.GodClass",
        "PMD.CouplingBetweenObjects", "PMD.TooManyMethods", "PMD.CyclomaticComplexity", "PMD.CognitiveComplexity",
        "PMD.ConfusingTernary", "PMD.NullAssignment"})
final class CustomerSeeder {

    static final int INDIVIDUALS = 560;
    static final int BUSINESSES = 45;
    private static final String ACTIVE = "ACTIVE";
    private static final String SUSPENDED = "SUSPENDED";
    private static final String CLOSED = "CLOSED";
    private static final String TERMINATED = "TERMINATED";
    private static final String POSTPAID = "POSTPAID";
    private static final String PREPAID = "PREPAID";
    private static final String INDIVIDUAL = "INDIVIDUAL";
    private static final String[] OPERATORS = {"M1", "STARHUB", "SIMBA", "CIRCLES_LIFE", "OTHER"};
    private static final String[] CONSUMER_PLAN_CODES = {"SG-5G-LITE", "SG-5G-CORE", "SG-5G-PLUS", "SG-5G-MAX",
            "SG-SIMO-100", "SG-SIMO-200", "SG-DATA-50", "GOMO-100", "GOMO-200-5G", "HI-5G-100", "SG-4G-COMBO3"};
    private static final int[] CONSUMER_PLAN_WEIGHTS = {10, 14, 8, 4, 8, 6, 4, 10, 9, 12, 5};
    private static final String LEGACY_PLAN = "SG-4G-COMBO3";
    private static final long LEGACY_MIN_AGE_DAYS = 700;
    private static final int MAX_CUSTOMER_AGE_DAYS = 1300;

    private final SeedContext ctx;
    private final Catalog catalog;
    private final List<Object[]> customerRows = new ArrayList<>();
    private final List<Object[]> accountRows = new ArrayList<>();
    private final List<Object[]> lineRows = new ArrayList<>();
    private final List<Object[]> addOnRows = new ArrayList<>();
    private final List<SeedCustomer> customers = new ArrayList<>();
    private final Set<String> emails = new HashSet<>();
    private final Set<String> companies = new HashSet<>();
    private long nextCustomerId = 1;
    private long nextAccountId = 1;
    private long nextLineId = 1;

    CustomerSeeder(SeedContext ctx, Catalog catalog) {
        this.ctx = ctx;
        this.catalog = catalog;
    }

    List<SeedCustomer> seed() {
        for (int i = 0; i < INDIVIDUALS; i++) {
            seedIndividual(i);
        }
        for (int i = 0; i < BUSINESSES; i++) {
            seedBusiness();
        }
        ctx.jdbc().batchUpdate("INSERT INTO customers (id, customer_number, type, first_name, last_name, company_name,"
                + " uen, contact_name, id_type, id_number, date_of_birth, email, phone, address_line, postal_code,"
                + " status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", customerRows);
        ctx.jdbc().batchUpdate("INSERT INTO accounts (id, account_number, customer_id, type, name, bill_cycle_day,"
                + " payment_method, balance, credit_limit, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                accountRows);
        ctx.jdbc()
                .batchUpdate("INSERT INTO mobile_lines (id, account_id, plan_id, msisdn, sim_type, iccid, status,"
                        + " status_reason, user_name, promotion_id, promotion_ends_on, contract_start, contract_end,"
                        + " port_in_from, port_out_to, activated_at, terminated_at, created_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", lineRows);
        ctx.jdbc().batchUpdate(
                "INSERT INTO line_add_ons (line_id, add_on_id, started_at, ended_at) VALUES (?, ?, ?, ?)", addOnRows);
        return List.copyOf(customers);
    }

    // ---- individuals
    // -------------------------------------------------------------------------------

    private void seedIndividual(int index) {
        long customerId = nextCustomerId++;
        String status = new String[] {ACTIVE, SUSPENDED, CLOSED}[ctx.weighted(94, 3, 3)];
        boolean tourist = ctx.chance(0.04);
        Instant created = ctx.daysAgo(ctx.between(20, MAX_CUSTOMER_AGE_DAYS));
        LocalDate joined = LocalDate.ofInstant(created, ZoneOffset.UTC);
        Person person = tourist ? tourist(joined) : resident(joined);
        List<SeedPlan> plans = individualPlans(index, tourist, created);
        String firstMsisdn = ctx.newMsisdn();
        SgReferenceData.Estate estate = ctx.pick(SgReferenceData.ESTATES);
        int block = ctx.between(101, 899);
        customerRows.add(new Object[] {customerId, customerNumber(customerId), INDIVIDUAL, person.first(),
                person.last(), null, null, null, person.idType(), person.idNumber(), SeedContext.date(person.dob()),
                email(person.first(), person.last()), formatPhone(firstMsisdn),
                tourist
                        ? null
                        : "Blk " + block + " " + estate.road() + " #"
                                + String.format("%02d-%03d", ctx.between(2, 25), ctx.between(1, 450)),
                tourist ? null : estate.sector() + String.format("%04d", block), status, SeedContext.ts(created)});

        Long postpaidAccount = null;
        Long prepaidAccount = null;
        List<SeedLine> lines = new ArrayList<>();
        for (SeedPlan plan : plans) {
            long accountId;
            if (plan.prepaid()) {
                if (prepaidAccount == null) {
                    prepaidAccount = addConsumerAccount(customerId, PREPAID, status, created);
                }
                accountId = prepaidAccount;
            } else {
                if (postpaidAccount == null) {
                    postpaidAccount = addConsumerAccount(customerId, POSTPAID, status, created);
                }
                accountId = postpaidAccount;
            }
            String msisdn = lines.isEmpty() ? firstMsisdn : ctx.newMsisdn();
            lines.add(addLine(customerId, accountId, plan, msisdn, lineStatus(status), created, null, false));
        }
        customers.add(new SeedCustomer(customerId, status, created, lines));
    }

    /**
     * Most people have one to three lines. A few profiles exceed the IMDA limits
     * (10 postpaid or 3 prepaid lines per person across operators) so the anti-scam
     * checks have realistic cases.
     */
    private List<SeedPlan> individualPlans(int index, boolean tourist, Instant created) {
        List<SeedPlan> plans = new ArrayList<>();
        if (tourist) {
            plans.add(catalog.plan("HI-TOURIST-14"));
            return plans;
        }
        if (index % 56 == 7) {
            int count = ctx.between(9, 12);
            for (int i = 0; i < count; i++) {
                plans.add(catalog.plan(ctx.chance(0.5) ? "SG-SIMO-100" : "GOMO-100"));
            }
            return plans;
        }
        if (index % 70 == 33) {
            int count = ctx.between(3, 5);
            for (int i = 0; i < count; i++) {
                plans.add(catalog.plan("HI-5G-100"));
            }
            plans.add(consumerPlan(created));
            return plans;
        }
        int count = new int[] {1, 2, 3, 4, 5}[ctx.weighted(58, 24, 11, 5, 2)];
        for (int i = 0; i < count; i++) {
            plans.add(consumerPlan(created));
        }
        return plans;
    }

    private SeedPlan consumerPlan(Instant customerCreated) {
        String code = CONSUMER_PLAN_CODES[ctx.weighted(CONSUMER_PLAN_WEIGHTS)];
        boolean youngCustomer = customerCreated.isAfter(ctx.daysAgo(LEGACY_MIN_AGE_DAYS));
        if (LEGACY_PLAN.equals(code) && youngCustomer) {
            code = "SG-5G-CORE";
        }
        return catalog.plan(code);
    }

    /** A resident who was 18 to 80 years old on {@code onDate}. */
    private Person resident(LocalDate onDate) {
        LocalDate dob = onDate.minusYears(18).minusDays(ctx.between(0, 62 * 365));
        boolean foreigner = ctx.chance(0.16);
        int ethnicity = foreigner ? 3 : ctx.weighted(72, 16, 12);
        boolean male = ctx.chance(0.5);
        String first;
        String last;
        switch (ethnicity) {
            case 1 -> {
                first = ctx.pick(male ? SgReferenceData.MALAY_MALE : SgReferenceData.MALAY_FEMALE);
                last = (male ? "bin " : "binte ") + ctx.pick(SgReferenceData.MALAY_FATHERS);
            }
            case 2 -> {
                first = ctx.pick(male ? SgReferenceData.INDIAN_MALE : SgReferenceData.INDIAN_FEMALE);
                last = (male ? "s/o " : "d/o ") + ctx.pick(SgReferenceData.INDIAN_FATHERS);
            }
            case 3 -> {
                first = ctx.pick(SgReferenceData.OTHER_FIRST);
                last = ctx.pick(SgReferenceData.OTHER_LAST);
            }
            default -> {
                first = ctx.pick(SgReferenceData.CHINESE_GIVEN);
                last = ctx.pick(SgReferenceData.CHINESE_SURNAMES);
            }
        }
        return foreigner
                ? new Person(first, last, "FIN", finNumber(), dob)
                : new Person(first, last, "NRIC", nricNumber(dob), dob);
    }

    private Person tourist(LocalDate onDate) {
        LocalDate dob = onDate.minusYears(20).minusDays(ctx.between(0, 45 * 365));
        String passport;
        do {
            passport = "P" + ctx.digits(7) + (char) ('A' + ctx.between(0, 25));
        } while (!ctx.reserveIdentity(passport));
        return new Person(ctx.pick(SgReferenceData.OTHER_FIRST), ctx.pick(SgReferenceData.OTHER_LAST), "PASSPORT",
                passport, dob);
    }

    /**
     * Synthetic NRIC: S for people born before 2000, T after, starting with the
     * two-digit birth year.
     */
    private String nricNumber(LocalDate dob) {
        char prefix = dob.getYear() < 2000 ? 'S' : 'T';
        String nric;
        do {
            String digits = String.format("%02d", dob.getYear() % 100) + ctx.digits(5);
            nric = prefix + digits + IdentityNumbers.checkLetter(prefix, digits);
        } while (!ctx.reserveIdentity(nric));
        return nric;
    }

    private String finNumber() {
        char prefix = new char[] {'F', 'G', 'M'}[ctx.weighted(25, 60, 15)];
        String fin;
        do {
            String digits = ctx.digits(7);
            fin = prefix + digits + IdentityNumbers.checkLetter(prefix, digits);
        } while (!ctx.reserveIdentity(fin));
        return fin;
    }

    private long addConsumerAccount(long customerId, String type, String customerStatus, Instant created) {
        long id = nextAccountId++;
        boolean postpaid = POSTPAID.equals(type);
        double balance;
        if (!postpaid) {
            balance = round(ctx.between(0.0, 60.0));
        } else if (SUSPENDED.equals(customerStatus)) {
            balance = round(ctx.between(120.0, 450.0));
        } else {
            balance = ctx.chance(0.3) ? round(ctx.between(20.0, 200.0)) : 0.0;
        }
        String payment = postpaid ? new String[] {"GIRO", "CREDIT_CARD", "PAYNOW"}[ctx.weighted(45, 45, 10)] : "TOP_UP";
        accountRows.add(
                new Object[] {id, ctx.newAccountNumber(), customerId, type, null, postpaid ? ctx.between(1, 28) : null,
                        payment, balance, postpaid ? 500.0 : null, customerStatus, SeedContext.ts(created)});
        return id;
    }

    private String lineStatus(String customerStatus) {
        if (CLOSED.equals(customerStatus)) {
            return TERMINATED;
        }
        if (SUSPENDED.equals(customerStatus)) {
            return SUSPENDED;
        }
        return new String[] {ACTIVE, SUSPENDED, "BARRED", TERMINATED, "PENDING"}[ctx.weighted(85, 3, 1, 10, 1)];
    }

    // ---- businesses
    // --------------------------------------------------------------------------------

    private void seedBusiness() {
        long customerId = nextCustomerId++;
        String company = companyName();
        String slug = slug(company.replace(" Pte. Ltd.", "")).replace(".", "");
        int addressIndex = ctx.between(0, SgReferenceData.BUSINESS_ADDRESSES.size() - 1);
        String uen = ctx.between(1995, 2024) + ctx.digits(5) + (char) ('A' + ctx.between(0, 25));
        Instant created = ctx.daysAgo(ctx.between(200, MAX_CUSTOMER_AGE_DAYS));
        Person contact = resident(LocalDate.ofInstant(created, ZoneOffset.UTC));
        customerRows.add(new Object[] {customerId, customerNumber(customerId), "BUSINESS", null, null, company, uen,
                contact.first() + " " + contact.last(), null, null, null, uniqueEmail("telecom", slug + ".com.sg"),
                "+65 6" + ctx.digits(3) + " " + ctx.digits(4), SgReferenceData.BUSINESS_ADDRESSES.get(addressIndex),
                SgReferenceData.BUSINESS_POSTAL_CODES.get(addressIndex), ACTIVE, SeedContext.ts(created)});

        int[] range = new int[][] {{5, 15}, {16, 40}, {41, 80}}[ctx.weighted(60, 30, 10)];
        int totalLines = ctx.between(range[0], range[1]);
        int accounts = Math.min(3, 1 + totalLines / 20);
        List<SeedLine> lines = new ArrayList<>();
        for (int a = 0; a < accounts; a++) {
            long accountId = nextAccountId++;
            accountRows.add(new Object[] {accountId, ctx.newAccountNumber(), customerId, POSTPAID,
                    SgReferenceData.COST_CENTRES.get(a), 1, "INVOICE",
                    ctx.chance(0.6) ? 0.0 : round(ctx.between(500.0, 8000.0)), (double) (ctx.between(1, 10) * 5000),
                    ACTIVE, SeedContext.ts(created)});
            for (int i = a; i < totalLines; i += accounts) {
                SeedPlan plan = catalog
                        .plan(new String[] {"BIZ-5G-80", "BIZ-5G-UNL", "BIZ-IOT-5"}[ctx.weighted(55, 30, 15)]);
                String user = plan.dataOnly()
                        ? "Fleet tracker " + (i + 1)
                        : employeeName() + " (" + ctx.pick(SgReferenceData.EMPLOYEE_ROLES) + ")";
                String status = new String[] {ACTIVE, TERMINATED, SUSPENDED}[ctx.weighted(92, 7, 1)];
                lines.add(addLine(customerId, accountId, plan, ctx.newMsisdn(), status, created, user, true));
            }
        }
        customers.add(new SeedCustomer(customerId, ACTIVE, created, lines));
    }

    private String companyName() {
        String name;
        do {
            name = ctx.pick(SgReferenceData.COMPANY_PREFIXES) + " " + ctx.pick(SgReferenceData.COMPANY_TRADES)
                    + " Pte. Ltd.";
        } while (!companies.add(name));
        return name;
    }

    private String employeeName() {
        return ctx.chance(0.6)
                ? ctx.pick(SgReferenceData.CHINESE_SURNAMES) + " " + ctx.pick(SgReferenceData.CHINESE_GIVEN)
                : ctx.pick(SgReferenceData.OTHER_FIRST) + " " + ctx.pick(SgReferenceData.OTHER_LAST);
    }

    // ---- lines
    // -------------------------------------------------------------------------------------

    @SuppressWarnings({"PMD.ExcessiveParameterList", "PMD.CognitiveComplexity", "PMD.CyclomaticComplexity",
            "PMD.NPathComplexity", "PMD.NcssCount"})
    private SeedLine addLine(long customerId, long accountId, SeedPlan plan, String msisdn, String status,
            Instant customerCreated, String userName, boolean business) {
        long id = nextLineId++;
        Instant earliest = LEGACY_PLAN.equals(plan.code())
                ? customerCreated
                : max(customerCreated, ctx.daysAgo(LEGACY_MIN_AGE_DAYS * 2));
        Instant activated = ctx.randomBetween(earliest, ctx.daysAgo(1));
        boolean pending = "PENDING".equals(status);
        Instant terminated = null;
        String reason = null;
        String portOut = null;
        if (TERMINATED.equals(status)) {
            terminated = ctx.randomBetween(min(activated.plusSeconds(30L * 86_400), ctx.daysAgo(1)), ctx.now());
            if (ctx.chance(0.4)) {
                portOut = OPERATORS[ctx.weighted(35, 35, 15, 12, 3)];
                reason = "Ported out to " + portOut;
            } else {
                reason = "Terminated at customer request";
            }
        } else if (SUSPENDED.equals(status)) {
            reason = "Overdue bill";
        } else if ("BARRED".equals(status)) {
            reason = "Suspected fraudulent use; outgoing calls and SMS barred";
        } else if (pending) {
            activated = null;
            reason = "Number port-in in progress";
        }
        String portIn = pending || ctx.chance(0.25) ? OPERATORS[ctx.weighted(35, 35, 15, 12, 3)] : null;
        Instant start = activated != null ? activated : ctx.now();
        LocalDate startDate = LocalDate.ofInstant(start, ZoneOffset.UTC);

        Long promotionId = null;
        LocalDate promotionEnds = null;
        if (business && ctx.chance(0.7)) {
            promotionId = catalog.businessPromoId();
        } else if (!business && portIn != null && ctx.chance(0.6)) {
            promotionId = catalog.switchPromoId();
        } else if (!business && ctx.chance(0.25)) {
            promotionId = ctx.pick(catalog.consumerPromoIds());
        }
        if (promotionId != null) {
            promotionEnds = startDate.plusMonths(catalog.promotionMonths(promotionId));
        }
        boolean contract = plan.contractMonths() > 0;
        boolean recent = start.isAfter(ctx.daysAgo(365));
        String simType = ctx.chance(recent ? 0.45 : 0.2) ? "ESIM" : "PHYSICAL";
        Instant created = activated != null ? activated.minusSeconds(ctx.between(600, 86_400)) : ctx.daysAgo(2);
        lineRows.add(new Object[] {id, accountId, plan.id(), msisdn, simType, ctx.newIccid(), status, reason, userName,
                promotionId, SeedContext.date(promotionEnds),
                contract && activated != null ? SeedContext.date(startDate) : null,
                contract && activated != null ? SeedContext.date(startDate.plusMonths(plan.contractMonths())) : null,
                portIn, portOut, SeedContext.ts(activated), SeedContext.ts(terminated), SeedContext.ts(created)});
        if (ACTIVE.equals(status) && !plan.prepaid()) {
            addAddOns(id, activated);
        }
        return new SeedLine(id, customerId, plan, status, activated, terminated, business);
    }

    private void addAddOns(long lineId, Instant activated) {
        if (!ctx.chance(0.22)) {
            return;
        }
        long first = ctx.pick(catalog.addOnIds());
        addOnRows.add(new Object[] {lineId, first, SeedContext.ts(ctx.randomBetween(activated, ctx.now())), null});
        if (ctx.chance(0.25)) {
            long second = ctx.pick(catalog.addOnIds());
            if (second != first) {
                addOnRows.add(
                        new Object[] {lineId, second, SeedContext.ts(ctx.randomBetween(activated, ctx.now())), null});
            }
        }
    }

    // ---- helpers
    // -----------------------------------------------------------------------------------

    private String email(String first, String last) {
        String local = slug(first + "." + last.replaceAll("^(bin|binte|s/o|d/o) ", ""));
        return uniqueEmail(local, ctx.pick(SgReferenceData.EMAIL_DOMAINS));
    }

    private String uniqueEmail(String local, String domain) {
        String email = local + "@" + domain;
        int suffix = 2;
        while (!emails.add(email)) {
            email = local + suffix++ + "@" + domain;
        }
        return email;
    }

    private static String slug(String value) {
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", ".").replaceAll("^\\.|\\.$", "");
    }

    static String customerNumber(long id) {
        return "C" + (1_000_000 + id);
    }

    static String formatPhone(String msisdn) {
        return "+65 " + msisdn.substring(0, 4) + " " + msisdn.substring(4);
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }

    private static Instant max(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }

    private static Instant min(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }

    /** A seeded line with what the activity seeder needs. */
    record SeedLine(long id, long customerId, SeedPlan plan, String status, Instant activatedAt, Instant terminatedAt,
            boolean business) {
    }

    record SeedCustomer(long id, String status, Instant createdAt, List<SeedLine> lines) {
        SeedCustomer {
            lines = List.copyOf(lines);
        }
    }

    private record Person(String first, String last, String idType, String idNumber, LocalDate dob) {
    }
}
