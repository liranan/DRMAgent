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

		// ΪSurfaceHolder��ӻص�
		sv.getHolder().addCallback(callback);

		// 4.0�汾֮����Ҫ���õ�����
		// ����Surface��ά���Լ��Ļ����������ǵȴ���Ļ����Ⱦ���潫�������͵�����
		// sv.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		// Ϊ��������ӽ��ȸ����¼�
		seekBar.setOnSeekBarChangeListener(change);
	}

	private Callback callback = new Callback() {
		// SurfaceHolder���޸ĵ�ʱ��ص�
		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			Log.i(TAG, "SurfaceHolder ������");
			// ����SurfaceHolder��ʱ���¼��ǰ�Ĳ���λ�ò�ֹͣ����
			if (mediaPlayer != null && mediaPlayer.isPlaying()) {
				currentPosition = mediaPlayer.getCurrentPosition();
				mediaPlayer.stop();
			}
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			Log.i(TAG, "SurfaceHolder ������");
			if (currentPosition > 0) {
				// ����SurfaceHolder��ʱ����������ϴβ��ŵ�λ�ã������ϴβ���λ�ý��в���
				play(currentPosition);
				currentPosition = 0;
			}
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			Log.i(TAG, "SurfaceHolder ��С���ı�");
		}

	};

	private OnSeekBarChangeListener change = new OnSeekBarChangeListener() {

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// ��������ֹͣ�޸ĵ�ʱ�򴥷�
			// ȡ�õ�ǰ�������Ŀ̶�
			int progress = seekBar.getProgress();
			if (mediaPlayer != null && mediaPlayer.isPlaying()) {
				// ���õ�ǰ���ŵ�λ��
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
						.show(MainActivity.this, "���Ժ�..", "���ڽ���DRM"
								+ btn_encode.getTag().toString() + "����...",
								true, false);
				Thread t1 = new Thread(XorRunnable);
				t1.start();
				if (btn_encode.getTag().toString().trim().equals("����")) {
					btn_encode.setTag("����");
					btn_encode.setImageResource(R.drawable.lock_open);
					return;
				} else {
					btn_encode.setTag("����");
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
	 * ֹͣ����
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
			Toast.makeText(this, "��Ƶ�ļ�·������", 0).show();
			return;
		}
		if (!fileIsExist(ropath)) {
			Toast.makeText(this, "RO�ļ�������", 0).show();
			return;
		}
		if (!fileIsExist(keypath)) {
			Toast.makeText(this, "˽���ļ�������", 0).show();
			return;
		}
		try {
			String evidence = IntegrityVerifier.getFileHash(file, "SHA-1");
			if (IntegrityVerifier.IntegrityVerify(ropath, keypath, evidence)) {
				btn_judge.setImageResource(R.drawable.judge);
				Toast.makeText(this, "DRM�ļ�����!", 0).show();
				return;
			} else {
				btn_judge.setImageResource(R.drawable.judge_off);
				Toast.makeText(this, "DRM�ļ�������!", 0).show();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * ��ʼ����
	 * 
	 * @param msec
	 *            ���ų�ʼλ��
	 */
	protected void play(final int msec) {
		// ��ȡ��Ƶ�ļ���ַ
		String path = et_path.getText().toString().trim();
		File file = new File(path);
		if (!file.exists()) {
			Toast.makeText(this, "��Ƶ�ļ�·������", 0).show();
			return;
		}
		try {
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			// ���ò��ŵ���ƵԴ
			mediaPlayer.setDataSource(file.getAbsolutePath());
			// ������ʾ��Ƶ��SurfaceHolder
			mediaPlayer.setDisplay(sv.getHolder());
			Log.i(TAG, "��ʼװ��");
			mediaPlayer.prepareAsync();
			mediaPlayer.setOnPreparedListener(new OnPreparedListener() {

				@Override
				public void onPrepared(MediaPlayer mp) {
					Log.i(TAG, "װ�����");
					mediaPlayer.start();
					// ���ճ�ʼλ�ò���
					mediaPlayer.seekTo(msec);
					// ���ý�������������Ϊ��Ƶ������󲥷�ʱ��
					seekBar.setMax(mediaPlayer.getDuration());
					// ��ʼ�̣߳����½������Ŀ̶�
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
					// �ڲ�����ϱ��ص�
					btn_play.setEnabled(true);
					btn_play.setImageResource(R.drawable.play);
				}
			});

			mediaPlayer.setOnErrorListener(new OnErrorListener() {

				@Override
				public boolean onError(MediaPlayer mp, int what, int extra) {
					Log.d(TAG, "OnError - Error code: " + what
							+ " Extra code: " + extra);
					// �����������²���
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
	 * ���¿�ʼ����
	 */
	protected void replay() {
		if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			mediaPlayer.seekTo(0);
			Toast.makeText(this, "���²���", 0).show();
			btn_pause.setTag("��ͣ");
			return;
		}
		isPlaying = false;
		play(0);

	}

	/**
	 * ��ͣ�����
	 */
	protected void pause() {
		if (btn_pause.getTag().toString().trim().equals("����")) {
			btn_pause.setTag("��ͣ");
			btn_pause.setImageResource(R.drawable.pause);
			mediaPlayer.start();
			Toast.makeText(this, "��������", 0).show();
			return;
		}
		if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
			btn_pause.setImageResource(R.drawable.play);
			btn_pause.setTag("����");
			Toast.makeText(this, "��ͣ����", 0).show();
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
			Toast.makeText(this, "��Ƶ�ļ�·������", 0).show();
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
