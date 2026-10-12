package com.selfmanagement.samsungbridge.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.selfmanagement.samsungbridge.databinding.ActivityRawJsonBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RawJsonActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRawJsonBinding
    private var pretty = true
    private var renderVersion = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRawJsonBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvJsonRange.text = RawJsonStore.currentRange() ?: "Latest local sync"
        val adapter = ArrayAdapter.createFromResource(this, com.selfmanagement.samsungbridge.R.array.raw_json_categories, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spJsonCategory.adapter = adapter
        binding.spJsonCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = render()
        }
        binding.btnCloseJson.setOnClickListener { finish() }
        binding.btnJsonFormat.setOnClickListener {
            pretty = !pretty
            binding.btnJsonFormat.text = if (pretty) "Pretty JSON" else "Compact JSON"
            render()
        }
        binding.btnCopyJson.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Raw health data", binding.tvRawJson.text))
            Toast.makeText(this, "JSON copied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun render() {
        if (!::binding.isInitialized) return
        val version = ++renderVersion
        val category = binding.spJsonCategory.selectedItem?.toString() ?: "All data"
        binding.btnCopyJson.isEnabled = false
        binding.tvRawJson.text = "Building $category JSON…"
        lifecycleScope.launch {
            val json = withContext(Dispatchers.Default) { RawJsonStore.json(category, pretty) }
            if (version == renderVersion) {
                binding.tvRawJson.text = json
                binding.btnCopyJson.isEnabled = true
            }
        }
    }
}
