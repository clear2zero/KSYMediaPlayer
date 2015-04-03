
package com.ksy.media.player;

import java.io.IOException;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.view.Surface;
import android.view.SurfaceHolder;

public interface IMediaPlayer {
    public static final int MEDIA_INFO_UNKNOWN = 1;
    public static final int MEDIA_INFO_STARTED_AS_NEXT = 2;
    public static final int MEDIA_INFO_VIDEO_RENDERING_START = 3;
    public static final int MEDIA_INFO_VIDEO_TRACK_LAGGING = 700;
    public static final int MEDIA_INFO_BUFFERING_START = 701;
    public static final int MEDIA_INFO_BUFFERING_END = 702;
    public static final int MEDIA_INFO_BAD_INTERLEAVING = 800;
    public static final int MEDIA_INFO_NOT_SEEKABLE = 801;
    public static final int MEDIA_INFO_METADATA_UPDATE = 802;
    public static final int MEDIA_INFO_TIMED_TEXT_ERROR = 900;

    public static final int MEDIA_ERROR_UNKNOWN = 1;
    public static final int MEDIA_ERROR_SERVER_DIED = 100;
    public static final int MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = 200;
    public static final int MEDIA_ERROR_IO = -1004;
    public static final int MEDIA_ERROR_MALFORMED = -1007;
    public static final int MEDIA_ERROR_UNSUPPORTED = -1010;
    public static final int MEDIA_ERROR_TIMED_OUT = -110;

    public static final int MEDIA_BUFFERSIZE_DEFAULT = 2 * 10 * 1024 *1024;
    
    public static final int MEDIA_ANALYSE_DURATION_DEFAULT = 2 * 1000 ; 
    
    public abstract void setDisplay(SurfaceHolder sh);

    public abstract void setDataSource(String path) throws IOException,
            IllegalArgumentException, SecurityException, IllegalStateException;

    public abstract String getDataSource();

    public abstract void prepareAsync() throws IllegalStateException;

    public abstract void start() throws IllegalStateException;

    public abstract void stop() throws IllegalStateException;

    public abstract void pause() throws IllegalStateException;

    public abstract void setScreenOnWhilePlaying(boolean screenOn);

    public abstract int getVideoWidth();

    public abstract int getVideoHeight();

    public abstract boolean isPlaying();

    public abstract void seekTo(long msec) throws IllegalStateException;

    public abstract long getCurrentPosition();

    public abstract long getDuration();

    public abstract void release();

    public abstract void reset();

    public abstract void setVolume(float leftVolume, float rightVolume);

    public abstract MediaInfo getMediaInfo();

    public abstract void setLogEnabled(boolean enable);

    public abstract boolean isPlayable();

    public abstract void setOnPreparedListener(OnPreparedListener listener);

    public abstract void setOnCompletionListener(OnCompletionListener listener);

    public abstract void setOnBufferingUpdateListener(
            OnBufferingUpdateListener listener);

    public abstract void setOnSeekCompleteListener(
            OnSeekCompleteListener listener);

    public abstract void setOnVideoSizeChangedListener(
            OnVideoSizeChangedListener listener);

    public abstract void setOnErrorListener(OnErrorListener listener);

    public abstract void setOnInfoListener(OnInfoListener listener);

    /*--------------------
     * Listeners
     */
    public static interface OnPreparedListener {
        public void onPrepared(IMediaPlayer mp);
    }

    public static interface OnCompletionListener {
        public void onCompletion(IMediaPlayer mp);
    }

    public static interface OnBufferingUpdateListener {
        public void onBufferingUpdate(IMediaPlayer mp, int percent);
    }

    public static interface OnSeekCompleteListener {
        public void onSeekComplete(IMediaPlayer mp);
    }

    public static interface OnVideoSizeChangedListener {
        public void onVideoSizeChanged(IMediaPlayer mp, int width, int height,
                int sar_num, int sar_den);
    }

    public static interface OnErrorListener {
        public boolean onError(IMediaPlayer mp, int what, int extra);
    }

    public static interface OnInfoListener {
        public boolean onInfo(IMediaPlayer mp, int what, int extra);
    }
    
    public static interface OnSurfaceListener{
        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h);
        public void surfaceCreated(SurfaceHolder holder);
        public void surfaceDestroyed(SurfaceHolder holder);
    }
    

    /*--------------------
     * Optional
     */
    public abstract void setAudioStreamType(int streamtype);

    public abstract void setKeepInBackground(boolean keepInBackground);

    public abstract int getVideoSarNum();

    public abstract int getVideoSarDen();

    @Deprecated
    public abstract void setWakeMode(Context context, int mode);

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public abstract void setSurface(Surface surface);
    
    public abstract void setAudioAmplify(float ratio);
    
    public abstract void setVideoRate(float rate);
    
    public abstract void getPicture(Bitmap bitmap);
    
    public abstract void setBufferSize(int size);
    
    public abstract void setAnalyseDuration(int duration);
}
