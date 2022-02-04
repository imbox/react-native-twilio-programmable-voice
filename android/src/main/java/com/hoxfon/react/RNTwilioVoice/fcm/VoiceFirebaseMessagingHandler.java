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

import android.telecom.Connection;
import android.telecom.DisconnectCause;
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
import com.hoxfon.react.RNTwilioVoice.TwilioVoiceModule;
import com.hoxfon.react.RNTwilioVoice.VoiceConnectionService;
import com.twilio.voice.CallException;
import com.hoxfon.react.RNTwilioVoice.BuildConfig;
import com.hoxfon.react.RNTwilioVoice.CallNotificationManager;
import com.twilio.voice.CallInvite;
import com.twilio.voice.CancelledCallInvite;
import com.twilio.voice.MessageListener;
import com.twilio.voice.Voice;

import java.util.Map;
import java.util.Random;

import static com.hoxfon.react.RNTwilioVoice.CallNotificationManager.getMainActivityClass;
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
                    Log.d(TAG, "CallInvite, addNewIncomingCall");
                    TelecomManager telecomManager = (TelecomManager) ctx.getSystemService(ctx.TELECOM_SERVICE);
                    Bundle extras = new Bundle();
                    String from = callInvite
                            .getCustomParameters().getOrDefault(Constants.INVITE_CUSTOM_PARAMETER_FROM, callInvite.getFrom());
                    Uri uri = Uri.fromParts(PhoneAccount.SCHEME_TEL, from, null);
                    extras.putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, uri);
                    extras.putBoolean(Constants.EXTRA_DISABLE_ADD_CALL, true);

                    TwilioVoiceModule.setActiveCallInvite(callInvite);

                    Bundle applicationExtras = new Bundle();
                    applicationExtras.putInt(Constants.INCOMING_CALL_NOTIFICATION_ID, notificationId);
                    extras.putBundle(TelecomManager.EXTRA_INCOMING_CALL_EXTRAS, applicationExtras);

                    telecomManager.addNewIncomingCall(TwilioVoiceModule.handle, extras);
                }

                @Override
                public void onCancelledCallInvite(@NonNull CancelledCallInvite cancelledCallInvite, @Nullable CallException callException) {
                    // The call is prematurely disconnected by the caller.
                    // The callee does not accept or reject the call within 30 seconds.
                    // The Voice SDK is unable to establish a connection to Twilio.
                    handleCancelledCallInvite(ctx, cancelledCallInvite, callException);

                    Connection conn = VoiceConnectionService.getConnection();
                    if (conn != null) {
                        conn.setDisconnected(new DisconnectCause(DisconnectCause.CANCELED));
                    }
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

    private void handleCancelledCallInvite(Context ctx, CancelledCallInvite cancelledCallInvite, CallException callException) {
        Intent intent = new Intent(ctx, IncomingCallNotificationService.class);
        intent.setAction(Constants.ACTION_CANCEL_CALL);
        intent.putExtra(Constants.CANCELLED_CALL_INVITE, cancelledCallInvite);
        if (callException != null) {
            intent.putExtra(Constants.CANCELLED_CALL_INVITE_EXCEPTION, callException.getMessage());
        }
        //ctx.startService(intent);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);
    }
}
