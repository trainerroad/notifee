package app.notifee.core;

/*
 * Copyright (c) 2016-present Invertase Limited & Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this library except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import app.notifee.core.event.ForegroundServiceEvent;
import app.notifee.core.event.NotificationEvent;
import app.notifee.core.interfaces.MethodCallResult;
import app.notifee.core.model.NotificationModel;

public class ForegroundService extends Service {
  private static final String TAG = "ForegroundService";
  public static final String START_FOREGROUND_SERVICE_ACTION =
      "app.notifee.core.ForegroundService.START";
  public static final String STOP_FOREGROUND_SERVICE_ACTION =
      "app.notifee.core.ForegroundService.STOP";

  public static String mCurrentNotificationId = null;

  static void start(int hashCode, Notification notification, Bundle notificationBundle) {
    Intent intent = new Intent(ContextHolder.getApplicationContext(), ForegroundService.class);
    intent.setAction(START_FOREGROUND_SERVICE_ACTION);
    intent.putExtra("hashCode", hashCode);
    intent.putExtra("notification", notification);
    intent.putExtra("notificationBundle", notificationBundle);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      try{
        Logger.d(TAG, "Starting foreground service");
          ContextHolder.getApplicationContext().startForegroundService(intent);
        } catch (Exception exception) {
          Logger.e(TAG, "Unable to start foreground service", exception);
        }
    } else {
        Logger.d(TAG, "Starting service");
      ContextHolder.getApplicationContext().startService(intent);
    }
  }

  static void stop() {
    Intent intent = new Intent(ContextHolder.getApplicationContext(), ForegroundService.class);
    intent.setAction(STOP_FOREGROUND_SERVICE_ACTION);

    try {
      // Call start service first with stop action
      ContextHolder.getApplicationContext().startService(intent);
    } catch (IllegalStateException illegalStateException) {
      // try to stop with stopService command
      ContextHolder.getApplicationContext().stopService(intent);
    } catch (Exception exception) {
      Logger.e(TAG, "Unable to stop foreground service", exception);
    }
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    Logger.d(TAG, "onConfigurationChanged");
  }


  @Override
  public void onCreate() {
    super.onCreate();
    Logger.d(TAG, "onCreate");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Logger.d(TAG, "onDestroy");

  }

  @Override
  public void onTaskRemoved(Intent rootIntent) {
    super.onTaskRemoved(rootIntent);
    Logger.d(TAG, "onTaskRemoved");
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    Logger.d(TAG, "onLowMemory");
  }
  @Override
  public void onTrimMemory(int level) {
    super.onTrimMemory(level);
    Logger.d(TAG, "onTrimMemory: " + level);
  }
  @Override
  public boolean onUnbind(Intent intent) {
    boolean result = super.onUnbind(intent);
    Logger.d(TAG, "onUnbind: " + result);
    return result;
  }
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.d(TAG, "onStartCommand, intent: " + intent + " flags: " + flags+ " startId: "  + startId);
    // Check if action is to stop the foreground service
    if (intent == null || STOP_FOREGROUND_SERVICE_ACTION.equals(intent.getAction())) {
      Logger.d(TAG, "stopping self and returning");
      stopSelf();
      mCurrentNotificationId = null;
      return START_STICKY_COMPATIBILITY;
    }

    Bundle extras = intent.getExtras();

    if (extras != null) {
      // Hash code is sent to service to ensure it is kept the same
      int hashCode = extras.getInt("hashCode");
      Notification notification = extras.getParcelable("notification");
      Bundle bundle = extras.getBundle("notificationBundle");

      if (notification != null & bundle != null) {
        NotificationModel notificationModel = NotificationModel.fromBundle(bundle);

        if (mCurrentNotificationId == null) {
          mCurrentNotificationId = notificationModel.getId();
          Logger.d(TAG, "calling startForeground");
          startForeground(hashCode, notification);

          // On headless task complete
          final MethodCallResult<Void> methodCallResult =
              (e, aVoid) -> {
                stopForeground(true);
                mCurrentNotificationId = null;
              };

          ForegroundServiceEvent foregroundServiceEvent =
              new ForegroundServiceEvent(notificationModel, methodCallResult);

          EventBus.post(foregroundServiceEvent);
        } else if (mCurrentNotificationId.equals(notificationModel.getId())) {
          Logger.d(TAG, "calling notify");
          NotificationManagerCompat.from(ContextHolder.getApplicationContext())
              .notify(hashCode, notification);
        } else {
          EventBus.post(
            new NotificationEvent(NotificationEvent.TYPE_FG_ALREADY_EXIST, notificationModel));
        }
      } else {
        Logger.d(TAG, "notification or bundle is null");
      }
    } else {
      Logger.d(TAG, "extras is null");
    }

    Logger.d(TAG, "returning START_NOT_STICKY");
    return START_NOT_STICKY;
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
