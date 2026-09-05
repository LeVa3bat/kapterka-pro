import re
import os

directory = "app/src/main/java/"
for root, dirs, files in os.walk(directory):
    for file in files:
        if file.endswith(".kt"):
            filepath = os.path.join(root, file)
            with open(filepath, "r") as f:
                content = f.read()
            
            new_content = re.sub(r'contentColor\s*=\s*Color\(0xFF[0-2][0-9A-Fa-f]{5}\)', 'contentColor = Color.White', content)
            
            if new_content != content:
                with open(filepath, "w") as f:
                    f.write(new_content)
                print(f"Patched {filepath}")
