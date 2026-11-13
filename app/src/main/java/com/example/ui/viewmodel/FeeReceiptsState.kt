package com.example.ui.viewmodel

import com.example.data.local.entities.FeePaymentTransactionEntity
import com.example.data.local.entities.FeeRecordEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.repository.FeeBalanceSummary
import com.example.data.repository.FeePaymentReceiptResult

sealed class FeeLedgerLoadState {
    data object Loading : FeeLedgerLoadState()
    data object Empty : FeeLedgerLoadState()
    data object Restricted : FeeLedgerLoadState()
    data class Ready(val summaries: List<FeeBalanceSummary>) : FeeLedgerLoadState()
    data class Error(val message: String) : FeeLedgerLoadState()
}

data class FeeWorkflowUiState(
    val loadState: FeeLedgerLoadState = FeeLedgerLoadState.Loading,
    val activeTenantId: String = "",
    val actorRoleName: String = "",
    val actorDisplayName: String = "",
    val isAuthorizedToManageFees: Boolean = false,
    val isReadOnlySelfScope: Boolean = false,
    val studentSearchQuery: String = "",
    val matchedStudents: List<StudentEntity> = emptyList(),
    val selectedStudent: StudentEntity? = null,
    val feeSummaries: List<FeeBalanceSummary> = emptyList(),
    val transactions: List<FeePaymentTransactionEntity> = emptyList(),
    val totalCollectedPaise: Long = 0L,
    val totalOutstandingPaise: Long = 0L,
    val settledAccountCount: Int = 0,
    val dueAccountCount: Int = 0,
    val pendingSyncTransactionCount: Int = 0,
    val isOnline: Boolean = true,
    val isSubmitting: Boolean = false,
    val isSyncingOutbox: Boolean = false,
    val syncStatusMessage: String = "Ledger ready",
    val validationError: String? = null,
    val feedbackMessage: String? = null,
    val activeReceiptResult: FeePaymentReceiptResult? = null,
    val selectedFeeRecordForPayment: FeeRecordEntity? = null
)
