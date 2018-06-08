package com.test.twitterginosi.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.test.twitterginosi.R;
import com.twitter.sdk.android.tweetcomposer.TweetUploadService;

public class TwitterResultReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (TweetUploadService.UPLOAD_SUCCESS.equals(intent.getAction()))
            Toast.makeText(context, context.getResources().getString(R.string.twitter_post_success),Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(context,context.getResources().getString(R.string.twitter_post_fail),Toast.LENGTH_SHORT).show();
    }
}