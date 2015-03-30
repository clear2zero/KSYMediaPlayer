package com.ksy.media.widget;

import android.content.Context;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.ksy.mediaPlayer.widget.R;

public class MediaPlayerControllerVolumeView extends RelativeLayout implements OnClickListener{
    
    private static final int MAX_PROGRESS = 100;
    
    private AudioManager mAudioManager;
    private MediaPlayerVolumeSeekBar mSeekBarVolumeProgress;
    private ImageView mMuteIv;
    
    private Callback mCallback;
    
    private volatile boolean isShowUpdateProgress = false;
    
    private boolean isChangedFromOnKeyChange = false;
    private int mOldVolume = 0;
    public MediaPlayerControllerVolumeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public MediaPlayerControllerVolumeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MediaPlayerControllerVolumeView(Context context) {
        super(context);
        init(context);
    }

    
    private void init(Context context){
        View root = LayoutInflater.from(context).inflate(R.layout.media_player_controller_volume_view, this);
        
        mSeekBarVolumeProgress = (MediaPlayerVolumeSeekBar) root.findViewById(R.id.seekbar_volume_progress);
        mSeekBarVolumeProgress.setMax(MAX_PROGRESS);
        mSeekBarVolumeProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                
            }
            
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                
                if(isShowUpdateProgress){
                    isShowUpdateProgress = false;
                    return;
                }
                if(isChangedFromOnKeyChange){
                    isChangedFromOnKeyChange = false;
                    return;
                }
                
                float percentage = (float) progress / seekBar.getMax();
                
                if(percentage < 0 || percentage > 1)
                    return;
                
                if(mAudioManager != null){
                    
                    int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    int newVolume = (int) (percentage * maxVolume);
                    setVolume(newVolume);
                    if(mCallback != null){
                        mCallback.onVolumeProgressChanged(mAudioManager, percentage);
                    }
                    
                }
                
            }
        });
        
        mMuteIv = (ImageView)root.findViewById(R.id.controller_volume_iv);
        mMuteIv.setOnClickListener(this);
        
    }
    
    private void setVolume(int volume){
    	if(null != mAudioManager){
    		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    		if(volume == 0){
    			mMuteIv.setSelected(true);
    		}else{
    			mMuteIv.setSelected(false);
    		}
    	}
    }
    
    private int getVolume(){
    	if(null != mAudioManager){
    		return mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    	}
    	return 0;
    }
    
    public void update(AudioManager audioManager){
        
        mAudioManager = audioManager;
    	isChangedFromOnKeyChange = false;
        // 目前存在bug:手动调用setProgress thumb会移动至初始位置
        // 每次show的时候,更新下最新的Volume进度
        // 但是因为seekbar max值和audioManager max值存在很大差距,可能会导致show完后会跳跃比较大间距
        if(mAudioManager != null){
            updateSeekBarVolumeProgress();
            isShowUpdateProgress = true;
        }
    }
    
    private void updateSeekBarVolumeProgress(){
        int curVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        
        float percentage = (float) curVolume / maxVolume;
        
        final int progress = (int) (percentage * mSeekBarVolumeProgress.getMax());
        mSeekBarVolumeProgress.post(new Runnable() {
            @Override
            public void run() {
                mSeekBarVolumeProgress.setProgress(progress);
            }
        });
    }
    
    public void setCallback(Callback callback){
        mCallback = callback;
    }
    
    public interface Callback{
        void onVolumeProgressChanged(AudioManager audioManager, float percentage);
    }
    
    private int getOldVolume(){
    	if(mOldVolume == 0){
    		mOldVolume = 1;
    	}
    	return mOldVolume;
    }
    
	@Override
	public void onClick(View v) {
		if(v == mMuteIv){
			if(mMuteIv.isSelected()){
				setVolume(getOldVolume());
			}else{
				mOldVolume = getVolume();
				setVolume(0);
			}
			isChangedFromOnKeyChange = true;
			updateSeekBarVolumeProgress();
		}
	}
	
	
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int eventAction = event.getAction();
		int keyCode = event.getKeyCode();
		if(eventAction == KeyEvent.ACTION_DOWN 
                && (keyCode == KeyEvent.KEYCODE_VOLUME_UP//音量+
                ||keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)){//音量-
            isChangedFromOnKeyChange = true;
            updateSeekBarVolumeProgress();
            return true;
        }
		return super.dispatchKeyEvent(event);
	}
    
}
