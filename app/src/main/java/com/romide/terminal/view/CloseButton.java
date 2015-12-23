package com.romide.terminal.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

/**
 * View which isn't automatically in the pressed state if its parent is
 * pressed. This allows the window's entry to be pressed without the close
 * button being triggered. Idea and code shamelessly borrowed from the
 * Android browser's tabs list.
 *
 * Used by layout xml.
 */
public class CloseButton extends ImageView {
    public CloseButton(Context context) {
        super(context);
    }

    public CloseButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CloseButton(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style);
    }

    @Override
    public void setPressed(boolean pressed) {
        if (pressed && ((View) getParent()).isPressed()) {
            return;
        }
        super.setPressed(pressed);
    }
}
