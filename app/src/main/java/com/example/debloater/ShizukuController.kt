package com.example.debloater

import rikka.shizuku.Shizuku
import com.topjohnwu.superuser.Shell

class ShizukuController {
    
    init {
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        )
    }

    private fun executeCommand(command: String): Boolean {
        if (Shizuku.pingBinder()) {
            return try {
                val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
                process.waitFor() == 0
            } catch (e: Exception) {
                false
            }
        }

        if (Shell.getShell().isRoot) {
            val result = Shell.cmd(command).exec()
            return result.isSuccess
        }

        return false
    }

    fun nukeApp(packageName: String): Boolean {
        return executeCommand("pm uninstall -k --user 0 $packageName")
    }

    [span_1](start_span)fun freezeApp(packageName: String): Boolean {
        // Disables the app at the system level so it cannot run or detect network interfaces[span_1](end_span)
        return executeCommand("pm disable-user --user 0 $packageName")
    }

    [span_2](start_span)fun restoreApp(packageName: String): Boolean {
        // Restores uninstalled system apps and ensures they are enabled[span_2](end_span)
        executeCommand("cmd package install-existing $packageName")
        return executeCommand("pm enable $packageName")
    }
}
