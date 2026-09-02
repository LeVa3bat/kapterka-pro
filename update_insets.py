import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# Add imports for WindowCompat and WindowInsetsCompat and WindowInsetsControllerCompat
imports = """import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat"""
if "androidx.core.view.WindowCompat" not in content:
    content = content.replace("import androidx.activity.ComponentActivity", imports + "\nimport androidx.activity.ComponentActivity")

on_create = """    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        TacticalNotificationHelper.createNotificationChannel(this)"""

content = re.sub(r'override fun onCreate\(savedInstanceState: Bundle\?\) \{\s*super\.onCreate\(savedInstanceState\)\s*enableEdgeToEdge\(\)\s*TacticalNotificationHelper\.createNotificationChannel\(this\)', on_create, content)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
