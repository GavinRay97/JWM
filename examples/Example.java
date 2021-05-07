package org.jetbrains.jwm.examples;

import java.util.*;
import java.util.function.*;
import lombok.*;
import org.jetbrains.annotations.*;
import org.jetbrains.jwm.*;
import org.jetbrains.skija.*;

public class Example implements Consumer<Event> {
    public Window window;
    public ContextMetal contextMtl;
    public DirectContext directContext;

    public int angle = 0;
    public Font font = new Font(FontMgr.getDefault().matchFamilyStyleCharacter(null, FontStyle.NORMAL, null, "↑".codePointAt(0)), 12);
    public Font font48 = new Font(FontMgr.getDefault().matchFamilyStyle(null, FontStyle.BOLD), 48);

    public String[] variants = new String[] {
        "Metal +vsync +transact", 
        "Metal +vsync −transact",
        "Metal −vsync +transact",
        "Metal −vsync −transact",
    };
    public int variantIdx = 1;

    public long t0 = System.nanoTime();
    public double[] times = new double[180];
    public int timesIdx = 0;

    public Example() {
        window = App.makeWindow();
        window.setEventListener(this);
        changeContext();
        window.show();
    }

    public void paint() {
        // System.out.println(System.currentTimeMillis() + " " + "paint()");
        if (contextMtl == null || directContext == null)
            return;
        
        long texturePtr = contextMtl.nextDrawableTexturePtr();
        float scale = 2; // TODO
        int width = contextMtl.getWidth();
        int height = contextMtl.getHeight();

        try (var renderTarget = BackendRenderTarget.makeMetal(width, height, texturePtr);
             var surface = Surface.makeFromBackendRenderTarget(
                             directContext,
                             renderTarget,
                             SurfaceOrigin.TOP_LEFT,
                             SurfaceColorFormat.BGRA_8888,
                             ColorSpace.getSRGB(),  // TODO load monitor profile
                             new SurfaceProps(PixelGeometry.RGB_H)))
        {
            var canvas = surface.getCanvas();
            canvas.clear(0xFF264653);
            int layer = canvas.save();
            canvas.scale(scale, scale);
            width = (int) (width / scale);
            height = (int) (height / scale);

            // Triangles
            try (var paint = new Paint()) {
                int[] colors = new int[] { 0xFFe76f51, 0xFF2a9d8f, 0xFFe9c46a };
                canvas.drawTriangles(new Point[] { new Point(10, 10), new Point(200, 10), new Point (10, 200) }, colors, paint);
                canvas.drawTriangles(new Point[] { new Point(width - 10, 10), new Point(width - 200, 10), new Point(width - 10, 200) }, colors, paint);
                canvas.drawTriangles(new Point[] { new Point(10, height - 10), new Point(200, height - 10), new Point (10, height - 200) }, colors, paint);
                // canvas.drawTriangles(new Point[] { new Point(width - 10, height - 10), new Point(width - 200, height - 10), new Point(width - 10, height - 200) }, colors, paint);

                canvas.save();
                canvas.translate(width / 2, height / 2);
                paint.setColor(0xFFFFFFFF);
                canvas.drawCircle(0, 0, 100, paint);
                angle = (angle + 3) % 360;
                canvas.rotate(angle);
                paint.setColor(0xFF264653);
                canvas.drawRect(Rect.makeXYWH(-7, -100, 14, 200), paint);
                canvas.restore();
            }

            // VSync
            try (var paint = new Paint()) {
                canvas.save();
                canvas.translate(width / 2 - 100, height - 120);
                paint.setColor(0xFFE0E0E0);
                canvas.drawRRect(RRect.makeXYWH(0, 0, 200, 80, 4), paint);
                paint.setColor(angle % 2 == 0 ? 0xFFEF8784 : 0xFFA1FCFE);
                var bounds = font48.measureText("VSYNC");
                canvas.drawString("VSYNC", (200 - bounds.getWidth()) / 2, (80 + font48.getMetrics().getCapHeight()) / 2, font48, paint);
                canvas.restore();
            }

            // HUD
            try (var paint = new Paint()) {
                canvas.save();
                canvas.translate(width - (8 + 180 + 8 + 8), height - (8 + 24 + 24 + 24 + 24 + 32 + 8 + 8));

                // bg
                paint.setColor(0x40000000);
                canvas.drawRRect(RRect.makeXYWH(0, 0, 8 + 180 + 8, 8 + 24 + 24 + 24 + 24 + 32 + 8, 4), paint);
                canvas.translate(8, 8);

                // Variant
                paint.setColor(0x80000000);
                canvas.drawRRect(RRect.makeXYWH(0, 0, 16, 16, 2), paint);
                paint.setColor(0xFFFFFFFF);
                canvas.drawString("↓↑", 0, 12, font, paint);
                canvas.drawString(variants[variantIdx], 24, 12, font, paint);
                canvas.translate(0, 24);

                paint.setColor(0x80000000);
                canvas.drawRRect(RRect.makeXYWH(0, 0, 16, 16, 2), paint);
                paint.setColor(0xFFFFFFFF);
                canvas.drawString("N", 3, 12, font, paint);
                canvas.drawString("New window", 24, 12, font, paint);
                canvas.translate(0, 24);

                paint.setColor(0x80000000);
                canvas.drawRRect(RRect.makeXYWH(0, 0, 16, 16, 2), paint);
                paint.setColor(0xFFFFFFFF);
                canvas.drawString("W", 3, 12, font, paint);
                canvas.drawString("Close window", 24, 12, font, paint);
                canvas.translate(0, 24);

                int frames = 0;
                double time = 0;
                for (int i = 0; i < times.length; ++i) {
                    var idx = (timesIdx - i + times.length) % times.length;
                    time += times[idx];
                    frames++;
                    if (time > 1000)
                        break;
                }
                canvas.drawString("FPS: " + String.format("%.01f", (frames / time * 1000)), 0, 12, font, paint);
                canvas.translate(0, 24);

                // FPS
                long t1 = System.nanoTime();
                times[timesIdx] = (t1 - t0) / 1000000.0;
                t0 = t1;
                timesIdx = (timesIdx + 1) % times.length;
                
                paint.setColor(0x4033cc33);
                canvas.drawRRect(RRect.makeXYWH(-2, -2, 184, 36, 2), paint);

                paint.setColor(0xFF33CC33);
                for (int i = 0; i < times.length; ++i) {
                    var idx = (timesIdx + i) % times.length;
                    canvas.drawRect(Rect.makeXYWH(i, 32 - (float) times[idx], 1, (float) times[idx]), paint);
                }

                paint.setColor(0x80000000);
                canvas.drawRect(Rect.makeXYWH(-2, 32 - 17, times.length + 4, 1), paint);
                canvas.drawRect(Rect.makeXYWH(-2, 32 - 8, times.length + 4, 1), paint);
                canvas.restore();
            }

            canvas.restoreToCount(layer);

            surface.flushAndSubmit();
            contextMtl.swapBuffers();
        }
    }

    public void changeContext() {
        if (directContext != null) {
            directContext.abandon();
            directContext.close();
        }

        boolean vsync = variants[variantIdx].indexOf("+vsync") >= 0;
        boolean transact = variants[variantIdx].indexOf("+transact") >= 0;
        contextMtl = new ContextMetal(ContextMetalOpts.DEFAULT.withVsync(vsync).withTransact(transact));
        window.attach(contextMtl);
        directContext = DirectContext.makeMetal(contextMtl.getDevicePtr(), contextMtl.getQueuePtr());
    }

    @Override
    public void accept(Event e) {
        if (e instanceof EventClose) {
            window.close();
            if (App._windows.size() == 0)
                App.terminate();
        } else if (e instanceof EventKeyboard) {
            EventKeyboard eventKeyboard = (EventKeyboard) e;
            if (eventKeyboard.isPressed() == true) {
                if (eventKeyboard.getKeyCode() == 45) { // n
                    new Example();
                } else if (eventKeyboard.getKeyCode() == 13 || eventKeyboard.getKeyCode() == 53) { // w || Esc
                    window.close();
                    if (App._windows.size() == 0)
                        App.terminate();
                } else if (eventKeyboard.getKeyCode() == 125) { // ↓
                    variantIdx = (variantIdx + 1) % variants.length;
                    changeContext();
                } else if (eventKeyboard.getKeyCode() == 126) { // ↑
                    variantIdx = (variantIdx + variants.length - 1) % variants.length;
                    changeContext();
                } else 
                    System.out.println("Key pressed: " + eventKeyboard.getKeyCode());
            }
        } else if (e instanceof EventPaint) {
            paint();
        }
    }

    public static void main(String[] args) {
        App.init();
        new Example();
        App.runEventLoop();
    }
}