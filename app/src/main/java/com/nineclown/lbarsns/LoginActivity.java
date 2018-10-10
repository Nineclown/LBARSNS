package com.nineclown.lbarsns;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "t";
    private FirebaseAuth mAuth;
    private EditText mEmailField;
    private EditText mPasswordField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Views
        mEmailField = (EditText) findViewById(R.id.field_email);
        mPasswordField = (EditText) findViewById(R.id.field_password);

        //Buttons
        findViewById(R.id.signin_button).setOnClickListener(this);
        findViewById(R.id.signup_button).setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();

    }

    private void createUserWithEmail(String email, String password) {
        Log.d(TAG, "createUserWithEmail:success");
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            moveMainPage(mAuth.getCurrentUser());
                            Toast.makeText(LoginActivity.this, "회원 가입이 완료되었습니다.", Toast.LENGTH_SHORT).show();


                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(LoginActivity.this, task.getException().toString(), Toast.LENGTH_SHORT).show();
                            //Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            //Toast.makeText(EmailPasswordActivity.this, "Authentication failed.",
                            //        Toast.LENGTH_SHORT).show();
                            //updateUI(null);
                        }

                        // ...
                    }
                });
    }

    private void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            moveMainPage(mAuth.getCurrentUser());
                            Toast.makeText(LoginActivity.this, "로그인 되었습니다.", Toast.LENGTH_SHORT).show();

                            //
                            //  Log.d(TAG, "signInWithEmail:success");
                            //FirebaseUser user = mAuth.getCurrentUser();
                            //updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(LoginActivity.this, task.getException().toString(), Toast.LENGTH_SHORT).show();
                            //Log.w(TAG, "signInWithEmail:failure", task.getException());
                            //Toast.makeText(EmailPasswordActivity.this, "Authentication failed.",
                            //        Toast.LENGTH_SHORT).show();
                            //updateUI(null);
                        }

                       /* // [START_EXCLUDE]
                        if (!task.isSuccessful()) {
                            mStatusTextView.setText(R.string.auth_failed);
                        }
                        hideProgressDialog();
                        // [END_EXCLUDE]*/
                    }
                });
    }

    private void moveMainPage(FirebaseUser user) {
        if (user != null) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.signin_button) {
            signIn(mEmailField.getText().toString(), mPasswordField.getText().toString());
        } else if (i == R.id.signup_button) {
            createUserWithEmail(mEmailField.getText().toString(), mPasswordField.getText().toString());
        }/* else if (i == R.id.sign_out_button) {
            signOut();
        } else if (i == R.id.verify_email_button) {
            sendEmailVerification();
        }*/
    }
}
