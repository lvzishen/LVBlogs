#### 目录介绍

- 1.Handler机制整体流程图
- 2.为什么需要使用Handler
- 3.Looper
- 4.如何做到延迟发送消息
- 5.MessageQueue的机制
- 6.Message分发的三个的优先级
- 7.Handler线程调度的实质
- 8.Looper.loop是一个死循环，拿不到需要处理的Message就会阻塞，那在UI线程中为什么不会导致ANR？
- 9.Handler 引起的内存泄露原因以及最佳解决方案
- 10.IdleHandler 是什么
- 11.同步屏障

先来一个自己画的Handler机制整体流程图，`本文不会带着你走一遍源码，只会对重点需要注意的地方以及一些细节的处理做出解释，让你更好的了解Handler机制整体的运作`。
![在这里插入图片描述](https://user-gold-cdn.xitu.io/2019/3/12/1697248a05a2d88a?w=1097&h=744&f=png&s=102812)

 - Handler通过sendMessage()发送Message到MessageQueue队列；
  -  Looper通过loop()，不断提取出达到触发条件的Message，并将Message交给target来处理； 
  - 经过dispatchMessage()后，交回给Handler的handleMessage()来进行相应地处理。 
  - 将Message加入MessageQueue时，处往管道写入字符，可以会唤醒loop线程；
  - 如果MessageQueue中没有Message，并处于Idle状态，则会执行IdelHandler接口中的方法，往往用于做一些清理性地工作。
### 下边放几个需要注意的Handler知识点：

 1. Handler 的背后有 Looper、MessageQueue 支撑，Looper 负责消息分发，MessageQueue 负责消息管理；
 1. 在创建 Handler 之前一定需要先创建 Looper；
 1. Looper 有退出的功能，但是主线程的 Looper 不允许退出；
 1. 异步线程的 Looper 需要自己调用 Looper.quit();  退出；
 1. Runnable 被封装进了 Message，可以说是一个特殊的 Message；
 1. Handler.handleMessage() 所在的线程是 Looper.loop() 方法被调用的线程，**`也可以说成 Looper 所在的线程，并不是创建 Handler 的线程，Handler新建时持有的Looper在哪个线程，最后Handler.handleMessage()就在哪个线程执行`**； 
 1. 使用内部类的方式使用 Handler 可能会导致内存泄露，即便在 Activity.onDestroy 里移除延时消息，必须要写成静态内部类；

### 为什么需要使用Handler
`因为Android系统不允许在非UI线程更新UI`，因为如果多个线程同时改变View的状态会造成最终View状态的不确定性，`如果给每个View的操作都上锁的话那么势必会造成性能的损耗，所以干脆规定只能在UI线程去更新UI,而Handler就是用来进行线程切换操作的。`
使用方法

```
class LooperThread extends Thread {
    public Handler mHandler;


    public void run() {
        Looper.prepare();  
        mHandler = new Handler() {  
            public void handleMessage(Message msg) {
                //TODO 定义消息处理逻辑.
            }
        };
        Looper.loop();  
    }
}
```
主线程中可以使用Handler的原因是在ActivityThread中程序的入口main方法中调用了Looper.prepare();和Looper.loop(); 

```
Looper.prepareMainLooper();
ActivityThread thread = new ActivityThread();
thread.attach(false);
if (sMainThreadHandler == null) {
    sMainThreadHandler = thread.getHandler();
}
if (false) {
    Looper.myLooper().setMessageLogging(new
            LogPrinter(Log.DEBUG, "ActivityThread"));
}
// End of event ActivityThreadMain.
Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
Looper.loop();
```
### Looper

 - Looper.prepare()
**Looper.prepare()在每个线程只允许执行一次，该方法给当前线程通过TL绑定一个线程所属的唯一一个实例。**

```
private static void prepare(boolean quitAllowed) {
    //看当前线程是否已通过TL绑定对应的实例,有的话抛异常,所以prepare方法只允许调用一次
    if (sThreadLocal.get() != null) {
        throw new RuntimeException("Only one Looper may be created per thread");
    }
    //创建Looper对象，并通过TL建立与线程的绑定关系
    sThreadLocal.set(new Looper(quitAllowed));
}
```
- ThreadLocal
我们看一下ThreadLocal.set方法

```
public void set(T value) {
   Thread t = Thread.currentThread();//获取当前线程
   ThreadLocalMap map = getMap(t);//获取当前线程所属的ThreadLocalMap实例,键值对结构
   if (map != null)
       map.set(this, value); //以当前ThreadLocal作为键,Looper作为值建立绑定关系
   else
       createMap(t, value);
   }
}
```
ThreadLocal.get方法

```
public T get() {
    Thread t = Thread.currentThread();//获取当前线程
    ThreadLocalMap map = getMap(t);//获取当前线程所属的ThreadLocalMap实例,键值对结构
    if (map != null) {
        ThreadLocalMap.Entry e = map.getEntry(this);//通过当前ThreadLocal作为键取出对应的值
        if (e != null) {
            @SuppressWarnings("unchecked")
            T result = (T)e.value;
            return result;
        }
    }
    return setInitialValue();
}
```
### 为什么要选择ThreadLocal建立绑定关系?
**`因为我们是要让每一个线程都有且只有一个唯一的Looper实例，这时就可以使用ThreadLocal给每个线程绑定一个唯一实例的特性很方便的建立绑定关系。如果不采用ThreadLocal去实现，那么只能使用一个LooperManager管理类然后通过其中的Map去统一管理，那么这样无疑是很麻烦的`**。ThreadLocal只是作为主键，如果是Thread作为主键，那么很显然一个线程只能与一个对应的对象建立绑定关系，这显然是非常不合理的。
 - Looper.loop()
loop()进入循环模式,主要进行了`如下几点`:
	1. **获取当前线程的Looper实例**
	2. **通过Looper获取MessageQueue实例**
	3. **开启死循环并在其中调用MessageQueue的next方法不断轮询MessageQueue的头结点**
```
public static void loop() {
    final Looper me = myLooper();  //获取TLS存储的Looper对象 -->sThreadLocal.get()
    final MessageQueue queue = me.mQueue;  //获取Looper对象中的消息队列


    Binder.clearCallingIdentity();
    //确保在权限检查时基于本地进程，而不是调用进程。
    final long ident = Binder.clearCallingIdentity();


    for (;;) { //进入loop的主循环方法
        Message msg = queue.next(); //可能会阻塞 
        if (msg == null) { //没有消息则退出循环,调用Looper.quit()方法后返回空的message,随即退出
            return;
        }

        Printer logging = me.mLogging;
        if (logging != null) {
            logging.println(">>>>> Dispatching to " + msg.target + " " +
                    msg.callback + ": " + msg.what);
        }
        msg.target.dispatchMessage(msg); //用于分发Message 
        if (logging != null) {
            logging.println("<<<<< Finished to " + msg.target + " " + msg.callback);
        }

        final long newIdent = Binder.clearCallingIdentity();
        msg.recycleUnchecked(); 
    }
}
```

 - Looper.quit()
`用于终止loop循环,主线程中不允许调用(能调用的话相当于主程序退出了，应用就直接挂掉了),子线程中要退出时需要主动调用,否则会造成子线程中一直处于死循环状态无法退出`。调用后会改变MessageQueue中的mQuitting标志位,next方法中如果检测到mQuitting为true则直接返回null,loop方法中检测到message是null则直接return终止死循环从而结束逻辑使得线程可以退出。

```
public void quit() {
    mQueue.quit(false); //全部消息移除
}


public void quitSafely() {
    mQueue.quit(true); //只移除没有执行的消息
}
```
### 如何做到延迟发送消息

 - **会根据Message发送时的时间戳确定Message在MessageQueue中的位置。**

	1. 放入Message时会根据`msg.when`这个时间戳进行顺序的排序,如果非延迟消息则msg.when为系统当前时间，延迟消息则为系统当前时间+延迟时间(如延迟发送3秒则为SystemClock.uptimeMillis() + 3000)
	2. 将Message放入MessageQueue时会以msg.when对msg进行排序确认当前msg处于单链表中的位置,分为几种情况:
 **(1)头结点为null(代表MessageQueue没有消息)**,Message直接放入头结点。
 **(2) 头结点不为null时开启死循环遍历所有节点**，退出死循环的条件是:
                    1.遍历出的节点的next节点为null(说明当前链表已经遍历到了末尾，将放入的Message放入next节点).
                    2.遍历出的节点的when大于放入message的when(说明当前message是一个比放入message延迟更久的消息，将放入的Message放入当前遍历的Message节点之前).

 - 当我们发送消息的时候其实最后都会调用到`sendMessageAtTime`这个方法，这个方法其实最终会把你的Handler对象赋值给Message实体,我们最终发送消息都是发送的Message实体，然后调用MessageQueue的enqueueMessage方法

```
public final boolean sendMessageDelayed(Message msg, long delayMillis)
{
    if (delayMillis < 0) {
        delayMillis = 0;
    }
    return sendMessageAtTime(msg, SystemClock.uptimeMillis() + delayMillis);
}

public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
    MessageQueue queue = mQueue;
    if (queue == null) {
        RuntimeException e = new RuntimeException(
                this + " sendMessageAtTime() called with no mQueue");
        Log.w("Looper", e.getMessage(), e);
        return false;
    }
    return enqueueMessage(queue, msg, uptimeMillis);
}

private boolean enqueueMessage(MessageQueue queue, Message msg, long uptimeMillis) {
    msg.target = this;
    if (mAsynchronous) {
        msg.setAsynchronous(true);
    }
    return queue.enqueueMessage(msg, uptimeMillis);
}
```

 - MessageQueue 是一个单链表结构，其中的Message节点是以Message放入MessageQueue的时间去进行顺序确定的(小的在前大的再后)，这样就完成了消息的延迟发送

```
boolean enqueueMessage(Message msg, long when) {
    // 每一个普通Message必须有一个target
    if (msg.target == null) {
        throw new IllegalArgumentException("Message must have a target.");
    }
    if (msg.isInUse()) {
        throw new IllegalStateException(msg + " This message is already in use.");
    }
    synchronized (this) {
        if (mQuitting) {  //正在退出时，回收msg，加入到消息池
            msg.recycle();
            return false;
        }
        msg.markInUse();
        msg.when = when;
        Message p = mMessages;
        boolean needWake;
        if (p == null || when == 0 || when < p.when) { //p为null(代表MessageQueue没有消息） 或者msg的触发时间是队列中最早的， 则进入该分支并将加入的message放入头结点
            msg.next = p;
            mMessages = msg;
            needWake = mBlocked; //当阻塞时需要唤醒
        } else {
            needWake = mBlocked && p.target == null && msg.isAsynchronous();
            Message prev;
            for (;;) {                             //开启死循环遍历message
                prev = p;
                p = p.next;
                if (p == null || when < p.when) {  //退出条件为当前message的下一个节点为null或者当前节点的message执行时间大于你放入message的执行时间
                    break;
                }
                if (needWake && p.isAsynchronous()) {
                    needWake = false;
                }
            }
            msg.next = p;                           //进行赋值
            prev.next = msg;
        }
        //消息没有退出，我们认为此时mPtr != 0
        if (needWake) {                        
            nativeWake(mPtr);                        //往管道中写数据,唤醒阻塞,nativePollOnce方法阻塞解除
        }
    }
    return true;
}
```
### MessageQueue的机制

-  `Message的入列和出列其实是一个很典型的**生产者-消费者模型**,其中使用了epoll机制，当没有消息的时候会进行阻塞释放CPU时间片避免死循环造成性能的浪费。`虽然是不断循环取出头结点的Message进行分发处理但是`如果没有消息时它是阻塞在 nativePollOnce这个native方法中的`，`当我们enqueue插入Message时会触发nativeWake这个方法去唤醒`,从而nativePollOnce阻塞解除继续遍历MessageQueue取出头结点去处理。
- Looper.loop()在一个线程中调用next()不断的取出消息，另外一个线程则通过enqueueMessage向队列中插入消息，`所以在这两个方法中使用了synchronized (this) {}同步机制，其中this为MessageQueue对象，不管在哪个线程，这个对象都是同一个`，因为Handler中的mQueue指向的是Looper中的mQueue，这样防止了多个线程对同一个队列的同时操作(如增加的同时正在轮询获取Message，有可能造成MessageQueue中最终结果的不确定性)。

```
Message next() {
    final long ptr = mPtr;
    if (ptr == 0) { //当消息循环已经退出，则直接返回
        return null;
    }
    int pendingIdleHandlerCount = -1; // 循环迭代的首次为-1
    int nextPollTimeoutMillis = 0;
    for (;;) {
        if (nextPollTimeoutMillis != 0) {
            Binder.flushPendingCommands();
        }
        //阻塞操作，当等待nextPollTimeoutMillis时长，或者消息队列被唤醒，都会返回
        //nextPollTimeoutMillis 为-1，一直阻塞，在调用nativeWake（enqueue Message或Looper.quit()退出Looper）时会被唤醒解除阻塞
        //nextPollTimeoutMillis 为0，不阻塞
        //nextPollTimeoutMillis 为>0，阻塞到对应时间后解除，如为10000则阻塞十秒后解除，用于处理延迟消息
        nativePollOnce(ptr, nextPollTimeoutMillis);
        synchronized (this) {
            final long now = SystemClock.uptimeMillis();
            Message prevMsg = null;
            Message msg = mMessages;
            if (msg != null && msg.target == null) {
                //当消息Handler为空时，查询MessageQueue中的下一条异步消息msg，则退出循环。
                do {
                    prevMsg = msg;
                    msg = msg.next;
                } while (msg != null && !msg.isAsynchronous());
            }
            if (msg != null) {
                if (now < msg.when) {                    
                    //说明是延迟消息，计算延迟的时间
                    nextPollTimeoutMillis = (int) Math.min(msg.when - now, Integer.MAX_VALUE);
                } else {
                    // 获取一条消息，并返回
                    mBlocked = false;
                    if (prevMsg != null) {
                        prevMsg.next = msg.next;
                    } else {
                        mMessages = msg.next;
                    }
                    msg.next = null;
                    //设置消息的使用状态，即flags |= FLAG_IN_USE
                    msg.markInUse();
                    return msg;   //成功地获取MessageQueue中的下一条即将要执行的消息
                }
            } else {
                //没有消息,阻塞
                nextPollTimeoutMillis = -1;
            }
            //消息正在退出，返回null
            if (mQuitting) {
                dispose();
                return null;
            }
            //当消息
```
### Message分发的三个的优先级
当遍历出Message后Message会获取其中的Handler并调用Handler的dispatchMessage进行分发,这时也会有三个优先级。

1. Message的回调方法：message.callback.run()，优先级最高；   对应handler.post(new Runnable)的方式发送消息
2. Handler的回调方法：Handler.mCallback.handleMessage(msg)，优先级仅次于1； 对应新建Handler时传进CallBack接口  Handler handler=new Handler(new Handler.Callback()....(`通常我们可以利用 Callback 这个拦截机制来拦截 Handler 的消息，场景如：Hook ActivityThread.mH，在 ActivityThread 中有个成员变量 mH ，它是个 Handler，又是个极其重要的类，几乎所有的插件化框架都使用了这个方法。）`
3. Handler的默认方法：Handler.handleMessage(msg)，优先级最低。    对应新建Handler并复写handleMessage方法

```
public void dispatchMessage(Message msg) {
    if (msg.callback != null) {             //callback是一个runnable对象
        handleCallback(msg);
    } else {
        if (mCallback != null) {            //mCallback是新建Handler时传进去的Callback接口
            if (mCallback.handleMessage(msg)) {
                return;
            }
        }
        handleMessage(msg);                //默认空实现,一般我们会自己复写实现这个方法
    }
}

private static void handleCallback(Message message) {
    message.callback.run();
}
```

1. 将 Runnable post 到主线程执行（很多第三方框架都使用的这种方式方便的完成主线程的切换，这也是为什么有handler.post(new Runnable)这种方式去发送消息）。
2. 利用 Looper 判断当前线程是否是主线程。

```
public final class MainThread {
    private MainThread() {
    }

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    public static void run(@NonNull Runnable runnable) {
        if (isMainThread()) {
           runnable.run();
        }else{
            HANDLER.post(runnable);
        }
    }

    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

}
```
### Handler线程调度的实质
`Handler的实质其实就是共享内存`,我们看一个例子。

```
public class Demo {
    List mList= new ArrayList()<Message>;
    public static void main(String[] args) {
        //子线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000l);
                    mList.add(new Message());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            }
        }).start();
        //主线程开启死循环不断遍历list取头结点
        for (;;) {
            //主线程中处理
            Message message = mList.get(0);
            if (message != null) {
                //处理
            }
        }

    }
}
```
我们为了将数据最终从子线程切换到主线程中去其实只要拿到mList这个实例即可,`这个mList对应的其实就是MessageQueue，而我们要获取MessageQueue只要获取对应的Looper即可,当我们Handler新建的时会根据Handler所在线程获取到其线程正在轮询消息的Looper对象，Handler中的mQueue指向的是其所在线程的Looper中的mQueue(当然也可以手动指定一个其他线程的Looper,不指定的话默认为当前线程的Looper)，由此便可在发送Message时将任务放到Looper所在线程中处理。`

```
public Handler(Callback callback, boolean async) {
    ...
    mLooper = Looper.myLooper(); //threadLocal.get获取线程对应的Looper
    if (mLooper == null) {
        throw new RuntimeException(
            "Can't create handler inside thread that has not called Looper.prepare()");
    }
    mQueue = mLooper.mQueue;     //通过Looper获取MessageQueue
    mCallback = callback;
    mAsynchronous = async;
}
```
### Looper.loop是一个死循环，拿不到需要处理的Message就会阻塞，那在UI线程中为什么不会导致ANR？
**首先我们来看造成ANR的原因：**
1. 当前的事件没有机会得到处理（即主线程正在处理前一个事件，没有及时的完成或者looper被某种原因阻塞住了）。
2. 当前的事件正在处理，但没有及时完成。


我们再来看一下APP的入口ActivityThread的main方法：

```
public static void main(String[] args) {
  
        ...

        Looper.prepareMainLooper();

        ActivityThread thread = new ActivityThread();
        thread.attach(false);

        if (sMainThreadHandler == null) {
            sMainThreadHandler = thread.getHandler();
        }

        Looper.loop();

        throw new RuntimeException("Main thread loop unexpectedly exited");
    }
```
我们知道Android 的是由事件驱动的，looper.loop() 不断地接收事件、处理事件，每一个点击触摸或者说Activity的生命周期都是运行在 Looper的控制之下，如果它停止了，应用也就停止了。`真正的阻塞是因为轮询出message后在处理message消息的时候由于执行了耗时操作导致了ANR，而不是死循环导致的阻塞，没有消息处理的时候消息队列是阻塞在nativePollOnce方法中的，**这个方法使用的是epoll管道机制，Linux底层执行后会释放CPU避免不断死循环造成的CPU浪费。**`
### Handler 引起的内存泄露原因以及最佳解决方案
`当我们用Handler发送延时消息时，如果在延时期间用户关闭了 Activity，那么该 Activity 会泄露。`这个泄露是因为 Message 会持有 Handler，而又因为 Java 的特性，内部类会持有外部类，使得 Activity 会被 Handler 持有，这样最终就导致 Activity 泄露。 解决该问题的最有效的方法是：`将 Handler 定义成静态的内部类（静态内部类不会持有外部类引用，但是静态内部类调用不到外部类的非静态属性和方法，所以我们需要在内部类中使用弱引用持有Activity，使用弱引用调用到Activity中的方法），并及时移除所有消息。
泄漏时的引用链 Activity->Handler->Message->MessageQueue ,延迟消息会一直在MessageQueue中等待处理，在等待的过程中有可能会造成内存泄漏。`
示例代码如下：

```
private static class SafeHandler extends Handler {

    private WeakReference<HandlerActivity> ref;

    public SafeHandler(HandlerActivity activity) {
        this.ref = new WeakReference(activity);
    }

    @Override
    public void handleMessage(final Message msg) {
        HandlerActivity activity = ref.get();
        if (activity != null) {
            activity.handleMessage(msg);
        }
    }
}
```

**并且再在 Activity.onDestroy() 前移除消息，加一层保障：**

```
@Override
protected void onDestroy() {
  safeHandler.removeCallbacksAndMessages(null);
  super.onDestroy();
}
```
这样双重保障，就能完全避免内存泄露了。
`注意：单纯的在 onDestroy 移除消息并不保险，因为 onDestroy 并不一定执行（如报异常）。`
### IdleHandler 是什么
从下边MessageQueue的源码可知道，`IdleHandler即在MessageQueue应该被阻塞之前去调用(当然前提是你要讲自定义的IdleHandler加入到集合中)`。
`IdleHandler接口表示当MessageQueue发现当前没有更多消息可以处理的时候,` 则顺便干点别的事情的callback函数(即如果发现idle了, 
那就找点别的事干). callback函数有个boolean的返回值, 表示是否keep. 如果返回false, 则它会在调用完毕之后从mIdleHandlers中移除.
IdleHandler 可以用来提升提升性能，主要用在我们希望能够在当前线程消息队列空闲时做些事情（譬如UI线程在显示完成后，如果线程空闲我们就可以提前准备其他内容）的情况下，**`不过最好不要做耗时操作`**。

```
Message next() {
    final long ptr = mPtr;
    if (ptr == 0) { //当消息循环已经退出，则直接返回
        return null;
    }
    int pendingIdleHandlerCount = -1; // 循环迭代的首次为-1
    int nextPollTimeoutMillis = 0;
    for (;;) {
        if (nextPollTimeoutMillis != 0) {
            Binder.flushPendingCommands();
        }
        //阻塞操作，当等待nextPollTimeoutMillis时长，或者消息队列被唤醒，都会返回
        //nextPollTimeoutMillis 为-1，一直阻塞，在调用nativeWake（enqueue Message或Looper.quit()退出Looper）时会被唤醒解除阻塞
        //nextPollTimeoutMillis 为0，不阻塞
        //nextPollTimeoutMillis 为>0，阻塞到对应时间后解除，如为10000则阻塞十秒后解除，用于处理延迟消息
        nativePollOnce(ptr, nextPollTimeoutMillis);
        synchronized (this) {
            final long now = SystemClock.uptimeMillis();
            Message prevMsg = null;
            Message msg = mMessages;
            if (msg != null && msg.target == null) {
                //当消息Handler为空时，查询MessageQueue中的下一条异步消息msg，则退出循环。
                do {
                    prevMsg = msg;
                    msg = msg.next;
                } while (msg != null && !msg.isAsynchronous());
            }
            if (msg != null) {
                if (now < msg.when) {                    
                    //说明是延迟消息，计算延迟的时间
                    nextPollTimeoutMillis = (int) Math.min(msg.when - now, Integer.MAX_VALUE);
                } else {
                    // 获取一条消息，并返回
                    mBlocked = false;
                    if (prevMsg != null) {
                        prevMsg.next = msg.next;
                    } else {
                        mMessages = msg.next;
                    }
                    msg.next = null;
                    //设置消息的使用状态，即flags |= FLAG_IN_USE
                    msg.markInUse();
                    return msg;   //成功地获取MessageQueue中的下一条即将要执行的消息
                }
            } else {
                //没有消息,阻塞
                nextPollTimeoutMillis = -1;
            }
            //消息正在退出，返回null
            if (mQuitting) {
                dispose();
                return null;
            }
            //如果当前MessageQueue头结点为空(没有消息要处理了)或者当前系统时间<消息触发时间
            if (pendingIdleHandlerCount < 0 && (mMessages == null || now < mMessages.when)) {
            //看是否加入了idleHandler
                pendingIdleHandlerCount = mIdleHandlers.size();
            }
            if (pendingIdleHandlerCount <= 0) {
                //没有idle handlers 需要运行，则循环并等待。
                mBlocked = true;
                continue;
            }
            if (mPendingIdleHandlers == null) {
                mPendingIdleHandlers = new IdleHandler[Math.max(pendingIdleHandlerCount, 4)];
            }
            mPendingIdleHandlers = mIdleHandlers.toArray(mPendingIdleHandlers);
        }
        //只有第一次循环时，会运行idle handlers，执行完成后，重置pendingIdleHandlerCount为0.
        for (int i = 0; i < pendingIdleHandlerCount; i++) {
            final IdleHandler idler = mPendingIdleHandlers[i];
            mPendingIdleHandlers[i] = null; //去掉handler的引用
            boolean keep = false;
            try {
                //如果queueIdle()返回false则当前idlehandler只能运行一次
                keep = idler.queueIdle();  //idle时执行的方法
            } catch (Throwable t) {
                Log.wtf(TAG, "IdleHandler threw exception", t);
            }
            if (!keep) {
                synchronized (this) {
                    //queueIdle返回false,移除idlehandler
                    mIdleHandlers.remove(idler);
                }
            }
        }
        //重置idle handler个数为0，以保证不会再次重复运行
        pendingIdleHandlerCount = 0;
        //当调用一个空闲handler时，一个新message能够被分发，因此无需等待可以直接查询pending message.
        nextPollTimeoutMillis = 0;
    }
}
```
### 同步屏障
同步屏障是由系统发送，一般用于刷新UI(如16ms刷新一次界面)。当设置了同步屏障之后，next函数将会忽略所有的同步消息，返回异步消息。**`换句话说就是，设置了同步屏障之后，Handler只会处理异步消息。再换句话说，同步屏障为Handler消息机制增加了一种简单的优先级机制，异步消息的优先级要高于同步消息。`**
- **如何判断是否为同步屏障消息?**
**当Message的target为null时（Message不持有Handler）则当前消息为异步消息，也就是同步屏障。**

Android应用框架中为了更快的响应UI刷新事件在ViewRootImpl.scheduleTraversals中使用了同步屏障

```
void scheduleTraversals() {
    if (!mTraversalScheduled) {
        mTraversalScheduled = true;
        //设置同步障碍，确保mTraversalRunnable优先被执行
        mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
        //内部通过Handler发送了一个异步消息
        mChoreographer.postCallback(
                Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
        if (!mUnbufferedInputDispatch) {
            scheduleConsumeBatchedInput();
        }
        notifyRendererOfFramePending();
        pokeDrawLockIfNeeded();
    }
}
```
