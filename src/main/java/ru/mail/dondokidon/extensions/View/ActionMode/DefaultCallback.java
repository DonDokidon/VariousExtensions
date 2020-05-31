package ru.mail.dondokidon.extensions.View.ActionMode;


import android.animation.Animator;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

/**
 * Default {@link ru.mail.dondokidon.extensions.View.ActionMode.ActionMode.Callback}
 * without appearing and disappearing animations.
 */
public abstract class DefaultCallback implements ActionMode.Callback {

    @NonNull
    @Override
    public abstract Toolbar createToolbar(Context context);

    @Nullable
    @Override
    public Animator createAppearAnimator(Toolbar ActionModeToolbar) {
        return null;
    }

    @Nullable
    @Override
    public Animator createDisappearAnimator(Toolbar ActionModeToolbar) {
        return null;
    }
}
