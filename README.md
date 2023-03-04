# TestAIDL
AIDL 权限验证
AIDL
一、AIDL 文件中支持的数据类型
1.基本数据类型 char, byte, short, int, long, flog, double, boolean
2.String ,charsequence
3.实现了 parcelable 接口的类型
4.ArrayList 里面的元素必须是AIDL支持的类型
5.HashMap Key和Value必须是AIDL支持的类型
6.AIDL 所有的AIDL接口本身也可以在AIDL中使用

7.实现了 parcelable 接口的自定义类型，要单独声明一个AIDL文件
例如下面为 Book.java类生成单独的AIDL文件
// Book.java
public class Book implements Parcelable {}

// Book.aidl 内容
parcelable Book;

8.AIDL整体项目结构
1.java目录下的包名与aidl目录下的包名要一致
2.如果有自定义的类型，先编写aidl文件再编写对应的java类，例如Book
否则Android studio 中新建Book.aidl时报错 Interface Name must be unique

￼

二、AIDL文件中声明的内容
1.只支持方法
2.不支持声明静态常量

三、Android studio 帮我们生成的AIDL接口的实现类
1.位置：build/generated/aidl_source_output_dir/debug/out/com/example/testaidl/IBookManager.java
2.Stub类的中的方法
(1)DESCRIPTOR
Binder的唯一标识，默认是当前Binder的包名+类名 例如 com.example.testaidl.IBookManager
(2)public static com.example.testaidl.IBookManager asInterface(android.os.IBinder obj):
asInterface方法用于将服务端的Binder对象转成客户端所需要的AIDL接口类型对象。
如果客户端与服务端在同一进程将返回服务端Stub对象本身，否则返回系统封装过的Stub.Proxy对象
(3)asBinder
返回当前Binder对象
(4)onTransact
此方法运行在服务端中的Binder线程池中

四、binder工作机制
￼


五、AIDL文件中声明的接口中的方法运行在客户端的Binder线程池中
六、Binder 的方法
1. linkToDeath  unLinkToDeath 配对使用
   服务端进程由于某种原因异常终止，导致客户端远程调用失败，使用 linkToDeath 客户端收到通知后重新绑定服务
   使用方法
   (1)声明DeathRecipient对象，DeathRecipient是个接口，有一个方法 binderDied(),当Binder死亡时系统会回调这个方法，
   我们就可以移除之前绑定的binder并重新绑定远程服务
   private IBinder.DeathRecipient mDeathRecipient  = new IBinder.DeathRecipient(){
   @override
   public void binderDied(){
   if(mBookManager == null) return
   mBookManager.asBinder.unlinkToDeath(mDeathRecipient , 0);
   //TODO 这里重新绑定远程Service
   }
   };
   //客户端成功绑定远程服务后，给Binder设置死亡回调
   binder.linkToDeath(mDeathRecipient , 0);
2.isBinderAlive() 判断Binder.是否死亡

七、普通的接口无法在 Binder 中使用，需要创建 aidl 文件
//IOnNewBookArrivedListener.aidl 文件内容

interface IOnNewBookArrivedListener {
void onNewBookArrived(in Book book);
}

八、 RemoteCallbackList 当服务端需要调用客户端接口传数据时，需要在服务端中使用 RemoteCallbackList 来管理注册的接口
1.使用 ArrayMap<key:IBinder , value:Callback类型>
2.用于删除跨进程的接口,是一个泛型，支持管理任意的 AIDL 接口
3.客户端进程终止后，自动移除客户端注册的 listener
4.内部实现了线程同步
5.需要在 beginBroadcast()方法与finishBroadcast()方法之间使用它的API,哪怕是获取size
6.获取元素使用 getBroadcastItem(i)

九、跨进程调用方法关于 binder 线程池
1.客户端调用服务端方法，被调用的方法运行在服务端的 binder 线程池中，同时客户端线程会被挂起，这时如果服务端方法运行耗时，客户端会长时间阻塞。
如果客户端的线程是 UI 线程，就会 ANR.所以避免在客户端的 UI 线程去调用远程服务。onServiceConnected 和 onServiceDisconnected 都运行在 UI 线程。
2.服务端的方法本身运行在服务端的 binder 线程池中，可以执行大量的耗时操作。所以不要在服务端的方法开启线程去进行异步任务。
3.客户端调用服务端的耗时方法时，可以开启一个线程去调用，防止 ANR.
4.服务端调用客户端的方法时，被调用的方法运行在客户端的 Binder 线程池中。同样不能在服务端调用客户端的耗时方法。

