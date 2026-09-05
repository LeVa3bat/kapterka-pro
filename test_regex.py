import re
import os

directory = "app/src/main/java/"
for root, dirs, files in os.walk(directory):
    for file in files:
        if file.endswith(".kt"):
            filepath = os.path.join(root, file)
            with open(filepath, "r") as f:
                content = f.read()
            
            # Find contentColor = Color(0xFF...)
            matches = re.findall(r'contentColor\s*=\s*Color\((0xFF[0-9A-Fa-f]{6})\)', content)
            if matches:
                for m in matches:
                    print(f"{filepath}: {m}")
