with open("app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt") as f:
    text = f.read()

import re
matches = re.findall(r'fun\s+([a-zA-Z0-9_]+)\s*\(', text)
for m in matches:
    print(m)
