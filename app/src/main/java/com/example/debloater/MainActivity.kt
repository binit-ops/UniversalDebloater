package com.example.debloater

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast

class MainActivity : Activity() {
    
    // Initialize the Shizuku engine we built earlier
    private val shizukuController = ShizukuController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val listView = ListView(this)
        setContentView(listView)

        val appList = getInstalledApps()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, appList)
        listView.adapter = adapter
        
        // The Trigger Logic
        listView.setOnItemClickListener { _, _, position, _ ->
            val clickedItem = appList[position]
            
            // Split the text to separate the App Name from the Package Name
            val lines = clickedItem.split("\n")
            val appName = lines[0]
            val packageName = lines[1]

            // Create a safety confirmation popup
            AlertDialog.Builder(this)
                .setTitle("Uninstall $appName?")
                .setMessage("Package: $packageName\n\nWarning: Uninstalling critical system apps may cause crashes or bootloops. Are you sure?")
                .setPositiveButton("Nuke It") { _, _ ->
                    // EXECUTE SHIZUKU COMMAND
                    val success = shizukuController.disableApp(packageName)
                    
                    if (success) {
                        Toast.makeText(this, "Killed: $appName", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Failed! Is Shizuku active in the background?", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun getInstalledApps(): List<String> {
        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val appNamesAndPackages = mutableListOf<String>()

        for (appInfo in packages) {
            val appName = pm.getApplicationLabel(appInfo).toString()
            val packageName = appInfo.packageName
            appNamesAndPackages.add("$appName\n$packageName")
        }
        
        return appNamesAndPackages.sorted()
    }
}
