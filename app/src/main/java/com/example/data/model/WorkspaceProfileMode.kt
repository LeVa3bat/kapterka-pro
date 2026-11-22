package com.example.data.model

/**
 * Стартовый и рабочий профиль учёта (Путь А).
 * Определяет набор типовых позиций при первом запуске, терминологию и комплект отчётов Excel.
 */
enum class WorkspaceProfileMode(
    val code: String,
    val titleRu: String,
    val shortBadge: String,
    val subtitleRu: String
) {
    CIVILIAN(
        code = "civilian",
        titleRu = "Гражданский склад / Мастерская",
        shortBadge = "Мастерская",
        subtitleRu = "Инструмент, расходники, СИЗ, крепёж, М-11 / ТОРГ-13 / МБ-7 / МБ-8"
    ),
    MILITARY(
        code = "military",
        titleRu = "Воинский учёт / Подразделение",
        shortBadge = "Подразделение",
        subtitleRu = "Службы РАВ, БПЛА, Связь, Вещевая, ГСМ, Форма № 8 и Форма № 18"
    ),
    EMPTY(
        code = "empty",
        titleRu = "Пустой склад (с нуля)",
        shortBadge = "Свой склад",
        subtitleRu = "Один чистый склад без навязанных позиций — всё на ваше усмотрение"
    );

    val isMilitary: Boolean get() = this == MILITARY

    companion object {
        fun fromCode(code: String?): WorkspaceProfileMode =
            values().firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) } ?: MILITARY
    }
}
