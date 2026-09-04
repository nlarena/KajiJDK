package javax.print;

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import javax.print.attribute.AttributeSetUtilities;
import javax.print.attribute.DocAttributeSet;

/**
 * KajiLibrary's javax.print.SimpleDoc -- la implementacion comun de {@link Doc}.
 *
 * <p>Cubre el caso normal: un dato ya en memoria o un flujo abierto, con su formato y sus atributos.
 * Solo hace falta escribir un {@code Doc} propio cuando el dato se produce sobre la marcha.
 *
 * <h2>Verifica que el dato sea del tipo que dice</h2>
 *
 * <p>El constructor comprueba que el dato sea instancia de la clase de representacion del formato, y
 * lanza {@link IllegalArgumentException} si no. Vale la pena que falle aca y no adentro del servicio,
 * donde el error saldria como un {@code ClassCastException} sin contexto.
 *
 * <h2>Se lee una sola vez</h2>
 *
 * <p>{@link #getReaderForText} y {@link #getStreamForBytes} guardan lo que devuelven y devuelven
 * siempre lo mismo, como pide {@link Doc}. La consecuencia es que un {@code SimpleDoc} sirve para una
 * sola impresion aunque el dato sea un {@code String}.
 */
public final class SimpleDoc implements Doc {

    /** El dato. */
    private final Object printData;

    /** De que tipo es. */
    private final DocFlavor flavor;

    /** Los atributos, ya de solo lectura. */
    private final DocAttributeSet attributes;

    /** El lector, una vez creado. Ver la nota de la clase. */
    private Reader reader;

    /** El flujo, una vez creado. */
    private InputStream inputStream;

    /**
     * @param printData el dato, que tiene que ser de la clase que declara el formato
     * @param flavor de que tipo es
     * @param attributes los atributos propios del documento, o null
     * @throws IllegalArgumentException si el dato o el formato son null, o si el dato no es de la
     *     clase declarada
     */
    public SimpleDoc(Object printData, DocFlavor flavor, DocAttributeSet attributes) {
        if (flavor == null || printData == null) {
            throw new IllegalArgumentException("null argument(s)");
        }
        Class<?> repClass;
        try {
            repClass = Class.forName(flavor.getRepresentationClassName(), false,
                                     getClass().getClassLoader());
        } catch (Throwable e) {
            throw new IllegalArgumentException("unknown representation class");
        }
        if (!repClass.isInstance(printData)) {
            throw new IllegalArgumentException("data is not of declared type");
        }
        this.printData = printData;
        this.flavor = flavor;
        if (attributes != null) {
            this.attributes = AttributeSetUtilities.unmodifiableView(attributes);
        } else {
            this.attributes = null;
        }
    }

    /** De que tipo es. */
    public DocFlavor getDocFlavor() {
        return this.flavor;
    }

    /** Los atributos, o null si no se pasaron. */
    public DocAttributeSet getAttributes() {
        return this.attributes;
    }

    /** El dato. */
    public Object getPrintData() throws IOException {
        return this.printData;
    }

    /**
     * El dato como caracteres, o null si no es texto.
     *
     * <p>Reconoce {@code char[]}, {@link String} y {@link Reader}. Siempre el mismo lector.
     */
    public synchronized Reader getReaderForText() throws IOException {
        if (this.printData instanceof char[]) {
            if (this.reader == null) {
                this.reader = new CharArrayReader((char[]) this.printData);
            }
        } else if (this.printData instanceof String) {
            if (this.reader == null) {
                this.reader = new StringReader((String) this.printData);
            }
        } else if (this.printData instanceof Reader) {
            this.reader = (Reader) this.printData;
        }
        return this.reader;
    }

    /**
     * El dato como bytes, o null si no lo es.
     *
     * <p>Reconoce {@code byte[]} e {@link InputStream}. Siempre el mismo flujo.
     */
    public synchronized InputStream getStreamForBytes() throws IOException {
        if (this.printData instanceof byte[]) {
            if (this.inputStream == null) {
                this.inputStream = new ByteArrayInputStream((byte[]) this.printData);
            }
        } else if (this.printData instanceof InputStream) {
            this.inputStream = (InputStream) this.printData;
        }
        return this.inputStream;
    }
}
