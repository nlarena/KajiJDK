package org.xml.sax.ext;

import org.xml.sax.Attributes;

/**
 * KajiLibrary's org.xml.sax.ext.Attributes2Impl -- `AttributesImpl` con los dos arreglos de
 * banderas que hacen falta para contestar {@link Attributes2}.
 *
 * <p>La herencia hace casi todo: los cinco campos por atributo, las busquedas por nombre y el
 * crecimiento del arreglo estan en `AttributesImpl`. Aca se agregan dos `boolean[]` paralelos, uno
 * por cada pregunta nueva, y la unica obligacion real es **mantenerlos alineados con la lista de
 * abajo**: cada `addAttribute` y cada `removeAttribute` tiene que mover las banderas igual que la
 * superclase mueve los datos, o el atributo `i` termina contestando por las banderas del `j`.
 *
 * <p><strong>Hay una trampa de inicializacion en el constructor de copia y esta puesta a
 * proposito.</strong> `super(atts)` llama a `setAttributes`, que es virtual y por lo tanto ejecuta
 * la version de **esta** clase mientras la superclase todavia se esta construyendo: los dos
 * arreglos quedan armados desde ahi. Eso funciona solo porque ningun campo de aca tiene
 * inicializador --si lo tuviera, correria despues del `super(...)` y pisaria con `null` lo que
 * `setAttributes` acababa de dejar--. Es la razon por la que `declared` y `specified` se declaran
 * pelados y se llenan en el constructor sin argumentos.
 *
 * <p>Los valores por omision de `addAttribute` no son relleno: `specified` queda en `true` --lo
 * estan agregando, luego fue especificado-- y `declared` sale del tipo, en `true` para todo lo que
 * no sea `CDATA`. Eso ultimo vale porque sin DTD todos los atributos son `CDATA`, asi que un tipo
 * distinto de `CDATA` solo puede haber salido de una declaracion. Es una deduccion correcta, no una
 * adivinanza; el que quiera otra cosa tiene {@link #setDeclared} y {@link #setSpecified}.
 *
 * <p>Y por eso mismo `setAttributes` mira si la fuente es un `Attributes2`: si lo es, copia las
 * banderas de verdad; si no, aplica esa misma deduccion, que es lo unico que se puede saber de una
 * lista que no las tiene.
 */
public class Attributes2Impl extends org.xml.sax.helpers.AttributesImpl
        implements Attributes2 {

    // Sin inicializador, por lo que explica el comentario de la clase.
    private boolean declared[];
    private boolean specified[];

    /** Una lista vacia, lista para `addAttribute`. */
    public Attributes2Impl() {
        declared = new boolean[0];
        specified = new boolean[0];
    }

    /**
     * Una copia independiente, banderas incluidas. El `Attributes` que el parser presta en
     * `startElement` deja de valer cuando la llamada termina; esta copia no.
     */
    public Attributes2Impl(Attributes atts) {
        super(atts);
    }

    public boolean isDeclared(int index) {
        if (index < 0 || index >= getLength()) {
            throw new ArrayIndexOutOfBoundsException("No attribute at index: " + index);
        }
        return declared[index];
    }

    public boolean isDeclared(String uri, String localName) {
        int index = getIndex(uri, localName);
        if (index < 0) {
            throw new IllegalArgumentException("No such attribute: local name="
                    + localName + ", namespace=" + uri);
        }
        return declared[index];
    }

    public boolean isDeclared(String qName) {
        int index = getIndex(qName);
        if (index < 0) {
            throw new IllegalArgumentException("No such attribute: " + qName);
        }
        return declared[index];
    }

    public boolean isSpecified(int index) {
        if (index < 0 || index >= getLength()) {
            throw new ArrayIndexOutOfBoundsException("No attribute at index: " + index);
        }
        return specified[index];
    }

    public boolean isSpecified(String uri, String localName) {
        int index = getIndex(uri, localName);
        if (index < 0) {
            throw new IllegalArgumentException("No such attribute: local name="
                    + localName + ", namespace=" + uri);
        }
        return specified[index];
    }

    public boolean isSpecified(String qName) {
        int index = getIndex(qName);
        if (index < 0) {
            throw new IllegalArgumentException("No such attribute: " + qName);
        }
        return specified[index];
    }

    /**
     * Reemplaza el contenido por una copia del de `atts`. Los arreglos se rehacen del tamano justo
     * en vez de reusarse: la lista anterior podia ser mas larga y dejar banderas viejas colgando
     * detras de la nueva.
     */
    public void setAttributes(Attributes atts) {
        int length = atts.getLength();

        super.setAttributes(atts);
        declared = new boolean[length];
        specified = new boolean[length];

        if (atts instanceof Attributes2) {
            Attributes2 a2 = (Attributes2) atts;
            for (int i = 0; i < length; i++) {
                declared[i] = a2.isDeclared(i);
                specified[i] = a2.isSpecified(i);
            }
        } else {
            for (int i = 0; i < length; i++) {
                declared[i] = !("CDATA".equals(atts.getType(i)));
                specified[i] = true;
            }
        }
    }

    /** Agrega al final con las banderas deducidas que explica el comentario de la clase. */
    public void addAttribute(String uri, String localName, String qName,
                             String type, String value) {
        super.addAttribute(uri, localName, qName, type, value);
        int length = getLength();

        if (length > declared.length) {
            boolean newFlags[];

            newFlags = new boolean[length];
            System.arraycopy(declared, 0, newFlags, 0, declared.length);
            declared = newFlags;

            newFlags = new boolean[length];
            System.arraycopy(specified, 0, newFlags, 0, specified.length);
            specified = newFlags;
        }

        specified[length - 1] = true;
        declared[length - 1] = !"CDATA".equals(type);
    }

    /**
     * Saca el atributo `index`. Los arreglos de banderas se corren igual que los datos de la
     * superclase; si no, los que quedan detras contestarian por el que se fue.
     */
    public void removeAttribute(int index) {
        int origMax = getLength() - 1;

        super.removeAttribute(index);
        if (index != origMax) {
            System.arraycopy(declared, index + 1, declared, index, origMax - index);
            System.arraycopy(specified, index + 1, specified, index, origMax - index);
        }
    }

    public void setDeclared(int index, boolean value) {
        if (index < 0 || index >= getLength()) {
            throw new ArrayIndexOutOfBoundsException("No attribute at index: " + index);
        }
        declared[index] = value;
    }

    public void setSpecified(int index, boolean value) {
        if (index < 0 || index >= getLength()) {
            throw new ArrayIndexOutOfBoundsException("No attribute at index: " + index);
        }
        specified[index] = value;
    }
}
