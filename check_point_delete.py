import re

with open("app/src/main/java/com/example/ui/screens/WarehousePointsScreen.kt", "r") as f:
    points_screen = f.read()

print("Points screen snippet around delete:")
for line in points_screen.splitlines():
    if "delete" in line.lower() or "point" in line.lower() and "on" in line.lower():
        print("  ", line)

with open("app/src/main/java/com/example/ui/viewmodel/KapterkaViewModel.kt", "r") as f:
    vm = f.read()

print("\nViewModel snippet around deletePoint:")
for line in vm.splitlines():
    if "deletePoint" in line or "deleteWarehousePoint" in line:
        print("  ", line)
