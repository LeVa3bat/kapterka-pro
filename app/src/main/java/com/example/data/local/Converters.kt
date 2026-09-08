package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.OperationType
import com.example.data.model.RequestStatus

class Converters {
    @TypeConverter
    fun fromOperationType(value: OperationType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toOperationType(value: String?): OperationType? {
        return value?.let {
            try {
                OperationType.valueOf(it)
            } catch (_: Exception) {
                OperationType.INCOME
            }
        }
    }

    @TypeConverter
    fun fromRequestStatus(value: RequestStatus?): String? {
        return value?.name
    }

    @TypeConverter
    fun toRequestStatus(value: String?): RequestStatus? {
        return value?.let {
            try {
                RequestStatus.valueOf(it)
            } catch (_: Exception) {
                RequestStatus.PENDING
            }
        }
    }
}
