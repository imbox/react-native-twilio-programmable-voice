package com.hoxfon.react.RNTwilioVoice;

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

import java.util.HashMap;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

@RequiresApi(api = Build.VERSION_CODES.M)
public class VoiceConnectionService extends ConnectionService {
    private static final String TAG = "VoiceConnectionService";

    private static Connection connection;

    public static Connection getConnection() {
        return connection;
    }

    public static void deinitConnection() {
        connection = null;
    }

    @Override
    public Connection onCreateIncomingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        Connection incomingCallConnection = createConnection(request);
        incomingCallConnection.setRinging();
        return incomingCallConnection;
    }

    @Override
    public Connection onCreateOutgoingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        Connection outgoingCallConnection = createConnection(request);
        outgoingCallConnection.setDialing();
        return outgoingCallConnection;
    }

    private Connection createConnection(ConnectionRequest request) {
        connection = new Connection() {

            @Override
            public void onStateChanged(int state) {
                if (state == Connection.STATE_DIALING) {
                    final Handler handler = new Handler();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            sendCallRequestToActivity(Constants.ACTION_OUTGOING_CALL);
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
                        sendCallRequestToActivity(Constants.ACTION_DTMF_SEND);
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
                        sendCallRequestToActivity(Constants.ACTION_DISCONNECT_CALL);
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
                        sendCallRequestToActivity(Constants.ACTION_ANSWER_CALL);
                    }
                });
            }

            @Override
            public void onReject() {
                super.onReject();
                Log.d(TAG, "onReject pressed");
                sendCallRequestToActivity(Constants.ACTION_REJECT_CALL);
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
                        sendCallRequestToActivity(Constants.ACTION_HOLD_CALL);
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
                        sendCallRequestToActivity(Constants.ACTION_UNHOLD_CALL);
                    }
                });
            }

            @Override
            public void onPostDialContinue(boolean proceed) {
                super.onPostDialContinue(true);
            }
        };
        connection.setConnectionCapabilities(Connection.CAPABILITY_MUTE);
        connection.setConnectionCapabilities(Connection.CAPABILITY_SUPPORT_HOLD);
        /*
        if (request.getExtras().getString(CALLEE) == null) {
            connection.setAddress(request.getAddress(), TelecomManager.PRESENTATION_ALLOWED);
        } else {
            connection.setAddress(Uri.parse(request.getExtras().getString(CALLEE)), TelecomManager.PRESENTATION_ALLOWED);
        }
        s */
        connection.setDialing();
        connection.setExtras(request.getExtras());
        return connection;
    }
    
    /*
     * Send call request to the VoiceConnectionServiceActivity
     */
    private void sendCallRequestToActivity(String action) {
        Intent intent = new Intent(action);
        Bundle extras = new Bundle();
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
                extras.putInt("Reason", DisconnectCause.LOCAL);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtras(extras);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                break;
            case Constants.ACTION_DTMF_SEND:
                String d = connection.getExtras().getString(Constants.DTMF);
                extras.putString(Constants.DTMF, connection.getExtras().getString(Constants.DTMF));
                intent.putExtras(extras);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                break;
            case Constants.ACTION_ANSWER_CALL:
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            default:
                break;
        }
    }

}
