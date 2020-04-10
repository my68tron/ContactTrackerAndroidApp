package com.ta.nearby;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class UploadWorker extends Worker {
    private String TAG = "UploadWorker";

    private String BASE_PROTOCOL = "http://";
    private String BASE_IP = "coronacontacttracker.herokuapp.com";
//    private static String BASE_IP = "192.168.43.195";
//    private String BASE_PORT = ":8000";
    private String REQUEST_URL = "/";
    private SharedPreferences mSharedPreferences;
    private File mUploadFile;

    public UploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /**
     * Returns new URL object from the given string URL.
     */
    public URL createUrl(String action) {
        BASE_IP = mSharedPreferences.getString(Constants.KEY_LOCAL_SERVER_IP, BASE_IP);
        // Create URL object
        String stringUrl = BASE_PROTOCOL + BASE_IP + REQUEST_URL + action;
        URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Problem building the URL ", e);
        }
        return url;
    }

    public JSONObject createUpdateRequestData() {
        String id = mSharedPreferences.getString(Constants.KEY_MOB_NO, "");
        JSONObject requestData = new JSONObject();
        JSONArray tuples = new JSONArray();
        try {
            requestData.put("from_mob_no", id);

            BufferedReader br = new BufferedReader(new FileReader(mUploadFile));
            String line;
            JSONObject tuple;
            while ((line = br.readLine()) != null) {
                String[] row = line.split(",");
                tuple = new JSONObject();
                tuple.put("to_mob_no", row[0]);
                tuple.put("timestamp", row[1]);
                tuples.put(tuple);
            }

            requestData.put("contacts", tuples);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return requestData;
    }

    /**
     * Make an HTTP request to the given URL and return a String as the response.
     */
    public String makeHttpRequest(URL url, String action, String jsonRequestData) throws IOException {
        String jsonResponse = "";

        // If the URL is null, then return early.
        if (url == null) {
            return jsonResponse;
        }

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(15000 /* milliseconds */);
            urlConnection.setConnectTimeout(5000 /* milliseconds */);

            switch (action) {
                case "update":
//                    sendData(urlConnection);
                    urlConnection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    urlConnection.setRequestProperty("Accept", "application/json");
                    urlConnection.setUseCaches(false);
                    urlConnection.setDoOutput(true);
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Connection", "Keep-Alive");
                    urlConnection.connect();

                    DataOutputStream dataOutputStream = new DataOutputStream(urlConnection.getOutputStream());
                    dataOutputStream.writeBytes(jsonRequestData);
                    dataOutputStream.flush();
                    dataOutputStream.close();
                    break;
            }

            // If the request was successful (response code 200),
            // then read the input stream and parse the response.
            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } else {
                Log.e(TAG, "Error response code: " + urlConnection.getResponseCode());
            }
        } catch (IOException e) {
            Log.e(TAG, "Problem retrieving the response JSON." + url);

        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                // Closing the input stream could throw an IOException, which is why
                // the makeHttpRequest() method signature specifies than an IOException
                // could be thrown.
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    private HttpURLConnection sendData(HttpURLConnection urlConnection) throws IOException {
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        FileInputStream fileInputStream;

        urlConnection.setRequestProperty("ENCTYPE", "multipart/form-data");
        urlConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
        urlConnection.setRequestProperty("Accept", "application/json");
        urlConnection.setUseCaches(false);
        urlConnection.setDoOutput(true);
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Connection", "Keep-Alive");
        urlConnection.connect();

        DataOutputStream dataOutputStream = new DataOutputStream(urlConnection.getOutputStream());
        String mobNo = mSharedPreferences.getString(Constants.KEY_MOB_NO, "mob_no");

        fileInputStream = new FileInputStream(mUploadFile);
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" + "contacts_info" + "\";filename=\""
                + mobNo + "\"" + lineEnd + lineEnd);

        // create a buffer of maximum size
        bytesAvailable = fileInputStream.available();

        bufferSize = Math.min(bytesAvailable, maxBufferSize);
        buffer = new byte[bufferSize];

        // read file and write it into form...
        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        while (bytesRead > 0) {
            dataOutputStream.write(buffer, 0, bufferSize);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math
                    .min(bytesAvailable, maxBufferSize);
            bytesRead = fileInputStream.read(buffer, 0,
                    bufferSize);

        }
        dataOutputStream.writeBytes(lineEnd + twoHyphens + boundary + twoHyphens + lineEnd);

        fileInputStream.close();
        dataOutputStream.flush();
        dataOutputStream.close();

        return urlConnection;
    }

    /**
     * Convert the {@link InputStream} into a String which contains the
     * whole JSON response from the server.
     */
    private String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    /**
     * Return a boolean value if {@param responseJson} is successfully parsed or not
     */
    public boolean processResponseJson(String responseJson) {
        // If the JSON string is empty or null, then return early.
        if (TextUtils.isEmpty(responseJson)) {
            return false;
        }

        // Try to parse the JSON response string. If there's a problem with the way the JSON
        // is formatted, a JSONException exception object will be thrown.
        // Catch the exception so the app doesn't crash, and print the error message to the logs.
        try {
            // Create a JSONObject from the JSON response string
            JSONObject baseJsonResponse = new JSONObject(responseJson);

            if (baseJsonResponse.has("total_contacts")) {

            }
        } catch (JSONException e) {
            // If an error is thrown when executing any of the above statements in the "try" block,
            // catch the exception here, so the app doesn't crash. Print a log message
            // with the message from the exception.
            Log.e(TAG, "Problem parsing the response JSON.", e);
        }

        // Return true if successfully processed
        return true;
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(Constants.KEY_LAST_UPLOAD_COUNT, 0);
        editor.putString(Constants.KEY_LAST_UPLOADED_AT, NearbyUtils.getCurrentDateTime());
        editor.apply();

        File filesDir = context.getFilesDir();
        File file = new File(filesDir, Constants.FILE_NAME);
        if (file.exists()) {
            Log.i(TAG, "Sending File");
            mUploadFile = new File(filesDir, Constants.FILE_NAME + ".old");
            file.renameTo(mUploadFile);

            String action = "update";
            URL url = createUrl(action);

            String jsonRequestData = createUpdateRequestData().toString();

            // Perform HTTP request to the URL and receive a JSON response back
            String jsonResponse = null;
            try {
                jsonResponse = makeHttpRequest(url, action, jsonRequestData);
            } catch (IOException e) {
                Log.e(TAG, "Problem making the HTTP request.", e);
            }

            if (TextUtils.isEmpty(jsonResponse)) {
                mUploadFile.renameTo(new File(filesDir, Constants.FILE_NAME));
                return Result.retry();
            } else {
                mUploadFile.delete();

                // Process relevant fields from the JSON response and return if it was successful or not
                if (processResponseJson(jsonResponse)) {
                    return Result.success();
                } else {
                    return Result.failure();
                }
            }
        }
        return Result.success();
    }
}
