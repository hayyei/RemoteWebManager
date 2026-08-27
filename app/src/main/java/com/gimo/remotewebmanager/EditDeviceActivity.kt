package com.gimo.remotewebmanager

import android.os.Bundle
import android.webkit.URLUtil
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gimo.remotewebmanager.databinding.ActivityEditDeviceBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import java.net.URI

class EditDeviceActivity: AppCompatActivity() {
    private lateinit var b: ActivityEditDeviceBinding
    private val dao by lazy { AppDb.get(this).deviceDao() }
    private var editing: Device?=null
    private val scanner=registerForActivityResult(ScanContract()){ result ->
        result.contents?.let { b.urlInput.setText(it); if(b.nameInput.text.isBlank()) b.nameInput.setText(guessName(it)) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); b=ActivityEditDeviceBinding.inflate(layoutInflater); setContentView(b.root)
        val id=intent.getLongExtra("device_id",0)
        if(id>0) lifecycleScope.launch { editing=dao.get(id); editing?.let { b.nameInput.setText(it.name); b.urlInput.setText(it.url) } }
        b.scanButton.setOnClickListener { scanner.launch(ScanOptions().setPrompt("扫描远程链接二维码").setBeepEnabled(false).setOrientationLocked(false)) }
        b.saveButton.setOnClickListener { save() }
    }
    private fun save(){
        val url=b.urlInput.text.toString().trim()
        if(!URLUtil.isNetworkUrl(url)){ Toast.makeText(this,"请输入有效的 http/https 链接",Toast.LENGTH_SHORT).show(); return }
        val name=b.nameInput.text.toString().trim().ifBlank{guessName(url)}
        lifecycleScope.launch {
            val old=editing
            if(old==null) dao.insert(Device(name=name,url=url)) else dao.update(old.copy(name=name,url=url))
            finish()
        }
    }
    private fun guessName(url:String):String = try {
        val u=URI(url); val q=u.rawQuery.orEmpty().split("&").mapNotNull { p-> val i=p.indexOf('='); if(i>0) p.substring(0,i) to java.net.URLDecoder.decode(p.substring(i+1),"UTF-8") else null }.toMap()
        q["name"]?.takeIf{it.isNotBlank()} ?: u.host ?: "远程设备"
    } catch(_:Exception){"远程设备"}
}
