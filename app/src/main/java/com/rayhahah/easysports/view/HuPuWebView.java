package com.rayhahah.easysports.view;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.rayhahah.easysports.app.C;
import com.rayhahah.easysports.common.RWebActivity;
import com.rayhahah.easysports.module.forum.business.forumdetail.ForumDetailActivity;
import com.rayhahah.easysports.utils.HuPuHelper;
import com.rayhahah.easysports.utils.SettingPrefUtils;
import com.rayhahah.rbase.utils.base.StringUtils;
import com.rayhahah.rbase.utils.base.ToastUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 虎扑帖子详情页
 */
public class HuPuWebView extends WebView {

    private String basicUA;
    private Map<String, String> header;

    HuPuHelper mRequestHelper;
    private Activity mPreActivity;

    public HuPuWebView(Context context) {
        this(context, null);
    }

    public HuPuWebView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HuPuWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setActivity(Activity preActivity) {
        mPreActivity = preActivity;
    }

    private void init() {
        WebSettings settings = getSettings();
        settings.setBuiltInZoomControls(false);
        settings.setSupportZoom(false);
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUseWideViewPort(true);
        if (Build.VERSION.SDK_INT > 6) {
            settings.setAppCacheEnabled(true);
            settings.setLoadWithOverviewMode(true);
        }
        String path = getContext().getFilesDir().getPath();
        settings.setGeolocationEnabled(true);
        settings.setGeolocationDatabasePath(path);
        this.basicUA = settings.getUserAgentString() + " kanqiu/7.05.6303/7059";
        setBackgroundColor(0);
        initWebViewClient();
        setWebChromeClient(new HuPuChromeClient());
        try {
            if (SettingPrefUtils.isLogin()) {
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.setCookie("http://bbs.mobileapi.hupu.com", "u=" + SettingPrefUtils.getCookies());
                cookieManager.setCookie("http://bbs.mobileapi.hupu.com", "_gamesu=" + URLEncoder.encode(SettingPrefUtils.getToken(), "utf-8"));
                cookieManager.setCookie("http://bbs.mobileapi.hupu.com", "_inKanqiuApp=1");
                cookieManager.setCookie("http://bbs.mobileapi.hupu.com", "_kanqiu=1");
                CookieSyncManager.getInstance().sync();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initWebViewClient() {
        CookieManager.getInstance().setAcceptCookie(true);
        setWebViewClient(new HupuWebClient());
    }


    public void setCallBack(HuPuWebViewCallBack callBack) {
        this.callBack = callBack;
    }

    public class HuPuChromeClient extends WebChromeClient {
        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            return true;
        }
    }

    private class HupuWebClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) { // 超链接监听
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();
            if (scheme != null) {
                handleScheme(scheme, url);
            }
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (callBack != null) {
                callBack.onFinish();
            }
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (callBack != null) {
                callBack.onError();
            }
        }
    }

    /**
     * 解析网页超链接
     *
     * @param scheme
     * @param url
     */
    private void handleScheme(String scheme, String url) {
        if (scheme != null) {
            if ("kanqiu".equalsIgnoreCase(scheme)) {
                handleKanQiu(url);
            } else if ("browser".equalsIgnoreCase(scheme)
                    || "http".equalsIgnoreCase(scheme)
                    || "https".equalsIgnoreCase(scheme)) {
                handleUrl(url);
            } else if ("hupu".equalsIgnoreCase(scheme)) {
                try {
                    JSONObject object = new JSONObject(Uri.decode(url.substring("hupu".length() + 3)));
                    String method = object.optString("method");
                    String successcb = object.optString("successcb");
                    handleHuPu(method, object.getJSONObject("data"), successcb);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleKanQiu(String url) {
        if (url.contains("topic")) {
            Uri uri = Uri.parse(url);
            String tid = uri.getLastPathSegment();
            String page = uri.getQueryParameter("page");
            String pid = uri.getQueryParameter("pid");
            ForumDetailActivity.start(getContext(), pid, tid,
                    TextUtils.isEmpty(page) ? 1 : Integer.valueOf(page), "");
        } else if (url.contains("board")) {
            ToastUtils.showShort("还未开启功能");
//            String boardId = url.substring(url.lastIndexOf("/") + 1);
//            ForumDetailListActivity.start(getContext(), boardId);
        } else if (url.contains("people")) {
            String uid = url.substring(url.lastIndexOf("/") + 1);
            // TODO UserProfileActivity.startActivity(getContext(), uid);
        }
    }

    /**
     * 跳转
     *
     * @param url
     */
    private void handleUrl(String url) {
        RWebActivity.start(getContext(), mPreActivity, url, "", true, true);
    }

    private void handleHuPu(String method, JSONObject data, String successcb) throws Exception {
        switch (method) {
            case "bridgeReady":
                JSONObject jSONObject = new JSONObject();
                try {
                    jSONObject.put("hybridVer", "1.0");
                    jSONObject.put("supportAjax", true);
                    jSONObject.put("appVer", "7.0.5.6303");
                    jSONObject.put("appName", "com.hupu.games");
                    jSONObject.put("lowDevice", false);
                    jSONObject.put("scheme", "hupu");
                    jSONObject.put("did", C.DEVICE_ID);
                    jSONObject.put("platform", "Android");
                    jSONObject.put("device", Build.PRODUCT);
                    jSONObject.put("osVer", Build.VERSION.RELEASE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String js = "javascript:HupuBridge._handle_('"
                        + successcb
                        + "','"
                        + jSONObject.toString()
                        + "','null','null');";
                loadUrl(js);
                break;
            case "hupu.ui.updatebbspager":
                int page = data.getInt("page");
                int total = data.getInt("total");
                if (callBack != null) {
                    callBack.onUpdatePager(page, total);
                }
                break;
            case "hupu.ui.bbsreply":
                // TODO: 2017/9/26 发帖功能还没完善

                boolean open = data.getBoolean("open");
                JSONObject extra = data.getJSONObject("extra");
                String tid = extra.getString("tid");
                long pid = extra.getLong("pid");
                String userName = extra.getString("username");
                String content = extra.getString("content");
                if (open) {
                    ToastUtils.showShort("发帖功能还没开发");
//                    PostActivity.start(getContext(), content, C.TYPE_REPLY, "", tid, String.valueOf(pid));
                }
                break;
            case "hupu.album.view":
                // TODO: 2017/9/26  查看大图模式 还没做
                ToastUtils.showShort("查看大图模式 还没做");

                int index = data.getInt("index");
                JSONArray images = data.getJSONArray("images");
                ArrayList<String> extraPics = new ArrayList<>();
                for (int i = 0; i < images.length(); i++) {
                    JSONObject image = images.getJSONObject(i);
                    extraPics.add(image.getString("url"));
                }
//                Intent intent = new Intent(getContext(), ImagePreViewActivity.class);
//                intent.putExtra(ImagePreViewActivity.INTENT_URLS, extraPics);
//                intent.putExtra(ImagePreViewActivity.INTENT_URL, extraPics.get(index));
//                getContext().startActivity(intent);
                break;
            case "hupu.ui.copy":
                String content1 = data.getString("content");
                StringUtils.copy(getContext(), content1);
                ToastUtils.showShort("复制成功");
                break;
            case "hupu.ui.report":
                // TODO: 2017/9/26 举报功能还没做
                ToastUtils.showShort("举报模块尚未完成");
                JSONObject reportExtra = data.getJSONObject("extra");
                String reportTid = reportExtra.getString("tid");
                long reportPid = reportExtra.getLong("pid");
//                ReportActivity.start(getContext(), String.valueOf(reportPid), reportTid);
                break;
            case "hupu.user.login":

                // TODO: 2017/10/11 虎扑账号登陆功能
//                getContext().startActivity(new Intent(getContext(), LoginActivity.class));
                ToastUtils.showShort("登陆功能还没做");
                break;
            case "hupu.ui.pageclose":
                mPreActivity.finish();
//                BaseAppManager.getInstance().getForwardActivity().finish();
                break;
            default:
                break;
        }
    }

    private void setUA(int i) {
        if (this.basicUA != null) {
            getSettings().setUserAgentString(this.basicUA + " isp/" + i + " network/" + i);
        }
    }

    @Override
    public void loadUrl(String url) {
        setUA(-1);
        if (header == null) {
            header = new HashMap<>();
            header.put("Accept-Encoding", "gzip");
        }
        super.loadUrl(url, header);
    }

    private HuPuWebViewCallBack callBack;

    public interface HuPuWebViewCallBack {

        void onFinish();

        void onUpdatePager(int page, int total);

        void onError();
    }

    private OnScrollChangedCallback mOnScrollChangedCallback;

    @Override
    protected void onScrollChanged(final int l, final int t, final int oldl, final int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        if (mOnScrollChangedCallback != null) {
            mOnScrollChangedCallback.onScroll(l - oldl, t - oldt, t, oldt);
        }
    }

    public OnScrollChangedCallback getOnScrollChangedCallback() {
        return mOnScrollChangedCallback;
    }

    public void setOnScrollChangedCallback(final OnScrollChangedCallback onScrollChangedCallback) {
        mOnScrollChangedCallback = onScrollChangedCallback;
    }

    /**
     * Impliment in the activity/fragment/view that you want to listen to the webview
     */
    public interface OnScrollChangedCallback {
        void onScroll(int dx, int dy, int y, int oldy);
    }
}
