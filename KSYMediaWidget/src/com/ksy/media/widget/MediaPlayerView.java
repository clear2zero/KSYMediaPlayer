package com.ksy.media.widget;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.ksy.media.data.MediaPlayMode;
import com.ksy.media.data.MediaPlayerUtils;
import com.ksy.media.data.MediaPlayerVideoQuality;
import com.ksy.media.data.NetReceiver;
import com.ksy.media.data.NetReceiver.NetState;
import com.ksy.media.data.NetReceiver.NetStateChangedListener;
import com.ksy.media.data.WakeLocker;
import com.ksy.media.player.IMediaPlayer;
import com.ksy.media.player.util.Constants;
import com.ksy.mediaPlayer.widget.R;

public class MediaPlayerView extends RelativeLayout {
	private static final int QUALITY_BEST = 100;
	private static final String CAPUTRE_SCREEN_PATH = "KSY_SDK_SCREENSHOT";
	private Activity mActivity;
	private LayoutInflater mLayoutInflater;
	private Window mWindow;

	private ViewGroup mRootView;
	private MediaPlayerVideoView mMediaPlayerVideoView;
	private MediaPlayerLargeControllerView mMediaPlayerControllerViewLarge;
	private MediaPlayerSmallControllerView mMediaPlayerControllerViewSmall;
	private MediaPlayerBufferingView mMediaPlayerBufferingView;
	private MediaPlayerLoadingView mMediaPlayerLoadingView;
	private MediaPlayerEventActionView mMediaPlayerCoverView;

	private PlayerViewCallback mPlayerViewCallback;

	private final int ORIENTATION_UNKNOWN = -2;
	private final int ORIENTATION_HORIZON = -1;
	private final int ORIENTATION_PORTRAIT_NORMAL = 0;
	private final int ORIENTATION_LANDSCAPE_REVERSED = 90;
	private final int ORIENTATION_PORTRAIT_REVERSED = 180;
	private final int ORIENTATION_LANDSCAPE_NORMAL = 270;

	private volatile boolean mNeedGesture = true;
	private volatile boolean mNeedLightGesture = true;
	private volatile boolean mNeedVolumeGesture = true;
	private volatile boolean mNeedSeekGesture = true;

	private volatile int mScreenOrientation = ORIENTATION_UNKNOWN;
	private volatile int mPlayMode = MediaPlayMode.PLAYMODE_FULLSCREEN;
	private volatile boolean mLockMode = false;
	private volatile boolean mScreenLockMode = false;

	private boolean mVideoReady = false;
	private boolean mStartAfterPause = false;

	private int mPausePosition = 0;

	private OrientationEventListener mOrientationEventListener;

	private android.view.ViewGroup.LayoutParams mLayoutParamWindowMode;
	private android.view.ViewGroup.LayoutParams mLayoutParamFullScreenMode;

	private RelativeLayout.LayoutParams mMediaPlayerControllerViewLargeParams;
	private RelativeLayout.LayoutParams mMediaPlayerControllerViewSmallParams;

	private volatile boolean mWindowActived = false; // 标识window窗口是否处于激活状态

	private boolean mDeviceNaturalOrientationLandscape;
	private boolean mCanLayoutSystemUI;
	private boolean mDeviceNavigationBarExist;
	private int mFullScreenNavigationBarHeight;
	private int mDeviceNavigationType = MediaPlayerUtils.DEVICE_NAVIGATION_TYPE_UNKNOWN;
	private int mDisplaySizeMode = MediaPlayerMovieRatioView.MOVIE_RATIO_MODE_16_9;
	private GlobleScreenShoot screenshot;

	private NetReceiver mNetReceiver;
	private NetStateChangedListener mNetChangedListener;

	private float mCurrentPlayingRatio = 1f;
	public final static  float MAX_PLAYING_RATIO = 4f;
	public MediaPlayerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs, defStyle);
	}

	public MediaPlayerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs, -1);
	}

	public MediaPlayerView(Context context) {
		super(context);
		init(context, null, -1);
	}

	private void init(Context context, AttributeSet attrs, int defStyle)
			throws IllegalArgumentException, NullPointerException {

		if (null == context)
			throw new NullPointerException("Context can not be null !");
		screenshot = new GlobleScreenShoot(context);
		TypedArray typedArray = context.obtainStyledAttributes(attrs,
				R.styleable.PlayerView);
		int playmode = typedArray.getInt(R.styleable.PlayerView_playmode,
				MediaPlayMode.PLAYMODE_FULLSCREEN);
		if (playmode == 0) {
			this.mPlayMode = MediaPlayMode.PLAYMODE_FULLSCREEN;
		} else if (playmode == 1) {
			this.mPlayMode = MediaPlayMode.PLAYMODE_WINDOW;
		}
		this.mLockMode = typedArray.getBoolean(R.styleable.PlayerView_lockmode,
				false);
		typedArray.recycle();

		this.mLayoutInflater = LayoutInflater.from(context);
		this.mActivity = (Activity) context;
		this.mWindow = mActivity.getWindow();

		this.setBackgroundColor(Color.BLACK);
		this.mDeviceNavigationBarExist = MediaPlayerUtils
				.hasNavigationBar(mWindow);
		this.mDeviceNaturalOrientationLandscape = (MediaPlayerUtils
				.getDeviceNaturalOrientation(mWindow) == MediaPlayerUtils.DEVICE_NATURAL_ORIENTATION_LANDSCAPE ? true
				: false);
		this.mCanLayoutSystemUI = Build.VERSION.SDK_INT >= 16 ? true : false;
		if (mDeviceNavigationBarExist
				&& MediaPlayerUtils.isFullScreenMode(mPlayMode)) {
			this.mFullScreenNavigationBarHeight = MediaPlayerUtils
					.getNavigationBarHeight(mWindow);
			this.mDeviceNavigationType = MediaPlayerUtils
					.getDeviceNavigationType(mWindow);
		}

		this.mRootView = (ViewGroup) mLayoutInflater.inflate(
				R.layout.media_player_view, null);
		this.mMediaPlayerVideoView = (MediaPlayerVideoView) mRootView
				.findViewById(R.id.ks_camera_video_view);
		this.mMediaPlayerBufferingView = (MediaPlayerBufferingView) mRootView
				.findViewById(R.id.ks_camera_buffering_view);
		this.mMediaPlayerLoadingView = (MediaPlayerLoadingView) mRootView
				.findViewById(R.id.ks_camera_loading_view);
		this.mMediaPlayerCoverView = (MediaPlayerEventActionView) mRootView
				.findViewById(R.id.ks_camera_covering_view);
		this.mMediaPlayerControllerViewLarge = (MediaPlayerLargeControllerView) mRootView
				.findViewById(R.id.media_player_controller_view_large);
		this.mMediaPlayerControllerViewSmall = (MediaPlayerSmallControllerView) mRootView
				.findViewById(R.id.media_player_controller_view_small);

		this.mMediaPlayerVideoView.setOnPreparedListener(mOnPreparedListener);
		this.mMediaPlayerVideoView
				.setOnBufferingUpdateListener(mOnPlaybackBufferingUpdateListener);
		this.mMediaPlayerVideoView
				.setOnCompletionListener(mOnCompletionListener);
		this.mMediaPlayerVideoView.setOnInfoListener(mOnInfoListener);
		this.mMediaPlayerVideoView.setOnErrorListener(mOnErrorListener);
		this.mMediaPlayerVideoView.setOnSurfaceListener(mOnSurfaceListener);
		this.mMediaPlayerVideoView
				.setMediaPlayerController(mMediaPlayerController);
		RelativeLayout.LayoutParams MediaPlayerVideoViewParams = new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		MediaPlayerVideoViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);

		RelativeLayout.LayoutParams MediaPlayerBufferingViewParams = new LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		MediaPlayerBufferingViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);
		this.mMediaPlayerBufferingView.hide();

		RelativeLayout.LayoutParams MediaPlayerLoadingViewParams = new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		MediaPlayerLoadingViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);
		this.mMediaPlayerLoadingView.hide();

		RelativeLayout.LayoutParams mediaPlayerCoverViewParams = new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		mediaPlayerCoverViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);
		this.mMediaPlayerCoverView
				.setCallback(new MediaPlayerEventActionView.EventActionViewCallback() {

					@Override
					public void onActionPlay() {
						Log.i(Constants.LOG_TAG, "cover view action play");
						mMediaPlayerCoverView.hide();
						mMediaPlayerLoadingView.show();
						mMediaPlayerVideoView.setVideoPath(url);
					}

					@Override
					public void onActionReplay() {
						Log.i(Constants.LOG_TAG, "cover view action replay");
						mMediaPlayerCoverView.hide();
						if (mMediaPlayerController != null) {
							mMediaPlayerController.start();
						} else {
							mMediaPlayerVideoView.start();
						}
					}

					@Override
					public void onActionError() {
						Log.i(Constants.LOG_TAG, "cover view action error");
						mMediaPlayerCoverView.hide();
						mMediaPlayerControllerViewLarge.hide();
						mMediaPlayerControllerViewSmall.hide();
						mMediaPlayerLoadingView.show();
						mMediaPlayerVideoView.setVideoPath(url);
					}

					@Override
					public void onActionBack() {
						Log.i(Constants.LOG_TAG, "cover view action back");
						mMediaPlayerController.onBackPress(mPlayMode);
					}
				});
		this.mMediaPlayerCoverView.show();

		// 初始化:ControllerViewLarge
		this.mMediaPlayerControllerViewLarge
				.setMediaPlayerController(mMediaPlayerController);
		this.mMediaPlayerControllerViewLarge.setHostWindow(mWindow);
		this.mMediaPlayerControllerViewLarge
				.setDeviceNavigationBarExist(mDeviceNavigationBarExist);
		this.mMediaPlayerControllerViewLarge
				.setNeedGestureDetector(mNeedGesture);
		this.mMediaPlayerControllerViewLarge.setNeedGestureAction(
				mNeedLightGesture, mNeedVolumeGesture, mNeedSeekGesture);
		this.mMediaPlayerControllerViewLargeParams = new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		this.mMediaPlayerControllerViewLargeParams
				.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		this.mMediaPlayerControllerViewLargeParams
				.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		if (mDeviceNavigationBarExist && mCanLayoutSystemUI
				&& mFullScreenNavigationBarHeight > 0) {
			if (mDeviceNavigationType == MediaPlayerUtils.DEVICE_NAVIGATION_TYPE_HANDSET) {
				mMediaPlayerControllerViewLargeParams.rightMargin = mFullScreenNavigationBarHeight;
			} else if (mDeviceNavigationType == MediaPlayerUtils.DEVICE_NAVIGATION_TYPE_TABLET) {
				mMediaPlayerControllerViewLargeParams.bottomMargin = mFullScreenNavigationBarHeight;
			}
		}

		this.mMediaPlayerControllerViewSmall
				.setMediaPlayerController(mMediaPlayerController);
		this.mMediaPlayerControllerViewSmall.setHostWindow(mWindow);
		this.mMediaPlayerControllerViewSmall
				.setDeviceNavigationBarExist(mDeviceNavigationBarExist);
		this.mMediaPlayerControllerViewSmall.setNeedGestureDetector(true);
		this.mMediaPlayerControllerViewSmall.setNeedGestureAction(false, false,
				false);
		this.mMediaPlayerControllerViewSmallParams = new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

		removeAllViews();
		mRootView.removeView(mMediaPlayerVideoView);
		mRootView.removeView(mMediaPlayerBufferingView);
		mRootView.removeView(mMediaPlayerLoadingView);
		mRootView.removeView(mMediaPlayerCoverView);
		mRootView.removeView(mMediaPlayerControllerViewLarge);
		mRootView.removeView(mMediaPlayerControllerViewSmall);

		addView(mMediaPlayerVideoView, MediaPlayerVideoViewParams);
		addView(mMediaPlayerBufferingView, MediaPlayerBufferingViewParams);
		addView(mMediaPlayerLoadingView, MediaPlayerLoadingViewParams);
		addView(mMediaPlayerCoverView, mediaPlayerCoverViewParams);
		if (MediaPlayerUtils.isFullScreenMode(mPlayMode)) {
			addView(mMediaPlayerControllerViewLarge,
					mMediaPlayerControllerViewLargeParams);
			mMediaPlayerControllerViewLarge.hide();
			mWindow.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
					| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
		} else if (MediaPlayerUtils.isWindowMode(mPlayMode)) {
			addView(mMediaPlayerControllerViewSmall,
					mMediaPlayerControllerViewSmallParams);
			mMediaPlayerControllerViewSmall.hide();
		}

		post(new Runnable() {

			@Override
			public void run() {

				if (MediaPlayerUtils.isWindowMode(mPlayMode)) {
					mLayoutParamWindowMode = getLayoutParams();
				}

				try {
					@SuppressWarnings("unchecked")
					Class<? extends LayoutParams> parentLayoutParamClazz = (Class<? extends LayoutParams>) getLayoutParams()
							.getClass();
					Constructor<? extends LayoutParams> constructor = parentLayoutParamClazz
							.getDeclaredConstructor(int.class, int.class);
					mLayoutParamFullScreenMode = (android.view.ViewGroup.LayoutParams) constructor
							.newInstance(
									android.view.ViewGroup.LayoutParams.MATCH_PARENT,
									android.view.ViewGroup.LayoutParams.MATCH_PARENT);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}

			}
		});

		initOrientationEventListener(context);

		mNetReceiver = NetReceiver.getInstance();
		mNetChangedListener = new NetStateChangedListener() {

			@Override
			public void onNetStateChanged(NetState netCode) {
				switch (netCode) {

				case NET_NO:
					Log.i(Constants.LOG_TAG, "网络断了");
					Toast.makeText(getContext(), "网络变化了:没有网络连接",
							Toast.LENGTH_LONG).show();
					break;
				case NET_2G:
					Log.i(Constants.LOG_TAG, "2g网络");
					Toast.makeText(getContext(), "网络变化了:2g网络",
							Toast.LENGTH_LONG).show();
					break;
				case NET_3G:
					Log.i(Constants.LOG_TAG, "3g网络");
					Toast.makeText(getContext(), "网络变化了:3g网络",
							Toast.LENGTH_LONG).show();
					break;
				case NET_4G:
					Log.i(Constants.LOG_TAG, "4g网络");
					Toast.makeText(getContext(), "网络变化了:4g网络",
							Toast.LENGTH_LONG).show();
					break;
				case NET_WIFI:
					Log.i(Constants.LOG_TAG, "WIFI网络");
					Toast.makeText(getContext(), "网络变化了:WIFI网络",
							Toast.LENGTH_LONG).show();
					break;

				case NET_UNKNOWN:
					Log.i(Constants.LOG_TAG, "未知网络");
					Toast.makeText(getContext(), "网络变化了:未知网络",
							Toast.LENGTH_LONG).show();
					break;
				default:
					Log.i(Constants.LOG_TAG, "不知道什么情况~>_<~");
					Toast.makeText(getContext(), "网络变化了:不知道什么情况~>_<~",
							Toast.LENGTH_LONG).show();
				}
			}
		};
	}

	private String url = null;

	public void play(String path) {
		if (this.mMediaPlayerVideoView != null) {
			// if(mNeedAutoPlay)
			// this.mMediaPlayerVideoView.setVideoPath(path);
			url = path;
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {

		if (mMediaPlayerCoverView.isShowing()) {
			return mMediaPlayerCoverView.dispatchTouchEvent(ev);
		}

		if (mVideoReady && !mMediaPlayerCoverView.isShowing()) {

			if (MediaPlayerUtils.isFullScreenMode(mPlayMode)) {
				return mMediaPlayerControllerViewLarge.dispatchTouchEvent(ev);
			}
			if (MediaPlayerUtils.isWindowMode(mPlayMode)) {
				return mMediaPlayerControllerViewSmall.dispatchTouchEvent(ev);
			}
		}

		return true;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return super.onTouchEvent(event);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_DOWN) {
			if (mScreenLockMode) {
				return true;
			}
			if (MediaPlayerUtils.isFullScreenMode(mPlayMode)) {
				if (mLockMode) {
					if (mPlayerViewCallback != null)
						mPlayerViewCallback.onFinish(mPlayMode);
				} else {
					mMediaPlayerController
							.onRequestPlayMode(MediaPlayMode.PLAYMODE_WINDOW);
				}
				return true;
			} else if (MediaPlayerUtils.isWindowMode(mPlayMode)) {

				if (mPlayerViewCallback != null)
					mPlayerViewCallback.onFinish(mPlayMode);
				return true;
			}

		} else if (event.getKeyCode() == KeyEvent.KEYCODE_MENU
				|| event.getKeyCode() == KeyEvent.KEYCODE_SEARCH) {
			if (mScreenLockMode) {
				return true;
			}
		}
		return false;
	}

	public void setPlayerViewCallback(PlayerViewCallback callback) {
		this.mPlayerViewCallback = callback;
	}

	public int getPlayMode() {
		return this.mPlayMode;
	}

	private boolean requestPlayMode(int requestPlayMode) {

		if (mPlayMode == requestPlayMode)
			return false;

		// 请求全屏模式
		if (MediaPlayerUtils.isFullScreenMode(requestPlayMode)) {

			if (mLayoutParamFullScreenMode == null)
				return false;

			removeView(mMediaPlayerControllerViewSmall);
			addView(mMediaPlayerControllerViewLarge,
					mMediaPlayerControllerViewLargeParams);
			this.setLayoutParams(mLayoutParamFullScreenMode);
			mMediaPlayerControllerViewLarge.hide();
			mMediaPlayerControllerViewSmall.hide();

			if (mPlayerViewCallback != null)
				mPlayerViewCallback.hideViews();

			mWindow.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
					| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
			if (mDeviceNavigationBarExist)
				MediaPlayerUtils.hideSystemUI(mWindow, true);

			mPlayMode = requestPlayMode;
			return true;

		}
		// 请求窗口模式
		else if (MediaPlayerUtils.isWindowMode(requestPlayMode)) {

			if (mLayoutParamWindowMode == null)
				return false;

			removeView(mMediaPlayerControllerViewLarge);
			addView(mMediaPlayerControllerViewSmall,
					mMediaPlayerControllerViewSmallParams);
			this.setLayoutParams(mLayoutParamWindowMode);
			mMediaPlayerControllerViewLarge.hide();
			mMediaPlayerControllerViewSmall.hide();

			if (mPlayerViewCallback != null)
				mPlayerViewCallback.restoreViews();

			mWindow.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
					| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
			if (mDeviceNavigationBarExist)
				MediaPlayerUtils.showSystemUI(mWindow, false);

			mPlayMode = requestPlayMode;
			return true;

		}

		return false;

	}

	public void onResume() {
		mWindowActived = true;

		enableOrientationEventListener();
		mNetReceiver.registNetBroadCast(getContext());
		mNetReceiver.addNetStateChangeListener(mNetChangedListener);
		if (mStartAfterPause) {
			mMediaPlayerController.start();
			mStartAfterPause = false;
		}
	}

	public void onPause() {
		mNetReceiver.remoteNetStateChangeListener(mNetChangedListener);
		mNetReceiver.unRegistNetBroadCast(getContext());
		mWindowActived = false;
		mPausePosition = mMediaPlayerController.getCurrentPosition();

		disableOrientationEventListener();

		if (mMediaPlayerController.isPlaying()) {
			mMediaPlayerController.pause();
			mStartAfterPause = true;
		}
		WakeLocker.release();
	}

	public void onDestroy() {
	}

	private void initOrientationEventListener(Context context) {

		if (null == context)
			return;

		if (null == mOrientationEventListener) {
			mOrientationEventListener = new OrientationEventListener(context,
					SensorManager.SENSOR_DELAY_NORMAL) {
				@Override
				public void onOrientationChanged(int orientation) {
					int preScreenOrientation = mScreenOrientation;
					mScreenOrientation = convertAngle2Orientation(orientation);
					if (mScreenLockMode)
						return;
					if (!mWindowActived)
						return;

					if (preScreenOrientation == ORIENTATION_UNKNOWN)
						return;
					if (mScreenOrientation == ORIENTATION_UNKNOWN)
						return;
					if (mScreenOrientation == ORIENTATION_HORIZON)
						return;

					if (preScreenOrientation != mScreenOrientation) {
						if (!MediaPlayerUtils.checkSystemGravity(getContext()))
							return;
						if (MediaPlayerUtils.isWindowMode(mPlayMode)) {
							Log.i(Constants.LOG_TAG, " Window to FullScreen ");
							if (mScreenOrientation == ORIENTATION_LANDSCAPE_NORMAL
									|| mScreenOrientation == ORIENTATION_LANDSCAPE_REVERSED) {
								if (!mLockMode) {
									boolean requestResult = requestPlayMode(MediaPlayMode.PLAYMODE_FULLSCREEN);
									if (requestResult) {
										doScreenOrientationRotate(mScreenOrientation);
									}
								}
							}
						} else if (MediaPlayerUtils.isFullScreenMode(mPlayMode)) {
							Log.i(Constants.LOG_TAG, " Full Screen to Window");
							if (mScreenOrientation == ORIENTATION_PORTRAIT_NORMAL) {
								if (!mLockMode) {
									boolean requestResult = requestPlayMode(MediaPlayMode.PLAYMODE_WINDOW);
									if (requestResult) {
										doScreenOrientationRotate(mScreenOrientation);
									}
								}
							} else if (mScreenOrientation == ORIENTATION_LANDSCAPE_NORMAL
									|| mScreenOrientation == ORIENTATION_LANDSCAPE_REVERSED) {
								doScreenOrientationRotate(mScreenOrientation);
							}
						}
					}
				}
			};
			enableOrientationEventListener();
		}

	}

	private int convertAngle2Orientation(int angle) {

		int screentOrientation = ORIENTATION_HORIZON;

		if ((angle >= 315 && angle <= 359) || (angle >= 0 && angle < 45)) {
			screentOrientation = ORIENTATION_PORTRAIT_NORMAL;
			if (mDeviceNaturalOrientationLandscape) {
				screentOrientation = ORIENTATION_LANDSCAPE_NORMAL;
			}
		} else if (angle >= 45 && angle < 135) {
			screentOrientation = ORIENTATION_LANDSCAPE_REVERSED;
			if (mDeviceNaturalOrientationLandscape) {
				screentOrientation = ORIENTATION_PORTRAIT_NORMAL;
			}
		} else if (angle >= 135 && angle < 225) {
			screentOrientation = ORIENTATION_PORTRAIT_REVERSED;
			if (mDeviceNaturalOrientationLandscape) {
				screentOrientation = ORIENTATION_LANDSCAPE_REVERSED;
			}
		} else if (angle >= 225 && angle < 315) {
			screentOrientation = ORIENTATION_LANDSCAPE_NORMAL;
			if (mDeviceNaturalOrientationLandscape) {
				screentOrientation = ORIENTATION_PORTRAIT_REVERSED;
			}
		}

		return screentOrientation;

	}

	private void doScreenOrientationRotate(int screenOrientation) {

		switch (screenOrientation) {
		case ORIENTATION_PORTRAIT_NORMAL:
			mActivity
					.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			break;
		case ORIENTATION_LANDSCAPE_REVERSED:
			mActivity
					.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
			if (mDeviceNavigationBarExist
					&& mFullScreenNavigationBarHeight <= 0
					&& MediaPlayerUtils.isFullScreenMode(mPlayMode)) {
				this.mFullScreenNavigationBarHeight = MediaPlayerUtils
						.getNavigationBarHeight(mWindow);
				this.mDeviceNavigationType = MediaPlayerUtils
						.getDeviceNavigationType(mWindow);
				if (mCanLayoutSystemUI && mFullScreenNavigationBarHeight > 0) {
					if (mDeviceNavigationType == MediaPlayerUtils.DEVICE_NAVIGATION_TYPE_HANDSET) {
						mMediaPlayerControllerViewLargeParams.rightMargin = mFullScreenNavigationBarHeight;
					} else if (mDeviceNavigationType == MediaPlayerUtils.DEVICE_NAVIGATION_TYPE_TABLET) {
						mMediaPlayerControllerViewLargeParams.bottomMargin = mFullScreenNavigationBarHeight;
					}
				}
			}
			break;
		case ORIENTATION_PORTRAIT_REVERSED:
			mActivity
					.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
			break;
		case ORIENTATION_LANDSCAPE_NORMAL:
			mActivity
					.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			if (mDeviceNavigationBarExist
					&& mFullScreenNavigationBarHeight <= 0
					&& MediaPlayerUtils.isFullScreenMode(mPlayMode)) {
				this.mFullScreenNavigationBarHeight = MediaPlayerUtils
						.getNavigationBarHeight(mWindow);
				this.mDeviceNavigationType = MediaPlayerUtils
						.getDeviceNavigationType(mWindow);
				if (mCanLayoutSystemUI && mFullScreenNavigationBarHeight > 0) {
					if (mDeviceNavigationType == MediaPlayerUtils.DEVICE_NAVIGATION_TYPE_HANDSET) {
						mMediaPlayerControllerViewLargeParams.rightMargin = mFullScreenNavigationBarHeight;
					} else if (mDeviceNavigationType == MediaPlayerUtils.DEVICE_NAVIGATION_TYPE_TABLET) {
						mMediaPlayerControllerViewLargeParams.bottomMargin = mFullScreenNavigationBarHeight;
					}
				}
			}
			break;
		}

	}

	private void enableOrientationEventListener() {
		if (mOrientationEventListener != null
				&& mOrientationEventListener.canDetectOrientation()) {
			mOrientationEventListener.enable();
		}
	}

	private void disableOrientationEventListener() {
		if (mOrientationEventListener != null) {
			mOrientationEventListener.disable();
			mScreenOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
		}
	}

	private void updateVideoInfo2Controller() {
		mMediaPlayerControllerViewSmall.updateVideoTitle("金山软件大厦-3楼云存储");

		mMediaPlayerControllerViewLarge.updateVideoTitle("金山软件大厦-3楼云存储");
		mMediaPlayerControllerViewLarge
				.updateVideoQualityState(MediaPlayerVideoQuality.SD);
		mMediaPlayerControllerViewLarge.updateVideoVolumeState();

		mMediaPlayerCoverView.updateVideoTitle("金山软件大厦-3楼云存储");
	}

	private void changeMovieRatio() {
		if (mDisplaySizeMode > MediaPlayerMovieRatioView.MOVIE_RATIO_MODE_ORIGIN) {
			mDisplaySizeMode = MediaPlayerMovieRatioView.MOVIE_RATIO_MODE_16_9;
		}
		Log.d(Constants.LOG_TAG, "Change Current Width/Heigh Ratio = "
				+ mDisplaySizeMode);
		mMediaPlayerVideoView.setVideoLayout(mDisplaySizeMode);
		mDisplaySizeMode++;
	}

	/************************************************************
	 * IMediaPlayer Callback
	 ************************************************************/
	IMediaPlayer.OnPreparedListener mOnPreparedListener = new IMediaPlayer.OnPreparedListener() {

		@Override
		public void onPrepared(IMediaPlayer mp) {
			int duration = 0;
			if (mMediaPlayerController != null)
				duration = mMediaPlayerController.getDuration();

			if (mPausePosition > 0 && duration > 0) {
				mMediaPlayerController.seekTo(mPausePosition);
				mPausePosition = 0;
			}
			if (!WakeLocker.isScreenOn(getContext())
					&& mMediaPlayerController.canPause()) {
				mMediaPlayerController.pause();
			}
			updateVideoInfo2Controller();
			mMediaPlayerLoadingView.hide();
			mMediaPlayerCoverView.hide();
			mVideoReady = true;
			if (mPlayerViewCallback != null)
				mPlayerViewCallback.onPrepared();
			if (mMediaPlayerController != null) {
				mMediaPlayerController.start();
			} else {
				mMediaPlayerVideoView.start();
			}
		}
	};

	IMediaPlayer.OnCompletionListener mOnCompletionListener = new IMediaPlayer.OnCompletionListener() {

		@Override
		public void onCompletion(IMediaPlayer mp) {
			Log.i(Constants.LOG_TAG, "================onCompletion============");
			mMediaPlayerControllerViewLarge.hide();
			mMediaPlayerControllerViewSmall.hide();
			mMediaPlayerCoverView.updateCoverMode(
					MediaPlayerEventActionView.EVENT_ACTION_VIEW_MODE_COMPLETE,
					null);
			mMediaPlayerCoverView.show();
			WakeLocker.release();
		}

	};

	IMediaPlayer.OnInfoListener mOnInfoListener = new IMediaPlayer.OnInfoListener() {

		@Override
		public boolean onInfo(IMediaPlayer mp, int what, int extra) {
			switch (what) {
			// 视频缓冲开始
			case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
				Log.i(Constants.LOG_TAG, "MEDIA_INFO_BUFFERING_START");
				mMediaPlayerBufferingView.show();
				break;
			// 视频缓冲结束
			case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
				Log.i(Constants.LOG_TAG, "MEDIA_INFO_BUFFERING_END");
				mMediaPlayerBufferingView.hide();
				break;
			default:
				break;
			}
			return true;
		}
	};

	IMediaPlayer.OnBufferingUpdateListener mOnPlaybackBufferingUpdateListener = new IMediaPlayer.OnBufferingUpdateListener() {

		@Override
		public void onBufferingUpdate(IMediaPlayer mp, int percent) {
			// Log.i(Constants.LOG_TAG, "precent :" + percent);
			if (percent > 0 && percent <= 100) {
				mMediaPlayerBufferingView.setBufferingProgress(percent);
			} else {
				mMediaPlayerBufferingView.setBufferingProgress(0);
			}

		}
	};

	IMediaPlayer.OnErrorListener mOnErrorListener = new IMediaPlayer.OnErrorListener() {

		@Override
		public boolean onError(IMediaPlayer mp, int what, int extra) {
			Log.i(Constants.LOG_TAG, "On Native Error,what :" + what + " , extra :" + extra);
			mMediaPlayerControllerViewLarge.hide();
			mMediaPlayerControllerViewSmall.hide();
			mMediaPlayerCoverView.updateCoverMode(
					MediaPlayerEventActionView.EVENT_ACTION_VIEW_MODE_ERROR,
					what + "," + extra);
			mMediaPlayerCoverView.show();
			return true;
		}
	};

	IMediaPlayer.OnSurfaceListener mOnSurfaceListener = new IMediaPlayer.OnSurfaceListener() {

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			mVideoReady = false;
			mMediaPlayerControllerViewLarge.hide();
			mMediaPlayerControllerViewSmall.hide();
			mMediaPlayerBufferingView.hide();
			mMediaPlayerLoadingView.setLoadingTip("loading...");
			mMediaPlayerLoadingView.show();
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int w,
				int h) {
		}
	};

	public interface PlayerViewCallback {
		void hideViews();

		void restoreViews();

		void onPrepared();

		void onQualityChanged();

		void onFinish(int playMode);

		void onError(int errorCode, String errorMsg);
	}

	private MediaPlayerBaseControllerView.MediaPlayerController mMediaPlayerController = new MediaPlayerBaseControllerView.MediaPlayerController() {

		private Bitmap bitmap;

		@Override
		public void start() {
			if (canStart()) {
				mMediaPlayerVideoView.start();
				mMediaPlayerControllerViewLarge.updateVideoPlaybackState(true);
				mMediaPlayerControllerViewSmall.updateVideoPlaybackState(true);
				WakeLocker.acquire(getContext());
			} else {
				mMediaPlayerControllerViewLarge.updateVideoPlaybackState(false);
				mMediaPlayerControllerViewSmall.updateVideoPlaybackState(false);
			}
		}

		@Override
		public void pause() {
			if (canPause()) {
				mMediaPlayerVideoView.pause();
				mMediaPlayerControllerViewLarge.updateVideoPlaybackState(false);
				mMediaPlayerControllerViewSmall.updateVideoPlaybackState(false);
				WakeLocker.release();
			}

		}

		@Override
		public int getDuration() {
			return mMediaPlayerVideoView.getDuration();
		}

		@Override
		public int getCurrentPosition() {
			return mMediaPlayerVideoView.getCurrentPosition();
		}

		@Override
		public void seekTo(long pos) {
			if (canSeekBackward() && canSeekForward()) {
				mMediaPlayerVideoView.seekTo(pos);
			} else {
				Toast.makeText(getContext(),
						"current is real stream, seek is unSupported !",
						Toast.LENGTH_SHORT).show();
			}
		}

		@Override
		public boolean isPlaying() {
			return mMediaPlayerVideoView.isPlaying();
		}

		@Override
		public int getBufferPercentage() {
			return mMediaPlayerVideoView.getBufferPercentage();
		}

		@Override
		public boolean canPause() {
			return mMediaPlayerVideoView.canPause();
		}

		@Override
		public boolean canSeekBackward() {
			return mMediaPlayerVideoView.canSeekBackward();
		}

		@Override
		public boolean canSeekForward() {
			return mMediaPlayerVideoView.canSeekForward();
		}

		@Override
		public boolean supportQuality() {
			return true;
		}

		@Override
		public boolean supportVolume() {
			return true;
		}

		@Override
		public boolean playVideo(String url) {
			mMediaPlayerVideoView.setVideoPath(url);
			return true;
		}

		@Override
		public int getPlayMode() {
			return mPlayMode;
		}

		@Override
		public void onRequestPlayMode(int requestPlayMode) {
			if (mPlayMode == requestPlayMode)
				return;
			if (mLockMode)
				return;
			// 请求全屏模式
			if (MediaPlayerUtils.isFullScreenMode(requestPlayMode)) {
				boolean requestResult = requestPlayMode(requestPlayMode);
				if (requestResult) {
					doScreenOrientationRotate(ORIENTATION_LANDSCAPE_NORMAL);
				}
			}
			// 请求窗口模式
			else if (MediaPlayerUtils.isWindowMode(requestPlayMode)) {
				boolean requestResult = requestPlayMode(requestPlayMode);
				if (requestResult) {
					doScreenOrientationRotate(ORIENTATION_PORTRAIT_NORMAL);
				}
			}
		}

		@Override
		public void onBackPress(int playMode) {
			Log.i(Constants.LOG_TAG,
					"========playerview back pressed ==============playMode :"
							+ playMode + ", mPlayerViewCallback is null "
							+ (mPlayerViewCallback == null));
			if (MediaPlayerUtils.isFullScreenMode(playMode)) {
				if (mLockMode) {
					if (mPlayerViewCallback != null)
						mPlayerViewCallback.onFinish(playMode);
				} else {
					mMediaPlayerController
							.onRequestPlayMode(MediaPlayMode.PLAYMODE_WINDOW);
				}
			} else if (MediaPlayerUtils.isWindowMode(playMode)) {
				if (mPlayerViewCallback != null)
					mPlayerViewCallback.onFinish(playMode);
			}
		}

		@Override
		public void onControllerShow(int playMode) {
		}

		@Override
		public void onControllerHide(int playMode) {
		}

		@Override
		public void onRequestLockMode(boolean lockMode) {
			if (mScreenLockMode != lockMode) {
				mScreenLockMode = lockMode;

				// 加锁:屏幕操作锁
				if (mScreenLockMode) {
				}
				// 解锁:屏幕操作锁
				else {
				}
			}
		}

		@Override
		public void onVideoPreparing() {
		}

		@Override
		public boolean canStart() {
			return mMediaPlayerVideoView.canStart();
		}

		@Override
		public void onPlay() {

		}

		@Override
		public void onPause() {

		}

		@Override
		public void onMovieRatioChange() {
			changeMovieRatio();
		}

		@Override
		public void onMoviePlayRatioUp() {
			Log.d(Constants.LOG_TAG, "speed up");
			if(mMediaPlayerController != null && mMediaPlayerController.isPlaying()){
				if(mCurrentPlayingRatio == MAX_PLAYING_RATIO){
					Log.d(Constants.LOG_TAG, "current playing ratio is max");
					return ;
				}else{
					mCurrentPlayingRatio = mCurrentPlayingRatio + 0.5f;
					mMediaPlayerVideoView.setVideoRate(mCurrentPlayingRatio);
					Log.d(Constants.LOG_TAG, "set playing ratio to --->" + mCurrentPlayingRatio);
				}
			}
			
			Log.d(Constants.LOG_TAG, "current video is not playing , set ratio unsupported");
			
		}

		@Override
		public void onMoviePlayRatioDown() {
			if(mMediaPlayerController != null && mMediaPlayerController.isPlaying()){
				if(mCurrentPlayingRatio == 0){
					Log.d(Constants.LOG_TAG, "current playing ratio is 0");
					return ;
				}else{
					mCurrentPlayingRatio = mCurrentPlayingRatio - 0.5f;
					mMediaPlayerVideoView.setVideoRate(mCurrentPlayingRatio);
					Log.d(Constants.LOG_TAG, "set playing ratio to --->" + mCurrentPlayingRatio);
					return;
				}
			}
			
			Log.d(Constants.LOG_TAG, "current video is not playing , set ratio unsupported");
		}

		@Override
		public void onMovieCrop() {
			screenshot.takeScreenshot(mMediaPlayerVideoView, new Runnable() {
				@Override
				public void run() {

				}
			}, false, false);

			bitmap = Bitmap.createBitmap(mMediaPlayerVideoView.getVideoWidth(),
					mMediaPlayerVideoView.getVideoHeight(), Config.ARGB_8888);
			if (bitmap != null) {
				mMediaPlayerVideoView.getCurrentFrame(bitmap);
				compressAndSaveBitmapToSDCard(bitmap, getCurrentTime(),
						MediaPlayerView.QUALITY_BEST);
			} else {
				Log.d(Constants.LOG_TAG, "bitmap is null");
			}

		}

		@Override
		public void onVolumeDown() {
			mMediaPlayerVideoView.setAudioAmplify(0.5f);
		}

		@Override
		public void onVolumeUp() {
			mMediaPlayerVideoView.setAudioAmplify(1.5f);
		}
	};

	private String getCurrentTime() {
		StringBuffer buffer = new StringBuffer();
		SimpleDateFormat sDateFormat = new SimpleDateFormat(
				"yyyy-MM-dd_hh:mm:ss", Locale.US);
		buffer.append(sDateFormat.format(new java.util.Date())).append(".")
				.append("png");
		return buffer.toString();
	}

	private void compressAndSaveBitmapToSDCard(Bitmap rawBitmap,
			String fileName, int quality) {
		File directory = new File(Environment.getExternalStorageDirectory()
				+ File.separator + MediaPlayerView.CAPUTRE_SCREEN_PATH);
		if (!directory.exists()) {
			directory.mkdir();
		}
		File saveFile = new File(directory, fileName);
		if (!saveFile.exists()) {
			try {
				FileOutputStream fileOutputStream = new FileOutputStream(
						saveFile);
				if (fileOutputStream != null) {
					rawBitmap.compress(Bitmap.CompressFormat.PNG, quality,
							fileOutputStream);
				}
				fileOutputStream.flush();
				fileOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();

			}
		} else {
			Log.d(Constants.LOG_TAG, "too frequently screen shot");
		}
	}

}
