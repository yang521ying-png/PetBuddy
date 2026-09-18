# -*- coding: utf-8 -*-
"""布偶素材处理：白底去背景 -> 裁剪 -> 缩放 -> 输出 drawable PNG 与 mipmap 图标"""
import os
import numpy as np
from PIL import Image, ImageDraw

RAW = "raw_assets"
DRAWABLE = "app/src/main/res/drawable"
MIPMAP = "app/src/main/res"

MAPPING = {
    "pet_normal.jpeg": "pet_normal.png",
    "pet_happy.jpeg": "pet_happy.png",
    "pet_hungry.jpeg": "pet_hungry.png",
    "pet_sleeping.jpeg": "pet_sleeping.png",
}

def alpha_bbox(img):
    a = np.array(img)[:, :, 3]
    rows = np.where(np.any(a > 12, axis=1))[0]
    cols = np.where(np.any(a > 12, axis=0))[0]
    if len(rows) == 0 or len(cols) == 0:
        return None
    return (int(cols[0]), int(rows[0]), int(cols[-1]) + 1, int(rows[-1]) + 1)

def remove_white_bg(img, thresh=42):
    """从四角 flood fill 白色背景为透明，保留主体内部白色"""
    for corner in [(0, 0), (img.width - 1, 0), (0, img.height - 1), (img.width - 1, img.height - 1)]:
        ImageDraw.floodfill(img, corner, (0, 0, 0, 0), thresh=thresh)
    return img

def fit_centered(img, canvas, padding_ratio=0.06):
    """把透明图按比例缩放后居中放在 canvas 尺寸的透明画布上"""
    pad = int(canvas * padding_ratio)
    max_w, max_h = canvas - pad * 2, canvas - pad * 2
    img.thumbnail((max_w, max_h), Image.LANCZOS)
    canvas_img = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    canvas_img.paste(img, ((canvas - img.width) // 2, (canvas - img.height) // 2), img)
    return canvas_img

os.makedirs(DRAWABLE, exist_ok=True)

for src_name, dst_name in MAPPING.items():
    src = os.path.join(RAW, src_name)
    img = Image.open(src).convert("RGBA")
    img = remove_white_bg(img)
    bbox = alpha_bbox(img)
    if bbox is None:
        print(f"[warn] {src_name}: empty after bg removal, skip")
        continue
    img = img.crop(bbox)
    # 四周加 4% 留白再缩放到 256
    w, h = img.size
    margin = int(max(w, h) * 0.04)
    img = Image.new("RGBA", (w + margin * 2, h + margin * 2), (0, 0, 0, 0))
    img.paste(Image.open(src).convert("RGBA").crop(bbox), (margin, margin))
    img.thumbnail((256, 256), Image.LANCZOS)
    out = os.path.join(DRAWABLE, dst_name)
    img.save(out)
    print(f"ok {dst_name} {img.size}")

# ---- 应用图标：基于 pet_normal 生成各密度 mipmap ----
launcher = Image.open(os.path.join(DRAWABLE, "pet_normal.png")).convert("RGBA")
sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}
for folder, size in sizes.items():
    out_dir = os.path.join(MIPMAP, folder)
    os.makedirs(out_dir, exist_ok=True)
    fit_centered(launcher, size).save(os.path.join(out_dir, "ic_launcher.png"))
    print(f"icon {folder} {size}x{size}")
print("done")
