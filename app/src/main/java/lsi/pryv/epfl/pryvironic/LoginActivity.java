package lsi.pryv.epfl.pryvironic;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class LoginActivity extends AppCompatActivity {
    private EditText passwordField;
    private EditText userField;

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
        String user = userField.getText().toString();
        String password = passwordField.getText().toString();
    }
}