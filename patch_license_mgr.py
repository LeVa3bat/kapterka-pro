import re

with open("app/src/main/java/com/example/data/license/LicenseManager.kt", "r") as f:
    text = f.read()

text = text.replace('val db = firestore ?: return@withContext Pair(false, "Не удалось подключиться к базе")', 'val db = firestore')

with open("app/src/main/java/com/example/data/license/LicenseManager.kt", "w") as f:
    f.write(text)
