package com.hoxfon.react.RNTwilioVoice;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;

import com.facebook.react.bridge.ReactApplicationContext;
import com.twilio.voice.CallInvite;

import java.util.List;

import static android.content.Context.ACTIVITY_SERVICE;

import static com.hoxfon.react.RNTwilioVoice.TwilioVoiceModule.TAG;

public class CallNotificationManager {

    private static final String VOICE_CHANNEL = "default";

    private NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

    public CallNotificationManager() {}

    public int getApplicationImportance(ReactApplicationContext context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        if (activityManager == null) {
            return 0;
        }
        List<ActivityManager.RunningAppProcessInfo> processInfos = activityManager.getRunningAppProcesses();
        if (processInfos == null) {
            return 0;
        }

        for (ActivityManager.RunningAppProcessInfo processInfo : processInfos) {
            if (processInfo.processName.equals(context.getApplicationInfo().packageName)) {
                return processInfo.importance;
            }
        }
        return 0;
    }

    public static Class getMainActivityClass(Context context) {
        String packageName = context.getPackageName();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        String className = launchIntent.getComponent().getClassName();
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void createIncomingCallNotification(Context context, CallInvite callInvite, int notificationId, int channelImportance) {
        Intent intent = new Intent(context, getMainActivityClass(context));
        intent.setAction(Constants.ACTION_INCOMING_CALL_NOTIFICATION);
        intent.putExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, notificationId);
        intent.putExtra(Constants.INCOMING_CALL_INVITE, callInvite);
        intent.putExtra(Constants.CALL_SID, callInvite.getCallSid());
        intent.putExtra(Constants.CALL_FROM, callInvite.getFrom());
        intent.putExtra(Constants.CALL_TO, callInvite.getTo());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        /*
         * Pass the notification id and call sid to use as an identifier to cancel the
         * notification later
         */
        Bundle extras = new Bundle();
        extras.putString(Constants.CALL_SID_KEY, callInvite.getCallSid());

        String contentText = callInvite.getFrom() + " " + context.getString(R.string.call_incoming_content);

        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = buildNotification(context,
                    contentText,
                    pendingIntent,
                    extras,
                    callInvite,
                    notificationId,
                    createIncomingCallsChannel(context, channelImportance));
        } else {
            // noinspection deprecation
            notification = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_call_white_24dp)
                    .setContentTitle(context.getString(R.string.call_incoming_title))
                    .setContentText(contentText)
                    .setAutoCancel(true)
                    .setExtras(extras)
                    .setContentIntent(pendingIntent)
                    .setGroup("test_app_notification")
                    .setColor(Color.rgb(214, 10, 37))
                    .build();
        }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(Constants.INCOMING_NOTIFICATION_ID, notification);
    }

    private static void removeIncomingCallNotification(ReactApplicationContext context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(Constants.INCOMING_NOTIFICATION_ID);
    }

    public void createMissedCallNotification(ReactApplicationContext context, String callSid, String callFrom) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "createMissedCallNotification()");
        }
        // First remove incoming call notification
        removeIncomingCallNotification(context);

        SharedPreferences sharedPref = context.getSharedPreferences(Constants.PREFERENCE_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();

        Intent intent = new Intent(context, getMainActivityClass(context));
        intent.setAction(Constants.ACTION_MISSED_CALL)
                .putExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, Constants.MISSED_CALLS_NOTIFICATION_ID)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        PendingIntent clearMissedCallsCountPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                new Intent(Constants.ACTION_CLEAR_MISSED_CALLS_COUNT)
                        .putExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, Constants.CLEAR_MISSED_CALLS_NOTIFICATION_ID),
                0
        );
        /*
         * Pass the notification id and call sid to use as an identifier to open the notification
         */
        Bundle extras = new Bundle();
        extras.putInt(Constants.INCOMING_CALL_NOTIFICATION_ID, Constants.MISSED_CALLS_NOTIFICATION_ID);
        extras.putString(Constants.CALL_SID_KEY, callSid);

        /*
         * Create the notification shown in the notification drawer
         */
        String title = context.getString(R.string.call_missed_title);
        NotificationCompat.Builder notification =
                new NotificationCompat.Builder(context, VOICE_CHANNEL)
                        .setGroup(Constants.MISSED_CALLS_GROUP)
                        .setGroupSummary(true)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setSmallIcon(R.drawable.ic_call_missed_white_24dp)
                        .setContentTitle(title)
                        .setContentText(callFrom + context.getString(R.string.call_missed_from))
                        .setAutoCancel(true)
                        .setShowWhen(true)
                        .setExtras(extras)
                        .setDeleteIntent(clearMissedCallsCountPendingIntent)
                        .setContentIntent(pendingIntent);

        int missedCalls = sharedPref.getInt(Constants.MISSED_CALLS_GROUP, 0);
        missedCalls++;
        if (missedCalls == 1) {
            inboxStyle = new NotificationCompat.InboxStyle();
            inboxStyle.setBigContentTitle(title);
        } else {
            inboxStyle.setBigContentTitle(String.valueOf(missedCalls) + " " + context.getString(R.string.call_missed_title_plural));
        }
        inboxStyle.addLine(context.getString(R.string.call_missed_more) + " " + callFrom);
        sharedPrefEditor.putInt(Constants.MISSED_CALLS_GROUP, missedCalls);
        sharedPrefEditor.commit();

        notification.setStyle(inboxStyle);

        // build notification large icon
        Resources res = context.getResources();
        int largeIconResId = res.getIdentifier("ic_launcher", "mipmap", context.getPackageName());
        Bitmap largeIconBitmap = BitmapFactory.decodeResource(res, largeIconResId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && largeIconResId != 0) {
            notification.setLargeIcon(largeIconBitmap);
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(Constants.MISSED_CALLS_NOTIFICATION_ID, notification.build());
    }

    public static void createHangupNotification(ReactApplicationContext context, String callSid, String caller) {
        // First remove incoming call notification
        removeIncomingCallNotification(context);

        Intent intent = new Intent(context, getMainActivityClass(context));
        intent.setAction(Constants.ACTION_OPEN_CALL_IN_PROGRESS)
                .putExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, Constants.HANGUP_NOTIFICATION_ID)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        PendingIntent hangupPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                new Intent(Constants.ACTION_HANGUP_CALL)
                        .putExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, Constants.HANGUP_NOTIFICATION_ID),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        Bundle extras = new Bundle();
        extras.putInt(Constants.INCOMING_CALL_NOTIFICATION_ID, Constants.HANGUP_NOTIFICATION_ID);
        extras.putString(Constants.CALL_SID_KEY, callSid);

        Notification notification;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String title = context.getString(R.string.call_in_progress);
        String actionText = context.getString(R.string.hangup);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new NotificationCompat.Builder(context, createChannel(title, notificationManager))
                    .setContentTitle(title)
                    .setContentText(caller)
                    .setSmallIcon(R.drawable.ic_call_white_24dp)
                    .setCategory(Notification.CATEGORY_CALL)
                    .setExtras(extras)
                    .setOngoing(true)
                    .setUsesChronometer(true)
                    .setFullScreenIntent(pendingIntent, true)
                    .addAction(0, actionText, hangupPendingIntent)
                    .build();
        } else {
            // noinspection deprecation
            notification = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(caller)
                    .setSmallIcon(R.drawable.ic_call_white_24dp)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setOngoing(true)
                    .setUsesChronometer(true)
                    .setExtras(extras)
                    .setContentIntent(pendingIntent)
                    .addAction(0, actionText, hangupPendingIntent)
                    .build();
        }
        notificationManager.notify(Constants.HANGUP_NOTIFICATION_ID, notification);
    }


    /**
     * Build a notification.
     *
     * @param text          the text of the notification
     * @param pendingIntent the body, pending intent for the notification
     * @param extras        extras passed with the notification
     * @return the builder
     */
    @TargetApi(Build.VERSION_CODES.O)
    private static Notification buildNotification(Context context,
                                           String text,
                                           PendingIntent pendingIntent,
                                           Bundle extras,
                                           final CallInvite callInvite,
                                           int notificationId,
                                           String channelId) {

        Intent rejectIntent = new Intent(context, getMainActivityClass(context));
        rejectIntent.setAction(Constants.ACTION_REJECT);
        rejectIntent.putExtra(Constants.INCOMING_CALL_INVITE, callInvite);
        rejectIntent.putExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, notificationId);
        NotificationCompat.Action rejectAction = new NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_delete,
                getActionText(context, R.string.reject, R.color.red),
                createActionPendingIntent(context, rejectIntent)
        ).build();

        Intent acceptIntent = new Intent(context, getMainActivityClass(context));
        acceptIntent.setAction(Constants.ACTION_ACCEPT);
        acceptIntent.putExtra(Constants.INCOMING_CALL_INVITE, callInvite);
        acceptIntent.putExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, notificationId);
        NotificationCompat.Action answerAction = new NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_call,
                getActionText(context, R.string.accept, R.color.green),
                createActionPendingIntent(context, acceptIntent)
        ).build();

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_call_white_24dp)
                        .setContentTitle(context.getString(R.string.call_incoming_title))
                        .setContentText(text)
                        .setExtras(extras)
                        .setAutoCancel(true)
                        .addAction(rejectAction)
                        .addAction(answerAction)
                        .setFullScreenIntent(pendingIntent, true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(Notification.CATEGORY_CALL)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                ;

        // build notification large icon
        Resources res = context.getResources();
        int largeIconResId = res.getIdentifier("ic_launcher", "mipmap", context.getPackageName());
        Bitmap largeIconBitmap = BitmapFactory.decodeResource(res, largeIconResId);

        if (largeIconResId != 0) {
            builder.setLargeIcon(largeIconBitmap);
        }

        return builder.build();
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static String createIncomingCallsChannel(Context context, int channelImportance) {
        String channelId = Constants.VOICE_CHANNEL_HIGH_IMPORTANCE;
        if (channelImportance == NotificationManager.IMPORTANCE_LOW) {
            channelId = Constants.VOICE_CHANNEL_LOW_IMPORTANCE;
        }
        NotificationChannel callInviteChannel = new NotificationChannel(channelId,
                "Incoming calls", channelImportance);
        callInviteChannel.setLightColor(Color.GREEN);

        // TODO set sound for background incoming call
        //Uri defaultRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE);
        //AudioAttributes audioAttributes = new AudioAttributes.Builder()
        //        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        //        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
        //        .build();
        //callInviteChannel.setSound(defaultRingtoneUri, audioAttributes);

        callInviteChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(callInviteChannel);

        return channelId;
    }

    private static Spannable getActionText(Context context, @StringRes int stringRes, @ColorRes int colorRes) {
        Spannable spannable = new SpannableString(context.getText(stringRes));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            spannable.setSpan(
                    new ForegroundColorSpan(context.getColor(colorRes)),
                    0,
                    spannable.length(),
                    0
            );
        }
        return spannable;
    }

    private static PendingIntent createActionPendingIntent(Context context, Intent intent) {
        return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
    }



    @TargetApi(Build.VERSION_CODES.O)
    private static String createChannel(String channelName, NotificationManager notificationManager) {
        String channelId = VOICE_CHANNEL;
        NotificationChannel channel = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setLightColor(Color.GREEN);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationManager.createNotificationChannel(channel);
        return channelId;
    }

    public void removeHangupNotification(ReactApplicationContext context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(Constants.HANGUP_NOTIFICATION_ID);
    }
}
