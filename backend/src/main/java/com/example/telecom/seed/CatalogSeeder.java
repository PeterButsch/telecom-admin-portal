package com.example.telecom.seed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Seeds the plan, add-on and promotion catalogues. */
final class CatalogSeeder {

    private static final String SINGTEL = "SINGTEL";
    private static final String CONSUMER = "CONSUMER";
    private static final String ENTERPRISE = "ENTERPRISE";
    private static final String POSTPAID = "POSTPAID";
    private static final String SIM_ONLY = "SIM_ONLY";
    private static final String DATA_ONLY = "DATA_ONLY";
    private static final String PREPAID = "PREPAID";

    private final SeedContext ctx;

    CatalogSeeder(SeedContext ctx) {
        this.ctx = ctx;
    }

    Catalog seed() {
        List<SeedPlan> plans = seedPlans();
        List<Long> addOns = seedAddOns();
        // Before the oldest seeded line, so every promotion was on offer when its line
        // started.
        String validFrom = SeedContext.date(ctx.today().minusYears(4));
        String insert = "INSERT INTO promotions (id, code, name, discount_type, discount_value, duration_months,"
                + " valid_from, valid_to, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        List<Object[]> rows = List.of(
                new Object[] {1, "WELCOME50", "50% off for 6 months", "PERCENT", 50, 6, validFrom, null, 1},
                new Object[] {2, "SWITCH20", "$20 off monthly for switching to Singtel", "AMOUNT", 20, 12, validFrom,
                        null, 1},
                new Object[] {3, "STUDENT10", "$10 off for students", "AMOUNT", 10, 24, validFrom, null, 1},
                new Object[] {4, "FAMILY15", "15% off each family line", "PERCENT", 15, 24, validFrom, null, 1},
                new Object[] {5, "BIZ5", "5% corporate volume discount", "PERCENT", 5, 24, validFrom, null, 1});
        ctx.jdbc().batchUpdate(insert, rows);
        Map<Long, Integer> months = new HashMap<>();
        for (Object[] row : rows) {
            months.put(((Integer) row[0]).longValue(), (Integer) row[5]);
        }
        return new Catalog(plans, addOns, 2, List.of(1L, 3L, 4L), 5, months);
    }

    private List<SeedPlan> seedPlans() {
        List<SeedPlan> plans = new ArrayList<>();
        Object[][] data = {
                {"SG-5G-LITE", "Singtel 5G Lite 30GB", SINGTEL, POSTPAID, CONSUMER, 35.00, 30, null, null, 24, "5G",
                        true},
                {"SG-5G-CORE", "Singtel 5G Core 100GB", SINGTEL, POSTPAID, CONSUMER, 55.00, 100, null, null, 24, "5G",
                        true},
                {"SG-5G-PLUS", "Singtel 5G Plus 200GB", SINGTEL, POSTPAID, CONSUMER, 85.00, 200, null, null, 24, "5G",
                        true},
                {"SG-5G-MAX", "Singtel 5G Max Unlimited", SINGTEL, POSTPAID, CONSUMER, 125.00, null, null, null, 24,
                        "5G", true},
                {"SG-SIMO-100", "Singtel SIM Only 100GB", SINGTEL, SIM_ONLY, CONSUMER, 30.00, 100, null, null, 0, "5G",
                        true},
                {"SG-SIMO-200", "Singtel SIM Only 200GB 5G", SINGTEL, SIM_ONLY, CONSUMER, 40.00, 200, null, null, 12,
                        "5G", true},
                {"SG-DATA-50", "Singtel Data Only 50GB", SINGTEL, DATA_ONLY, CONSUMER, 20.00, 50, 0, 0, 0, "5G", true},
                {"GOMO-100", "GOMO 100GB", "GOMO", SIM_ONLY, CONSUMER, 15.00, 100, 1000, 1000, 0, "4G", true},
                {"GOMO-200-5G", "GOMO 200GB 5G", "GOMO", SIM_ONLY, CONSUMER, 20.00, 200, 1000, 1000, 0, "5G", true},
                {"HI-5G-100", "hi!Card 5G 100GB (30 days)", "HI_CARD", PREPAID, CONSUMER, 18.00, 100, 500, 500, 0, "5G",
                        true},
                {"HI-TOURIST-14", "hi!Tourist 100GB (14 days)", "HI_CARD", PREPAID, CONSUMER, 15.00, 100, 100, 100, 0,
                        "4G", true},
                {"BIZ-5G-80", "Business Mobile 5G 80GB", SINGTEL, POSTPAID, ENTERPRISE, 40.00, 80, null, null, 24, "5G",
                        true},
                {"BIZ-5G-UNL", "Business Mobile 5G Unlimited", SINGTEL, POSTPAID, ENTERPRISE, 70.00, null, null, null,
                        24, "5G", true},
                {"BIZ-IOT-5", "Business IoT Data 5GB", SINGTEL, DATA_ONLY, ENTERPRISE, 8.00, 5, 0, 0, 12, "4G", true},
                {"SG-4G-COMBO3", "Singtel 4G Combo 3 (legacy)", SINGTEL, POSTPAID, CONSUMER, 48.00, 6, 300, 300, 24,
                        "4G", false}};
        String insert = "INSERT INTO plans (id, code, name, brand, category, segment, monthly_price, data_limit_gb,"
                + " minutes_limit, sms_limit, contract_months, network, is_active, created_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        List<Object[]> rows = new ArrayList<>();
        String created = SeedContext.ts(ctx.daysAgo(1200));
        for (int i = 0; i < data.length; i++) {
            Object[] p = data[i];
            long id = i + 1L;
            boolean active = (Boolean) p[11];
            rows.add(new Object[] {id, p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9], p[10],
                    active ? 1 : 0, created});
            plans.add(new SeedPlan(id, (String) p[0], (String) p[2], (String) p[3], (String) p[4], (Double) p[5],
                    (Integer) p[9], active, (Integer) p[6]));
        }
        ctx.jdbc().batchUpdate(insert, rows);
        return plans;
    }

    private List<Long> seedAddOns() {
        String insert = "INSERT INTO add_ons (id, code, name, category, monthly_price, is_active)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        List<Object[]> rows = List.of(new Object[] {1, "DATA-50", "Extra 50GB data", "DATA", 10.00, 1},
                new Object[] {2, "ROAM-ASEAN-10", "DataRoam ASEAN 10GB", "ROAMING", 15.00, 1},
                new Object[] {3, "ROAM-WORLD-5", "DataRoam Worldwide 5GB", "ROAMING", 25.00, 1},
                new Object[] {4, "VAS-MUSIC", "Music streaming bundle", "VALUE_ADDED", 9.90, 1},
                new Object[] {5, "VAS-CLOUD-200", "Cloud storage 200GB", "VALUE_ADDED", 3.90, 1},
                new Object[] {6, "INS-DEVICE", "Device Protection", "INSURANCE", 12.00, 1});
        ctx.jdbc().batchUpdate(insert, rows);
        return List.of(1L, 2L, 3L, 4L, 5L, 6L);
    }

    /** Plan as seeded, with the fields later seeding steps need. */
    record SeedPlan(long id, String code, String brand, String category, String segment, double price,
            int contractMonths, boolean active, Integer dataGb) {
        boolean prepaid() {
            return "PREPAID".equals(category);
        }

        boolean dataOnly() {
            return "DATA_ONLY".equals(category);
        }

        boolean simOnly() {
            return "SIM_ONLY".equals(category);
        }
    }

    /** Seeded catalogue ids grouped by how customers use them. */
    record Catalog(List<SeedPlan> plans, List<Long> addOnIds, long switchPromoId, List<Long> consumerPromoIds,
            long businessPromoId, Map<Long, Integer> promotionMonths) {

        Catalog {
            plans = List.copyOf(plans);
            addOnIds = List.copyOf(addOnIds);
            consumerPromoIds = List.copyOf(consumerPromoIds);
            promotionMonths = Map.copyOf(promotionMonths);
        }

        int promotionMonths(long promotionId) {
            return promotionMonths.get(promotionId);
        }

        SeedPlan plan(String code) {
            return plans.stream().filter(p -> p.code().equals(code)).findFirst().orElseThrow();
        }
    }
}
