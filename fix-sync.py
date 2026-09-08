import re

with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt", "r") as f:
    text = f.read()

# Replace the block in startSyncForUnit
old_block = """        val db = firestore
        val unitRef = db.collection("units").document(unitKey)
        
        registerUnitListeners(unitKey)"""

new_block = """        registerUnitListeners(unitKey)
        sendPresencePing(cleanKey, callsign, unitName)"""

if old_block in text:
    text = text.replace(old_block, new_block)
else:
    # Just to be sure, find registerUnitListeners(unitKey)
    text = re.sub(r'val db = firestore\s*val unitRef = db\.collection\("units"\)\.document\(unitKey\)\s*registerUnitListeners\(unitKey\)', 'registerUnitListeners(unitKey)\n        sendPresencePing(cleanKey, callsign, unitName)', text)

with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt", "w") as f:
    f.write(text)
