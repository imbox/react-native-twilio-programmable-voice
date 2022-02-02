package com.hoxfon.react.RNTwilioVoice.fcm;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.telecom.PhoneAccount;
import android.telecom.TelecomManager;
import android.util.Log;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.hoxfon.react.RNTwilioVoice.Constants;
import com.hoxfon.react.RNTwilioVoice.IncomingCallNotificationService;
import com.twilio.voice.CallException;
import com.hoxfon.react.RNTwilioVoice.BuildConfig;
import com.hoxfon.react.RNTwilioVoice.CallNotificationManager;
import com.twilio.voice.CallInvite;
import com.twilio.voice.CancelledCallInvite;
import com.twilio.voice.MessageListener;
import com.twilio.voice.Voice;

import java.util.Map;
import java.util.Random;

import static com.hoxfon.react.RNTwilioVoice.TwilioVoiceModule.TAG;

public class VoiceFirebaseMessagingHandler {

    public void handleNewToken(Context ctx, String token) {
        Intent intent = new Intent(Constants.ACTION_FCM_TOKEN);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);
    }

    public void handleMessageReceived(Application application, Context ctx, RemoteMessage remoteMessage) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Bundle data: " + remoteMessage.getData());
        }

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();

            // If notification ID is not provided by the user for push notification, generate one at random
            Random randomNumberGenerator = new Random(System.currentTimeMillis());
            final int notificationId = randomNumberGenerator.nextInt();

            boolean valid = Voice.handleMessage(ctx, data, new MessageListener() {
                @Override
                public void onCallInvite(final CallInvite callInvite) {
                    // We need to run this on the main thread, as the React code assumes that is true.
                    // Namely, DevServerHelper constructs a Handler() without a Looper, which triggers:
                    // "Can't create handler inside thread that has not called Looper.prepare()"
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        public void run() {
                            CallNotificationManager callNotificationManager = new CallNotificationManager();
                            // Construct and load our normal React JS code bundle
                            ReactInstanceManager mReactInstanceManager = ((ReactApplication) application).getReactNativeHost().getReactInstanceManager();
                            ReactContext context = mReactInstanceManager.getCurrentReactContext();

                            // initialise appImportance to the highest possible importance in case context is null
                            int appImportance = ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE;

                            if (context != null) {
                                appImportance = callNotificationManager.getApplicationImportance((ReactApplicationContext)context);
                            }
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "context: " + context + ". appImportance = " + appImportance);
                            }

                            // when the app is not started or in the background
                            /*
                            TODO: Kontrollera huruvida denna behövs eller ej..
                            if (appImportance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                                if (BuildConfig.DEBUG) {
                                    Log.d(TAG, "Background");
                                }
                                handleInvite(callInvite, notificationId);
                                return;
                            }
                             */

                            Log.d(TAG, "Sending intent ACTION INCOMING CALL..");
                            Intent intent = new Intent(Constants.ACTION_INCOMING_CALL);
                            intent.putExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, notificationId);
                            intent.putExtra(Constants.INCOMING_CALL_INVITE, callInvite);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                        }
                    });
                }

                @Override
                public void onCancelledCallInvite(@NonNull CancelledCallInvite cancelledCallInvite, @Nullable CallException callException) {
                    // The call is prematurely disconnected by the caller.
                    // The callee does not accept or reject the call within 30 seconds.
                    // The Voice SDK is unable to establish a connection to Twilio.
                    handleCancelledCallInvite(ctx, cancelledCallInvite, callException);
                }
            });

            if (!valid) {
                Log.e(TAG, "The message was not a valid Twilio Voice SDK payload: " + remoteMessage.getData());
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.e(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }

    private void handleInvite(CallInvite callInvite, int notificationId) {
        Log.d(TAG, "Hit skall jag ej komma.. Skall ej skicka en vanlig notis...");
        /*
        Intent intent = new Intent(this, IncomingCallNotificationService.class);
        intent.setAction(Constants.ACTION_INCOMING_CALL);
        intent.putExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, notificationId);
        intent.putExtra(Constants.INCOMING_CALL_INVITE, callInvite);

        startService(intent);

         */
    }

    private void handleCancelledCallInvite(Context ctx, CancelledCallInvite cancelledCallInvite, CallException callException) {
        Intent intent = new Intent(ctx, IncomingCallNotificationService.class);
        intent.setAction(Constants.ACTION_CANCEL_CALL);
        intent.putExtra(Constants.CANCELLED_CALL_INVITE, cancelledCallInvite);
        if (callException != null) {
            intent.putExtra(Constants.CANCELLED_CALL_INVITE_EXCEPTION, callException.getMessage());
        }
        ctx.startService(intent);
    }
}
