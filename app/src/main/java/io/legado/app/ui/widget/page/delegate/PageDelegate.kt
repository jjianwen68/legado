package io.legado.app.ui.widget.page.delegate

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Scroller
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import io.legado.app.ui.widget.page.PageView
import io.legado.app.utils.screenshot
import kotlinx.android.synthetic.main.view_book_page.view.*
import kotlin.math.abs

abstract class PageDelegate(private val pageView: PageView) {

    //起始点
    protected var startX: Float = 0.toFloat()
    protected var startY: Float = 0.toFloat()
    //触碰点
    protected var touchX: Float = 0.toFloat()
    protected var touchY: Float = 0.toFloat()

    protected val view: View
        get() = pageView.page_panel

    protected var bitmap: Bitmap? = null

    protected var viewWidth: Int = pageView.width
    protected var viewHeight: Int = pageView.height

    protected val scroller: Scroller by lazy { Scroller(pageView.context, FastOutLinearInInterpolator()) }

    private val detector: GestureDetector by lazy { GestureDetector(pageView.context, GestureListener()) }

    private var isMoved = false
    private var noNext = true

    //移动方向
    var direction = Direction.NONE
    var isCancel = false
    var isRunning = false
    var isStarted = false

    protected fun setStartPoint(x: Float, y: Float, invalidate: Boolean = true) {
        startX = x
        startY = y

        if (invalidate) {
            invalidate()
        }
    }

    protected fun setTouchPoint(x: Float, y: Float, invalidate: Boolean = true) {
        touchX = x
        touchY = y

        if (invalidate) {
            invalidate()
        }
    }

    fun setViewSize(width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        invalidate()
    }

    fun invalidate() {
        pageView.invalidate()
    }

    fun start() {
        isRunning = true
        isStarted = true
        invalidate()
    }

    fun stop() {
        isRunning = false
        isStarted = false
        bitmap = null
        invalidate()
    }

    fun scroll() {
        if (scroller.computeScrollOffset()) {
            setTouchPoint(scroller.currX.toFloat(), scroller.currY.toFloat())
        } else if(isStarted){
            setTouchPoint(scroller.finalX.toFloat(), scroller.finalY.toFloat(), false)
            stop()
        }
    }

    fun abort() {
        if (!scroller.isFinished) {
            scroller.abortAnimation()
        }
    }

    fun start(direction: Direction) {
        if (isStarted) return
        this.direction = direction
        if (direction === Direction.NEXT) {
            val x = viewWidth.toFloat()
            val y = viewHeight.toFloat()
            //初始化动画
            setStartPoint(x, y, false)
            //设置点击点
            setTouchPoint(x, y, false)
            //设置方向
            val hasNext = pageView.hasNext()
            if (!hasNext) {
                return
            }
        } else {
            val x = 0.toFloat()
            val y = viewHeight.toFloat()
            //初始化动画
            setStartPoint(x, y, false)
            //设置点击点
            setTouchPoint(x, y, false)
            //设置方向方向
            val hashPrev = pageView.hasPrev()
            if (!hashPrev) {
                return
            }
        }
        onStart()
    }

    fun onTouch(event: MotionEvent): Boolean {
        if (isMoved && event.action == MotionEvent.ACTION_UP) {
            // 开启翻页效果
            if (!noNext) {
                onStart()
            }
            return true
        }
        return detector.onTouchEvent(event)
    }

    fun getDuration(distance: Float): Int {
        val duration = 300 * abs(distance) / viewWidth
        return duration.toInt()
    }

    abstract fun onStart()

    abstract fun onPerform(canvas: Canvas)

    enum class Direction {
        NONE, PREV, NEXT
    }

    private inner class GestureListener : GestureDetector.OnGestureListener {

        override fun onDown(e: MotionEvent): Boolean {
            abort()
            //是否移动
            isMoved = false
            //是否存在下一章
            noNext = false
            //是否正在执行动画
            isRunning = false
            //取消
            isCancel = false
            //是下一章还是前一章
            direction = Direction.NONE
            //设置起始位置的触摸点
            setStartPoint(e.x, e.y)
            return true
        }

        override fun onShowPress(e: MotionEvent) {}

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val x = e.x
            val y = e.y
            direction = if (x > viewWidth / 2) Direction.NEXT else Direction.PREV
            if (direction == Direction.NEXT) {
                //判断是否下一页存在
                val hasNext = pageView.hasNext()
                //设置动画方向
                if (!hasNext) {
                    return true
                }
            } else {
                val hasPrev = pageView.hasPrev()
                if (!hasPrev) {
                    return true
                }
            }
            setTouchPoint(x, y)
            return true
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (!isMoved && abs(distanceX) > abs(distanceY)) {
                //上一页或下一页截图，还未处理
                bitmap = pageView.screenshot()
                if (distanceX < 0) {
                    //上一页的参数配置
                    direction = Direction.PREV
                    //判断是否上一页存在
                    val hasPrev = pageView.hasPrev()
                    //如果上一页不存在
                    if (!hasPrev) {
                        noNext = true
                        return true
                    }
                } else {
                    //进行下一页的配置
                    direction = Direction.NEXT
                    //判断是否下一页存在
                    val hasNext = pageView.hasNext()
                    //如果不存在表示没有下一页了
                    if (!hasNext) {
                        noNext = true
                        return true
                    }
                }
                isMoved = true
            }
            if (isMoved) {
                isCancel = if (direction == Direction.NEXT) distanceX < 0 else distanceX > 0
                isRunning = true
                //设置触摸点
                setTouchPoint(e2.x, e2.y)
            }
            return isMoved
        }

        override fun onLongPress(e: MotionEvent) {

        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            return false
        }
    }

    interface PageInterface {
        fun hasNext(): Boolean
        fun hasPrev(): Boolean
    }
}