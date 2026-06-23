"""
Скрипт верификации датасета после очистки.

Проверяет:
1. Отсутствие multi-class label-файлов
2. Целостность: каждому label соответствует изображение и наоборот
"""

from pathlib import Path


DATASET_ROOT = Path(__file__).resolve().parent.parent / "ds"
SPLITS = ["train", "valid", "test"]
IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}


def verify_split(split_name: str) -> dict:
    """Верифицирует один сплит датасета."""
    labels_dir = DATASET_ROOT / split_name / "labels"
    images_dir = DATASET_ROOT / split_name / "images"

    result = {
        "split": split_name,
        "label_count": 0,
        "image_count": 0,
        "multi_class_found": 0,
        "labels_without_images": [],
        "images_without_labels": [],
    }

    # Собираем stems
    label_stems = set()
    image_stems = set()

    if labels_dir.exists():
        for lf in labels_dir.glob("*.txt"):
            label_stems.add(lf.stem)
            result["label_count"] += 1

            # Проверяем multi-class
            classes = set()
            with open(lf, "r", encoding="utf-8") as f:
                for line in f:
                    line = line.strip()
                    if line:
                        classes.add(line.split()[0])
            if len(classes) > 1:
                result["multi_class_found"] += 1

    if images_dir.exists():
        for img in images_dir.iterdir():
            if img.suffix.lower() in IMAGE_EXTENSIONS:
                image_stems.add(img.stem)
                result["image_count"] += 1

    # Проверяем целостность
    result["labels_without_images"] = sorted(label_stems - image_stems)
    result["images_without_labels"] = sorted(image_stems - label_stems)

    return result


def main():
    print("=" * 60)
    print("Dataset Verification")
    print(f"Dataset root: {DATASET_ROOT}")
    print("=" * 60)

    all_ok = True

    for split in SPLITS:
        print(f"\n--- {split} ---")
        r = verify_split(split)

        print(f"  Labels: {r['label_count']}, Images: {r['image_count']}")

        if r["multi_class_found"] > 0:
            print(f"  FAIL: {r['multi_class_found']} multi-class labels found!")
            all_ok = False
        else:
            print(f"  OK: No multi-class labels")

        if r["labels_without_images"]:
            print(f"  FAIL: {len(r['labels_without_images'])} labels without images")
            for name in r["labels_without_images"][:5]:
                print(f"    - {name}")
            all_ok = False
        else:
            print(f"  OK: All labels have matching images")

        if r["images_without_labels"]:
            print(f"  WARN: {len(r['images_without_labels'])} images without labels")
            for name in r["images_without_labels"][:5]:
                print(f"    - {name}")
        else:
            print(f"  OK: All images have matching labels")

    print("\n" + "=" * 60)
    if all_ok:
        print("RESULT: ALL CHECKS PASSED")
    else:
        print("RESULT: SOME CHECKS FAILED")
    print("=" * 60)


if __name__ == "__main__":
    main()
