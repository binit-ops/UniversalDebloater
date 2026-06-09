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

    fun freezeApp(packageName: String): Boolean {
        return executeCommand("pm disable-user --user 0 $packageName")
    }

    fun restoreApp(packageName: String): Boolean {
        executeCommand("cmd package install-existing $packageName")
        return executeCommand("pm enable $packageName")
    }
}
