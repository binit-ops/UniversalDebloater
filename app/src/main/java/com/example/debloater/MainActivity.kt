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

// A container to hold the icon, name, and package together
data class AppItem(val name: String, val packageName: String, val icon: Drawable)

class MainActivity : Activity() {
    
    private val shizukuController = ShizukuController()
    private lateinit var adapter: AppAdapter
    private var allApps = listOf<AppItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Connect to the activity_main.xml layout we just created
        setContentView(R.layout.activity_main)

        val searchBar = findViewById<EditText>(R.id.searchBar)
        val listView = findViewById<ListView>(R.id.listView)

        Toast.makeText(this, "Scanning system...", Toast.LENGTH_SHORT).show()

        // Scan the apps and load them into the custom adapter
        allApps = getInstalledApps()
        adapter = AppAdapter(this, allApps.toMutableList())
        listView.adapter = adapter

        // --- SEARCH BAR LOGIC ---
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                // Filter the list if the search matches the name OR the package
                val filtered = allApps.filter { 
                    it.name.lowercase().contains(query) || it.packageName.lowercase().contains(query) 
                }
                adapter.updateList(filtered)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // --- NUKE TRIGGER ---
        listView.setOnItemClickListener { _, _, position, _ ->
            val clickedApp = adapter.getItem(position) ?: return@setOnItemClickListener
            
            AlertDialog.Builder(this)
                .setTitle("Uninstall ${clickedApp.name}?")
                .setMessage("Package: ${clickedApp.packageName}\n\nWarning: Are you absolutely sure?")
                .setPositiveButton("Nuke It") { _, _ ->
                    val success = shizukuController.disableApp(clickedApp.packageName)
                    if (success) {
                        Toast.makeText(this, "Killed: ${clickedApp.name}", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Failed! Is Shizuku active?", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun getInstalledApps(): List<AppItem> {
        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val appList = mutableListOf<AppItem>()

        for (appInfo in packages) {
            val appName = pm.getApplicationLabel(appInfo).toString()
            val packageName = appInfo.packageName
            val icon = pm.getApplicationIcon(appInfo) // GRAB THE ICON
            
            appList.add(AppItem(appName, packageName, icon))
        }
        return appList.sortedBy { it.name.lowercase() }
    }
}

// --- CUSTOM ADAPTER TO BIND ICONS AND TEXT TO THE UI ---
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
        
        // Link our variables to the elements inside list_item.xml
        val iconView = view.findViewById<ImageView>(R.id.appIcon)
        val nameView = view.findViewById<TextView>(R.id.appName)
        val packageView = view.findViewById<TextView>(R.id.appPackage)

        // Set the actual data
        iconView.setImageDrawable(app.icon)
        nameView.text = app.name
        packageView.text = app.packageName

        return view
    }
    
    override fun getCount(): Int = apps.size
    override fun getItem(position: Int): AppItem? = apps[position]
    }
    
