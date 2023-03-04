package com.example.testaidl;

import static android.os.Binder.getCallingPid;
import static android.os.Binder.getCallingUid;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author : wanghailong
 * @date :
 * @description: TODO
 */
public class ServiceAIDL extends Service {

    private AtomicBoolean mDestroy = new AtomicBoolean(false);//Service是否销毁了
    private List<Book> mBookList = new CopyOnWriteArrayList<>();
    /**
     * RemoteCallbackList
     * 1.使用 ArrayMap<key:IBinder , value:Callback类型>
     * 2.用于删除跨进程的接口,是一个泛型，支持管理任意的 AIDL 接口
     * 3.客户端进程终止后，自动移除客户端注册的 listener
     * 4.内部实现了线程同步
     * 5.需要在 beginBroadcast()方法与finishBroadcast()方法之间使用它的API,哪怕是获取size
     * 6.获取元素使用 getBroadcastItem(i)
     */
    private RemoteCallbackList<IOnNewBookArrivedListener> mListenerList = new RemoteCallbackList<>();

    private Binder mBinder = new IBookManager.Stub() {

        @Override
        public List<Book> getBookList() throws RemoteException {
            return mBookList;
        }

        @Override
        public void addBook(Book book) throws RemoteException {
            mBookList.add(book);
        }

        @Override
        public void registerListener(IOnNewBookArrivedListener l) throws RemoteException {
            mListenerList.register(l);
        }

        @Override
        public void unRegisterListener(IOnNewBookArrivedListener l) throws RemoteException {
            mListenerList.unregister(l);
            int n = mListenerList.beginBroadcast();
            Log.e(Cont.TAG, "current size:" + n);
            mListenerList.finishBroadcast();
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            //权限验证
//            String packageName = null;
//            String[] packages = getPackageManager().getPackagesForUid(getCallingUid());
//            if (packages != null && packages.length > 0) {
//                packageName = packages[0];
//            }
//            if (packageName == null) {
//                return false;
//            }
//            if (!packageName.startsWith("com.example.testaidl")) {
//                return false;
//            }

            //二选一
//            int check = checkCallingPermission("com.example.testaidl.permission.ACCESS_BOOK_SERVICE");
//            if (check == PackageManager.PERMISSION_DENIED) {
//                Log.e(Cont.TAG, "客户端没有权限");
//                return false;
//            }

            //或用下面这种方式
//            int check = checkCallingOrSelfPermission("com.example.testaidl.permission.ACCESS_BOOK_SERVICE");
//            if (check == PackageManager.PERMISSION_DENIED) {
//                Log.e(Cont.TAG, "客户端没有权限");
//                return false;
//            }
            return super.onTransact(code, data, reply, flags);
        }
    };

    private void onNewBookArrived(Book book) throws RemoteException {
        int n = mListenerList.beginBroadcast();
        for (int i = 0; i < n; ++i) {
            IOnNewBookArrivedListener l = mListenerList.getBroadcastItem(i);
            if (l != null) {
                try {
                    l.onNewBookArrived(book);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        mListenerList.finishBroadcast();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mBookList.add(new Book("玩android"));
        mBookList.add(new Book("android 开发"));
        new Thread(new ServiceTask()).start();
    }

    // 以 startService 方式启动服务调用此方法
    @Override
    public void onStart(Intent intent, int startId) {
        Log.e(Cont.TAG, "service onStart");
        super.onStart(intent, startId);
    }

    //以 startService 方式启动服务，重复启动调用此方法，不会再走 onStart 方法
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(Cont.TAG, "service onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.e(Cont.TAG, "service onBind");
        // 权限校验
        String packageName = null;
        String[] packages = getPackageManager().getPackagesForUid(getCallingUid());
        if (packages != null && packages.length > 0) {
            packageName = packages[0];
        }
        if (packageName == null) {
            return null;
        }
        if (!packageName.startsWith("com.example.testaidl")) {
            return null;
        }
        /**
         * 下面这两个方法运行在 IPC 通信方法中，所以在 onTransact 方法调用会成功。在 onBind 方法中调用一直都是失败
         * checkCallingPermission
         * checkCallingOrSelfPermission
         */
        int check = checkPermission("com.example.testaidl.permission.ACCESS_BOOK_SERVICE", getCallingPid(), getCallingUid());
        if (check == PackageManager.PERMISSION_DENIED) {
            Log.e(Cont.TAG, "客户端没有权限");
            return null;
        }
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mDestroy.set(true);
        Log.e(Cont.TAG, "service onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public void onDestroy() {
        mDestroy.set(true);
        Log.e(Cont.TAG, "service onDestroy");
        super.onDestroy();
    }

    public class ServiceTask implements Runnable {
        int i = 1;
        @Override
        public void run() {
            while (!mDestroy.get()) {
                try {
                    Thread.sleep(2000);
                    Book book = new Book("kwg " + i++);
                    onNewBookArrived(book);
                } catch (RemoteException | InterruptedException e) {
                    e.printStackTrace();
                }
                //模拟服务端异常终止
//                if (i == 5) {
//                    android.os.Process.killProcess(android.os.Process.myPid());
//                }
            }
        }
    }
}
