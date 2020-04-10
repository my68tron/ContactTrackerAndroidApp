package com.ta.nearby;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NearbyUtils {
    private static final String TAG = "NearbyUtils";
    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;
    private static SharedPreferences mSharedPreferences;

    private static Context mContext;
    // Our handle to Nearby Connections
    private static ConnectionsClient connectionsClient;
    private static String mPackageName;
    private static File mFilesDir;

    /**
     * Callbacks for receiving payloads
     */
    private static final PayloadCallback mPayloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    // A new payload is being sent over.
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                        // Payload progress has updated.
                    }
                }
            };

    /**
     * Callbacks for connections to other devices
     */
    private static final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    // Automatically accept the connection on both sides.
                    connectionsClient.acceptConnection(endpointId, mPayloadCallback);
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putInt(Constants.KEY_LAST_UPLOAD_COUNT, mSharedPreferences.getInt(Constants.KEY_LAST_UPLOAD_COUNT, 0) + 1);
                    editor.apply();
//                    String data = connectionInfo.getEndpointName() + "," + getCurrentDateTime();
//                    appendToFile("connection_initiated.txt", data);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            // We're connected! Can now start sending and receiving data.
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            // The connection was rejected by one or both sides.
                            break;
                        default:
                            // The connection was broken before it was accepted.
                            break;
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    // We've been disconnected from this endpoint. No more data can be
                    // sent or received.
                    Log.i(TAG, "onDisconnected: endpoint disconnected!");
                }
            };

    /**
     * Callbacks for finding other devices
     */
    private static final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    Log.i(TAG, "onEndpointFound: endpoint found, connecting");
                    connectionsClient.requestConnection(mSharedPreferences.getString(Constants.KEY_MOB_NO, "John Doe"), endpointId, connectionLifecycleCallback);
                    String data = info.getEndpointName() + "," + getCurrentDateTime();
                    appendToFile(Constants.FILE_NAME, data);
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    Log.i(TAG, "onEndpointLost: endpoint lost!");
                }
            };

    /**
     * Helper function to get current DateTime
     * @return a String of current TimeStamp
     */
    public static String getCurrentDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    /**
     * Starting point of this Nearby API Util class
     * @param context of the calling Service
     */
    public static void startNearby(Context context) {
        Log.i(TAG, "Started");
        mContext = context;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mFilesDir = context.getFilesDir();
        mPackageName = context.getPackageName();
        connectionsClient = Nearby.getConnectionsClient(context);
        startAdvertising();
        startDiscovery();
    }

    /**
     * Starts looking for other players using Nearby Connections.
     */
    private static void startDiscovery() {
        Log.i(TAG, "Started Discovering");
        // Note: Discovery may fail. To keep this demo simple, we don't handle failures.
        connectionsClient.startDiscovery(
                mPackageName, endpointDiscoveryCallback,
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build());
    }

    /**
     * Broadcasts our presence using Nearby Connections so other players can find us.
     */
    private static void startAdvertising() {
        Log.i(TAG, "Started Advertising");
        // Note: Advertising may fail. To keep this demo simple, we don't handle failures.
        connectionsClient.startAdvertising(
                mSharedPreferences.getString(Constants.KEY_MOB_NO, "John Doe"), mPackageName, connectionLifecycleCallback,
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build());
    }

    /**
     * Helper function to append data to fineName
     * @param fileName is name of the file to append data to
     * @param data is the String which is to be appended
     */
    private static void appendToFile(String fileName, String data) {
        Log.i(TAG, "Appending to file " + fileName + ", data = " + data);
        File file = new File(mFilesDir, fileName);
        FileOutputStream fileOutputStream = null;
        PrintStream printstream = null;
        try {
            fileOutputStream = mContext.openFileOutput(fileName, mContext.MODE_APPEND);
            printstream = new PrintStream(fileOutputStream);
            printstream.print(data + "\n");
            fileOutputStream.close();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (printstream != null) { printstream.close(); }
        }
    }
}
