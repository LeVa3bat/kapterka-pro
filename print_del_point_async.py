with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt") as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "fun deleteWarehousePointAsync" in line:
        for j in range(i, min(i + 35, len(lines))):
            print(f"{j+1}: {lines[j]}", end="")
        break
