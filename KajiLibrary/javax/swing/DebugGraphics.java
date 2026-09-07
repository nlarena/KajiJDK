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
 * Un {@link Graphics} que ademas cuenta lo que se dibuja.
 *
 * <h2>Para que sirve</h2>
 *
 * <p>Para averiguar por que un componente se ve mal. Envuelve al {@code Graphics} de verdad y, segun
 * como este configurado, deja constancia de cada operacion: con {@link #LOG_OPTION} escribe una
 * linea por llamada, con {@link #FLASH_OPTION} dibuja cada cosa varias veces en un color llamativo
 * para que se vea <em>cuando</em> aparece, y con {@link #BUFFERED_OPTION} muestra el dibujado en una
 * ventana aparte.
 *
 * <p>Es la respuesta de Swing a una pregunta que no tiene otra forma de contestarse: quien pinto
 * este pixel.
 *
 * <h2>Sin pantalla, el destello no destella</h2>
 *
 * <p>{@link #LOG_OPTION} anda igual -- escribe a un {@link PrintStream} --, y es la unica de las tres
 * que sirve sin pantalla. Las otras dos guardan y devuelven su configuracion, y la parte que
 * necesitaria dibujar sobre una ventana visible no hace nada: destellar sin que nadie mire no es
 * dibujar mal, es no tener a quien mostrarle.
 *
 * <p>El {@code Graphics} envuelto recibe todas las operaciones tal cual y en el orden en que
 * llegan, asi que el resultado dibujado es identico con o sin esta clase.
 */
public class DebugGraphics extends Graphics {

    /** El de verdad; todas las operaciones terminan ahi. */
    Graphics graphics;

    Image buffer;
    int debugOptions;
    int graphicsID = 0;
    int xOffset = 0;
    int yOffset = 0;

    /** Escribe una linea por operacion. */
    public static final int LOG_OPTION = 1 << 0;

    /** Dibuja cada cosa varias veces, en un color llamativo. */
    public static final int FLASH_OPTION = 1 << 1;

    /** Muestra el dibujado en una ventana aparte. */
    public static final int BUFFERED_OPTION = 1 << 2;

    /** Ninguna; apaga las tres. */
    public static final int NONE_OPTION = -1;

    private static Color flashColor = Color.red;
    private static int flashTime = 100;
    private static int flashCount = 2;
    private static PrintStream logStream = System.out;
    private static int contador = 0;

    /** Uno vacio, sin nada envuelto; hay que darle el {@code Graphics} despues. */
    public DebugGraphics() {
        super();
        buffer = null;
        xOffset = 0;
        yOffset = 0;
    }

    /** Envuelve ese {@code Graphics}, tomando las opciones de ese componente. */
    public DebugGraphics(Graphics graphics, JComponent component) {
        this(graphics);
        setDebugOptions(component.getDebugGraphicsOptions());
    }

    /** Envuelve ese {@code Graphics}. */
    public DebugGraphics(Graphics graphics) {
        this();
        this.graphics = graphics;
        graphicsID = proximoId();
    }

    private static synchronized int proximoId() {
        contador = contador + 1;
        return contador;
    }

    /** Otro igual, sobre una copia del envuelto. */
    public Graphics create() {
        DebugGraphics debugGraphics = new DebugGraphics(graphics.create());
        debugGraphics.debugOptions = debugOptions;
        debugGraphics.xOffset = xOffset;
        debugGraphics.yOffset = yOffset;
        return debugGraphics;
    }

    /** Otro igual, recortado a ese rectangulo. */
    public Graphics create(int x, int y, int width, int height) {
        DebugGraphics debugGraphics = new DebugGraphics(
                graphics.create(x, y, width, height));
        debugGraphics.debugOptions = debugOptions;
        debugGraphics.xOffset = xOffset + x;
        debugGraphics.yOffset = yOffset + y;
        return debugGraphics;
    }

    /** El color con el que se destella; ver la nota de la clase. */
    public static void setFlashColor(Color flashColor) {
        DebugGraphics.flashColor = flashColor;
    }

    public static Color flashColor() {
        return flashColor;
    }

    /** Cuanto dura cada destello, en milisegundos. */
    public static void setFlashTime(int flashTime) {
        DebugGraphics.flashTime = flashTime;
    }

    public static int flashTime() {
        return flashTime;
    }

    /** Cuantas veces destella cada operacion. */
    public static void setFlashCount(int flashCount) {
        DebugGraphics.flashCount = flashCount;
    }

    public static int flashCount() {
        return flashCount;
    }

    /** A donde va el registro; nulo lo apaga. */
    public static void setLogStream(PrintStream stream) {
        DebugGraphics.logStream = stream;
    }

    public static PrintStream logStream() {
        return logStream;
    }

    /** Escribe una linea si el registro esta prendido. */
    private void anotar(String texto) {
        if (debugLog()) {
            PrintStream s = logStream();
            if (s != null) {
                s.println(toShortString() + " " + texto);
            }
        }
    }

    public void setFont(Font aFont) {
        anotar("Setting font: " + aFont);
        graphics.setFont(aFont);
    }

    public Font getFont() {
        return graphics.getFont();
    }

    public void setColor(Color aColor) {
        anotar("Setting color: " + aColor);
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

    /** Corre el origen; tambien corre el que esta clase anota, para que los numeros cierren. */
    public void translate(int x, int y) {
        anotar("Translating by: " + new java.awt.Point(x, y));
        xOffset = xOffset + x;
        yOffset = yOffset + y;
        graphics.translate(x, y);
    }

    public void setPaintMode() {
        anotar("Setting paint mode");
        graphics.setPaintMode();
    }

    public void setXORMode(Color aColor) {
        anotar("Setting XOR mode: " + aColor);
        graphics.setXORMode(aColor);
    }

    public Rectangle getClipBounds() {
        return graphics.getClipBounds();
    }

    public void clipRect(int x, int y, int width, int height) {
        graphics.clipRect(x, y, width, height);
        anotar("Setting clipRect: " + new Rectangle(x, y, width, height)
                + " New clipRect: " + graphics.getClip());
    }

    public void setClip(int x, int y, int width, int height) {
        graphics.setClip(x, y, width, height);
        anotar("Setting new clipRect: " + graphics.getClip());
    }

    public Shape getClip() {
        return graphics.getClip();
    }

    public void setClip(Shape clip) {
        graphics.setClip(clip);
        anotar("Setting new clipRect: " + graphics.getClip());
    }

    public void drawRect(int x, int y, int width, int height) {
        anotar("Drawing rect: " + new Rectangle(x, y, width, height));
        graphics.drawRect(x, y, width, height);
    }

    public void fillRect(int x, int y, int width, int height) {
        anotar("Filling rect: " + new Rectangle(x, y, width, height));
        graphics.fillRect(x, y, width, height);
    }

    public void clearRect(int x, int y, int width, int height) {
        anotar("Clearing rect: " + new Rectangle(x, y, width, height));
        graphics.clearRect(x, y, width, height);
    }

    public void drawRoundRect(int x, int y, int width, int height, int arcWidth,
            int arcHeight) {
        anotar("Drawing round rect: " + new Rectangle(x, y, width, height)
                + " arcWidth: " + arcWidth + " archHeight: " + arcHeight);
        graphics.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    public void fillRoundRect(int x, int y, int width, int height, int arcWidth,
            int arcHeight) {
        anotar("Filling round rect: " + new Rectangle(x, y, width, height)
                + " arcWidth: " + arcWidth + " archHeight: " + arcHeight);
        graphics.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        anotar("Drawing line: from " + pointToString(x1, y1) + " to "
                + pointToString(x2, y2));
        graphics.drawLine(x1, y1, x2, y2);
    }

    public void draw3DRect(int x, int y, int width, int height, boolean raised) {
        anotar("Drawing 3D rect: " + new Rectangle(x, y, width, height)
                + " Raised bezel: " + raised);
        graphics.draw3DRect(x, y, width, height, raised);
    }

    public void fill3DRect(int x, int y, int width, int height, boolean raised) {
        anotar("Filling 3D rect: " + new Rectangle(x, y, width, height)
                + " Raised bezel: " + raised);
        graphics.fill3DRect(x, y, width, height, raised);
    }

    public void drawOval(int x, int y, int width, int height) {
        anotar("Drawing oval: " + new Rectangle(x, y, width, height));
        graphics.drawOval(x, y, width, height);
    }

    public void fillOval(int x, int y, int width, int height) {
        anotar("Filling oval: " + new Rectangle(x, y, width, height));
        graphics.fillOval(x, y, width, height);
    }

    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        anotar("Drawing arc: " + new Rectangle(x, y, width, height)
                + " startAngle: " + startAngle + " arcAngle: " + arcAngle);
        graphics.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        anotar("Filling arc: " + new Rectangle(x, y, width, height)
                + " startAngle: " + startAngle + " arcAngle: " + arcAngle);
        graphics.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        anotar("Drawing polyline: nPoints: " + nPoints);
        graphics.drawPolyline(xPoints, yPoints, nPoints);
    }

    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        anotar("Drawing polygon: nPoints: " + nPoints);
        graphics.drawPolygon(xPoints, yPoints, nPoints);
    }

    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        anotar("Filling polygon: nPoints: " + nPoints);
        graphics.fillPolygon(xPoints, yPoints, nPoints);
    }

    public void drawString(String aString, int x, int y) {
        anotar("Drawing string: \"" + aString + "\" at: " + new java.awt.Point(x, y));
        graphics.drawString(aString, x, y);
    }

    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        anotar("Drawing text: \"" + iterator + "\" at: " + new java.awt.Point(x, y));
        graphics.drawString(iterator, x, y);
    }

    public void drawBytes(byte[] data, int offset, int length, int x, int y) {
        anotar("Drawing bytes at: " + new java.awt.Point(x, y));
        graphics.drawBytes(data, offset, length, x, y);
    }

    public void drawChars(char[] data, int offset, int length, int x, int y) {
        anotar("Drawing chars at " + new java.awt.Point(x, y));
        graphics.drawChars(data, offset, length, x, y);
    }

    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        anotar("Drawing image: " + img + " at: " + new java.awt.Point(x, y));
        return graphics.drawImage(img, x, y, observer);
    }

    public boolean drawImage(Image img, int x, int y, int width, int height,
            ImageObserver observer) {
        anotar("Drawing image: " + img + " at: " + new Rectangle(x, y, width, height));
        return graphics.drawImage(img, x, y, width, height, observer);
    }

    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        anotar("Drawing image: " + img + " at: " + new java.awt.Point(x, y)
                + ", bgcolor: " + bgcolor);
        return graphics.drawImage(img, x, y, bgcolor, observer);
    }

    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor,
            ImageObserver observer) {
        anotar("Drawing image: " + img + " at: " + new Rectangle(x, y, width, height)
                + ", bgcolor: " + bgcolor);
        return graphics.drawImage(img, x, y, width, height, bgcolor, observer);
    }

    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1,
            int sx2, int sy2, ImageObserver observer) {
        anotar("Drawing image: " + img + " destination: "
                + new Rectangle(dx1, dy1, dx2 - dx1, dy2 - dy1)
                + " source: " + new Rectangle(sx1, sy1, sx2 - sx1, sy2 - sy1));
        return graphics.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
    }

    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1,
            int sx2, int sy2, Color bgcolor, ImageObserver observer) {
        anotar("Drawing image: " + img + " destination: "
                + new Rectangle(dx1, dy1, dx2 - dx1, dy2 - dy1)
                + " source: " + new Rectangle(sx1, sy1, sx2 - sx1, sy2 - sy1)
                + ", bgcolor: " + bgcolor);
        return graphics.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor,
                observer);
    }

    public void copyArea(int x, int y, int width, int height, int destX, int destY) {
        anotar("Copying area from: " + new Rectangle(x, y, width, height)
                + " to: " + new java.awt.Point(destX, destY));
        graphics.copyArea(x, y, width, height, destX, destY);
    }

    /** Espera; es lo que hace la pausa entre destellos. */
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

    /** Si esta dibujando sobre una ventana aparte; ver {@link #BUFFERED_OPTION}. */
    public boolean isDrawingBuffer() {
        return buffer != null;
    }

    /** Como se identifica en el registro: un numero y el origen. */
    String toShortString() {
        return "Graphics" + (isDrawingBuffer() ? "<B>" : "") + "(" + graphicsID + "-"
                + (1 + (xOffset / 10)) + ")";
    }

    String pointToString(int x, int y) {
        return "(" + x + ", " + y + ")";
    }

    /**
     * Que se registra.
     *
     * <p>{@link #NONE_OPTION} apaga todo; los demas valores se suman a lo que ya estaba, no lo
     * reemplazan. Es lo que permite prender el registro sin apagar el destello.
     */
    public void setDebugOptions(int options) {
        if (options != 0) {
            if (options == NONE_OPTION) {
                if (debugOptions != 0) {
                    anotar("Disabling debug");
                    debugOptions = 0;
                }
            } else {
                if (debugOptions != options) {
                    debugOptions = debugOptions | options;
                    if (debugLog()) {
                        anotar("Enabling debug");
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
