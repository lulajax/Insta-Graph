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
            // Add a constraint to ensure usernames are unique for Blogger nodes
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (b:Blogger) REQUIRE b.username IS UNIQUE");
            session.run("CREATE CONSTRAINT blogger_instagram_id_uk IF NOT EXISTS FOR (b:Blogger) REQUIRE b.instagram_id IS UNIQUE;");
            session.run("CREATE CONSTRAINT post_pk IF NOT EXISTS FOR (p:Post) REQUIRE p.id IS UNIQUE;");
            session.run("CREATE CONSTRAINT hashtag_name_pk IF NOT EXISTS FOR (h:Hashtag) REQUIRE h.name IS UNIQUE;");
            session.run("CREATE CONSTRAINT location_id_pk IF NOT EXISTS FOR (l:Location) REQUIRE l.id IS UNIQUE;");
            session.run("CREATE INDEX blogger_seed_group_idx IF NOT EXISTS FOR (b:Blogger) ON (b.seed_group);");
            LOGGER.info("Database initialization completed successfully.");
        } catch (Exception e) {
            LOGGER.error("Error during database initialization", e);
        }
    }
}
