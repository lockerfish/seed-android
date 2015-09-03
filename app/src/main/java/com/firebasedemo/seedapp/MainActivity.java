package com.firebasedemo.seedapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.ui.FirebaseListAdapter;

public class MainActivity extends ActionBarActivity implements Firebase.AuthStateListener, Firebase.AuthResultHandler {
    private static String TAG = MainActivity.class.getSimpleName();

    private Firebase mFirebaseRef;
    private String mUsername;
    private Button mSendButton;
    private EditText mTextEdit;
    private FirebaseListAdapter<Message> mFirebaseAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create a reference to a location in our Firebase Database that stores the chat messages
        mFirebaseRef = new Firebase("https://<your-firebase-app>.firebaseio.com/messages");

        mTextEdit = (EditText) findViewById(R.id.text_edit);

        // When the user clicks the Send button, we call onSendButtonClick()
        mSendButton = (Button) findViewById(R.id.send_button);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.onSendButtonClick();
            }
        });

        // Whenever the authentication state changes, our onAuthStateChanged method will be called
        mFirebaseRef.addAuthStateListener(this);

        // Create an object that adapts the messages from the Firebase Database into Views for the ListView
        mFirebaseAdapter = new FirebaseListAdapter<Message>(this, Message.class, android.R.layout.two_line_list_item, mFirebaseRef) {
            @Override
            protected void populateView(View view, Message message) {
                ((TextView)view.findViewById(android.R.id.text1)).setText(message.getName());
                ((TextView)view.findViewById(android.R.id.text2)).setText(message.getText());
            }
        };
        ((ListView)findViewById(R.id.messages_list)).setAdapter(mFirebaseAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Tell the adapter to close its connection to Firebase
        mFirebaseAdapter.cleanup();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // Show/hide the Login/Logout menu options based on whether a user is logged in
        menu.findItem(R.id.action_auth).setVisible(mUsername == null);
        menu.findItem(R.id.action_unauth).setVisible(mUsername != null);

        return true; // display the menu
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_auth) {
            // Build a dialog where the user enters their email and password
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(R.string.login_message)
                    .setTitle(R.string.login_label)
                    .setView(MainActivity.this.getLayoutInflater().inflate(R.layout.dialog_signin, null))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            AlertDialog dlg = (AlertDialog) dialog;
                            final String email = ((TextView)dlg.findViewById(R.id.email)).getText().toString();
                            final String password =((TextView)dlg.findViewById(R.id.password)).getText().toString();

                            // Use the email and password to create and/or authenticate the user
                            mFirebaseRef.createUser(email, password, new Firebase.ResultHandler() {
                                @Override
                                public void onSuccess() {
                                    mFirebaseRef.authWithPassword(email, password, MainActivity.this);
                                }
                                @Override
                                public void onError(FirebaseError firebaseError) {
                                    mFirebaseRef.authWithPassword(email, password, MainActivity.this);
                                }
                            });
                        }
                    })
                    .create()
                    .show();
        }

        if (id == R.id.action_unauth) {
            // Firebase.unauth() is a synchronous method. If the method succeeds, the user is immediately logged out.
            mFirebaseRef.unauth();
        }


        return super.onOptionsItemSelected(item);
    }

    private void onSendButtonClick() {
        // Create a message object out of the username and the message the user entered
        Message message = new Message(mUsername, mTextEdit.getText().toString());

        // Create a new message in the Firebase Database and with the values
        mFirebaseRef.push().setValue(message);
    }


    @Override
    public void onAuthStateChanged(AuthData authData) {
        if(authData != null) {
            mUsername = ((String)authData.getProviderData().get("email"));
        }
        else {
            mUsername = null;
        }
        Log.i(TAG, "onAuthStateChanged: mUserName="+mUsername);
        invalidateOptionsMenu();
    }

    @Override
    public void onAuthenticated(AuthData authData) {
        // We already capture auth state changes in the method above, so there is nothing to do here
    }

    @Override
    public void onAuthenticationError(final FirebaseError firebaseError) {
        new AlertDialog.Builder(this)
                .setMessage(firebaseError.getMessage())
                .setTitle(R.string.auth_error_title)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }
}
