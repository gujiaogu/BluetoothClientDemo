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
            UUID.fromString("2b695c0a-e703-4167-875e-d230791fa275");

    private Handler mHandler;
    private BluetoothAdapter mAdapter;
    private BluetoothServerSocket mServerSocket;
    private BluetoothSocket mSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean isCancel = false;

    public AcceptThread(BluetoothAdapter adapter, Handler handler) {
        this.mAdapter = adapter;
        this.mHandler = handler;
    }

    @Override
    public void run() {
        super.run();
        if (mAdapter == null) {
            return;
        }
        try {
            mServerSocket = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
        } catch (IOException e) {
            e.printStackTrace();
            LogWrapper.d("监听失败");
            cancel();
            mHandler.obtainMessage(MainActivity.MESSAGE_RECEIVED_FAILED).sendToTarget();
            return;
        }

        try {
            mHandler.obtainMessage(MainActivity.LISTENING).sendToTarget();
            mSocket = mServerSocket.accept();
        } catch (IOException e) {
            e.printStackTrace();
            LogWrapper.d("获取socket失败");
            cancel();
            mHandler.obtainMessage(MainActivity.MESSAGE_RECEIVED_FAILED).sendToTarget();
            return;
        }

        try {
            inputStream = mSocket.getInputStream();
            outputStream = mSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
            cancel();
            mHandler.obtainMessage(MainActivity.MESSAGE_RECEIVED_FAILED).sendToTarget();
            return;
        }

        byte[] buffer = null;
        mHandler.obtainMessage(MainActivity.CONNECTION_SUCCESS).sendToTarget();
        while (!isCancel) {
            buffer = new byte[1024];
            try {
                inputStream.read(buffer);
                String result = new String(buffer).trim();
                mHandler.obtainMessage(MainActivity.MESSAGE_RECEIVED, result).sendToTarget();
            } catch (IOException e) {
                isCancel = true;
                mHandler.obtainMessage(MainActivity.MESSAGE_RECEIVED_FAILED).sendToTarget();
                cancel();
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
        inputStream = null;
        outputStream = null;
        mSocket = null;
        mServerSocket = null;
    }
}
