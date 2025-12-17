package com.example.forevernote.util;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * Utility class for common animations and transitions in the application.
 * Provides fade, slide, scale, and other visual effects.
 */
public class Animations {
    
    // Duration constants
    public static final Duration FAST = Duration.millis(150);
    public static final Duration NORMAL = Duration.millis(300);
    public static final Duration SLOW = Duration.millis(500);
    
    /**
     * Creates a fade in animation.
     */
    public static FadeTransition fadeIn(Node node, Duration duration) {
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        return fade;
    }
    
    /**
     * Creates a fade out animation.
     */
    public static FadeTransition fadeOut(Node node, Duration duration) {
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        return fade;
    }
    
    /**
     * Fades in a node and makes it visible.
     */
    public static void fadeInAndShow(Node node, Duration duration) {
        node.setOpacity(0.0);
        node.setVisible(true);
        FadeTransition fade = fadeIn(node, duration);
        fade.play();
    }
    
    /**
     * Fades out a node and hides it when complete.
     */
    public static void fadeOutAndHide(Node node, Duration duration) {
        FadeTransition fade = fadeOut(node, duration);
        fade.setOnFinished(e -> node.setVisible(false));
        fade.play();
    }
    
    /**
     * Creates a slide in from top animation.
     */
    public static TranslateTransition slideInFromTop(Node node, Duration duration) {
        node.setTranslateY(-node.getBoundsInParent().getHeight());
        TranslateTransition slide = new TranslateTransition(duration, node);
        slide.setToY(0);
        return slide;
    }
    
    /**
     * Creates a slide in from bottom animation.
     */
    public static TranslateTransition slideInFromBottom(Node node, Duration duration) {
        node.setTranslateY(node.getBoundsInParent().getHeight());
        TranslateTransition slide = new TranslateTransition(duration, node);
        slide.setToY(0);
        return slide;
    }
    
    /**
     * Creates a slide in from left animation.
     */
    public static TranslateTransition slideInFromLeft(Node node, Duration duration) {
        node.setTranslateX(-node.getBoundsInParent().getWidth());
        TranslateTransition slide = new TranslateTransition(duration, node);
        slide.setToX(0);
        return slide;
    }
    
    /**
     * Creates a slide in from right animation.
     */
    public static TranslateTransition slideInFromRight(Node node, Duration duration) {
        node.setTranslateX(node.getBoundsInParent().getWidth());
        TranslateTransition slide = new TranslateTransition(duration, node);
        slide.setToX(0);
        return slide;
    }
    
    /**
     * Creates a scale in animation.
     */
    public static ScaleTransition scaleIn(Node node, Duration duration) {
        node.setScaleX(0.0);
        node.setScaleY(0.0);
        ScaleTransition scale = new ScaleTransition(duration, node);
        scale.setToX(1.0);
        scale.setToY(1.0);
        return scale;
    }
    
    /**
     * Creates a scale out animation.
     */
    public static ScaleTransition scaleOut(Node node, Duration duration) {
        ScaleTransition scale = new ScaleTransition(duration, node);
        scale.setToX(0.0);
        scale.setToY(0.0);
        return scale;
    }
    
    /**
     * Creates a bounce animation.
     */
    public static ScaleTransition bounce(Node node, Duration duration) {
        ScaleTransition bounce = new ScaleTransition(duration, node);
        bounce.setFromX(1.0);
        bounce.setFromY(1.0);
        bounce.setToX(1.2);
        bounce.setToY(1.2);
        bounce.setAutoReverse(true);
        bounce.setCycleCount(2);
        return bounce;
    }
    
    /**
     * Creates a shake animation for error feedback.
     */
    public static Timeline shake(Node node, Duration duration) {
        double originalX = node.getTranslateX();
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(node.translateXProperty(), originalX)),
            new KeyFrame(duration.multiply(0.1), new KeyValue(node.translateXProperty(), originalX - 10)),
            new KeyFrame(duration.multiply(0.2), new KeyValue(node.translateXProperty(), originalX + 10)),
            new KeyFrame(duration.multiply(0.3), new KeyValue(node.translateXProperty(), originalX - 10)),
            new KeyFrame(duration.multiply(0.4), new KeyValue(node.translateXProperty(), originalX + 10)),
            new KeyFrame(duration.multiply(0.5), new KeyValue(node.translateXProperty(), originalX - 5)),
            new KeyFrame(duration.multiply(0.6), new KeyValue(node.translateXProperty(), originalX + 5)),
            new KeyFrame(duration.multiply(0.7), new KeyValue(node.translateXProperty(), originalX))
        );
        return timeline;
    }
    
    /**
     * Creates a pulse animation.
     */
    public static Timeline pulse(Node node, Duration duration) {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(node.opacityProperty(), 1.0)),
            new KeyFrame(duration.multiply(0.5), new KeyValue(node.opacityProperty(), 0.5)),
            new KeyFrame(duration, new KeyValue(node.opacityProperty(), 1.0))
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        return timeline;
    }
    
    /**
     * Creates a highlight animation.
     */
    public static Timeline highlight(Node node, Duration duration) {
        String originalStyle = node.getStyle();
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(node.styleProperty(), originalStyle)),
            new KeyFrame(duration.multiply(0.5), new KeyValue(node.styleProperty(), originalStyle + "; -fx-background-color: #fff3cd;")),
            new KeyFrame(duration, new KeyValue(node.styleProperty(), originalStyle))
        );
        return timeline;
    }
    
    /**
     * Creates a sequential animation that plays multiple animations one after another.
     */
    public static SequentialTransition sequential(Animation... animations) {
        return new SequentialTransition(animations);
    }
    
    /**
     * Creates a parallel animation that plays multiple animations simultaneously.
     */
    public static ParallelTransition parallel(Animation... animations) {
        return new ParallelTransition(animations);
    }
    
    /**
     * Creates a smooth transition between two nodes (crossfade).
     */
    public static void crossfade(Node nodeOut, Node nodeIn, Duration duration) {
        nodeOut.setVisible(true);
        nodeIn.setVisible(true);
        nodeIn.setOpacity(0.0);
        
        FadeTransition fadeOut = fadeOut(nodeOut, duration);
        FadeTransition fadeIn = fadeIn(nodeIn, duration);
        
        fadeOut.setOnFinished(e -> nodeOut.setVisible(false));
        
        ParallelTransition crossfade = parallel(fadeOut, fadeIn);
        crossfade.play();
    }
    
    /**
     * Creates a loading animation with rotation.
     */
    public static RotateTransition loading(Node node, Duration duration) {
        RotateTransition rotate = new RotateTransition(duration, node);
        rotate.setByAngle(360);
        rotate.setCycleCount(Animation.INDEFINITE);
        rotate.setInterpolator(Interpolator.LINEAR);
        return rotate;
    }
    
    /**
     * Creates a smooth size change animation.
     */
    public static Timeline resize(Region region, double targetWidth, double targetHeight, Duration duration) {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(region.prefWidthProperty(), region.getPrefWidth()),
                new KeyValue(region.prefHeightProperty(), region.getPrefHeight())),
            new KeyFrame(duration,
                new KeyValue(region.prefWidthProperty(), targetWidth),
                new KeyValue(region.prefHeightProperty(), targetHeight))
        );
        return timeline;
    }
    
    /**
     * Easing functions for more natural animations.
     */
    public static class Easing {
        public static final Interpolator EASE_OUT = Interpolator.SPLINE(0.25, 0.1, 0.25, 1);
        public static final Interpolator EASE_IN = Interpolator.SPLINE(0.42, 0, 1, 1);
        public static final Interpolator EASE_IN_OUT = Interpolator.SPLINE(0.42, 0, 0.58, 1);
        public static final Interpolator BOUNCE = Interpolator.SPLINE(0.68, -0.55, 0.265, 1.55);
    }
}