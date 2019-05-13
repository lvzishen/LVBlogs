
#### 目录介绍

- 1.View测量布局绘制整体流程
    - 1.1 MeasureSpec是什么?
    - 1.2 LayoutParams是什么？    
    - 1.3 View的测量流程(Measure)
    - 1.4 在getChildMeasureSpec方法中都做了什么？
    - 1.5 Layout布局过程
    - 1.6 Draw过程
- 2.getWidth，getMeasureWidth的区别
- 3.requestLayout()、invalidate()与postInvalidate()有什么区别？
- 4.自定义View整体思想和类型
- 5.什么时候可以获取到View的宽高，为什么？
- 6.获取控件宽高的几种方法
- 7.子线程中真的不能更新UI吗？
- 8.常用布局测量流程
    - 8.1 LinearLayout
    - 8.2 FrameLayout
    - 8.3 RelativeLayout


###1. View测量布局绘制整体流程
**首先明确两个概念：**
####1.1 MeasureSpec是什么?
MeasureSpec是一个大小跟模式的组合值,MeasureSpec中的值是一个整型（32位）将size和mode打包成一个Int型，其中高两位是mode，后面30位存的是size，为了减少对象的分配开支所以使用了int类型去进行存储。要注意的是一般的int值是十进制的数，而MeasureSpec 是二进制存储的。一定要注意的是**`MeasureSpec是父View对子View的期望宽高要求`，可以认为是父View传递给子View的。**
**SpecMode有三类**，每一类都表示特殊的含义，如下所示：
1.   **`UNSPECIFIED`**： 父容器不对View有任何限制，要多大给多大，这种情况一般用于系统内部，表示一种测量的状态。 (如ListView或ScrollView)
2.  **`EXACTLY`** ：一个明确的大小值，如多少多少dp或matchparent
3.  **`AT_MOST`** ：对应于LayoutParams中的wrap_content。

####1.2 LayoutParams是什么？
其实其中保存的就是我们XML文件对View的赋值。
```
<View    
	android:layout_width="100dp"    
	android:layout_height="100dp"   />
```
比如上面这种情况layoutParams.width和layoutParams.height就是100dp
**具体分为三种**：
	1.  **`LayoutParams.MATCH_PARENT`**：精确模式，大小就是窗口的大小；
	2.  **`LayoutParams.WRAP_CONTENT`**：最大模式，大小不定，但是不能超过窗口的大小；
	3.  **`具体的大小值（比如100dp）`**：精确模式，大小为LayoutParams中指定的大小。

####1.3 View的测量流程(Measure)：
首先由一段代码来说明
代码所示：
```
protected void onMeasure(int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
    for (int i = 0; i < getChildCount(); i++) {
        View child = getChildAt(i);
        //获取子View的LayoutParams
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        //根据子View自身的LayoutParams和父View的MeasureSpec和可用空间获取子View自身的MeasureSpec
        //获取宽度MeasureSpec
        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                mPaddingLeft + mPaddingRight + lp.leftMargin + lp.rightMargin
                        + widthUsed, lp.width);
        //获取高度MeasureSpec
        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                mPaddingTop + mPaddingBottom + lp.topMargin + lp.bottomMargin
                        + heightUsed, lp.height);
        //根据父View对子View的期望MeasureSpec结合自身的规则进行最终的测量得出自身的期望宽高
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        widthUsed+=child.getMeasuredWidth();
        heightUsed+=child.getMeasuredHeight();
    }
    //给父View设置上最终的期望宽高
    setMeasuredDimension(widthUsed, heightUsed);
}
```
####**以ViewGroup为例**
1.`首先会遍历所有子View。(for循环)`
2.`根据子View自身的LayoutParams和父View自身的MeasureSpec以及父View的可用空间获取子View自身的MeasureSpec，这个MeasureSpec是父View对子View的期望宽高。`(对应getChildMeasureSpec方法，最终在getChildMeasureSpec方法中使用MeasureSpec.makeMeasureSpec(size, mode) 来求得结果)
（有这一步的原因是因为我们在XML中定义的View宽高比如说是match_parent或wrap_content这种格式，那么我们其实并不知道他具体应该被赋值多大，google就要帮我们计算你match_parent的时候是多大，wrap_content的是多大，这个计算过程，就是计算出来的父View的MeasureSpec不断往子View传递，结合子View的LayoutParams 一起再算出子View的MeasureSpec，然后继续传给子View，不断计算每个View的MeasureSpec，子View有了MeasureSpec才能测量自己和自己的子View。）
3.`子View根据父View对其的期望宽高和自身的规则算出其最终的期望宽高。`(child.measure(childWidthMeasureSpec, childHeightMeasureSpec))
（这里的自身规则指的是其在OnMeasure中的逻辑，比如TextView会根据其中字符串的长度高度确定最终的大小值）。

####MeasureSpec中的值既然是父View对子View的期望值，那么最外层的View是如何设置的？
在最外层的DecorView中，有这样一段代码:
```
private void performTraversals() {
......
int childWidthMeasureSpec = getRootMeasureSpec(mWidth, lp.width);
int childHeightMeasureSpec = getRootMeasureSpec(mHeight, lp.height);
......
mView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
......
mView.layout(0, 0, mView.getMeasuredWidth(), mView.getMeasuredHeight());
......
mView.draw(canvas);
......
}

private static int getRootMeasureSpec(int windowSize, int rootDimension) {
   int measureSpec;
   switch (rootDimension) {
   case ViewGroup.LayoutParams.MATCH_PARENT:
   measureSpec = MeasureSpec.makeMeasureSpec(windowSize,MeasureSpec.EXACTLY);
   break;
   ......
  }
return measureSpec;
}
```
可以看到我们最外层的View也就是DecorView中根据getRootMeasureSpec这个方法获取的**`MeasureSpec的Mode是EXACTLY,size是屏幕的宽高。`**
也就是说我们最外层的DecorView中默认的宽高就是屏幕的宽高，EXACTLY代表固定大小。

#### 1.4 在getChildMeasureSpec方法中都做了什么？
**`在这个方法中子View根据自身的LayoutParams和父View自身的MeasureSpec及可用空间获取子View自身的MeasureSpec。`**
![](https://user-gold-cdn.xitu.io/2019/5/13/16ab057998dc5f5c?w=952&h=257&f=png&s=71595)
可以看到当我们定义子View为match_parent或wrap_content的时候，最终生成的MeasureSpec的Size为父View的大小，而在View的默认实现中当调用measure开始测量后走到onMearsure设置最终期望宽高的时候默认实现为直接使用MeasureSpec中的Size值。
```
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {    
    setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                     getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
 }

public static int getDefaultSize(int size, int measureSpec) {
    int result = size;
    int specMode = MeasureSpec.getMode(measureSpec);
    int specSize = MeasureSpec.getSize(measureSpec);
    switch (specMode) {
    case MeasureSpec.UNSPECIFIED:
        result = size;
        break;
    case MeasureSpec.AT_MOST:
    case MeasureSpec.EXACTLY:
        result = specSize;
        break;
    }
    return result;
}
```
也就是说当我们自定义View的时候如果我们需要使自己的View支持wrap_content，那么就必须重写OnMeasure方法并对wrap_content做一个特殊的测量,否则在wrap_content的情况下我们自定义View的大小就会和父View的大小相同。

#### 1.5 Layout布局过程
Layout的作用是ViewGroup用来确定子元素的位置，当ViewGroup的位置被确定后，它在onLayout中会遍历所有的子元素并调用其layout方法，在layout方法中的onLayout方法又会被调用。
layout方法中会调用setFrame方法保存其在ViewGroup中的位置，自定义ViewGroup的时候必须重写OnLayout方法，在其中进行子View位置的设置。
- 在View中onLayout默认是一个空实现
```
protected void onLayout(boolean changed, int left, int top, int right, int bottom) {  
}  
```
- 在ViewGroup中是抽象方法，**`所以重写ViewGroup的时候必须去实现OnLayout方法。`**
```
@Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.layout(l, t, r,b);
        } 
```
具体的计算过程可以看下最简单FrameLayout 的onLayout 函数的源码，每个不同的ViewGroup 的实现都不一样。
MeasuredWidth和MeasuredHeight这两个参数为layout过程提供了一个很重要的依据（如果不知道View的大小，你怎么固定四个点的位置呢），但是这两个参数也不是必须的，layout过程中的4个参数l, t, r, b完全可以由我们任意指定，而View的最终的布局位置和大小（mRight - mLeft=实际宽或者mBottom-mTop=实际高）完全由这4个参数决定，但通常情况下用的就是第一步在measure过程中计算出来的期望宽高。
`从measure和layout方法中可以看出的另一点是measure只是进行一些初始化参数的工作，真正的测量逻辑是在OnMeasure中进行的。而layout方法直接对你的View进行了位置和大小的确定，真正的逻辑不是在OnLayout中进行的。`

#### 1.6 Draw过程
View的绘制主要分为四部分：
1. ` 绘制背景background.draw(canvas)。`
2. ` 绘制自己（onDraw）。`
3.  `绘制children（dispatchDraw）。`
4.  `绘制装饰（onDrawScrollBars）。`
####OnDraw
onDraw(canvas) 方法是view用来draw 自己的，具体如何绘制，颜色线条什么样式就需要子View自己去实现，View.java 的onDraw(canvas) 是空实现，ViewGroup 也没有实现，每个View的内容是各不相同的，所以需要由子类去实现具体逻辑。
####dispatchDraw
dispatchDraw(canvas) 方法是用来绘制子View的，View.java 的dispatchDraw()方法是一个空方法,因为View没有子View,不需要实现dispatchDraw ()方法，ViewGroup就不一样了，它实现了dispatchDraw ()方法并在其中遍历子View然后调用子View的draw()方法。

**`当我们自定义ViewGroup的时候默认是不会执行OnDraw方法的（ViewGroup默认调用了setWillNotDraw（true），因为系统默认认为我们不会在ViewGroup中绘制内容），我们如果需要进行绘制可以在dispatchDraw中去进行或者调用setWillNotDraw(false)方法。`**
从setWillNotDraw这个方法的注释中可以看出，如果一个View不需要绘制任何内容，那么设置这个标记位为true以后，系统会进行相应的优化。默认情况下，View没有启用这个优化标记位，但是ViewGroup会默认启用这个优化标记位。这个标记位对实际开发的意义是：当我们的自定义控件继承于ViewGroup并且本身不具备绘制功能时，就可以开启这个标记位从而便于系统进行后续的优化。当然，当明确知道一个ViewGroup需要通过onDraw来绘制内容时，我们需要显式地关闭WILL_NOT_DRAW这个标记位。
```
/**
* If this view doesn't do any drawing on its own,set this flag to
* allow further optimizations. By default,this flag is not set on
* View,but could be set on some View subclasses such as ViewGroup.
*
* Typically,if you override {@link #onDraw(android.graphics.Canvas)}
* you should clear this flag.
*
* @param willNotDraw whether or not this View draw on its own
*/
public void setWillNotDraw(boolean willNotDraw) {
setFlags(willNotDraw ? WILL_NOT_DRAW : 0,DRAW_MASK);
}
```
### 2.getWidth，getMeasureWidth的区别
首先要明确一点，测量得到的宽高并不一定是View的最终宽高，当measure执行完毕后（准确的是我们在onMeasure中调用setMeasuredDimension(width,height)方法后）我们就可以得到View的一个期望宽高，通常情况下期望宽高是和最终的宽高相同的，但是也有特殊情况(比如在layout方法最终赋值View宽高的时候手动的修改值而不用测量得到的值)。
- getMeasureWidth()方法在measure()过程结束后就可以获取到了，另外，getMeasureWidth()方法中的值是通过setMeasuredDimension()方法来进行设置的。
- getWidth()方法要在layout()过程结束后才能获取到，当在layout方法中调用setFrame()后就可以获取此值了，这个值是View的真实宽高。
    - getWidth()方法中的值则是通过视图右边的坐标减去左边的坐标计算出来的。

 ```
 public final int getWidth() {
     return mRight - mLeft;
}
 ```
### 3.requestLayout()、invalidate()与postInvalidate()有什么区别？
invalidate和postInvalidate都是调用onDraw()方法，然后去达到重绘view的目的。
invalidate()用于主线程，postInvalidate()用于子线程， postInvalidate的原理其实就是通过主线程的handler完成线程的调度最终在主线程中调用invalidate方法。
requestLayout()会调用measure和layout方法，当View的大小位置需要改变的时候调用。如果view的大小发生了变化那么requestlayout也会调用draw()方法。

###  4.自定义View整体思想和类型
自定义View
**1.继承自系统View（ImageView，TextView等）**
一般重写OnMearsure方法，因为系统View再其自身的OnMearsure，OnDraw中都处理好了内容，我们一般不需要进行修改，复写的时候通常直接super父类方法然后实现自己的逻辑即可。
比如实现一个正方形的ImageView

**2.继承View**
如果你的View是定义了明确宽高的话，那么通常不需要我们重写OnMeasure的，如果宽高定义为了wrap_content的话我们需要早OnMeasure中针对wrap_content这种模式进行一个修改并设置最终宽高，因为默认情况下View的wrap_content和match_parent大小是相同的(在getChildMeasureSpec方法计算得出)。
如果我们的一些用到的属性是跟View的大小变化相关的话，那么我们可以通过OnSizeChanged去进行监听(OnSizeChanged在layout方法中的setFrame执行时会被调用，也就是说当我们调用requestLayout时可以通过OnSizeChanged去获取新的控件宽高等值)。
我们可以在OnDraw中进行内容的绘制，onDraw不要进行过多的耗时操作，如频繁的创建对象。

**3.继承自ViewGroup**
需要重写OnMeasure并且对子View进行遍历测量，然后自身去调用setMeasureDimens设置自身宽高。
onLayout必须重写并遍历子View调用其layout方法进行布局和大小的确定。（如果不调用会没有子View显示）
onDraw默认不执行，如果需要进行绘制可以调用setWillNotDraw（false）取消onDraw的禁用或者在dispatchDraw中进行绘制。
`TagLayout（流式布局）布局思路`：
需要定义一个已使用宽度(widthUsed)和高度(heightUsed)，在OnMeasure执行完对所有子View测量后，OnLayout方法中根据自身定义的规则如果widthUsed+view.getMeasureWidth>viewGroup.getMeasureWidth的话需要进行换行，widthUsed清零且heightUsed+=view.getMeasureHeight,子View调用layout时传入的四个点坐标就是(widthUsed,heightUsed,widthUsed+view.getMeasureWidth,heightUsed+view.getMeasureHeight)，以此类推完成所有子View的布局;

**4.继承自系统ViewGroup**
这种情况不需要我们重写OnMearsure和OnLayout，因为系统已经帮我们写好了，通常这种情况下是我们将自己定义的布局添加到ViewGroup中，对整个的View进行一个封装复用。

### 5.什么时候可以获取到View的宽高，为什么？
在OnResume执行完后可以获取宽高，因为View的测绘流程是由ViewRootImpl的performTraversals开始的。当Activity创建时执行到handleResumeActivity方法中先会执行`OnResume方法然后WindowManager会调用addView将DecorView添加进去，之后ViewRootImpl才会被创建出来从而调用performTraversals开始View的测绘流程。`
```
final void handleResumeActivity( ... ... ) {
     // 最终会执行到 onResume()，不是重点
     r = performResumeActivity(token, clearHide, reason);

     if (r != null) {
         final Activity a = r.activity;

         if (r.window == null && !a.mFinished && willBeVisible) {
             r.window = r.activity.getWindow();
             View decor = r.window.getDecorView();
             ViewManager wm = a.getWindowManager();
             // 5. 执行到 WindowManagerImpl 的 addView()
             // 然后会跳转到 WindowManagerGlobal 的 addView()
             if (a.mVisibleFromClient) {
                 if (!a.mWindowAdded) {
                     a.mWindowAdded = true;
                     wm.addView(decor, l);
                 }
             }
         }
     }
}

public void addView( ... ... ) {
     ViewRootImpl root;
     synchronized (mLock) {
         // 初始化一个 ViewRootImpl 的实例
         root = new ViewRootImpl(view.getContext(), display);
         try {
             // 调用 setView，为 root 布局 setView
             // 其中 view 为传下来的 DecorView 对象
             // 也就是说，实际上根布局并不是我们认为的 DecorView，而是 ViewRootImpl
             root.setView(view, wparams, panelParentView);
         }
     }
}

// 6. 将 DecorView 加载到 WindowManager, View 的绘制流程从此刻才开始public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
    // 请求对 View 进行测量和绘制
    // 与 setContentView() 不同，此处的方法是 ViewRootImpl 的方法
    requestLayout();
}

@Overridepublic void requestLayout() {
    if (!mHandlingLayoutInLayoutRequest) {
        checkThread();
        mLayoutRequested = true;
        // 7. 此方法内部有一个 post 了一个 Runnable 对象
        // 在其中又调用一个 doTraversal() 方法；
        // 再之后又会调用到 performTraversals() 方法，然后 View 的测绘流程就从此处开始了
        scheduleTraversals();
    }
}

private void performTraversals() {
    ... ...
    // Ask host how big it wants to be
    performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
    ... ...
    performLayout(lp, mWidth, mHeight);
    ... ...
    performDraw();
    ... ...
}
```
### 6.获取控件宽高的几种方法
1.**onWindowFocusChanged**
这个方法会被调用多次，在View初始化完毕后会调用，当Activity的窗口得到焦点和失去焦点都会被调用一次（Activity继续执行和暂停执行时）。

2.**ViewTreeObserver**
当View树的状态发生改变或者View树内部的View可见性发现改变时，onGlobalLayout方法将被回调。

3.**View.post（new Runnble）**
`内部分两种情况:`
第一种View已经完成测绘（这种直接调用主线程handler.post(new Runnable)发送一个Message并回调给Runnble处理）
第二种View没有完成测绘，这种会先将Runnble任务通过数组保存下来，当View开始测绘时（ViewRootImpl.performTraversals()）会将包存下来的Runnble任务通过主线程handler进行发送消息，由于消息在messagequeue中是串行处理的，所以view.post的Runnble任务会在view的测绘完成后在开始执行其自身的消息，这时View已经完成测绘，自然就可以获取到宽高了。
更详细的可参考: http://www.cnblogs.com/dasusu/p/8047172.html

### 7.子线程中真的不能更新UI吗？
众所周知安卓不允许在非UI线程中去更新UI，每当我们对View状态做出改变的时候（如调用requestLayout()或invalidate()等方式时）都会去检查当前线程是否是主线程，而**`检查线程的判断是在ViewRootImpl的checkThread()方法中去执行的。`**也就是说在ViewRootImpl没有创建出来的时候（OnResume执行完后ViewRootImpl才创建出来的）checkThread()这一步检测是不会执行的，在这种情况下我们在子线程中是可以更新UI的。
```
ViewRootImpl.java
void checkThread() {
           if (mThread != Thread.currentThread()) {
              throw new CalledFromWrongThreadException(
                "Only the original thread that created a view hierarchy can touch its views.");
             }
}
```
### 8.常用布局测量流程
####8.1 LinearLayout设置权重测量流程
详细分析可参考https://toutiao.io/posts/08f9tz/preview
**垂直布局分析**
设置了权重的View会被测量两次，没有只会测量一次。（特殊情况：如果子View的lp.weight>0且lp.height==0且LinearLayout设置了明确宽高的(mode==MeasureSpec.EXACTLY)情况下子View也只会测量一次。）

1.LinearLayout中的第一个循环会遍历所有的子View计算其高度并将高度进行累加。
- **如果子View的lp.weight>0且lp.height==0且LinearLayout设置了明确宽高的(mode==MeasureSpec.EXACTLY)情况下子View只会测量一次。**

第一次测量完成后会根据LinearLayout总高度-累加高度算出剩余高度，剩余高度有可能是负值，**最后根据剩余高度和总权重算出每一份权重的占比。**
2.第二个循环会对所有设置了权重weight的子View进行测量,并根据子View设置的权重值分配子View最终的高度。

结论：简而言之就是`第一次循环算出所有子View的高度和，然后用Linearlayout自身高度-已用高度算出剩余高度并根据剩余高度/总权重算出每一份权重的大小，第二次循环给设置了权重的View根据权重设置的值分配大小。`

#### 8.2 FrameLayout测量过程
FrameLayout只会测量一次，计算出所有子View的宽高之后，如果FrameLayout自身MeasureSpec.MODE=EXACTLY,那么它最终宽高就是设置的值，如果是MeasureSpec.MODE=AT_MOST(wrap_content)的话那么最终宽高会选取所有子View中的最大宽和最大高作为最终宽高。
```
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
...
for (int i = 0; i < count; i++) {
    final View child = getChildAt(i);
    if (mMeasureAllChildren || child.getVisibility() != GONE) {
        //子View测量自身宽高，因为Framelayout内部View可重叠放置所以当前可用宽高都传的0    
        measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        //记录最大宽高
        maxWidth = Math.max(maxWidth,
                child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
        maxHeight = Math.max(maxHeight,
                child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
        childState = combineMeasuredStates(childState, child.getMeasuredState());
        if (measureMatchParentChildren) {
            if (lp.width == LayoutParams.MATCH_PARENT ||
                    lp.height == LayoutParams.MATCH_PARENT) {
                mMatchParentChildren.add(child);
            }
        }
    }
}
   //修正最大宽高
// Account for padding too
maxWidth += getPaddingLeftWithForeground() + getPaddingRightWithForeground();
maxHeight += getPaddingTopWithForeground() + getPaddingBottomWithForeground();
// Check against our minimum height and width
maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());
// Check against our foreground's minimum height and width
final Drawable drawable = getForeground();
if (drawable != null) {
    maxHeight = Math.max(maxHeight, drawable.getMinimumHeight());
    maxWidth = Math.max(maxWidth, drawable.getMinimumWidth());
}
//设置最终FrameLayou宽高
setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
        resolveSizeAndState(maxHeight, heightMeasureSpec,
                childState << MEASURED_HEIGHT_STATE_SHIFT));
}

```

#### 8.3 RelativeLayout测量过程
在OnMeasure中会测量两次子View，第一次水平方向根据水平方向规则(toLeft,toBottom等)测量获取子View左右值（mLeft,mRight），高度可认为设置为最大值。第二次测量根据竖直方向的规则(Above,Bottom等)测量获取子View上下值(mTop,mBottom)。
**`为什么需要测量两次?`**
因为RelativeLayout子View之前既可以是水平依赖也可以是竖直依赖，所以水平竖直方向都需要去进行一次测量。
这里需要注意的一点是在规则的处理上alignParentLeft的优先级是高于toLeft的。
详情可见:https://www.jianshu.com/p/87bc61b8a195
![](https://user-gold-cdn.xitu.io/2019/5/13/16ab0541405cf05e?w=1000&h=1074&f=png&s=533457)
