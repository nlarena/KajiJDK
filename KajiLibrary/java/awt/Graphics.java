package java.awt;

import java.awt.image.ImageObserver;
import java.text.AttributedCharacterIterator;

/**
 * Una superficie sobre la que se dibuja, con su estado.
 *
 * <p>No es sólo un destino: es un destino **más** el color, la fuente, el recorte y el
 * desplazamiento del origen. Por eso {@link #create()} existe y se usa tanto — devuelve otra vista
 * del mismo destino con una copia del estado, de modo que un componente puede cambiar el color y el
 * recorte a gusto sin ensuciarle nada a quien lo llamó.
 *
 * <p>El recorte es **acumulativo**: {@link #clipRect} lo interseca con el que ya había y nunca lo
 * agranda. Es lo que hace que un hijo no pueda pintar fuera de su padre por más que lo intente, y
 * por eso hay que quedarse con el que se recibe en vez de fijar el propio.
 *
 * <p>Todo dibujo pasa por la línea de base del "modo": en modo pintura el color reemplaza al que
 * había, y en modo XOR se combina con él, de manera que dibujar dos veces lo mismo deja la
 * superficie como estaba. Eso último es lo que permitía dibujar un cursor o una selección elástica
 * sin guardar el fondo.
 *
 * <p>Las coordenadas son enteras y los bordes se pintan: {@link #drawRect} de 3×3 marca un cuadrado
 * de 4×4 píxeles, porque dibuja la línea que **rodea** al rectángulo. {@link #fillRect} de 3×3
 * pinta 9. La diferencia es vieja y sorprende siempre.
 */
public abstract class Graphics {

    /** Para las subclases. */
    protected Graphics() {
    }

    /** Otra vista del mismo destino, con una copia de este estado. */
    public abstract Graphics create();

    /**
     * Otra vista con el origen corrido y el recorte reducido a ese rectángulo.
     *
     * <p>El rectángulo se da en las coordenadas de **este** contexto, y en el que sale su ángulo
     * superior izquierdo pasa a ser el origen.
     */
    public Graphics create(int x, int y, int width, int height) {
        Graphics g = this.create();
        if (g == null) {
            return null;
        }
        g.translate(x, y);
        g.clipRect(0, 0, width, height);
        return g;
    }

    /** Corre el origen. */
    public abstract void translate(int x, int y);

    /** El color con el que se dibuja. */
    public abstract Color getColor();

    /** Cambia el color con el que se dibuja. */
    public abstract void setColor(Color c);

    /** Pone el modo en el que el color reemplaza a lo que había. */
    public abstract void setPaintMode();

    /**
     * Pone el modo en el que el color se combina con lo que había.
     *
     * <p>Un píxel del color actual pasa al de alternancia y viceversa; el resto cambia de una manera
     * que se deshace al repetir el dibujo. De ahí que sirva para lo que hay que borrar después.
     */
    public abstract void setXORMode(Color c1);

    /** La fuente con la que se dibuja el texto. */
    public abstract Font getFont();

    /** Cambia la fuente con la que se dibuja el texto. */
    public abstract void setFont(Font font);

    /** Las medidas de la fuente actual. */
    public FontMetrics getFontMetrics() {
        return this.getFontMetrics(this.getFont());
    }

    /** Las medidas de esa fuente en este destino. */
    public abstract FontMetrics getFontMetrics(Font f);

    /** El rectángulo que encierra al recorte, o `null` si no hay recorte. */
    public abstract Rectangle getClipBounds();

    /** Reduce el recorte a la intersección con ese rectángulo. */
    public abstract void clipRect(int x, int y, int width, int height);

    /**
     * Fija el recorte a ese rectángulo.
     *
     * <p>A diferencia de {@link #clipRect}, esto **puede agrandar** el recorte, así que usarlo sobre
     * un contexto prestado le permite a un componente pintar afuera de lo suyo.
     */
    public abstract void setClip(int x, int y, int width, int height);

    /** El recorte, o `null` si no hay. */
    public abstract Shape getClip();

    /** Fija el recorte a esa figura. */
    public abstract void setClip(Shape clip);

    /**
     * Copia un rectángulo del destino a otro lugar del mismo destino.
     *
     * <p>Lo que se copie desde fuera del recorte, o desde una parte que estuviera tapada, queda sin
     * definir: no hay de dónde sacar esos píxeles.
     */
    public abstract void copyArea(int x, int y, int width, int height, int dx, int dy);

    /** Una línea entre dos puntos, con los dos extremos incluidos. */
    public abstract void drawLine(int x1, int y1, int x2, int y2);

    /** Rellena un rectángulo con el color actual. */
    public abstract void fillRect(int x, int y, int width, int height);

    /**
     * El contorno de un rectángulo.
     *
     * <p>Cubre `width + 1` por `height + 1` píxeles: la línea rodea al rectángulo.
     */
    public void drawRect(int x, int y, int width, int height) {
        if (width < 0 || height < 0) {
            return;
        }
        if (height == 0 || width == 0) {
            this.drawLine(x, y, x + width, y + height);
        } else {
            this.drawLine(x, y, x + width - 1, y);
            this.drawLine(x + width, y, x + width, y + height - 1);
            this.drawLine(x + width, y + height, x + 1, y + height);
            this.drawLine(x, y + height, x, y + 1);
        }
    }

    /** Rellena un rectángulo con el color de fondo. */
    public abstract void clearRect(int x, int y, int width, int height);

    /** El contorno de un rectángulo con las esquinas redondeadas. */
    public abstract void drawRoundRect(int x, int y, int width, int height, int arcWidth,
            int arcHeight);

    /** Rellena un rectángulo con las esquinas redondeadas. */
    public abstract void fillRoundRect(int x, int y, int width, int height, int arcWidth,
            int arcHeight);

    /**
     * Un rectángulo con relieve.
     *
     * <p>El relieve se hace con dos tonos del color actual: el claro en los lados que dan a la luz y
     * el oscuro en los otros. Invertirlos es lo que hace que se vea hundido en vez de saliente.
     */
    public void draw3DRect(int x, int y, int width, int height, boolean raised) {
        Color c = this.getColor();
        Color brighter = c.brighter();
        Color darker = c.darker();
        this.setColor(raised ? brighter : darker);
        this.drawLine(x, y, x, y + height);
        this.drawLine(x + 1, y, x + width - 1, y);
        this.setColor(raised ? darker : brighter);
        this.drawLine(x + 1, y + height, x + width, y + height);
        this.drawLine(x + width, y, x + width, y + height - 1);
        this.setColor(c);
    }

    /** Un rectángulo relleno con relieve. */
    public void fill3DRect(int x, int y, int width, int height, boolean raised) {
        Color c = this.getColor();
        Color brighter = c.brighter();
        Color darker = c.darker();
        if (!raised) {
            this.setColor(darker);
        }
        this.fillRect(x + 1, y + 1, width - 2, height - 2);
        this.setColor(raised ? brighter : darker);
        this.drawLine(x, y, x, y + height - 1);
        this.drawLine(x + 1, y, x + width - 2, y);
        this.setColor(raised ? darker : brighter);
        this.drawLine(x + 1, y + height - 1, x + width - 1, y + height - 1);
        this.drawLine(x + width - 1, y, x + width - 1, y + height - 2);
        this.setColor(c);
    }

    /** El contorno de un óvalo inscripto en ese rectángulo. */
    public abstract void drawOval(int x, int y, int width, int height);

    /** Rellena un óvalo inscripto en ese rectángulo. */
    public abstract void fillOval(int x, int y, int width, int height);

    /**
     * Un arco de un óvalo.
     *
     * <p>Los ángulos van en grados, con el cero a las tres y creciendo en sentido antihorario.
     */
    public abstract void drawArc(int x, int y, int width, int height, int startAngle,
            int arcAngle);

    /** Rellena un sector de un óvalo. */
    public abstract void fillArc(int x, int y, int width, int height, int startAngle,
            int arcAngle);

    /** Una sucesión de segmentos, sin cerrar. */
    public abstract void drawPolyline(int[] xPoints, int[] yPoints, int nPoints);

    /** El contorno de un polígono, cerrado. */
    public abstract void drawPolygon(int[] xPoints, int[] yPoints, int nPoints);

    /** El contorno de un polígono. */
    public void drawPolygon(Polygon p) {
        this.drawPolygon(p.xpoints, p.ypoints, p.npoints);
    }

    /** Rellena un polígono. */
    public abstract void fillPolygon(int[] xPoints, int[] yPoints, int nPoints);

    /** Rellena un polígono. */
    public void fillPolygon(Polygon p) {
        this.fillPolygon(p.xpoints, p.ypoints, p.npoints);
    }

    /**
     * Dibuja un texto.
     *
     * <p>`(x, y)` es el comienzo de la **línea de base**, no la esquina: el texto sube por encima de
     * ese punto.
     */
    public abstract void drawString(String str, int x, int y);

    /** Dibuja un texto con atributos. */
    public abstract void drawString(AttributedCharacterIterator iterator, int x, int y);

    /** Dibuja un tramo de un arreglo de caracteres. */
    public void drawChars(char[] data, int offset, int length, int x, int y) {
        this.drawString(new String(data, offset, length), x, y);
    }

    /**
     * Dibuja un tramo de bytes, tomando cada uno como un carácter.
     *
     * @deprecated no traduce correctamente los bytes a caracteres en ninguna codificación que no sea
     *     Latin-1. Se mantiene porque está en la API desde 1.0.
     */
    @Deprecated
    public void drawBytes(byte[] data, int offset, int length, int x, int y) {
        this.drawString(new String(data, offset, length), x, y);
    }

    /**
     * Dibuja una imagen con su ángulo superior izquierdo en `(x, y)`.
     *
     * <p>Devuelve `false` si la imagen todavía no está entera; el observador se va a enterar cuando
     * llegue el resto.
     */
    public abstract boolean drawImage(Image img, int x, int y, ImageObserver observer);

    /** Dibuja una imagen escalada a ese tamaño. */
    public abstract boolean drawImage(Image img, int x, int y, int width, int height,
            ImageObserver observer);

    /** Dibuja una imagen pintando de `bgcolor` lo que sea transparente. */
    public abstract boolean drawImage(Image img, int x, int y, Color bgcolor,
            ImageObserver observer);

    /** Dibuja una imagen escalada, pintando de `bgcolor` lo que sea transparente. */
    public abstract boolean drawImage(Image img, int x, int y, int width, int height,
            Color bgcolor, ImageObserver observer);

    /**
     * Dibuja un recorte de una imagen dentro de un rectángulo del destino.
     *
     * <p>Si los rectángulos no miden lo mismo, la imagen se estira; si un par de coordenadas está
     * dado al revés, se refleja. Ese reflejo es a propósito y es la única manera de espejar una
     * imagen con esta API.
     */
    public abstract boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1,
            int sy1, int sx2, int sy2, ImageObserver observer);

    /** Como el anterior, pintando de `bgcolor` lo que sea transparente. */
    public abstract boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1,
            int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer);

    /**
     * Suelta los recursos de este contexto.
     *
     * <p>Hay que llamarlo para todo contexto que se haya pedido con {@link #create()}. Usarlo
     * después es un error.
     */
    public abstract void dispose();

    /**
     * Suelta los recursos.
     *
     * @deprecated depende de la recolección de basura, que no da ninguna garantía de cuándo va a
     *     correr ni de que vaya a correr. Hay que llamar a {@link #dispose} a mano.
     */
    @Deprecated
    public void finalize() {
        this.dispose();
    }

    public String toString() {
        return this.getClass().getName() + "[font=" + this.getFont() + ",color="
                + this.getColor() + "]";
    }

    /**
     * El rectángulo del recorte.
     *
     * @deprecated el nombre no dice que devuelve el rectángulo que **encierra** al recorte, que
     *     puede no ser un rectángulo. Usar {@link #getClipBounds}.
     */
    @Deprecated
    public Rectangle getClipRect() {
        return this.getClipBounds();
    }

    /**
     * Si ese rectángulo toca el recorte.
     *
     * <p>Puede dar `true` de más pero nunca `false` de menos: sirve para saltearse un dibujo que
     * seguro no se ve, no para saber si se ve.
     */
    public boolean hitClip(int x, int y, int width, int height) {
        Rectangle clipRect = this.getClipBounds();
        if (clipRect == null) {
            return true;
        }
        return clipRect.intersects(x, y, width, height);
    }

    /**
     * El rectángulo del recorte, escrito en el que se pasa.
     *
     * <p>Existe para no crear un objeto por consulta en un bucle de dibujado.
     */
    public Rectangle getClipBounds(Rectangle r) {
        Rectangle clipRect = this.getClipBounds();
        if (clipRect != null) {
            r.x = clipRect.x;
            r.y = clipRect.y;
            r.width = clipRect.width;
            r.height = clipRect.height;
        } else if (r == null) {
            throw new NullPointerException("null rectangle parameter");
        }
        return r;
    }
}
