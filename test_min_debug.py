with open("app/build.gradle.kts", "r") as f:
    text = f.read()

# Let's inspect debug block
import re
print(re.search(r'buildTypes\s*\{.*?\}', text, re.DOTALL).group(0))
