package edu.pitt.coe.tuber;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.READ_CONTACTS;

public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    private static final String TAG = "Login";
    private static final int REQUEST_READ_CONTACTS = 0;

    private FirebaseAuth auth;
    private boolean login;

    private AutoCompleteTextView emailAutoComplete;
    private TextInputLayout emailLayout;
    private TextInputLayout emailConfirmLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout passwordConfirmLayout;
    private ProgressBar loginProgress;
    private View loginForm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup Data
        auth = FirebaseAuth.getInstance();
        login = true;

        // Setup UI
        setContentView(R.layout.activity_login);

        // Text
        emailAutoComplete = findViewById(R.id.email);
        emailLayout = findViewById(R.id.email_layout);
        emailConfirmLayout = findViewById(R.id.email_confirm_layout);
        passwordLayout = findViewById(R.id.password_layout);
        passwordConfirmLayout = findViewById(R.id.password_confirm_layout);

        // Button
        Button submitButton = findViewById(R.id.submit_button);
        Button switchButton = findViewById(R.id.switch_button);

        // Misc
        loginForm = findViewById(R.id.login_form);
        loginProgress = findViewById(R.id.login_progress);

        // Adapters
        populateAutoComplete();

        // Controllers

        passwordLayout.getEditText().setOnEditorActionListener((TextView textView, int id, KeyEvent keyEvent) -> {
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptAuth();
                return true;
            }
            return false;
        });

        passwordConfirmLayout.getEditText().setOnEditorActionListener((TextView textView, int id, KeyEvent keyEvent) -> {
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptAuth();
                return true;
            }
            return false;
        });

        submitButton.setOnClickListener((View view) -> attemptAuth());
        switchButton.setOnClickListener((View view) -> switchForm());
    }

    private void switchForm() {
        login = !login;
        if (login)
        {
            emailConfirmLayout.setVisibility(View.GONE);
            passwordConfirmLayout.setVisibility(View.GONE);
        } else {
            emailConfirmLayout.setVisibility(View.VISIBLE);
            passwordConfirmLayout.setVisibility(View.VISIBLE);
        }
    }

    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        loginForm.setVisibility(show ? View.GONE : View.VISIBLE);
        loginForm.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                loginForm.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        loginProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        loginProgress.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                loginProgress.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void attemptAuth() {
        if (login) {
            attemptLogin();
        } else {
            attemptRegistration();
        }
    }

    private void attemptLogin() {
        if (!isLoginValid()) {
            return;
        }

        showProgress(true);

        String email = emailLayout.getEditText().getText().toString().concat("@pitt.edu");
        String password = passwordLayout.getEditText().getText().toString();

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this,
            (@NonNull Task<AuthResult> task) -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "signInWithEmail:success");
                    FirebaseUser user = auth.getCurrentUser();
                    Toast.makeText(LoginActivity.this, "Welcome " + user.getEmail(), Toast.LENGTH_SHORT).show();
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.getException());
                    Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                }

                //if (!task.isSuccessful()) {
                //    mStatusTextView.setText(R.string.auth_failed);
                //}
                showProgress(false);
            });
    }

    private boolean isLoginValid()
    {
        emailLayout.setError(null);
        passwordLayout.setError(null);

        String email = emailLayout.getEditText().getText().toString();
        String password = passwordLayout.getEditText().getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError(getString(R.string.error_field_required));
            focusView = passwordLayout;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            passwordLayout.setError(getString(R.string.error_invalid_password));
            focusView = passwordLayout;
            cancel = true;
        }

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError(getString(R.string.error_field_required));
            focusView = emailLayout;
            cancel = true;
        } else if (!isEmailValid(email)) {
            emailLayout.setError(getString(R.string.error_invalid_email));
            focusView = emailLayout;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
            return false;
        } else {
            return true;
        }
    }

    private void attemptRegistration() {
        if (!isRegistrationValid()) {
            return;
        }

        String email = emailLayout.getEditText().getText().toString().concat("@pitt.edu");
        String password = passwordLayout.getEditText().getText().toString();

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this,
                (@NonNull Task<AuthResult> task) -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = auth.getCurrentUser();
                        Toast.makeText(LoginActivity.this, "Welcome " + user.getEmail(), Toast.LENGTH_SHORT).show();
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }

                    //if (!task.isSuccessful()) {
                    //    mStatusTextView.setText(R.string.auth_failed);
                    //}
                    showProgress(false);
                });

        showProgress(true);
    }

    private boolean isRegistrationValid()
    {
        emailLayout.setError(null);
        passwordLayout.setError(null);
        emailConfirmLayout.setError(null);
        passwordConfirmLayout.setError(null);

        String email = emailLayout.getEditText().getText().toString();
        String password = passwordLayout.getEditText().getText().toString();
        String emailConfirm = emailConfirmLayout.getEditText().getText().toString();
        String passwordConfirm = passwordConfirmLayout.getEditText().getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (!TextUtils.equals(password, passwordConfirm)) {
            passwordConfirmLayout.setError(getString(R.string.error_mismatch_password));
            focusView = passwordConfirmLayout;
            cancel = true;
        }

        if (!TextUtils.equals(email, emailConfirm)) {
            emailConfirmLayout.setError(getString(R.string.error_mismatch_email));
            focusView = emailConfirmLayout;
            cancel = true;
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError(getString(R.string.error_field_required));
            focusView = passwordLayout;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            passwordLayout.setError(getString(R.string.error_invalid_password));
            focusView = passwordLayout;
            cancel = true;
        }

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError(getString(R.string.error_field_required));
            focusView = emailLayout;
            cancel = true;
        } else if (!isEmailValid(email)) {
            emailLayout.setError(getString(R.string.error_invalid_email));
            focusView = emailLayout;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
            return false;
        } else {
            return true;
        }
    }

    private boolean isEmailValid(String email) {
        // TODO
        return true;
    }

    private boolean isPasswordValid(String password) {
        // TODO
        return password.length() > 5;
    }

    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }
        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(emailAutoComplete, R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE).setAction(android.R.string.ok, (View view) ->
                    requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS));
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
            Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
            ContactsContract.Contacts.Data.CONTENT_DIRECTORY),
            ProfileQuery.PROJECTION,
            ContactsContract.Contacts.Data.MIMETYPE + " = ?",
            new String[]{ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE},
            ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }
        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(LoginActivity.this,
                android.R.layout.simple_dropdown_item_1line,
                emailAddressCollection);
        emailAutoComplete.setAdapter(adapter);
    }

    private interface ProfileQuery {
        String[] PROJECTION = {
            ContactsContract.CommonDataKinds.Email.ADDRESS,
            ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }
}

