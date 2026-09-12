"""Reduce a seeded database back to the state right after onboarding.

    python make_baseline.py <db path>

删掉自建成就、笔记和媒体，并把自增序列退回 109，这样中英两次播种会拿到
完全相同的行 id（截图里的两套数据才是同一个应用状态）。
"""

import sqlite3
import sys


def main(path):
    con = sqlite3.connect(path)
    con.execute("DELETE FROM media")
    con.execute("DELETE FROM notes")
    con.execute("DELETE FROM achievements WHERE presetId IS NULL")
    con.execute("UPDATE sqlite_sequence SET seq = 109 WHERE name = 'achievements'")
    con.commit()
    con.execute("PRAGMA wal_checkpoint(TRUNCATE)")
    print("rows left:", con.execute("SELECT COUNT(*) FROM achievements").fetchone()[0])
    print("sequence  :", con.execute("SELECT seq FROM sqlite_sequence WHERE name='achievements'").fetchone())
    con.close()


if __name__ == "__main__":
    main(sys.argv[1])
