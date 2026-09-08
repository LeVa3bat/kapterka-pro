import re

with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt", "r") as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if i in [94, 232, 253]: # Python is 0-indexed, so 95->94
        lines[i] = lines[i].replace("return@launch", "return")

with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt", "w") as f:
    f.writelines(lines)
