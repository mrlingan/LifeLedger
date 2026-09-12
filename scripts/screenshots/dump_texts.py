"""Save the visible texts of a uiautomator dump, and flag ciphertext leaking into the UI."""

import io
import re
import sys


def main(path):
    xml = io.open(path, encoding="utf-8").read()
    texts = [t for t in re.findall(r'text="([^"]+)"', xml) if t.strip()]
    out = path.rsplit(".", 1)[0] + ".txt"
    io.open(out, "w", encoding="utf-8").write("\n".join(texts))
    leaked = [t for t in texts if t.startswith("enc1:") or t.startswith("enc2:")]
    print("nodes:", len(texts), "| ciphertext leaked into UI:", len(leaked))
    for t in leaked[:5]:
        print("  LEAK:", t[:60])


if __name__ == "__main__":
    main(sys.argv[1])
