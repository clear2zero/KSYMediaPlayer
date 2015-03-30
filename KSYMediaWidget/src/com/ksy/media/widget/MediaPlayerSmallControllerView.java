package com.ksy.media.widget;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.ksy.media.data.MediaPlayMode;
import com.ksy.mediaPlayer.widget.R;

public class MediaPlayerSmallControllerView extends MediaPlayerBaseControllerView implements View.OnClickListener{
    
    private RelativeLayout mControllerTopView;
    private RelativeLayout mBackLayout;
    private TextView mTitleTextView;
    
    private RelativeLayout mControllerBottomView;
    private MediaPlayerVideoSeekBar mSeekBar;
    private ImageView mPlaybackImageView;
    private ImageView mScreenModeImageView;
    
    public MediaPlayerSmallControllerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MediaPlayerSmallControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MediaPlayerSmallControllerView(Context context) {
        super(context);
        mLayoutInflater.inflate(R.layout.media_player_controller_small, this);
        initViews();
        initListeners();
    }
    
    @Override
    protected void initViews(){
        
        mControllerTopView = (RelativeLayout) findViewById(R.id.controller_top_layout);
        mBackLayout = (RelativeLayout) findViewById(R.id.back_layout);
        mTitleTextView = (TextView) findViewById(R.id.title_text_view);
        
        mControllerBottomView = (RelativeLayout) findViewById(R.id.controller_bottom_layout);
        mSeekBar = (MediaPlayerVideoSeekBar) findViewById(R.id.seekbar_video_progress);
        mPlaybackImageView = (ImageView) findViewById(R.id.video_playback_image_view);
        mScreenModeImageView = (ImageView) findViewById(R.id.video_fullscreen_image_view);
        
        mSeekBar.setMax(MediaPlayerBaseControllerView.MAX_VIDEO_PROGRESS);
        mSeekBar.setProgress(0);
        
    }
    
    @Override
    protected void initListeners(){
        
        mBackLayout.setOnClickListener(this);
        mTitleTextView.setOnClickListener(this);
        mPlaybackImageView.setOnClickListener(this);
        mScreenModeImageView.setOnClickListener(this);
        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                
                mVideoProgressTrackingTouch = false;
                
                int curProgress = seekBar.getProgress();
                int maxProgress = seekBar.getMax();
                
                if(curProgress > 0 && curProgress <= maxProgress){
                    float percentage = ((float) curProgress) / maxProgress;
                    int position = (int) (mMediaPlayerController.getDuration() * percentage);
                    mMediaPlayerController.seekTo(position);
                    mMediaPlayerController.start();
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mVideoProgressTrackingTouch = true;
            }
            
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                
                if(fromUser){
                    if(isShowing()){
                        show();
                    }
                }
                
            }
        });
        
    }
    
   
    
    @Override
    void onTimerTicker() {

    	long currentTime = mMediaPlayerController.getCurrentPosition();
        long durationTime = mMediaPlayerController.getDuration();
        
        if(durationTime > 0 && currentTime <= durationTime){
            float percentage = ((float) currentTime) / durationTime;
            updateVideoProgress(percentage);
        }
        
    }
    
    @Override
    void onShow() {
        mControllerTopView.setVisibility(View.VISIBLE);
        mControllerBottomView.setVisibility(View.VISIBLE);
    }

    @Override
    void onHide() {       
        mControllerTopView.setVisibility(View.INVISIBLE);
        mControllerBottomView.setVisibility(View.INVISIBLE);       
    }
    
    
    public void updateVideoTitle(String title){
        if(!TextUtils.isEmpty(title)){
            mTitleTextView.setText(title);
        }
    }
    
    public void updateVideoProgress(float percentage){
        if(percentage >= 0 && percentage <= 1){
            int progress = (int) (percentage * mSeekBar.getMax());
            if(!mVideoProgressTrackingTouch)
                mSeekBar.setProgress(progress);
        }
    }
    
    public void updateVideoPlaybackState(boolean isStart){
        // 播放中
        if(isStart){
            mPlaybackImageView.setImageResource(R.drawable.player_controller_pause);
            if(mMediaPlayerController.canPause()){
                mPlaybackImageView.setEnabled(true);
            }else{
                mPlaybackImageView.setEnabled(false);
            }
        }
        // 未播放
        else{
            mPlaybackImageView.setImageResource(R.drawable.player_controller_play);
            if(mMediaPlayerController.canStart()){
                mPlaybackImageView.setEnabled(true);
            }else{
                mPlaybackImageView.setEnabled(false);
            }
        }
    }
    
    @Override
    public void onClick(View v) {
        
        int id = v.getId();
        
        if(id == mBackLayout.getId() || id == mTitleTextView.getId()){
            
            mMediaPlayerController.onBackPress(MediaPlayMode.PLAYMODE_WINDOW);
            
        }
        else if(id == mPlaybackImageView.getId()){
            
            if(mMediaPlayerController.isPlaying()){
                mMediaPlayerController.pause();
                show(0);
            }
            else if(!mMediaPlayerController.isPlaying()){
                mMediaPlayerController.start();
                show();
            }
            
        }
        else if(id == mScreenModeImageView.getId()){
            mMediaPlayerController.onRequestPlayMode(MediaPlayMode.PLAYMODE_FULLSCREEN);
        }
        
    }
    
}
