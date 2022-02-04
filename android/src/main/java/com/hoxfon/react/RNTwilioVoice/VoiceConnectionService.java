package com.hoxfon.react.RNTwilioVoice;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.telecom.CallAudioState;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.Log;

import com.twilio.voice.CallInvite;

import java.util.HashMap;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

@RequiresApi(api = Build.VERSION_CODES.M)
public class VoiceConnectionService extends ConnectionService {
    private static final String TAG = "VoiceConnectionService";

    private static VoiceConnection connection;

    public static Connection getConnection() {
        return connection;
    }

    public static void deinitConnection() {
        connection = null;
    }

    @Override
    public Connection onCreateIncomingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        Log.d(TAG, "onCreateIncomingConnection 222");
        Connection incomingCallConnection = createConnection(request);
        incomingCallConnection.setRinging();
        /*
        Bundle extras = new Bundle();
        extras.putString(Constants.INCOMING_CALL_NOTIFICATION_ID, request.getExtras().getString(Constants.INCOMING_CALL_NOTIFICATION_ID));
        sendCallRequestToActivity(
                Constants.ACTION_INCOMING_CALL_RECEIVED,
                extras);
         */
        return incomingCallConnection;
    }

    @Override
    public void onCreateIncomingConnectionFailed(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        // This might happen if the user is
        // in an emergency call (and some other cases)
        // https://developer.android.com/reference/android/telecom/ConnectionService#onCreateIncomingConnectionFailed(android.telecom.PhoneAccountHandle,%20android.telecom.ConnectionRequest)
        Bundle extras = new Bundle();
        extras.putString(Constants.INCOMING_CALL_NOTIFICATION_ID, request.getExtras().getString(Constants.INCOMING_CALL_NOTIFICATION_ID));
        sendCallRequestToActivity(
                Constants.ACTION_INCOMING_CALL_FAILED,
                extras);
    }

    @Override
    public Connection onCreateOutgoingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        Connection outgoingCallConnection = createConnection(request);
        outgoingCallConnection.setDialing();
        return outgoingCallConnection;
    }

    @Override
    public void onCreateOutgoingConnectionFailed(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        // This might happen if the user is
        // in an emergency call (and some other cases)
        // https://developer.android.com/reference/android/telecom/ConnectionService#onCreateIncomingConnectionFailed(android.telecom.PhoneAccountHandle,%20android.telecom.ConnectionRequest)
        //sendCallRequestToActivity(Constants.ACTION_OUTGOING_CALL_FAILED);
    }


    private Connection createConnection(ConnectionRequest request) {
        Context context = this;
        connection = new VoiceConnection(this, null) {

            @Override
            public void onStateChanged(int state) {
                if (state == Connection.STATE_DIALING) {
                    final Handler handler = new Handler();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            sendCallRequestToActivity(Constants.ACTION_OUTGOING_CALL, null);
                        }
                    });
                }
            }

            @Override
            public void onCallAudioStateChanged(CallAudioState state) {
                Log.d(TAG, "onCallAudioStateChanged called, current state is " + state);
            }

            @Override
            public void onPlayDtmfTone(char c) {
                Log.d(TAG, "onPlayDtmfTone called with DTMF " + c);
                Bundle extras = new Bundle();
                extras.putString(Constants.DTMF, Character.toString(c));
                connection.setExtras(extras);
                final Handler handler = new Handler();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        sendCallRequestToActivity(Constants.ACTION_DTMF_SEND, null);
                    }
                });
            }

            @Override
            public void onDisconnect() {
                super.onDisconnect();
                connection.setDisconnected(new DisconnectCause(DisconnectCause.LOCAL));
                connection.destroy();
                connection = null;
                final Handler handler = new Handler();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        sendCallRequestToActivity(Constants.ACTION_DISCONNECT_CALL, null);
                    }
                });
            }

            @Override
            public void onSeparate() {
                super.onSeparate();
            }

            @Override
            public void onAbort() {
                super.onAbort();
                connection.setDisconnected(new DisconnectCause(DisconnectCause.CANCELED));
                connection.destroy();
            }

            @Override
            public void onAnswer() {
                super.onAnswer();
                Log.d(TAG, "onAnswer pressed");
                final Handler handler = new Handler();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        sendCallRequestToActivity(Constants.ACTION_ANSWER_CALL, null);
                    }
                });
            }

            @Override
            public void onReject() {
                super.onReject();
                Log.d(TAG, "onReject pressed");
                sendCallRequestToActivity(Constants.ACTION_REJECT_CALL, null);
                connection.setDisconnected(new DisconnectCause(DisconnectCause.CANCELED));
                connection.destroy();
            }

            @Override
            public void onHold() {
                super.onHold();
                final Handler handler = new Handler();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        sendCallRequestToActivity(Constants.ACTION_HOLD_CALL, null);
                    }
                });
            }

            @Override
            public void onUnhold() {
                super.onUnhold();
                final Handler handler = new Handler();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        sendCallRequestToActivity(Constants.ACTION_UNHOLD_CALL, null);
                    }
                });
            }

            @Override
            public void onPostDialContinue(boolean proceed) {
                super.onPostDialContinue(true);
            }

            @Override
            public void onShowIncomingCallUi() {
                Log.d(TAG, "[VoiceConnection] onShowIncomingCallUi");
                final int notificationId = this.getExtras().getInt(Constants.INCOMING_CALL_NOTIFICATION_ID);
                final CallInvite callInvite = TwilioVoiceModule.getActiveCallInvite();

                Log.d(TAG, "Extras onShowIncomingUi:" + this.getExtras());
                Log.d(TAG, "Extras onShowIncomingUi:" + this.getExtras().getParcelable(Constants.INCOMING_CALL_INVITE));
/*
                final Handler handler = new Handler();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        */
                        // TODO: app background? Show notification. App foreground? Show UI in App somehow..
                        Log.d(TAG, "app background? Show notification. App foreground? Show UI in App somehow..");
                        //sendCallRequestToActivity(Constants.ACTION_SHOW_INCOMING_CALL_UI, handle);

                // App in background
                        CallNotificationManager.createIncomingCallNotification(
                                context,
                                callInvite,
                                notificationId,
                                NotificationManager.IMPORTANCE_HIGH
                        );
                        /*
                        Intent intent = new Intent(context, IncomingCallNotificationService.class);
                        intent.setAction(Constants.ACTION_INCOMING_CALL);
                        intent.putExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, notificationId);
                        intent.putExtra(Constants.INCOMING_CALL_INVITE, callInvite);

                        startService(intent);
                        */
                    }
                /*});

            }*/
        };
        connection.setConnectionCapabilities(Connection.CAPABILITY_MUTE | Connection.CAPABILITY_SUPPORT_HOLD);
        connection.setConnectionProperties(Connection.PROPERTY_SELF_MANAGED);

        /*
        if (request.getExtras().getString(CALLEE) == null) {
            connection.setAddress(request.getAddress(), TelecomManager.PRESENTATION_ALLOWED);
        } else {
            connection.setAddress(Uri.parse(request.getExtras().getString(CALLEE)), TelecomManager.PRESENTATION_ALLOWED);
        }
        s */
        connection.setDialing();
        connection.setExtras(request.getExtras());

        Log.d(TAG, "Extras request:" + request.getExtras());

        Bundle appExtras = request.getExtras().getBundle(TelecomManager.EXTRA_INCOMING_CALL_EXTRAS);

        // sendCallRequestToActivity(Constants.ACTION_INCOMING_CALL_RECEIVED, null);
        Log.d(TAG, "Created connection");
        return connection;
    }

    /*
     * Send call request to the VoiceConnectionServiceActivity
     */
    private void sendCallRequestToActivity(String action, Bundle extras) {
        Intent intent = new Intent(action);
        Bundle intentExtras = extras;
        if (intentExtras == null) {
            intentExtras = new Bundle();
        }
        switch (action) {
            /*
            case Constants.ACTION_OUTGOING_CALL:
                Uri address = connection.getAddress();
                extras.putString(OUTGOING_CALL_ADDRESS, address.toString());
                intent.putExtras(extras);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                break;
                */
            case Constants.ACTION_DISCONNECT_CALL:
                intentExtras.putInt("Reason", DisconnectCause.LOCAL);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtras(intentExtras);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                break;
            case Constants.ACTION_DTMF_SEND:
                String d = connection.getExtras().getString(Constants.DTMF);
                intentExtras.putString(Constants.DTMF, connection.getExtras().getString(Constants.DTMF));
                intent.putExtras(intentExtras);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                break;
            case Constants.ACTION_INCOMING_CALL_RECEIVED:
            case Constants.ACTION_ANSWER_CALL:
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            default:
                break;
        }
    }

}
