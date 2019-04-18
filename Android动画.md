
#### 目录介绍

- 1.动画的分类
- 2.补间动画为何不能真正改变View的位置？而属性动画为何可以？属性动画是如何改变View的属性？
- 3.属性动画插值器和估值器的作用？插值器和估值器分别是如何更改动画的？
- 4.为什么属性动画最后会改变View的点击事件位置而View动画不会?
- 5.属性动画内存泄漏的原因
- 6.具体用法

### 动画的分类
1. **属性动画**： 对该类对象进行动画操作，真正改变了对象的属性。（ObjectAnimator，ValueAnimator）
2. **帧动画**：由一帧一帧的图片构建起来的动画效果,帧动画需要注意图片过大会发生OOM。
3. **补间动画(View动画)** ：对View进行平移、缩放、旋转和透明度变化的动画，不能真正的改变view的位置，限制比较大且种类只有四种 **`AlphaAnimation（透明度动画）、RotateAnimation（旋转动画）、ScaleAnimation（缩放动画）、TranslateAnimation（平移动画）`**且只能作用于View上。 （应用如Activity切换动画）
### 补间动画为何不能真正改变View的位置？而属性动画为何可以？属性动画是如何改变View的属性？
- View动画**`改变的只是View的画布`**，而没有改变View的点击响应区域；而属性动画会通过反射技术来获取和执行属性的get、set方法，从而**`改变了对象位置的属性值`**。
-  Animation产生的动画数据实际并不是应用在View本身的，而是应用在RenderNode或者Canvas上的（**`通过画布的移动实现动画`**），这就是为什么Animation不会改变View的属性的根本所在。我们可以理解为Animation只是操作的View画布而并不是改变View的位置（mLeft,mRight,mTop,mBottom）。

在View的draw()方法中：
```
final Animation a = getAnimation();//是否设置了动画
if (a != null) {
    more = applyLegacyAnimation(parent, drawingTime, a, scalingRequired);
    concatMatrix = a.willChangeTransformationMatrix();
    if (concatMatrix) {
        mPrivateFlags3 |= PFLAG3_VIEW_IS_ANIMATING_TRANSFORM;
    }
    transformToApply = parent.getChildTransformation();
} else {
    if ((mPrivateFlags3 & PFLAG3_VIEW_IS_ANIMATING_TRANSFORM) != 0) {
        // No longer animating: clear out old animation matrix
        mRenderNode.setAnimationMatrix(null);
        mPrivateFlags3 &= ~PFLAG3_VIEW_IS_ANIMATING_TRANSFORM;
    }
    if (!drawingWithRenderNode
            && (parentFlags & ViewGroup.FLAG_SUPPORT_STATIC_TRANSFORMATIONS) != 0) {
        final Transformation t = parent.getChildTransformation();
        final boolean hasTransform = parent.getChildStaticTransformation(this, t);
        if (hasTransform) {
            final int transformType = t.getTransformationType();
            transformToApply = transformType != Transformation.TYPE_IDENTITY ? t : null;
            concatMatrix = (transformType & Transformation.TYPE_MATRIX) != 0;
        }
    }
}
......
if (transformToApply != null) {
    if (concatMatrix) {
        if (drawingWithRenderNode) {
            renderNode.setAnimationMatrix(transformToApply.getMatrix());
        } else {
            // Undo the scroll translation, apply the transformation matrix,
            // then redo the scroll translate to get the correct result.
            canvas.translate(-transX, -transY);
            canvas.concat(transformToApply.getMatrix());
            canvas.translate(transX, transY);
        }
        parent.mGroupFlags |= ViewGroup.FLAG_CLEAR_TRANSFORMATION;
    }
  ......
}

```
在View的draw方法中我们可以看到当我们设置了动画之后会生成transformToApply对象，当transformToApply不为null的时候会进行根据动画的参数矩阵进行View的重新绘制。重点看到Animation产生的动画数据实际并不是应用在View本身的，而是应用在RenderNode或者Canvas上的，这就是为什么Animation不会改变View的属性的根本所在。另一方面，我们知道Animation仅在View被绘制的时候才能发挥自己的价值，这也是为什么补间动画被放在Android.view包内。
### 属性动画插值器和估值器的作用？插值器和估值器分别是如何更改动画的？
* 插值器(Interpolator)：根据时间流逝的百分比计算出当前属性值改变的百分比。确定了动画效果变化的模式，如匀速变化、加速变化等等。View动画和属性动画均可使用。（**`可理解为改变动画的速度曲线，默认使用的是先加速再减速的插值器`**）
* 常用的系统内置插值器：

    * 线性插值器(LinearInterpolator)：匀速动画
	* 加速减速插值器(AccelerateDecelerateInterpolator)：动画两头慢中间快
	* 减速插值器(DecelerateInterpolator)：动画越来越慢

自定义插值器实现Interpolator接口。
```
public class LinearInterpolator implements Interpolator {
    public LinearInterpolator() {
    }
    public LinearInterpolator(Context context,AttributeSet attrs) {
    }
    public float getInterpolation(float input) {
        return input;
    }
}
```
* 类型估值器(TypeEvaluator)：根据当前属性改变的百分比计算出改变后的属性值（计算在百分之多少的时候返回什么值）。针对于属性动画，View动画不需要类型估值器。常用的系统内置的估值器：

	* 整形估值器(IntEvaluator)
	* 浮点型估值器(FloatEvaluator)
	* Color属性估值器(ArgbEvaluator)

自定义估值器实现TypeEvaluator接口。
```
public class IntEvaluator implements TypeEvaluator<Integer> {
    public Integer evaluate(float fraction,Integer startValue,Integer endValue) {
        int startInt = startValue;      start:1,end:3 那么在0.2的时候则为:   1+(3-1)*0.2
        return (int)(startInt + fraction * (endValue -startInt));
    }
}
```
### 为什么属性动画最后会改变View的点击事件位置而View动画不会?
以下为例：
```
mView.setOnTouchListener(new View.OnTouchListener() {
    int lastX, lastY;
    Toast toast = Toast.makeText(TestActivity.this, "", Toast.LENGTH_SHORT);
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int x = (int) event.getRawX();
        int y = (int) event.getRawY();
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            //Toolbar和状态栏的高度
            int toolbarHeight = (getWindow().getDecorView().getHeight() - findViewById(R.id.root_view).getHeight());
            int widthOffset = mView.getWidth() / 2;
            int heightOffset = mView.getHeight() / 2;
            mView.setTranslationX(x - mView.getLeft() - widthOffset);
            mView.setTranslationY(y - mView.getTop() - heightOffset - toolbarHeight);
            toast.setText(String.format("left: %d, top: %d, right: %d, bottom: %d",
                    mView.getLeft(), mView.getTop(), mView.getRight(), mView.getBottom()));
            toast.show();
        }
        lastX = x;
        lastY = y;
        return true;
    }
});
```
![](https://user-gold-cdn.xitu.io/2019/4/18/16a300998f36d22b?w=360&h=640&f=gif&s=277305)

当我们调用了setTranslationX或setRotation等方法后其实改变的并不是View的真正位置，只是对View画布的改变。而**改变View真正坐标只能使用`view.layout()`方法**，那么为什么属性动画可以通过setTranslationX等方法改变View的点击事件区域呢？
因为在事件分发中当我们去判断View是否在手指点击区域内的时候会去判断View是否调用了setTranslation,setRotation,setScale这些方法,**如果调用的话会调用`matrix.mapPoints`这个方法将View的初始坐标值和经过动画改变的坐标值进行一个融合计算从而得到最终的View坐标值**，以此值去判断View是否再点击区域内,**而补间动画并没有将矩阵设置给View,那么最终做坐标融合的时候自然不会以融合改变后的坐标去判断View是否在手指点击区域内**，所以View动画不会改变点击区域而属性动画可以。
```
boolean draw(Canvas canvas, ViewGroup parent, long drawingTime) {
......
    Transformation transformToApply = null;
    final Animation a = getAnimation();
    if (a != null) {
        more = applyLegacyAnimation(parent, drawingTime, a, scalingRequired);
        transformToApply = parent.getChildTransformation();
    }
    if (transformToApply != null) {
        if (drawingWithRenderNode) {   //属性动画会设置矩阵
            renderNode.setAnimationMatrix(transformToApply.getMatrix());
        } else {
            canvas.translate(-transX, -transY); //View动画不会设置到矩阵中
            canvas.concat(transformToApply.getMatrix());
            canvas.translate(transX, transY);
        }
    }
}
```
当我们设置的动画播放补间动画的时候，我们所看到的变化，都只是`临时`的。而属性动画呢，它所改变的东西，`却会更新到这个View所对应的矩阵中`，所以当ViewGroup分派事件的时候，会正确的将当前触摸坐标，转换成矩阵变化后的坐标，这就是为什么播放补间动画不会改变触摸区域的原因了。

### 属性动画内存泄漏的原因
```
public AnimationHandler getAnimationHandler() {    return AnimationHandler.getInstance();}

private void addAnimationCallback(long delay) {
    if (!mSelfPulse) {
        return;
    }
    getAnimationHandler().addAnimationFrameCallback(this, delay);this就是ValueAnimator自身
}
```
如上当我们新建ValueAnimator的时候会创建AnimationHandler这个静态类，同时它会持有ValueAnimator，当进入Activity界面后如果有一些和控件绑定在一起的属性动画在运行同时设置成了**`无限循环模式`**，退出的时候要记得**`cancel`**掉这些动画否则会造成内存泄漏。
**`引用链关系：Activity->View->ValueAnimator->AnimationHandler（静态类,GCROOT）`**
```
public void startAnimation(Animation animation) {
        animation.setStartTime(Animation.START_ON_FIRST_FRAME);
        setAnimation(animation);
        invalidateParentCaches();
        invalidate(true);
    }
```

### 具体用法
参考：https://github.com/lvzishen/LVViewList/blob/master/app/src/main/java/com/mystudydemo/animator/AnimatorActivity.java
里边包括各种类型的动画使用，关键帧等的使用实例。
下边给一个属性动画的使用实例
```
ValueAnimator mFirstPhaseAnimator;

if (mFirstPhaseAnimator == null) {
    mFirstPhaseAnimator = ValueAnimator.ofInt(0, 100);
    mFirstPhaseAnimator.setDuration(2000);
    mFirstPhaseAnimator.setInterpolator(new DecelerateInterpolator());
    mFirstPhaseAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            if (!mIsAnimationSetFinished) {
                mCurrentVal = (int) valueAnimator.getAnimatedValue(); 获取当前的值
                if (mProcessor != null) {
                    String text = mProcessor.getText(getContext(), mCurrentVal);
                    if (TextUtils.isEmpty(text)) {
                        setValText(text);
                    }
                } else {
                    setValText(String.valueOf(mCurrentVal));
                }
            }
        }
    });
    mFirstPhaseAnimator.start();//开启动画
}
```