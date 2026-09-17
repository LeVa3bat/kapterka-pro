with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt") as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "fun pushStockRecordAsync" in line:
        for j in range(i - 5, min(i + 30, len(lines))):
            print(f"{j+1}: {lines[j]}", end="")
        break
