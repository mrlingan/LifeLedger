"""Print a quick summary of the pulled LifeLedger database."""

import os
import sqlite3
import sys
import time


def main(path):
    now = int(time.time() * 1000)
    con = sqlite3.connect(path)
    tables = [r[0] for r in con.execute("SELECT name FROM sqlite_master WHERE type='table'")]
    print("tables   :", tables)
    print("presets  :", con.execute("SELECT COUNT(*) FROM preset_achievements").fetchone()[0])
    print("achieve  :", con.execute("SELECT COUNT(*) FROM achievements").fetchone()[0])
    print("done     :", con.execute("SELECT COUNT(*) FROM achievements WHERE isCompleted=1").fetchone()[0])
    print("notes    :", con.execute("SELECT COUNT(*) FROM notes").fetchone()[0])
    print("media    :", con.execute("SELECT COUNT(*) FROM media").fetchone()[0])
    print("--- first 3 rows ---")
    for row in con.execute(
        "SELECT id, title, presetId, isCompleted, createdDate, completedDate, iconEmoji"
        " FROM achievements ORDER BY id LIMIT 3"
    ):
        print(
            "  id={0} title={1!r} presetId={2} done={3} created_days_ago={4:.2f} icon={5!r} size={6} bytes".format(
                row[0], row[1][:20], row[2], row[3], (now - row[4]) / 86400000, row[6], os.path.getsize(path)
            )
        )
    con.close()


if __name__ == "__main__":
    main(sys.argv[1])
