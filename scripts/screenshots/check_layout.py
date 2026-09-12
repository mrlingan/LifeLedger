"""Geometry checks on a uiautomator dump: the four home metrics must be centred in their columns.

    python check_layout.py <ui xml> <label1> <label2> ...

对每个标签，找到它下面同一列里最近的那个数字，比较两者中心是否对齐；
再看四个数字的中心是不是等距分布、左右留白是否对称。
"""

import io
import re
import sys


def nodes_of(xml):
    out = []
    for raw in re.finditer(r"<node[^>]*>", xml):
        chunk = raw.group(0)
        text = re.search(r'text="([^"]*)"', chunk)
        bounds = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', chunk)
        if text and bounds and text.group(1).strip():
            x1, y1, x2, y2 = map(int, bounds.groups())
            out.append({"text": text.group(1), "x1": x1, "x2": x2, "y1": y1, "y2": y2})
    return out


def center(node):
    return (node["x1"] + node["x2"]) / 2


def main(path, labels):
    nodes = nodes_of(io.open(path, encoding="utf-8").read())
    groups = []
    for label in labels:
        lab = next(n for n in nodes if n["text"] == label)
        below = [
            n
            for n in nodes
            if n["y1"] > lab["y1"]
            and abs(center(n) - center(lab)) < 130
            and n["text"] not in labels
        ]
        number = min(below, key=lambda n: n["y1"])
        groups.append((label, lab, number))

    print(f"{'列':<12}{'标签中心':>10}{'数字中心':>12}{'偏差':>8}")
    for label, lab, number in groups:
        print(f"{label:<12}{center(lab):>10.1f}{center(number):>12.1f}{abs(center(lab) - center(number)):>8.1f}")

    centers = [center(lab) for _, lab, _ in groups]
    gaps = [round(centers[i + 1] - centers[i]) for i in range(len(centers) - 1)]
    print()
    print("四列中心 :", [round(c) for c in centers])
    print("相邻间距 :", gaps, " 极差 =", max(gaps) - min(gaps))
    print("左留白 / 右留白 :", round(centers[0]), "/", round(1080 - centers[-1]))
    print("屏幕中线 :", 540, " 平均中心 :", round(sum(centers) / len(centers)))


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2:])
