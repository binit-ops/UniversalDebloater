package com.example.debloater

import rikka.shizuku.Shizuku
import com.topjohnwu.superuser.Shell

class ShizukuController {
    
    // Initialize the root shell environment safely
    init {
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        )
    }

    fun disableApp(packageName: String): Boolean {
        val command = "pm uninstall -k --user 0 $packageName"

        // TRIGGER 1: Try Shizuku first
        if (Shizuku.pingBinder()) {
            return try {
                val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
                process.waitFor() == 0
            } catch (e: Exception) {
                false
            }
        }

        // TRIGGER 2: Fallback to Root (KernelSU / Magisk)
        if (Shell.getShell().isRoot) {
            val result = Shell.cmd(command).exec()
            return result.isSuccess
        }

        // If neither Shizuku nor Root is available
        return false
    }
}
