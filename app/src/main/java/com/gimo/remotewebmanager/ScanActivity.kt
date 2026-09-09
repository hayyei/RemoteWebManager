package com.gimo.remotewebmanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.gimo.remotewebmanager.databinding.ActivityScanBinding
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.google.zxing.BarcodeFormat

/** 自定义扫码页：竖屏锁定、右上角退出，识别到二维码把结果带回给调用方。 */
class ScanActivity: AppCompatActivity() {
    private lateinit var b: ActivityScanBinding

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) start() else { Toast.makeText(this, "需要相机权限才能扫码", Toast.LENGTH_SHORT).show(); finish() }
    }
    private val callback = object: BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {
            val text = result?.text ?: return
            setResult(RESULT_OK, Intent().putExtra("scan_result", text))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityScanBinding.inflate(layoutInflater); setContentView(b.root)
        b.close.setOnClickListener { finish() }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) start()
        else permLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun start() {
        b.barcodeScanner.barcodeView.decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
        b.barcodeScanner.setStatusText("将二维码对准取景框")
        b.barcodeScanner.decodeSingle(callback)
    }

    override fun onResume() { super.onResume(); if (::b.isInitialized) b.barcodeScanner.resume() }
    override fun onPause() { if (::b.isInitialized) b.barcodeScanner.pause(); super.onPause() }
}
