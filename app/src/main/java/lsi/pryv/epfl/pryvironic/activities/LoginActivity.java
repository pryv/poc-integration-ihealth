package lsi.pryv.epfl.pryvironic.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.webkit.WebView;

import com.pryv.Pryv;
import com.pryv.api.model.Permission;
import com.pryv.auth.AuthController;
import com.pryv.auth.AuthControllerImpl;
import com.pryv.auth.AuthView;

import java.util.ArrayList;

import lsi.pryv.epfl.pryvironic.R;

public class LoginActivity extends AppCompatActivity {

    private String webViewUrl;
    private String errorMessage = "Unknown error";
    private WebView webView;

    private Permission creatorPermission = new Permission("*", Permission.Level.manage, "Creator");
    private ArrayList<Permission> permissions;

    private final static String CREDITENTIALS = "creditentials";
    private final static String USERNAME = "username";
    private final static String TOKEN = "token";
    private static SharedPreferences preferences;

    public final static String DOMAIN = "pryv-switch.ch";
    public final static String APPID = "app-android-iHealth";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        preferences = getSharedPreferences(CREDITENTIALS, MODE_PRIVATE);

        webView = (WebView) findViewById(R.id.webview);
        Pryv.setDomain(DOMAIN);
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
            AuthController authenticator = new AuthControllerImpl(APPID, permissions, null, null, new CustomAuthView());
            authenticator.signIn();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            progressDialog.dismiss();
            if (webViewUrl != null) {
                webView.requestFocus(View.FOCUS_DOWN);
                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setUseWideViewPort(true);
                webView.loadUrl(webViewUrl);
            } else {
                showError();
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
            setCreditentials(username, token);
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
        }

        @Override
        public void onAuthError(String msg) {
            errorMessage = msg;
        }

        @Override
        public void onAuthRefused(int reasonId, String msg, String detail) {
            errorMessage = msg;
        }
    }

    public void showError() {
        new AlertDialog.Builder(this)
                .setTitle("Authentification error: ")
                .setMessage(errorMessage)
                .setPositiveButton("Try again", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        startActivity(getIntent());
                    }
                })
                .show();
    }

    private void setCreditentials(String username, String token) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(encrypt(USERNAME), encrypt(username));
        editor.putString(encrypt(TOKEN), encrypt(token));
        editor.apply();
    }

    public static void resetCreditentials() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(encrypt(USERNAME));
        editor.remove(encrypt(TOKEN));
        editor.apply();
    }

    public static String getUsername() {
        return preferences.getString(decrypt(USERNAME), null);
    }

    public static String getToken() {
        return preferences.getString(decrypt(TOKEN), null);
    }

    public static String encrypt(String input) {
        return Base64.encodeToString(input.getBytes(), Base64.DEFAULT);
    }

    public static String decrypt(String input) {
        return new String(Base64.decode(input, Base64.DEFAULT));
    }

}