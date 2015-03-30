package com.ksy.media.widget;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ksy.media.widget.MediaPlayerBaseControllerView.OnGuestureChangeListener;
import com.ksy.mediaPlayer.widget.R;

public class MediaPlayerVolumeView extends RelativeLayout {

    private Context mContext;
    
    private static final int MAX_LEVEL = 4;
    
    private static final int DEFAULT_TIMEOUT = 1000;
    private static final int MSG_SHOW = 0;
    private static final int MSG_HIDE = MSG_SHOW + 1;
    private static final int MSG_PARAM_HIDE_NO_ANIMATION = 100;
    
    private static final int LEVEL_VOLUME = 100;
    
    private ImageView mIvVolumeState;
    private TextView mTvVolumeProgress;
    
    private Animation mAnimationHide;
    private OnGuestureChangeListener mOnGuestureChangeListener;
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
            case MSG_HIDE:
                clearAnimation();
                if(msg.arg1 == MSG_PARAM_HIDE_NO_ANIMATION){
                    setVisibility(View.GONE);
                }else{
                    startAnimation(mAnimationHide);
                }
                break;
            case MSG_SHOW:
                if(null!=mOnGuestureChangeListener){
                    mOnGuestureChangeListener.onLightChanged();
                }
                setVisibility(View.VISIBLE);
                break;
            default:
                break;
            }
        }
    };
    
    public MediaPlayerVolumeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public MediaPlayerVolumeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MediaPlayerVolumeView(Context context) {
        super(context);
        init(context);
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mIvVolumeState = (ImageView) findViewById(R.id.iv_volume_status);
        mTvVolumeProgress = (TextView) findViewById(R.id.tv_volume_progress);
    }
    
    private void init(Context context){
        
        this.mContext = context;
        View rootView = LayoutInflater.from(mContext).inflate(R.layout.media_player_volume_view, null);
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        addView(rootView, params);
        
        mAnimationHide = new AlphaAnimation(1, 0.5f);
        mAnimationHide.setInterpolator(new AccelerateInterpolator());
        mAnimationHide.setDuration(300);
        mAnimationHide.setAnimationListener(new AnimationListener() {
            
            @Override
            public void onAnimationStart(Animation animation) {
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
            
            @Override
            public void onAnimationEnd(Animation animation) {
                setVisibility(View.GONE);
            }
        });
        
    }
    
    // 记录一次有效手势滑动的总距离,改值是由于多次的delta累加而成,要大于一个基础阀值,才能真正实现效果
    private float mTotalDeltaVolumeDistance = 0;
    private float mTotalLastDeltaVolumePercentage = 0;
    public void onGestureVolumeChange(float deltaVolumeDistance, float totalVolumeDistance, AudioManager audioManager){
        
        mTotalDeltaVolumeDistance = mTotalDeltaVolumeDistance + deltaVolumeDistance;
        float minVolumeDistance = totalVolumeDistance / LEVEL_VOLUME;
        float minVolumePercentage = (float) 1 / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (Math.abs(mTotalDeltaVolumeDistance) >= minVolumeDistance) {
            
            float deltaVolumePercentage = mTotalDeltaVolumeDistance / totalVolumeDistance;
            mTotalLastDeltaVolumePercentage = mTotalLastDeltaVolumePercentage + deltaVolumePercentage;
            
            int curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            float curVolumePercentage = (float) curVolume / maxVolume;
            
            int newVolume = curVolume;
            float newVolumePercentage = curVolumePercentage + mTotalLastDeltaVolumePercentage;
            
            if(mTotalLastDeltaVolumePercentage > 0 && mTotalLastDeltaVolumePercentage > minVolumePercentage){
                mTotalLastDeltaVolumePercentage = 0;
                newVolume++;
            }
            else if(mTotalLastDeltaVolumePercentage < 0 && mTotalLastDeltaVolumePercentage < -minVolumePercentage){
                mTotalLastDeltaVolumePercentage = 0;
                newVolume--;
            }
            
            if(newVolume < 0){
                newVolume = 0;
            }
            else if(newVolume > maxVolume){
                newVolume = maxVolume;
            }
            
            if(newVolumePercentage < 0){
                newVolumePercentage = 0.0f;
            }
            else if(newVolumePercentage > 1){
                newVolumePercentage = 1.0f;
            }
            
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
            performVolumeChange(newVolumePercentage);
            
            mTotalDeltaVolumeDistance = 0;
            
        }
        
    }
    
    public void onGestureVolumeFinish(){
        
        mTotalDeltaVolumeDistance = 0;
        mTotalLastDeltaVolumePercentage = 0;
        
    }
    
    private void performVolumeChange(float volumePercentage) {
        
        int level = 0;
        if (volumePercentage == 0.0f) {
            level = 0;
        } else if (volumePercentage <= 0.5f) {
            level = 1;
        } else if (volumePercentage < 1.0f) {
            level = 2;
        } else if (volumePercentage == 1.0f){
            level = 3;
        }
        
        mIvVolumeState.setImageLevel(level);
        mTvVolumeProgress.setText(((int)(volumePercentage*100)) + "%");

        show();

    }
    
    private void show(){
        show(DEFAULT_TIMEOUT);
    }
    
    private void show(int timeMs){
        
        mHandler.sendEmptyMessage(MSG_SHOW);
        
        mHandler.removeMessages(MSG_HIDE);
        
        if(timeMs > 0){
            Message msgHide = mHandler.obtainMessage(MSG_HIDE);
            msgHide.arg1 = MSG_PARAM_HIDE_NO_ANIMATION;
            mHandler.sendMessageDelayed(msgHide, timeMs);
        }
        
    }
    
    public void hide(boolean now){
        
        Message msgHide = mHandler.obtainMessage(MSG_HIDE);
        if(now){
            msgHide.arg1 = MSG_PARAM_HIDE_NO_ANIMATION;
        }
        mHandler.sendMessage(msgHide);
        
    }
    
    public boolean isShowing(){
        return (getVisibility() == View.VISIBLE ? true : false);
    }
    public void setOnGuestureChangeListener(OnGuestureChangeListener onGuestureChangeListener) {
        this.mOnGuestureChangeListener = onGuestureChangeListener;
    }
    
}
