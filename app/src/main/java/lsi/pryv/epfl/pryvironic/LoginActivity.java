package lsi.pryv.epfl.pryvironic;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import com.pryv.Pryv;
import com.pryv.api.model.Permission;
import com.pryv.auth.AuthController;
import com.pryv.auth.AuthControllerImpl;
import com.pryv.auth.AuthView;

import java.util.ArrayList;

public class LoginActivity extends AppCompatActivity {

    private String webViewUrl;
    private String message;
    private WebView webView;

    private Permission creatorPermission = new Permission("*", Permission.Level.manage, "Creator");
    private ArrayList<Permission> permissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        webView = (WebView) findViewById(R.id.webview);
        Pryv.setDomain(AccountManager.DOMAIN);
        permissions = new ArrayList<>();
        permissions.add(creatorPermission);
        new SigninAsync().execute();
    }

    private class SigninAsync extends AsyncTask<Void, Void, Void> {

        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(LoginActivity.this);
            progressDialog.setMessage("Please wait...");
            progressDialog.show();
            progressDialog.setCancelable(false);
        }

        @Override
        protected Void doInBackground(Void... arg0) {

            AuthController authenticator = new AuthControllerImpl(AccountManager.APPID, permissions, null, null, new CustomAuthView());
            authenticator.signIn();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if(webViewUrl!=null) {
                progressDialog.dismiss();
                webView.requestFocus(View.FOCUS_DOWN);
                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setUseWideViewPort(true);
                webView.loadUrl(webViewUrl);
            } else {
                progressDialog.setMessage(message);
                progressDialog.setCancelable(true);
            }
        }
    }

    private class CustomAuthView implements AuthView {

        @Override
        public void displayLoginView(String loginURL) {
            webViewUrl = loginURL;
        }

        @Override
        public void onAuthSuccess(String username, String token) {
            AccountManager.setCreditentials(username, token);
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
        }

        @Override
        public void onAuthError(String msg) {
            message = msg;
        }

        @Override
        public void onAuthRefused(int reasonId, String msg, String detail) {
            message = msg;
        }

    }

}