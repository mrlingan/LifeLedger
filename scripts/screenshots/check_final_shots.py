"""Final sanity check on the screenshots that went into docs/screenshots."""

import glob
import hashlib
import os
import sys

from PIL import Image


def main(root):
    hashes = {}
    files = sorted(glob.glob(os.path.join(root, "*", "*.png")))
    for path in files:
        name = os.path.relpath(path, root)
        im = Image.open(path)
        digest = hashlib.sha256(open(path, "rb").read()).hexdigest()[:12]
        hashes.setdefault(digest, []).append(name)
        print(f"{name:28s} {im.size} {im.mode} {os.path.getsize(path) / 1024:6.1f} KB  {digest}")
    duplicates = [group for group in hashes.values() if len(group) > 1]
    print()
    print("files:", len(files), " distinct:", len(hashes), " duplicates:", duplicates)


if __name__ == "__main__":
    main(sys.argv[1])
