USE payflow_admin;
DELETE FROM flyway_schema_history WHERE success = 0;
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
