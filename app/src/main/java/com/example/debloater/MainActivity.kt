package com.example.debloater

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.io.File

// Added boolean to track if the checkbox is ticked
data class AppItem(val name: String, val packageName: String, val icon: Drawable, var isSelected: Boolean = false)

class MainActivity : Activity() {
    
    private val shizukuController = ShizukuController()
    private lateinit var adapter: AppAdapter
    private var allApps = listOf<AppItem>()
    private lateinit var profileFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up the local file path for saving the debloat profile
        profileFile = File(getExternalFilesDir(null), "debloat_profile.txt")

        val searchBar = findViewById<EditText>(R.id.searchBar)
        val listView = findViewById<ListView>(R.id.listView)
        val btnNuke = findViewById<Button>(R.id.btnNuke)
        val btnSaveProfile = findViewById<Button>(R.id.btnSaveProfile)
        val btnLoadProfile = findViewById<Button>(R.id.btnLoadProfile)

        Toast.makeText(this, "Scanning system...", Toast.LENGTH_SHORT).show()

        allApps = getInstalledApps()
        adapter = AppAdapter(this, allApps.toMutableList())
        listView.adapter = adapter

        // --- CHECKBOX TOGGLE LOGIC ---
        listView.setOnItemClickListener { _, _, position, _ ->
            val clickedApp = adapter.getItem(position) ?: return@setOnItemClickListener
            clickedApp.isSelected = !clickedApp.isSelected
            adapter.notifyDataSetChanged()
        }

        // --- SEARCH BAR LOGIC ---
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                val filtered = allApps.filter { 
                    it.name.lowercase().contains(query) || it.packageName.lowercase().contains(query) 
                }
                adapter.updateList(filtered)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // --- BATCH NUKE TRIGGER ---
        btnNuke.setOnClickListener {
            val selectedApps = allApps.filter { it.isSelected }
            if (selectedApps.isEmpty()) {
                Toast.makeText(this, "No apps selected!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Nuke ${selectedApps.size} apps?")
                .setMessage("Are you absolutely sure you want to uninstall these selected apps via Shizuku?")
                .setPositiveButton("Nuke All") { _, _ ->
                    var successCount = 0
                    for (app in selectedApps) {
                        if (shizukuController.disableApp(app.packageName)) {
                            successCount++
                        }
                    }
                    Toast.makeText(this, "Successfully killed $successCount apps!", Toast.LENGTH_LONG).show()
                    
                    // Uncheck them after execution to reset UI
                    selectedApps.forEach { it.isSelected = false }
                    adapter.notifyDataSetChanged()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // --- SAVE PROFILE ---
        btnSaveProfile.setOnClickListener {
            val selectedPackages = allApps.filter { it.isSelected }.map { it.packageName }
            if (selectedPackages.isEmpty()) {
                Toast.makeText(this, "Check some apps first to create a profile!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Writes the package names line-by-line to a text file
            profileFile.writeText(selectedPackages.joinToString("\n"))
            Toast.makeText(this, "Profile Saved to storage!", Toast.LENGTH_SHORT).show()
        }

        // --- LOAD PROFILE ---
        btnLoadProfile.setOnClickListener {
            if (!profileFile.exists()) {
                Toast.makeText(this, "No saved profile found!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val savedPackages = profileFile.readLines()
            var matchCount = 0
            
            // Loop through our apps and check the boxes if they exist in the text file
            for (app in allApps) {
                if (savedPackages.contains(app.packageName)) {
                    app.isSelected = true
                    matchCount++
                } else {
                    app.isSelected = false
                }
            }
            
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "Loaded $matchCount apps from profile!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getInstalledApps(): List<AppItem> {
        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val appList = mutableListOf<AppItem>()

        for (appInfo in packages) {
            val appName = pm.getApplicationLabel(appInfo).toString()
            val packageName = appInfo.packageName
            val icon = pm.getApplicationIcon(appInfo)
            
            appList.add(AppItem(appName, packageName, icon))
        }
        return appList.sortedBy { it.name.lowercase() }
    }
}

// --- UPDATED ADAPTER ---
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
        
        // Feed the checkbox the data
        checkbox.isChecked = app.isSelected

        return view
    }
    
    override fun getCount(): Int = apps.size
    override fun getItem(position: Int): AppItem? = apps[position]
    }
    
