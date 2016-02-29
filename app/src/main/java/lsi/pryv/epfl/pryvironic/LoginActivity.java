package lsi.pryv.epfl.pryvironic;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

import com.pryv.Pryv;
import com.pryv.api.model.Permission;
import com.pryv.auth.AuthController;
import com.pryv.auth.AuthControllerImpl;
import com.pryv.auth.AuthView;

import java.net.URL;
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

    private String webViewUrl;
    private String message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userField = (EditText) findViewById(R.id.user_field);
        passwordField = (EditText) findViewById(R.id.password_field);
    }

    public void login(View view) {
        String user = userField.getText().toString();
        String password = passwordField.getText().toString();
    }

    public void signin(View view) {
        permissions.add(testPermission1);
        permissions.add(testPermission2);
        Pryv.setDomain("pryv-switch.ch");
        new SigninAsync().execute();
    }

    private class SigninAsync extends AsyncTask<Void, Void, Void> {

        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(LoginActivity.this);
            progressDialog.setMessage("Signin...");
            progressDialog.show();
            progressDialog.setCancelable(false);
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            AuthController authenticator = new AuthControllerImpl(reqAppId, permissions, lang, returnURL, new CustomAuthView());
            authenticator.signIn();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if(webViewUrl!=null) {
                progressDialog.dismiss();
                AlertDialog.Builder alert = new AlertDialog.Builder(LoginActivity.this);
                WebView webView = new WebView(LoginActivity.this);
                webView.loadUrl(webViewUrl);
                webView.requestFocus(View.FOCUS_DOWN);
                webView.setOnTouchListener(new View.OnTouchListener()
                {
                    @Override
                    public boolean onTouch(View v, MotionEvent event)
                    {
                        switch (event.getAction())
                        {
                            case MotionEvent.ACTION_DOWN:
                            case MotionEvent.ACTION_UP:
                                if (!v.hasFocus())
                                {
                                    v.requestFocus();
                                }
                                break;
                        }
                        return false;
                    }
                });
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
            } else {
                progressDialog.setMessage(message);
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
            CreditentialsManager.setCreditentials(username, token);
            message = "You can now login!";
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