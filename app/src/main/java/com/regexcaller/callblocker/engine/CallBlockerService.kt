package com.regexcaller.callblocker.engine

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.regexcaller.callblocker.data.db.AppDatabase
import com.regexcaller.callblocker.data.model.BlockAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

class CallBlockerService : CallScreeningService() {

    companion object {
        private const val TAG = "CallBlockerService"
        private const val SCREENING_TIMEOUT_MS = 4500L
    }

    override fun onScreenCall(callDetails: Call.Details) {
        try {
            val incomingNumber = callDetails.handle?.schemeSpecificPart?.trim().orEmpty()
            if (incomingNumber.isBlank()) {
                respondToCall(callDetails, buildResponse(block = false, silence = false))
                return
            }

            val response = runBlocking(Dispatchers.IO) {
                withTimeoutOrNull(SCREENING_TIMEOUT_MS) {
                    val dao = AppDatabase.getInstance(applicationContext).blockRuleDao()
                    val enabledRules = dao.getEnabledRules()
                    val matchingRule = PatternMatcher.findMatchingRule(incomingNumber, enabledRules)
                    val action = matchingRule?.action?.trim()?.uppercase()

                    when (action) {
                        BlockAction.BLOCK -> {
                            dao.incrementMatchCount(matchingRule.id)
                            Log.d(TAG, "Blocking incoming call via ruleId=${matchingRule.id}")
                            buildResponse(block = true, silence = true)
                        }
                        BlockAction.SILENCE -> {
                            dao.incrementMatchCount(matchingRule.id)
                            Log.d(TAG, "Silencing incoming call via ruleId=${matchingRule.id}")
                            buildResponse(block = false, silence = true)
                        }
                        BlockAction.ALLOW -> {
                            Log.d(TAG, "Allowing incoming call via allowlist ruleId=${matchingRule.id}")
                            buildResponse(block = false, silence = false)
                        }
                        else -> {
                            Log.d(TAG, "No matching rule for incoming number")
                            buildResponse(block = false, silence = false)
                        }
                    }
                } ?: run {
                    Log.w(TAG, "Call screening timed out; allowing call")
                    buildResponse(block = false, silence = false)
                }
            }

            respondToCall(callDetails, response)
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in onScreenCall", e)
            respondToCall(callDetails, buildResponse(block = false, silence = false))
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

}
