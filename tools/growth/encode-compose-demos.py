"""Build captioned MP4 demonstrations only from verified production Compose captures.

The source PNGs are copied byte-for-byte for RuStore. Video captions sit outside
the screenshot area. These are demonstrations of rendered screens on test data,
not recordings of a physical Android device. No service uploads are performed.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import subprocess
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[2]
WIDTH, HEIGHT = 1080, 1920
DISCLOSURE = "Демонстрация экранов · тестовые данные"
REQUIRED_FRAMES = {
    "01-dashboard-before", "02-stock-before", "03-issue-empty", "04-issue-ready",
    "05-stock-after", "06-dashboard-after", "07-journal", "08-journal-expanded",
    "09-local-stock", "10-report-preview", "11-report-exported",
}


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    candidates = [
        Path("C:/Windows/Fonts") / ("arialbd.ttf" if bold else "arial.ttf"),
        Path("/usr/share/fonts/truetype/dejavu") / ("DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"),
    ]
    for candidate in candidates:
        if candidate.is_file():
            return ImageFont.truetype(str(candidate), size)
    raise RuntimeError("A Cyrillic font is required; Arial/DejaVu Sans was not found")


def wrap(draw: ImageDraw.ImageDraw, text: str, face: ImageFont.FreeTypeFont, width: int) -> list[str]:
    lines: list[str] = []
    current = ""
    for word in text.split():
        candidate = f"{current} {word}".strip()
        if current and draw.textlength(candidate, font=face) > width:
            lines.append(current)
            current = word
        else:
            current = candidate
    if current:
        lines.append(current)
    return lines


def video_frame(source: Path, title: str, caption: str, output: Path) -> None:
    canvas = Image.new("RGB", (WIDTH, HEIGHT), "#0b1410")
    draw = ImageDraw.Draw(canvas)
    draw.text((54, 34), "Каптёрка ПРО", font=font(40, True), fill="#9ce4bc")
    draw.text((54, 89), DISCLOSURE, font=font(31), fill="#c3d2c8")
    draw.text((54, 142), title, font=font(40, True), fill="white")
    with Image.open(source) as image:
        screenshot = image.convert("RGB")
        screenshot.thumbnail((972, 1398), Image.Resampling.LANCZOS)
        canvas.paste(screenshot, ((WIDTH - screenshot.width) // 2, 220 + (1398 - screenshot.height) // 2))
    lines = wrap(draw, caption, font(37), WIDTH - 108)
    if len(lines) > 4:
        raise ValueError(f"Caption exceeds the reserved area: {caption}")
    for index, line in enumerate(lines):
        draw.text((54, 1650 + index * 49), line, font=font(37), fill="white")
    draw.text((54, 1862), "kapterka-pro.ru", font=font(28), fill="#9ce4bc")
    canvas.save(output)


def ffmpeg_path(explicit: str | None) -> str:
    if explicit:
        path = Path(explicit).resolve()
        if not path.is_file():
            raise FileNotFoundError(path)
        return str(path)
    found = shutil.which("ffmpeg")
    if found:
        return found
    packages = ROOT / "growth/.tmp/media-tools"
    if packages.is_dir():
        binaries = list((packages / "imageio_ffmpeg/binaries").glob("ffmpeg-*.exe"))
        if sys.platform == "win32" and len(binaries) == 1:
            return str(binaries[0])
        sys.path.insert(0, str(packages))
    try:
        import imageio_ffmpeg
        return imageio_ffmpeg.get_ffmpeg_exe()
    except ImportError as error:
        raise RuntimeError("FFmpeg is required. Install imageio-ffmpeg==0.6.0 into growth/.tmp/media-tools or pass --ffmpeg") from error


def srt_time(seconds: float) -> str:
    milliseconds = int(round(seconds * 1000))
    hours, milliseconds = divmod(milliseconds, 3_600_000)
    minutes, milliseconds = divmod(milliseconds, 60_000)
    seconds, milliseconds = divmod(milliseconds, 1000)
    return f"{hours:02d}:{minutes:02d}:{seconds:02d},{milliseconds:03d}"


def update_publication_kit(videos: Path, manifest: dict) -> None:
    """Keep copy tied to actual frames; never claim a phone recording or Excel opening."""
    import re
    path = videos / "publication-kit.json"
    kit = json.loads(path.read_text(encoding="utf-8"))
    kit["status"] = "Созданы и проверены 3 MP4 по 30 секунд. Демонстрации настоящего интерфейса на тестовых данных. Внешняя публикация не выполнялась."
    kit["captureManifestFile"] = "capture-manifest.json"
    descriptions = {
        "issue_check": "Демонстрация экранов Каптёрки ПРО на вымышленных данных: в кладовой было 12 отвёрток, выдаём 3 на рабочее место, проверяем остатки 9 и 3 и запись в журнале. Операция и остатки получены через настоящую логику приложения в отдельной учебной базе.",
        "offline_stock": "Демонстрация локальных остатков и журнала на вымышленных данных: в учебной базе сохранено 9 отвёрток. В условном ручном пересчёте получилось 8; сначала выясняем причину разницы. Облачная синхронизация учебной базы отключена. Испытание физического телефона без сети этим роликом не подтверждается.",
        "report_export": "Демонстрация экранов отчёта на вымышленных данных: открываем ведомость кладовой и нажимаем «Скачать Excel». Настоящий XLSX создан приложением; в строке отвёртки подтверждён остаток 9 штук. Файл demo-point-stock.xlsx входит в комплект ролика. Для экспорта нужен действующий демо или PRO; в учебном профиле указан тестовый PRO.",
    }
    result_by_id = {Path(result["file"]).stem.replace("-", "_"): result for result in manifest["videoFiles"]}
    for video in kit["videos"]:
        result = result_by_id[video["id"]]
        video.update({
            "status": "Готовый MP4; полный decode проверен. Последовательность реальных кадров интерфейса с подписями, вымышленные данные.",
            "videoFile": result["file"], "subtitleFile": result["subtitles"],
            "posterFile": f"posters/{Path(result['file']).stem}.png",
            "durationSeconds": result["durationSeconds"], "videoSha256": result["sha256"],
            "physicalDeviceRecording": False, "published": False,
        })
        for channel in ("rutube", "telegram"):
            urls = re.findall(r"https://[^\s]+", video[channel]["description"])
            if len(urls) != 2:
                raise ValueError(f"Expected two fixed existing links for {video['id']}/{channel}")
            video[channel]["description"] = descriptions[video["id"]] + f"\n\nИнструкция: {urls[0]}\nУстановка из RuStore: {urls[1]}\n\n30-секундный ролик собран из отдельных захваченных экранов приложения на тестовых данных."
        video["site"]["description"] = descriptions[video["id"]]
        video["site"]["implementationStatus"] = "Локальный MP4 готов и проверен. Для встраивания нужен реальный URL после разрешённой публикации."
        if video["id"] == "offline_stock":
            video["rutube"]["title"] = "Как сверить локальный остаток: учёт и ручной пересчёт"
            video["site"]["title"] = "Сверка локального остатка"
        if video["id"] == "report_export":
            video["exportFile"] = "demo-point-stock.xlsx"
    path.write_text(json.dumps(kit, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--capture-dir", type=Path, default=ROOT / "app/build/outputs/growth-capture")
    parser.add_argument("--ffmpeg")
    parser.add_argument("--check", action="store_true", help="Validate captures and encoder without creating videos")
    args = parser.parse_args()
    captures = args.capture_dir.resolve()
    source_manifest = captures / "capture-source.json"
    source = json.loads(source_manifest.read_text(encoding="utf-8"))
    if "not physical-device recording" not in source.get("kind", ""):
        raise ValueError("Capture manifest must identify the real source and recording limitations")
    verified = source["verified"]
    if verified != {"before": 12, "issued": 3, "storeAfter": 9, "benchAfter": 3, "conservedTotal": 12, "xlsxContainsStoreQuantity": 9}:
        raise ValueError(f"Repository calculation has not been verified for this demonstration: {verified}")
    frames: dict[str, Path] = {}
    for frame in source["frames"]:
        path = (captures / frame["file"]).resolve()
        if path.parent != captures or sha256(path) != frame["sha256"]:
            raise ValueError(f"Capture was changed after rendering: {path}")
        if path.stem in frames:
            raise ValueError(f"Duplicate capture in manifest: {path.stem}")
        with Image.open(path) as image:
            if image.size != (frame["width"], frame["height"]):
                raise ValueError(f"Capture dimensions changed: {path}")
        frames[path.stem] = path
    if set(frames) != REQUIRED_FRAMES:
        raise ValueError(f"Incomplete capture set: missing {sorted(REQUIRED_FRAMES - set(frames))}, unexpected {sorted(set(frames) - REQUIRED_FRAMES)}")
    export = (captures / source["export"]["file"]).resolve()
    if export.parent != captures:
        raise ValueError("Export path must stay in the capture directory")
    if sha256(export) != source["export"]["sha256"]:
        raise ValueError("The generated XLSX was changed after verification")
    ffmpeg = ffmpeg_path(args.ffmpeg)
    probe = subprocess.run([ffmpeg, "-version"], check=True, capture_output=True, text=True)
    encoder_version = probe.stdout.splitlines()[0]
    if args.check:
        print(json.dumps({"status": "PASS", "capturedFrames": len(frames), "exportBytes": export.stat().st_size, "encoder": encoder_version}, ensure_ascii=False))
        return

    videos = ROOT / "growth/videos"
    store = ROOT / "growth/rustore/screenshots"
    posters = videos / "posters"
    working = ROOT / "growth/.tmp/compose-video"
    for directory in (videos, store, posters, working):
        directory.mkdir(parents=True, exist_ok=True)
    store_files = []
    for position, name in enumerate(("06-dashboard-after", "05-stock-after", "08-journal-expanded"), start=1):
        target = store / f"{position:02d}-{name}.png"
        shutil.copyfile(frames[name], target)
        store_files.append({"file": target.relative_to(ROOT).as_posix(), "sourceFrame": name, "sha256": sha256(target)})
    shutil.copyfile(export, videos / "demo-point-stock.xlsx")

    clips = {
        "issue-check": ("Выдача и проверка остатка", [
            ("02-stock-before", 6, "В учебной кладовой 12 отвёрток. Открываем выдачу."),
            ("04-issue-ready", 8, "Выбираем отвёртку и 3 штуки. Получатель — рабочее место."),
            ("05-stock-after", 8, "Сохранено через приложение: в кладовой осталось 9, на рабочем месте — 3."),
            ("08-journal-expanded", 8, "В журнале проверяем направление и количество. Выдача отличается от списания."),
        ]),
        "offline-stock": ("Сверка локального остатка", [
            ("09-local-stock", 10, "Сохранённый остаток в учебной базе — 9 отвёрток. Облачная синхронизация отключена."),
            ("09-local-stock", 10, "При ручном пересчёте получилось 8. Разница — минус 1. Сначала выясняем причину."),
            ("08-journal-expanded", 10, "Проверяем последнюю выдачу в журнале. Этот пример не проверяет свежесть данных другого телефона."),
        ]),
        "report-export": ("Настоящий отчёт Excel", [
            ("06-dashboard-after", 8, "Открываем «Отчёты» из главного экрана. В примере действует тестовый PRO."),
            ("10-report-preview", 12, "Выбираем ведомость кладовой и проверяем данные. Экспорт доступен с действующим демо или PRO."),
            ("11-report-exported", 10, "Нажали «Скачать Excel». Получен настоящий XLSX: отвёртка — 9 штук. Файл приложен к ролику."),
        ]),
    }
    results = []
    for name, (title, scenes) in clips.items():
        files, subtitles, elapsed = [], [], 0
        for index, (frame, duration, caption) in enumerate(scenes):
            slide = working / f"{name}-{index:02d}.png"
            video_frame(frames[frame], title, caption, slide)
            files.extend([f"file '{slide.as_posix()}'", f"duration {duration}"])
            subtitles.append(f"{index + 1}\n{srt_time(elapsed)} --> {srt_time(elapsed + duration)}\n{caption}\n")
            elapsed += duration
        files.append(files[-2])
        concat = working / f"{name}.ffconcat"
        concat.write_text("ffconcat version 1.0\n" + "\n".join(files) + "\n", encoding="utf-8")
        target = videos / f"{name}.mp4"
        subprocess.run([ffmpeg, "-hide_banner", "-loglevel", "error", "-y", "-f", "concat", "-safe", "0", "-i", str(concat),
                        "-an", "-vf", "fps=30", "-filter_threads", "1", "-t", str(elapsed), "-c:v", "libx264", "-threads", "2", "-preset", "veryfast", "-crf", "20",
                        "-pix_fmt", "yuv420p", "-movflags", "+faststart", str(target)], check=True)
        subtitle = videos / f"{name}.srt"
        subtitle.write_text("\n".join(subtitles), encoding="utf-8")
        shutil.copyfile(working / f"{name}-00.png", posters / f"{name}.png")
        # A complete decode confirms a usable file; encoding success alone is insufficient.
        subprocess.run([ffmpeg, "-hide_banner", "-loglevel", "error", "-threads", "2", "-i", str(target), "-f", "null", "-"], check=True)
        results.append({"file": target.name, "sha256": sha256(target), "durationSeconds": elapsed, "width": WIDTH, "height": HEIGHT,
                        "fps": 30, "audio": False, "subtitles": subtitle.name, "sourceFrames": [scene[0] for scene in scenes], "decodeVerified": True})
        print(f"Created {target.relative_to(ROOT).as_posix()} ({elapsed}s)", flush=True)
    manifest = {
        "status": "Сгенерированы 3 MP4 и 3 актуальных снимка настоящего Compose-интерфейса на вымышленных данных",
        "packageName": "com.aistudio.kapterka.jmwqve",
        "renderedAppVersionName": source["appVersionName"], "renderedAppVersionCode": source["appVersionCode"],
        "renderDateMoscow": source["captureDateMoscow"],
        "encodedDateMoscow": datetime.now(timezone(timedelta(hours=3))).isoformat(timespec="seconds"),
        "sourceType": "Robolectric native Compose renders; slideshow with captions; not physical-device screen recording",
        "sourceManifest": "app/build/outputs/growth-capture/capture-source.json",
        "physicalDeviceRecording": False, "dataType": source["dataOrigin"], "verified": verified,
        "encoder": encoder_version, "screenResolution": [source["frames"][0]["width"], source["frames"][0]["height"]],
        "screenshotFiles": store_files, "videoFiles": results,
        "covers": [f"covers/{name}.png" for name in clips],
        "posters": [f"posters/{name}.png" for name in clips],
        "subtitleStatus": "*.srt — синхронизированы с созданными MP4. *.planned.srt — прежний план съёмки",
        "exportFile": {"file": "demo-point-stock.xlsx", "sha256": sha256(videos / "demo-point-stock.xlsx")},
        "published": False,
    }
    (videos / "capture-manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    shutil.copyfile(source_manifest, videos / "capture-source.json")
    update_publication_kit(videos, manifest)
    print("PASS: verified 3 videos, synchronized subtitles, original screenshots and actual XLSX")


if __name__ == "__main__":
    main()
