package com.example.telecom.seed;

import com.example.telecom.seed.CustomerSeeder.SeedCustomer;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Fills an empty database with generated Singapore demo data. Runs after Flyway
 * has migrated the schema and before the web server accepts requests, so the
 * API never serves a half-seeded database.
 */
@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataSeeder implements SmartInitializingSingleton {

    private static final Logger LOG = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final long seed;

    public DemoDataSeeder(JdbcTemplate jdbc, PlatformTransactionManager transactionManager,
            @Value("${app.demo-data.seed:42}") long seed) {
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactionManager);
        this.seed = seed;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Long plans = jdbc.queryForObject("SELECT COUNT(*) FROM plans", Long.class);
        if (plans != null && plans > 0) {
            LOG.info("Database already has data; skipping demo data");
            return;
        }
        long started = System.currentTimeMillis();
        transaction.executeWithoutResult(status -> {
            SeedContext ctx = new SeedContext(jdbc, seed, Instant.now());
            CatalogSeeder.Catalog catalog = new CatalogSeeder(ctx).seed();
            List<SeedCustomer> customers = new CustomerSeeder(ctx, catalog).seed();
            int usage = new ActivitySeeder(ctx).seed(customers);
            long lines = customers.stream().mapToLong(c -> c.lines().size()).sum();
            LOG.info("Seeded demo data: {} customers, {} lines, {} usage records", customers.size(), lines, usage);
        });
        LOG.info("Demo data ready in {} ms", System.currentTimeMillis() - started);
    }
}
