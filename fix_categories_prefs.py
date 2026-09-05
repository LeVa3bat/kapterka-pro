import re

with open('app/src/main/java/com/example/ui/viewmodel/KapterkaViewModel.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = """    private fun loadCategoriesFromPrefs(): List<String> {
        val saved = prefs.getStringSet("saved_categories", null)
        return if (saved != null && saved.isNotEmpty()) {
            val defaultOrder = com.example.data.local.InitialData.getDefaultCategories()
            val list = defaultOrder.filter { it in saved }.toMutableList()
            saved.filter { it !in defaultOrder }.forEach { list.add(it) }
            list
        } else {
            com.example.data.local.InitialData.getDefaultCategories()
        }
    }"""

new_code = """    private fun loadCategoriesFromPrefs(): List<String> {
        val saved = prefs.getStringSet("saved_categories", null)
        val defaultOrder = com.example.data.local.InitialData.getDefaultCategories()
        return if (saved != null && saved.isNotEmpty()) {
            val list = defaultOrder.toMutableList() // Always include default categories (to force new ones)
            saved.filter { it !in defaultOrder }.forEach { list.add(it) }
            list
        } else {
            defaultOrder
        }
    }"""

if target in content:
    content = content.replace(target, new_code)
    with open('app/src/main/java/com/example/ui/viewmodel/KapterkaViewModel.kt', 'w', encoding='utf-8') as f:
        f.write(content)
    print("Updated loadCategoriesFromPrefs")
else:
    print("Target not found")
