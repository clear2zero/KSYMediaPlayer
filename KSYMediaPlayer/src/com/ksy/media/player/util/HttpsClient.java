package com.ksy.media.player.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;

public class HttpsClient {

	private static final AllowAllHostnameVerifier HOSTNAME_VERIFIER = new AllowAllHostnameVerifier();
	private static X509TrustManager xtm = new X509TrustManager() {

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) {

		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) {

		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {

			return null;
		}
	};
	private static X509TrustManager[] xtmArray = new X509TrustManager[] { xtm };
	private static HttpsURLConnection conn = null;

	public InputStream sendPOSTRequestForInputStream(String path, Map<String, String> params, String encoding) throws Exception {

		StringBuilder entityBuilder = new StringBuilder("");
		if (params != null && !params.isEmpty()) {
			for (Map.Entry<String, String> entry : params.entrySet()) {
				entityBuilder.append(entry.getKey()).append('=');
				entityBuilder.append(URLEncoder.encode(entry.getValue(), encoding));
				entityBuilder.append('&');
			}
			entityBuilder.deleteCharAt(entityBuilder.length() - 1);
		}
		byte[] entity = entityBuilder.toString().getBytes();
		URL url = new URL(path);
		conn = (HttpsURLConnection) url.openConnection();
		if (conn instanceof HttpsURLConnection) {
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(new KeyManager[0], xtmArray, new SecureRandom());
			SSLSocketFactory socketFactory = context.getSocketFactory();
			conn.setSSLSocketFactory(socketFactory);
			conn.setHostnameVerifier(HOSTNAME_VERIFIER);
		}
		conn.setConnectTimeout(5 * 1000);
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setRequestProperty("Content-Length", String.valueOf(entity.length));
		OutputStream outStream = conn.getOutputStream();
		outStream.write(entity);
		outStream.flush();
		outStream.close();
		if (conn.getResponseCode() == 200) {
			return conn.getInputStream();
		}
		return conn.getInputStream();
	}

	public static void closeConnection() {

		if (conn != null)
			conn.disconnect();
	}
}