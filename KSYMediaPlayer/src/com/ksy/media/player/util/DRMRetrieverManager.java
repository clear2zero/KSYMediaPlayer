package com.ksy.media.player.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class DRMRetrieverManager {

	private static final String DRM_CEK_STR = "Cek";

	private static final int RESPONSE_SUCCESS = 0;
	private static final int RESPONSE_FAIL = 1;

	private final HttpsClient mHttpsClient;
	private static DRMRetrieverManager mInstance;
	private final ExecutorService mThreadPool;
	private final DRMInnerHandler mhandler;

	class DRMInnerHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {

			int what = msg.what;
			DRMRetrieverResponseHandler handler = (DRMRetrieverResponseHandler) msg.getData().getSerializable("handler");
			switch (what) {
			case RESPONSE_SUCCESS:
				String cek = msg.getData().getString(DRM_CEK_STR);
				handler.onSuccess(200, cek);
				break;
			case RESPONSE_FAIL:
				handler.onFailure(404, "", null);
				break;
			}
		}
	}

	private DRMRetrieverManager() {

		mHttpsClient = new HttpsClient();
		mThreadPool = Executors.newCachedThreadPool();
		mhandler = new DRMInnerHandler();
	}

	public static DRMRetrieverManager getInstance() {

		synchronized (DRMRetrieverManager.class) {
			if (mInstance == null) {
				mInstance = new DRMRetrieverManager();
			}
			return mInstance;
		}
	}

	public void retrieveDRM(String url, DRMRetrieverResponseHandler handler) {

		mThreadPool.submit(new DRMRetrieverTask(url, handler), handler);
	}

	class DRMRetrieverTask implements Runnable {

		private final String url;
		private final DRMRetrieverResponseHandler handler;

		public DRMRetrieverTask(String url, DRMRetrieverResponseHandler handler) {

			this.url = url;
			this.handler = handler;
		}

		@Override
		public void run() {

			try {
				InputStream in = mHttpsClient.sendPOSTRequestForInputStream(url, null, "UTF-8");
				String cek = parseDRMSecFromInputStream(in);
				Message msg = mhandler.obtainMessage(RESPONSE_SUCCESS);
				Bundle data = new Bundle();
				data.putString(DRM_CEK_STR, cek);
				data.putSerializable("handler", handler);
				msg.setData(data);
				mhandler.sendMessage(msg);
			} catch (Exception e) {
				Message msg = mhandler.obtainMessage(RESPONSE_FAIL);
				Bundle data = new Bundle();
				data.putString("response", "");
				data.putSerializable("handler", handler);
				mhandler.sendMessage(msg);
			}
		}
	}

	private String parseDRMSecFromInputStream(InputStream inputStream) throws IOException {

		XmlPullParserFactory factory = null;
		String cek = "";
		try {
			factory = XmlPullParserFactory.newInstance();
			XmlPullParser parse = factory.newPullParser();
			parse.setInput(inputStream, "UTF-8");
			int eventType = parse.getEventType();
			while (XmlPullParser.END_DOCUMENT != eventType) {
				String nodeName = parse.getName();
				switch (eventType) {
				case XmlPullParser.START_DOCUMENT:
				case XmlPullParser.END_DOCUMENT:
					break;
				case XmlPullParser.START_TAG:
					if (DRM_CEK_STR.equalsIgnoreCase(nodeName)) {
						cek = parse.nextText();
					}
					break;
				case XmlPullParser.END_TAG:
					break;
				case XmlPullParser.TEXT:
					break;
				default:
					break;
				}
				eventType = parse.next();
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cek;

	}
}
