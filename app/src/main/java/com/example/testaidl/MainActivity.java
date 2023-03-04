package com.example.testaidl;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.NoSuchElementException;

public class MainActivity extends AppCompatActivity {
    private boolean bindSuccess = false;
    private static final int MESSAGE_NEW_BOOK_ARRIVED = 1;
    private IBookManager mRemoteBookManager;
    private MyHandler mHandler = new MyHandler(Looper.getMainLooper(), this) {
        @Override
        public void onHandleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MESSAGE_NEW_BOOK_ARRIVED:
                    Log.e(Cont.TAG, "接收到新书：" + msg.obj);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    private static class MyHandler extends Handler {
        private WeakReference<Activity> mWeakReference;

        public MyHandler(Looper looper, Activity activity) {
            super(looper);
            mWeakReference = new WeakReference<>(activity);
        }

        public void onHandleMessage(@NonNull Message msg) {

        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (mWeakReference != null && mWeakReference.get() != null) {
                onHandleMessage(msg);
            }
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //主线程
            IBookManager iBookManager = IBookManager.Stub.asInterface(service);
            mRemoteBookManager = iBookManager;
            try {
                //客户端成功绑定远程服务后，给服务端 Binder 设置死亡回调
                mRemoteBookManager.asBinder().linkToDeath(mDeathRecipient, 0);
                mRemoteBookManager.registerListener(mOnNewBookArrivedListener);
                //获取服务端书列表
                List<Book> bookList = mRemoteBookManager.getBookList();
                Log.e(Cont.TAG, bookList.toString());
                //添加书
                mRemoteBookManager.addBook(new Book("kwg kwg kwg "));
                mRemoteBookManager.addBook(new Book("kwg kwg2 "));
                Log.e(Cont.TAG, mRemoteBookManager.getBookList().toString());

                bindSuccess = true;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(Cont.TAG, "service Disconnected");
        }
    };

    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {//运行在 客户端的 binder 线程池中，不能访问UI.服务端意外终止时回调此方法,先调用引方法，再调用 onServiceDisconnected
            Log.e(Cont.TAG, "binderDied");
            if (mRemoteBookManager == null) return;
            mRemoteBookManager.asBinder().unlinkToDeath(mDeathRecipient, 0);
            mRemoteBookManager = null;
            // 重新绑定方法
            bindService();
        }
    };

    /**
     * 向服务端注册此接口
     * 由服务端的 RemoteCallbackList 管理
     */
    private IOnNewBookArrivedListener mOnNewBookArrivedListener = new IOnNewBookArrivedListener.Stub() {
        @Override
        public void onNewBookArrived(Book book) throws RemoteException {
            mHandler.obtainMessage(MESSAGE_NEW_BOOK_ARRIVED, book).sendToTarget();
        }
    };

    /**
     * 绑定服务
     */
    protected void bindService() {
        //绑定服务
        Intent intent = new Intent(MainActivity.this, ServiceAIDL.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                bindService();
            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.e(Cont.TAG, "MainActivity onDestroy");
        if (mRemoteBookManager != null && mRemoteBookManager.asBinder().isBinderAlive()) {
            try {
                mRemoteBookManager.unRegisterListener(mOnNewBookArrivedListener);
            } catch (RemoteException | NoSuchElementException e) {
                e.printStackTrace();
            }
        }
        if (bindSuccess) {
            unbindService(mServiceConnection);
        }

        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
    }
}