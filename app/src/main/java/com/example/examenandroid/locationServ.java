package com.example.examenandroid;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class locationServ extends Service {
    public locationServ() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}