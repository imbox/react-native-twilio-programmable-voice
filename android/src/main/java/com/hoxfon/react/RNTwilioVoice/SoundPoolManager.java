package com.hoxfon.react.RNTwilioVoice;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class SoundPoolManager {

    private boolean playing = false;
    private static SoundPoolManager instance;
    private Ringtone ringtone = null;
    private Vibrator vibrator;
    private boolean shouldVibrate = false;
    private boolean shouldPlaySound = false;

    private SoundPoolManager(Context context) {
        Uri ringtoneSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        ringtone = RingtoneManager.getRingtone(context, ringtoneSound);
        AudioAttributes alarmAttribute = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        ringtone.setAudioAttributes(alarmAttribute);
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        setRingingMode(context);
    }

    public static SoundPoolManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundPoolManager(context);
        }
        return instance;
    }

    @SuppressLint("MissingPermission")
    public void playRinging() {
        if (!playing) {
            if (shouldPlaySound) {
                ringtone.play();
            }
            if (shouldVibrate) {
                long[] timings = {150, 150, 150};
                vibrator.vibrate(VibrationEffect.createWaveform(timings, 0));
            }

            playing = true;

        }

    }

    public void stopRinging() {
        if (playing) {
            ringtone.stop();
            playing = false;
            vibrator.cancel();
        }
    }

    public void playDisconnect() {
        if (!playing) {
            ringtone.stop();
            playing = false;
            vibrator.cancel();
        }
    }

    private void setRingingMode(Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int mode = am.getRingerMode();

        switch (mode) {
            case AudioManager.RINGER_MODE_NORMAL:
                this.shouldVibrate = true;
                this.shouldPlaySound = true;
                break;

            case AudioManager.RINGER_MODE_SILENT:
                this.shouldVibrate = false;
                this.shouldPlaySound = false;
                break;

            case AudioManager.RINGER_MODE_VIBRATE:
                this.shouldVibrate = true;
                this.shouldPlaySound = false;
                break;
        }
    }

}
