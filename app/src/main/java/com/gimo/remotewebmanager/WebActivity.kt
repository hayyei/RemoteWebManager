package com.gimo.remotewebmanager

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gimo.remotewebmanager.databinding.ActivityWebBinding
import kotlinx.coroutines.launch

class WebActivity: AppCompatActivity() {
    private lateinit var b: ActivityWebBinding
    private var fileCallback: ValueCallback<Array<Uri>>?=null
    private val PICK=9001
    private var popup: WebView?=null
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); b=ActivityWebBinding.inflate(layoutInflater); setContentView(b.root)
        SystemBars.apply(b.root)
        val id=intent.getLongExtra("device_id",0)
        val w=b.webView
        CookieManager.getInstance().setAcceptCookie(true); CookieManager.getInstance().setAcceptThirdPartyCookies(w,true)
        with(w.settings){ javaScriptEnabled=true; domStorageEnabled=true; databaseEnabled=true; allowFileAccess=true; mediaPlaybackRequiresUserGesture=false; mixedContentMode=WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE; setSupportZoom(true); builtInZoomControls=true; displayZoomControls=false; setSupportMultipleWindows(true); javaScriptCanOpenWindowsAutomatically=true }
        w.webViewClient=object:WebViewClient(){ override fun shouldOverrideUrlLoading(view:WebView, request:WebResourceRequest):Boolean=false }
        w.webChromeClient=object:WebChromeClient(){
            override fun onProgressChanged(view:WebView?, newProgress:Int){ b.progress.progress=newProgress; b.progress.visibility=if(newProgress>=100) View.GONE else View.VISIBLE }
            override fun onShowFileChooser(webView:WebView?, cb:ValueCallback<Array<Uri>>?, params:FileChooserParams?):Boolean{
                fileCallback?.onReceiveValue(null); fileCallback=cb
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{ addCategory(Intent.CATEGORY_OPENABLE); type="*/*"; putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true) },PICK); return true
            }
            override fun onCreateWindow(view:WebView, isDialog:Boolean, isUserGesture:Boolean, resultMsg:Message):Boolean{ openPopup(view, resultMsg); return true }
        }
        lifecycleScope.launch { AppDb.get(this@WebActivity).deviceDao().get(id)?.let { d -> b.title.text=d.name; if(savedInstanceState==null) w.loadUrl(d.url) } }
        if(savedInstanceState!=null) w.restoreState(savedInstanceState)
        b.reload.setOnClickListener{w.reload()}
        b.backBtn.setOnClickListener{ onBackPressedDispatcher.onBackPressed() }
        b.popupClose.setOnClickListener{ closePopupIfShown() }
        onBackPressedDispatcher.addCallback(this, object:OnBackPressedCallback(true){ override fun handleOnBackPressed(){ if(closePopupIfShown()) return; if(w.canGoBack()) w.goBack() else finish() } })
    }
    override fun onSaveInstanceState(outState:Bundle){ b.webView.saveState(outState); super.onSaveInstanceState(outState) }

    /** 新窗口（window.open / target=_blank）：用覆盖层承载真实弹窗 WebView，保留 opener 关系，登录回调类页面才能正常工作。 */
    @SuppressLint("SetJavaScriptEnabled")
    private fun openPopup(host:WebView, resultMsg:Message) {
        closePopupIfShown()
        val pw=WebView(host.context)
        with(pw.settings){ javaScriptEnabled=true; domStorageEnabled=true; databaseEnabled=true; mediaPlaybackRequiresUserGesture=false; mixedContentMode=WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE; setSupportZoom(true); builtInZoomControls=true; displayZoomControls=false }
        CookieManager.getInstance().setAcceptThirdPartyCookies(pw,true)
        pw.webViewClient=object:WebViewClient(){ override fun shouldOverrideUrlLoading(view:WebView, request:WebResourceRequest):Boolean=false }
        pw.webChromeClient=object:WebChromeClient(){ override fun onCloseWindow(window:WebView?){ runOnUiThread { closePopupIfShown() } } }
        (resultMsg.obj as WebView.WebViewTransport).webView=pw
        resultMsg.sendToTarget()
        popup=pw
        b.popupSlot.addView(pw, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        b.popupLayer.visibility=View.VISIBLE
    }

    /** @return 是否关闭了弹窗（供返回键消费） */
    private fun closePopupIfShown():Boolean {
        if(b.popupLayer.visibility!=View.VISIBLE) return false
        b.popupLayer.visibility=View.GONE
        b.popupSlot.removeAllViews()
        popup?.apply { stopLoading(); destroy() }
        popup=null
        return true
    }

    override fun onResume(){ super.onResume(); popup?.onResume() }
    override fun onPause(){ CookieManager.getInstance().flush(); popup?.onPause(); super.onPause() }
    override fun onDestroy(){ closePopupIfShown(); super.onDestroy() }
    @Deprecated("Deprecated in Android") override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){ super.onActivityResult(requestCode,resultCode,data); if(requestCode==PICK){ val out=if(resultCode== Activity.RESULT_OK){ data?.clipData?.let{ c->Array(c.itemCount){i->c.getItemAt(i).uri} } ?: data?.data?.let{arrayOf(it)} } else null; fileCallback?.onReceiveValue(out); fileCallback=null } }
}
