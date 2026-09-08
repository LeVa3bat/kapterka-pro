import re
with open("app/src/main/java/com/example/data/license/LicenseManager.kt", "r") as f:
    text = f.read()

text = re.sub(r'val db = firestore\n', 'val db = firestore ?: return\n', text)
text = re.sub(r'val db = firestore\s*val doc =', 'val db = firestore ?: return@withContext Pair(false, "Не удалось подключиться к базе (нет ключей)")\n            val doc =', text)

with open("app/src/main/java/com/example/data/license/LicenseManager.kt", "w") as f:
    f.write(text)
