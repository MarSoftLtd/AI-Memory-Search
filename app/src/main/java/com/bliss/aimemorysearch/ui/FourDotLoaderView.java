package com.bliss.aimemorysearch.ui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public final class FourDotLoaderView extends View {
    private static final int[] DOT_COLORS = {
            Color.rgb(66, 133, 244),
            Color.rgb(234, 67, 53),
            Color.rgb(251, 188, 4),
            Color.rgb(52, 168, 83)
    };
    private static final long CYCLE_DURATION_MS = 1200L;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private AnimatorSet animatorSet;
    private float pulsePhase;

    public FourDotLoaderView(Context context) {
        super(context);
    }

    public FourDotLoaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FourDotLoaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float baseRadius = Math.min(getWidth(), getHeight()) * 0.095f;
        float orbitRadius = Math.min(getWidth(), getHeight()) * 0.29f;

        for (int index = 0; index < DOT_COLORS.length; index++) {
            double angle = Math.PI * 2d * index / DOT_COLORS.length;
            float wave = (float) Math.sin(
                    Math.PI * 2d * pulsePhase - Math.PI * 2d * index / DOT_COLORS.length);
            float radius = baseRadius * (1f + 0.18f * wave);
            paint.setColor(DOT_COLORS[index]);
            canvas.drawCircle(
                    centerX + orbitRadius * (float) Math.cos(angle),
                    centerY + orbitRadius * (float) Math.sin(angle),
                    radius,
                    paint);
        }
    }

    public void setPulsePhase(float pulsePhase) {
        this.pulsePhase = pulsePhase;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateAnimationState();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnimation();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        updateAnimationState();
    }

    private void updateAnimationState() {
        if (isAttachedToWindow() && isShown()) {
            startAnimation();
        } else {
            stopAnimation();
        }
    }

    private void startAnimation() {
        if (animatorSet != null && animatorSet.isStarted()) {
            return;
        }
        ObjectAnimator rotationAnimator =
                ObjectAnimator.ofFloat(this, View.ROTATION, 0f, 360f);
        rotationAnimator.setDuration(CYCLE_DURATION_MS);
        rotationAnimator.setInterpolator(new LinearInterpolator());
        rotationAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        rotationAnimator.setRepeatMode(ObjectAnimator.RESTART);

        ObjectAnimator pulseAnimator =
                ObjectAnimator.ofFloat(this, "pulsePhase", 0f, 1f);
        pulseAnimator.setDuration(CYCLE_DURATION_MS);
        pulseAnimator.setInterpolator(new LinearInterpolator());
        pulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ObjectAnimator.RESTART);

        animatorSet = new AnimatorSet();
        animatorSet.playTogether(rotationAnimator, pulseAnimator);
        animatorSet.start();
    }

    private void stopAnimation() {
        if (animatorSet == null) {
            return;
        }
        for (Animator animator : animatorSet.getChildAnimations()) {
            animator.cancel();
        }
        animatorSet.cancel();
        animatorSet = null;
        setRotation(0f);
        pulsePhase = 0f;
        invalidate();
    }
}
