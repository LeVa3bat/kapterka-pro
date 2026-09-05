import re
with open('app/src/main/java/com/example/MainActivity.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = re.sub(r'com\.example\.util\.AppTerminology\.clearMode\(context\)\s*', '', content)

with open('app/src/main/java/com/example/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(content)
