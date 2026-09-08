import re
with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt", "r") as f:
    text = f.read()

text = text.replace("try {\n            registerUnitListeners(unitKey)\n        } catch(e: Exception) {}", "registerUnitListeners(unitKey)")

with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt", "w") as f:
    f.write(text)
