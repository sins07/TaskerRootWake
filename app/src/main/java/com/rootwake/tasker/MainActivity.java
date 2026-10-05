package com.rootwake.tasker;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

/**
 * Invisible activity: launches, asks root to start Tasker's ServiceRequestQuery
 * service via the activity manager, shows a toast with the result, and exits.
 */
public class MainActivity extends Activity {

    private static final String TAG = "TaskerRootWake";
    private static final String COMPONENT =
            "net.dinglisch.android.taskerm/com.joaomgcd.taskerm.plugin.ServiceRequestQuery";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context app = getApplicationContext();

        new Thread(new Runnable() {
            @Override
            public void run() {
                final String result = wakeTasker();
                Log.i(TAG, result);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(app, result, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();

        // Theme.NoDisplay activities must finish before onResume.
        finish();
    }

    private static String wakeTasker() {
        try {
            Result r = su("am startservice -n " + COMPONENT);
            if (r.ok()) return "Tasker woken";

            // Android 8+ may refuse a background start; retry as a foreground service.
            if (Build.VERSION.SDK_INT >= 26) {
                Result r2 = su("am start-foreground-service -n " + COMPONENT);
                if (r2.ok()) return "Tasker woken (foreground)";
                return "Tasker wake failed: " + r2.output;
            }
            return "Tasker wake failed: " + r.output;
        } catch (Exception e) {
            return "Root error: " + e.getMessage();
        }
    }

    private static Result su(String command) throws Exception {
        Process p = new ProcessBuilder("su").redirectErrorStream(true).start();

        DataOutputStream os = new DataOutputStream(p.getOutputStream());
        os.writeBytes(command + "\n");
        os.writeBytes("exit\n");
        os.flush();
        os.close();

        StringBuilder out = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = br.readLine()) != null) {
            out.append(line).append('\n');
        }
        br.close();

        int code = p.waitFor();
        Log.i(TAG, "$ " + command + "\n(exit " + code + ")\n" + out);
        return new Result(code, out.toString().trim());
    }

    private static final class Result {
        final int exitCode;
        final String output;

        Result(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }

        boolean ok() {
            return exitCode == 0
                    && !output.contains("Error")
                    && !output.contains("Exception");
        }
    }
}
