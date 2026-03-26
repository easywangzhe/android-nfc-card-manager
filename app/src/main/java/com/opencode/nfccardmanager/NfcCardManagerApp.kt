package com.opencode.nfccardmanager

import android.app.Application
import com.opencode.nfccardmanager.core.database.AuditLogManager
import com.opencode.nfccardmanager.core.security.SecurityManager
import com.opencode.nfccardmanager.feature.template.LocalTemplateRepository

class NfcCardManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AuditLogManager.init(this)
        SecurityManager.init(this)
        LocalTemplateRepository.init(this)
    }
}
