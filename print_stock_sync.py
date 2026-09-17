with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt") as f:
    lines = f.readlines()
for i in range(600, 635):
    if i < len(lines):
        print(f"{i+1}: {lines[i]}", end="")
