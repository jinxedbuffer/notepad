package com.jinxedbuffer.notepad

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.color.DynamicColors

class MainActivity : AppCompatActivity() {

    private var currentlyOpenedFileUri: Uri? = null
    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                currentlyOpenedFileUri = it
                val text = readFromUri(it)
                findViewById<EditText>(R.id.textfield).setText(text)
                Toast.makeText(this, "Opened ${getFileNameFromUri(this, it)}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setSupportActionBar(findViewById(R.id.toolbar))

        // start cursor blink
        val editText = findViewById<EditText>(R.id.textfield)
        editText.requestFocus()

        // get intent that started this activity
        val i = intent
        if (Intent.ACTION_VIEW == i.action) {
            val uri = i.data
            uri?.let {
                editText.setText(readFromUri(uri))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_open_file -> {
                openFile()
                true
            }
            R.id.action_save_file -> {
                if (currentlyOpenedFileUri == null) {
                    saveAsFile()
                } else {
                    saveFile(currentlyOpenedFileUri!!)
                }
                true
            }
            R.id.action_save_as -> {
                saveAsFile()
                true
            }
            R.id.action_settings -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openFile() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        getContent.launch(i)
    }

    private fun saveFile(uri: Uri) {
        grantUriPermission("com.jinxedbuffer.notepad", uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        contentResolver.openOutputStream(uri).use { outputStream ->
            val text = findViewById<EditText>(R.id.textfield).text.toString()
            outputStream?.write(text.toByteArray())
        }
        Toast.makeText(this, "Saved ${getFileNameFromUri(this, uri)}", Toast.LENGTH_SHORT).show()
    }

    private fun saveAsFile() {
    }

    private fun readFromUri(uri: Uri?): String? {
        uri?.let {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                return inputStream.bufferedReader().use { it.readText() }
            }
        }
        return null
    }

    @SuppressLint("Range")
    private fun getFileNameFromUri(context: Context, uri: Uri?): String? {
        val cursor = uri?.let {
            context.contentResolver.query(it, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        }
        return cursor?.use {
            it.moveToFirst()
            it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        }
    }

    private fun getStoragePermission() {}
}