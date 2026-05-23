#!/usr/bin/env python3
"""快速核对商户回调演示数据。"""
import json
import urllib.request

import mysql.connector

conn = mysql.connector.connect(host="127.0.0.1", user="root", password="root")
cur = conn.cursor()
cur.execute(
    "SELECT summary_status, COUNT(*) FROM payflow_cashier.cashier_merchant_notify "
    "WHERE notify_id LIKE 'MN-DEMO-%' GROUP BY summary_status ORDER BY summary_status"
)
print("汇总按状态:", cur.fetchall())
cur.execute(
    "SELECT COUNT(*) FROM payflow_cashier.cashier_merchant_notify_attempt "
    "WHERE notify_id LIKE 'MN-DEMO-%'"
)
print("明细条数:", cur.fetchone()[0])

body = json.dumps({"username": "admin", "password": "admin123"}).encode()
login = json.loads(
    urllib.request.urlopen(
        urllib.request.Request(
            "http://127.0.0.1:3003/api/v1/admin/auth/login",
            data=body,
            headers={"Content-Type": "application/json"},
        )
    ).read()
)
token = login["data"]["token"]
req = urllib.request.Request(
    "http://127.0.0.1:3003/api/v1/admin/merchant-notifies?page=1&size=20",
    headers={"Authorization": f"Bearer {token}"},
)
res = json.loads(urllib.request.urlopen(req).read())
print("API total:", res["data"]["total"])
cur.close()
conn.close()
