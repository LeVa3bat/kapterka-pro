package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.InitialData
import com.example.data.local.KapterkaDatabase
import com.example.data.model.InventoryItem
import com.example.data.model.OperationItemEntry
import com.example.data.model.OperationRecord
import com.example.data.model.OperationType
import com.example.data.model.StockRecord
import com.example.data.model.UserProfile
import com.example.data.model.WarehousePoint
import com.example.data.model.WorkspaceProfileMode
import com.example.data.repository.KapterkaRepository
import com.example.util.ExcelExportHelper
import com.example.util.ReportBuilders
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ReportBuildersAndProfilesTest {

    private val parseOps: (String) -> List<OperationItemEntry> = { json ->
        if (json.isBlank()) emptyList()
        else try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                OperationItemEntry(
                    itemId = o.optString("itemId"),
                    itemName = o.optString("itemName"),
                    unit = o.optString("unit", "шт."),
                    quantity = o.optInt("quantity", 0),
                    categoryClass = o.optString("categoryClass", "Кат. 1"),
                    reason = o.optString("reason", "")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // -------------------------------------------------------------------------
    // 1. Starter Catalogs & Profile Modes (Path A)
    // -------------------------------------------------------------------------

    @Test
    fun starterCatalogs_haveUniqueIdsAndAccurateStructure() {
        val militaryItems = InitialData.getDefaultItems()
        val civilianItems = InitialData.getCivilianItems()
        val emptyItems = InitialData.getItemsFor(WorkspaceProfileMode.EMPTY)

        assertEquals(
            "Military catalog must not have duplicate item IDs",
            militaryItems.size,
            militaryItems.map { it.id }.distinct().size
        )
        assertEquals(
            "Civilian catalog must not have duplicate item IDs",
            civilianItems.size,
            civilianItems.map { it.id }.distinct().size
        )
        assertTrue("Civilian starter catalog must have rich assortment (>= 60 items)", civilianItems.size >= 60)
        assertEquals(9, InitialData.getCivilianCategories().size)
        assertTrue("Every civilian item must have non-blank unit & category", civilianItems.all {
            it.name.isNotBlank() && it.unit.isNotBlank() && it.serviceCategory in InitialData.getCivilianCategories()
        })
        assertTrue("Empty warehouse profile must have ZERO forced items", emptyItems.isEmpty())
        assertTrue("Empty warehouse profile must have ZERO forced categories", InitialData.getCategoriesFor(WorkspaceProfileMode.EMPTY).isEmpty())

        val emptyPoints = InitialData.getPointsFor(WorkspaceProfileMode.EMPTY)
        assertEquals(1, emptyPoints.size)
        assertEquals("Основной склад", emptyPoints.single().name)
        assertTrue(emptyPoints.single().isBase)
    }

    @Test
    fun repository_emptyProfileNeverForcesMilitaryItemsAndProtectsExistingData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, KapterkaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.kapterkaDao()
            val repo = KapterkaRepository(dao, null)

            // Brand new install choosing EMPTY profile
            repo.ensureInitialized(WorkspaceProfileMode.EMPTY)
            assertEquals(1, dao.getAllPoints().first().size)
            assertEquals("Основной склад", dao.getAllPoints().first().single().name)
            assertEquals(0, dao.getAllItems().first().size)
            assertFalse("Clean EMPTY install has no user operations/stocks yet", repo.hasLocalUnitData())

            // Switching starter profile BEFORE entering data succeeds (CIVILIAN)
            val applied = repo.applyStarterProfile(WorkspaceProfileMode.CIVILIAN)
            assertTrue("Applying starter profile on clean DB must succeed", applied)
            val civItems = dao.getAllItems().first()
            val civPoints = dao.getAllPoints().first()
            assertEquals(InitialData.getCivilianItems().size, civItems.size)
            assertEquals(InitialData.getCivilianPoints().size, civPoints.size)

            // User records a real income operation
            val firstItem = civItems.first()
            val basePoint = civPoints.first { it.isBase }
            repo.recordIncome(
                toPointId = basePoint.id,
                toPointName = basePoint.name,
                supplier = "Поставщик ООО Строй",
                items = listOf(OperationItemEntry(firstItem.id, firstItem.name, firstItem.unit, 15)),
                comment = "Первая поставка",
                actor = "Алексей"
            )
            assertTrue("DB with operations/stocks must be detected as existing user data", repo.hasLocalUnitData())

            // Trying to apply another starter profile MUST NOT wipe existing data!
            val blocked = repo.applyStarterProfile(WorkspaceProfileMode.EMPTY)
            assertFalse("Starter profile replacement must be blocked once user data exists", blocked)
            assertEquals(InitialData.getCivilianItems().size, dao.getAllItems().first().size)
            assertEquals(15, dao.getAllStockRecords().first().single().quantity)

            // Non-destructive template import adds military items WITHOUT removing existing civilian items or stocks
            val addedCount = repo.seedTemplateCatalog(WorkspaceProfileMode.MILITARY)
            assertTrue("Non-destructive import must add new catalog items", addedCount > 0)
            assertEquals(15, dao.getAllStockRecords().first().single().quantity)
            assertTrue(dao.getAllItems().first().any { it.id == firstItem.id })
        } finally {
            db.close()
        }
    }

    // -------------------------------------------------------------------------
    // 2. Exact Military Statutory Reports (Form 8 OKUD 6002203 & Form 18 OKUD 0504042)
    // -------------------------------------------------------------------------

    private val milProfile = UserProfile(
        callsign = "ст. прапорщик Петров И.В.",
        unitName = "в/ч 12345 (2 мср)",
        isLoggedIn = true
    )

    private val milPoints = listOf(
        WarehousePoint("base", "Базовый склад (КЗ)", "КЗ роты", true, 0),
        WarehousePoint("p1", "1 взвод", "Точка 1", false, 1)
    )

    private val milItems = listOf(
        InventoryItem("i_ak", "Патрон 5,45х39 ПС", "Служба РАВ", "Боеприпасы", "шт.", "Кат. 1", "КВТ-01"),
        InventoryItem("i_dt", "Дизельное топливо ДТ", "Служба ГСМ", "Топливо", "л.", "Кат. 1", "ГСМ-01"),
        InventoryItem("i_kit", "Аптечка АИ-4", "Медицинская служба", "Аптечки", "шт.", "Кат. 1", "МЕД-01")
    )

    @Test
    fun militaryForm8_exactOkud6002203ColumnsTotalsAndLegacyFallback() {
        val op1 = OperationRecord(
            id = "op1",
            type = OperationType.ISSUE,
            fromPointName = "Базовый склад (КЗ)",
            toPointName = "Иванов А.С.",
            docNumber = "14",
            responsiblePerson = milProfile.callsign,
            comment = "На смену",
            timestamp = 1_730_000_000_000L,
            itemsSummary = "Патрон 5,45х39 ПС — 120 шт.",
            itemsJson = """[{"itemId":"i_ak","itemName":"Патрон 5,45х39 ПС","unit":"шт.","quantity":120}]"""
        )
        // Legacy record with blank itemsJson -> must parse itemsSummary cleanly!
        val op2 = OperationRecord(
            id = "op2",
            type = OperationType.ISSUE,
            fromPointName = "Базовый склад (КЗ)",
            toPointName = "Сидоров В.П.",
            docNumber = "15",
            responsiblePerson = milProfile.callsign,
            comment = "",
            timestamp = 1_730_003_600_000L,
            itemsSummary = "Патрон 5,45х39 ПС — 60 шт., Дизельное топливо ДТ — 40 л.",
            itemsJson = ""
        )

        val form8 = ReportBuilders.buildMilitaryForm8(
            operations = listOf(op1, op2),
            catalogItems = milItems,
            unitName = milProfile.unitName,
            responsiblePerson = milProfile.callsign,
            parseItems = parseOps
        )

        assertTrue(form8.docMainTitle.contains("РАЗДАТОЧНАЯ"))
        assertTrue(form8.formCodeRows.any { it.second == "6002203" })
        assertTrue(form8.approvalLines.first().contains("УТВЕРЖДАЮ"))

        // Headers: № п/п, Получатель, Код, [item1], [item2], Дата, Расписка
        assertEquals(7, form8.headers.size)
        assertEquals(2, form8.rows.size)

        // Totals row sums: 120 + 60 = 180 and 40
        assertNotNull(form8.totalRow)
        val totals = form8.totalRow!!
        assertTrue(totals[1].contains("Итого", ignoreCase = true))
        val akColIdx = form8.headers.indexOfFirst { it.contains("Патрон") }
        val dtColIdx = form8.headers.indexOfFirst { it.contains("Дизельное") }
        assertEquals("180", totals[akColIdx])
        assertEquals("40", totals[dtColIdx])

        // Statutory price & sum rows for accounting
        assertEquals(2, form8.extraFooterRows.size)
        assertTrue(form8.extraFooterRows[0][1].contains("Цена за единицу"))
        assertTrue(form8.extraFooterRows[1][1].contains("Сумма"))
        assertTrue(form8.signers.any { it.first.contains("Правильность выдачи") })
    }

    @Test
    fun militaryForm18_noDoubleSubtractionOnInternalTransfersAndIncludesStockOnlyItems() {
        val t0 = 1_730_000_000_000L
        val ops = listOf(
            // Income +100 to base
            OperationRecord(
                id = "m1", type = OperationType.INCOME, fromPointName = "Склад дивизии", toPointName = "Базовый склад (КЗ)",
                docNumber = "Н-101", responsiblePerson = milProfile.callsign, timestamp = t0,
                itemsSummary = "Патрон 5,45х39 ПС — 100 шт.",
                itemsJson = """[{"itemId":"i_ak","itemName":"Патрон 5,45х39 ПС","unit":"шт.","quantity":100}]"""
            ),
            // Internal TRANSFER 30 from base -> 1 взвод (total unit balance MUST STAY 100!)
            OperationRecord(
                id = "m2", type = OperationType.TRANSFER, fromPointName = "Базовый склад (КЗ)", toPointName = "1 взвод",
                docNumber = "П-12", responsiblePerson = milProfile.callsign, timestamp = t0 + 1000L,
                itemsSummary = "Патрон 5,45х39 ПС — 30 шт.",
                itemsJson = """[{"itemId":"i_ak","itemName":"Патрон 5,45х39 ПС","unit":"шт.","quantity":30}]"""
            ),
            // Internal ISSUE 20 from base -> 1 взвод (warehouse point! total unit balance MUST STAY 100!)
            OperationRecord(
                id = "m3", type = OperationType.ISSUE, fromPointName = "Базовый склад (КЗ)", toPointName = "1 взвод",
                docNumber = "В-13", responsiblePerson = milProfile.callsign, timestamp = t0 + 2000L,
                itemsSummary = "Патрон 5,45х39 ПС — 20 шт.",
                itemsJson = """[{"itemId":"i_ak","itemName":"Патрон 5,45х39 ПС","unit":"шт.","quantity":20}]"""
            ),
            // External ISSUE 15 to person -> balance becomes 85!
            OperationRecord(
                id = "m4", type = OperationType.ISSUE, fromPointName = "Базовый склад (КЗ)", toPointName = "Иванов А.С.",
                docNumber = "В-14", responsiblePerson = milProfile.callsign, timestamp = t0 + 3000L,
                itemsSummary = "Патрон 5,45х39 ПС — 15 шт.",
                itemsJson = """[{"itemId":"i_ak","itemName":"Патрон 5,45х39 ПС","unit":"шт.","quantity":15}]"""
            ),
            // EXPENDITURE 10 -> balance becomes 75!
            OperationRecord(
                id = "m5", type = OperationType.EXPENDITURE, fromPointName = "1 взвод", toPointName = "Списание",
                docNumber = "А-05", responsiblePerson = milProfile.callsign, timestamp = t0 + 4000L,
                itemsSummary = "Патрон 5,45х39 ПС — 10 шт.",
                itemsJson = """[{"itemId":"i_ak","itemName":"Патрон 5,45х39 ПС","unit":"шт.","quantity":10}]"""
            )
        )
        // Actual stocks: 35 on base + 40 on p1 = 75 for i_ak; plus 12 for i_kit (stock-only item with zero ops!)
        val stocks = listOf(
            StockRecord(pointId = "base", itemId = "i_ak", quantity = 35, incomeTotal = 100, expenseTotal = 65),
            StockRecord(pointId = "p1", itemId = "i_ak", quantity = 40, incomeTotal = 50, expenseTotal = 10),
            StockRecord(pointId = "base", itemId = "i_kit", quantity = 12, incomeTotal = 12, expenseTotal = 0)
        )

        val form18 = ReportBuilders.buildMilitaryForm18(
            operations = ops,
            stockRecords = stocks,
            points = milPoints,
            catalogItems = milItems,
            unitName = milProfile.unitName,
            responsiblePerson = milProfile.callsign,
            parseItems = parseOps
        )

        assertEquals(9, form18.headers.size)
        assertTrue(form18.formCodeRows.any { it.second == "0504042" })

        // Verify internal transfer & internal issue rows keep running balance at 100 (col index 6 = Состоит)
        val transferRow = form18.rows.first { it[3].contains("Внутр. перемещение") }
        assertEquals("100", transferRow[6])
        assertEquals("", transferRow[5])

        val internalIssueRow = form18.rows.first { it[3].contains("Передано на точку") }
        assertEquals("100", internalIssueRow[6])
        assertEquals("", internalIssueRow[5])

        val extIssueRow = form18.rows.first { it[3].contains("Иванов") }
        assertEquals("15", extIssueRow[5])
        assertEquals("85", extIssueRow[6])

        val expRow = form18.rows.first { it[3].contains("Списано") }
        assertEquals("10", expRow[5])
        assertEquals("75", expRow[6])

        // Verify stock-only item (Аптечка АИ-4) is ALSO included in Form 18!
        assertTrue(form18.rows.any { it[2].contains("Аптечка АИ-4") })
        val kitRow = form18.rows.first { it[2].contains("Инвентаризация / остаток") }
        assertEquals("12", kitRow[6])
    }

    // -------------------------------------------------------------------------
    // 3. Civilian Statutory Forms (M-11/TORG-13, MB-7, MB-8) & Unit-Safe Consolidated
    // -------------------------------------------------------------------------

    @Test
    fun civilianReports_m11Mb7Mb8AndUnitSafeConsolidated() {
        val civProfile = UserProfile(callsign = "Алексей", unitName = "Мастерская № 1", isLoggedIn = true)
        val civPoints = listOf(
            WarehousePoint("base_sklad", "Основной склад", "", true, 0),
            WarehousePoint("pt_site", "Объект № 1", "", false, 1)
        )
        val civItems = listOf(
            InventoryItem("c1", "Перфоратор SDS-Plus", "Электроинструмент", "Перфораторы", "шт.", "Кат. 1", "EL-001"),
            InventoryItem("c2", "Масло 5W-40", "ГСМ, химия и масла", "Масла", "л.", "Кат. 1", "CH-001")
        )
        val ops = listOf(
            OperationRecord(
                id = "c_op1", type = OperationType.INCOME, fromPointName = "Оптовик", toPointName = "Основной склад",
                docNumber = "УПД-5", responsiblePerson = "Алексей", timestamp = 1_730_000_000_000L,
                itemsSummary = "Перфоратор SDS-Plus — 10 шт., Масло 5W-40 — 50 л.",
                itemsJson = """[{"itemId":"c1","itemName":"Перфоратор SDS-Plus","unit":"шт.","quantity":10},{"itemId":"c2","itemName":"Масло 5W-40","unit":"л.","quantity":50}]"""
            ),
            OperationRecord(
                id = "c_op2", type = OperationType.TRANSFER, fromPointName = "Основной склад", toPointName = "Объект № 1",
                docNumber = "М11-01", responsiblePerson = "Алексей", timestamp = 1_730_001_000_000L,
                itemsSummary = "Перфоратор SDS-Plus — 4 шт.",
                itemsJson = """[{"itemId":"c1","itemName":"Перфоратор SDS-Plus","unit":"шт.","quantity":4}]"""
            ),
            OperationRecord(
                id = "c_op3", type = OperationType.ISSUE, fromPointName = "Основной склад", toPointName = "Бригадир Смирнов",
                docNumber = "МБ7-01", responsiblePerson = "Алексей", timestamp = 1_730_002_000_000L,
                itemsSummary = "Перфоратор SDS-Plus — 2 шт.",
                itemsJson = """[{"itemId":"c1","itemName":"Перфоратор SDS-Plus","unit":"шт.","quantity":2}]"""
            ),
            OperationRecord(
                id = "c_op4", type = OperationType.EXPENDITURE, fromPointName = "Основной склад", toPointName = "Списание",
                docNumber = "МБ8-01", responsiblePerson = "Алексей", timestamp = 1_730_003_000_000L,
                itemsSummary = "Масло 5W-40 — 10 л.",
                itemsJson = """[{"itemId":"c2","itemName":"Масло 5W-40","unit":"л.","quantity":10}]"""
            )
        )
        val stocks = listOf(
            StockRecord(pointId = "base_sklad", itemId = "c1", quantity = 4, incomeTotal = 10, expenseTotal = 6),
            StockRecord(pointId = "pt_site", itemId = "c1", quantity = 4, incomeTotal = 4, expenseTotal = 0),
            StockRecord(pointId = "base_sklad", itemId = "c2", quantity = 40, incomeTotal = 50, expenseTotal = 10)
        )

        // M-11 / ТОРГ-13: captures internal transfer only (not external person issue)
        val m11 = ReportBuilders.buildCivilianTransferInvoiceM11(
            operations = ops,
            points = civPoints,
            catalogItems = civItems,
            unitName = civProfile.unitName,
            responsiblePerson = civProfile.callsign,
            parseItems = parseOps
        )
        assertTrue(m11.formCodeRows.any { it.second.contains("0315006") })
        assertEquals(1, m11.rows.size)
        assertTrue(m11.rows[0].any { it.contains("Перфоратор SDS-Plus") })
        assertTrue(m11.rows[0].any { it == "4" })

        // МБ-7: captures external issue to Бригадир Смирнов
        val mb7 = ReportBuilders.buildCivilianIssueRegisterMB7(
            operations = ops,
            catalogItems = civItems,
            unitName = civProfile.unitName,
            responsiblePerson = civProfile.callsign,
            parseItems = parseOps
        )
        assertTrue(mb7.formCodeRows.any { it.second == "0320003" })
        assertEquals(1, mb7.rows.size)
        assertEquals("Бригадир Смирнов", mb7.rows[0][1])
        assertEquals("2", mb7.rows[0][5])

        // МБ-8: captures write-off of 10 л. oil
        val mb8 = ReportBuilders.buildCivilianWriteOffActMB8(
            operations = ops,
            catalogItems = civItems,
            unitName = civProfile.unitName,
            responsiblePerson = civProfile.callsign,
            parseItems = parseOps
        )
        assertEquals(1, mb8.rows.size)
        assertTrue(mb8.rows[0].any { it == "10" })

        // Consolidated report: MUST NOT mix шт. + л. into 48, MUST NOT inflate transfer!
        val consolidated = ReportBuilders.buildConsolidatedTurnoverReport(
            profileMode = WorkspaceProfileMode.CIVILIAN,
            catalogItems = civItems,
            stockRecords = stocks,
            operations = ops,
            points = civPoints,
            unitName = civProfile.unitName,
            responsiblePerson = civProfile.callsign,
            parseItems = parseOps
        )
        val totals = consolidated.totalRow!!
        val finalStockCell = totals.last { it.isNotBlank() && it != "—" }
        assertTrue("Unit-safe stock total must preserve шт. (was: $finalStockCell)", finalStockCell.contains("8 шт."))
        assertTrue("Unit-safe stock total must preserve л. (was: $finalStockCell)", finalStockCell.contains("40 л."))
        assertFalse("Must never sum pieces + liters into 48", finalStockCell == "48")

        // External income = 10 шт. + 50 л. (NOT inflated to 14 шт. by internal transfer of 4!)
        val incomeColIdx = consolidated.headers.indexOfFirst { it.contains("Приход", ignoreCase = true) }
        assertTrue(totals[incomeColIdx].contains("10 шт."))
        assertFalse(totals[incomeColIdx].contains("14 шт."))
    }

    // -------------------------------------------------------------------------
    // 4. XLSX Generation Hygiene (Dynamic mergeCells count + Formula Escaping)
    // -------------------------------------------------------------------------

    @Test
    fun excelExport_generatesValidXmlWithDynamicMergeCountAndFormulaEscaping() {
        val maliciousOp = OperationRecord(
            id = "x1",
            type = OperationType.ISSUE,
            fromPointName = "Базовый склад (КЗ)",
            toPointName = "=HYPERLINK(\"http://evil\")",
            docNumber = "1",
            responsiblePerson = "+cmd",
            timestamp = 1_730_000_000_000L,
            itemsSummary = "Патрон 5,45х39 ПС — 5 шт.",
            itemsJson = """[{"itemId":"i_ak","itemName":"Патрон 5,45х39 ПС","unit":"шт.","quantity":5}]"""
        )
        val report = ReportBuilders.buildMilitaryForm8(
            operations = listOf(maliciousOp),
            catalogItems = milItems,
            unitName = milProfile.unitName,
            responsiblePerson = milProfile.callsign,
            parseItems = parseOps
        )
        val bytes = ExcelExportHelper.generateXlsxBytes(report.toExcelReportData())
        assertTrue("XLSX must be non-empty", bytes.size > 1000)

        var sheetXml = ""
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                if (entry.name == "xl/worksheets/sheet1.xml") {
                    sheetXml = zis.bufferedReader().readText()
                }
            }
        }
        assertTrue("sheet1.xml must exist", sheetXml.isNotBlank())

        // Verify mergeCells count matches exact number of <mergeCell .../> tags!
        val countMatch = Regex("""<mergeCells count="(\d+)">""").find(sheetXml)
        assertNotNull("mergeCells tag must exist", countMatch)
        val declaredCount = countMatch!!.groupValues[1].toInt()
        val actualCount = Regex("""<mergeCell ref="[^"]+"/>""").findAll(sheetXml).count()
        assertEquals("Declared mergeCells count must exactly equal actual mergeCell nodes", actualCount, declaredCount)

        // Verify Excel formula injection is neutralized with leading apostrophe (XML-escaped as &apos;)
        assertTrue(
            "Leading = must be neutralized with apostrophe",
            sheetXml.contains("&apos;=HYPERLINK") || sheetXml.contains("'=HYPERLINK")
        )
        assertFalse("Unescaped formula tag must not appear", sheetXml.contains("<t>=HYPERLINK"))
        assertTrue("Landscape A4 print setup must be present", sheetXml.contains("""orientation="landscape""""))
    }

    // -------------------------------------------------------------------------
    // 5. Returns (Возврат), Multi-Warehouse Reconciliation & Profile Reconfigure
    // -------------------------------------------------------------------------

    @Test
    fun returnOperation_populatesMb7ReturnColumnAndDoesNotInflateTurnover() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, KapterkaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.kapterkaDao()
            val repo = KapterkaRepository(dao, null)
            repo.ensureInitialized(WorkspaceProfileMode.CIVILIAN)

            val points = dao.getAllPoints().first()
            val base = points.first { it.isBase }
            val workshop = points.first { !it.isBase }
            val drill = dao.getAllItems().first().first { it.name.contains("Перфоратор") || it.unit == "шт." }

            // 1. Purchase 10 pcs on Base
            repo.recordIncome(
                toPointId = base.id,
                toPointName = base.name,
                supplier = "ООО Инструмент",
                items = listOf(OperationItemEntry(drill.id, drill.name, drill.unit, 10)),
                comment = "УПД 100",
                actor = "Алексей",
                isMilitary = false
            )
            // 2. Transfer 4 pcs Base -> Workshop
            repo.recordTransfer(
                fromPointId = base.id,
                fromPointName = base.name,
                toPointId = workshop.id,
                toPointName = workshop.name,
                items = listOf(OperationItemEntry(drill.id, drill.name, drill.unit, 4)),
                comment = "На объект",
                actor = "Алексей"
            )
            // 3. Issue 3 pcs from Base to Петров А.С.
            repo.recordIssueToPerson(
                fromPointId = base.id,
                fromPointName = base.name,
                recipient = "Петров А.С.",
                items = listOf(OperationItemEntry(drill.id, drill.name, drill.unit, 3)),
                comment = "Бригада 1",
                actor = "Алексей"
            )
            // 4. Return 2 pcs from Петров А.С. back to Base
            repo.recordReturn(
                toPointId = base.id,
                toPointName = base.name,
                returnedBy = "Петров А.С.",
                items = listOf(OperationItemEntry(drill.id, drill.name, drill.unit, 2)),
                comment = "Исправен",
                actor = "Алексей"
            )

            val ops = dao.getAllOperations().first()
            val stocks = dao.getAllStockRecords().first()
            val items = dao.getAllItems().first()

            // Base stock = 10 - 4 - 3 + 2 = 5; Workshop stock = 4; Total across ALL warehouses = 9!
            val baseReport = ReportBuilders.buildSinglePointReport(
                profileMode = WorkspaceProfileMode.CIVILIAN,
                point = base,
                catalogItems = items,
                stockRecords = stocks.filter { it.pointId == base.id },
                operations = ops,
                unitName = "Мастерская",
                parseItems = parseOps
            )
            val workshopReport = ReportBuilders.buildSinglePointReport(
                profileMode = WorkspaceProfileMode.CIVILIAN,
                point = workshop,
                catalogItems = items,
                stockRecords = stocks.filter { it.pointId == workshop.id },
                operations = ops,
                unitName = "Мастерская",
                parseItems = parseOps
            )
            assertEquals("5", baseReport.rows.single()[6])
            assertEquals("4", workshopReport.rows.single()[6])

            // Turnover report: external income MUST remain 10 (not 12!), net issued = 3 - 2 = 1, total stock = 9!
            val turnover = ReportBuilders.buildConsolidatedTurnoverReport(
                profileMode = WorkspaceProfileMode.CIVILIAN,
                catalogItems = items,
                stockRecords = stocks,
                operations = ops,
                points = points,
                unitName = "Мастерская",
                parseItems = parseOps
            )
            val drillRow = turnover.rows.single()
            assertEquals("10", drillRow[4]) // External income
            assertEquals("1", drillRow[5])  // Net outgoing (3 issued - 2 returned)
            assertEquals("9", drillRow[7])  // Exact sum of Base (5) + Workshop (4)
            assertTrue(drillRow[6].contains("${base.name}: 5"))
            assertTrue(drillRow[6].contains("${workshop.name}: 4"))

            // MB-7 must match Петров А.С.'s return in column 9!
            val mb7 = ReportBuilders.buildCivilianIssueRegisterMB7(
                operations = ops,
                catalogItems = items,
                unitName = "Мастерская",
                parseItems = parseOps
            )
            assertEquals(1, mb7.rows.size)
            assertTrue(
                "MB-7 column 9 must record partial return 2 of 3 (was: '${mb7.rows[0][8]}')",
                mb7.rows[0][8].contains("2 из 3")
            )

            // Form 18 must also reconcile ending balance to exact 9 across all warehouses
            val form18 = ReportBuilders.buildMilitaryForm18(
                operations = ops,
                stockRecords = stocks,
                points = points,
                catalogItems = items,
                unitName = "Мастерская",
                parseItems = parseOps
            )
            assertEquals("9", form18.totalRow!![6])
        } finally {
            db.close()
        }
    }

    @Test
    fun reconfigureProfile_resetsOldOperationsStocksRequisitionsAndWarehousesWhenSwitching() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, KapterkaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.kapterkaDao()
            val repo = KapterkaRepository(dao, null)

            // Start in Military mode (clean)
            repo.ensureInitialized(WorkspaceProfileMode.MILITARY)
            assertTrue(dao.getAllItems().first().any { it.id.startsWith("rav_") })

            // Reconfigure to Civilian on clean DB -> switches cleanly to Civilian catalog & warehouses
            val (changedClean, hadDataClean) = repo.reconfigureProfile(WorkspaceProfileMode.CIVILIAN, true)
            assertTrue(changedClean)
            assertFalse(hadDataClean)
            assertFalse(dao.getAllItems().first().any { it.id.startsWith("rav_") })
            assertEquals(InitialData.getCivilianItems().size, dao.getAllItems().first().size)

            // Record real operations (income + issue), stock, requisition, and custom warehouse in Civilian mode
            val base = dao.getAllPoints().first().first { it.isBase }
            val civItem = dao.getAllItems().first().first()
            repo.recordIncome(
                base.id, base.name, "Поставщик",
                listOf(OperationItemEntry(civItem.id, civItem.name, civItem.unit, 10)),
                "Приход по накладной", "Алексей", isMilitary = false
            )
            repo.recordIssueToPerson(
                base.id, base.name, "Иванов И.И.",
                listOf(OperationItemEntry(civItem.id, civItem.name, civItem.unit, 3)),
                "Выдача в работу", "Алексей"
            )
            repo.createRequisition(
                pointName = "Бригада №1",
                applicant = "Петров",
                items = listOf(com.example.data.model.RequisitionItemEntry(civItem.name, 2, civItem.unit)),
                comment = "Срочно"
            )
            repo.addWarehousePoint("Временный вагончик", "Объект")

            assertEquals(2, dao.getAllOperations().first().size)
            assertEquals(1, dao.getAllStockRecords().first().size)
            assertEquals(1, dao.getAllRequisitions().first().size)

            // Mode-only switch (updateWarehousesAndCatalog = false) preserves operations & stocks
            val (changedFormsOnly, hadDataFormsOnly) = repo.reconfigureProfile(WorkspaceProfileMode.MILITARY, false)
            assertFalse(changedFormsOnly)
            assertTrue(hadDataFormsOnly)
            assertEquals(2, dao.getAllOperations().first().size)
            assertEquals(7, dao.getAllStockRecords().first().single().quantity)

            // Switching warehouses in profile (updateWarehousesAndCatalog = true) MUST reset all old operations, stocks, requisitions & warehouses!
            val (changedWithReset, hadData) = repo.reconfigureProfile(WorkspaceProfileMode.EMPTY, true)
            assertTrue(changedWithReset)
            assertTrue(hadData)
            assertTrue("Old operations must be cleared when switching warehouses in profile", dao.getAllOperations().first().isEmpty())
            assertTrue("Old stock records must be cleared when switching warehouses in profile", dao.getAllStockRecords().first().isEmpty())
            assertTrue("Old requisitions must be cleared when switching warehouses in profile", dao.getAllRequisitions().first().isEmpty())
            assertTrue("EMPTY profile must have 0 items", dao.getAllItems().first().isEmpty())
            assertEquals(1, dao.getAllPoints().first().size)
            assertEquals("Основной склад", dao.getAllPoints().first().single().name)

            // Switching from EMPTY back to MILITARY loads clean Military points & catalog with 0 stale operations
            val (changedToMil, _) = repo.reconfigureProfile(WorkspaceProfileMode.MILITARY, true)
            assertTrue(changedToMil)
            assertTrue(dao.getAllOperations().first().isEmpty())
            assertTrue(dao.getAllStockRecords().first().isEmpty())
            assertEquals(InitialData.getDefaultPoints().size, dao.getAllPoints().first().size)
            assertEquals(InitialData.getDefaultItems().size, dao.getAllItems().first().size)
        } finally {
            db.close()
        }
    }
}
