package com.lavruk.cityguide.views;

import com.lavruk.cityguide.R;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

public class MultiSwitch extends View {

    private static final int THUMB_ANIMATION_DURATION = 250;

    private static final int TOUCH_MODE_IDLE = 0;
    private static final int TOUCH_MODE_DOWN = 1;
    private static final int TOUCH_MODE_DRAGGING = 2;
    private final int mNormalColor;
    private final int mSelectedColor;

    private Drawable mThumbDrawable;
    private Drawable mTrayDrawable;

    private CharSequence[] mStates;

    private int mTouchMode;
    private int mTouchSlop;
    private float mTouchX;
    private float mTouchY;
    private VelocityTracker mVelocityTracker = VelocityTracker.obtain();
    private int mMinFlingVelocity;

    private float mThumbPosition;
    private int mSwitchWidth;
    private int mSwitchHeight;
    private int mThumbWidth; // Does not include padding

    private int mSwitchLeft;
    private int mSwitchTop;
    private int mSwitchRight;
    private int mSwitchBottom;

    private TextPaint mTextPaint;
    private ColorStateList mTextColors;

    @SuppressWarnings("hiding")
    private final Rect mTempRect = new Rect();

    private Layout[] mStateLayouts;
    private int mSelectedState;
    private boolean mHitThumb;
    private int mDownHitState;
    private OnStateChangedListener mListener;
    private ObjectAnimator mPositionAnimator;

    /**
     * Construct a new Switch with default styling.
     *
     * @param context The Context that will determine this widget's theming.
     */
    public MultiSwitch(Context context) {
        this(context, null);
    }

    /**
     * Construct a new Switch with a default style determined by the given theme attribute,
     * overriding specific style attributes as requested.
     *
     * @param context The Context that will determine this widget's theming.
     * @param attrs   Specification of attributes that should deviate from the default styling.
     *                default style for this widget. e.g. android.R.attr.switchStyle.
     */
    public MultiSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        Resources res = getResources();
        mTextPaint.density = res.getDisplayMetrics().density;

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.MultiSwitch);

        mThumbDrawable = a.getDrawable(R.styleable.MultiSwitch_slider);
        if (mThumbDrawable == null) {
            mThumbDrawable = getResources().getDrawable(R.drawable.slider);
        }
        mTrayDrawable = a.getDrawable(R.styleable.MultiSwitch_tray);
        if (mTrayDrawable == null) {
            mTrayDrawable = getResources().getDrawable(R.drawable.tray);
        }

        mStates = a.getTextArray(R.styleable.MultiSwitch_states);

        mNormalColor = a.getColor(R.styleable.MultiSwitch_normalColor, Color.BLACK);
        mSelectedColor = a.getColor(R.styleable.MultiSwitch_selectedColor, Color.BLACK);

        mTextPaint.setTextSize(
                a.getDimensionPixelSize(R.styleable.MultiSwitch_textSize, (int) (16 * mTextPaint.density)));

        ViewConfiguration config = ViewConfiguration.get(context);
        mTouchSlop = config.getScaledTouchSlop();
        mMinFlingVelocity = config.getScaledMinimumFlingVelocity();

        // Refresh display with current params
        refreshDrawableState();
    }

    /**
     * Sets the text displayed when the button is not in the checked state.
     */
    public void setStates(CharSequence[] states) {
        mStates = states;
        requestLayout();
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
//
        if (mStates != null) {
            mStateLayouts = new Layout[mStates.length];
            for (int i = 0; i < mStates.length; i++) {
                mStateLayouts[i] = makeLayout(mStates[i]);
            }
        }

        final Rect padding = mTempRect;
        final int thumbHeight;
        if (mThumbDrawable != null) {
            // Cached thumb width does not include padding.
            mThumbDrawable.getPadding(padding);
            thumbHeight = mThumbDrawable.getIntrinsicHeight();
        } else {
            thumbHeight = 0;
        }

        final int trackHeight;
        if (mTrayDrawable != null) {
            mTrayDrawable.getPadding(padding);
            trackHeight = mTrayDrawable.getIntrinsicHeight();
        } else {
            padding.setEmpty();
            trackHeight = 0;
        }

        final int switchWidth = widthSize - getPaddingLeft() - getPaddingRight();
        final int switchHeight = Math.max(trackHeight, thumbHeight);
        mSwitchWidth = switchWidth;
        mSwitchHeight = switchHeight;

        mThumbWidth = switchWidth / mStateLayouts.length;

        setMeasuredDimension(switchWidth + getPaddingLeft() + getPaddingRight(),
                switchHeight + getPaddingTop() + getPaddingBottom());
    }

    private int maxStateTextWidth() {
        int maxWidth = 0;
        if (mStateLayouts == null) {
            return maxWidth;
        }

        for (Layout stateLayout : mStateLayouts) {
            maxWidth = Math.max(stateLayout.getWidth(), maxWidth);
        }

        return maxWidth;
    }

    private Layout makeLayout(CharSequence text) {
        return new StaticLayout(text, mTextPaint,
                (int) Math.ceil(Layout.getDesiredWidth(text, mTextPaint)),
                Layout.Alignment.ALIGN_NORMAL, 1.f, 0, true);
    }

    /**
     * @return true if (x, y) is within the target area of the switch thumb
     */
    private boolean hitThumb(float x, float y) {
        mThumbDrawable.getPadding(mTempRect);
        final int thumbTop = mSwitchTop - mTouchSlop;
        final int thumbLeft = mSwitchLeft + (int) (mThumbPosition + 0.5f) - mTouchSlop;
        final int thumbRight = thumbLeft + mThumbWidth +
                mTempRect.left + mTempRect.right + mTouchSlop;
        final int thumbBottom = mSwitchBottom + mTouchSlop;
        return x > thumbLeft && x < thumbRight && y > thumbTop && y < thumbBottom;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mVelocityTracker.addMovement(ev);
        final int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                mHitThumb = hitThumb(x, y);
                mDownHitState = getHitState(ev.getX());
                if (isEnabled()) {
                    mTouchMode = TOUCH_MODE_DOWN;
                    mTouchX = x;
                    mTouchY = y;
                }
                return true;
            }

            case MotionEvent.ACTION_MOVE: {
                switch (mTouchMode) {
                    case TOUCH_MODE_IDLE:
                        // Didn't target the thumb, treat normally.
                        break;

                    case TOUCH_MODE_DOWN: {
                        if (!mHitThumb) {
                            break;
                        }
                        final float x = ev.getX();
                        final float y = ev.getY();
                        if (Math.abs(x - mTouchX) > mTouchSlop ||
                                Math.abs(y - mTouchY) > mTouchSlop) {
                            mTouchMode = TOUCH_MODE_DRAGGING;
                            getParent().requestDisallowInterceptTouchEvent(true);
                            mTouchX = x;
                            mTouchY = y;
                            return true;
                        }
                        break;
                    }

                    case TOUCH_MODE_DRAGGING: {
                        final float x = ev.getX();
                        final float dx = x - mTouchX;
                        float newPos = Math.max(0,
                                Math.min(mThumbPosition + dx, getThumbScrollRange()));
                        if (newPos != mThumbPosition) {
                            mThumbPosition = newPos;
                            mTouchX = x;
                            invalidate();
                        }
                        return true;
                    }
                }
                break;
            }

            case MotionEvent.ACTION_UP:
                if (mTouchMode == TOUCH_MODE_DOWN) {
                    int newState = getHitState(ev.getX());

                    // same state, handle click
                    if (mDownHitState == newState) {
                        animateThumbToCheckedState(newState);
                        mSelectedState = newState;
                        if (mListener != null) {
                            mListener.onStateChanged(newState);
                        }

                        cancelSuperTouch(ev);
                        mTouchMode = TOUCH_MODE_IDLE;
                        mVelocityTracker.clear();
                        return true;
                    }
                }
            case MotionEvent.ACTION_CANCEL: {
                if (mTouchMode == TOUCH_MODE_DRAGGING) {
                    stopDrag(ev);
                    return true;
                }
                mTouchMode = TOUCH_MODE_IDLE;
                mVelocityTracker.clear();
                break;
            }
        }

        return super.onTouchEvent(ev);
    }

    private void cancelSuperTouch(MotionEvent ev) {
        MotionEvent cancel = MotionEvent.obtain(ev);
        cancel.setAction(MotionEvent.ACTION_CANCEL);
        super.onTouchEvent(cancel);
        cancel.recycle();
    }

    /**
     * Called from onTouchEvent to end a drag operation.
     *
     * @param ev Event that triggered the end of drag mode - ACTION_UP or ACTION_CANCEL
     */
    private void stopDrag(MotionEvent ev) {
        mTouchMode = TOUCH_MODE_IDLE;
        // Up and not canceled, also checks the switch has not been disabled during the drag
        boolean commitChange = ev.getAction() == MotionEvent.ACTION_UP && isEnabled();

        cancelSuperTouch(ev);

        if (commitChange) {
            int newState;
            mVelocityTracker.computeCurrentVelocity(1000);
            float xvel = mVelocityTracker.getXVelocity();
            if (Math.abs(xvel) > mMinFlingVelocity * 10) {
                if (xvel > 0) {
                    newState = Math.min(mSelectedState + 1, mStates.length - 1);
                } else {
                    newState = Math.max(mSelectedState - 1, 0);
                }
            } else {
                newState = getTargetStateByThumbPosition();
            }
            animateThumbToCheckedState(newState);
            mSelectedState = newState;
            if (mListener != null) {
                mListener.onStateChanged(newState);
            }
        } else {
            animateThumbToCheckedState(getSelectedState());
        }
    }

    public int getSelectedState() {
        return mSelectedState;
    }

    private void animateThumbToCheckedState(int newState) {
        float targetPos = newState * mThumbWidth;
        mPositionAnimator = ObjectAnimator.ofFloat(this, "thumbPosition", targetPos);
        mPositionAnimator.setDuration(THUMB_ANIMATION_DURATION);
        mPositionAnimator.start();

// TODO without animation
//        mThumbPosition = targetPos;
//        invalidate();
    }

    private void cancelPositionAnimator() {
        if (mPositionAnimator != null) {
            mPositionAnimator.cancel();
        }
    }

    public void setThumbPosition(float position) {
        mThumbPosition = position;
        invalidate();
    }

    public float getThumbPosition() {
        return mThumbPosition;
    }

    private int getTargetStateByThumbPosition() {
        float thumbCenter = mThumbPosition + mThumbWidth / 2;
        for (int i = 0; i < mStates.length; i++) {
            if (thumbCenter > i * mThumbWidth && thumbCenter <= (i + 1) * mThumbWidth) {
                return i;
            }
        }
        return 0;
    }

    private int getHitState(float x) {
        for (int i = 0; i < mStates.length; i++) {
            if (x > i * mThumbWidth && x <= (i + 1) * mThumbWidth) {
                return i;
            }
        }
        return 0;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        mThumbPosition = mSelectedState * mThumbWidth;

        final int switchLeft = getPaddingLeft();
        final int switchRight = switchLeft + mSwitchWidth;

        final int switchTop;
        final int switchBottom;
        switch (getGravity() & Gravity.VERTICAL_GRAVITY_MASK) {
            default:
            case Gravity.TOP:
                switchTop = getPaddingTop();
                switchBottom = switchTop + mSwitchHeight;
                break;

            case Gravity.CENTER_VERTICAL:
                switchTop = (getPaddingTop() + getHeight() - getPaddingBottom()) / 2 -
                        mSwitchHeight / 2;
                switchBottom = switchTop + mSwitchHeight;
                break;

            case Gravity.BOTTOM:
                switchBottom = getHeight() - getPaddingBottom();
                switchTop = switchBottom - mSwitchHeight;
                break;
        }

        mSwitchLeft = switchLeft;
        mSwitchTop = switchTop;
        mSwitchBottom = switchBottom;
        mSwitchRight = switchRight;
    }

    private int getGravity() {
        return Gravity.TOP;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the switch
        int switchLeft = mSwitchLeft;
        int switchTop = mSwitchTop;
        int switchRight = mSwitchRight;
        int switchBottom = mSwitchBottom;

        mTrayDrawable.setBounds(switchLeft, switchTop, switchRight, switchBottom);
        mTrayDrawable.draw(canvas);

        canvas.save();

        mTrayDrawable.getPadding(mTempRect);
        int switchInnerLeft = switchLeft + mTempRect.left;
        int switchInnerTop = switchTop + mTempRect.top;
        int switchInnerRight = switchRight - mTempRect.right;
        int switchInnerBottom = switchBottom - mTempRect.bottom;
        canvas.clipRect(switchInnerLeft, switchTop, switchInnerRight, switchBottom);

        mThumbDrawable.getPadding(mTempRect);
        final int thumbPos = (int) (mThumbPosition + 0.5f);
        int thumbLeft = switchInnerLeft - mTempRect.left + thumbPos;
        int thumbRight = switchInnerLeft + thumbPos + mThumbWidth + mTempRect.right;

        mThumbDrawable.setBounds(thumbLeft, switchTop, thumbRight, switchBottom);
        mThumbDrawable.draw(canvas);

        // mTextColors should not be null, but just in case
        if (mTextColors != null) {
            mTextPaint.setColor(mTextColors.getColorForState(getDrawableState(),
                    mTextColors.getDefaultColor()));
        }
        mTextPaint.drawableState = getDrawableState();

        if (mStateLayouts != null) {

            for (int i = mStateLayouts.length - 1; i >= 0; i--) {

                float left = i * mThumbWidth + getPaddingLeft();
                Layout stateText = mStateLayouts[i];

                float textLeft = left + mThumbWidth / 2 - stateText.getWidth() / 2;
                canvas.translate(textLeft,
                        (switchInnerTop + switchInnerBottom) / 2 - stateText.getHeight() / 2);

                if ((textLeft < (mThumbPosition + mThumbWidth) && textLeft > mThumbPosition)
                        || ((textLeft + stateText.getWidth()) > mThumbPosition) && ((textLeft + stateText.getWidth())
                        < (mThumbPosition + mThumbWidth))) {
                    mTextPaint.setColor(mSelectedColor);
                } else {
                    mTextPaint.setColor(mNormalColor);
                }
                stateText.draw(canvas);

                canvas.restore();
            }
        }
    }

    private int getThumbScrollRange() {
        if (mTrayDrawable == null) {
            return 0;
        }
        mTrayDrawable.getPadding(mTempRect);
        return mSwitchWidth - mThumbWidth - mTempRect.left - mTempRect.right;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        int[] myDrawableState = getDrawableState();

        // Set the state of the Drawable
        // Drawable may be null when checked state is set from XML, from super constructor
        if (mThumbDrawable != null) {
            mThumbDrawable.setState(myDrawableState);
        }
        if (mTrayDrawable != null) {
            mTrayDrawable.setState(myDrawableState);
        }

        invalidate();
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == mThumbDrawable || who == mTrayDrawable;
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        mThumbDrawable.jumpToCurrentState();
        mTrayDrawable.jumpToCurrentState();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);

        ss.state = getSelectedState();
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;

        super.onRestoreInstanceState(ss.getSuperState());
        mSelectedState = ss.state;
        requestLayout();
    }

    public void setOnStateChangedListener(OnStateChangedListener listener) {
        mListener = listener;
    }

    public interface OnStateChangedListener {

        void onStateChanged(int state);
    }

    static class SavedState extends BaseSavedState {

        int state;

        SavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            state = (Integer) in.readValue(null);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeValue(state);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
