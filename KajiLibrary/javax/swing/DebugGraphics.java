package javax.swing;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.ImageObserver;
import java.io.PrintStream;
import java.text.AttributedCharacterIterator;

/**
 * A {@link Graphics} that also tells what is drawn.
 *
 * <h2>What it is for</h2>
 *
 * <p>For finding out why a component looks wrong. It wraps the real {@code Graphics} and,
 * according to how it is configured, leaves a record of each operation: with
 * {@link #LOG_OPTION} it writes a line per call, with {@link #FLASH_OPTION} it draws each thing
 * several times in a striking colour so that it can be seen <em>when</em> it appears, and with
 * {@link #BUFFERED_OPTION} it shows the drawing in a separate window.
 *
 * <p>It is Swing's answer to a question that has no other way of being answered: who painted
 * this pixel.
 *
 * <h2>With no screen, the flash does not flash</h2>
 *
 * <p>{@link #LOG_OPTION} works all the same -- it writes to a {@link PrintStream} --, and it is
 * the only one of the three that serves with no screen. The other two keep and return their
 * configuration, and the part that would need to draw on a visible window does nothing:
 * flashing with nobody looking is not drawing badly, it is having nobody to show it to.
 *
 * <p>The wrapped {@code Graphics} receives every operation as it is and in the order they
 * arrive, so the drawn result is identical with or without this class.
 */
public class DebugGraphics extends Graphics {

    /** The real one; every operation ends up there. */
    Graphics graphics;

    Image buffer;
    int debugOptions;
    int graphicsID = 0;
    int xOffset = 0;
    int yOffset = 0;

    /** It writes a line per operation. */
    public static final int LOG_OPTION = 1 << 0;

    /** It draws each thing several times, in a striking colour. */
    public static final int FLASH_OPTION = 1 << 1;

    /** It shows the drawing in a separate window. */
    public static final int BUFFERED_OPTION = 1 << 2;

    /** None; it switches the three off. */
    public static final int NONE_OPTION = -1;

    private static Color flashColor = Color.red;
    private static int flashTime = 100;
    private static int flashCount = 2;
    private static PrintStream logStream = System.out;
    private static int counter = 0;

    /** An empty one, with nothing wrapped; it has to be given the {@code Graphics} afterwards. */
    public DebugGraphics() {
        super();
        buffer = null;
        xOffset = 0;
        yOffset = 0;
    }

    /** It wraps that {@code Graphics}, taking the options from that component. */
    public DebugGraphics(Graphics graphics, JComponent component) {
        this(graphics);
        setDebugOptions(component.getDebugGraphicsOptions());
    }

    /** It wraps that {@code Graphics}. */
    public DebugGraphics(Graphics graphics) {
        this();
        this.graphics = graphics;
        graphicsID = nextId();
    }

    private static synchronized int nextId() {
        counter = counter + 1;
        return counter;
    }

    /** Another one the same, over a copy of the wrapped one. */
    public Graphics create() {
        DebugGraphics debugGraphics = new DebugGraphics(graphics.create());
        debugGraphics.debugOptions = debugOptions;
        debugGraphics.xOffset = xOffset;
        debugGraphics.yOffset = yOffset;
        return debugGraphics;
    }

    /** Another one the same, clipped to that rectangle. */
    public Graphics create(int x, int y, int width, int height) {
        DebugGraphics debugGraphics = new DebugGraphics(
                graphics.create(x, y, width, height));
        debugGraphics.debugOptions = debugOptions;
        debugGraphics.xOffset = xOffset + x;
        debugGraphics.yOffset = yOffset + y;
        return debugGraphics;
    }

    /** The colour it flashes with; see the class note. */
    public static void setFlashColor(Color flashColor) {
        DebugGraphics.flashColor = flashColor;
    }

    public static Color flashColor() {
        return flashColor;
    }

    /** How long each flash lasts, in milliseconds. */
    public static void setFlashTime(int flashTime) {
        DebugGraphics.flashTime = flashTime;
    }

    public static int flashTime() {
        return flashTime;
    }

    /** How many times each operation flashes. */
    public static void setFlashCount(int flashCount) {
        DebugGraphics.flashCount = flashCount;
    }

    public static int flashCount() {
        return flashCount;
    }

    /** Where the log goes; null switches it off. */
    public static void setLogStream(PrintStream stream) {
        DebugGraphics.logStream = stream;
    }

    public static PrintStream logStream() {
        return logStream;
    }

    /** It writes a line if the log is switched on. */
    private void record(String text) {
        if (debugLog()) {
            PrintStream s = logStream();
            if (s != null) {
                s.println(toShortString() + " " + text);
            }
        }
    }

    public void setFont(Font aFont) {
        record("Setting font: " + aFont);
        graphics.setFont(aFont);
    }

    public Font getFont() {
        return graphics.getFont();
    }

    public void setColor(Color aColor) {
        record("Setting color: " + aColor);
        graphics.setColor(aColor);
    }

    public Color getColor() {
        return graphics.getColor();
    }

    public FontMetrics getFontMetrics() {
        return graphics.getFontMetrics();
    }

    public FontMetrics getFontMetrics(Font f) {
        return graphics.getFontMetrics(f);
    }

    /**
     * It shifts the origin; it also shifts the one this class notes, so that the numbers add up.
     */
    public void translate(int x, int y) {
        record("Translating by: " + new java.awt.Point(x, y));
        xOffset = xOffset + x;
        yOffset = yOffset + y;
        graphics.translate(x, y);
    }

    public void setPaintMode() {
        record("Setting paint mode");
        graphics.setPaintMode();
    }

    public void setXORMode(Color aColor) {
        record("Setting XOR mode: " + aColor);
        graphics.setXORMode(aColor);
    }

    public Rectangle getClipBounds() {
        return graphics.getClipBounds();
    }

    public void clipRect(int x, int y, int width, int height) {
        graphics.clipRect(x, y, width, height);
        record("Setting clipRect: " + new Rectangle(x, y, width, height)
                + " New clipRect: " + graphics.getClip());
    }

    public void setClip(int x, int y, int width, int height) {
        graphics.setClip(x, y, width, height);
        record("Setting new clipRect: " + graphics.getClip());
    }

    public Shape getClip() {
        return graphics.getClip();
    }

    public void setClip(Shape clip) {
        graphics.setClip(clip);
        record("Setting new clipRect: " + graphics.getClip());
    }

    public void drawRect(int x, int y, int width, int height) {
        record("Drawing rect: " + new Rectangle(x, y, width, height));
        graphics.drawRect(x, y, width, height);
    }

    public void fillRect(int x, int y, int width, int height) {
        record("Filling rect: " + new Rectangle(x, y, width, height));
        graphics.fillRect(x, y, width, height);
    }

    public void clearRect(int x, int y, int width, int height) {
        record("Clearing rect: " + new Rectangle(x, y, width, height));
        graphics.clearRect(x, y, width, height);
    }

    public void drawRoundRect(int x, int y, int width, int height, int arcWidth,
            int arcHeight) {
        record("Drawing round rect: " + new Rectangle(x, y, width, height)
                + " arcWidth: " + arcWidth + " archHeight: " + arcHeight);
        graphics.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    public void fillRoundRect(int x, int y, int width, int height, int arcWidth,
            int arcHeight) {
        record("Filling round rect: " + new Rectangle(x, y, width, height)
                + " arcWidth: " + arcWidth + " archHeight: " + arcHeight);
        graphics.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        record("Drawing line: from " + pointToString(x1, y1) + " to "
                + pointToString(x2, y2));
        graphics.drawLine(x1, y1, x2, y2);
    }

    public void draw3DRect(int x, int y, int width, int height, boolean raised) {
        record("Drawing 3D rect: " + new Rectangle(x, y, width, height)
                + " Raised bezel: " + raised);
        graphics.draw3DRect(x, y, width, height, raised);
    }

    public void fill3DRect(int x, int y, int width, int height, boolean raised) {
        record("Filling 3D rect: " + new Rectangle(x, y, width, height)
                + " Raised bezel: " + raised);
        graphics.fill3DRect(x, y, width, height, raised);
    }

    public void drawOval(int x, int y, int width, int height) {
        record("Drawing oval: " + new Rectangle(x, y, width, height));
        graphics.drawOval(x, y, width, height);
    }

    public void fillOval(int x, int y, int width, int height) {
        record("Filling oval: " + new Rectangle(x, y, width, height));
        graphics.fillOval(x, y, width, height);
    }

    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        record("Drawing arc: " + new Rectangle(x, y, width, height)
                + " startAngle: " + startAngle + " arcAngle: " + arcAngle);
        graphics.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        record("Filling arc: " + new Rectangle(x, y, width, height)
                + " startAngle: " + startAngle + " arcAngle: " + arcAngle);
        graphics.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        record("Drawing polyline: nPoints: " + nPoints);
        graphics.drawPolyline(xPoints, yPoints, nPoints);
    }

    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        record("Drawing polygon: nPoints: " + nPoints);
        graphics.drawPolygon(xPoints, yPoints, nPoints);
    }

    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        record("Filling polygon: nPoints: " + nPoints);
        graphics.fillPolygon(xPoints, yPoints, nPoints);
    }

    public void drawString(String aString, int x, int y) {
        record("Drawing string: \"" + aString + "\" at: " + new java.awt.Point(x, y));
        graphics.drawString(aString, x, y);
    }

    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        record("Drawing text: \"" + iterator + "\" at: " + new java.awt.Point(x, y));
        graphics.drawString(iterator, x, y);
    }

    public void drawBytes(byte[] data, int offset, int length, int x, int y) {
        record("Drawing bytes at: " + new java.awt.Point(x, y));
        graphics.drawBytes(data, offset, length, x, y);
    }

    public void drawChars(char[] data, int offset, int length, int x, int y) {
        record("Drawing chars at " + new java.awt.Point(x, y));
        graphics.drawChars(data, offset, length, x, y);
    }

    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        record("Drawing image: " + img + " at: " + new java.awt.Point(x, y));
        return graphics.drawImage(img, x, y, observer);
    }

    public boolean drawImage(Image img, int x, int y, int width, int height,
            ImageObserver observer) {
        record("Drawing image: " + img + " at: " + new Rectangle(x, y, width, height));
        return graphics.drawImage(img, x, y, width, height, observer);
    }

    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        record("Drawing image: " + img + " at: " + new java.awt.Point(x, y)
                + ", bgcolor: " + bgcolor);
        return graphics.drawImage(img, x, y, bgcolor, observer);
    }

    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor,
            ImageObserver observer) {
        record("Drawing image: " + img + " at: " + new Rectangle(x, y, width, height)
                + ", bgcolor: " + bgcolor);
        return graphics.drawImage(img, x, y, width, height, bgcolor, observer);
    }

    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1,
            int sx2, int sy2, ImageObserver observer) {
        record("Drawing image: " + img + " destination: "
                + new Rectangle(dx1, dy1, dx2 - dx1, dy2 - dy1)
                + " source: " + new Rectangle(sx1, sy1, sx2 - sx1, sy2 - sy1));
        return graphics.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
    }

    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1,
            int sx2, int sy2, Color bgcolor, ImageObserver observer) {
        record("Drawing image: " + img + " destination: "
                + new Rectangle(dx1, dy1, dx2 - dx1, dy2 - dy1)
                + " source: " + new Rectangle(sx1, sy1, sx2 - sx1, sy2 - sy1)
                + ", bgcolor: " + bgcolor);
        return graphics.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor,
                observer);
    }

    public void copyArea(int x, int y, int width, int height, int destX, int destY) {
        record("Copying area from: " + new Rectangle(x, y, width, height)
                + " to: " + new java.awt.Point(destX, destY));
        graphics.copyArea(x, y, width, height, destX, destY);
    }

    /** It waits; it is what makes the pause between flashes. */
    final void sleep(int mSecs) {
        try {
            Thread.sleep(mSecs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void dispose() {
        graphics.dispose();
        graphics = null;
    }

    /** Whether it is drawing on a separate window; see {@link #BUFFERED_OPTION}. */
    public boolean isDrawingBuffer() {
        return buffer != null;
    }

    /** How it identifies itself in the log: a number and the origin. */
    String toShortString() {
        return "Graphics" + (isDrawingBuffer() ? "<B>" : "") + "(" + graphicsID + "-"
                + (1 + (xOffset / 10)) + ")";
    }

    String pointToString(int x, int y) {
        return "(" + x + ", " + y + ")";
    }

    /**
     * What is recorded.
     *
     * <p>{@link #NONE_OPTION} switches everything off; the other values are added to what was
     * already there, they do not replace it. It is what allows the log to be switched on without
     * switching the flash off.
     */
    public void setDebugOptions(int options) {
        if (options != 0) {
            if (options == NONE_OPTION) {
                if (debugOptions != 0) {
                    record("Disabling debug");
                    debugOptions = 0;
                }
            } else {
                if (debugOptions != options) {
                    debugOptions = debugOptions | options;
                    if (debugLog()) {
                        record("Enabling debug");
                    }
                }
            }
        }
    }

    public int getDebugOptions() {
        return debugOptions;
    }

    boolean debugLog() {
        return (debugOptions & LOG_OPTION) == LOG_OPTION;
    }

    boolean debugFlash() {
        return (debugOptions & FLASH_OPTION) == FLASH_OPTION;
    }

    boolean debugBuffered() {
        return (debugOptions & BUFFERED_OPTION) == BUFFERED_OPTION;
    }
}
