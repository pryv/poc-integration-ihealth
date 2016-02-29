package lsi.pryv.epfl.pryvironic;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import com.pryv.Pryv;
import com.pryv.api.model.Permission;
import com.pryv.auth.AuthController;
import com.pryv.auth.AuthControllerImpl;
import com.pryv.auth.AuthView;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private EditText passwordField;
    private EditText userField;

    private String user = "";
    private String tk = "";

    private String reqAppId = "epfl-lsi-ironic";
    private List<Permission> permissions = new ArrayList<Permission>();
    private String streamId1 = "pics";
    private Permission.Level perm1 = Permission.Level.contribute;
    private String defaultName1 = "ddd";
    private Permission testPermission1 = new Permission(streamId1, perm1, defaultName1);
    private String streamId2 = "vids";
    private Permission.Level perm2 = Permission.Level.read;
    private String defaultName2 = "eee";
    private Permission testPermission2 = new Permission(streamId2, perm2, defaultName2);
    private String lang = "en";
    private String returnURL = "fakeURL";

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userField = (EditText) findViewById(R.id.user_field);
        passwordField = (EditText) findViewById(R.id.password_field);

        progressDialog = new ProgressDialog(LoginActivity.this);

    }

    public void login(View view) {
        String user = userField.getText().toString();
        String password = passwordField.getText().toString();
    }

    public void signin(View view) {
        permissions.add(testPermission1);
        permissions.add(testPermission2);
        new SigninAsync().execute();
    }

    private class SigninAsync extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            AuthController authenticator = new AuthControllerImpl(reqAppId, permissions, lang, returnURL, new CustomAuthView());
            authenticator.signIn();
            return null;
        }

        @Override
        protected void onPreExecute(){
            progressDialog.setMessage("Signing in...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

    }

    private class CustomAuthView implements AuthView {

        @Override
        public void displayLoginView(String loginURL) {
            progressDialog.dismiss();
            AlertDialog.Builder alert = new AlertDialog.Builder(LoginActivity.this);
            WebView webView = new WebView(LoginActivity.this);
            webView.loadUrl(loginURL);
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.loadUrl(url);
                    return true;
                }
            });
            alert.setView(webView);
            alert.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            alert.show();
        }

        @Override
        public void onAuthSuccess(String username, String token) {
            CreditentialsManager.setCreditentials(username, token);
            progressDialog.setMessage("Creditentials created, you can now login!");
            progressDialog.setCancelable(true);
        }

        @Override
        public void onAuthError(String msg) {
            progressDialog.setMessage(msg);
            progressDialog.setCancelable(true);
        }

        @Override
        public void onAuthRefused(int reasonId, String message, String detail) {
            progressDialog.setMessage(message);
            progressDialog.setCancelable(true);
        }

    }

}