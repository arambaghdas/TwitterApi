package com.test.twitterginosi.service;

import android.content.Intent;
import android.util.Log;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.test.twitterginosi.MainActivity;

public class NetworkJobService extends JobService {

    private String TAG = "NetworkJobService";

    @Override
    public boolean onStartJob(JobParameters job) {
        Log.v(TAG, "onStartJob");
        sendNetworkBroadCast();
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        Log.v(TAG, "onStopJob");
        return false;
    }

    private void sendNetworkBroadCast() {
        try {
            Intent broadCastIntent = new Intent();
            broadCastIntent.setAction(MainActivity.BROADCAST_ACTION);
            sendBroadcast(broadCastIntent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
