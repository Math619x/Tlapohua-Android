package com.rey.celdas;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;

final class TlapohuaArt {
    static final int PAPER = 0xfff3e9d5;
    static final int SAND = 0xffe8d8b8;
    static final int JADE = 0xff1e6253;
    static final int BASALT = 0xff203d35;
    static final int TERRACOTTA = 0xff90452f;
    static final int GOLD = 0xffe7bb71;
    static final int CREAM = 0xfffff7e6;

    enum Mark { TRAIL, SUN, LESSON, GRID, COLLECTION, EXIT }

    private TlapohuaArt() { }

    static Drawable paper(float density, boolean forest) {
        return new Painted() {
            @Override void paint(Canvas canvas, Rect bounds) {
                float width = bounds.width(), height = bounds.height();
                fill.setColor(0xffffffff);
                fill.setShader(new LinearGradient(0, 0, width, height,
                    forest ? 0xffe0e9d2 : 0xfffff5df, forest ? 0xfff1e1bd : 0xffe6d3ac, Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, width, height, fill);
                fill.setShader(null);
                stroke.setColor(forest ? 0x38536d4b : 0x358c6639);
                stroke.setStrokeWidth(density);
                float step = 7 * density;
                for (float y = step; y + 4 * step < height; y += 5 * step) {
                    stepped(canvas, step * .4f, y, step * .55f, true);
                    stepped(canvas, width - step * .4f, y + 2 * step, step * .55f, false);
                }
                fill.setColor(forest ? 0x155b7551 : 0x168c6639);
                for (float y = 13 * density; y < height; y += 29 * density) {
                    for (float x = 12 * density + (y % (13 * density)); x < width; x += 31 * density) {
                        canvas.drawRect(x, y, x + density, y + density, fill);
                    }
                }
            }
        };
    }

    static Drawable tile(int color, float density, int radius, boolean interactive) {
        Painted stone = new Painted() {
            @Override void paint(Canvas canvas, Rect bounds) {
                float width = bounds.width(), height = bounds.height();
                float cut = Math.min(radius * density * .55f, Math.min(width, height) / 4f);
                float edge = density * .7f;
                path.reset();
                path.moveTo(cut, edge);
                path.lineTo(width - cut, edge);
                path.lineTo(width - edge, cut);
                path.lineTo(width - edge, height - cut);
                path.lineTo(width - cut, height - edge);
                path.lineTo(cut, height - edge);
                path.lineTo(edge, height - cut);
                path.lineTo(edge, cut);
                path.close();
                fill.setShader(new LinearGradient(0, 0, 0, height, blend(color, 0xffffffff, .09f),
                    blend(color, 0xff000000, .08f), Shader.TileMode.CLAMP));
                canvas.drawPath(path, fill);
                fill.setShader(null);
                stroke.setColor(blend(color, 0xff000000, .24f));
                stroke.setStrokeWidth(density * 1.1f);
                canvas.drawPath(path, stroke);
                stroke.setColor(blend(color, 0xffffffff, .35f));
                stroke.setStrokeWidth(density * .8f);
                canvas.drawLine(cut + density, 3 * density, width - cut - density, 3 * density, stroke);
                if (width >= 65 * density) {
                    stroke.setColor(blend(color, GOLD, .52f));
                    float corner = 4 * density;
                    canvas.drawLine(corner, cut, corner, cut + 4 * density, stroke);
                    canvas.drawLine(width - corner, height - cut - 4 * density, width - corner, height - cut, stroke);
                }
            }
        };
        if (!interactive) return stone;
        return new RippleDrawable(ColorStateList.valueOf(0x407ad4ad), stone, null);
    }

    static Drawable scene(boolean forest) {
        return new Painted() {
            @Override void paint(Canvas canvas, Rect bounds) {
                canvas.scale(bounds.width() / 132f, bounds.height() / 86f);
                circle(canvas, 92, 26, 16, 0xffb7814d);
                circle(canvas, 92, 26, 12, GOLD);
                polygon(canvas, 0xff32695a, 0, 61, 17, 43, 34, 51, 51, 33, 82, 58, 99, 45, 132, 64, 132, 86, 0, 86);
                polygon(canvas, 0xff214e43, 0, 68, 24, 58, 48, 68, 79, 59, 103, 66, 132, 61, 132, 86, 0, 86);
                if (forest) {
                    tree(canvas, 22, 64, 13, 0xff82a57c);
                    tree(canvas, 40, 73, 17, 0xffa1b587);
                    tree(canvas, 112, 72, 18, 0xff88a97c);
                    tree(canvas, 123, 61, 13, 0xff6f9674);
                    polygon(canvas, 0xffddbd83, 59, 86, 77, 73, 73, 67, 92, 58, 99, 60, 84, 68, 89, 74, 76, 86);
                    polygon(canvas, 0xffc38d5c, 95, 63, 95, 55, 108, 55, 108, 63);
                } else {
                    polygon(canvas, 0xffb47b59, 31, 81, 31, 72, 40, 72, 40, 63, 51, 63, 51, 54, 61, 54, 61, 45,
                        95, 45, 95, 54, 105, 54, 105, 63, 115, 63, 115, 72, 124, 72, 124, 81);
                    polygon(canvas, 0xffdec38d, 68, 80, 72, 46, 84, 46, 89, 80);
                    stroke.setColor(0xff7f543e);
                    stroke.setStrokeWidth(1);
                    for (int y = 51; y < 80; y += 4) canvas.drawLine(69, y, 88, y, stroke);
                    polygon(canvas, 0xffe2c791, 60, 45, 60, 33, 69, 33, 69, 45);
                    polygon(canvas, 0xffe2c791, 87, 45, 87, 33, 96, 33, 96, 45);
                    polygon(canvas, TERRACOTTA, 58, 34, 58, 31, 71, 31, 71, 34);
                    polygon(canvas, TERRACOTTA, 85, 34, 85, 31, 98, 31, 98, 34);
                    polygon(canvas, BASALT, 63, 44, 63, 38, 66, 38, 66, 44);
                    polygon(canvas, BASALT, 90, 44, 90, 38, 93, 38, 93, 44);
                    stroke.setColor(0xffe7b683);
                    canvas.drawLine(44, 71, 68, 71, stroke);
                    canvas.drawLine(88, 71, 111, 71, stroke);
                    canvas.drawLine(54, 62, 69, 62, stroke);
                    canvas.drawLine(87, 62, 103, 62, stroke);
                    tree(canvas, 20, 81, 12, 0xff86a57d);
                }
            }
        };
    }

    static Drawable frieze() {
        return new Painted() {
            @Override void paint(Canvas canvas, Rect bounds) {
                float width = bounds.width(), height = bounds.height();
                float unit = height / 6f;
                float repeat = unit * 12;
                fill.setColor(BASALT);
                canvas.drawRect(0, 0, width, height, fill);
                stroke.setColor(0xffa47649);
                stroke.setStrokeWidth(unit * .5f);
                canvas.drawLine(0, unit * .4f, width, unit * .4f, stroke);
                canvas.drawLine(0, height - unit * .4f, width, height - unit * .4f, stroke);
                int motifs = (int) (width / repeat);
                float start = (width - motifs * repeat) / 2f;
                for (int i = 0; i < motifs; i++) {
                    float x = start + i * repeat;
                    stroke.setColor(GOLD);
                    line(canvas, x + unit, 4 * unit, x + 3 * unit, 4 * unit, x + 3 * unit, 2 * unit,
                        x + 6 * unit, 2 * unit, x + 6 * unit, 4 * unit, x + 4 * unit, 4 * unit);
                    float center = x + 9 * unit;
                    polygon(canvas, 0xffc48260, center, 2 * unit, center + unit, 3 * unit,
                        center, 4 * unit, center - unit, 3 * unit);
                }
            }
        };
    }

    static Drawable seal(Mark mark) {
        return new Painted() {
            @Override void paint(Canvas canvas, Rect bounds) {
                canvas.scale(bounds.width() / 48f, bounds.height() / 48f);
                polygon(canvas, 0xffdfc797, 8, 2, 40, 2, 46, 8, 46, 40, 40, 46, 8, 46, 2, 40, 2, 8);
                polygon(canvas, BASALT, 10, 5, 38, 5, 43, 10, 43, 38, 38, 43, 10, 43, 5, 38, 5, 10);
                stroke.setColor(GOLD);
                stroke.setStrokeWidth(2);
                switch (mark) {
                    case TRAIL:
                        line(canvas, 11, 34, 19, 27, 25, 28, 35, 13);
                        circle(canvas, 11, 34, 2.5f, GOLD);
                        circle(canvas, 35, 13, 2.5f, GOLD);
                        line(canvas, 21, 18, 21, 12, 16, 12);
                        break;
                    case SUN:
                        canvas.drawCircle(24, 24, 7, stroke);
                        for (int i = 0; i < 8; i++) {
                            double angle = Math.PI * i / 4;
                            float x = (float) Math.cos(angle), y = (float) Math.sin(angle);
                            canvas.drawLine(24 + x * 11, 24 + y * 11, 24 + x * 15, 24 + y * 15, stroke);
                        }
                        break;
                    case LESSON:
                        line(canvas, 12, 34, 12, 15, 20, 15, 24, 19, 28, 15, 36, 15, 36, 34, 28, 34, 24, 37, 20, 34, 12, 34);
                        canvas.drawLine(24, 19, 24, 36, stroke);
                        break;
                    case GRID:
                        canvas.drawRect(13, 13, 35, 35, stroke);
                        canvas.drawLine(24, 13, 24, 35, stroke);
                        canvas.drawLine(13, 24, 35, 24, stroke);
                        break;
                    case COLLECTION:
                        line(canvas, 14, 30, 14, 12, 30, 12);
                        canvas.drawRect(19, 17, 35, 37, stroke);
                        canvas.drawLine(23, 24, 31, 24, stroke);
                        canvas.drawLine(23, 30, 31, 30, stroke);
                        break;
                    case EXIT:
                        line(canvas, 25, 13, 14, 13, 14, 35, 25, 35);
                        line(canvas, 20, 24, 35, 24, 29, 18);
                        line(canvas, 35, 24, 29, 30);
                        break;
                }
            }
        };
    }

    static Drawable rankSeal(int rank) {
        int[] materials = {0xffa97146, 0xffbd724d, 0xff393e53, 0xff328063, 0xff2fabb0};
        int material = materials[rank];
        return new Painted() {
            @Override void paint(Canvas canvas, Rect bounds) {
                canvas.scale(bounds.width() / 64f, bounds.height() / 64f);
                polygon(canvas, 0xffc8a36b, 12, 2, 52, 2, 62, 12, 62, 52, 52, 62, 12, 62, 2, 52, 2, 12);
                polygon(canvas, BASALT, 14, 5, 50, 5, 59, 14, 59, 50, 50, 59, 14, 59, 5, 50, 5, 14);
                int light = blend(material, CREAM, .4f);
                int dark = blend(material, 0xff101421, .48f);
                polygon(canvas, material, 32, 10, 52, 25, 46, 49, 32, 55, 18, 49, 12, 25);
                stroke.setStrokeWidth(1.6f);
                switch (rank) {
                    case 0:
                        stroke.setColor(dark);
                        for (int i = 0; i < 4; i++)
                            line(canvas, 17 + i * 7, 26 - i * 3, 22 + i * 6, 32, 21 + i * 6, 43, 25 + i * 5, 49);
                        stroke.setColor(light);
                        canvas.drawOval(27, 27, 39, 41, stroke);
                        break;
                    case 1:
                        polygon(canvas, light, 32, 11, 50, 25, 39, 29, 20, 22);
                        for (int i = 0; i < 8; i++) circle(canvas, 21 + (i % 3) * 9, 29 + (i / 3) * 8, 1.9f, dark);
                        break;
                    case 2:
                        polygon(canvas, light, 32, 10, 39, 27, 19, 29, 12, 25);
                        polygon(canvas, dark, 39, 27, 52, 25, 46, 49, 32, 55);
                        stroke.setColor(0xffa9b4c2);
                        line(canvas, 32, 14, 38, 27, 29, 37, 33, 44);
                        break;
                    case 3:
                        stroke.setColor(light);
                        line(canvas, 20, 33, 20, 26, 31, 26, 31, 18, 40, 25, 40, 34, 46, 34);
                        line(canvas, 22, 40, 30, 40, 30, 47, 39, 43);
                        circle(canvas, 32, 34, 4, dark);
                        break;
                    default:
                        polygon(canvas, light, 32, 14, 47, 26, 40, 30, 27, 23);
                        stroke.setColor(dark);
                        line(canvas, 15, 27, 26, 31, 28, 40, 19, 46);
                        line(canvas, 51, 27, 40, 30, 38, 40, 46, 49);
                        line(canvas, 28, 40, 38, 40, 32, 54);
                        line(canvas, 26, 31, 27, 23, 23, 17);
                        circle(canvas, 34, 32, 3, CREAM);
                }
            }
        };
    }

    static Drawable stone(int color, float density, boolean interactive) {
        Painted stone = new Painted() {
            @Override void paint(Canvas canvas, Rect bounds) {
                float width = bounds.width(), height = bounds.height();
                fill.setShader(new LinearGradient(0, 0, width, height, blend(color, CREAM, .13f),
                    blend(color, 0xff1a1d1c, .24f), Shader.TileMode.CLAMP));
                canvas.drawRoundRect(0, 0, width, height, 4 * density, 4 * density, fill);
                fill.setShader(null);
                stroke.setStrokeWidth(density);
                stroke.setColor(blend(color, 0xff1a1d1c, .42f));
                canvas.drawRoundRect(density, density, width - density, height - density, 4 * density, 4 * density, stroke);
                for (int i = 0; i < 19; i++) {
                    float x = (.08f + ((i * 37) % 83) / 100f) * width;
                    float y = (.08f + ((i * 51) % 83) / 100f) * height;
                    circle(canvas, x, y, (i % 3 + 1) * density * .45f,
                        i % 2 == 0 ? 0x28403024 : 0x36fff6dd);
                }
                stroke.setColor(0x3c403024);
                line(canvas, width * .06f, height * .25f, width * .18f, height * .32f, width * .22f, height * .45f);
                line(canvas, width * .95f, height * .68f, width * .81f, height * .72f, width * .77f, height * .82f);
            }
        };
        return interactive ? new RippleDrawable(ColorStateList.valueOf(0x407ad4ad), stone, null) : stone;
    }

    static final class Carving extends Painted {
        private final Path groove = new Path();
        private final PathMeasure measure = new PathMeasure();
        private final float[] tip = new float[2];
        private float progress = 1f;

        void setProgress(float progress) {
            this.progress = progress;
            invalidateSelf();
        }

        @Override void paint(Canvas canvas, Rect bounds) {
            canvas.scale(bounds.width() / 48f, bounds.height() / 48f);
            groove.reset();
            groove.moveTo(14, 35);
            groove.lineTo(21, 41);
            groove.lineTo(34, 32);
            measure.setPath(groove, false);
            float strikes = progress * 5;
            float engraved = Math.min(1f, (float) Math.ceil(strikes) / 5f);
            float length = measure.getLength() * engraved;
            path.reset();
            measure.getSegment(0, length, path, true);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            stroke.setStrokeWidth(3.8f);
            stroke.setColor(0xb9fff2cd);
            canvas.save();
            canvas.translate(0, 1.1f);
            canvas.drawPath(path, stroke);
            canvas.restore();
            stroke.setStrokeWidth(2.9f);
            stroke.setColor(0xff30413a);
            canvas.drawPath(path, stroke);
            if (progress > 0f && progress < 1f && measure.getPosTan(length, tip, null)) {
                float burst = strikes - (int) strikes;
                float lift = 3f * burst;
                stroke.setStrokeWidth(1.8f);
                stroke.setColor(0xffe1cda7);
                canvas.drawLine(tip[0] + lift, tip[1] - lift, tip[0] + 6 + lift, tip[1] - 9 - lift, stroke);
                for (int i = 0; i < 5; i++) {
                    float x = tip[0] + (i - 2) * 5 * burst;
                    float y = tip[1] - (5 + i % 3 * 4) * burst;
                    circle(canvas, x, y, 1.8f * (1f - burst), i % 2 == 0 ? CREAM : 0xff967552);
                }
            }
        }
    }

    private static int blend(int base, int other, float amount) {
        int red = Math.round(((base >> 16) & 255) * (1 - amount) + ((other >> 16) & 255) * amount);
        int green = Math.round(((base >> 8) & 255) * (1 - amount) + ((other >> 8) & 255) * amount);
        int blue = Math.round((base & 255) * (1 - amount) + (other & 255) * amount);
        return 0xff000000 | red << 16 | green << 8 | blue;
    }

    private abstract static class Painted extends Drawable {
        final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path path = new Path();
        private int alpha = 255;

        Painted() {
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override public final void draw(Canvas canvas) {
            Rect bounds = getBounds();
            if (bounds.isEmpty() || alpha == 0) return;
            int layer = alpha == 255 ? canvas.save()
                : canvas.saveLayerAlpha(bounds.left, bounds.top, bounds.right, bounds.bottom, alpha);
            canvas.translate(bounds.left, bounds.top);
            paint(canvas, bounds);
            canvas.restoreToCount(layer);
        }

        abstract void paint(Canvas canvas, Rect bounds);

        void line(Canvas canvas, float... points) {
            shape(points, false);
            canvas.drawPath(path, stroke);
        }

        void polygon(Canvas canvas, int color, float... points) {
            shape(points, true);
            fill.setColor(color);
            canvas.drawPath(path, fill);
        }

        void circle(Canvas canvas, float x, float y, float radius, int color) {
            fill.setColor(color);
            canvas.drawCircle(x, y, radius, fill);
        }

        void tree(Canvas canvas, float x, float y, float size, int color) {
            polygon(canvas, 0xff72553d, x - 1.7f, y, x - 1.7f, y - size, x + 1.7f, y - size, x + 1.7f, y);
            polygon(canvas, color, x, y - size * 1.7f, x + size * .55f, y - size * .88f, x + size * .35f, y - size * .88f,
                x + size * .66f, y - size * .48f, x - size * .66f, y - size * .48f,
                x - size * .35f, y - size * .88f, x - size * .55f, y - size * .88f);
        }

        void stepped(Canvas canvas, float x, float y, float unit, boolean right) {
            float dx = right ? unit : -unit;
            line(canvas, x, y, x + dx, y, x + dx, y + unit, x + 2 * dx, y + unit,
                x + 2 * dx, y + 2 * unit, x + dx, y + 2 * unit, x + dx, y + 3 * unit, x, y + 3 * unit);
        }

        private void shape(float[] points, boolean closed) {
            path.reset();
            path.moveTo(points[0], points[1]);
            for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
            if (closed) path.close();
        }

        @Override public void setAlpha(int alpha) { this.alpha = alpha; invalidateSelf(); }
        @Override public int getAlpha() { return alpha; }
        @Override public void setColorFilter(ColorFilter filter) { fill.setColorFilter(filter); stroke.setColorFilter(filter); invalidateSelf(); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
