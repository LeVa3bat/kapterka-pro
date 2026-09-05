import re

with open('app/src/main/java/com/example/ui/screens/MoreSettingsScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Remove the reset profile button
new_content = re.sub(r'                Button\(\s*onClick = onResetProfileAndLicense,.*?Text\("Выйти и сбросить лицензию \(для проверки новым бойцом\)", fontSize = 11\.sp\)\s*\}', '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/MoreSettingsScreen.kt', 'w', encoding='utf-8') as f:
    f.write(new_content)

print("Length difference:", len(content) - len(new_content))
