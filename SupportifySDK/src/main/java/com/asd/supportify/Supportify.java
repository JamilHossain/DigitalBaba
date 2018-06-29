package com.asd.supportify;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;
import com.asd.supportify.Model.AuthorPojo;
import com.asd.supportify.Model.Message;
import com.asd.supportify.Model.MessagePojo;
import com.stfalcon.chatkit.commons.ImageLoader;
import com.stfalcon.chatkit.messages.MessageInput;
import com.stfalcon.chatkit.messages.MessagesList;
import com.stfalcon.chatkit.messages.MessagesListAdapter;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by android on 14/8/17.
 */

public class Supportify extends AppCompatActivity {

    MessagesList messagesList;
    MessageInput input;

    public static void start(Activity activity){
        activity.startActivity(new Intent(activity,Supportify.class));
    }

    private String getName() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(Supportify.this);
        return sp.getString("name", null);
    }

    private void setName(String name) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(Supportify.this);
        sp.edit().putString("name", name).apply();

    }

    private String getEmail() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(Supportify.this);
        return sp.getString("email", null);
    }

    private void setEmail(String email) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(Supportify.this);
        sp.edit().putString("email", email).apply();
    }

    private boolean intialize;
    MessagesListAdapter<Message> adapter;
    private String senderId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        senderId = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        messagesList = findViewById(R.id.messagesList);
        input = findViewById(R.id.input);
        adapter = new MessagesListAdapter<>(senderId, new ImageLoader() {
            @Override
            public void loadImage(ImageView imageView, String url) {
                Glide.with(Supportify.this).load(url).into(imageView);
            }
        });
        messagesList.setAdapter(adapter);
        if (getEmail() == null) {
            inituserInfo();
        } else {
            init();
            initFirebase();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.add(0,1,1,"Profile");
        menu.add(0,2,1,"Logout");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == 1) {
            inituserInfo();
            return true;
        } else if (id == 2) {
            logout();
        }

        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        FirebaseDatabase.getInstance().getReference().child("Chat").child(senderId).removeValue(new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                finish();
            }
        });
    }

    private void inituserInfo() {
        View view = getLayoutInflater().inflate(R.layout.supportify_dialog, null);
        final EditText name = view.findViewById(R.id.username);
        final EditText email = view.findViewById(R.id.email);
        email.setText(getEmail());
        name.setText(getName());
        new AlertDialog.Builder(this)
                .setView(view)
                .setTitle("Please provide your information.")
                .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        setName(name.getText().toString());
                        setEmail(email.getText().toString());
                        if (email.length() == 0) {
                            inituserInfo();
                        } else {
                            init();
                            initFirebase();
                        }

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                }).create().show();
    }

    private void initFirebase() {
        final DatabaseReference query = FirebaseDatabase.getInstance().getReference().child("Users").child(senderId);
        Map<String, Object> taskMap = new HashMap<String, Object>();
        taskMap.put("status", "ONLINE");
        taskMap.put("name", getName());
        taskMap.put("email", getEmail());
        taskMap.put("token", FirebaseInstanceId.getInstance().getToken());
        query.updateChildren(taskMap, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                query.child("status").onDisconnect().setValue(ServerValue.TIMESTAMP);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseDatabase.getInstance().goOffline();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseDatabase.getInstance().goOnline();
    }

    private void init() {
        DatabaseReference query = FirebaseDatabase.getInstance().getReference().child("Chat").child(senderId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!intialize) {
                    //Log.e("E",dataSnapshot.getKey());
                    intialize = true;
                    FirebaseDatabase.getInstance().getReference().child("Chat").child(dataSnapshot.getKey()).limitToLast(10).addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                            if (dataSnapshot.hasChildren()) {
                                Message chat = dataSnapshot.getValue(MessagePojo.class).getM();
                                if (adapter != null) {
                                    adapter.addToStart(chat, true);
                                }

                            }

                        }

                        @Override
                        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onChildRemoved(DataSnapshot dataSnapshot) {

                        }

                        @Override
                        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        input.setInputListener(new MessageInput.InputListener() {
            @Override
            public boolean onSubmit(CharSequence input) {
                sendMessage(input.toString(), false);
                return true;
            }
        });

        input.setAttachmentsListener(new MessageInput.AttachmentsListener() {
            @Override
            public void onAddAttachments() {
                //select attachments

                if (ContextCompat.checkSelfPermission(Supportify.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(Supportify.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.

                    } else {

                        // No explanation needed, we can request the permission.

                        ActivityCompat.requestPermissions(Supportify.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                8888);

                        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                        // app-defined int constant. The callback method gets the
                        // result of the request.
                    }
                } else {
                    Matisse.from(Supportify.this)
                            .choose(MimeType.allOf())
                            .countable(true)
                            .maxSelectable(1)
                            .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                            .thumbnailScale(0.85f)
                            .imageEngine(new GlideEngine())
                            .forResult(9632);
                }


            }
        });

    }


    private void sendMessage(String message, boolean isImage) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Chat");
        String messageId = reference.push().getKey();
        MessagePojo chat = new MessagePojo();
        chat.setId(senderId);
        chat.setText(message);
        if (isImage) {
            chat.setText("");
            chat.setImage(message);
        }

        chat.setCreatedAt(Calendar.getInstance().getTime().getTime());
        AuthorPojo user = new AuthorPojo();
        user.setId(senderId);
        user.setName(getName());
        chat.setUser(user);
        reference.child(senderId).child(messageId).setValue(chat).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {


            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 9632 && resultCode == RESULT_OK) {
            List<Uri> mSelected = Matisse.obtainResult(data);
            FirebaseStorage.getInstance().getReference("uploads").child("hello").putFile(mSelected.get(0)).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()) {
                        sendMessage(task.getResult().getDownloadUrl().toString(), true);
                    }
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 8888: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Matisse.from(Supportify.this)
                            .choose(MimeType.allOf())
                            .countable(true)
                            .maxSelectable(1)
                            .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                            .thumbnailScale(0.85f)
                            .imageEngine(new GlideEngine())
                            .forResult(9632);
                } else {
                    Toast.makeText(this, "Please approve permission to user this feature.", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
