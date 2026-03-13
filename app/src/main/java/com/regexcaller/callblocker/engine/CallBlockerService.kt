package com.regexcaller.callblocker.engine

import android.telecom.Call
import android.telecom.CallScreeningService
import com.regexcaller.callblocker.data.db.AppDatabase
import com.regexcaller.callblocker.data.model.BlockAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CallBlockerService : CallScreeningService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onScreenCall(callDetails: Call.Details) {
        val incomingNumber = callDetails.handle?.schemeSpecificPart ?: run {
            respondToCall(callDetails, buildResponse(block = false, silence = false))
            return
        }

        serviceScope.launch {
            try {
                val dao = AppDatabase.getInstance(applicationContext).blockRuleDao()
                val enabledRules = dao.getEnabledRules()
                val matchingRule = PatternMatcher.findMatchingRule(incomingNumber, enabledRules)

                val response = when (matchingRule?.action) {
                    BlockAction.BLOCK -> {
                        dao.incrementMatchCount(matchingRule.id)
                        buildResponse(block = true, silence = true)
                    }
                    BlockAction.SILENCE -> {
                        dao.incrementMatchCount(matchingRule.id)
                        buildResponse(block = false, silence = true)
                    }
                    BlockAction.ALLOW -> {
                        buildResponse(block = false, silence = false)
                    }
                    else -> {
                        buildResponse(block = false, silence = false)
                    }
                }

                respondToCall(callDetails, response)
            } catch (e: Exception) {
                // Safety net INSIDE the coroutine — must be here, not outside launch{}
                // A try-catch outside serviceScope.launch{} would NOT catch async exceptions
                android.util.Log.e("CallBlockerService", "Error screening call", e)
                respondToCall(callDetails, buildResponse(block = false, silence = false))
            }
        }
    }

    private fun buildResponse(block: Boolean, silence: Boolean): CallResponse {
        return CallResponse.Builder()
            .setDisallowCall(block)
            .setRejectCall(block)
            .setSilenceCall(silence)
            .setSkipCallLog(false)          // Always log — user can review
            .setSkipNotification(block)     // Hide notification only for blocked
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
