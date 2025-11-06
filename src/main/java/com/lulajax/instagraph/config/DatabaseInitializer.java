package com.lulajax.instagraph.config;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
public class DatabaseInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseInitializer.class);
    private final Driver driver;

    public DatabaseInitializer(Driver driver) {
        this.driver = driver;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeDatabase() {
        LOGGER.info("Initializing database constraints and indexes...");
        try (Session session = driver.session()) {
            session.run("CREATE CONSTRAINT blogger_username_pk IF NOT EXISTS FOR (b:Blogger) REQUIRE b.username IS NODE KEY;");
            session.run("CREATE CONSTRAINT post_id_pk IF NOT EXISTS FOR (p:Post) REQUIRE p.post_id IS NODE KEY;");
            session.run("CREATE CONSTRAINT hashtag_name_pk IF NOT EXISTS FOR (h:Hashtag) REQUIRE h.name IS NODE KEY;");
            session.run("CREATE INDEX blogger_seed_group_idx IF NOT EXISTS FOR (b:Blogger) ON (b.seed_group);");
            LOGGER.info("Database initialization complete.");
        } catch (Exception e) {
            LOGGER.error("Error during database initialization", e);
        }
    }
}
