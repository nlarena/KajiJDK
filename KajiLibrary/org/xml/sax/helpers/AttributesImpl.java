package org.xml.sax.helpers;

import org.xml.sax.Attributes;

// KajiLibrary's org.xml.sax.helpers.AttributesImpl -- la implementacion de {@link Attributes}
// que sirve para las dos cosas que hacen falta: **congelar** los atributos que un parser presto
// durante `startElement`, y **construirlos** a mano cuando uno mismo genera eventos.
//
// El almacenamiento es un solo `String[]` con cinco casilleros por atributo --uri, nombre local,
// nombre calificado, tipo, valor-- en vez de un arreglo de objetos. Es la representacion del JDK
// y vale la pena entender por que: los atributos de un elemento son tipicamente cero, uno o dos,
// y a esa escala un objeto por atributo cuesta mas en asignaciones y en salto de puntero que lo
// que ahorra en claridad. El precio es que todos los indices van multiplicados por cinco, asi
// que el atributo `i` vive en `data[i*5 .. i*5+4]`.
//
// **La asimetria de los indices fuera de rango es del contrato, no un descuido.** Los `getXxx(int)`
// devuelven `null` --un indice que no existe simplemente no tiene valor--, pero los `setXxx(int)`
// y `removeAttribute(int)` tiran `ArrayIndexOutOfBoundsException`: escribir en una posicion que
// no existe es un error del programa y callarlo perderia el dato.
//
// Las busquedas por nombre son lineales. Con la cantidad de atributos que tiene un elemento real
// eso es mas rapido que cualquier mapa, y evita mantener un indice que habria que rehacer en cada
// `setQName`.
public class AttributesImpl implements Attributes {

    // Cantidad de atributos; el arreglo puede ser mas largo.
    int length;

    // uri, localName, qName, type, value por cada atributo, en ese orden.
    String data[];

    // Una lista vacia, lista para `addAttribute`.
    public AttributesImpl() {
        length = 0;
        data = null;
    }

    // Una copia independiente de `atts`. Este es el constructor que resuelve el bug clasico de
    // SAX: el objeto que llega a `startElement` deja de valer cuando la llamada termina, y esta
    // copia no.
    public AttributesImpl(Attributes atts) {
        setAttributes(atts);
    }

    public int getLength() {
        return length;
    }

    public String getURI(int index) {
        if (index >= 0 && index < length) {
            return data[index * 5];
        } else {
            return null;
        }
    }

    public String getLocalName(int index) {
        if (index >= 0 && index < length) {
            return data[index * 5 + 1];
        } else {
            return null;
        }
    }

    public String getQName(int index) {
        if (index >= 0 && index < length) {
            return data[index * 5 + 2];
        } else {
            return null;
        }
    }

    public String getType(int index) {
        if (index >= 0 && index < length) {
            return data[index * 5 + 3];
        } else {
            return null;
        }
    }

    public String getValue(int index) {
        if (index >= 0 && index < length) {
            return data[index * 5 + 4];
        } else {
            return null;
        }
    }

    // Busqueda por (URI, nombre local). -1 si no esta.
    public int getIndex(String uri, String localName) {
        int max = length * 5;
        for (int i = 0; i < max; i += 5) {
            if (data[i].equals(uri) && data[i + 1].equals(localName)) {
                return i / 5;
            }
        }
        return -1;
    }

    // Busqueda por nombre calificado. -1 si no esta.
    public int getIndex(String qName) {
        int max = length * 5;
        for (int i = 0; i < max; i += 5) {
            if (data[i + 2].equals(qName)) {
                return i / 5;
            }
        }
        return -1;
    }

    public String getType(String uri, String localName) {
        int max = length * 5;
        for (int i = 0; i < max; i += 5) {
            if (data[i].equals(uri) && data[i + 1].equals(localName)) {
                return data[i + 3];
            }
        }
        return null;
    }

    public String getType(String qName) {
        int max = length * 5;
        for (int i = 0; i < max; i += 5) {
            if (data[i + 2].equals(qName)) {
                return data[i + 3];
            }
        }
        return null;
    }

    public String getValue(String uri, String localName) {
        int max = length * 5;
        for (int i = 0; i < max; i += 5) {
            if (data[i].equals(uri) && data[i + 1].equals(localName)) {
                return data[i + 4];
            }
        }
        return null;
    }

    public String getValue(String qName) {
        int max = length * 5;
        for (int i = 0; i < max; i += 5) {
            if (data[i + 2].equals(qName)) {
                return data[i + 4];
            }
        }
        return null;
    }

    // Vacia la lista. Anula tambien los casilleros usados, no solo `length`: si no, la lista
    // seguiria sosteniendo cadenas que ya nadie mira.
    public void clear() {
        if (data != null) {
            for (int i = 0; i < (length * 5); i++) {
                data[i] = null;
            }
        }
        length = 0;
    }

    // Reemplaza el contenido por una copia del de `atts`.
    public void setAttributes(Attributes atts) {
        clear();
        length = atts.getLength();
        if (length > 0) {
            data = new String[length * 5];
            for (int i = 0; i < length; i++) {
                data[i * 5] = atts.getURI(i);
                data[i * 5 + 1] = atts.getLocalName(i);
                data[i * 5 + 2] = atts.getQName(i);
                data[i * 5 + 3] = atts.getType(i);
                data[i * 5 + 4] = atts.getValue(i);
            }
        }
    }

    // Agrega al final. No chequea duplicados: XML lo prohibe, pero quien genera los eventos es
    // responsable de eso, no esta lista.
    public void addAttribute(String uri, String localName, String qName,
                             String type, String value) {
        ensureCapacity(length + 1);
        data[length * 5] = uri;
        data[length * 5 + 1] = localName;
        data[length * 5 + 2] = qName;
        data[length * 5 + 3] = type;
        data[length * 5 + 4] = value;
        length++;
    }

    // Reescribe los cinco campos del atributo `index`.
    public void setAttribute(int index, String uri, String localName,
                             String qName, String type, String value) {
        if (index >= 0 && index < length) {
            data[index * 5] = uri;
            data[index * 5 + 1] = localName;
            data[index * 5 + 2] = qName;
            data[index * 5 + 3] = type;
            data[index * 5 + 4] = value;
        } else {
            badIndex(index);
        }
    }

    // Saca el atributo `index` corriendo los de atras una posicion. Los indices de los que
    // siguen cambian, que es lo esperable en una lista.
    public void removeAttribute(int index) {
        if (index >= 0 && index < length) {
            if (index < length - 1) {
                System.arraycopy(data, (index + 1) * 5, data, index * 5,
                                 (length - index - 1) * 5);
            }
            index = (length - 1) * 5;
            data[index++] = null;
            data[index++] = null;
            data[index++] = null;
            data[index++] = null;
            data[index] = null;
            length--;
        } else {
            badIndex(index);
        }
    }

    public void setURI(int index, String uri) {
        if (index >= 0 && index < length) {
            data[index * 5] = uri;
        } else {
            badIndex(index);
        }
    }

    public void setLocalName(int index, String localName) {
        if (index >= 0 && index < length) {
            data[index * 5 + 1] = localName;
        } else {
            badIndex(index);
        }
    }

    public void setQName(int index, String qName) {
        if (index >= 0 && index < length) {
            data[index * 5 + 2] = qName;
        } else {
            badIndex(index);
        }
    }

    public void setType(int index, String type) {
        if (index >= 0 && index < length) {
            data[index * 5 + 3] = type;
        } else {
            badIndex(index);
        }
    }

    public void setValue(int index, String value) {
        if (index >= 0 && index < length) {
            data[index * 5 + 4] = value;
        } else {
            badIndex(index);
        }
    }

    // Crece al doble desde un piso de 25 casilleros (cinco atributos), que es lo que el JDK
    // eligio: la enorme mayoria de los elementos entra en el primer bloque y nunca vuelve a
    // copiar.
    private void ensureCapacity(int n) {
        if (n <= 0) {
            return;
        }
        int max;
        if (data == null || data.length == 0) {
            max = 25;
        } else if (data.length >= n * 5) {
            return;
        } else {
            max = data.length;
        }
        while (max < n * 5) {
            max *= 2;
        }
        String newData[] = new String[max];
        if (length > 0) {
            System.arraycopy(data, 0, newData, 0, length * 5);
        }
        data = newData;
    }

    private void badIndex(int index) {
        String msg = "Attempt to modify attribute at illegal index: " + index;
        throw new ArrayIndexOutOfBoundsException(msg);
    }
}
