package com.daka.life;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.*;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import java.io.*;
import androidx.webkit.WebViewAssetLoader;

public class MainActivity extends Activity {
 private WebView web;
 private ValueCallback<Uri[]> chooser;
 private String pendingData;
 private String pendingMime;
 @Override public void onCreate(Bundle b) {
  super.onCreate(b);
  getWindow().setStatusBarColor(0xff000000);
  getWindow().setNavigationBarColor(0xff000000);
  web=new WebView(this); setContentView(web);
  web.getSettings().setJavaScriptEnabled(true);
  web.getSettings().setDomStorageEnabled(true);
  web.getSettings().setAllowFileAccess(false);
  web.getSettings().setAllowContentAccess(true);
  web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  WebViewAssetLoader loader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
  web.setWebViewClient(new WebViewClient(){
   public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){
    WebResourceResponse local=loader.shouldInterceptRequest(r.getUrl());
    if(local!=null)return local;
    return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));
   }
   public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){ return !r.getUrl().toString().startsWith("https://appassets.androidplatform.net/assets/"); }
  });
  web.setWebChromeClient(new WebChromeClient(){
   public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> callback,FileChooserParams params){
    if(chooser!=null)chooser.onReceiveValue(null); chooser=callback;
    try { startActivityForResult(params.createIntent(),20); }catch(Exception e){ chooser.onReceiveValue(null); chooser=null; }
    return true;
   }
  });
  web.addJavascriptInterface(new Object(){
   @JavascriptInterface public void save(String name,String mime,String base64){
    if(base64.length()>24000000)return;
    runOnUiThread(()->{ pendingData=base64; pendingMime=mime;
     Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType(mime); i.putExtra(Intent.EXTRA_TITLE,name);
     try{startActivityForResult(i,21);}catch(Exception e){pendingData=null;}
    });
   }
  },"DakaNative");
  web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
 }
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(request==20&&chooser!=null){chooser.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));chooser=null;}
  if(request==21&&pendingData!=null){
   if(result==RESULT_OK&&data!=null&&data.getData()!=null){
    try(OutputStream out=getContentResolver().openOutputStream(data.getData())){out.write(Base64.decode(pendingData,Base64.DEFAULT));web.evaluateJavascript("toast('文件已保存')",null);}
    catch(Exception e){web.evaluateJavascript("toast('保存失败，请重试')",null);}
   }
   pendingData=null;
  }
 }
 @Override public void onBackPressed(){web.evaluateJavascript("closeModal()",null);}
 @Override protected void onDestroy(){if(chooser!=null)chooser.onReceiveValue(null);web.destroy();super.onDestroy();}
}