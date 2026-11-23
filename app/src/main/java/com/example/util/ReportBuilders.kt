package com.example.util

import com.example.data.model.InventoryItem
import com.example.data.model.OperationItemEntry
import com.example.data.model.OperationRecord
import com.example.data.model.OperationType
import com.example.data.model.RequisitionRequest
import com.example.data.model.StockRecord
import com.example.data.model.WarehousePoint
import com.example.data.model.WorkspaceProfileMode
import com.example.data.model.isReturnOperation
import com.example.data.model.returnSenderName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ReportColAlign { START, CENTER, END }

data class StructuredReport(
    val sheetName: String,
    val fileBaseName: String,
    val docFormTitle: String,
    val approvalLines: List<String>,
    val formCodeRows: List<Pair<String, String>>,
    val docMainTitle: String,
    val subTitle: String,
    val details: List<Pair<String, String>>,
    val headers: List<String>,
    val colWidthsDp: List<Int>,
    val alignments: List<ReportColAlign>,
    val rows: List<List<String>>,
    val sectionRowIndices: Set<Int> = emptySet(),
    val totalRow: List<String>? = null,
    val extraFooterRows: List<List<String>> = emptyList(),
    val signaturesTitle: String = "Подписи должностных лиц:",
    val signers: List<Triple<String, String, String>> = emptyList()
) {
    fun toExcelReportData(): ExcelReportData {
        val cleanSheet = sheetName
            .replace("[/\\\\?*\\]\\[:]".toRegex(), " ")
            .take(31)
            .trim()
            .ifBlank { "Ведомость" }
        return ExcelReportData(
            sheetName = cleanSheet,
            title = docMainTitle,
            subtitle = subTitle,
            details = details,
            headers = headers.map { it.replace("\n", " ") },
            colWidthsChars = colWidthsDp.map { (it / 5.0).coerceIn(8.0, 48.0) },
            rows = rows.map { r -> r.map { it.replace("\n", " ") } },
            totalRow = totalRow?.map { it.replace("\n", " ") },
            signers = signers,
            approvalLines = approvalLines,
            formCodeRows = formCodeRows,
            sectionRowIndices = sectionRowIndices,
            extraFooterRows = extraFooterRows.map { r -> r.map { it.replace("\n", " ") } },
            showColumnNumbers = true
        )
    }

    fun toTsvText(): String {
        val sb = StringBuilder()
        if (approvalLines.isNotEmpty()) {
            approvalLines.forEach { sb.append("\t\t\t").append(it).append('\n') }
        }
        sb.append(docMainTitle).append('\t').append(docFormTitle.replace("\n", " ")).append('\n')
        if (subTitle.isNotBlank()) sb.append(subTitle).append('\n')
        details.forEach { (k, v) -> sb.append(k).append('\t').append(v).append('\n') }
        sb.append('\n')
        sb.append(headers.joinToString("\t") { it.replace("\n", " ") }).append('\n')
        sb.append(headers.indices.joinToString("\t") { (it + 1).toString() }).append('\n')
        rows.forEach { r -> sb.append(r.joinToString("\t") { it.replace("\n", " ") }).append('\n') }
        totalRow?.let { sb.append(it.joinToString("\t") { c -> c.replace("\n", " ") }).append('\n') }
        extraFooterRows.forEach { r -> sb.append(r.joinToString("\t") { c -> c.replace("\n", " ") }).append('\n') }
        if (signers.isNotEmpty()) {
            sb.append('\n').append(signaturesTitle).append('\n')
            signers.forEach { (role, sig, person) ->
                sb.append(role).append('\t').append(sig).append('\t').append(person).append('\n')
            }
        }
        return sb.toString()
    }
}

data class ResolvedReportItem(
    val key: String,
    val itemId: String,
    val name: String,
    val code: String,
    val unit: String,
    val category: String,
    val categoryClass: String,
    val quantity: Int,
    val reason: String
)

object ReportBuilders {

    private val ruLocale = Locale("ru")
    private fun dateFmt() = SimpleDateFormat("dd.MM.yyyy", ruLocale)
    private fun dateTimeFmt() = SimpleDateFormat("dd.MM.yyyy HH:mm", ruLocale)

    private fun safeFileToken(raw: String): String =
        raw.trim().replace(Regex("[^\\p{L}\\p{N}_-]+"), "_").trim('_').ifBlank { "Склад" }

    /**
     * Форматирует суммы без смешивания разных единиц измерения:
     * - одинаковые ед. изм. → чистое число (для формул Excel),
     * - разные ед. изм. → «120 шт.; 450 л.».
     */
    fun formatUnitSafeTotals(entries: List<Pair<String, Int>>): String {
        val nonZero = entries.filter { it.second != 0 }
        if (nonZero.isEmpty()) return "0"
        val byUnit = linkedMapOf<String, Int>()
        for ((rawUnit, qty) in nonZero) {
            val u = rawUnit.trim().ifBlank { "шт." }
            byUnit[u] = (byUnit[u] ?: 0) + qty
        }
        return if (byUnit.size == 1) {
            byUnit.values.first().toString()
        } else {
            byUnit.entries.joinToString("; ") { (u, sum) -> "$sum $u" }
        }
    }

    /**
     * Надёжно извлекает позиции операции: из itemsJson, а если itemsJson пустой (архивные записи) —
     * восстанавливает из itemsSummary, обогащая данными из каталога.
     */
    fun resolveOperationEntries(
        op: OperationRecord,
        catalogItems: List<InventoryItem>,
        parseItems: (String) -> List<OperationItemEntry>
    ): List<ResolvedReportItem> {
        val byId = catalogItems.associateBy { it.id }
        val byName = catalogItems.associateBy { it.name.trim().lowercase(ruLocale) }

        val parsed = parseItems(op.itemsJson).ifEmpty { parseFallbackSummary(op.itemsSummary) }
        return parsed.filter { it.quantity > 0 || it.itemName.isNotBlank() }.map { entry ->
            val cat = byId[entry.itemId] ?: byName[entry.itemName.trim().lowercase(ruLocale)]
            val resolvedId = entry.itemId.ifBlank { cat?.id.orEmpty() }
            val resolvedName = entry.itemName.ifBlank { cat?.name ?: resolvedId.ifBlank { "Позиция" } }
            val resolvedKey = resolvedId.ifBlank { resolvedName.trim().lowercase(ruLocale) }
            val resolvedCode = cat?.standardCode?.takeIf { it.isNotBlank() }
                ?: resolvedId.takeIf { it.isNotBlank() && it.length <= 14 }
                ?: "—"
            val resolvedUnit = entry.unit.ifBlank { cat?.unit ?: "шт." }
            val resolvedCategory = cat?.serviceCategory?.ifBlank { "Прочее" } ?: "Прочее"
            val resolvedClass = entry.categoryClass.ifBlank { cat?.categoryClass ?: "Кат. 1" }
            ResolvedReportItem(
                key = resolvedKey,
                itemId = resolvedId,
                name = resolvedName,
                code = resolvedCode,
                unit = resolvedUnit,
                category = resolvedCategory,
                categoryClass = resolvedClass,
                quantity = entry.quantity.coerceAtLeast(0),
                reason = entry.reason
            )
        }
    }

    private fun parseFallbackSummary(summary: String): List<OperationItemEntry> {
        if (summary.isBlank()) return emptyList()
        val pattern = Regex("""^(.+?)\s*[—–-]\s*(\d+)\s*(.*)$""")
        val rawParts = summary.split(Regex(""",\s+"""))
        val merged = mutableListOf<String>()
        var buffer = ""
        for (part in rawParts) {
            val clean = part.trim()
            if (clean.isEmpty()) continue
            val candidate = if (buffer.isEmpty()) clean else "$buffer, $clean"
            if (pattern.matches(candidate)) {
                merged.add(candidate)
                buffer = ""
            } else {
                buffer = candidate
            }
        }
        if (buffer.isNotEmpty()) merged.add(buffer)

        return merged.mapNotNull { chunk ->
            val m = pattern.matchEntire(chunk)
            if (m != null) {
                OperationItemEntry(
                    itemId = "",
                    itemName = m.groupValues[1].trim(),
                    unit = m.groupValues[3].trim().ifBlank { "шт." },
                    quantity = m.groupValues[2].toIntOrNull() ?: 0
                )
            } else {
                OperationItemEntry("", chunk, "шт.", 1)
            }
        }
    }

    private fun isInternalPointDestination(
        toPointName: String,
        points: List<WarehousePoint>
    ): Boolean {
        val clean = toPointName.trim().lowercase(ruLocale)
        if (clean.isEmpty()) return false
        return points.any { it.name.trim().lowercase(ruLocale) == clean }
    }

    // =========================================================================
    // ВОЕННЫЙ ПРОФИЛЬ: ФОРМА № 8 (ОКУД 6002203, Приказ МО РФ от 28.03.2008 № 139)
    // =========================================================================
    fun buildMilitaryForm8(
        operations: List<OperationRecord>,
        catalogItems: List<InventoryItem>,
        unitName: String,
        responsiblePerson: String = "",
        parseItems: (String) -> List<OperationItemEntry>
    ): StructuredReport {
        val issues = operations.filter { it.type == OperationType.ISSUE }.sortedBy { it.timestamp }
        val df = dateFmt()
        val today = df.format(Date())
        val periodFrom = issues.firstOrNull()?.let { df.format(Date(it.timestamp)) } ?: today
        val periodTo = issues.lastOrNull()?.let { df.format(Date(it.timestamp)) } ?: today
        val mol = responsiblePerson.ifBlank { "________________________" }

        data class IssueLine(
            val recipient: String,
            val recipientCode: String,
            val date: String,
            val qtyByKey: Map<String, Int>
        )

        val materials = linkedMapOf<String, ResolvedReportItem>()
        val lines = mutableListOf<IssueLine>()

        for (op in issues) {
            val resolved = resolveOperationEntries(op, catalogItems, parseItems)
            if (resolved.isEmpty()) continue
            resolved.forEach { materials.putIfAbsent(it.key, it) }
            val qtyMap = resolved.groupBy { it.key }.mapValues { (_, list) -> list.sumOf { it.quantity } }
            val recipient = op.toPointName.ifBlank { "—" }
            lines.add(
                IssueLine(
                    recipient = recipient,
                    recipientCode = op.docNumber.trim(),
                    date = df.format(Date(op.timestamp)),
                    qtyByKey = qtyMap
                )
            )
        }

        val matKeys = materials.keys.toList()
        val headers = buildList {
            add("№\nп/п")
            add("Получатель\n(сдатчик)")
            add("Код\nполучателя\n(сдатчика)")
            matKeys.forEach { key ->
                val m = materials.getValue(key)
                add("${m.name}\nкод: ${m.code}\n${m.unit} • ${m.categoryClass}")
            }
            if (matKeys.isEmpty()) {
                add("Наименование\nматериальных\nценностей")
            }
            add("Дата\nполучения\n(сдачи)")
            add("Расписка\nполучателя\n(сдатчика)")
        }

        val widths = buildList {
            add(34); add(108); add(76)
            if (matKeys.isEmpty()) add(120) else repeat(matKeys.size) { add(100) }
            add(78); add(90)
        }
        val aligns = buildList {
            add(ReportColAlign.CENTER); add(ReportColAlign.START); add(ReportColAlign.CENTER)
            if (matKeys.isEmpty()) add(ReportColAlign.CENTER) else repeat(matKeys.size) { add(ReportColAlign.END) }
            add(ReportColAlign.CENTER); add(ReportColAlign.CENTER)
        }

        val rows = mutableListOf<List<String>>()
        lines.forEachIndexed { idx, line ->
            rows.add(
                buildList {
                    add((idx + 1).toString())
                    add(line.recipient)
                    add(line.recipientCode)
                    if (matKeys.isEmpty()) {
                        add("")
                    } else {
                        matKeys.forEach { key ->
                            val q = line.qtyByKey[key] ?: 0
                            add(if (q > 0) q.toString() else "")
                        }
                    }
                    add(line.date)
                    add("")
                }
            )
        }
        if (rows.isEmpty()) {
            rows.add(List(headers.size) { if (it == 1) "Выдач за выбранный период нет" else "" })
        }

        val totalRow = buildList {
            add("")
            add("Итого (количество)")
            add("X")
            if (matKeys.isEmpty()) {
                add("0")
            } else {
                matKeys.forEach { key -> add(lines.sumOf { it.qtyByKey[key] ?: 0 }.toString()) }
            }
            add("X")
            add("X")
        }

        val priceRow = buildList {
            add("")
            add("Цена за единицу, руб. коп.")
            add("X")
            repeat(maxOf(matKeys.size, 1)) { add("") }
            add("X")
            add("X")
        }
        val sumRow = buildList {
            add("")
            add("Сумма, руб. коп.")
            add("X")
            repeat(maxOf(matKeys.size, 1)) { add("") }
            add("X")
            add("X")
        }

        val issueSources = issues.map { it.fromPointName }.filter { it.isNotBlank() }.distinct().joinToString(", ")

        return StructuredReport(
            sheetName = "Форма 8 (ОКУД 6002203)",
            fileBaseName = "Форма_8_ОКУД_6002203_${safeFileToken(unitName)}",
            docFormTitle = "Форма № 8 по ОКУД 6002203\nПриказ МО РФ от 28.03.2008 № 139",
            approvalLines = listOf(
                "УТВЕРЖДАЮ",
                "Командир (начальник) ____________________",
                "(воинское звание, подпись, инициал имени, фамилия)",
                "«___» ________________ 20__ г."
            ),
            formCodeRows = listOf(
                "Форма № 8 по ОКУД" to "6002203",
                "Дата" to today,
                "по ОКПО" to "________"
            ),
            docMainTitle = "РАЗДАТОЧНАЯ (СДАТОЧНАЯ) ВЕДОМОСТЬ № ___ МАТЕРИАЛЬНЫХ ЦЕННОСТЕЙ",
            subTitle = "за период с $periodFrom по $periodTo",
            details = listOf(
                "Воинская часть:" to "________________________ (условное / действительное наименование)",
                "Структурное подразделение:" to unitName.ifBlank { "Подразделение" },
                "Склад / точка выдачи:" to issueSources.ifBlank { "Базовый склад" },
                "Материально ответственное лицо:" to mol,
                "Ведомость действительна по:" to "«__» ____________ 20__ г."
            ),
            headers = headers,
            colWidthsDp = widths,
            alignments = aligns,
            rows = rows,
            totalRow = totalRow,
            extraFooterRows = listOf(priceRow, sumRow),
            signaturesTitle = "Строки «Цена за единицу» и «Сумма» заполняются финансовым органом (бухгалтерией).",
            signers = listOf(
                Triple("Выдал:", "____________________", "(должность, воинское звание, подпись, инициал имени, фамилия)"),
                Triple("Принял:", "____________________", "(должность, воинское звание, подпись, инициал имени, фамилия)"),
                Triple("Правильность выдачи (приема) проверил:", "____________________", "«__» ________ 20__ г."),
                Triple("Бухгалтер:", "____________________", "(подпись, инициал имени, фамилия) «__» ________ 20__ г.")
            )
        )
    }

    // =========================================================================
    // ВОЕННЫЙ ПРОФИЛЬ: ФОРМА № 18 (Книга учёта наличия и движения МЦ, ОКУД 0504042)
    // =========================================================================
    fun buildMilitaryForm18(
        operations: List<OperationRecord>,
        stockRecords: List<StockRecord>,
        points: List<WarehousePoint>,
        catalogItems: List<InventoryItem>,
        unitName: String,
        responsiblePerson: String = "",
        parseItems: (String) -> List<OperationItemEntry>
    ): StructuredReport {
        val df = dateFmt()
        val today = df.format(Date())
        val ordered = operations.sortedBy { it.timestamp }
        val periodFrom = ordered.firstOrNull()?.let { df.format(Date(it.timestamp)) } ?: today
        val mol = responsiblePerson.ifBlank { "________________________" }

        data class Move(val op: OperationRecord, val item: ResolvedReportItem)

        val byItem = linkedMapOf<String, MutableList<Move>>()
        val itemMeta = linkedMapOf<String, ResolvedReportItem>()

        for (op in ordered) {
            for (entry in resolveOperationEntries(op, catalogItems, parseItems)) {
                itemMeta.putIfAbsent(entry.key, entry)
                byItem.getOrPut(entry.key) { mutableListOf() }.add(Move(op, entry))
            }
        }

        // Также включаем позиции, которые числятся на остатках (например, импорт/синхронизация без операций).
        val catalogMap = catalogItems.associateBy { it.id }
        val pointMap = points.associateBy { it.id }
        val stockByItemId = stockRecords.groupBy { it.itemId }
        for ((itemId, recs) in stockByItemId) {
            val totalQty = recs.sumOf { it.quantity }
            if (totalQty <= 0 && recs.sumOf { it.incomeTotal + it.expenseTotal } <= 0) continue
            val cat = catalogMap[itemId]
            val key = itemId.ifBlank { cat?.name?.lowercase(ruLocale) ?: continue }
            if (!itemMeta.containsKey(key)) {
                itemMeta[key] = ResolvedReportItem(
                    key = key,
                    itemId = itemId,
                    name = cat?.name ?: itemId,
                    code = cat?.standardCode?.ifBlank { itemId.take(12) } ?: itemId.take(12),
                    unit = cat?.unit ?: "шт.",
                    category = cat?.serviceCategory ?: "Прочее",
                    categoryClass = cat?.categoryClass ?: "Кат. 1",
                    quantity = totalQty,
                    reason = ""
                )
                byItem.putIfAbsent(key, mutableListOf())
            }
        }

        val servicesPresent = itemMeta.values.map { it.category }.filter { it.isNotBlank() }.distinct()
        val headers = listOf(
            "№\nзаписи",
            "Дата\nзаписи",
            "Документ\n(наименование,\nдата, номер)",
            "От кого получено /\nКому отпущено",
            "Приход",
            "Расход",
            "Состоит\n(остаток)",
            "Местонахождение /\nсклад",
            "Контроль\n(подпись, дата)"
        )
        val widths = listOf(42, 72, 122, 168, 58, 58, 68, 112, 82)
        val aligns = listOf(
            ReportColAlign.CENTER,
            ReportColAlign.CENTER,
            ReportColAlign.START,
            ReportColAlign.START,
            ReportColAlign.END,
            ReportColAlign.END,
            ReportColAlign.END,
            ReportColAlign.START,
            ReportColAlign.CENTER
        )

        val rows = mutableListOf<List<String>>()
        val sectionIndices = mutableSetOf<Int>()
        val incomingPairs = mutableListOf<Pair<String, Int>>()
        val outgoingPairs = mutableListOf<Pair<String, Int>>()
        val endingPairs = mutableListOf<Pair<String, Int>>()

        for ((key, moves) in byItem) {
            val meta = itemMeta.getValue(key)
            sectionIndices.add(rows.size)
            rows.add(
                listOf(
                    "",
                    "",
                    "▌ ${meta.name}",
                    "Код (КВТ): ${meta.code} • Ед. изм.: ${meta.unit} • ${meta.categoryClass}",
                    "",
                    "",
                    "",
                    "Норма запаса: ___",
                    ""
                )
            )

            var balance = 0
            if (moves.isEmpty()) {
                val recs = stockByItemId[meta.itemId].orEmpty()
                val actualQty = recs.sumOf { it.quantity }
                val locs = recs.filter { it.quantity > 0 }.joinToString("; ") { r ->
                    "${pointMap[r.pointId]?.name ?: "Склад"}: ${r.quantity}"
                }.ifBlank { "Базовый склад" }
                balance = actualQty
                if (actualQty > 0) incomingPairs.add(meta.unit to actualQty)
                rows.add(
                    listOf(
                        "1",
                        today,
                        "Инвентаризация / остаток на начало",
                        "Числится по учёту подразделения",
                        if (actualQty > 0) actualQty.toString() else "",
                        "",
                        balance.toString(),
                        locs,
                        ""
                    )
                )
            } else {
                // Pre-calculate net movement so Form 18 reconciles with actual multi-warehouse stock
                // even when initial stock existed before the operation log began.
                val recs = stockByItemId[meta.itemId].orEmpty()
                val actualQty = recs.sumOf { it.quantity }
                val netMoves = moves.sumOf { (op, entry) ->
                    val isInternalIssue = op.type == OperationType.ISSUE &&
                        isInternalPointDestination(op.toPointName, points)
                    when {
                        op.type == OperationType.INCOME -> entry.quantity
                        op.type == OperationType.TRANSFER || isInternalIssue -> 0
                        else -> -entry.quantity
                    }
                }
                val openingBalance = (actualQty - netMoves).coerceAtLeast(0)
                var lineNo = 1
                if (openingBalance > 0) {
                    balance = openingBalance
                    incomingPairs.add(meta.unit to openingBalance)
                    rows.add(
                        listOf(
                            (lineNo++).toString(),
                            periodFrom,
                            "Остаток на начало периода",
                            "Входящий остаток по учёту",
                            openingBalance.toString(),
                            "",
                            balance.toString(),
                            "Все склады",
                            ""
                        )
                    )
                }

                moves.forEach { (op, entry) ->
                    val qty = entry.quantity
                    val docDate = df.format(Date(op.timestamp))
                    val docNum = op.docNumber.trim().removePrefix("№").trim()
                    val isReturn = op.isReturnOperation()
                    val docTitle = when {
                        isReturn -> "Накладная на возврат от $docDate"
                        else -> "${militaryDocumentName(op.type)} от $docDate" +
                            if (docNum.isNotBlank()) " № $docNum" else ""
                    }

                    val isInternalIssue = op.type == OperationType.ISSUE &&
                        isInternalPointDestination(op.toPointName, points)

                    val (inc, out, party, location) = when {
                        isReturn -> Quad(
                            qty,
                            0,
                            "Возврат: ${op.returnSenderName()} → ${op.toPointName.ifBlank { "Базовый склад" }}",
                            op.toPointName.ifBlank { "Базовый склад" }
                        )
                        op.type == OperationType.INCOME -> Quad(
                            qty,
                            0,
                            "Получено: ${op.fromPointName.ifBlank { "Служба снабжения" }} → ${op.toPointName.ifBlank { "Базовый склад" }}",
                            op.toPointName.ifBlank { "Базовый склад" }
                        )
                        op.type == OperationType.TRANSFER -> Quad(
                            0,
                            0,
                            "Внутр. перемещение ($qty ${entry.unit}): ${op.fromPointName} → ${op.toPointName}",
                            op.toPointName.ifBlank { op.fromPointName }
                        )
                        isInternalIssue -> Quad(
                            0,
                            0,
                            "Передано на точку ($qty ${entry.unit}): ${op.fromPointName} → ${op.toPointName}",
                            op.toPointName
                        )
                        op.type == OperationType.ISSUE -> Quad(
                            0,
                            qty,
                            "Выдано на руки: ${op.toPointName.ifBlank { "Получатель" }}",
                            op.fromPointName.ifBlank { "Базовый склад" }
                        )
                        else -> Quad(
                            0,
                            qty,
                            "Списано: ${entry.reason.ifBlank { op.comment.ifBlank { "расход по акту" } }}",
                            op.fromPointName.ifBlank { "Позиция" }
                        )
                    }

                    balance = (balance + inc - out).coerceAtLeast(0)
                    if (inc > 0) incomingPairs.add(entry.unit to inc)
                    if (out > 0) outgoingPairs.add(entry.unit to out)

                    rows.add(
                        listOf(
                            (lineNo++).toString(),
                            docDate,
                            docTitle,
                            party,
                            if (inc > 0) inc.toString() else "",
                            if (out > 0) out.toString() else "",
                            balance.toString(),
                            location,
                            ""
                        )
                    )
                }
            }
            endingPairs.add(meta.unit to balance)
        }

        if (byItem.isEmpty()) {
            rows.add(listOf("", "", "Записей о движении нет", "", "", "", "", "", ""))
        }

        val totalRow = listOf(
            "",
            "",
            "ИТОГО по книге (${byItem.size} наим.)",
            "Обороты без двойного счёта перемещений",
            formatUnitSafeTotals(incomingPairs),
            formatUnitSafeTotals(outgoingPairs),
            formatUnitSafeTotals(endingPairs),
            "",
            ""
        )

        return StructuredReport(
            sheetName = "Форма 18 (Книга учёта)",
            fileBaseName = "Форма_18_Книга_учета_${safeFileToken(unitName)}",
            docFormTitle = "Форма по ОКУД 0504042\nКнига учёта ф. 18 (ведётся бессрочно)",
            approvalLines = listOf(
                "УТВЕРЖДАЮ",
                "Командир подразделения ____________________",
                "«___» ________________ 20__ г."
            ),
            formCodeRows = listOf(
                "Форма по ОКУД" to "0504042",
                "Ведомственная форма" to "Форма № 18",
                "Дата сверки" to today
            ),
            docMainTitle = "КНИГА УЧЁТА НАЛИЧИЯ И ДВИЖЕНИЯ МАТЕРИАЛЬНЫХ ЦЕННОСТЕЙ",
            subTitle = "за период с $periodFrom по $today",
            details = listOf(
                "Воинская часть:" to "________________________",
                "Структурное подразделение:" to unitName.ifBlank { "Подразделение" },
                "Служба:" to servicesPresent.take(4).joinToString(", ").ifBlank { "Служба МТО / РАВ" },
                "Материально ответственное лицо:" to mol,
                "Разделов (наименований):" to byItem.size.toString()
            ),
            headers = headers,
            colWidthsDp = widths,
            alignments = aligns,
            rows = rows,
            sectionRowIndices = sectionIndices,
            totalRow = totalRow,
            signaturesTitle = "Сверено с данными учёта службы воинской части:",
            signers = listOf(
                Triple("Материально ответственное лицо:", "____________________", "(подпись, инициал имени, фамилия)"),
                Triple("Командир подразделения:", "____________________", "(подпись, инициал имени, фамилия)"),
                Triple("Начальник службы:", "____________________", "«__» ________ 20__ г.")
            )
        )
    }

    // =========================================================================
    // ВОЕННЫЙ ПРОФИЛЬ: АКТ СПИСАНИЯ (РАСХОДА) МАТЕРИАЛЬНЫХ ЦЕННОСТЕЙ
    // =========================================================================
    fun buildMilitaryExpenditureAct(
        operations: List<OperationRecord>,
        catalogItems: List<InventoryItem>,
        unitName: String,
        responsiblePerson: String = "",
        parseItems: (String) -> List<OperationItemEntry>
    ): StructuredReport {
        val df = dateFmt()
        val today = df.format(Date())
        val exps = operations.filter { it.type == OperationType.EXPENDITURE }.sortedBy { it.timestamp }
        val periodFrom = exps.firstOrNull()?.let { df.format(Date(it.timestamp)) } ?: today

        val headers = listOf(
            "№\nп/п",
            "Дата и\n№ акта",
            "Точка /\nподразделение",
            "Наименование\nматериальных ценностей",
            "Код /\nКВТ",
            "Ед.\nизм.",
            "Кат.",
            "Кол-во",
            "Причина /\nоснование расхода",
            "Ответственный"
        )
        val widths = listOf(34, 88, 102, 154, 62, 42, 46, 56, 130, 86)
        val aligns = listOf(
            ReportColAlign.CENTER, ReportColAlign.CENTER, ReportColAlign.START, ReportColAlign.START,
            ReportColAlign.CENTER, ReportColAlign.CENTER, ReportColAlign.CENTER, ReportColAlign.END,
            ReportColAlign.START, ReportColAlign.START
        )

        val rows = mutableListOf<List<String>>()
        val unitTotals = mutableListOf<Pair<String, Int>>()
        var n = 1
        for (op in exps) {
            val d = df.format(Date(op.timestamp))
            val docRef = if (op.docNumber.isNotBlank()) "$d\n№ ${op.docNumber.removePrefix("№").trim()}" else d
            for (entry in resolveOperationEntries(op, catalogItems, parseItems)) {
                rows.add(
                    listOf(
                        (n++).toString(),
                        docRef,
                        op.fromPointName.ifBlank { unitName },
                        entry.name,
                        entry.code,
                        entry.unit,
                        entry.categoryClass,
                        entry.quantity.toString(),
                        entry.reason.ifBlank { op.comment.ifBlank { "Списание по акту" } },
                        op.responsiblePerson.ifBlank { responsiblePerson.ifBlank { "—" } }
                    )
                )
                unitTotals.add(entry.unit to entry.quantity)
            }
        }
        if (rows.isEmpty()) {
            rows.add(List(headers.size) { if (it == 3) "Операций списания (расхода) за период нет" else "" })
        }

        val totalRow = listOf(
            "",
            "",
            "ИТОГО списано:",
            "Строк в акте: ${unitTotals.size}",
            "—",
            "—",
            "—",
            formatUnitSafeTotals(unitTotals),
            "",
            ""
        )

        return StructuredReport(
            sheetName = "Акт списания",
            fileBaseName = "Акт_списания_${safeFileToken(unitName)}",
            docFormTitle = "Акт списания (расхода) МЦ\nВоинский материальный учёт",
            approvalLines = listOf(
                "УТВЕРЖДАЮ",
                "Командир подразделения ____________________",
                "«___» ________________ 20__ г."
            ),
            formCodeRows = listOf(
                "Документ" to "Акт списания",
                "Дата" to today
            ),
            docMainTitle = "АКТ СПИСАНИЯ (РАСХОДА) МАТЕРИАЛЬНЫХ ЦЕННОСТЕЙ",
            subTitle = "за период с $periodFrom по $today",
            details = listOf(
                "Структурное подразделение:" to unitName.ifBlank { "Подразделение" },
                "Материально ответственное лицо:" to responsiblePerson.ifBlank { "________________________" },
                "Основание:" to "Фактический расход и списание по учёту подразделения"
            ),
            headers = headers,
            colWidthsDp = widths,
            alignments = aligns,
            rows = rows,
            totalRow = totalRow,
            signaturesTitle = "Комиссия подтверждает фактический расход материальных ценностей:",
            signers = listOf(
                Triple("Председатель комиссии:", "____________________", "(должность, звание, И.О. Фамилия)"),
                Triple("Члены комиссии:", "____________________", "(должность, звание, И.О. Фамилия)"),
                Triple("Материально ответственное лицо:", "____________________", responsiblePerson.ifBlank { "(подпись, И.О. Фамилия)" })
            )
        )
    }

    // =========================================================================
    // ГРАЖДАНСКИЙ ПРОФИЛЬ: НАКЛАДНАЯ М-11 (ОКУД 0315006) / ТОРГ-13 (ОКУД 0330213)
    // =========================================================================
    fun buildCivilianTransferInvoiceM11(
        operations: List<OperationRecord>,
        points: List<WarehousePoint>,
        catalogItems: List<InventoryItem>,
        unitName: String,
        responsiblePerson: String = "",
        parseItems: (String) -> List<OperationItemEntry>
    ): StructuredReport {
        val df = dateFmt()
        val today = df.format(Date())
        val transfers = operations.filter {
            it.type == OperationType.TRANSFER ||
                (it.type == OperationType.ISSUE && isInternalPointDestination(it.toPointName, points))
        }.sortedBy { it.timestamp }
        val periodFrom = transfers.firstOrNull()?.let { df.format(Date(it.timestamp)) } ?: today

        val headers = listOf(
            "№\nп/п",
            "Дата и\n№ накладной",
            "Отправитель →\nПолучатель",
            "Наименование ТМЦ",
            "Артикул /\nкод",
            "Ед.\nизм.",
            "Отправлено\n(затреб.)",
            "Получено\n(отпущено)",
            "Примечание"
        )
        val widths = listOf(34, 86, 138, 152, 66, 44, 66, 66, 110)
        val aligns = listOf(
            ReportColAlign.CENTER, ReportColAlign.CENTER, ReportColAlign.START, ReportColAlign.START,
            ReportColAlign.CENTER, ReportColAlign.CENTER, ReportColAlign.END, ReportColAlign.END,
            ReportColAlign.START
        )

        val rows = mutableListOf<List<String>>()
        val sentPairs = mutableListOf<Pair<String, Int>>()
        var n = 1
        for (op in transfers) {
            val d = df.format(Date(op.timestamp))
            val docRef = if (op.docNumber.isNotBlank()) "$d\n№ ${op.docNumber.removePrefix("№").trim()}" else "$d\nМ-11"
            val route = "${op.fromPointName.ifBlank { "Основной склад" }} → ${op.toPointName.ifBlank { "Участок" }}"
            for (entry in resolveOperationEntries(op, catalogItems, parseItems)) {
                rows.add(
                    listOf(
                        (n++).toString(),
                        docRef,
                        route,
                        entry.name,
                        entry.code,
                        entry.unit,
                        entry.quantity.toString(),
                        entry.quantity.toString(),
                        op.comment.ifBlank { entry.reason }
                    )
                )
                sentPairs.add(entry.unit to entry.quantity)
            }
        }
        if (rows.isEmpty()) {
            rows.add(List(headers.size) { if (it == 3) "Внутренних перемещений между складами за период нет" else "" })
        }

        val totalQtyStr = formatUnitSafeTotals(sentPairs)
        val totalRow = listOf(
            "",
            "",
            "ИТОГО перемещено:",
            "Позиций: ${sentPairs.size}",
            "—",
            "—",
            totalQtyStr,
            totalQtyStr,
            ""
        )

        val senders = transfers.map { it.fromPointName }.filter { it.isNotBlank() }.distinct().joinToString(", ")
        val receivers = transfers.map { it.toPointName }.filter { it.isNotBlank() }.distinct().joinToString(", ")

        return StructuredReport(
            sheetName = "Накладная М-11 (ТОРГ-13)",
            fileBaseName = "Накладная_М11_Перемещения_${safeFileToken(unitName)}",
            docFormTitle = "Типовая межотраслевая форма № М-11 (ОКУД 0315006)\nНакладная на внутреннее перемещение ТОРГ-13 (ОКУД 0330213)",
            approvalLines = listOf(
                "Унифицированная форма № М-11 / ТОРГ-13",
                "Постановление Госкомстата РФ № 71а / № 132"
            ),
            formCodeRows = listOf(
                "Форма по ОКУД" to "0315006 / 0330213",
                "Дата составления" to today,
                "по ОКПО" to "________"
            ),
            docMainTitle = "НАКЛАДНАЯ № ___ НА ВНУТРЕННЕЕ ПЕРЕМЕЩЕНИЕ ТМЦ",
            subTitle = "за период с $periodFrom по $today",
            details = listOf(
                "Организация:" to unitName.ifBlank { "Основной склад" },
                "Отправитель (склад):" to senders.ifBlank { "Основной склад" },
                "Получатель (цех / бригада):" to receivers.ifBlank { "Участок / бригада" },
                "Ответственное лицо:" to responsiblePerson.ifBlank { "________________________" }
            ),
            headers = headers,
            colWidthsDp = widths,
            alignments = aligns,
            rows = rows,
            totalRow = totalRow,
            signaturesTitle = "Материальные ценности сдал и принял в указанном количестве:",
            signers = listOf(
                Triple("Отпустил (Сдал):", "____________________", "(должность, подпись, расшифровка)"),
                Triple("Получил (Принял):", "____________________", "(должность, подпись, расшифровка)"),
                Triple("Руководитель / Мастер:", "____________________", "«__» ________ 20__ г.")
            )
        )
    }

    // =========================================================================
    // ГРАЖДАНСКИЙ ПРОФИЛЬ: ВЕДОМОСТЬ ВЫДАЧИ НА РУКИ (МБ-7, ОКУД 0320003)
    // =========================================================================
    fun buildCivilianIssueRegisterMB7(
        operations: List<OperationRecord>,
        catalogItems: List<InventoryItem>,
        unitName: String,
        responsiblePerson: String = "",
        parseItems: (String) -> List<OperationItemEntry>
    ): StructuredReport {
        val df = dateFmt()
        val today = df.format(Date())
        val issues = operations.filter { it.type == OperationType.ISSUE }.sortedBy { it.timestamp }
        val returns = operations.filter { it.isReturnOperation() }.sortedBy { it.timestamp }
        val periodFrom = issues.firstOrNull()?.let { df.format(Date(it.timestamp)) }
            ?: returns.firstOrNull()?.let { df.format(Date(it.timestamp)) }
            ?: today

        data class ReturnPoolEntry(
            val senderKey: String,
            val itemKey: String,
            val date: String,
            val unit: String,
            var remainingQty: Int
        )

        val returnPool = mutableListOf<ReturnPoolEntry>()
        val returnedPairs = mutableListOf<Pair<String, Int>>()
        for (retOp in returns) {
            val retDate = df.format(Date(retOp.timestamp))
            val senderNorm = retOp.returnSenderName().lowercase(ruLocale)
            for (entry in resolveOperationEntries(retOp, catalogItems, parseItems)) {
                if (entry.quantity <= 0) continue
                returnPool.add(
                    ReturnPoolEntry(
                        senderKey = senderNorm,
                        itemKey = entry.key,
                        date = retDate,
                        unit = entry.unit,
                        remainingQty = entry.quantity
                    )
                )
                returnedPairs.add(entry.unit to entry.quantity)
            }
        }

        val headers = listOf(
            "№\nп/п",
            "ФИО получателя /\nбригада",
            "Наименование\nинструмента / ТМЦ",
            "Артикул /\nкод",
            "Ед.\nизм.",
            "Кол-во",
            "Дата\nвыдачи",
            "Подпись в\nполучении",
            "Возврат\n(дата / подпись)"
        )
        val widths = listOf(34, 128, 152, 66, 44, 54, 74, 82, 118)
        val aligns = listOf(
            ReportColAlign.CENTER, ReportColAlign.START, ReportColAlign.START, ReportColAlign.CENTER,
            ReportColAlign.CENTER, ReportColAlign.END, ReportColAlign.CENTER, ReportColAlign.CENTER,
            ReportColAlign.CENTER
        )

        val rows = mutableListOf<List<String>>()
        val qtyPairs = mutableListOf<Pair<String, Int>>()
        var n = 1
        for (op in issues) {
            val d = df.format(Date(op.timestamp))
            val recipient = op.toPointName.ifBlank { "Получатель" }
            val recipientNorm = recipient.trim().lowercase(ruLocale)
            for (entry in resolveOperationEntries(op, catalogItems, parseItems)) {
                var needToMatch = entry.quantity
                var matchedQty = 0
                var lastReturnDate = ""
                for (pool in returnPool) {
                    if (needToMatch <= 0) break
                    if (pool.remainingQty <= 0) continue
                    val sameItem = pool.itemKey == entry.key
                    val sameSender = pool.senderKey == recipientNorm ||
                        pool.senderKey.contains(recipientNorm) ||
                        recipientNorm.contains(pool.senderKey)
                    if (sameItem && sameSender) {
                        val take = minOf(needToMatch, pool.remainingQty)
                        pool.remainingQty -= take
                        needToMatch -= take
                        matchedQty += take
                        lastReturnDate = pool.date
                    }
                }
                val returnCell = when {
                    matchedQty <= 0 -> ""
                    matchedQty >= entry.quantity -> "$lastReturnDate (возврат $matchedQty ${entry.unit})"
                    else -> "$lastReturnDate ($matchedQty из ${entry.quantity} ${entry.unit})"
                }
                rows.add(
                    listOf(
                        (n++).toString(),
                        recipient,
                        entry.name,
                        entry.code,
                        entry.unit,
                        entry.quantity.toString(),
                        d,
                        "",
                        returnCell
                    )
                )
                qtyPairs.add(entry.unit to entry.quantity)
            }
        }
        if (rows.isEmpty()) {
            rows.add(List(headers.size) { if (it == 2) "Выдач инструмента и ТМЦ на руки за период нет" else "" })
        }

        val returnedSummary = if (returnedPairs.isEmpty()) "" else "Возврат: ${formatUnitSafeTotals(returnedPairs)}"
        val totalRow = listOf(
            "",
            "ИТОГО выдано:",
            "Записей: ${qtyPairs.size}",
            "—",
            "—",
            formatUnitSafeTotals(qtyPairs),
            "",
            "",
            returnedSummary
        )

        return StructuredReport(
            sheetName = "Ведомость выдачи МБ-7",
            fileBaseName = "Ведомость_выдачи_МБ7_${safeFileToken(unitName)}",
            docFormTitle = "Форма № МБ-7 по ОКУД 0320003\nВедомость учёта выдачи в пользование и возврата",
            approvalLines = listOf(
                "Типовая межотраслевая форма № МБ-7",
                "Утверждена Постановлением Госкомстата РФ № 71а"
            ),
            formCodeRows = listOf(
                "Форма по ОКУД" to "0320003",
                "Дата" to today,
                "по ОКПО" to "________"
            ),
            docMainTitle = "ВЕДОМОСТЬ ВЫДАЧИ НА РУКИ И ВОЗВРАТА ТМЦ / ИНСТРУМЕНТА",
            subTitle = "за период с $periodFrom по $today",
            details = listOf(
                "Организация / склад:" to unitName.ifBlank { "Основной склад" },
                "Кладовщик (МОЛ):" to responsiblePerson.ifBlank { "________________________" },
                "Всего выдач / возвратов:" to "${qtyPairs.size} / ${returnedPairs.size}"
            ),
            headers = headers,
            colWidthsDp = widths,
            alignments = aligns,
            rows = rows,
            totalRow = totalRow,
            signaturesTitle = "Выдачу произвёл и проверил:",
            signers = listOf(
                Triple("Выдал (кладовщик / МОЛ):", "____________________", responsiblePerson.ifBlank { "(подпись, расшифровка)" }),
                Triple("Руководитель участка / мастер:", "____________________", "«__» ________ 20__ г.")
            )
        )
    }

    // =========================================================================
    // ГРАЖДАНСКИЙ ПРОФИЛЬ: АКТ СПИСАНИЯ ТМЦ (МБ-8 / ТОРГ-16)
    // =========================================================================
    fun buildCivilianWriteOffActMB8(
        operations: List<OperationRecord>,
        catalogItems: List<InventoryItem>,
        unitName: String,
        responsiblePerson: String = "",
        parseItems: (String) -> List<OperationItemEntry>
    ): StructuredReport {
        val df = dateFmt()
        val today = df.format(Date())
        val exps = operations.filter { it.type == OperationType.EXPENDITURE }.sortedBy { it.timestamp }
        val periodFrom = exps.firstOrNull()?.let { df.format(Date(it.timestamp)) } ?: today

        val headers = listOf(
            "№\nп/п",
            "Дата и\n№ акта",
            "Склад /\nучасток",
            "Наименование ТМЦ",
            "Артикул /\nкод",
            "Ед.\nизм.",
            "Кол-во",
            "Причина списания"
        )
        val widths = listOf(34, 88, 108, 158, 66, 44, 56, 154)
        val aligns = listOf(
            ReportColAlign.CENTER, ReportColAlign.CENTER, ReportColAlign.START, ReportColAlign.START,
            ReportColAlign.CENTER, ReportColAlign.CENTER, ReportColAlign.END, ReportColAlign.START
        )

        val rows = mutableListOf<List<String>>()
        val qtyPairs = mutableListOf<Pair<String, Int>>()
        var n = 1
        for (op in exps) {
            val d = df.format(Date(op.timestamp))
            val docRef = if (op.docNumber.isNotBlank()) "$d\n№ ${op.docNumber.removePrefix("№").trim()}" else "$d\nМБ-8"
            for (entry in resolveOperationEntries(op, catalogItems, parseItems)) {
                rows.add(
                    listOf(
                        (n++).toString(),
                        docRef,
                        op.fromPointName.ifBlank { unitName },
                        entry.name,
                        entry.code,
                        entry.unit,
                        entry.quantity.toString(),
                        entry.reason.ifBlank { op.comment.ifBlank { "Износ / расход на производство" } }
                    )
                )
                qtyPairs.add(entry.unit to entry.quantity)
            }
        }
        if (rows.isEmpty()) {
            rows.add(List(headers.size) { if (it == 3) "Операций списания ТМЦ за период нет" else "" })
        }

        val totalRow = listOf(
            "",
            "",
            "ИТОГО списано:",
            "Позиций: ${qtyPairs.size}",
            "—",
            "—",
            formatUnitSafeTotals(qtyPairs),
            ""
        )

        return StructuredReport(
            sheetName = "Акт списания МБ-8",
            fileBaseName = "Акт_списания_МБ8_${safeFileToken(unitName)}",
            docFormTitle = "Акт на списание ТМЦ / инвентаря\nпо образцу формы № МБ-8 (ОКУД 0320004) / ТОРГ-16",
            approvalLines = listOf(
                "УТВЕРЖДАЮ",
                "Руководитель организации ____________________",
                "«___» ________________ 20__ г."
            ),
            formCodeRows = listOf(
                "Форма по ОКУД" to "0320004 / 0330216",
                "Дата" to today
            ),
            docMainTitle = "АКТ НА СПИСАНИЕ ТОВАРНО-МАТЕРИАЛЬНЫХ ЦЕННОСТЕЙ № ___",
            subTitle = "за период с $periodFrom по $today",
            details = listOf(
                "Организация / склад:" to unitName.ifBlank { "Основной склад" },
                "Материально ответственное лицо:" to responsiblePerson.ifBlank { "________________________" },
                "Основание:" to "Износ, поломка, производственный расход"
            ),
            headers = headers,
            colWidthsDp = widths,
            alignments = aligns,
            rows = rows,
            totalRow = totalRow,
            signaturesTitle = "Комиссия провела осмотр и подтверждает списание:",
            signers = listOf(
                Triple("Председатель комиссии:", "____________________", "(должность, подпись, расшифровка)"),
                Triple("Члены комиссии:", "____________________", "(должность, подпись, расшифровка)"),
                Triple("Материально ответственное лицо:", "____________________", responsiblePerson.ifBlank { "(подпись, расшифровка)" })
            )
        )
    }

    // =========================================================================
    // СВОДНАЯ / ОБОРОТНАЯ ВЕДОМОСТЬ (ДЛЯ ОБОИХ ПРОФИЛЕЙ, БЕЗ ДВОЙНОГО СЧЁТА)
    // =========================================================================
    fun buildConsolidatedTurnoverReport(
        profileMode: WorkspaceProfileMode,
        catalogItems: List<InventoryItem>,
        stockRecords: List<StockRecord>,
        operations: List<OperationRecord>,
        points: List<WarehousePoint>,
        unitName: String,
        responsiblePerson: String = "",
        parseItems: (String) -> List<OperationItemEntry>
    ): StructuredReport {
        val isMil = profileMode.isMilitary
        val df = dateFmt()
        val today = df.format(Date())

        val catalogMap = catalogItems.associateBy { it.id }
        val pointMap = points.associateBy { it.id }

        // Точный внешний приход, возврат и внешний расход из журнала (без завышения перемещениями и возвратами)
        val externalInByItem = mutableMapOf<String, Int>()
        val externalOutByItem = mutableMapOf<String, Int>()
        val returnsByItem = mutableMapOf<String, Int>()
        val opMetaByItem = mutableMapOf<String, ResolvedReportItem>()

        for (op in operations) {
            val isInternalIssue = op.type == OperationType.ISSUE && isInternalPointDestination(op.toPointName, points)
            val isReturn = op.isReturnOperation()
            for (entry in resolveOperationEntries(op, catalogItems, parseItems)) {
                val idKey = entry.itemId.ifBlank { entry.key }
                opMetaByItem.putIfAbsent(idKey, entry)
                when {
                    isReturn ->
                        returnsByItem[idKey] = (returnsByItem[idKey] ?: 0) + entry.quantity
                    op.type == OperationType.INCOME ->
                        externalInByItem[idKey] = (externalInByItem[idKey] ?: 0) + entry.quantity
                    op.type == OperationType.EXPENDITURE || (op.type == OperationType.ISSUE && !isInternalIssue) ->
                        externalOutByItem[idKey] = (externalOutByItem[idKey] ?: 0) + entry.quantity
                    else -> Unit // Внутреннее перемещение не меняет общий итог организации/подразделения
                }
            }
        }

        val stockByItem = stockRecords.groupBy { it.itemId }
        val allItemKeys = (stockByItem.keys + opMetaByItem.keys).distinct().sortedWith(
            compareBy<String>(
                { (catalogMap[it]?.serviceCategory ?: opMetaByItem[it]?.category ?: "Прочее").lowercase(ruLocale) },
                { (catalogMap[it]?.name ?: opMetaByItem[it]?.name ?: it).lowercase(ruLocale) }
            )
        )

        val headers = listOf(
            "№\nп/п",
            if (isMil) "Наименование имущества\n(номенклатура)" else "Наименование ТМЦ /\nинструмента",
            if (isMil) "Служба /\nКатегория" else "Категория",
            "Ед.\nизм.",
            "Приход\n(внешний)",
            "Расход /\nвыдача",
            if (isMil) "Остатки по складам\nи точкам" else "Наличие по складам\nи участкам",
            "Итоговый\nостаток"
        )
        val widths = listOf(34, 162, 92, 42, 60, 60, 184, 68)
        val aligns = listOf(
            ReportColAlign.CENTER, ReportColAlign.START, ReportColAlign.START, ReportColAlign.CENTER,
            ReportColAlign.END, ReportColAlign.END, ReportColAlign.START, ReportColAlign.END
        )

        val rows = mutableListOf<List<String>>()
        val totalInPairs = mutableListOf<Pair<String, Int>>()
        val totalOutPairs = mutableListOf<Pair<String, Int>>()
        val totalRemPairs = mutableListOf<Pair<String, Int>>()
        var idx = 1

        for (itemKey in allItemKeys) {
            val recs = stockByItem[itemKey].orEmpty()
            val cat = catalogMap[itemKey]
            val opMeta = opMetaByItem[itemKey]

            val name = cat?.name ?: opMeta?.name ?: itemKey
            val category = cat?.serviceCategory ?: opMeta?.category ?: "Прочее"
            val unit = cat?.unit ?: opMeta?.unit ?: "шт."

            val hasExternalOps = externalInByItem.containsKey(itemKey) ||
                externalOutByItem.containsKey(itemKey) ||
                returnsByItem.containsKey(itemKey)
            val returned = returnsByItem[itemKey] ?: 0
            val inc = if (hasExternalOps) externalInByItem[itemKey] ?: 0 else recs.sumOf { it.incomeTotal }
            val exp = if (hasExternalOps) {
                ((externalOutByItem[itemKey] ?: 0) - returned).coerceAtLeast(0)
            } else {
                recs.sumOf { it.expenseTotal }
            }
            val rem = recs.sumOf { it.quantity }

            if (inc == 0 && exp == 0 && rem == 0) continue

            val breakdown = recs.filter { it.quantity > 0 }.joinToString("; ") { r ->
                val pName = pointMap[r.pointId]?.name ?: "Склад"
                "$pName: ${r.quantity}"
            }.ifEmpty { "Нет в наличии" }

            rows.add(
                listOf(
                    (idx++).toString(),
                    name,
                    category,
                    unit,
                    inc.toString(),
                    exp.toString(),
                    breakdown,
                    rem.toString()
                )
            )
            totalInPairs.add(unit to inc)
            totalOutPairs.add(unit to exp)
            totalRemPairs.add(unit to rem)
        }

        if (rows.isEmpty()) {
            rows.add(List(headers.size) { if (it == 1) "На складах пока нет учтённых позиций" else "" })
        }

        val totalRow = listOf(
            "ИТОГО",
            "Позиций в ведомости: ${totalRemPairs.size}",
            "—",
            "—",
            formatUnitSafeTotals(totalInPairs),
            formatUnitSafeTotals(totalOutPairs),
            "Складов: ${points.size}",
            formatUnitSafeTotals(totalRemPairs)
        )

        val cats = allItemKeys.mapNotNull { catalogMap[it]?.serviceCategory ?: opMetaByItem[it]?.category }.distinct()

        return StructuredReport(
            sheetName = if (isMil) "Сводная МТО" else "Оборотная ТМЦ",
            fileBaseName = if (isMil) "Сводная_ведомость_${safeFileToken(unitName)}" else "Оборотная_ведомость_ТМЦ_${safeFileToken(unitName)}",
            docFormTitle = if (isMil) "Служебный документ МТО\nВоинский материальный учёт" else "Складской учёт ТМЦ\nОборотная ведомость остатков",
            approvalLines = if (isMil) {
                listOf("УТВЕРЖДАЮ", "Командир подразделения ____________________", "«___» ________________ 20__ г.")
            } else {
                listOf("УТВЕРЖДАЮ", "Руководитель ____________________", "«___» ________________ 20__ г.")
            },
            formCodeRows = listOf(
                "Документ" to if (isMil) "Сводная МТО" else "Оборотная ТМЦ",
                "Дата" to today
            ),
            docMainTitle = if (isMil) "СВОДНАЯ ОБОРОТНАЯ ВЕДОМОСТЬ НАЛИЧИЯ ИМУЩЕСТВА" else "ОБОРОТНАЯ ВЕДОМОСТЬ ОСТАТКОВ И ДВИЖЕНИЯ ТМЦ",
            subTitle = "По состоянию на $today г.",
            details = listOf(
                (if (isMil) "Подразделение:" else "Организация / склад:") to unitName.ifBlank { "Основной склад" },
                (if (isMil) "Количество точек учёта:" else "Складов и участков:") to points.size.toString(),
                (if (isMil) "Службы:" else "Категории:") to cats.take(5).joinToString(", ").ifBlank { "Все категории" }
            ),
            headers = headers,
            colWidthsDp = widths,
            alignments = aligns,
            rows = rows,
            totalRow = totalRow,
            signaturesTitle = "Ведомость составил и сверил:",
            signers = if (isMil) {
                listOf(
                    Triple("Материально ответственное лицо:", "____________________", responsiblePerson.ifBlank { "Старшина" }),
                    Triple("Сверил командир подразделения:", "____________________", unitName)
                )
            } else {
                listOf(
                    Triple("Кладовщик (МОЛ):", "____________________", responsiblePerson.ifBlank { "(подпись, расшифровка)" }),
                    Triple("Руководитель / Мастер:", "____________________", unitName)
                )
            }
        )
    }

    // =========================================================================
    // ВЕДОМОСТЬ ПО КОНКРЕТНОМУ СКЛАДУ / ТОЧКЕ (Колонка G = Фактический остаток!)
    // =========================================================================
    fun buildSinglePointReport(
        profileMode: WorkspaceProfileMode,
        point: WarehousePoint,
        catalogItems: List<InventoryItem>,
        stockRecords: List<StockRecord>,
        operations: List<OperationRecord>,
        unitName: String,
        responsiblePerson: String = "",
        parseItems: (String) -> List<OperationItemEntry>
    ): StructuredReport {
        val isMil = profileMode.isMilitary
        val df = dateFmt()
        val today = df.format(Date())

        val catalogMap = catalogItems.associateBy { it.id }
        val opMetaMap = mutableMapOf<String, ResolvedReportItem>()
        for (op in operations) {
            for (entry in resolveOperationEntries(op, catalogItems, parseItems)) {
                if (entry.itemId.isNotBlank()) opMetaMap.putIfAbsent(entry.itemId, entry)
            }
        }

        // Важно: колонка G (индекс 6) — числовой «Фактический остаток» (проверяется тестами и экспортом).
        val headers = listOf(
            "№\nп/п",
            if (isMil) "Наименование имущества" else "Наименование ТМЦ / инструмента",
            if (isMil) "Служба /\nКатегория" else "Категория",
            "Ед.\nизм.",
            if (isMil) "Поступило\nна точку" else "Поступило\nна склад",
            "Выдано /\nСписано",
            "Фактический\nостаток",
            "Отметка о\nсверке"
        )
        val widths = listOf(34, 162, 88, 42, 65, 65, 68, 66)
        val aligns = listOf(
            ReportColAlign.CENTER, ReportColAlign.START, ReportColAlign.START, ReportColAlign.CENTER,
            ReportColAlign.END, ReportColAlign.END, ReportColAlign.END, ReportColAlign.CENTER
        )

        val rows = mutableListOf<List<String>>()
        val inPairs = mutableListOf<Pair<String, Int>>()
        val outPairs = mutableListOf<Pair<String, Int>>()
        val remPairs = mutableListOf<Pair<String, Int>>()
        var idx = 1

        val sortedRecords = stockRecords.sortedWith(
            compareBy<StockRecord>(
                { (catalogMap[it.itemId]?.serviceCategory ?: opMetaMap[it.itemId]?.category ?: "Прочее").lowercase(ruLocale) },
                { (catalogMap[it.itemId]?.name ?: opMetaMap[it.itemId]?.name ?: it.itemId).lowercase(ruLocale) }
            )
        )

        for (r in sortedRecords) {
            if (r.quantity == 0 && r.incomeTotal == 0 && r.expenseTotal == 0) continue
            val cat = catalogMap[r.itemId]
            val opMeta = opMetaMap[r.itemId]
            val name = cat?.name ?: opMeta?.name ?: r.itemId
            val category = cat?.serviceCategory ?: opMeta?.category ?: "Прочее"
            val unit = cat?.unit ?: opMeta?.unit ?: "шт."

            rows.add(
                listOf(
                    (idx++).toString(),
                    name,
                    category,
                    unit,
                    r.incomeTotal.toString(),
                    r.expenseTotal.toString(),
                    r.quantity.toString(),
                    if (r.quantity > 0) "В наличии" else "Израсход."
                )
            )
            inPairs.add(unit to r.incomeTotal)
            outPairs.add(unit to r.expenseTotal)
            remPairs.add(unit to r.quantity)
        }

        if (rows.isEmpty()) {
            rows.add(List(headers.size) { if (it == 1) "На складе «${point.name}» остатков пока нет" else "" })
        }

        val totalRow = listOf(
            "ИТОГО",
            "Позиций на «${point.name}»: ${remPairs.size}",
            "—",
            "—",
            formatUnitSafeTotals(inPairs),
            formatUnitSafeTotals(outPairs),
            formatUnitSafeTotals(remPairs),
            "—"
        )

        return StructuredReport(
            sheetName = point.name.take(28),
            fileBaseName = "Ведомость_${safeFileToken(point.name)}",
            docFormTitle = if (isMil) "Учётная ведомость\nТочка хранения" else "Инвентаризационная ведомость\nСклад / участок",
            approvalLines = listOf(
                "УТВЕРЖДАЮ",
                (if (isMil) "Командир подразделения" else "Руководитель") + " ____________________",
                "«___» ________________ 20__ г."
            ),
            formCodeRows = listOf(
                "Склад" to point.name.take(18),
                "Дата" to today
            ),
            docMainTitle = if (isMil) "ВЕДОМОСТЬ НАЛИЧИЯ ИМУЩЕСТВА НА СКЛАДЕ / ТОЧКЕ" else "ВЕДОМОСТЬ ОСТАТКОВ НА СКЛАДЕ",
            subTitle = "«${point.name}»${if (point.isBase) " (Основной склад)" else ""} на $today г.",
            details = listOf(
                (if (isMil) "Подразделение:" else "Организация:") to unitName.ifBlank { "Основной склад" },
                "Склад / место хранения:" to point.name,
                "Описание:" to point.description.ifBlank { if (isMil) "Склад / точка учёта" else "Место хранения ТМЦ" }
            ),
            headers = headers,
            colWidthsDp = widths,
            alignments = aligns,
            rows = rows,
            totalRow = totalRow,
            signaturesTitle = "Имущество на ответственное хранение принял:",
            signers = listOf(
                Triple("Ответственное лицо (${point.name}):", "____________________", responsiblePerson.ifBlank { "(подпись, ФИО)" }),
                Triple(if (isMil) "Начальник службы / старшина:" else "Руководитель / мастер:", "____________________", "«__» ________ 20__ г.")
            )
        )
    }

    // =========================================================================
    // РЕЕСТР ЗАЯВОК
    // =========================================================================
    fun buildRequisitionsReport(
        profileMode: WorkspaceProfileMode,
        requisitions: List<RequisitionRequest>,
        unitName: String
    ): StructuredReport {
        val isMil = profileMode.isMilitary
        val df = dateTimeFmt()
        val today = df.format(Date())

        val headers = listOf(
            "№\nп/п",
            "Дата и время\nподачи",
            if (isMil) "Заявитель /\nпозывной" else "Заявитель /\nбригада",
            if (isMil) "Точка /\nсклад" else "Склад /\nучасток",
            "Статус",
            "Запрашиваемые позиции\nи количество",
            "Примечание"
        )
        val widths = listOf(34, 88, 98, 98, 82, 158, 114)
        val aligns = listOf(
            ReportColAlign.CENTER, ReportColAlign.CENTER, ReportColAlign.START, ReportColAlign.START,
            ReportColAlign.CENTER, ReportColAlign.START, ReportColAlign.START
        )

        val rows = requisitions.sortedByDescending { it.timestamp }.mapIndexed { idx, req ->
            listOf(
                (idx + 1).toString(),
                df.format(Date(req.timestamp)),
                req.applicantName,
                req.pointName,
                req.status.titleRu,
                req.itemsSummary,
                req.comment.ifEmpty { "Плановая заявка" }
            )
        }.ifEmpty {
            listOf(List(headers.size) { if (it == 5) "Зарегистрированных заявок нет" else "" })
        }

        return StructuredReport(
            sheetName = "Реестр заявок",
            fileBaseName = "Реестр_заявок_${safeFileToken(unitName)}",
            docFormTitle = if (isMil) "Журнал заявок МТО" else "Журнал заявок на выдачу",
            approvalLines = emptyList(),
            formCodeRows = listOf("Заявок" to requisitions.size.toString()),
            docMainTitle = if (isMil) "РЕЕСТР ЗАЯВОК НА МАТЕРИАЛЬНЫЕ СРЕДСТВА" else "РЕЕСТР ЗАЯВОК НА ВЫДАЧУ ТМЦ И ИНСТРУМЕНТА",
            subTitle = "По состоянию на $today",
            details = listOf(
                (if (isMil) "Подразделение:" else "Организация:") to unitName.ifBlank { "Основной склад" },
                "Всего заявок:" to requisitions.size.toString()
            ),
            headers = headers,
            colWidthsDp = widths,
            alignments = aligns,
            rows = rows,
            totalRow = null,
            signaturesTitle = "Реестр заявок сверил:",
            signers = listOf(
                Triple(if (isMil) "Ответственный по снабжению:" else "Кладовщик:", "____________________", "(подпись, расшифровка)"),
                Triple(if (isMil) "Командир подразделения:" else "Руководитель:", "____________________", unitName)
            )
        )
    }

    private fun militaryDocumentName(type: OperationType): String = when (type) {
        OperationType.INCOME -> "Накладная (приход)"
        OperationType.ISSUE -> "Раздаточная ведомость ф. 8"
        OperationType.EXPENDITURE -> "Акт списания"
        OperationType.TRANSFER -> "Накладная (перемещение)"
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
