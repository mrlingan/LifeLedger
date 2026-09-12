"""Generate throwaway 'photos' for the emulator demo data (covers, detail images, avatar).

These are only used to take README screenshots on a test device; they are not shipped.
"""

import os
import random
import sys
from PIL import Image, ImageDraw, ImageFilter

W, H = 1080, 720


def vertical_gradient(draw, top, bottom):
    for y in range(H):
        t = y / (H - 1)
        color = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        draw.line([(0, y), (W, y)], fill=color)


def ridge(draw, base_y, height, colour, seed, steps=9):
    rng = random.Random(seed)
    points = [(-10, base_y)]
    for step in range(steps + 1):
        x = -10 + (W + 20) * step / steps
        points.append((x, base_y - rng.randint(0, height)))
    points += [(W + 10, base_y), (W + 10, H), (-10, H)]
    draw.polygon(points, fill=colour)


def scene_sunrise():
    im = Image.new("RGB", (W, H))
    d = ImageDraw.Draw(im)
    vertical_gradient(d, (58, 46, 74), (232, 158, 106))
    d.ellipse([W * 0.62, H * 0.30, W * 0.62 + 150, H * 0.30 + 150], fill=(250, 214, 156))
    ridge(d, int(H * 0.80), int(H * 0.46), (74, 62, 78), 5)
    ridge(d, int(H * 0.92), int(H * 0.30), (40, 35, 46), 11)
    return im.filter(ImageFilter.GaussianBlur(0.7))


def scene_sea():
    im = Image.new("RGB", (W, H))
    d = ImageDraw.Draw(im)
    vertical_gradient(d, (46, 58, 84), (226, 152, 116))
    horizon = int(H * 0.58)
    d.rectangle([0, horizon, W, H], fill=(30, 40, 56))
    for i in range(70):
        y = horizon + i * 3
        shade = 34 + i // 2
        d.line([(0, y), (W, y)], fill=(shade, shade + 10, shade + 22))
    d.ellipse([W * 0.20, horizon - 42, W * 0.20 + 84, horizon + 42], fill=(248, 206, 158))
    return im.filter(ImageFilter.GaussianBlur(1.1))


def scene_forest():
    im = Image.new("RGB", (W, H))
    d = ImageDraw.Draw(im)
    vertical_gradient(d, (208, 214, 200), (120, 140, 118))
    rng = random.Random(23)
    for _ in range(48):
        x = rng.randint(-40, W + 40)
        width = rng.randint(10, 34)
        top = rng.randint(0, int(H * 0.55))
        tone = rng.randint(0, 26)
        d.rectangle([x, top, x + width, H], fill=(26 + tone, 42 + tone, 36 + tone))
    return im.filter(ImageFilter.GaussianBlur(1.6))


def scene_dunes():
    im = Image.new("RGB", (W, H))
    d = ImageDraw.Draw(im)
    vertical_gradient(d, (86, 122, 160), (240, 214, 168))
    rng = random.Random(31)
    for i, base in enumerate((0.62, 0.74, 0.88)):
        pts = []
        for step in range(7):
            pts.append((step * W / 6, H * base - rng.randint(0, 60) + i * 6))
        pts += [(W, H), (0, H)]
        d.polygon(pts, fill=(226 - i * 26, 190 - i * 24, 138 - i * 20))
    return im.filter(ImageFilter.GaussianBlur(0.9))


def scene_night():
    im = Image.new("RGB", (W, H))
    d = ImageDraw.Draw(im)
    vertical_gradient(d, (14, 18, 34), (46, 52, 76))
    rng = random.Random(47)
    for _ in range(220):
        x, y = rng.randint(0, W), rng.randint(0, int(H * 0.62))
        r = rng.choice((1, 1, 2))
        d.ellipse([x, y, x + r, y + r], fill=(238, 238, 226))
    d.ellipse([W * 0.74, H * 0.14, W * 0.74 + 66, H * 0.14 + 66], fill=(246, 244, 226))
    d.rectangle([0, int(H * 0.70), W, H], fill=(18, 24, 38))
    for i in range(60):
        y = int(H * 0.70) + i * 2
        d.line([(0, y), (W, y)], fill=(20 + i, 28 + i, 44 + i))
    return im.filter(ImageFilter.GaussianBlur(0.8))


def make_avatar(size=512):
    """A calm geometric avatar: one muted field plus a soft disc."""
    im = Image.new("RGB", (size, size), (206, 214, 208))
    d = ImageDraw.Draw(im)
    for y in range(size):
        t = y / (size - 1)
        d.line([(0, y), (size, y)], fill=(int(214 - 34 * t), int(220 - 28 * t), int(210 - 26 * t)))
    d.ellipse([size * 0.18, size * 0.22, size * 0.82, size * 0.86], fill=(122, 150, 142))
    d.ellipse([size * 0.30, size * 0.34, size * 0.70, size * 0.74], fill=(232, 236, 230))
    return im.filter(ImageFilter.GaussianBlur(3.0))


def main(outdir):
    os.makedirs(outdir, exist_ok=True)
    scenes = {
        "photo_1.jpg": scene_sunrise,
        "photo_2.jpg": scene_sea,
        "photo_3.jpg": scene_forest,
        "photo_4.jpg": scene_dunes,
        "photo_5.jpg": scene_night,
    }
    for name, builder in scenes.items():
        builder().save(f"{outdir}/{name}", quality=86, optimize=True)
    make_avatar().save(f"{outdir}/avatar.jpg", quality=90, optimize=True)
    print("wrote", ", ".join(list(scenes) + ["avatar.jpg"]), "to", outdir)


if __name__ == "__main__":
    main(sys.argv[1])
