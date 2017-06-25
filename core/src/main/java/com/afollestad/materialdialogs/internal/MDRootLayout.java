package com.afollestad.materialdialogs.internal;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ScrollView;

import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.R;
import com.afollestad.materialdialogs.StackingBehavior;
import com.afollestad.materialdialogs.util.DialogUtils;

/**
 * @author Kevin Barry (teslacoil) 4/02/2015 This is the top level view for all MaterialDialogs It
 *         handles the layout of: titleFrame (md_stub_titleframe) content (text, custom view, listview,
 *         etc) buttonDefault... (either stacked or horizontal)
 */
public class MDRootLayout extends ViewGroup {

    private static final int INDEX_NEGATIVE = 0;
    private static final int INDEX_POSITIVE = 1;
    private final MDButton[] buttons = new MDButton[2];
    private int maxHeight;
    private View headingInfoBar;
    private View titleBar;
    private View content;
    private boolean drawTopDivider = false;
    private boolean drawBottomDivider = false;
    private StackingBehavior stackBehavior = StackingBehavior.ADAPTIVE;
    private boolean useFullPadding = true;
    private boolean reducePaddingNoTitleNoButtons;
    private boolean noTitleNoPadding;

    private boolean noHeadingInfoNoPadding;

    private int noTitlePaddingFull;
    private int buttonBarHeight;

    /* Margin from dialog frame to first button */
    private int buttonHorizontalEdgeMargin;

    private Paint dividerPaint;

    private int dividerWidth;

    public MDRootLayout(Context context) {
        super(context);
        init(context, null, 0);
    }

    public MDRootLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public MDRootLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MDRootLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr);
    }

    private static boolean isVisible(View v) {
        boolean visible = v != null && v.getVisibility() != View.GONE;
        if (visible && v instanceof MDButton) {
            visible = ((MDButton) v).getText().toString().trim().length() > 0;
        }
        return visible;
    }


    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        Resources r = context.getResources();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MDRootLayout, defStyleAttr, 0);
        reducePaddingNoTitleNoButtons =
                a.getBoolean(R.styleable.MDRootLayout_md_reduce_padding_no_title_no_buttons, true);
        a.recycle();

        noTitlePaddingFull = r.getDimensionPixelSize(R.dimen.md_notitle_vertical_padding);

        buttonHorizontalEdgeMargin = r.getDimensionPixelSize(R.dimen.md_button_padding_frame_side);
        buttonBarHeight = r.getDimensionPixelSize(R.dimen.md_button_height);

        dividerPaint = new Paint();
        dividerWidth = r.getDimensionPixelSize(R.dimen.md_divider_height);
        dividerPaint.setColor(DialogUtils.resolveColor(context, R.attr.md_divider_color));
        setWillNotDraw(false);
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    public void noTitleNoPadding() {
        noTitleNoPadding = true;
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (v.getId() == R.id.md_headingInfoFrame) {
                headingInfoBar = v;
            } else if (v.getId() == R.id.md_titleFrame) {
                titleBar = v;
            } else if (v.getId() == R.id.md_buttonDefaultNegative) {
                buttons[INDEX_NEGATIVE] = (MDButton) v;
            } else if (v.getId() == R.id.md_buttonDefaultPositive) {
                buttons[INDEX_POSITIVE] = (MDButton) v;
            } else {
                content = v;
            }
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (height > maxHeight) {
            height = maxHeight;
        }

        useFullPadding = true;
        boolean hasButtons = false;

        final boolean stacked;
        if (stackBehavior == StackingBehavior.ALWAYS) {
            stacked = true;
        } else if (stackBehavior == StackingBehavior.NEVER) {
            stacked = false;
        } else {
            int buttonsWidth = 0;
            for (MDButton button : buttons) {
                if (button != null && isVisible(button)) {
                    button.setStacked(false, false);
                    measureChild(button, widthMeasureSpec, heightMeasureSpec);
                    buttonsWidth += button.getMeasuredWidth();
                    hasButtons = true;
                }
            }

            int buttonBarPadding =
                    getContext().getResources().getDimensionPixelSize(R.dimen.md_neutral_button_margin);
            final int buttonFrameWidth = width - 2 * buttonBarPadding;
            stacked = buttonsWidth > buttonFrameWidth;
        }

        int stackedHeight = 0;

        if (stacked) {
            for (MDButton button : buttons) {
                if (button != null && isVisible(button)) {
                    button.setStacked(true, false);
                    measureChild(button, widthMeasureSpec, heightMeasureSpec);
                    stackedHeight += button.getMeasuredHeight();
                    hasButtons = true;
                }
            }
        }

        int availableHeight = height;
        int fullPadding = 0;
        int minPadding = 0;
        if (hasButtons) {
            availableHeight -= buttonBarHeight;
        }

        if (isVisible(headingInfoBar)) {
            headingInfoBar.measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.UNSPECIFIED);
            availableHeight -= headingInfoBar.getMeasuredHeight();
        } else if (!noTitleNoPadding) {
            fullPadding += noTitlePaddingFull;
        }

        if (isVisible(titleBar)) {
            titleBar.measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.UNSPECIFIED);
            availableHeight -= titleBar.getMeasuredHeight();
        } else if (!noTitleNoPadding) {
            fullPadding += noTitlePaddingFull;
        }

        if (isVisible(content)) {
            content.measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(availableHeight - minPadding, MeasureSpec.AT_MOST));

            if (content.getMeasuredHeight() <= availableHeight - fullPadding) {
                if (!reducePaddingNoTitleNoButtons || isVisible(titleBar) || hasButtons) {
                    useFullPadding = true;
                    availableHeight -= content.getMeasuredHeight() + fullPadding;
                } else {
                    useFullPadding = false;
                    availableHeight -= content.getMeasuredHeight() + minPadding;
                }
            } else {
                useFullPadding = false;
                availableHeight = 0;
            }
        }

        setMeasuredDimension(width, height - availableHeight);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);


        if (drawTopDivider) {
            int y = titleBar.getTop();
            canvas.drawRect(0, y - dividerWidth, getMeasuredWidth(), y, dividerPaint);
        }

        if (drawBottomDivider) {
            int y = content.getBottom();
            canvas.drawRect(0, y, getMeasuredWidth(), y + dividerWidth, dividerPaint);
        }

    }

    @Override
    protected void onLayout(boolean changed, final int l, int t, final int r, int b) {
        if (isVisible(headingInfoBar)) {
            int height = headingInfoBar.getMeasuredHeight();
            headingInfoBar.layout(l, t, r, t + height);
            t += height;
        } else if (!noTitleNoPadding && useFullPadding) {
            t += noTitlePaddingFull;
        }

        if (isVisible(titleBar)) {
            int height = titleBar.getMeasuredHeight();
            titleBar.layout(l, t, r, t + height);
            t += height;
        } else if (!noTitleNoPadding && useFullPadding) {
            t += noTitlePaddingFull;
        }

        if (isVisible(content)) {
            content.layout(l, t, r, t + content.getMeasuredHeight());
        }


        int barTop;
        int barBottom = b;

        barTop = barBottom - buttonBarHeight;

        int offset = buttonHorizontalEdgeMargin;

        if (isVisible(buttons[INDEX_NEGATIVE])) {
            int bl, br;

            bl = l + buttonHorizontalEdgeMargin;
            br = isVisible(buttons[INDEX_POSITIVE]) ? bl + content.getMeasuredWidth()/2 : content.getMeasuredWidth();

            buttons[INDEX_NEGATIVE].layout(bl, barTop, br, barBottom);
        }

        if (isVisible(buttons[INDEX_POSITIVE])) {
            int bl, br;

            br = r - offset;
            bl = isVisible(buttons[INDEX_NEGATIVE]) ? br - content.getMeasuredWidth()/2 : 0;

            buttons[INDEX_POSITIVE].layout(bl, barTop, br, barBottom);
            offset += buttons[INDEX_POSITIVE].getMeasuredWidth();
        }




        setUpDividersVisibility(content);
    }

    public void setStackingBehavior(StackingBehavior behavior) {
        stackBehavior = behavior;
        invalidate();
    }

    public void setDividerColor(int color) {
        dividerPaint.setColor(color);
        invalidate();
    }


    public void setButtonStackedGravity(GravityEnum gravity) {
        for (MDButton mButton : buttons) {
            if (mButton != null) {
                mButton.setStackedGravity(gravity);
            }
        }
    }

    private void setUpDividersVisibility(
            final View view) {
        if (view == null) {
            return;
        }

        boolean hasButtons = false;
        for (MDButton button : buttons) {
            if (button != null && button.getVisibility() != View.GONE) {
                hasButtons = true;
                break;
            }
        }
        invalidateDividers(view, hasButtons);
        invalidate();

    }


    private void invalidateDividers(
            View view, boolean hasButtons) {
        drawTopDivider =
                headingInfoBar != null
                        && headingInfoBar.getVisibility() != View.GONE;

        drawBottomDivider = hasButtons;

    }

}
