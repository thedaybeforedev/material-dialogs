package com.initialz.materialdialogs.simplelist;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.initialz.materialdialogs.R;

/**
 * Created by initialz on 2017. 7. 2..
 */

public class DividerItemDecoration extends RecyclerView.ItemDecoration {

    private Drawable mDivider;
    private int paddingLeftRight;

    public DividerItemDecoration(Context context) {
        mDivider = context.getResources().getDrawable(R.drawable.md_line_divider);
        paddingLeftRight = context.getResources().getDimensionPixelSize(R.dimen.md_listitem_default_padding);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int left = paddingLeftRight;
        int right = parent.getWidth() - paddingLeftRight;

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            int top = child.getBottom() + params.bottomMargin;
            int bottom = top + mDivider.getIntrinsicHeight();

            if (i != childCount-1){
                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }

        }
    }

}
