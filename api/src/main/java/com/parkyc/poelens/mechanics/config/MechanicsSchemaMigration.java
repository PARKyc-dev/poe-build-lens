package com.parkyc.poelens.mechanics.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MechanicsSchemaMigration implements ApplicationRunner {

    private static final List<String> OVERRIDE_KEY = List.of("NAME", "GAME_VERSION");
    private static final List<String> VERSIONED_DELIVERY_KEY = List.of("NAME", "GAME_VERSION", "DELIVERY");

    private final JdbcTemplate jdbcTemplate;

    public MechanicsSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        migrate();
    }

    void migrate() {
        Map<String, List<String>> uniqueConstraints = uniqueConstraints();
        uniqueConstraints.entrySet().stream()
                .filter(entry -> entry.getValue().equals(VERSIONED_DELIVERY_KEY))
                .map(Map.Entry::getKey)
                .forEach(this::dropConstraint);

        if (!uniqueConstraints().containsValue(OVERRIDE_KEY)) {
            jdbcTemplate.execute("""
                    ALTER TABLE mechanics ADD CONSTRAINT uk_mechanics_name_game_version
                    UNIQUE (name, game_version)
                    """);
        }
    }

    private Map<String, List<String>> uniqueConstraints() {
        return jdbcTemplate.query("""
                SELECT tc.constraint_name, kcu.column_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                  ON tc.constraint_catalog = kcu.constraint_catalog
                 AND tc.constraint_schema = kcu.constraint_schema
                 AND tc.constraint_name = kcu.constraint_name
                WHERE tc.table_schema = CURRENT_SCHEMA
                  AND tc.table_name = 'MECHANICS'
                  AND tc.constraint_type = 'UNIQUE'
                ORDER BY tc.constraint_name, kcu.ordinal_position
                """, resultSet -> {
            Map<String, List<String>> constraints = new LinkedHashMap<>();
            while (resultSet.next()) {
                constraints.computeIfAbsent(resultSet.getString("constraint_name"), ignored -> new java.util.ArrayList<>())
                        .add(resultSet.getString("column_name"));
            }
            return constraints;
        });
    }

    private void dropConstraint(String name) {
        if (!name.matches("[A-Za-z0-9_]+")) {
            throw new IllegalStateException("Unexpected mechanics constraint name: " + name);
        }
        jdbcTemplate.execute("ALTER TABLE mechanics DROP CONSTRAINT " + name);
    }
}
