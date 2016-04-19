package cn.edu.uestc.graduationproject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;
import cn.bgxt.surfaceviewdemo.R;

public class MainActivity extends Activity {
	private final String TAG = "main";
	private EditText et_path;
	private SurfaceView sv;
	private ImageButton btn_play, btn_pause, btn_replay, btn_stop, btn_encode,
			btn_judge;
	private MediaPlayer mediaPlayer;
	private SeekBar seekBar;
	private int currentPosition = 0;
	private boolean isPlaying;
	protected ProgressDialog pd;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		seekBar = (SeekBar) findViewById(R.id.seekBar);
		sv = (SurfaceView) findViewById(R.id.sv);
		et_path = (EditText) findViewById(R.id.et_path);

		btn_play = (ImageButton) findViewById(R.id.btn_play);
		btn_pause = (ImageButton) findViewById(R.id.btn_pause);
		btn_replay = (ImageButton) findViewById(R.id.btn_replay);
		btn_stop = (ImageButton) findViewById(R.id.btn_stop);
		btn_encode = (ImageButton) findViewById(R.id.btn_encode);
		btn_judge = (ImageButton) findViewById(R.id.btn_judge);

		btn_play.setOnClickListener(click);
		btn_pause.setOnClickListener(click);
		btn_replay.setOnClickListener(click);
		btn_stop.setOnClickListener(click);
		btn_encode.setOnClickListener(click);
		btn_judge.setOnClickListener(click);

		// 为SurfaceHolder添加回调
		sv.getHolder().addCallback(callback);

		// 4.0版本之下需要设置的属性
		// 设置Surface不维护自己的缓冲区，而是等待屏幕的渲染引擎将内容推送到界面
		// sv.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		// 为进度条添加进度更改事件
		seekBar.setOnSeekBarChangeListener(change);
	}

	private Callback callback = new Callback() {
		// SurfaceHolder被修改的时候回调
		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			Log.i(TAG, "SurfaceHolder 被销毁");
			// 销毁SurfaceHolder的时候记录当前的播放位置并停止播放
			if (mediaPlayer != null && mediaPlayer.isPlaying()) {
				currentPosition = mediaPlayer.getCurrentPosition();
				mediaPlayer.stop();
			}
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			Log.i(TAG, "SurfaceHolder 被创建");
			if (currentPosition > 0) {
				// 创建SurfaceHolder的时候，如果存在上次播放的位置，则按照上次播放位置进行播放
				play(currentPosition);
				currentPosition = 0;
			}
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			Log.i(TAG, "SurfaceHolder 大小被改变");
		}

	};

	private OnSeekBarChangeListener change = new OnSeekBarChangeListener() {

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// 当进度条停止修改的时候触发
			// 取得当前进度条的刻度
			int progress = seekBar.getProgress();
			if (mediaPlayer != null && mediaPlayer.isPlaying()) {
				// 设置当前播放的位置
				mediaPlayer.seekTo(progress);
			}
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {

		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {

		}
	};

	private View.OnClickListener click = new View.OnClickListener() {

		@Override
		public void onClick(View v) {

			switch (v.getId()) {
			case R.id.btn_play:
				play(0);
				break;
			case R.id.btn_pause:
				pause();
				break;
			case R.id.btn_replay:
				replay();
				break;
			case R.id.btn_stop:
				stop();
				break;
			case R.id.btn_judge:
				judge();
				break;
			case R.id.btn_encode:
				pd = ProgressDialog
						.show(MainActivity.this, "请稍候..", "正在进行DRM"
								+ btn_encode.getTag().toString() + "操作...",
								true, false);
				Thread t1 = new Thread(XorRunnable);
				t1.start();
				if (btn_encode.getTag().toString().trim().equals("加密")) {
					btn_encode.setTag("解密");
					btn_encode.setImageResource(R.drawable.lock_open);
					return;
				} else {
					btn_encode.setTag("加密");
					btn_encode.setImageResource(R.drawable.lock_close);
				}
				break;
			default:
				break;
			}
		}
	};

	Runnable XorRunnable = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				XorCode();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			pd.dismiss();
		}
	};

	/*
	 * 停止播放
	 */
	protected void stop() {
		if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
			btn_play.setEnabled(true);
			btn_play.setImageResource(R.drawable.play);
			isPlaying = false;
		}
	}

	@SuppressLint({ "ShowToast", "SdCardPath" })
	protected void judge() {
		// TODO Auto-generated method stub
		String filepath = et_path.getText().toString().trim();
		String ropath = "/sdcard/ro.evidencestore";
		String keypath = "/sdcard/privateKey.keystore";
		File file = new File(filepath);
		if (!file.exists()) {
			Toast.makeText(this, "视频文件路径错误", 0).show();
			return;
		}
		if (!fileIsExist(ropath)) {
			Toast.makeText(this, "RO文件不存在", 0).show();
			return;
		}
		if (!fileIsExist(keypath)) {
			Toast.makeText(this, "私密文件不存在", 0).show();
			return;
		}
		try {
			String evidence = IntegrityVerifier.getFileHash(file, "SHA-1");
			if (IntegrityVerifier.IntegrityVerify(ropath, keypath, evidence)) {
				btn_judge.setImageResource(R.drawable.judge);
				Toast.makeText(this, "DRM文件完整!", 0).show();
				return;
			} else {
				btn_judge.setImageResource(R.drawable.judge_off);
				Toast.makeText(this, "DRM文件不完整!", 0).show();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 开始播放
	 * 
	 * @param msec
	 *            播放初始位置
	 */
	protected void play(final int msec) {
		// 获取视频文件地址
		String path = et_path.getText().toString().trim();
		File file = new File(path);
		if (!file.exists()) {
			Toast.makeText(this, "视频文件路径错误", 0).show();
			return;
		}
		try {
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			// 设置播放的视频源
			mediaPlayer.setDataSource(file.getAbsolutePath());
			// 设置显示视频的SurfaceHolder
			mediaPlayer.setDisplay(sv.getHolder());
			Log.i(TAG, "开始装载");
			mediaPlayer.prepareAsync();
			mediaPlayer.setOnPreparedListener(new OnPreparedListener() {

				@Override
				public void onPrepared(MediaPlayer mp) {
					Log.i(TAG, "装载完成");
					mediaPlayer.start();
					// 按照初始位置播放
					mediaPlayer.seekTo(msec);
					// 设置进度条的最大进度为视频流的最大播放时长
					seekBar.setMax(mediaPlayer.getDuration());
					// 开始线程，更新进度条的刻度
					new Thread() {

						@Override
						public void run() {
							try {
								isPlaying = true;
								while (isPlaying) {
									int current = mediaPlayer
											.getCurrentPosition();
									seekBar.setProgress(current);

									sleep(500);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}.start();

					btn_play.setEnabled(false);
					btn_play.setImageResource(R.drawable.play_off);
				}
			});
			mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

				@Override
				public void onCompletion(MediaPlayer mp) {
					// 在播放完毕被回调
					btn_play.setEnabled(true);
					btn_play.setImageResource(R.drawable.play);
				}
			});

			mediaPlayer.setOnErrorListener(new OnErrorListener() {

				@Override
				public boolean onError(MediaPlayer mp, int what, int extra) {
					Log.d(TAG, "OnError - Error code: " + what
							+ " Extra code: " + extra);
					// 发生错误重新播放
					play(0);
					isPlaying = false;
					return false;
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * 重新开始播放
	 */
	protected void replay() {
		if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			mediaPlayer.seekTo(0);
			Toast.makeText(this, "重新播放", 0).show();
			btn_pause.setTag("暂停");
			return;
		}
		isPlaying = false;
		play(0);

	}

	/**
	 * 暂停或继续
	 */
	protected void pause() {
		if (btn_pause.getTag().toString().trim().equals("继续")) {
			btn_pause.setTag("暂停");
			btn_pause.setImageResource(R.drawable.pause);
			mediaPlayer.start();
			Toast.makeText(this, "继续播放", 0).show();
			return;
		}
		if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
			btn_pause.setImageResource(R.drawable.play);
			btn_pause.setTag("继续");
			Toast.makeText(this, "暂停播放", 0).show();
		}
	}

	private String getUUID() {
		// TODO Auto-generated method stub
		final TelephonyManager tm = (TelephonyManager) getBaseContext()
				.getSystemService(Context.TELEPHONY_SERVICE);
		final String tmDevice, tmSerial, androidId;
		tmDevice = "" + tm.getDeviceId();
		tmSerial = "" + tm.getSimSerialNumber();
		androidId = ""
				+ android.provider.Settings.Secure.getString(
						getContentResolver(),
						android.provider.Settings.Secure.ANDROID_ID);
		UUID deviceUuid = new UUID(androidId.hashCode(),
				((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
		return deviceUuid.toString();
	}

	private void XorCode() throws IOException {
		String path = et_path.getText().toString().trim();
		File enfile = new File(path);
		File exfile = new File(path + "temp");
		if (!enfile.exists()) {
			Toast.makeText(this, "视频文件路径错误", 0).show();
			return;
		}

		String xorKey = getUUID();
		byte[] keyBytes = xorKey.getBytes();
		FileInputStream fis = new FileInputStream(enfile);
		FileOutputStream fos = new FileOutputStream(exfile);
		byte[] bs = new byte[10240];
		int len = 0;
		int keyIndex = 0;
		while ((len = fis.read(bs)) != -1) {
			for (int i = 0; i < len; i++) {
				bs[i] ^= keyBytes[keyIndex];
				if (++keyIndex == keyBytes.length) {
					keyIndex = 0;
				}
			}
			fos.write(bs, 0, len);
		}
		fos.close();
		fis.close();
		if (enfile.delete())
			exfile.renameTo(enfile);
	}

	protected boolean fileIsExist(String filePath) {
		File file = new File(filePath);
		return file.exists();
	}
}
