package com.android.webcrawler;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.webcrawler.dao.mem.MemDbHelperFactory;

public class MainActivity extends Activity implements OnClickListener {

	private static final int DEFAULT_THROTTLE = 4;
	private static final int MAX_THROTTLE = 60_000;
	private static final long REFRESH_MIN_DELAY = 500;

	private LinearLayout crawlingInfo;
	private Button startButton;
	private EditText urlInputView;
	private EditText throttleInputView;
	private TextView progressText;

	private volatile WebCrawler crawler;
	private volatile long lastRefresh;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		crawlingInfo = (LinearLayout) findViewById(R.id.crawlingInfo);
		startButton = (Button) findViewById(R.id.start);
		urlInputView = (EditText) findViewById(R.id.webUrl);
		throttleInputView = (EditText) findViewById(R.id.throttle);
		progressText = (TextView) findViewById(R.id.progressText);
	}


	/**
	 * Callback for handling button onclick events
	 */
	@Override
	public void onClick(View v) {
		int viewId = v.getId();
		switch (viewId) {
		case R.id.start:
			if (crawler == null) {
				startCrawling();
			} else {
				stopCrawling();
			}
			break;
		}
	}

	private void startCrawling() {
		if (crawler != null) {
			return;
		}
		String webUrl = urlInputView.getText().toString();
		if (TextUtils.isEmpty(webUrl)) {
			webUrl = null;
		}

		int throttle;
		String throttleText = throttleInputView.getText().toString();
		if (TextUtils.isEmpty(throttleText)) {
			throttle = DEFAULT_THROTTLE;
		} else {
			try {
				throttle = Integer.parseInt(throttleText);
			} catch (Exception e) {
				Log.wtf(Constant.TAG, String.format("Failed to parse [throttleText=%s]", throttleText), e);
				throttle = MAX_THROTTLE;
			}
		}
		if (throttle > MAX_THROTTLE) {
			Log.w(Constant.TAG, String.format("Throttle value is to high, set maximum value [throttle=%s, MAX_THROTTLE=%s]", throttle, MAX_THROTTLE));
			throttle = MAX_THROTTLE;
		}

		startButton.setText("Stop Crawling");
		progressText.setText("Running...");
		crawlingInfo.setVisibility(View.VISIBLE);

		Configuration configuration = new Configuration();
		startCrawler(webUrl, throttle, configuration );
	}

	private void startCrawler(final String webUrl, final int throttle, final Configuration configuration) {
		final Context ctx = this;
		final CrawlingCallback callback = new CrawlingCallback() {
			@Override
			public void onPageCrawlingCompleted(String url) {
				updateProgressText(url);
			}

			@Override
			public void onPageCrawlingFailed(String url, int errorCode) {
				updateProgressText(url);
			}

			private void updateProgressText(final String url) {
				long currentTimeMillis = System.currentTimeMillis();
				if (currentTimeMillis - lastRefresh > REFRESH_MIN_DELAY) {
					lastRefresh = currentTimeMillis;
					progressText.post(new Runnable() {
						@Override
						public void run() {
							progressText.setText(url);

						}
					});
				}
			}

			@Override
			public void onPageCrawlingFinished() {
				new AsyncTask<Object, Object, Object>() {
					@Override
					protected Object doInBackground(Object... params) {
						stopCrawling();
						startCrawling();
						return null;
					}
				}.execute();
			}
		};

		new AsyncTask<Object, Object, Object>() {
			@Override
			protected Object doInBackground(Object... params) {
				crawler = new WebCrawler(ctx, throttle, configuration, callback);
				crawler.start(webUrl);
				return null;
			}
		}.execute();
	}

	/**
	 * API to handle post crawling events
	 */
	private void stopCrawling() {
		WebCrawler oldCrawler = crawler;
		crawler = null;

		if (oldCrawler == null) {
			return;
		}
		progressText.setText("Stopping...");
		oldCrawler.stopCrawlerTasks();

		crawlingInfo.setVisibility(View.INVISIBLE);
		startButton.setText("Start Crawling");
	}

}
