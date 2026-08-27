package com.gimo.remotewebmanager

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gimo.remotewebmanager.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity: AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private val dao by lazy { AppDb.get(this).deviceDao() }
    private lateinit var adapter: DeviceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b=ActivityMainBinding.inflate(layoutInflater); setContentView(b.root)
        adapter=DeviceAdapter(::openDevice, ::deviceMenu)
        b.list.layoutManager=LinearLayoutManager(this); b.list.adapter=adapter
        b.addButton.setOnClickListener { startActivity(Intent(this, EditDeviceActivity::class.java)) }
        lifecycleScope.launch { dao.observeAll().collect { list ->
            adapter.submit(list)
            b.emptyText.visibility=if(list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            b.list.visibility=if(list.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
        } }
    }
    private fun openDevice(d: Device) {
        lifecycleScope.launch { dao.touch(d.id, System.currentTimeMillis()) }
        getSharedPreferences("prefs", MODE_PRIVATE).edit().putLong("last_device", d.id).apply()
        startActivity(Intent(this, WebActivity::class.java).putExtra("device_id", d.id))
    }
    private fun deviceMenu(d: Device) {
        AlertDialog.Builder(this).setTitle(d.name).setItems(arrayOf("打开","编辑","删除")) { _, which ->
            when(which){
                0->openDevice(d)
                1->startActivity(Intent(this, EditDeviceActivity::class.java).putExtra("device_id", d.id))
                2->AlertDialog.Builder(this).setMessage("删除 ${d.name}？").setPositiveButton("删除") {_,_-> lifecycleScope.launch{dao.delete(d)}}.setNegativeButton("取消",null).show()
            }
        }.show()
    }
}
