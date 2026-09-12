"""Set the demo profile (nickname / signature / avatar path) in the app's SharedPreferences.

    python set_prefs.py <prefs xml> <zh|en> <avatar absolute path on device>

其它键（比如加密用的包装数据密钥）原样保留。
"""

import sys
import xml.etree.ElementTree as ET

PROFILE = {
    "zh": ("小满", "慢慢来，比较快。"),
    "en": ("May", "Slow is smooth, smooth is fast."),
}


def main(path, lang, avatar_path):
    tree = ET.parse(path)
    root = tree.getroot()
    values = {
        "profile_nickname": PROFILE[lang][0],
        "profile_signature": PROFILE[lang][1],
        "profile_avatar_path": avatar_path,
        "theme_mode": "SYSTEM",
    }
    for name, value in values.items():
        node = root.find(f"./string[@name='{name}']")
        if node is None:
            node = ET.SubElement(root, "string", {"name": name})
        node.text = value
    tree.write(path, encoding="utf-8", xml_declaration=True)

    # Android 期望的声明是 standalone='yes'
    text = open(path, encoding="utf-8").read()
    text = text.replace(
        "<?xml version='1.0' encoding='utf-8'?>",
        "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>",
    )
    open(path, "w", encoding="utf-8", newline="\n").write(text)
    print(text)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], sys.argv[3])
