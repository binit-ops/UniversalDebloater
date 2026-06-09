package com.example.debloater

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.io.File

data class AppItem(
    val name: String, 
    val packageName: String, 
    val icon: Drawable, 
    val isSystemApp: Boolean,
    val apkPath: String,
    var isSelected: Boolean = false
)

class MainActivity : Activity() {
    
    private val shizukuController = ShizukuController()
    private lateinit var adapter: AppAdapter
    private var allApps = listOf<AppItem>()
    private var currentFilteredApps = listOf<AppItem>()
    private lateinit var profileFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        profileFile = File(getExternalFilesDir(null), "debloat_profile.txt")

        val searchBar = findViewById<EditText>(R.id.searchBar)
        val listView = findViewById<ListView>(R.id.listView)
        
        allApps = getInstalledApps()
        currentFilteredApps = allApps
        adapter = AppAdapter(this, currentFilteredApps.toMutableList())
        listView.adapter = adapter

        // --- FILTER AND SEARCH LOGIC ---
        fun applyFilterAndSearch() {
            val query = searchBar.text.toString().lowercase()
            val searched = currentFilteredApps.filter { 
                it.name.lowercase().contains(query) || it.packageName.lowercase().contains(query) 
            }
            adapter.updateList(searched)
        }

        findViewById<Button>(R.id.btnFilterAll).setOnClickListener {
            currentFilteredApps = allApps; applyFilterAndSearch()
        }
        findViewById<Button>(R.id.btnFilterSystem).setOnClickListener {
            currentFilteredApps = allApps.filter { it.isSystemApp }; applyFilterAndSearch()
        }
        findViewById<Button>(R.id.btnFilterUser).setOnClickListener {
            currentFilteredApps = allApps.filter { !it.isSystemApp }; applyFilterAndSearch()
        }

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilterAndSearch()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        listView.setOnItemClickListener { _, _, position, _ ->
            val clickedApp = adapter.getItem(position) ?: return@setOnItemClickListener
            clickedApp.isSelected = !clickedApp.isSelected
            adapter.notifyDataSetChanged()
        }

        // --- BACKUP APK LOGIC ---
        findViewById<Button>(R.id.btnBackup).setOnClickListener {
            val selectedApps = allApps.filter { it.isSelected }
            if (selectedApps.isEmpty()) return@setOnClickListener
            
            val backupDir = File(getExternalFilesDir(null), "ApkBackups")
            if (!backupDir.exists()) backupDir.mkdirs()
            
            var count = 0
            for (app in selectedApps) {
                try {
                    val sourceFile = File(app.apkPath)
                    if (sourceFile.exists()) {
                        val destFile = File(backupDir, "${app.packageName}.apk")
                        sourceFile.copyTo(destFile, overwrite = true)
                        count++
                    }
                } catch (e: Exception) { }
            }
            Toast.makeText(this, "Backed up $count APKs to ${backupDir.absolutePath}", Toast.LENGTH_LONG).show()
        }

        // --- RESTORE LOGIC ---
        findViewById<Button>(R.id.btnRestore).setOnClickListener {
            executeBatchAction("Restore") { pkg -> shizukuController.restoreApp(pkg) }
        }

        // --- FREEZE LOGIC ---
        findViewById<Button>(R.id.btnFreeze).setOnClickListener {
            executeBatchAction("Freeze") { pkg -> shizukuController.freezeApp(pkg) }
        }

        // --- NUKE LOGIC ---
        findViewById<Button>(R.id.btnNuke).setOnClickListener {
            executeBatchAction("Nuke") { pkg -> shizukuController.nukeApp(pkg) }
        }

        // --- PROFILE SAVE & LOAD ---
        findViewById<Button>(R.id.btnSaveProfile).setOnClickListener {
            val pkgs = allApps.filter { it.isSelected }.map { it.packageName }
            if (pkgs.isNotEmpty()) {
                profileFile.writeText(pkgs.joinToString("\n"))
                Toast.makeText(this, "Profile Saved!", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnLoadProfile).setOnClickListener {
            if (profileFile.exists()) {
                val savedPkgs = profileFile.readLines()
                allApps.forEach { it.isSelected = savedPkgs.contains(it.packageName) }
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Profile Loaded!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun executeBatchAction(actionName: String, action: (String) -> Boolean) {
        val selectedApps = allApps.filter { it.isSelected }
        if (selectedApps.isEmpty()) {
            Toast.makeText(this, "No apps selected!", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("$actionName ${selectedApps.size} apps?")
            .setMessage("Are you sure you want to execute this action via Shizuku/Root?")
            .setPositiveButton("Confirm") { _, _ ->
                var successCount = 0
                for (app in selectedApps) {
                    if (action(app.packageName)) successCount++
                }
                Toast.makeText(this, "Successfully executed on $successCount apps!", Toast.LENGTH_LONG).show()
                selectedApps.forEach { it.isSelected = false }
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getInstalledApps(): List<AppItem> {
        val pm = packageManager
        // Matches uninstalled apps so they can be restored
        val flags = PackageManager.GET_META_DATA or PackageManager.MATCH_UNINSTALLED_PACKAGES
        val packages = pm.getInstalledApplications(flags)
        val appList = mutableListOf<AppItem>()

        for (appInfo in packages) {
            val appName = pm.getApplicationLabel(appInfo).toString()
            val packageName = appInfo.packageName
            val icon = pm.getApplicationIcon(appInfo)
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val path = appInfo.sourceDir ?: ""
            
            appList.add(AppItem(appName, packageName, icon, isSystem, path))
        }
        return appList.sortedBy { it.name.lowercase() }
    }
}

class AppAdapter(context: Activity, private var apps: MutableList<AppItem>) : 
    ArrayAdapter<AppItem>(context, R.layout.list_item, apps) {

    fun updateList(newlist: List<AppItem>) {
        apps.clear()
        apps.addAll(newlist)
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)
        val app = apps[position]
        
        val iconView = view.findViewById<ImageView>(R.id.appIcon)
        val nameView = view.findViewById<TextView>(R.id.appName)
        val packageView = view.findViewById<TextView>(R.id.appPackage)
        val checkbox = view.findViewById<CheckBox>(R.id.appCheckbox)

        iconView.setImageDrawable(app.icon)
        nameView.text = app.name
        packageView.text = app.packageName
        checkbox.isChecked = app.isSelected

        return view
    }
    
    override fun getCount(): Int = apps.size
    override fun getItem(position: Int): AppItem? = apps[position]
    }
    
