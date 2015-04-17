package com.ksy.media.widget;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.media.AudioManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ksy.media.data.MediaPlayMode;
import com.ksy.media.data.MediaPlayerUtils;
import com.ksy.media.data.MediaPlayerVideoQuality;
import com.ksy.media.player.util.Constants;
import com.ksy.media.widget.MediaPlayerVolumeSeekBar.onScreenShowListener;
import com.ksy.mediaPlayer.widget.R;

public class MediaPlayerLargeControllerView extends
		MediaPlayerBaseControllerView implements View.OnClickListener,
		onScreenShowListener, OnSystemUiVisibilityChangeListener {

	private RelativeLayout mControllerTopView;
	private RelativeLayout mBackLayout;
	private TextView mTitleTextView;

	private RelativeLayout mControllerBottomView;
	private ImageView mVideoPlayImageView;
	private MediaPlayerMovieRatioView mWidgetMovieRatioView;

	private ImageView mVideoSizeImageView;

	private LinearLayout mQualityLayout;
	private TextView mQualityTextView;

	private RelativeLayout mVideoProgressLayout;
	private MediaPlayerVideoSeekBar mSeekBar;
	private TextView mCurrentTimeTextView;
	private TextView mTotalTimeTextView;
	private ImageView mScreenModeImageView;

	private MediaPlayerQualityPopupView mQualityPopup;

	private MediaPlayerLockView mLockView;

	private MediaPlayerControllerVolumeView mWidgetControllerVolumeView;
	private ImageView mVideoRatioBackView;
	private ImageView mVideoRatioForwardView;
	private ImageView mVideoCropView;
	private ImageView mVideoVolumeUpView;
	private ImageView mVideoVolumeDownView;

	public MediaPlayerLargeControllerView(Context context, AttributeSet attrs,
			int defStyle) {

		super(context, attrs, defStyle);
	}

	public MediaPlayerLargeControllerView(Context context, AttributeSet attrs) {

		super(context, attrs);
	}

	public MediaPlayerLargeControllerView(Context context) {

		super(context);
		mLayoutInflater.inflate(R.layout.media_player_controller_large, this);
		initViews();
		initListeners();
	}

	@Override
	protected void initViews() {

		mControllerTopView = (RelativeLayout) findViewById(R.id.controller_top_layout);
		mBackLayout = (RelativeLayout) findViewById(R.id.back_layout);
		mTitleTextView = (TextView) findViewById(R.id.title_text_view);

		mControllerBottomView = (RelativeLayout) findViewById(R.id.controller_bottom_layout);
		mVideoPlayImageView = (ImageView) findViewById(R.id.video_start_pause_image_view);
		mVideoSizeImageView = (ImageView) findViewById(R.id.video_size_image_view);
		mVideoRatioBackView = (ImageView) findViewById(R.id.video_fast_back_view);
		mVideoRatioForwardView = (ImageView) findViewById(R.id.video_fast_forward_view);
		mVideoCropView = (ImageView) findViewById(R.id.crop_view);
		mVideoVolumeUpView = (ImageView) findViewById(R.id.volume_up_view);
		mVideoVolumeDownView = (ImageView) findViewById(R.id.volume_down_view);

		mQualityLayout = (LinearLayout) findViewById(R.id.video_quality_layout);
		mQualityTextView = (TextView) findViewById(R.id.video_quality_text_view);

		mLockView = (MediaPlayerLockView) findViewById(R.id.widget_lock_view);

		mVideoProgressLayout = (RelativeLayout) findViewById(R.id.video_progress_layout);
		mSeekBar = (MediaPlayerVideoSeekBar) findViewById(R.id.video_seekbar);
		mCurrentTimeTextView = (TextView) findViewById(R.id.video_current_time_text_view);
		mTotalTimeTextView = (TextView) findViewById(R.id.video_total_time_text_view);
		mScreenModeImageView = (ImageView) findViewById(R.id.video_window_screen_image_view);

		mSeekBar.setMax(MediaPlayerBaseControllerView.MAX_VIDEO_PROGRESS);
		mSeekBar.setProgress(0);

		mQualityPopup = new MediaPlayerQualityPopupView(getContext());

		mWidgetLightView = (MediaPlayerBrightView) findViewById(R.id.widget_light_view);
		mWidgetMovieRatioView = (MediaPlayerMovieRatioView) findViewById(R.id.widget_video_ratio_view);
		mWidgetVolumeView = (MediaPlayerVolumeView) findViewById(R.id.widget_volume_view);
		mWidgetSeekView = (MediaPlayerSeekView) findViewById(R.id.widget_seek_view);

		mWidgetControllerVolumeView = (MediaPlayerControllerVolumeView) findViewById(R.id.widget_controller_volume);
		Log.d(Constants.LOG_TAG, " listener set in L C");
		mWidgetControllerVolumeView.setOnScreenShowListener(this);
		setOnSystemUiVisibilityChangeListener(this);
	}

	@Override
	protected void initListeners() {

		mScreenModeImageView.setOnClickListener(this);
		mVideoVolumeUpView.setOnClickListener(this);
		mVideoVolumeDownView.setOnClickListener(this);
		mVideoCropView.setOnClickListener(this);
		mVideoRatioBackView.setOnClickListener(this);
		mVideoRatioForwardView.setOnClickListener(this);
		mBackLayout.setOnClickListener(this);
		mVideoPlayImageView.setOnClickListener(this);
		mQualityLayout.setOnClickListener(this);
		mTitleTextView.setOnClickListener(this);
		mVideoSizeImageView.setOnClickListener(this);
		mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

				mVideoProgressTrackingTouch = false;

				int curProgress = seekBar.getProgress();
				int maxProgress = seekBar.getMax();

				if (curProgress > 0 && curProgress <= maxProgress) {
					float percentage = ((float) curProgress) / maxProgress;
					int position = (int) (mMediaPlayerController.getDuration() * percentage);
					mMediaPlayerController.seekTo(position);
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

				mVideoProgressTrackingTouch = true;
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {

				if (fromUser) {
					if (isShowing()) {
						show();
					}
				}

			}

		});

		mQualityPopup.setCallback(new MediaPlayerQualityPopupView.Callback() {

			@Override
			public void onQualitySelected(MediaPlayerVideoQuality quality) {

				mQualityPopup.hide();
				mQualityTextView.setText(quality.getName());
				setMediaQuality(quality);
			}

			@Override
			public void onPopupViewDismiss() {

				mQualityLayout.setSelected(false);
				if (isShowing())
					show();
			}
		});

		mLockView.setCallback(new MediaPlayerLockView.ScreenLockCallback() {

			@Override
			public void onActionLockMode(boolean lock) {

				// 加锁
				if (lock) {
					mScreenLock = lock;
					mMediaPlayerController.onRequestLockMode(lock);
					show();
				}
				// 解锁
				else {
					mScreenLock = lock;
					mMediaPlayerController.onRequestLockMode(lock);
					show();
				}
			}
		});

		mWidgetControllerVolumeView
				.setCallback(new MediaPlayerControllerVolumeView.Callback() {

					@Override
					public void onVolumeProgressChanged(
							AudioManager audioManager, float percentage) {

						// TODO Auto-generated method stub
					}
				});

	}

	@Override
	void onTimerTicker() {

		long curTime = mMediaPlayerController.getCurrentPosition();
		long durTime = mMediaPlayerController.getDuration();

		if (durTime > 0 && curTime <= durTime) {
			float percentage = ((float) curTime) / durTime;
			updateVideoProgress(percentage);
		}

	}

	@Override
	void onShow() {

		mMediaPlayerController
				.onControllerShow(MediaPlayMode.PLAYMODE_FULLSCREEN);

		mLockView.show();
		// 如果开启屏幕锁后,controller显示时把其他控件隐藏,只显示出LockView
		if (mScreenLock) {
			mControllerTopView.setVisibility(View.INVISIBLE);
			mControllerBottomView.setVisibility(View.INVISIBLE);
			mVideoProgressLayout.setVisibility(View.INVISIBLE);
			mWidgetControllerVolumeView.setVisibility(View.INVISIBLE);
		} else {
			mControllerTopView.setVisibility(View.VISIBLE);
			AudioManager audioManager = (AudioManager) getContext()
					.getSystemService(Context.AUDIO_SERVICE);
			mWidgetControllerVolumeView.update(audioManager);
			mWidgetControllerVolumeView.setVisibility(View.VISIBLE);
			mControllerBottomView.setVisibility(View.VISIBLE);
			mVideoProgressLayout.setVisibility(View.VISIBLE);

		}
		if (MediaPlayerUtils.isFullScreenMode(mMediaPlayerController
				.getPlayMode())) {
			Log.d(Constants.LOG_TAG, "onShow");
//			MediaPlayerUtils.showSystemUI(mHostWindow, false);
		}
	}

	@Override
	void onHide() {

		mMediaPlayerController
				.onControllerHide(MediaPlayMode.PLAYMODE_FULLSCREEN);

		mControllerTopView.setVisibility(View.INVISIBLE);
		mControllerBottomView.setVisibility(View.INVISIBLE);
		mVideoProgressLayout.setVisibility(View.INVISIBLE);
		if (mQualityPopup.isShowing())
			mQualityPopup.hide();

		// 当前全屏模式,隐藏系统UI
		if (mDeviceNavigationBarExist) {
			if (MediaPlayerUtils.isFullScreenMode(mMediaPlayerController
					.getPlayMode())) {
				Log.d(Constants.LOG_TAG, "onHide");
				MediaPlayerUtils.hideSystemUI(mHostWindow, false);
			}
		}

		mLockView.hide();

	}

	@Override
	protected void onAttachedToWindow() {

		super.onAttachedToWindow();
	}

	@Override
	protected void onDetachedFromWindow() {

		super.onDetachedFromWindow();
	}

	@Override
	protected void onFinishInflate() {

		super.onFinishInflate();
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {

		return super.dispatchTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return super.onTouchEvent(event);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {

		return mWidgetControllerVolumeView.dispatchKeyEvent(event);
	}

	/***************************
	 * Public Method
	 ***************************/
	public void updateVideoTitle(String title) {

		if (!TextUtils.isEmpty(title)) {
			mTitleTextView.setText(title);
		}
	}

	public void updateVideoProgress(float percentage) {

		if (percentage >= 0 && percentage <= 1) {
			int progress = (int) (percentage * mSeekBar.getMax());
			if (!mVideoProgressTrackingTouch)
				mSeekBar.setProgress(progress);

			long curTime = mMediaPlayerController.getCurrentPosition();
			long durTime = mMediaPlayerController.getDuration();
			if (durTime > 0 && curTime <= durTime) {
				mCurrentTimeTextView.setText(MediaPlayerUtils
						.getVideoDisplayTime(curTime));
				mTotalTimeTextView.setText(MediaPlayerUtils
						.getVideoDisplayTime(durTime));
			}
		}
	}

	public void updateVideoPlaybackState(boolean isStart) {

		// 播放中
		Log.i(Constants.LOG_TAG, "updateVideoPlaybackState  ----> start ? "
				+ isStart);
		if (isStart) {
			mVideoPlayImageView.setSelected(true);
		}
		// 未播放
		else {
			mVideoPlayImageView.setSelected(false);
		}
	}

	public void updateVideoQualityState(MediaPlayerVideoQuality quality) {

		mQualityTextView.setText(quality.getName());
	}

	public void updateVideoVolumeState() {

	}

	@Override
	public void onClick(View v) {

		int id = v.getId();

		if (id == mBackLayout.getId() || id == mTitleTextView.getId()) {
			mMediaPlayerController
					.onBackPress(MediaPlayMode.PLAYMODE_FULLSCREEN);

		} else if (id == mVideoPlayImageView.getId()) {
			Log.i(Constants.LOG_TAG,
					"playing  ? " + (mMediaPlayerController.isPlaying()));
			if (mMediaPlayerController.isPlaying()) {
				mMediaPlayerController.pause();
				if (mScreenLock) {
					show();
				} else {
					show(0);
				}
			} else if (!mMediaPlayerController.isPlaying()) {
				mMediaPlayerController.start();
				show();
			}

		} else if (id == mQualityLayout.getId()) {

			displayQualityPopupWindow();

		} else if (id == mVideoSizeImageView.getId()) {
			mMediaPlayerController.onMovieRatioChange();
			mWidgetMovieRatioView.show();
			show();
		}

		else if (id == mVideoRatioForwardView.getId()) {
			mMediaPlayerController.onMoviePlayRatioUp();
			show();
		} else if (id == mVideoRatioBackView.getId()) {
			mMediaPlayerController.onMoviePlayRatioDown();
			show();
		} else if (id == mVideoCropView.getId()) {
			mMediaPlayerController.onMovieCrop();
			show();
		} else if (id == mVideoVolumeDownView.getId()) {
			mMediaPlayerController.onVolumeDown();
			show();
		} else if (id == mVideoVolumeUpView.getId()) {
			mMediaPlayerController.onVolumeUp();
			show();
		} else if (id == mScreenModeImageView.getId()) {
			mMediaPlayerController
					.onRequestPlayMode(MediaPlayMode.PLAYMODE_FULLSCREEN);
		}

	}

	/**
	 * 由于拖动引起的视图变化的回调
	 * 
	 */
	public interface OnGuestureChangeListener {

		/**
		 * 亮度有变化回调
		 * 
		 */
		void onLightChanged();

		/**
		 * 声音有变化回调
		 * 
		 */
		void onVolumeChanged();

		/**
		 * 播放进度有变化回调
		 * 
		 */
		void onPlayProgressChanged();
	}

	private void displayQualityPopupWindow() {

		// 弹出清晰度框
		List<MediaPlayerVideoQuality> qualityList = new ArrayList<MediaPlayerVideoQuality>();
		qualityList.add(MediaPlayerVideoQuality.UNKNOWN);
		qualityList.add(MediaPlayerVideoQuality.SD);
		qualityList.add(MediaPlayerVideoQuality.HD);
		int widthExtra = MediaPlayerUtils.dip2px(getContext(), 5);
		int width = mQualityLayout.getMeasuredWidth() + widthExtra;
		int height = (MediaPlayerUtils.dip2px(getContext(), 50) + MediaPlayerUtils
				.dip2px(getContext(), 2)) * qualityList.size();
		int x = MediaPlayerUtils.getXLocationOnScreen(mQualityLayout)
				- widthExtra / 2;
		int y = MediaPlayerUtils.getYLocationOnScreen(mQualityLayout) - height;
		mQualityPopup.show(mQualityLayout, qualityList, this.mCurrentQuality,
				x, y, width, height);
		mQualityLayout.setSelected(true);
		show(0);
	}

	@Override
	public void onScreenShow() {
		show();
	}

	@Override
	public void onSystemUiVisibilityChange(int visibility) {
		Log.d(Constants.LOG_TAG,
				"onSystemUiVisibilityChange :"+visibility);
	}
	
	@Override
	public void onWindowSystemUiVisibilityChanged(int visible) {
		Log.d(Constants.LOG_TAG,
				"onWindowSystemUiVisibilityChanged :"+visible);
	}

}
