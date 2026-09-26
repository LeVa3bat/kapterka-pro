package com.example.data.backup

import com.example.data.model.InventoryItem
import com.example.data.model.OperationRecord
import com.example.data.model.OperationType
import com.example.data.model.RequestStatus
import com.example.data.model.RequisitionRequest
import com.example.data.model.StockRecord
import com.example.data.model.WarehousePoint
import org.json.JSONArray
import org.json.JSONObject

/** Складские данные, попадающие в файл резервной копии. Лицензия, ключи и профиль не включаются. */
data class BackupSnapshot(
    val items: List<InventoryItem>,
    val points: List<WarehousePoint>,
    val stocks: List<StockRecord>,
    val operations: List<OperationRecord>,
    val requisitions: List<RequisitionRequest>
)

class BackupFormatException(message: String) : Exception(message)

object BackupCodec {
    const val FORMAT = "kapterka-backup"
    const val VERSION = 1

    fun encode(snapshot: BackupSnapshot, appVersion: String, createdAt: Long): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", VERSION)
        root.put("appVersion", appVersion)
        root.put("createdAt", createdAt)
        root.put("items", JSONArray().also { a ->
            snapshot.items.forEach {
                a.put(
                    JSONObject()
                        .put("id", it.id).put("name", it.name)
                        .put("serviceCategory", it.serviceCategory).put("subType", it.subType)
                        .put("unit", it.unit).put("categoryClass", it.categoryClass)
                        .put("standardCode", it.standardCode).put("isCustom", it.isCustom)
                )
            }
        })
        root.put("points", JSONArray().also { a ->
            snapshot.points.forEach {
                a.put(
                    JSONObject()
                        .put("id", it.id).put("name", it.name).put("description", it.description)
                        .put("isBase", it.isBase).put("orderIndex", it.orderIndex)
                        .put("createdAt", it.createdAt)
                )
            }
        })
        root.put("stocks", JSONArray().also { a ->
            snapshot.stocks.forEach {
                a.put(
                    JSONObject()
                        .put("pointId", it.pointId).put("itemId", it.itemId)
                        .put("quantity", it.quantity).put("incomeTotal", it.incomeTotal)
                        .put("expenseTotal", it.expenseTotal).put("lastUpdated", it.lastUpdated)
                )
            }
        })
        root.put("operations", JSONArray().also { a ->
            snapshot.operations.forEach {
                a.put(
                    JSONObject()
                        .put("id", it.id).put("type", it.type.name)
                        .put("fromPointName", it.fromPointName).put("toPointName", it.toPointName)
                        .put("docNumber", it.docNumber).put("responsiblePerson", it.responsiblePerson)
                        .put("comment", it.comment).put("timestamp", it.timestamp)
                        .put("itemsSummary", it.itemsSummary).put("itemsJson", it.itemsJson)
                )
            }
        })
        root.put("requisitions", JSONArray().also { a ->
            snapshot.requisitions.forEach {
                a.put(
                    JSONObject()
                        .put("id", it.id).put("pointName", it.pointName)
                        .put("applicantName", it.applicantName).put("status", it.status.name)
                        .put("comment", it.comment).put("timestamp", it.timestamp)
                        .put("itemsSummary", it.itemsSummary).put("itemsJson", it.itemsJson)
                )
            }
        })
        return root.toString()
    }

    /** Разбирает файл целиком; при любой ошибке бросает [BackupFormatException], базу не трогает. */
    fun decode(text: String): BackupSnapshot {
        try {
            val root = JSONObject(text)
            if (root.optString("format") != FORMAT) throw BackupFormatException("Это не файл копии Каптёрки")
            if (root.optInt("version", 0) > VERSION) {
                throw BackupFormatException("Копия сделана более новой версией приложения. Обновите приложение")
            }
            fun list(name: String): List<JSONObject> {
                val a = root.getJSONArray(name)
                return List(a.length()) { a.getJSONObject(it) }
            }
            return BackupSnapshot(
                items = list("items").map {
                    InventoryItem(
                        id = it.getString("id"), name = it.getString("name"),
                        serviceCategory = it.getString("serviceCategory"), subType = it.getString("subType"),
                        unit = it.getString("unit"), categoryClass = it.optString("categoryClass", "Кат. 1"),
                        standardCode = it.optString("standardCode", ""), isCustom = it.optBoolean("isCustom", false)
                    )
                },
                points = list("points").map {
                    WarehousePoint(
                        id = it.getString("id"), name = it.getString("name"),
                        description = it.optString("description", ""), isBase = it.optBoolean("isBase", false),
                        orderIndex = it.optInt("orderIndex", 0), createdAt = it.optLong("createdAt", 0L)
                    )
                },
                stocks = list("stocks").map {
                    StockRecord(
                        pointId = it.getString("pointId"), itemId = it.getString("itemId"),
                        quantity = it.optInt("quantity", 0), incomeTotal = it.optInt("incomeTotal", 0),
                        expenseTotal = it.optInt("expenseTotal", 0), lastUpdated = it.optLong("lastUpdated", 0L)
                    )
                },
                operations = list("operations").map {
                    OperationRecord(
                        id = it.getString("id"),
                        type = runCatching { OperationType.valueOf(it.getString("type")) }.getOrDefault(OperationType.INCOME),
                        fromPointName = it.optString("fromPointName", ""), toPointName = it.optString("toPointName", ""),
                        docNumber = it.optString("docNumber", ""), responsiblePerson = it.optString("responsiblePerson", ""),
                        comment = it.optString("comment", ""), timestamp = it.optLong("timestamp", 0L),
                        itemsSummary = it.optString("itemsSummary", ""), itemsJson = it.optString("itemsJson", "")
                    )
                },
                requisitions = list("requisitions").map {
                    RequisitionRequest(
                        id = it.getString("id"), pointName = it.getString("pointName"),
                        applicantName = it.optString("applicantName", ""),
                        status = runCatching { RequestStatus.valueOf(it.getString("status")) }.getOrDefault(RequestStatus.PENDING),
                        comment = it.optString("comment", ""), timestamp = it.optLong("timestamp", 0L),
                        itemsSummary = it.optString("itemsSummary", ""), itemsJson = it.optString("itemsJson", "")
                    )
                }
            )
        } catch (e: BackupFormatException) {
            throw e
        } catch (e: Exception) {
            throw BackupFormatException("Файл повреждён или это не копия Каптёрки")
        }
    }
}
