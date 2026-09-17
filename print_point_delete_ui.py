with open("app/src/main/java/com/example/ui/screens/MainDashboardScreen.kt") as f:
    text = f.read()

for line in text.splitlines():
    if "deleteWarehousePoint" in line or "deletePoint" in line or "onDeletePoint" in line:
        print(line)

with open("app/src/main/java/com/example/ui/components/PointDialogs.kt") as f:
    text2 = f.read()

print("PointDialogs:")
for line in text2.splitlines():
    if "delete" in line.lower() or "point" in line.lower() and "on" in line.lower():
        print(line)
