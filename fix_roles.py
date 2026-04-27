#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import sys
sys.stdout.reconfigure(encoding='utf-8')
import urllib.request
import urllib.error
import json

def api_call(url, method, token, data=None):
    headers = {
        'Content-Type': 'application/json',
        'Authorization': f'Bearer {token}'
    }
    body = json.dumps(data).encode('utf-8') if data else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            return json.loads(resp.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        return {"http_error": e.code, "body": e.read().decode('utf-8')}
    except Exception as e:
        return {"error": str(e)}

base_uri = "http://localhost:3003/api/v1/admin"

# Login
login_resp = api_call(f"{base_uri}/auth/login", "POST", "", {"username": "admin", "password": "admin123"})
print(f"Login result: code={login_resp.get('code')}, message={login_resp.get('message')}")
token = login_resp.get("data", {}).get("token", "")
if not token:
    print("ERROR: No token obtained!")
    sys.exit(1)

# Roles to fix
roles_to_fix = [
    (1, "SUPER_ADMIN", "超级管理员", "拥有系统所有权限"),
    (2, "ADMIN", "管理员", "拥有大部分管理权限"),
    (3, "FINANCE", "财务人员", "负责财务相关操作"),
    (4, "RISK", "风控人员", "负责风控管理操作"),
]

for role_id, role_code, role_name, description in roles_to_fix:
    data = {"roleCode": role_code, "roleName": role_name, "description": description, "status": "ACTIVE"}
    result = api_call(f"{base_uri}/roles/{role_id}", "PUT", token, data)
    print(f"Role {role_id} ({role_code}): code={result.get('code')}, message={result.get('message')}")

# Verify
print("\n=== Verification: All Roles ===")
verify_result = api_call(f"{base_uri}/roles", "GET", token)
roles = verify_result.get("data", [])
for role in roles:
    print(f"  id={role['id']}, roleCode={role['roleCode']}, roleName={role['roleName']}, description={role['description']}")