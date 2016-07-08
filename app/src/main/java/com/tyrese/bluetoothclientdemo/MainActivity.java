package com.tyrese.bluetoothclientdemo;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

public class MainActivity extends AppCompatActivity {

    public static final int MESSAGE_RECEIVED = 1;
    public static final int FILE_RECEIVED = 2;

    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 3;
    private AcceptThread acceptThread;
    private Button mBtnTest;
    private TextView mTextView;
    private ImageView mImg;
    private String cacheDir;
    private String testDir;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_RECEIVED:
                    mTextView.append((String) msg.obj);
                    break;
                case FILE_RECEIVED:
                    String path = (String) msg.obj;
                    Picasso.with(MainActivity.this).load(path).centerCrop().into(mImg);
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

        acceptThread = new AcceptThread(mBluetoothAdapter, mHandler, testDir + "/Download/test");
        acceptThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        acceptThread.cancel();
    }

    public void send(View view) {
        acceptThread.write("来自从设备的消息".getBytes());
    }
}
