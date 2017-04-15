package com.peterlaurence.trekadvisor.menu.mapview.components;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;

import com.peterlaurence.trekadvisor.R;

/**
 * This {@link android.widget.ImageView} has two states :
 * <ul>
 * <li>Static : it appears as a classic "point of interest" marker.</li>
 * <li>Dynamic : it has a round shape with arrows turning around it, indicating that it can
 * be moved.</li>
 * </ul>
 * The transitions between the two states are animated, using animated vector drawables.
 *
 * @author peterLaurence on 08/04/17.
 */
public class MovableMarker extends android.support.v7.widget.AppCompatImageView {
    private AnimatedVectorDrawable mCurrentDrawable;
    private AnimatedVectorDrawable mRounded;
    private AnimatedVectorDrawable mStaticToDynamic;
    private AnimatedVectorDrawable mDynamicToStatic;


    /**
     * The {@code mRounded} drawable is just the end state of the {@code mStaticToDynamic}
     * {@link AnimatedVectorDrawable}. So by default, the maker is in its dynamic shape.
     */
    public MovableMarker(Context context) {
        super(context);

        mRounded = (AnimatedVectorDrawable) context.getDrawable(R.drawable.avd_marker_rounded);
        mStaticToDynamic = (AnimatedVectorDrawable) context.getDrawable(R.drawable.avd_marker_location_rounded);
        mDynamicToStatic = (AnimatedVectorDrawable) context.getDrawable(R.drawable.avd_marker_rounded_location);

        mCurrentDrawable = mRounded;
        setImageDrawable(mRounded);
        mRounded.start();
    }

    public void morphToStaticForm() {
        if (mCurrentDrawable == mRounded || mCurrentDrawable == mStaticToDynamic) {
            mCurrentDrawable.stop();
            mCurrentDrawable = mDynamicToStatic;
            setImageDrawable(mDynamicToStatic);
            mDynamicToStatic.start();
        }
    }

    public void morphToDynamicForm() {
        if (mCurrentDrawable == mDynamicToStatic) {
            mCurrentDrawable.stop();
            mCurrentDrawable = mStaticToDynamic;
            setImageDrawable(mStaticToDynamic);
            mStaticToDynamic.start();
        }
    }
}
