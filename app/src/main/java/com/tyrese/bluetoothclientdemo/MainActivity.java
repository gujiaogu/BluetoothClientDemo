package com.tyrese.bluetoothclientdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.FileInputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    public static final int MESSAGE_RECEIVED = 1;
    public static final int MESSAGE_RECEIVED_FAILED = 7;
    public static final int FILE_RECEIVED = 2;
    public static final int FILE_RECEIVED_EXCEPTION = 3;
    public static final int CONNECTION_SUCCESS = 5;
    public static final int LISTENING = 6;

    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 3;
    private AcceptThread acceptThread;
    private AcceptFileThread acceptFileThread;
    private Button mBtnTest;
    private TextView mTextView;
    private ImageView mImg;
    private String cacheDir;
    private String testDir;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CONNECTION_SUCCESS:
                    getSupportActionBar().setSubtitle("连接成功");
                    break;
                case LISTENING:
                    getSupportActionBar().setSubtitle("正在监听");
                    break;
                case MESSAGE_RECEIVED:
                    mTextView.append((String) msg.obj + "\n");
                    break;
                case MESSAGE_RECEIVED_FAILED:
                    getSupportActionBar().setSubtitle("未连接");
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            acceptThread = new AcceptThread(mBluetoothAdapter, mHandler);
                            acceptThread.start();
                            getSupportActionBar().setSubtitle("正在监听");
                        }
                    }, 100);
                    break;
                case FILE_RECEIVED_EXCEPTION:
                    acceptFileThread = new AcceptFileThread(mBluetoothAdapter, mHandler, testDir + "/Download/");
                    acceptFileThread.start();
                    break;
                case FILE_RECEIVED:
                    final String path = (String) msg.obj;
                    int width = (int) getResources().getDimension(R.dimen.pic_width);
                    Picasso.with(MainActivity.this).load("file://" + path).resize(width, width).centerCrop().into(mImg);
                    Toast.makeText(MainActivity.this, "文件收到", Toast.LENGTH_SHORT).show();
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            acceptFileThread = new AcceptFileThread(mBluetoothAdapter, mHandler, testDir + "/Download/");
                            acceptFileThread.start();
                        }
                    }, 100);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setSubtitle("未连接");
        mBtnTest = (Button) findViewById(R.id.btn_test_send);
        mTextView = (TextView) findViewById(R.id.text);
        mImg = (ImageView) findViewById(R.id.image);
        cacheDir = getCacheDir().getAbsolutePath();
        testDir = Environment.getExternalStorageDirectory().getAbsolutePath();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "您的设备不支持蓝牙！", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        acceptThread = new AcceptThread(mBluetoothAdapter, mHandler);
        acceptThread.start();
        acceptFileThread = new AcceptFileThread(mBluetoothAdapter, mHandler, testDir + "/Download/");
        acceptFileThread.start();
    }

    @Override
    public void onBackPressed() {
        if (acceptFileThread != null
                && acceptFileThread.getStateFileAccept() == acceptFileThread.STATE_FILE_ACCEPTING) {
            Toast.makeText(this, "正在传送文件请不要退出！", Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
        if (acceptFileThread != null) {
            acceptFileThread.cancel();
            acceptFileThread = null;
        }
        mBluetoothAdapter = null;
        super.onDestroy();
    }

    public void send(View view) {
        acceptThread.write("来自从设备的消息".getBytes());
    }
}
