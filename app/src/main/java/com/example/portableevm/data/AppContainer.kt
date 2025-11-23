package com.example.portableevm.data

import android.content.Context

class AppContainer(context: Context) {
    private val database: EvmDatabase = EvmDatabase.getInstance(context)

    val electionRepository: ElectionRepository = ElectionRepository(database.electionDao())
    val adminRepository: AdminRepository = AdminRepository(database.adminSettingsDao())
}
