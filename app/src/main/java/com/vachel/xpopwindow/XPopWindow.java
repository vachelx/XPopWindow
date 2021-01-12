package com.vachel.xpopwindow;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vachel.xpopwindow.util.Utils;

/**
 * 用法参照      XPopWindow.build(MainActivity.this, view)
 *                         .bindRecyclerView(recycleView)
 *                         .bindLifeCycle(lifecycleOwner)
 *                         .setItems(items)
 *                         .setIcons(icons)
 *                         .setDividerVerticalEnable(true)
 *                         .setDividerHorizontalEnable(false)
 *                         .setListener(MainActivity.this)
 *                         .show();
 */
public class XPopWindow extends RecyclerView.OnScrollListener implements LifecycleObserver, PopupWindow.OnDismissListener {
    private static final float DEFAULT_TEXT_SIZE_DP = 14;
    private static final float DEFAULT_PADDING_DP = 5.0f;
    private static final int DEFAULT_BACKGROUND_RADIUS_DP = 6;
    private static final int DEFAULT_SPAN_COUNT = 5;

    private static final int DEFAULT_MARGIN_HORIZONTAL_DP = 16;
    private static final int DEFAULT_INDICATOR_WIDTH_DP = 14;
    private static final int DEFAULT_INDICATOR_HEIGHT_DP = 7;

    private static boolean mHasShow = false; // 唯一标识，只能同时显示一个
    private Context mContext;
    private PopupWindow mPopupWindow;
    private View mAnchorView;
    private View mIndicatorView;
    private String[] mPopupLabels;
    private int[] mPopupIcons;
    private IXPopupListener mIXPopupListener;
    private ColorStateList mTextColorStateList;
    private GradientDrawable mCornerBackground;
    // 默认左右margin最小值
    private int mMarginHorizontal;
    //指示器属性
    private int mIndicatorWidth;
    private int mIndicatorHeight;
    //PopupWindow属性
    private int mPopupWindowWidth;
    private int mPopupWindowHeight;
    //文本属性
    private int mNormalTextColor;
    private int mPressedTextColor;
    private float mTextSize;
    private int mTextPaddingLeft;
    private int mTextPaddingTop;
    private int mTextPaddingRight;
    private int mTextPaddingBottom;
    private int mNormalBackgroundColor;
    private int mPressedBackgroundColor;
    private int mBackgroundCornerRadius;
    //是否显示在下方
    private boolean mIsShowBottom;
    //倒转高度,当落下位置比这个值小时，气泡显示在下方; 默认为状态栏高度
    private int mReversalHeight;
    private boolean mDividerHorizontalEnable;
    private boolean mDividerVerticalEnable;
    private RecyclerView mBindRecyclerView;
    private int mScrollState = -1;
    private Lifecycle mLifecycle;

    // anchorView决定了显示位置； 显示箭头会对齐anchorView中点
    public static XPopWindow build(Context context, View anchorView) {
        XPopWindow popupView = new XPopWindow(context);
        popupView.mAnchorView = anchorView;
        return popupView;
    }

    /**
     * 绑定scrollView后， 滑动过程中可以重定位弹窗位置
     */
    public XPopWindow bindRecyclerView(RecyclerView recyclerView) {
        mBindRecyclerView = recyclerView;
        return this;
    }

    // 弹窗的item标签 必须设置
    public XPopWindow setItems(String[] labels) {
        mPopupLabels = labels;
        return this;
    }

    // 各个标签对应的图标；可以不设置； 设置和Items个数不对应也不展示
    public XPopWindow setIcons(int[] icons) {
        mPopupIcons = icons;
        return this;
    }

    // 顶部默认不可用距离， 默认为状态栏高度
    public XPopWindow setReversalHeight(int reversalHeight) {
        mReversalHeight = reversalHeight;
        return this;
    }

    public XPopWindow setHorizontalMarginMin(int marginMin) {
        mMarginHorizontal = marginMin;
        return this;
    }

    public XPopWindow setListener(IXPopupListener listener) {
        mIXPopupListener = listener;
        return this;
    }

    public XPopWindow bindLifeCycle(LifecycleOwner lifecycleOwner) {
        // 生命周期绑定不在这里而是show时才监听
        mLifecycle = lifecycleOwner.getLifecycle();
        return this;
    }

    private XPopWindow(Context context) {
        mContext = context;
    }

    public boolean isShowing() {
        return mPopupWindow != null && mPopupWindow.isShowing();
    }

    public void dismiss() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
            mPopupWindow = null;
        }
    }

    private void initParameters() {
        if (mReversalHeight == 0) {
            mReversalHeight = Utils.getStatusBarHeight(mContext);
        }
        if (mTextSize == 0) {
            mTextSize = dp2px(DEFAULT_TEXT_SIZE_DP);
        }
        if (mTextPaddingLeft == 0) {
            mTextPaddingLeft = dp2px(DEFAULT_PADDING_DP);
        }
        if (mTextPaddingRight == 0) {
            mTextPaddingRight = dp2px(DEFAULT_PADDING_DP);
        }
        if (mNormalTextColor == 0) {
            mNormalTextColor = mContext.getResources().getColor(R.color.white);
        }
        if (mPressedTextColor == 0) {
            mPressedTextColor = mContext.getResources().getColor(R.color.popup_text_color);
        }
        if (mNormalBackgroundColor == 0) {
            mNormalBackgroundColor = mContext.getResources().getColor(R.color.popup_bg);
        }
        if (mPressedBackgroundColor == 0) {
            mPressedBackgroundColor = mContext.getResources().getColor(R.color.popup_bg_pressed);
        }
        if (mBackgroundCornerRadius == 0) {
            mBackgroundCornerRadius = dp2px(DEFAULT_BACKGROUND_RADIUS_DP);
        }
        if (mMarginHorizontal == 0) {
            mMarginHorizontal = dp2px(DEFAULT_MARGIN_HORIZONTAL_DP);
        }
        if (mIndicatorWidth == 0) {
            mIndicatorWidth = dp2px(DEFAULT_INDICATOR_WIDTH_DP);
        }
        if (mIndicatorHeight == 0) {
            mIndicatorHeight = dp2px(DEFAULT_INDICATOR_HEIGHT_DP);
        }
        refreshBackgroundOrRadiusStateList();
        refreshTextColorStateList(mPressedTextColor, mNormalTextColor);
        mIndicatorView = getTriangleIndicatorView(mIndicatorWidth, mIndicatorHeight, mNormalBackgroundColor);
    }

    /**
     * 创建布局和显示
     */
    public void show() {
        if (mContext instanceof Activity && ((Activity) mContext).isFinishing() || mHasShow) {
            return;
        }
        if (mScrollState == 1 && !mAnchorView.isShown()) {
            return;
        }
        initParameters();
        int[] location = new int[2];
        mAnchorView.getLocationOnScreen(location);
        if (mPopupWindow == null) {
            createPopupWindow(location);
        }
        if (mPopupWindow.isShowing()) {
            return;
        }
        int offsetX = mAnchorView.getWidth() / 2;
        int[] showLoc = new int[2];
        showLoc[0] = (int) (location[0] + offsetX - mPopupWindowWidth / 2f + 0.5f);
        int marginOffsetX = adjustMarginHorizontal(showLoc);
        showLoc[1] = mIsShowBottom ? (int) (location[1] + mAnchorView.getHeight() + 0.5f) : (int) (location[1] - mPopupWindowHeight + 0.5f);
        translateIndicator(location[0], marginOffsetX);
        mPopupWindow.showAtLocation(mAnchorView, Gravity.NO_GRAVITY, showLoc[0], showLoc[1]);
        mHasShow = true;
        if (mLifecycle != null) {
            mLifecycle.addObserver(this);
        }
        // mScrollState==1时是还在滚动中途调用的show，这时候不需要重置
        if (mBindRecyclerView != null && mScrollState != 1) {
            mBindRecyclerView.removeOnScrollListener(XPopWindow.this);
            mScrollState = -1;
            mBindRecyclerView.addOnScrollListener(this);
        }
        mPopupWindow.setOnDismissListener(this);
    }

    /**
     * 调整横向与屏幕至少有mMarginHorizontal的边距
     *
     * @param showLoc
     * @return 因为margin产生的offset
     */
    private int adjustMarginHorizontal(int[] showLoc) {
        int marginOffsetX = 0;
        if (showLoc[0] < mMarginHorizontal) {
            showLoc[0] = mMarginHorizontal;
            marginOffsetX = -mMarginHorizontal;
        }
        int screenWidth = Utils.getScreenWidth(mContext);
        if (showLoc[0] + mPopupWindowWidth > screenWidth - mMarginHorizontal) {
            showLoc[0] = screenWidth - mMarginHorizontal - mPopupWindowWidth;
            marginOffsetX = mMarginHorizontal;
        }
        return marginOffsetX;
    }

    private void translateIndicator(int viewX, int marginOffsetX) {
        int offsetX = mAnchorView.getWidth() / 2;
        float leftTranslationLimit = mIndicatorWidth / 2f + mBackgroundCornerRadius - mPopupWindowWidth / 2f;
        float rightTranslationLimit = mPopupWindowWidth / 2f - mIndicatorWidth / 2f - mBackgroundCornerRadius;
        //获取最大绝对宽度，单位是px
        float maxWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        //通过setTranslationX改变view的位置，是不改变view的LayoutParams的，也即不改变getLeft等view的信息
        if (viewX + offsetX < mPopupWindowWidth / 2f) {
            mIndicatorView.setTranslationX(Math.max(viewX + offsetX - mPopupWindowWidth / 2f + marginOffsetX, leftTranslationLimit));
        } else if (viewX + offsetX + mPopupWindowWidth / 2f > maxWidth) {
            mIndicatorView.setTranslationX(Math.min(viewX + offsetX + mPopupWindowWidth / 2f - maxWidth + marginOffsetX, rightTranslationLimit));
        } else {
            mIndicatorView.setTranslationX(0);
        }
    }

    private void createPopupWindow(int[] location) {
        //创建根布局
        LinearLayout contentView = new LinearLayout(mContext);
        contentView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        contentView.setOrientation(LinearLayout.VERTICAL);
        //创建list布局
        LinearLayout popupListContainer = new LinearLayout(mContext);
        popupListContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        popupListContainer.setOrientation(LinearLayout.HORIZONTAL);
        popupListContainer.setBackgroundDrawable(mCornerBackground);

        RecyclerView recyclerView = new RecyclerView(mContext);
        recyclerView.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT));
        recyclerView.setPadding(mTextPaddingLeft, mTextPaddingTop, mTextPaddingRight, mTextPaddingBottom);
        int spanCount = Math.min(mPopupLabels.length, DEFAULT_SPAN_COUNT);
        GridLayoutManager layoutManager = new GridLayoutManager(mContext, spanCount);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        CxPopupWindowAdapter mAdapter = new CxPopupWindowAdapter(mContext, mPopupLabels, mPopupIcons);
        mAdapter.setItemClickListener(mIXPopupListener);
        if (mDividerVerticalEnable) {
            BubblePopupDivider divider = new BubblePopupDivider(mContext, DividerItemDecoration.VERTICAL, spanCount);
            divider.setDrawable(ContextCompat.getDrawable(mContext, R.drawable.popup_divider_line_horizontal));
            recyclerView.addItemDecoration(divider);
        }
        if (mDividerHorizontalEnable) {
            BubblePopupDivider divider = new BubblePopupDivider(mContext, DividerItemDecoration.HORIZONTAL, spanCount);
            divider.setDrawable(ContextCompat.getDrawable(mContext, R.drawable.popup_divider_line_vertical));
            recyclerView.addItemDecoration(divider);
        }
        recyclerView.setAdapter(mAdapter);
        //创建指示器
        LinearLayout.LayoutParams layoutParams;
        if (mIndicatorView.getLayoutParams() == null) {
            layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        } else {
            layoutParams = (LinearLayout.LayoutParams) mIndicatorView.getLayoutParams();
        }
        layoutParams.gravity = Gravity.CENTER;
        mIndicatorView.setLayoutParams(layoutParams);
        ViewParent viewParent = mIndicatorView.getParent();
        if (viewParent instanceof ViewGroup) {
            ((ViewGroup) viewParent).removeView(mIndicatorView);
        }
        contentView.addView(popupListContainer);
        popupListContainer.addView(recyclerView);
        // 根据显示位置在屏幕位置确定window显示在mAnchorView的上方还是下方
        int mPopupHeight = getViewHeight(popupListContainer) + mIndicatorHeight;
        mIsShowBottom = location[1] - mReversalHeight - mPopupHeight < dp2px(2);
        if (!mIsShowBottom) {
            contentView.addView(mIndicatorView);
        } else {
            contentView.addView(mIndicatorView, 0);
        }

        if (mPopupWindowWidth == 0) {
            mPopupWindowWidth = getViewWidth(popupListContainer);
        }
        if (mPopupWindowHeight == 0) {
            mPopupWindowHeight = getViewHeight(popupListContainer) + mIndicatorHeight;
        }
        mPopupWindow = new PopupWindow(contentView, mPopupWindowWidth, mPopupWindowHeight, true);
        mPopupWindow.setTouchable(true);
        mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
    }

    /**
     * 刷新背景或附加状态列表
     */
    private void refreshBackgroundOrRadiusStateList() {
        GradientDrawable cornerItemPressedDrawable = new GradientDrawable();
        cornerItemPressedDrawable.setColor(mPressedBackgroundColor);
        cornerItemPressedDrawable.setCornerRadius(mBackgroundCornerRadius);
        GradientDrawable cornerItemNormalDrawable = new GradientDrawable();
        cornerItemNormalDrawable.setColor(Color.TRANSPARENT);
        cornerItemNormalDrawable.setCornerRadius(mBackgroundCornerRadius);
        StateListDrawable mCornerItemBackground = new StateListDrawable();
        mCornerItemBackground.addState(new int[]{android.R.attr.state_pressed}, cornerItemPressedDrawable);
        mCornerItemBackground.addState(new int[]{}, cornerItemNormalDrawable);
        mCornerBackground = new GradientDrawable();
        mCornerBackground.setColor(mNormalBackgroundColor);
        mCornerBackground.setCornerRadius(mBackgroundCornerRadius);
    }

    /**
     * 获取中心item背景
     */
    private StateListDrawable getCenterItemBackground() {
        StateListDrawable centerItemBackground = new StateListDrawable();
        GradientDrawable centerItemPressedDrawable = new GradientDrawable();
        centerItemPressedDrawable.setColor(mPressedBackgroundColor);
        GradientDrawable centerItemNormalDrawable = new GradientDrawable();
        centerItemNormalDrawable.setColor(Color.TRANSPARENT);
        centerItemBackground.addState(new int[]{android.R.attr.state_pressed}, centerItemPressedDrawable);
        centerItemBackground.addState(new int[]{}, centerItemNormalDrawable);
        return centerItemBackground;
    }

    /**
     * 刷新文本颜色状态列表
     *
     * @param pressedTextColor 按下文本颜色
     * @param normalTextColor  正常状态下文本颜色
     */
    private void refreshTextColorStateList(int pressedTextColor, int normalTextColor) {
        int[][] states = new int[2][];
        states[0] = new int[]{android.R.attr.state_pressed};
        states[1] = new int[]{};
        int[] colors = new int[]{pressedTextColor, normalTextColor};
        mTextColorStateList = new ColorStateList(states, colors);
    }

    public void hidePopupListWindow() {
        if (mContext instanceof Activity && ((Activity) mContext).isFinishing()) {
            return;
        }
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
    }

    private View getTriangleIndicatorView(final float widthPixel, final float heightPixel,
                                          final int color) {
        ImageView indicator = new ImageView(mContext);
        Drawable drawable = new Drawable() {
            @Override
            public void draw(Canvas canvas) {
                Path path = new Path();
                Paint paint = new Paint();
                paint.setColor(color);
                paint.setStyle(Paint.Style.FILL);
                if (!mIsShowBottom) {
                    //这里画的倒三角
                    path.moveTo(0f, 0f);
                    path.lineTo(widthPixel, 0f);
                    path.lineTo(widthPixel / 2, heightPixel);
                } else {
                    //正三角
                    path.moveTo(0f, heightPixel);
                    path.lineTo(widthPixel, heightPixel);
                    path.lineTo(widthPixel / 2, 0);
                }
                path.close();
                canvas.drawPath(path, paint);
            }

            @Override
            public void setAlpha(int alpha) {

            }

            @Override
            public void setColorFilter(ColorFilter colorFilter) {

            }

            @Override
            public int getOpacity() {
                return PixelFormat.TRANSLUCENT;
            }

            @Override
            public int getIntrinsicWidth() {
                return (int) widthPixel;
            }

            @Override
            public int getIntrinsicHeight() {
                return (int) heightPixel;
            }
        };
        indicator.setImageDrawable(drawable);
        return indicator;
    }

    public XPopWindow setIndicatorSize(int widthPixel, int heightPixel) {
        mIndicatorWidth = widthPixel;
        mIndicatorHeight = heightPixel;
        return this;
    }

    public int getNormalTextColor() {
        return mNormalTextColor;
    }

    public XPopWindow setNormalTextColor(int normalTextColor) {
        mNormalTextColor = normalTextColor;
        refreshTextColorStateList(mPressedTextColor, mNormalTextColor);
        return this;
    }

    public int getPressedTextColor() {
        return mPressedTextColor;
    }

    public XPopWindow setPressedTextColor(int pressedTextColor) {
        mPressedTextColor = pressedTextColor;
        return this;
    }

    public float getTextSize() {
        return mTextSize;
    }

    public XPopWindow setTextSize(float textSizePixel) {
        mTextSize = textSizePixel;
        return this;
    }

    public int getTextPaddingLeft() {
        return mTextPaddingLeft;
    }

    public XPopWindow setTextPaddingLeft(int textPaddingLeft) {
        mTextPaddingLeft = textPaddingLeft;
        return this;
    }

    public int getTextPaddingTop() {
        return mTextPaddingTop;
    }

    public XPopWindow setTextPaddingTop(int textPaddingTop) {
        mTextPaddingTop = textPaddingTop;
        return this;
    }

    public int getTextPaddingRight() {
        return mTextPaddingRight;
    }

    public XPopWindow setTextPaddingRight(int textPaddingRight) {
        mTextPaddingRight = textPaddingRight;
        return this;
    }

    public int getTextPaddingBottom() {
        return mTextPaddingBottom;
    }

    public XPopWindow setTextPaddingBottom(int textPaddingBottom) {
        mTextPaddingBottom = textPaddingBottom;
        return this;
    }

    public XPopWindow setTextPadding(int left, int top, int right, int bottom) {
        this.mTextPaddingLeft = left;
        this.mTextPaddingTop = top;
        this.mTextPaddingRight = right;
        this.mTextPaddingBottom = bottom;
        return this;
    }

    public int getNormalBackgroundColor() {
        return mNormalBackgroundColor;
    }

    public XPopWindow setNormalBackgroundColor(int normalBackgroundColor) {
        mNormalBackgroundColor = normalBackgroundColor;
        return this;
    }

    public int getPressedBackgroundColor() {
        return mPressedBackgroundColor;
    }

    public XPopWindow setPressedBackgroundColor(int pressedBackgroundColor) {
        mPressedBackgroundColor = pressedBackgroundColor;
        return this;
    }

    public int getBackgroundCornerRadius() {
        return mBackgroundCornerRadius;
    }

    public XPopWindow setBackgroundCornerRadius(int backgroundCornerRadiusPixel) {
        mBackgroundCornerRadius = backgroundCornerRadiusPixel;
        return this;
    }

    // 是否展示横向滚动方向的分割线 （分割线是竖直的）
    public XPopWindow setDividerHorizontalEnable(boolean enable) {
        mDividerHorizontalEnable = enable;
        return this;
    }

    // 是否展示竖直方向的分割线 （分割线是横向的）
    public XPopWindow setDividerVerticalEnable(boolean enable) {
        mDividerVerticalEnable = enable;
        return this;
    }

    public Resources getResources() {
        if (mContext == null) {
            return Resources.getSystem();
        } else {
            return mContext.getResources();
        }
    }

    private int getViewWidth(View view) {
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        return view.getMeasuredWidth();
    }

    private int getViewHeight(View view) {
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        return view.getMeasuredHeight();
    }

    private int dp2px(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                value, getResources().getDisplayMetrics());
    }

    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
        if (newState == 1) {
            mScrollState = newState;
        } else if (newState == 0) {
            recyclerView.removeCallbacks(mDelayRunnable);
            mBindRecyclerView.removeOnScrollListener(XPopWindow.this);
            mScrollState = -1;
            if (!isShowing()) {
                show();
            }
        } else {
            recyclerView.removeCallbacks(mDelayRunnable);
            mBindRecyclerView.removeOnScrollListener(XPopWindow.this);
            mScrollState = -1;
        }
    }

    private Runnable mDelayRunnable = new Runnable() {
        @Override
        public void run() {
            show();
        }
    };

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        if (Math.abs(dy) > 2 && mScrollState == 1) {
            dismiss();
            recyclerView.removeCallbacks(mDelayRunnable);
            recyclerView.postDelayed(mDelayRunnable, 400);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        dismiss();
        mHasShow = false;
    }

    @Override
    public void onDismiss() {
        mHasShow = false;
        if (mScrollState == -1 && mBindRecyclerView != null) {
            mBindRecyclerView.removeOnScrollListener(this);
        }
        if (mLifecycle != null) {
            mLifecycle.removeObserver(this);
        }
    }

    public class CxPopupWindowAdapter extends RecyclerView.Adapter<CxPopupWindowAdapter.CxPopupWindowViewHolder> {
        private final Context mContext;
        private final String[] mLabels;
        private int[] mIcons;
        private IXPopupListener mItemClickListener;

        //设置点击事件的方法
        public void setItemClickListener(IXPopupListener itemClickListener) {
            this.mItemClickListener = itemClickListener;
        }

        public CxPopupWindowAdapter(Context context, String[] labels, int[] icons) {
            this.mContext = context;
            this.mLabels = labels;
            if (icons != null && icons.length != labels.length) {
                icons = null;
            }
            mIcons = icons;
        }

        @NonNull
        @Override
        public CxPopupWindowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new CxPopupWindowViewHolder(LayoutInflater.from(mContext).inflate(R.layout.popup_window_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull CxPopupWindowViewHolder holder, final int position) {
            holder.tv.setText(mLabels[position]);
            if (mIcons == null) {
                holder.image.setVisibility(View.GONE);
            } else {
                holder.image.setVisibility(View.VISIBLE);
                holder.image.setBackgroundResource(mIcons[position]);
            }
            holder.itemView.setBackgroundDrawable(getCenterItemBackground());
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mItemClickListener != null) {
                        mItemClickListener.onPopupListClick(v, mLabels[position]);
                        hidePopupListWindow();
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mLabels.length;
        }

        class CxPopupWindowViewHolder extends RecyclerView.ViewHolder {
            TextView tv;
            ImageView image;

            public CxPopupWindowViewHolder(@NonNull View itemView) {
                super(itemView);
                tv = itemView.findViewById(R.id.text);
                tv.setTextColor(mTextColorStateList);
                image = itemView.findViewById(R.id.image);
            }
        }
    }

    /**
     * 回调监听器
     */
    public interface IXPopupListener {
        void onPopupListClick(View contextView, String label);
    }

    /**
     * 分割线不展示横竖向的最后一条
     */
    static class BubblePopupDivider extends RecyclerView.ItemDecoration {
        static final int HORIZONTAL = LinearLayout.HORIZONTAL;
        static final int VERTICAL = LinearLayout.VERTICAL;
        private static final String TAG = "DividerItem";
        private static final int[] ATTRS = new int[]{android.R.attr.listDivider};
        private Drawable mDivider;
        private int mOrientation;
        private final Rect mBounds = new Rect();
        private int mSpanCount = -1;

        BubblePopupDivider(Context context, int orientation, int spanCount) {
            this(context, orientation);
            mSpanCount = spanCount;
        }

        BubblePopupDivider(Context context, int orientation) {
            final TypedArray a = context.obtainStyledAttributes(ATTRS);
            mDivider = a.getDrawable(0);
            if (mDivider == null) {
                Log.w(TAG, "@android:attr/listDivider was not set in the theme used for this "
                        + "DividerItemDecoration. Please set that attribute all call setDrawable()");
            }
            a.recycle();
            setOrientation(orientation);
        }

        public void setOrientation(int orientation) {
            if (orientation != HORIZONTAL && orientation != VERTICAL) {
                throw new IllegalArgumentException(
                        "Invalid orientation. It should be either HORIZONTAL or VERTICAL");
            }
            mOrientation = orientation;
        }

        public void setDrawable(@NonNull Drawable drawable) {
            if (drawable == null) {
                throw new IllegalArgumentException("Drawable cannot be null.");
            }
            mDivider = drawable;
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            if (parent.getLayoutManager() == null || mDivider == null) {
                return;
            }
            if (mOrientation == VERTICAL) {
                drawVertical(c, parent);
            } else {
                drawHorizontal(c, parent);
            }
        }

        private void drawVertical(Canvas canvas, RecyclerView parent) {
            canvas.save();
            final int left;
            final int right;
            //noinspection AndroidLintNewApi - NewApi lint fails to handle overrides.
            if (parent.getClipToPadding()) {
                left = parent.getPaddingLeft();
                right = parent.getWidth() - parent.getPaddingRight();
                canvas.clipRect(left, parent.getPaddingTop(), right,
                        parent.getHeight() - parent.getPaddingBottom());
            } else {
                left = 0;
                right = parent.getWidth();
            }

            int childCount = parent.getChildCount();
            if (mSpanCount != -1) {
                childCount = childCount / mSpanCount + (childCount % mSpanCount > 0 ? 1 : 0);
            }
            for (int i = 0; i < childCount - 1; i++) {
                final View child = parent.getChildAt(i);
                parent.getDecoratedBoundsWithMargins(child, mBounds);
                final int bottom = mBounds.bottom + Math.round(child.getTranslationY());
                final int top = bottom - mDivider.getIntrinsicHeight();
                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(canvas);
            }
            canvas.restore();
        }

        private void drawHorizontal(Canvas canvas, RecyclerView parent) {
            canvas.save();
            final int top;
            final int bottom;
            //noinspection AndroidLintNewApi - NewApi lint fails to handle overrides.
            if (parent.getClipToPadding()) {
                top = parent.getPaddingTop();
                bottom = parent.getHeight() - parent.getPaddingBottom();
                canvas.clipRect(parent.getPaddingLeft(), top,
                        parent.getWidth() - parent.getPaddingRight(), bottom);
            } else {
                top = 0;
                bottom = parent.getHeight();
            }

            int childCount = parent.getChildCount();
            if (mSpanCount != -1) {
                childCount = mSpanCount;
            }
            for (int i = 0; i < childCount - 1; i++) {
                final View child = parent.getChildAt(i);
                parent.getLayoutManager().getDecoratedBoundsWithMargins(child, mBounds);
                final int right = mBounds.right + Math.round(child.getTranslationX());
                final int left = right - mDivider.getIntrinsicWidth();
                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(canvas);
            }
            canvas.restore();
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                   RecyclerView.State state) {
            if (mDivider == null) {
                outRect.set(0, 0, 0, 0);
                return;
            }
            if (mOrientation == VERTICAL) {
                int childCount = parent.getChildCount();
                if (mSpanCount != -1) {
                    childCount = childCount / mSpanCount + (childCount % mSpanCount > 0 ? 1 : 0);
                }
                int position = parent.getChildAdapterPosition(view);
                if (position < childCount - 1) {
                    outRect.set(0, 0, 0, mDivider.getIntrinsicHeight());
                } else {
                    outRect.set(0, 0, 0, 0);
                }
            } else {
                int childCount = parent.getChildCount();
                if (mSpanCount != -1) {
                    childCount = mSpanCount;
                }
                int position = parent.getChildAdapterPosition(view);
                if (position < childCount - 1) {
                    outRect.set(0, 0, mDivider.getIntrinsicWidth(), 0);
                } else {
                    outRect.set(0, 0, 0, 0);
                }
            }
        }
    }
}
