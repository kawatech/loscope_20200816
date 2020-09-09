package com.losteaka.no001.oscilloscopeapp


import android.app.Activity
import android.content.Intent
import android.net.Uri
//import androidx.appcompat.app.AppCompatActivity

import android.os.Bundle
import android.provider.DocumentsContract
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.*                   // coroutinesが赤字になる。

import java.io.FileOutputStream

class DeleteActivity : AppCompatActivity() {
    private val TAG = "DeleteExampleActivity"
    private val READ_REQUEST_CODE: Int = 42

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)

        openFile()
    }

    private fun openFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            //   type = "image/*"
            type = "*/*"                // kawa すべてのファイル
        }

        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                Log.i(TAG, "Uri: $uri")
                deleteFile(uri)
            }
        }
    }

    private fun deleteFile(uri: Uri) {
        DocumentsContract.deleteDocument(contentResolver, uri)

        val filePass = "$uri"
        // 後ろから21文字を切り出す
        val str1 = filePass
        val str2 = str1.substring(str1.length - 21)
        
        Toast.makeText(applicationContext, "Done deleting : " + str2, Toast.LENGTH_LONG).show()
    }

}