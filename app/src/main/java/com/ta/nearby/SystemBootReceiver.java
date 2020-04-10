package com.ta.nearby;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SystemBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.e("SystemBoot", "Received!");
            Intent startJob = new Intent(context, NearbyJobIntentService.class);
            NearbyJobIntentService.enqueueWork(context, startJob);
        }
    }
}
