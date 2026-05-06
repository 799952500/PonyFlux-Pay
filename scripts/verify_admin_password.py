import mysql.connector
import bcrypt


def main() -> None:
    conn = mysql.connector.connect(
        host="127.0.0.1",
        port=3306,
        user="root",
        password="root",
        database="payflow_admin",
        autocommit=True,
        use_pure=True,
    )
    cur = conn.cursor()
    cur.execute("select username,password,status from admin_users")
    rows = cur.fetchall()
    print("admin_users:", rows)
    for username, pw_hash, status in rows:
        if username == "admin":
            for candidate in ["admin123", "Admin123", "123456", "admin"]:
                ok = bcrypt.checkpw(candidate.encode("utf-8"), pw_hash.encode("utf-8"))
                print("verify", candidate, ok)
    cur.close()
    conn.close()


if __name__ == "__main__":
    main()

