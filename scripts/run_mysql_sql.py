import argparse
import pathlib

import mysql.connector


def split_sql(text: str) -> list[str]:
    out: list[str] = []
    buf: list[str] = []

    in_single = False
    in_double = False
    escape = False

    for ch in text:
        if escape:
            buf.append(ch)
            escape = False
            continue

        if ch == "\\":
            buf.append(ch)
            escape = True
            continue

        if ch == "'" and not in_double:
            in_single = not in_single
            buf.append(ch)
            continue

        if ch == '"' and not in_single:
            in_double = not in_double
            buf.append(ch)
            continue

        if ch == ";" and not in_single and not in_double:
            stmt = "".join(buf).strip()
            buf = []
            if stmt:
                out.append(stmt)
            continue

        buf.append(ch)

    tail = "".join(buf).strip()
    if tail:
        out.append(tail)
    return out


def is_comment_only(stmt: str) -> bool:
    for line in stmt.splitlines():
        s = line.strip()
        if not s:
            continue
        if s.startswith("--"):
            continue
        return False
    return True


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--host", default="127.0.0.1")
    p.add_argument("--port", type=int, default=3306)
    p.add_argument("--user", default="root")
    p.add_argument("--password", default="root")
    p.add_argument("--file", required=True)
    args = p.parse_args()

    sql_path = pathlib.Path(args.file)
    text = sql_path.read_text(encoding="utf-8")
    stmts = split_sql(text)

    conn = mysql.connector.connect(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        autocommit=True,
        use_pure=True,
    )
    cur = conn.cursor()

    executed = 0
    for idx, st in enumerate(stmts, start=1):
        if not st or is_comment_only(st):
            continue
        try:
            cur.execute(st)
            if getattr(cur, "with_rows", False):
                cur.fetchall()
            executed += 1
        except Exception as e:  # noqa: BLE001
            preview = st.replace("\n", " ")[:200]
            raise RuntimeError(f"SQL failed at #{idx}: {preview} ... -> {e}") from e

    cur.close()
    conn.close()
    print(f"OK: executed {executed} statements from {sql_path}")


if __name__ == "__main__":
    main()

