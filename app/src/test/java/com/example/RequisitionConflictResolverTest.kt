package com.example

import com.example.data.model.RequestStatus
import com.example.data.model.RequisitionRequest
import com.example.data.sync.RequisitionConflictResolver
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RequisitionConflictResolverTest {

    private fun req(
        id: String = "req-1",
        status: RequestStatus = RequestStatus.PENDING,
        timestamp: Long = 1_000L
    ) = RequisitionRequest(
        id = id,
        pointName = "Основной склад",
        applicantName = "Иванов И.И.",
        status = status,
        comment = "",
        timestamp = timestamp,
        itemsSummary = "Кабель ВВГ - 10 м.",
        itemsJson = "[]"
    )

    @Test
    fun brandNewCloudRequisitionIsAlwaysAccepted() {
        val cloud = req(status = RequestStatus.PENDING, timestamp = 1_000L)
        assertTrue(RequisitionConflictResolver.cloudMayReplaceLocal(null, cloud, 0L, 1_000L))
    }

    @Test
    fun staleCloudPendingDoesNotRevertOfflineAssemblingStatus() {
        val local = req(status = RequestStatus.ASSEMBLING, timestamp = 1_000L)
        val cloud = req(status = RequestStatus.PENDING, timestamp = 1_000L)
        assertFalse(RequisitionConflictResolver.cloudMayReplaceLocal(local, cloud, localUpdatedAt = 2_000L, cloudUpdatedAt = 1_000L))
        assertFalse(RequisitionConflictResolver.cloudMayReplaceLocal(local, cloud, localUpdatedAt = 0L, cloudUpdatedAt = 1_000L))
    }

    @Test
    fun cloudForwardProgressIsAccepted() {
        val local = req(status = RequestStatus.PENDING, timestamp = 1_000L)
        val cloud = req(status = RequestStatus.COLLECTED, timestamp = 1_000L)
        assertTrue(RequisitionConflictResolver.cloudMayReplaceLocal(local, cloud, localUpdatedAt = 1_000L, cloudUpdatedAt = 2_500L))
        assertTrue(RequisitionConflictResolver.cloudMayReplaceLocal(local, cloud, localUpdatedAt = 0L, cloudUpdatedAt = 1_000L))
    }

    @Test
    fun newerCloudStepBackWinsOverOlderLocalStatus() {
        val local = req(status = RequestStatus.COLLECTED, timestamp = 1_000L)
        val cloud = req(status = RequestStatus.ASSEMBLING, timestamp = 1_000L)
        assertTrue(RequisitionConflictResolver.cloudMayReplaceLocal(local, cloud, localUpdatedAt = 2_000L, cloudUpdatedAt = 3_500L))
    }

    @Test
    fun olderCloudStepBackDoesNotOverwriteNewerLocalProgress() {
        val local = req(status = RequestStatus.ISSUED, timestamp = 1_000L)
        val cloud = req(status = RequestStatus.ASSEMBLING, timestamp = 1_000L)
        assertFalse(RequisitionConflictResolver.cloudMayReplaceLocal(local, cloud, localUpdatedAt = 4_000L, cloudUpdatedAt = 3_500L))
    }
}
