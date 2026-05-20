import mysql.connector

c = mysql.connector.connect(
    host="127.0.0.1", user="root", password="root", database="payflow_admin"
)
cur = c.cursor()
for t in [
    "admin_churn_alert",
    "admin_dashboard_metrics",
    "admin_channels",
    "admin_merchants",
]:
    cur.execute("SHOW TABLES LIKE %s", (t,))
    print(t, "exists" if cur.fetchone() else "missing")

for table in ("admin_merchants", "admin_channels"):
    cur.execute(
        """
        SELECT COLUMN_NAME FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'payflow_admin' AND TABLE_NAME = %s
        ORDER BY ORDINAL_POSITION
        """,
        (table,),
    )
    print(table, "cols:", [r[0] for r in cur.fetchall()])

cur.close()
c.close()
