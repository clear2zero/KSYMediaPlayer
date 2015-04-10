package com.ksy.media.demo;

import java.io.File;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.ksy.media.player.util.Constants;
import com.ksy.media.widget.MediaPlayerView;

public class VideoPlayerActivity extends Activity implements MediaPlayerView.PlayerViewCallback {

	MediaPlayerView playerView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// 设置全屏
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_player);
		playerView = (MediaPlayerView) findViewById(R.id.player_view);
		startPlayer();
	}

	@Override
	protected void onResume() {

		super.onResume();
		playerView.onResume();
	}

	@Override
	protected void onPause() {

		super.onPause();
		playerView.onPause();
	}

	@Override
	protected void onDestroy() {

		super.onDestroy();
		playerView.onDestroy();
	}

	private void startPlayer() {

		playerView.setPlayerViewCallback(this);
		// String path = "rtmp://192.168.135.185/myLive/guoli1234";
		// String path = "http://live.3gv.ifeng.com/zixun.m3u8"; // vod
		// String path = "rtmp://192.168.135.185/myLive/guoyankai"; // with drm
		// sec
		// playerView.play(path);
		// String path = "http://www.modrails.com/videos/passenger_nginx.mov";
		File file = new File(Environment.getExternalStorageDirectory(), "a.mp4");
		playerView.play(file.getPath());
	}

	@Override
	public void hideViews() {

	}

	@Override
	public void restoreViews() {

	}

	@Override
	public void onPrepared() {

	}

	@Override
	public void onQualityChanged() {

	}

	@Override
	public void onFinish(int playMode) {

		Log.i(Constants.LOG_TAG, "activity on finish ===========");
		// this.onBackPressed();
		this.finish();
	}

	@Override
	public void onError(int errorCode, String errorMsg) {

		// TODO Auto-generated method stub

	}
}
