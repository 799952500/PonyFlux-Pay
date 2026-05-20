import mysql.connector

conn = mysql.connector.connect(
    host="127.0.0.1",
    user="root",
    password="root",
    database="payflow_admin",
    autocommit=True,
)
cur = conn.cursor()
cur.execute("SELECT COUNT(*) FROM flyway_schema_history WHERE version = %s", ("6",))
if cur.fetchone()[0] == 0:
    cur.execute(
        """
        INSERT INTO flyway_schema_history
        (installed_rank, version, description, type, script,
         checksum, installed_by, installed_on, execution_time, success)
        SELECT COALESCE(MAX(installed_rank), 0) + 1,
               '6', 'dashboard and routing', 'SQL',
               'V6__dashboard_and_routing.sql',
               NULL, 'manual', NOW(), 0, 1
        FROM flyway_schema_history
        """
    )
    print("flyway_schema_history v6 recorded")
else:
    print("flyway_schema_history v6 already exists")
cur.close()
conn.close()
