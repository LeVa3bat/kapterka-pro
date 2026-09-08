import re

with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt", "r") as f:
    text = f.read()

# Most are scope.launch
text = text.replace("val db = firestore ?: return\n", "val db = firestore ?: return@launch\n")
# Except maybe ones not in launch. Let's check:
text = text.replace("return@launch@launch", "return@launch") # just in case

with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/data/license/LicenseManager.kt", "r") as f:
    text = f.read()

# Fix LicenseManager return types
text = re.sub(r'val db = firestore \?: return\n', 'val db = firestore ?: return@withContext Pair(false, "Не удалось подключиться к базе")\n', text)
text = text.replace('val db = firestore ?: return@withContext Pair(false, "Не удалось подключиться к базе (нет ключей)")\n            val doc =', 'val db = firestore ?: return@withContext Pair(false, "Не удалось подключиться к базе (нет ключей)")\n            val doc =')

with open("app/src/main/java/com/example/data/license/LicenseManager.kt", "w") as f:
    f.write(text)

