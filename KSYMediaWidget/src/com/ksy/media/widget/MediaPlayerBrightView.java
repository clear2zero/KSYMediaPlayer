package com.ksy.media.widget;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ksy.media.widget.MediaPlayerBaseControllerView.OnGuestureChangeListener;
import com.ksy.mediaPlayer.widget.R;

public class MediaPlayerBrightView extends RelativeLayout {

    private Context mContext;

    private static final int MAX_LEVEL = 5;

    private static final int DEFAULT_TIMEOUT = 1000;
    private static final int MSG_SHOW = 0;
    private static final int MSG_HIDE = MSG_SHOW + 1;
    private static final int MSG_PARAM_HIDE_NO_ANIMATION = 100;

    private static final int MAX_BRIGNTNESS = 255;
    private static final int LEVEL_BRIGNTNESS = 100;

    private ImageView mIvLightState;
    private TextView mTvLightProgress;

    private Animation mAnimationHide;
    private OnGuestureChangeListener mOnGuestureChangeListener;
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
            case MSG_HIDE:
                clearAnimation();
                if (msg.arg1 == MSG_PARAM_HIDE_NO_ANIMATION) {
                    setVisibility(View.GONE);
                } else {
                    startAnimation(mAnimationHide);
                }
                break;
            case MSG_SHOW:
                if (null != mOnGuestureChangeListener) {
                    mOnGuestureChangeListener.onLightChanged();
                }
                setVisibility(View.VISIBLE);
                break;
            default:
                break;
            }
        }
    };

    public MediaPlayerBrightView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public MediaPlayerBrightView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MediaPlayerBrightView(Context context) {
        super(context);
        init(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mIvLightState = (ImageView) findViewById(R.id.iv_light_status);
        mTvLightProgress = (TextView) findViewById(R.id.tv_light_progress);
    }

    private void init(Context context) {
        this.mContext = context;
        View rootView = LayoutInflater.from(mContext).inflate(R.layout.media_player_bright_view, null);
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
    private float mTotalDeltaBrintnessDistance = 0;

    public void onGestureLightChange(float deltaBrightnessDistance, float totalBrightnessDistance, Window window) {

        mTotalDeltaBrintnessDistance = mTotalDeltaBrintnessDistance + deltaBrightnessDistance;
        float minBringhtnessDistance = totalBrightnessDistance / LEVEL_BRIGNTNESS;
        if (Math.abs(mTotalDeltaBrintnessDistance) >= minBringhtnessDistance) {
            float deltaLightPercentage = mTotalDeltaBrintnessDistance / totalBrightnessDistance;
            onLightChange(deltaLightPercentage, window);
            mTotalDeltaBrintnessDistance = 0;
        }

    }

    public void onGestureLightFinish() {

        mTotalDeltaBrintnessDistance = 0;

    }

    private void onLightChange(float deltaLightPercentage, Window window) {

        if (Math.abs(deltaLightPercentage) > 1 || window == null) {
            return;
        }

        WindowManager.LayoutParams params = window.getAttributes();

        if (params.screenBrightness == WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
            params.screenBrightness = getScreenBrightness(mContext);
        }

        params.screenBrightness = params.screenBrightness + deltaLightPercentage;

        if (params.screenBrightness > 1.0f) {
            params.screenBrightness = 1.0f;
        }
        if (params.screenBrightness <= 0.01f) {
            params.screenBrightness = 0.01f;
        }

        window.setAttributes(params);

        performLightChange(params.screenBrightness);

    }

    private void performLightChange(float lightPercentage) {

        int level = 0;
        if (lightPercentage <= 0.01f) {
            level = 0;
        } else if (lightPercentage <= 0.25f) {
            level = 1;
        } else if (lightPercentage <= 0.5f) {
            level = 2;
        } else if (lightPercentage < 1.0f) {
            level = 3;
        } else if (lightPercentage == 1.0f) {
            level = 4;
        }

        mIvLightState.setImageLevel(level);
        mTvLightProgress.setText(((int) (lightPercentage * 100)) + "%");

        show();

    }

    private float getScreenBrightness(Context context) {
        float result = 1;
        int value = 0;
        ContentResolver cr = context.getContentResolver();
        try {
            value = Settings.System.getInt(cr,
                    Settings.System.SCREEN_BRIGHTNESS);
        } catch (SettingNotFoundException e) {
            e.printStackTrace();
        }
        result = (float) value / MAX_BRIGNTNESS;
        return result;
    }

    private void show() {
        show(DEFAULT_TIMEOUT);
    }

    private void show(int timeMs) {

        mHandler.sendEmptyMessage(MSG_SHOW);

        mHandler.removeMessages(MSG_HIDE);

        if (timeMs > 0) {
            Message msgHide = mHandler.obtainMessage(MSG_HIDE);
            msgHide.arg1 = MSG_PARAM_HIDE_NO_ANIMATION;
            mHandler.sendMessageDelayed(msgHide, timeMs);
        }

    }

    public void hide(boolean now) {

        Message msgHide = mHandler.obtainMessage(MSG_HIDE);
        if (now) {
            msgHide.arg1 = MSG_PARAM_HIDE_NO_ANIMATION;
        }
        mHandler.sendMessage(msgHide);

    }

    public boolean isShowing() {
        return (getVisibility() == View.VISIBLE ? true : false);
    }

    public void setOnGuestureChangeListener(OnGuestureChangeListener onGuestureChangeListener) {
        this.mOnGuestureChangeListener = onGuestureChangeListener;
    }

}
