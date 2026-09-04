package java.text;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Texto con atributos pegados a rangos de caracteres, y la fábrica de los iteradores que lo
 * recorren.
 *
 * <p>Un {@code String} dice qué caracteres hay; esto dice además qué rige sobre cada tramo: el
 * idioma, la fuente, o —dentro de este paquete— qué campo del resultado de un formateo es cada
 * pedazo. Es la contraparte escribible de {@link AttributedCharacterIterator}: acá se arma, allá se
 * lee.
 *
 * <p><b>El orden de las llamadas importa.</b> Dos {@code addAttribute} con la misma clave sobre
 * rangos que se pisan no se fusionan ni se rechazan: gana el último. Eso permite el patrón normal
 * de "pintá todo de A y después el pedazo del medio de B" sin tener que calcular la resta de
 * rangos.
 *
 * <p><b>Diferencia con el JDK, deliberada.</b> El JDK guarda tramos físicos y parte uno nuevo en
 * cada borde que se agrega, sin volver a fusionarlos; el {@code getRunLimit()} sin argumento
 * devuelve entonces ese borde físico, que puede quedar corto aunque los atributos a los dos lados
 * sean idénticos. Acá los tramos se calculan al leer, comparando los mapas de atributos, así que el
 * límite es el que dice el contrato ("hasta donde no cambia ningún atributo") y no un residuo de
 * cómo se construyó el objeto. Para un llamador correcto la diferencia es invisible; para uno que
 * cuente tramos, la nuestra es la que el javadoc promete.
 *
 * <p>Miembros no públicos del JDK que no están: los campos {@code text}/{@code runCount}/… y el
 * constructor de concatenación son de acceso de paquete y describen la representación interna del
 * JDK, que acá es otra. No son parte de la API.
 */
public class AttributedString {

    // Acceso de paquete: AttributedStringIterator los lee directamente. La alternativa —accesores—
    // sólo agregaría ruido, porque las dos clases son una sola pieza partida en dos archivos.
    final String texto;
    AttributedCharacterIterator.Attribute[] claves;
    Object[] valores;
    int[] desde;
    int[] hasta;
    int cantidad;

    public AttributedString(String text) {
        if (text == null) {
            throw new NullPointerException();
        }
        this.texto = text;
        this.claves = new AttributedCharacterIterator.Attribute[4];
        this.valores = new Object[4];
        this.desde = new int[4];
        this.hasta = new int[4];
        this.cantidad = 0;
    }

    public AttributedString(String text, Map<? extends AttributedCharacterIterator.Attribute, ?> attributes) {
        this(text);
        if (attributes == null) {
            throw new NullPointerException();
        }
        if (text.length() == 0) {
            // Un texto vacío con atributos es una contradicción: no hay ningún carácter sobre el
            // que rijan. El JDK lo rechaza y nosotros también, porque aceptarlo dejaría un objeto
            // cuyos atributos ningún iterador podría devolver jamás.
            if (!attributes.isEmpty()) {
                throw new IllegalArgumentException("Can't add attribute to 0-length text");
            }
            return;
        }
        for (Map.Entry<? extends AttributedCharacterIterator.Attribute, ?> e : attributes.entrySet()) {
            this.agregar(e.getKey(), e.getValue(), 0, text.length());
        }
    }

    public AttributedString(AttributedCharacterIterator text) {
        this(text, obtenerInicio(text), obtenerFin(text), null);
    }

    public AttributedString(AttributedCharacterIterator text, int beginIndex, int endIndex) {
        this(text, beginIndex, endIndex, null);
    }

    /**
     * Copia un rango del iterador quedándose sólo con los atributos listados.
     *
     * @param attributes las claves a conservar; {@code null} conserva todas. Un arreglo VACÍO no es
     *                   lo mismo que {@code null}: descarta todos los atributos y deja el texto pelado.
     */
    public AttributedString(AttributedCharacterIterator text, int beginIndex, int endIndex,
                            AttributedCharacterIterator.Attribute[] attributes) {
        if (text == null) {
            throw new NullPointerException();
        }
        if (beginIndex < text.getBeginIndex() || endIndex > text.getEndIndex() || beginIndex > endIndex) {
            throw new IllegalArgumentException("Invalid substring range");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = beginIndex; i < endIndex; i++) {
            text.setIndex(i);
            sb.append(text.current());
        }
        this.texto = sb.toString();
        this.claves = new AttributedCharacterIterator.Attribute[4];
        this.valores = new Object[4];
        this.desde = new int[4];
        this.hasta = new int[4];
        this.cantidad = 0;

        Set<AttributedCharacterIterator.Attribute> filtro = null;
        if (attributes != null) {
            filtro = new HashSet<AttributedCharacterIterator.Attribute>();
            for (int i = 0; i < attributes.length; i++) {
                filtro.add(attributes[i]);
            }
        }

        // Se copia tramo por tramo y no carácter por carácter porque el iterador ya sabe dónde
        // cambian los atributos: preguntárselo evita releer el mismo mapa una vez por posición.
        int i = beginIndex;
        while (i < endIndex) {
            text.setIndex(i);
            int fin = text.getRunLimit();
            if (fin > endIndex) {
                fin = endIndex;
            }
            if (fin <= i) {
                fin = i + 1;
            }
            Map<AttributedCharacterIterator.Attribute, Object> mapa = text.getAttributes();
            if (mapa != null) {
                for (Map.Entry<AttributedCharacterIterator.Attribute, Object> e : mapa.entrySet()) {
                    if (filtro == null || filtro.contains(e.getKey())) {
                        this.agregar(e.getKey(), e.getValue(), i - beginIndex, fin - beginIndex);
                    }
                }
            }
            i = fin;
        }
    }

    private static int obtenerInicio(AttributedCharacterIterator it) {
        if (it == null) {
            throw new NullPointerException();
        }
        return it.getBeginIndex();
    }

    private static int obtenerFin(AttributedCharacterIterator it) {
        if (it == null) {
            throw new NullPointerException();
        }
        return it.getEndIndex();
    }

    public void addAttribute(AttributedCharacterIterator.Attribute attribute, Object value) {
        if (attribute == null) {
            throw new NullPointerException();
        }
        if (this.texto.length() == 0) {
            throw new IllegalArgumentException("Can't add attribute to 0-length text");
        }
        this.agregar(attribute, value, 0, this.texto.length());
    }

    public void addAttribute(AttributedCharacterIterator.Attribute attribute, Object value,
                             int beginIndex, int endIndex) {
        if (attribute == null) {
            throw new NullPointerException();
        }
        if (beginIndex < 0 || endIndex > this.texto.length() || beginIndex >= endIndex) {
            throw new IllegalArgumentException("Invalid substring range");
        }
        this.agregar(attribute, value, beginIndex, endIndex);
    }

    public void addAttributes(Map<? extends AttributedCharacterIterator.Attribute, ?> attributes,
                              int beginIndex, int endIndex) {
        if (attributes == null) {
            throw new NullPointerException();
        }
        if (beginIndex < 0 || endIndex > this.texto.length() || beginIndex > endIndex) {
            throw new IllegalArgumentException("Invalid substring range");
        }
        if (beginIndex == endIndex) {
            // Rango vacío: el JDK lo acepta y no hace nada. No es lo mismo que el caso de
            // addAttribute, donde el rango vacío viene de un texto vacío y sí es un error.
            if (attributes.isEmpty()) {
                return;
            }
            throw new IllegalArgumentException("Can't add attribute to 0-length text");
        }
        for (Map.Entry<? extends AttributedCharacterIterator.Attribute, ?> e : attributes.entrySet()) {
            this.agregar(e.getKey(), e.getValue(), beginIndex, endIndex);
        }
    }

    public AttributedCharacterIterator getIterator() {
        return this.getIterator(null, 0, this.texto.length());
    }

    public AttributedCharacterIterator getIterator(AttributedCharacterIterator.Attribute[] attributes) {
        return this.getIterator(attributes, 0, this.texto.length());
    }

    public AttributedCharacterIterator getIterator(AttributedCharacterIterator.Attribute[] attributes,
                                                   int beginIndex, int endIndex) {
        return new AttributedStringIterator(this, attributes, beginIndex, endIndex);
    }

    // El agregado es un append puro, sin fusionar ni recortar lo anterior: la resolución "gana el
    // último" se hace al leer, recorriendo la lista en orden. Fusionar acá obligaría a partir
    // tramos viejos en cada llamada y no cambiaría ningún resultado observable.
    private void agregar(AttributedCharacterIterator.Attribute clave, Object valor, int d, int h) {
        if (clave == null) {
            throw new NullPointerException();
        }
        if (this.cantidad == this.claves.length) {
            int nuevo = this.claves.length * 2;
            AttributedCharacterIterator.Attribute[] k = new AttributedCharacterIterator.Attribute[nuevo];
            Object[] v = new Object[nuevo];
            int[] a = new int[nuevo];
            int[] b = new int[nuevo];
            for (int i = 0; i < this.cantidad; i++) {
                k[i] = this.claves[i];
                v[i] = this.valores[i];
                a[i] = this.desde[i];
                b[i] = this.hasta[i];
            }
            this.claves = k;
            this.valores = v;
            this.desde = a;
            this.hasta = b;
        }
        this.claves[this.cantidad] = clave;
        this.valores[this.cantidad] = valor;
        this.desde[this.cantidad] = d;
        this.hasta[this.cantidad] = h;
        this.cantidad = this.cantidad + 1;
    }
}
