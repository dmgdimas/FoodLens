"""
Скрипт для очистки YOLO-датасета от изображений с несколькими классами.

Анализирует label-файлы во всех сплитах (train, valid, test).
Если в label-файле встречаются аннотации с более чем одним уникальным class_id,
удаляет и label-файл, и соответствующее изображение.
"""

import os
from pathlib import Path


# Путь к корню датасета (относительно корня проекта)
DATASET_ROOT = Path(__file__).resolve().parent.parent / "ds"
SPLITS = ["train", "valid", "test"]


def get_unique_classes(label_path: Path) -> set[str]:
    """Извлекает уникальные class_id из label-файла."""
    classes = set()
    with open(label_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line:
                class_id = line.split()[0]
                classes.add(class_id)
    return classes


def find_image_for_label(label_path: Path, images_dir: Path) -> Path | None:
    """Находит файл изображения, соответствующий label-файлу."""
    stem = label_path.stem
    for img_path in images_dir.glob(f"{stem}.*"):
        if img_path.suffix.lower() in (".jpg", ".jpeg", ".png", ".bmp", ".webp"):
            return img_path
    return None


def clean_split(split_name: str) -> dict:
    """Очищает один сплит датасета от multi-class изображений."""
    labels_dir = DATASET_ROOT / split_name / "labels"
    images_dir = DATASET_ROOT / split_name / "images"

    stats = {
        "split": split_name,
        "checked": 0,
        "deleted": 0,
        "deleted_files": [],
        "warnings": [],
    }

    if not labels_dir.exists():
        stats["warnings"].append(f"Labels directory not found: {labels_dir}")
        return stats

    label_files = sorted(labels_dir.glob("*.txt"))
    stats["checked"] = len(label_files)

    for label_path in label_files:
        classes = get_unique_classes(label_path)

        # Если больше одного уникального класса — удаляем
        if len(classes) > 1:
            stats["deleted"] += 1

            # Удаляем изображение
            img_path = find_image_for_label(label_path, images_dir)
            if img_path and img_path.exists():
                os.remove(img_path)
                stats["deleted_files"].append(str(img_path.name))
            else:
                stats["warnings"].append(
                    f"Image not found for label: {label_path.name}"
                )

            # Удаляем label
            os.remove(label_path)

    return stats


def main():
    print("=" * 60)
    print("Dataset Cleaner: удаление multi-class изображений")
    print(f"Dataset root: {DATASET_ROOT}")
    print("=" * 60)

    total_checked = 0
    total_deleted = 0

    for split in SPLITS:
        print(f"\n--- Processing split: {split} ---")
        stats = clean_split(split)

        total_checked += stats["checked"]
        total_deleted += stats["deleted"]

        print(f"  Checked:  {stats['checked']} label files")
        print(f"  Deleted:  {stats['deleted']} multi-class pairs")

        if stats["warnings"]:
            for w in stats["warnings"]:
                print(f"  ⚠ {w}")

    print("\n" + "=" * 60)
    print("ИТОГО:")
    print(f"  Проверено:  {total_checked} файлов")
    print(f"  Удалено:    {total_deleted} multi-class пар")
    print(f"  Осталось:   {total_checked - total_deleted} файлов")
    print("=" * 60)


if __name__ == "__main__":
    main()
