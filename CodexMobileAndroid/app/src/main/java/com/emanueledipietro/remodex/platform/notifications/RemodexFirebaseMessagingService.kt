package com.emanueledipietro.remodex.platform.notifications

import com.emanueledipietro.remodex.RemodexApplication
import com.google.firebase.messaging.FirebaseMessagingService

class RemodexFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val app = applicationContext as? RemodexApplication ?: return
        app.container.managedPushRegistrationCoordinator.handleTokenUpdated(token)
    }
}
