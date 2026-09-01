package com.rank.tempbox

import android.app.assist.AssistStructure
import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.O)
class TempBoxAutofillService : AutofillService() {

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        val structure = request.fillContexts.lastOrNull()?.structure ?: run {
            callback.onSuccess(null)
            return
        }

        val email = getActiveEmail() ?: run {
            callback.onSuccess(null)
            return
        }

        val emailAutofillIds = mutableListOf<AutofillId>()
        traverseStructure(structure, emailAutofillIds)

        if (emailAutofillIds.isEmpty()) {
            callback.onSuccess(null)
            return
        }

        val datasetBuilder = Dataset.Builder()
        for (id in emailAutofillIds) {
            datasetBuilder.setValue(id, AutofillValue.forText(email))
        }

        val response = FillResponse.Builder()
            .addDataset(datasetBuilder.build())
            .build()

        callback.onSuccess(response)
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
    }

    private fun getActiveEmail(): String? {
        val prefs = getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)
        val slot = prefs.getInt("active_inbox_slot", 1)
        return prefs.getString(PrefKeys.email(slot), null)
    }

    private fun traverseStructure(structure: AssistStructure, results: MutableList<AutofillId>) {
        val nodes = structure.windowNodeCount
        for (i in 0 until nodes) {
            val node = structure.getWindowNodeAt(i).rootViewNode
            traverseNode(node, results)
        }
    }

    private fun traverseNode(node: AssistStructure.ViewNode, results: MutableList<AutofillId>) {
        if (isEmailField(node)) {
            node.autofillId?.let { results.add(it) }
        }
        for (i in 0 until node.childCount) {
            traverseNode(node.getChildAt(i), results)
        }
    }

    private fun isEmailField(node: AssistStructure.ViewNode): Boolean {
        if (!node.isEnabled || node.autofillId == null) return false

        val hints = node.autofillHints
        if (hints != null) {
            for (hint in hints) {
                val lower = hint.lowercase()
                if (lower.contains("email") || lower.contains("username")) return true
            }
        }

        val hint = node.hint
        if (hint != null && hint.lowercase().contains("email")) return true

        val inputType = node.inputType
        if (inputType != android.text.InputType.TYPE_NULL) {
            val masked = inputType and android.text.InputType.TYPE_MASK_VARIATION
            if (masked == android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                masked == android.text.InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) {
                return true
            }
        }

        val idEntry = node.idEntry?.lowercase() ?: ""
        return idEntry.contains("email") || idEntry.contains("username")
    }
}
