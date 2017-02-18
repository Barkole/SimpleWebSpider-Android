package com.android.webcrawler;

import android.app.Activity;
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

public class MainActivity extends Activity implements OnClickListener {

	private LinearLayout crawlingInfo;
	private Button startButton;
	private EditText urlInputView;
	private EditText queueSizeInputView;
	private EditText resetTimeInputView;
	private EditText throttleInputView;
	private TextView progressText;

	private volatile WebCrawler crawler;
	private static final long HOUR_IN_MS = 60*60*1_000;
	private static final long DEFAULT_RESET_TIME = 24*HOUR_IN_MS;
	private static final int DEFAULT_QUEUE_SIZE = 1_000;
	private static final int DEFAULT_THROTTLE = 10;
	private static final int REFRESH_DELAY = 100;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		crawlingInfo = (LinearLayout) findViewById(R.id.crawlingInfo);
		startButton = (Button) findViewById(R.id.start);
		urlInputView = (EditText) findViewById(R.id.webUrl);
		queueSizeInputView = (EditText) findViewById(R.id.queueSize);
		resetTimeInputView = (EditText) findViewById(R.id.resetTime);
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

		int queueSize;
		String queueSizeText = queueSizeInputView.getText().toString();
		if (TextUtils.isEmpty(queueSizeText)) {
			queueSize = DEFAULT_QUEUE_SIZE;
		} else {
			try {
				queueSize = Integer.parseInt(queueSizeText);
			} catch (Exception e) {
				Log.wtf(Constant.TAG, String.format("Failed to parse [queueSizeText=%s]", queueSizeText), e);
				queueSize = 500_000;
			}
		}
		if (queueSize < 1000) {
			queueSize = 1000;
		}
		int crawledQueueSize = queueSize;
		if (crawledQueueSize < 1000) {
			crawledQueueSize = 1000;
		}

		long resetTime;
		String resetTimeText = resetTimeInputView.getText().toString();
		if (TextUtils.isEmpty(resetTimeText)) {
			resetTime = DEFAULT_RESET_TIME;
		} else {
			try {
				resetTime = Long.parseLong(resetTimeText) * HOUR_IN_MS;
			} catch (Exception e) {
				Log.wtf(Constant.TAG, String.format("Failed to parse [resetTimeText=%s]", resetTimeText), e);
				resetTime = 356 * 24 * HOUR_IN_MS;
			}
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
				throttle = 500_000;
			}
		}

		Configuration configuration = new Configuration();


		crawler = new WebCrawler(this, throttle, configuration );
		crawler.start(webUrl);

		startButton.setText("Stop Crawling");
		crawlingInfo.setVisibility(View.VISIBLE);

		// Send delayed message to resetHandler for restarting crawling
		resetHandler.sendEmptyMessageDelayed(Constant.MSG_RESTART_CRAWLING, resetTime);
		updateTextHandler.sendEmptyMessageDelayed(Constant.MSG_UPDATE_INFO, REFRESH_DELAY);
	}

	private Handler resetHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			stopCrawling();
			startCrawling();
		};
	};

	private Handler updateTextHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			WebCrawler currentCrawler = crawler;
			if (currentCrawler == null) {
				return;
			}

			final String url = currentCrawler.getLastUrl();
			final long count = currentCrawler.getCrawledCount();

			progressText.post(new Runnable() {

				@Override
				public void run() {
					progressText.setText(String.format("%s pages\n %s", count, url));

				}
			});

			updateTextHandler.sendEmptyMessageDelayed(Constant.MSG_UPDATE_INFO, REFRESH_DELAY);
		};
	};

	/**
	 * API to handle post crawling events
	 */
	private void stopCrawling() {
		if (crawler == null) {
			return;
		}
		WebCrawler oldCrawler = crawler;
		crawler = null;

		resetHandler.removeMessages(Constant.MSG_RESTART_CRAWLING);
		updateTextHandler.removeMessages(Constant.MSG_UPDATE_INFO);
		oldCrawler.stopCrawlerTasks();

		crawlingInfo.setVisibility(View.INVISIBLE);
		startButton.setText("Start Crawling");
		progressText.setText("crawledUrlCount.get() + \" pages crawled\"");

	}

}
