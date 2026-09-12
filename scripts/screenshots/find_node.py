"""Print bounds of nodes whose text or content-desc contains a substring."""

import io
import re
import sys


def main(path, needle):
    xml = io.open(path, encoding="utf-8").read()
    for node in re.finditer(r"<node[^>]*>", xml):
        raw = node.group(0)
        text = (re.search(r'text="([^"]*)"', raw) or [None, ""])[1]
        desc = (re.search(r'content-desc="([^"]*)"', raw) or [None, ""])[1]
        bounds = (re.search(r'bounds="([^"]*)"', raw) or [None, ""])[1]
        if needle in text or needle in desc:
            print(f"{text or '[desc] ' + desc:<40} {bounds}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
