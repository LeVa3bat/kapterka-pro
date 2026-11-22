package com.example.data.model

/**
 * Room v3 & cross-device sync-safe marker for item returns («Возврат»).
 * Stored as OperationType.INCOME so older app versions and Firestore sync
 * deserialize it without schema migrations or crashes.
 */
const val RETURN_DOC_MARKER = "ВОЗВРАТ"
const val RETURN_FROM_PREFIX = "Возврат: "

fun OperationRecord.isReturnOperation(): Boolean =
    type == OperationType.INCOME && (
        docNumber.trim().equals(RETURN_DOC_MARKER, ignoreCase = true) ||
            fromPointName.trim().startsWith(RETURN_FROM_PREFIX, ignoreCase = true)
        )

fun OperationRecord.returnSenderName(): String =
    fromPointName
        .trim()
        .removePrefix(RETURN_FROM_PREFIX)
        .removePrefix("Возврат:")
        .trim()
        .ifBlank { "Сотрудник / объект" }
