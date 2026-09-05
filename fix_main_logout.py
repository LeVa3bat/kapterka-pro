import re
with open('app/src/main/java/com/example/MainActivity.kt', 'r', encoding='utf-8') as f:
    content = f.read()

replacement = """
                            onLogoutClick = {
                                val current = profile ?: com.example.data.model.UserProfile()
                                viewModel.updateProfile(current.copy(isLoggedIn = false))
                                com.example.util.AppTerminology.clearMode(context)
                            },
"""

content = re.sub(r'onLogoutClick = \{\s*val current = profile \?: com\.example\.data\.model\.UserProfile\(\)\s*viewModel\.updateProfile\(current\.copy\(isLoggedIn = false\)\)\s*\},', replacement.strip(), content)

with open('app/src/main/java/com/example/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(content)
