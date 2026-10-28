package com.example.multi_stores.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

/**
 * Applies the database changes Hibernate's ddl-auto=update cannot make by itself
 * (same SQL as db/migrations/V2026_10_28_01 and _02), once, at startup, using the app's own connection.
 *
 * Safe by design:
 *  - PostgreSQL only; runs after Hibernate has created/updated the tables (@DependsOn entityManagerFactory).
 *  - Idempotent: every step checks the current state first, so restarts change nothing.
 *  - Non-destructive: only adds a column/default and replaces CHECK constraints; drops NOT NULL on price.
 *  - Each upgrade runs in one transaction; on any error it is rolled back, logged, and the app still starts
 *    (existing features keep working; only Shoes/Luggage/drafts stay unavailable).
 */
@Component
@DependsOn("entityManagerFactory")
public class SchemaUpgrade {

    private static final Logger log = LoggerFactory.getLogger(SchemaUpgrade.class);

    static final List<String> CATEGORIES = List.of(
            "KITCHEN", "SPICES", "GROCERY", "CLOTHING", "WOMEN_UNDERWEAR", "KIDS_UNDERWEAR", "CHILD_GAME",
            "ELECTRONICS", "BEAUTY", "HOME_DECOR", "SPORTS", "PETS", "HEALTH", "TOOLS", "OFFICE",
            "SHOES", "LUGGAGE");

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public SchemaUpgrade(DataSource dataSource, PlatformTransactionManager txManager) {
        this.dataSource = dataSource;
        this.jdbc = new JdbcTemplate(dataSource);
        this.tx = new TransactionTemplate(txManager);
    }

    @PostConstruct
    public void upgrade() {
        try (Connection c = dataSource.getConnection()) {
            if (!"PostgreSQL".equalsIgnoreCase(c.getMetaData().getDatabaseProductName())) {
                return;
            }
        } catch (Exception e) {
            log.error("Schema upgrade: cannot inspect database: {}", e.getMessage());
            return;
        }
        run("categories (SHOES, LUGGAGE)", this::upgradeCategories);
        run("product drafts", this::upgradeDrafts);
    }

    private void run(String name, Runnable step) {
        try {
            tx.executeWithoutResult(s -> step.run());
        } catch (Exception e) {
            log.error("Schema upgrade '{}' failed and was rolled back: {}", name, e.getMessage());
        }
    }

    private boolean tableExists(String table) {
        Integer n = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_schema = current_schema() and table_name = ?",
                Integer.class, table);
        return n != null && n > 0;
    }

    void upgradeCategories() {
        if (!tableExists("stores")) return;
        List<String> defs = jdbc.queryForList(
                "select conname || '|' || pg_get_constraintdef(oid) from pg_constraint "
                        + "where conrelid = 'stores'::regclass and contype = 'c' and pg_get_constraintdef(oid) ilike '%category%'",
                String.class);
        boolean current = defs.size() == 1 && defs.get(0).startsWith("stores_category_check|")
                && CATEGORIES.stream().allMatch(c -> defs.get(0).contains("'" + c + "'"));
        if (current) return;
        if (defs.size() > 1 || (defs.size() == 1 && !defs.get(0).startsWith("stores_category_check|"))) {
            throw new IllegalStateException("unexpected category constraints " + defs + "; not changed");
        }
        String list = String.join("', '", CATEGORIES);
        jdbc.execute("ALTER TABLE stores DROP CONSTRAINT IF EXISTS stores_category_check");
        jdbc.execute("ALTER TABLE stores ADD CONSTRAINT stores_category_check CHECK (category IN ('" + list + "'))");
        log.info("Schema upgrade: store categories now include SHOES and LUGGAGE.");
    }

    void upgradeDrafts() {
        if (!tableExists("products")) return;
        jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS status VARCHAR(20)");
        int backfilled = jdbc.update("UPDATE products SET status = 'PUBLISHED' WHERE status IS NULL");
        jdbc.execute("ALTER TABLE products ALTER COLUMN status SET DEFAULT 'PUBLISHED'");
        jdbc.execute("ALTER TABLE products DROP CONSTRAINT IF EXISTS products_status_check");
        jdbc.execute("ALTER TABLE products ADD CONSTRAINT products_status_check "
                + "CHECK (status IS NULL OR status IN ('DRAFT', 'PUBLISHED'))");
        jdbc.execute("ALTER TABLE products ALTER COLUMN price DROP NOT NULL");
        jdbc.execute("ALTER TABLE products DROP CONSTRAINT IF EXISTS products_published_has_price");
        jdbc.execute("ALTER TABLE products ADD CONSTRAINT products_published_has_price "
                + "CHECK (status = 'DRAFT' OR price IS NOT NULL)");
        if (backfilled > 0) {
            log.info("Schema upgrade: {} existing products marked PUBLISHED.", backfilled);
        }
        log.info("Schema upgrade: draft products enabled (price may be empty for drafts).");
    }
}
