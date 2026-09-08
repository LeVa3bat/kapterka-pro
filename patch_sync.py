import re
with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt", "r") as f:
    text = f.read()

# Replace `val db = firestore` with `val db = firestore ?: return`
text = re.sub(r'val db = firestore\n', 'val db = firestore ?: return\n', text)
# If it was already replaced partially, fix it
text = text.replace("val db = firestore ?: return ?: return", "val db = firestore ?: return")

with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt", "w") as f:
    f.write(text)
