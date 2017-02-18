package com.android.webcrawler;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import android.widget.Toast;

import java.util.concurrent.atomic.AtomicLong;

public class MainActivity extends Activity implements OnClickListener {

	private LinearLayout crawlingInfo;
	private Button startButton;
	private EditText urlInputView;
	private EditText queueSizeInputView;
	private EditText resetTimeInputView;
	private EditText throttleInputView;
	private TextView progressText;

	// WebCrawler object will be used to start crawling on root Url
	private WebCrawler crawler;
	// count variable for url crawled so far
	final private AtomicLong crawledUrlCount = new AtomicLong();
	// state variable to check crawling status
	private volatile boolean crawlingRunning;
	// For sending message to Handler in order to stop crawling after 60000 ms
	private static final int MSG_STOP_CRAWLING = 111;
	private static final long HOUR_IN_MS = 60*60*1_000;
	private static final long DEFAULT_RESET_TIME = 24*HOUR_IN_MS;
	private static final int DEFAULT_QUEUE_SIZE = 1_000;
	private static final int DEFAULT_THROTTLE = 10;


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

		crawler = new WebCrawler(this, mCallback);
	}

	/**
	 * callback for crawling events
	 */
	private WebCrawler.CrawlingCallback mCallback = new WebCrawler.CrawlingCallback() {

		@Override
		public void onPageCrawlingCompleted(final String url) {
			final long count = crawledUrlCount.incrementAndGet();
			progressText.post(new Runnable() {

				@Override
				public void run() {
					progressText.setText(String.format("%s pages\n %s", count, url));

				}
			});
		}

		@Override
		public void onPageCrawlingFailed(String url, int errorCode) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onCrawlingCompleted() {
			stopCrawling();
		}

	};

	/**
	 * Callback for handling button onclick events
	 */
	@Override
	public void onClick(View v) {
		int viewId = v.getId();
		switch (viewId) {
		case R.id.start:
//			String webUrl = urlInputView.getText().toString();
//			if (TextUtils.isEmpty(webUrl)) {
//				Toast.makeText(getApplicationContext(), "Please input web Url",
//						Toast.LENGTH_SHORT).show();
//			} else {
			if (crawlingRunning) {
				stopCrawling();
			} else {
				startCrawling();
			}
//			}
			break;
//		case R.id.stop:
			// remove any scheduled messages if user stopped crawling by
			// clicking stop button
			//handler.removeMessages(MSG_STOP_CRAWLING);
//			stopCrawling();
//			break;
		}
	}

	private void startCrawling() {
		if (!crawlingRunning) {
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
					Log.wtf("AndroidSRC_Crawler", String.format("Failed to parse [queueSizeText=%s]", queueSizeText), e);
					queueSize = 500_000;
				}
			}

			long resetTime;
			String resetTimeText = resetTimeInputView.getText().toString();
			if (TextUtils.isEmpty(resetTimeText)) {
				resetTime = DEFAULT_RESET_TIME;
			} else {
				try {
					resetTime = Long.parseLong(resetTimeText) * HOUR_IN_MS;
				} catch (Exception e) {
					Log.wtf("AndroidSRC_Crawler", String.format("Failed to parse [resetTimeText=%s]", resetTimeText), e);
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
					Log.wtf("AndroidSRC_Crawler", String.format("Failed to parse [throttleText=%s]", throttleText), e);
					throttle = 500_000;
				}
			}

			crawlingRunning = true;
			crawler.setup(queueSize, throttle);
			crawler.startCrawlerTask(webUrl, true);
//			startButton.setEnabled(false);
			startButton.setText("Stop Crawling");
			crawlingInfo.setVisibility(View.VISIBLE);

			// Send delayed message to handler for restarting crawling
			handler.sendEmptyMessageDelayed(MSG_STOP_CRAWLING, resetTime);
		}
	}

	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			stopCrawling();
			startCrawling();
		};
	};

	/**
	 * API to handle post crawling events
	 */
	private void stopCrawling() {
		if (crawlingRunning) {
			handler.removeMessages(MSG_STOP_CRAWLING);
			crawler.stopCrawlerTasks();
			crawlingInfo.setVisibility(View.INVISIBLE);
//			startButton.setEnabled(true);
			startButton.setVisibility(View.VISIBLE);
			startButton.setText("Start Crawling");
			crawlingRunning = false;
			// if (crawledUrlCount.get() > 0)
			//	Toast.makeText(getApplicationContext(),
			//			crawledUrlCount.get() + " pages crawled",
			//			Toast.LENGTH_SHORT).show();

			progressText.setText("crawledUrlCount.get() + \" pages crawled\"");
			crawledUrlCount.set(0);
		}
	}

	/**
	 * API to output crawled urls in logcat
	 * 
	 * @return number of rows saved in crawling database
	 */
	protected int printCrawledEntriesFromDb() {

		int count = 0;
		CrawlerDB mCrawlerDB = new CrawlerDB(this);
		SQLiteDatabase db = mCrawlerDB.getReadableDatabase();

		Cursor mCursor = db.query(CrawlerDB.TABLE_NAME, null, null, null, null,
				null, null);
		if (mCursor != null && mCursor.getCount() > 0) {
			count = mCursor.getCount();
			mCursor.moveToFirst();
			int columnIndex = mCursor
					.getColumnIndex(CrawlerDB.COLUMNS_NAME.CRAWLED_URL);
			for (int i = 0; i < count; i++) {
				Log.d("AndroidSRC_Crawler",
						"Crawled Url " + mCursor.getString(columnIndex));
				mCursor.moveToNext();
			}
		}

		return count;
	}

}
