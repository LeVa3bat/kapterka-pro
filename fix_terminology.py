with open('app/src/main/java/com/example/util/AppTerminology.kt', 'r', encoding='utf-8') as f:
    content = f.read()

new_methods = """
    fun clearMode(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun getDefaultUnitName(mode: AppMode) = when(mode) {
        AppMode.MILITARY -> "1-е Подразделение"
        AppMode.CONSTRUCTION -> "Объект 1"
        AppMode.WAREHOUSE -> "Главный склад"
        AppMode.UNIVERSAL -> "Основная группа"
    }

    fun getUnitSyncText(mode: AppMode) = when(mode) {
        AppMode.MILITARY -> "Вход в подразделение"
        AppMode.CONSTRUCTION -> "Вход на объект"
        AppMode.WAREHOUSE -> "Вход на склад"
        AppMode.UNIVERSAL -> "Присоединиться к группе"
    }

    fun getForm8Label(mode: AppMode) = when(mode) {
        AppMode.MILITARY -> "Форма №8 (Расход)"
        AppMode.CONSTRUCTION -> "Акт расхода"
        AppMode.WAREHOUSE -> "Расходная накладная"
        AppMode.UNIVERSAL -> "Журнал расхода"
    }
    
    fun getForm18Label(mode: AppMode) = when(mode) {
        AppMode.MILITARY -> "Оборотная ведомость подразделения"
        AppMode.CONSTRUCTION -> "Ведомость объекта"
        AppMode.WAREHOUSE -> "Оборотная ведомость склада"
        AppMode.UNIVERSAL -> "Сводный отчет"
    }

    fun getAppRole(mode: AppMode) = when(mode) {
        AppMode.MILITARY -> "Старшина подразделения"
        AppMode.CONSTRUCTION -> "Прораб / Кладовщик"
        AppMode.WAREHOUSE -> "Заведующий складом"
        AppMode.UNIVERSAL -> "Администратор"
    }
"""

if "fun getDefaultUnitName" not in content:
    content = content.replace("}", new_methods + "\n}")

with open('app/src/main/java/com/example/util/AppTerminology.kt', 'w', encoding='utf-8') as f:
    f.write(content)
