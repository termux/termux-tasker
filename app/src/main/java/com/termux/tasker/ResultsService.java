package com.termux.tasker;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * A service to handle the results sent back from Termux-app when it finishes executing a command we
 * want to set Tasker variables with
 *
 * Expects an "originalIntent" ParcelableExtra containing the original {@link Intent} (Sent with the
 * {@link android.app.PendingIntent} from the {@link FireReceiver}) and a {@link Bundle} object extra
 * at "result" containing the keys "stdout" (String), "stderr" (String), and "exitCode" (Integer).
 */
public class ResultsService extends IntentService {
    public ResultsService(){
        super("ResultsService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        final Bundle result = intent.getBundleExtra("result");
        final Intent originalIntent = intent.getParcelableExtra(FireReceiver.ORIGINAL_INTENT);
        final Bundle varsBundle = new Bundle();

        varsBundle.putString("%stdout", result.getString("stdout", ""));
        varsBundle.putString("%stderr", result.getString("stderr", ""));
        varsBundle.putString("%result", Integer.toString(result.getInt("exitCode")));

        TaskerPlugin.Setting.signalFinish(this, originalIntent, TaskerPlugin.Setting.RESULT_CODE_OK, varsBundle);
    }
}
