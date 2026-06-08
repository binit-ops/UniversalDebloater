package com.example.debloater

import moe.shizuku.api.Shizuku

class ShizukuController {
    fun disableApp(packageName: String): Boolean {
        if (!Shizuku.pingBinder()) return false
        val command = "pm uninstall -k --user 0 $packageName"
        return try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}
