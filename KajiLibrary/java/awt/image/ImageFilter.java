package java.awt.image;

import java.util.Hashtable;

/**
 * Un consumidor que le pasa los píxeles a otro consumidor, cambiándolos en el camino.
 *
 * <p>Es a la vez el extremo receptor de una tubería y el emisor de la siguiente, y por eso los
 * filtros se encadenan. Tal como está no cambia nada: todos sus métodos reenvían. Sirve como base y
 * como filtro nulo.
 *
 * <p>{@link #getFilterInstance} es la pieza que hace que un filtro se pueda reusar. Un filtro
 * describe una transformación, pero al aplicarse guarda estado —la imagen que va llegando— y ese
 * estado no se puede compartir entre dos consumidores. Así que el filtro que uno arma es un
 * **molde**: cada vez que se conecta a alguien se clona, y el clon es el que trabaja.
 */
public class ImageFilter implements ImageConsumer, Cloneable {

    /** A quién se le pasan los píxeles ya filtrados. */
    protected ImageConsumer consumer;

    /** Un filtro nulo. */
    public ImageFilter() {
    }

    /**
     * Una copia de este filtro conectada a ese consumidor.
     *
     * <p>Es lo que hay que llamar para usar un filtro: el original queda como molde y el clon lleva
     * el estado de una entrega concreta.
     */
    public ImageFilter getFilterInstance(ImageConsumer ic) {
        ImageFilter instance = (ImageFilter) this.clone();
        instance.consumer = ic;
        return instance;
    }

    /** Reenvía el tamaño. */
    public void setDimensions(int width, int height) {
        this.consumer.setDimensions(width, height);
    }

    /**
     * Reenvía las propiedades, agregando este filtro a la lista de los que pasó la imagen.
     *
     * <p>La propiedad `filters` deja constancia de por dónde pasó, que es lo único que queda de la
     * cadena una vez que la imagen llegó.
     */
    public void setProperties(Hashtable<?, ?> props) {
        Hashtable<Object, Object> p = copiar(props);
        Object o = p.get("filters");
        if (o == null) {
            p.put("filters", this.toString());
        } else if (o instanceof String) {
            p.put("filters", ((String) o) + this.toString());
        }
        this.consumer.setProperties(p);
    }

    /**
     * Una copia de la tabla, con las claves y los valores como `Object`.
     *
     * <p>Se copia entrada por entrada en vez de clonar porque hay que ensanchar los tipos: la que
     * entra puede ser de cualquier par de tipos y la que sale tiene que aceptar las claves que los
     * filtros agregan.
     */
    static Hashtable<Object, Object> copiar(Hashtable<?, ?> props) {
        Hashtable<Object, Object> p = new Hashtable<Object, Object>();
        java.util.Enumeration<?> e = props.keys();
        while (e.hasMoreElements()) {
            Object k = e.nextElement();
            p.put(k, props.get(k));
        }
        return p;
    }

    /** Reenvía el modelo de color. */
    public void setColorModel(ColorModel model) {
        this.consumer.setColorModel(model);
    }

    /** Reenvía las pistas. */
    public void setHints(int hints) {
        this.consumer.setHints(hints);
    }

    /** Reenvía los píxeles de un byte. */
    public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off,
            int scansize) {
        this.consumer.setPixels(x, y, w, h, model, pixels, off, scansize);
    }

    /** Reenvía los píxeles de un `int`. */
    public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off,
            int scansize) {
        this.consumer.setPixels(x, y, w, h, model, pixels, off, scansize);
    }

    /** Reenvía el fin de la entrega. */
    public void imageComplete(int status) {
        this.consumer.imageComplete(status);
    }

    /**
     * Le pide al productor que vuelva a mandar todo de arriba abajo.
     *
     * <p>Un filtro que pueda entregar en ese orden aunque lo reciba salteado tiene que redefinir
     * esto y hacer la reentrega él mismo en vez de pasarle el pedido al productor.
     */
    public void resendTopDownLeftRight(ImageProducer ip) {
        ip.requestTopDownLeftRightResend(this);
    }

    /** Una copia superficial. */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // No puede pasar: esta clase declara Cloneable.
            throw new InternalError(e.toString());
        }
    }
}
