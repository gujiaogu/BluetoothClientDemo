package com.tyrese.bluetoothclientdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Tyrese on 2016/7/8.
 */
public class AcceptFileThread extends Thread {

    private static final String NAME = "BluetoothChatDemoFile";
    private static final UUID MY_UUID =
            UUID.fromString("7a051945-79c9-40cc-9947-0e9f3862db16");
    private static final String END_STR = "bt socket closed, read return: -1";

    public static final int STATE_FILE_WAITING = 1;
    public static final int STATE_FILE_ACCEPTING = 2;
    public static final int STATE_FILE_ACCEPTED = 3;
    private int stateFileAccept = STATE_FILE_WAITING;

    private Handler mHandler;
    private BluetoothAdapter mAdapter;
    private BluetoothServerSocket mServerSocket;
    private BluetoothSocket mSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private FileOutputStream fos;
    private String cacheDir;
    private File file;

    public AcceptFileThread(BluetoothAdapter adapter, Handler handler, String cacheDir) {
        this.mAdapter = adapter;
        this.mHandler = handler;
        this.cacheDir = cacheDir;
    }

    @Override
    public void run() {
        super.run();
        if (mAdapter == null) {
            return;
        }
        try {
            mServerSocket = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
        } catch (Exception e) {
            e.printStackTrace();
            LogWrapper.d("监听失败");
            cancel();
            mHandler.obtainMessage(MainActivity.FILE_RECEIVED_EXCEPTION).sendToTarget();
            return;
        }

        try {
            mSocket = mServerSocket.accept();
        } catch (IOException e) {
            e.printStackTrace();
            LogWrapper.d("获取socket失败");
            cancel();
            mHandler.obtainMessage(MainActivity.FILE_RECEIVED_EXCEPTION).sendToTarget();
            return;
        }

        try {
            inputStream = mSocket.getInputStream();
            outputStream = mSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
            cancel();
            mHandler.obtainMessage(MainActivity.FILE_RECEIVED_EXCEPTION).sendToTarget();
            return;
        }

        byte[] buffer = new byte[512];
        try {
            long timeStamp = System.currentTimeMillis();
            file = new File(cacheDir + "/" + timeStamp + ".png");
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            fos = new FileOutputStream(file);
            int n = 0;
            while ((n = inputStream.read(buffer, 0, buffer.length)) != -1) {
                if (stateFileAccept != STATE_FILE_ACCEPTING) {
                    stateFileAccept = STATE_FILE_ACCEPTING;
                }
                fos.write(buffer, 0, n);
            }
        } catch (IOException e) {
            if (e.getMessage().contains(END_STR)) {
                try {
                    fos.flush();
                } catch (IOException a) {
                    stateFileAccept = STATE_FILE_WAITING;
                    a.printStackTrace();
                }
                if (file != null) {
                    stateFileAccept = STATE_FILE_ACCEPTED;
                    mHandler.obtainMessage(MainActivity.FILE_RECEIVED, file.getPath()).sendToTarget();
                } else {
                    stateFileAccept = STATE_FILE_WAITING;
                }
            } else {
                stateFileAccept = STATE_FILE_WAITING;
                mHandler.obtainMessage(MainActivity.FILE_RECEIVED_EXCEPTION).sendToTarget();
            }
            e.printStackTrace();
        } finally {
            cancel();
        }
    }

    public void cancel() {
        StreamUtil.close(mServerSocket);
        StreamUtil.close(mSocket);
        StreamUtil.close(inputStream);
        StreamUtil.close(outputStream);
        StreamUtil.close(fos);
    }

    public int getStateFileAccept() {
        return stateFileAccept;
    }

    public void setStateFileAccept(int stateFileAccept) {
        this.stateFileAccept = stateFileAccept;
    }
}
