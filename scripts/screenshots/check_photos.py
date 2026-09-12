"""Coarse checks on generated images: size, spread, and a brightness grid."""

import glob
import os
import sys

from PIL import Image, ImageStat


def grid(im, cols=16, rows=8):
    small = im.convert("L").resize((cols, rows))
    chars = " .:-=+*#%@"
    lines = []
    for y in range(rows):
        line = ""
        for x in range(cols):
            value = small.getpixel((x, y))
            line += chars[min(len(chars) - 1, value * len(chars) // 256)]
        lines.append(line)
    return lines


def main(folder):
    for path in sorted(glob.glob(os.path.join(folder, "*.jpg"))):
        im = Image.open(path)
        stat = ImageStat.Stat(im)
        mean = tuple(round(v) for v in stat.mean)
        std = tuple(round(v) for v in stat.stddev)
        print(f"{os.path.basename(path):>14}  {im.size}  mean={mean}  stddev={std}")
        for line in grid(im):
            print("                |" + line + "|")


if __name__ == "__main__":
    main(sys.argv[1])
