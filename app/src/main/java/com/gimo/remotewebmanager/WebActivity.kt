package com.gimo.remotewebmanager

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.webkit.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gimo.remotewebmanager.databinding.ActivityWebBinding
import kotlinx.coroutines.launch

class WebActivity: AppCompatActivity() {
    private lateinit var b: ActivityWebBinding
    private var fileCallback: ValueCallback<Array<Uri>>?=null
    private val PICK=9001
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); b=ActivityWebBinding.inflate(layoutInflater); setContentView(b.root)
        SystemBars.apply(b.root)
        val id=intent.getLongExtra("device_id",0)
        val w=b.webView
        CookieManager.getInstance().setAcceptCookie(true); CookieManager.getInstance().setAcceptThirdPartyCookies(w,true)
        with(w.settings){ javaScriptEnabled=true; domStorageEnabled=true; databaseEnabled=true; allowFileAccess=true; mediaPlaybackRequiresUserGesture=false; mixedContentMode=WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE; setSupportZoom(true); builtInZoomControls=true; displayZoomControls=false }
        w.webViewClient=object:WebViewClient(){ override fun shouldOverrideUrlLoading(view:WebView, request:WebResourceRequest):Boolean=false }
        w.webChromeClient=object:WebChromeClient(){
            override fun onProgressChanged(view:WebView?, newProgress:Int){ b.progress.progress=newProgress; b.progress.visibility=if(newProgress>=100) View.GONE else View.VISIBLE }
            override fun onShowFileChooser(webView:WebView?, cb:ValueCallback<Array<Uri>>?, params:FileChooserParams?):Boolean{
                fileCallback?.onReceiveValue(null); fileCallback=cb
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{ addCategory(Intent.CATEGORY_OPENABLE); type="*/*"; putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true) },PICK); return true
            }
        }
        lifecycleScope.launch { AppDb.get(this@WebActivity).deviceDao().get(id)?.let { d -> b.title.text=d.name; if(savedInstanceState==null) w.loadUrl(d.url) } }
        if(savedInstanceState!=null) w.restoreState(savedInstanceState)
        b.reload.setOnClickListener{w.reload()}
        onBackPressedDispatcher.addCallback(this, object:OnBackPressedCallback(true){ override fun handleOnBackPressed(){ if(w.canGoBack()) w.goBack() else finish() } })
    }
    override fun onSaveInstanceState(outState:Bundle){ b.webView.saveState(outState); super.onSaveInstanceState(outState) }
    @Deprecated("Deprecated in Android") override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){ super.onActivityResult(requestCode,resultCode,data); if(requestCode==PICK){ val out=if(resultCode== Activity.RESULT_OK){ data?.clipData?.let{ c->Array(c.itemCount){i->c.getItemAt(i).uri} } ?: data?.data?.let{arrayOf(it)} } else null; fileCallback?.onReceiveValue(out); fileCallback=null } }
    override fun onPause(){ CookieManager.getInstance().flush(); super.onPause() }
}
