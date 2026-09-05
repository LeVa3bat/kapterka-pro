import os

directory = "app/src/main/java/"
for root, dirs, files in os.walk(directory):
    for file in files:
        if file.endswith(".kt"):
            filepath = os.path.join(root, file)
            with open(filepath, "r") as f:
                content = f.read()
            if "Color.White" in content and "import androidx.compose.ui.graphics.Color" not in content:
                print(f"Missing import in {filepath}")
                # patch it
                lines = content.split('\n')
                for i, line in enumerate(lines):
                    if line.startswith("import "):
                        lines.insert(i, "import androidx.compose.ui.graphics.Color")
                        break
                with open(filepath, "w") as f:
                    f.write('\n'.join(lines))
