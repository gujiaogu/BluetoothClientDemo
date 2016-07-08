package com.tyrese.bluetoothclientdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.SystemClock;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Tyrese on 2016/7/8.
 */
public class AcceptThread extends Thread {

    private static final String NAME = "BluetoothChatDemo";
    private static final UUID MY_UUID =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private static final String TAG_FIEL = "fileiscoming";

    private Handler mHandler;
    private BluetoothAdapter mAdapter;
    private BluetoothServerSocket mServerSocket;
    private BluetoothSocket mSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean isCancel = false;
    private boolean isFileComing = false;
    private String cacheDir;

    public AcceptThread(BluetoothAdapter adapter, Handler handler, String cacheDir) {
        this.mAdapter = adapter;
        this.mHandler = handler;
        this.cacheDir = cacheDir;
    }

    @Override
    public void run() {
        super.run();
        try {
            mServerSocket = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
        } catch (IOException e) {
            e.printStackTrace();
            LogWrapper.d("监听失败");
            return;
        }

        try {
            mSocket = mServerSocket.accept();
        } catch (IOException e) {
            e.printStackTrace();
            LogWrapper.d("获取socket失败");
            return;
        }

        try {
            inputStream = mSocket.getInputStream();
            outputStream = mSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] buffer = new byte[1024];
        int n = 0;
        while (!isCancel) {
            try {
                if (isFileComing) {
                    long timeStamp = System.currentTimeMillis();
                    File file = new File(cacheDir + "/" + timeStamp + ".png");
                    if (file.exists()) {
                        file.delete();
                    }
                    file.createNewFile();
                    FileOutputStream fos = new FileOutputStream(file);
                    while ((n = inputStream.read(buffer, 0, buffer.length)) != -1) {
                        fos.write(buffer, 0, n);
                    }
                    fos.flush();
                    fos.close();
                    mHandler.obtainMessage(MainActivity.FILE_RECEIVED, file.getAbsolutePath());
                    isFileComing = false;
                } else {
                    inputStream.read(buffer);
                    String result = new String(buffer).trim();
                    if (TAG_FIEL.equals(result)) {
                        isFileComing = true;
                    } else {
                        mHandler.obtainMessage(MainActivity.MESSAGE_RECEIVED, result).sendToTarget();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void write(byte[] buffer) {
        try {
            outputStream.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cancel() {
        isCancel = true;
        StreamUtil.close(inputStream);
        StreamUtil.close(outputStream);
        StreamUtil.close(mSocket);
        StreamUtil.close(mServerSocket);
    }
}
