package com.example.debloater

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Create a simple list view programmatically (no messy XML files needed yet)
        val listView = ListView(this)
        setContentView(listView)

        Toast.makeText(this, "Scanning for bloatware...", Toast.LENGTH_SHORT).show()

        // 2. Run the scanner to get the apps
        val appList = getInstalledApps()

        // 3. Feed the scanned apps into the visual list
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, appList)
        listView.adapter = adapter
        
        // 4. Test trigger: When you tap an app, it shows a popup with its name
        listView.setOnItemClickListener { _, _, position, _ ->
            val clickedApp = appList[position]
            Toast.makeText(this, "Ready to target: \n$clickedApp", Toast.LENGTH_SHORT).show()
        }
    }

    // --- THE SCANNER LOGIC ---
    private fun getInstalledApps(): List<String> {
        val pm = packageManager
        // This relies on the QUERY_ALL_PACKAGES permission we added to your Manifest!
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val appNamesAndPackages = mutableListOf<String>()

        for (appInfo in packages) {
            val appName = pm.getApplicationLabel(appInfo).toString()
            val packageName = appInfo.packageName
            
            // Format the text to show both the Name and the System Package ID
            appNamesAndPackages.add("$appName\n$packageName")
        }
        
        // Sort them alphabetically so you can actually find what you are looking for
        return appNamesAndPackages.sorted()
    }
}
