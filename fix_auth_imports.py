with open('app/src/main/java/com/example/ui/screens/AuthScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

if 'import androidx.compose.material3.Card' not in content:
    content = content.replace('import androidx.compose.material3.Button', 'import androidx.compose.material3.Button\nimport androidx.compose.material3.Card\nimport androidx.compose.material3.CardDefaults')

with open('app/src/main/java/com/example/ui/screens/AuthScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
