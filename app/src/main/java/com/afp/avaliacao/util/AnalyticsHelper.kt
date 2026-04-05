package com.afp.avaliacao.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

class AnalyticsHelper(context: Context) {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    fun logEvent(eventName: String, params: Bundle? = null) {
        firebaseAnalytics.logEvent(eventName, params)
    }

    fun logScreenView(screenName: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    companion object {
        const val EVENT_CHECKIN_COMPLETE = "checkin_complete"
        const val EVENT_CHECKOUT_COMPLETE = "checkout_complete"
        const val EVENT_PLANO_GENERATED = "plano_generated"
        const val EVENT_PDF_EXPORTED = "pdf_exported"
        const val EVENT_DEMO_MODE_TOGGLED = "demo_mode_toggled"
    }
}
