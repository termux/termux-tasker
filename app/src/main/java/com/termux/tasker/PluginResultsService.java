package com.termux.tasker;

import android.app.IntentService;
import android.content.Intent;
import androidx.annotation.Nullable;

import com.termux.shared.logger.Logger;
import com.termux.tasker.utils.PluginUtils;

public class PluginResultsService extends IntentService {

    public static final String PLUGIN_SERVICE_LABEL = "PluginResultsService";

    private static final String LOG_TAG = "PluginResultsService";


    public PluginResultsService(){
        super(PLUGIN_SERVICE_LABEL);
    }

    /**
     * Receive intent containing result of commands and send pending result back to plugin host app.
     *
     * @param intent The {@link Intent} containing result and original intent received by {@link FireReceiver}.
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            if(intent.getComponent() != null)
                Logger.logInfo(LOG_TAG, PLUGIN_SERVICE_LABEL + " received execution result");
            PluginUtils.sendPendingResultToPluginHostApp(this, intent);
        }
    }

}
