package com.test.twitterginosi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;
import com.test.twitterginosi.network.Network;
import com.test.twitterginosi.service.NetworkJobService;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.DefaultLogger;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;
import com.twitter.sdk.android.tweetcomposer.ComposerActivity;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    public static final String BROADCAST_ACTION = "com.test.twitter.ginosi.broadcast";
    private NetworkBroadCastReceiver networkBroadCastReceiver;
    private static final int REQUEST_PICTURE_CAPTURE = 1;
    private static final int REQUEST_PICTURE_FROM_GALLERY = 2;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 3;
    private static final int REQUEST_CAMERA_PERMISSION = 4;
    private String pictureFilePath;

    @BindView(R.id.get_tweets) Button getTwitters;
    @BindView(R.id.tweet_from_camera) Button tweetFromCamera;
    @BindView(R.id.tweet_from_gallery) Button tweetFromGallery;
    @BindView(R.id.twitter_login) TwitterLoginButton twitterLogin;
    @BindView(R.id.twitter_signout) Button twitterSignout;
    @BindView(R.id.network_message) TextView networkMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initTwitter();
        networkBroadCastReceiver = new NetworkBroadCastReceiver();
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        twitterLogin.setEnabled(true);
        // Login via Twitter application
        twitterLogin.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                enableButtons();
            }

            @Override
            public void failure(TwitterException exception) {
                showAuthFailedMessage(exception.getMessage());
                Log.v("Exception", "message: " + exception.getMessage());
                disableButtons();
            }
        });
        scheduleNetworkTask();
    }

    private void showAuthFailedMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        Toast.makeText(this,  getResources().getString(R.string.install_twitter), Toast.LENGTH_LONG).show();
    }
    @OnClick(R.id.get_tweets)
    public void getTweets(Button button) {
        if (Network.isNetworkAvailable(this)) {
            Intent Intent = new Intent(getApplication(), TimelineActivity.class);
            startActivity(Intent);
        } else {
            showNetworkConnMess();
        }
    }
    @OnClick(R.id.tweet_from_camera)
    public void tweetFromCamera(Button button) {
        if (Network.isNetworkAvailable(this)) {
            if (isPermissionGranted(android.Manifest.permission.CAMERA))
                startCameraIntent();
        } else {
            showNetworkConnMess();
        }
    }
    // Open native camera application
    private void startCameraIntent() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, REQUEST_PICTURE_CAPTURE);
            File pictureFile = null;
            try {
                pictureFile = getPictureFile();
            } catch (IOException ex) {
                Toast.makeText(this,
                        getResources().getString(R.string.photo_load_error),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (pictureFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.test.twitterginosi.fileprovider",
                        pictureFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(cameraIntent, REQUEST_PICTURE_CAPTURE);
            }
        }
    }
    private File getPictureFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(new Date());
        String pictureFile = "image" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(pictureFile,  ".jpg", storageDir);
        pictureFilePath = image.getAbsolutePath();
        return image;
    }
    @OnClick(R.id.tweet_from_gallery)
    public void tweetFromGallery(Button button) {
        if (Network.isNetworkAvailable(this)) {
            if (isPermissionGranted(android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
                startGalleryIntent();
        }  else {
            showNetworkConnMess();
        }
    }
    // Open gallery for choosing image
    private void startGalleryIntent() {
        Intent pickerPhotoIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickerPhotoIntent, REQUEST_PICTURE_FROM_GALLERY);
    }

    @OnClick(R.id.twitter_signout)
    public void twitterSignout(Button button) {
        TwitterCore.getInstance().getSessionManager().clearActiveSession();
        disableButtons();
    }

    //Initialize Twitter SDK
    private void initTwitter() {
        String CONSUMER_KEY = getResources().getString(R.string.twitter_consumer_key);
        String CONSUMER_SECRET = getResources().getString(R.string.twitter_consumer_secret);
        TwitterConfig config = new TwitterConfig.Builder(this)
                .logger(new DefaultLogger(Log.DEBUG))
                .twitterAuthConfig(new TwitterAuthConfig(CONSUMER_KEY, CONSUMER_SECRET))
                .debug(true)
                .build();
        Twitter.initialize(config);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICTURE_CAPTURE && resultCode == RESULT_OK) {
            File imgFile = new File(pictureFilePath);
            if (imgFile.exists())
                 postInTwitter(Uri.fromFile(imgFile));
        } else if (requestCode == REQUEST_PICTURE_FROM_GALLERY && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();
            postInTwitter(imageUri);
        } else {
            twitterLogin.onActivityResult(requestCode, resultCode, data);
        }
    }
    // Post image in the Twitter
    private void postInTwitter(Uri imageUri) {
         Intent intent = new ComposerActivity.Builder(MainActivity.this)
                .session(TwitterCore.getInstance().getSessionManager().getActiveSession())
                .image(imageUri)
                .createIntent();
           startActivity(intent);
    }
    // Run firebase job scheduler for getting event in case of network is available
    private void scheduleNetworkTask() {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(getBaseContext()));
        Job myJob = dispatcher.newJobBuilder()
                .setService(NetworkJobService.class)
                .setTag("network-tag")
                .setReplaceCurrent(false)
                .setRecurring(true)
                .setTrigger(Trigger.executionWindow(0, 10))
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .build();
        dispatcher.mustSchedule(myJob);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED && requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION ) {
            if (Network.isNetworkAvailable(this))
                startGalleryIntent();
            else
                showNetworkConnMess();
        } else if(grantResults[0]== PackageManager.PERMISSION_GRANTED && requestCode == REQUEST_CAMERA_PERMISSION ) {
            if (Network.isNetworkAvailable(this))
                startCameraIntent();
            else
                showNetworkConnMess();
        }
    }
    // Check permission in run time
    private  boolean isPermissionGranted(String permission) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                if (permission.equals(android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
                    ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION);
                else   if (permission.equals(android.Manifest.permission.CAMERA))
                    ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_CAMERA_PERMISSION);
                return false;
            }
        } else {
            return true;
        }
    }
    private void showNetworkConnMess() {
        networkMessage.setText(getResources().getString(R.string.network_error));
    }

    private void enableButtons() {
        getTwitters.setEnabled(true);
        tweetFromGallery.setEnabled(true);
        tweetFromCamera.setEnabled(true);
        twitterLogin.setVisibility(View.GONE);
        twitterSignout.setVisibility(View.VISIBLE);
    }
    private void disableButtons() {
        getTwitters.setEnabled(false);
        tweetFromGallery.setEnabled(false);
        tweetFromCamera.setEnabled(false);
        twitterLogin.setVisibility(View.VISIBLE);
        twitterSignout.setVisibility(View.GONE);
    }

    @Override
    public void onStart(){
        super.onStart();
        registerNetworkReceiver();
        if (TwitterCore.getInstance().getSessionManager().getActiveSession() == null)
            disableButtons();
        else
            enableButtons();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(networkBroadCastReceiver);
    }

    class NetworkBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v("NetworkJobService", "onReceive");
            networkMessage.setText("");
        }
    }
    // Register broadcast for network change  event
    private void registerNetworkReceiver() {
        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BROADCAST_ACTION);
            registerReceiver(networkBroadCastReceiver, intentFilter);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
