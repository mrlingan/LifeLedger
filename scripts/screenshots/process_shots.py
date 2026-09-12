"""Resize the captured screenshots to the README size and drop them into the repo.

    python process_shots.py <shots dir> <repo docs/screenshots dir>

旧截图是 600x1333（1080x2400 等比缩到 600 宽），这里保持一致。
"""

import os
import sys

from PIL import Image, ImageStat

TARGET = (600, 1333)

# 文件名 -> 仓库里的位置
LAYOUT = {
    "home": "home.png",
    "home_dark": "home-dark.png",
    "all": "all-achievements.png",
    "detail": "detail.png",
    "codex": "codex.png",
    "settings": "settings.png",
}


def main(shots_dir, out_dir):
    report = []
    for lang in ("zh", "en"):
        for key, target in LAYOUT.items():
            src = os.path.join(shots_dir, f"{lang}_{key}.png")
            if not os.path.exists(src):
                raise SystemExit(f"缺截图：{src}")
            im = Image.open(src).convert("RGB")
            mean = sum(ImageStat.Stat(im).mean) / 3
            small = im.resize(TARGET, Image.LANCZOS)
            os.makedirs(os.path.join(out_dir, lang), exist_ok=True)
            destination = os.path.join(out_dir, lang, target)
            small.save(destination, format="PNG", optimize=True)
            size = os.path.getsize(destination)
            report.append((f"{lang}/{target}", im.size, f"{mean:6.1f}", f"{size / 1024:6.1f} KB"))

    for row in report:
        print(f"{row[0]:26s} {row[1]}  mean={row[2]}  -> {row[3]}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
