package javax.xml.crypto.dsig.spec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * KajiLibrary's javax.xml.crypto.dsig.spec.XPathFilter2ParameterSpec -- una secuencia de filtros
 * XPath.
 *
 * <p>Los parametros de la transformacion XPath Filter 2.0, que reemplaza a la original por ser mucho
 * mas rapida: trabaja por subarboles en vez de nodo por nodo. Ver {@link XPathType} para las tres
 * operaciones.
 *
 * <p>La lista es una <b>secuencia</b> y no un conjunto: se aplican en orden sobre el resultado
 * acumulado. Cambiar el orden cambia lo que se firma.
 */
public final class XPathFilter2ParameterSpec implements TransformParameterSpec {

    /** Los filtros, en orden. No modificable. */
    private final List<XPathType> xPathList;

    /**
     * @param xPathList los filtros, en el orden en que se aplican
     * @throws NullPointerException si la lista es null
     * @throws IllegalArgumentException si esta vacia: una secuencia sin filtros no selecciona nada
     * @throws ClassCastException si algun elemento no es un {@link XPathType}
     */
    public XPathFilter2ParameterSpec(List<XPathType> xPathList) {
        if (xPathList == null) {
            throw new NullPointerException("xPathList cannot be null");
        }
        if (xPathList.isEmpty()) {
            throw new IllegalArgumentException("xPathList cannot be empty");
        }
        List<XPathType> copy = new ArrayList<XPathType>();
        int i = 0;
        while (i < xPathList.size()) {
            Object x = xPathList.get(i);
            if (!(x instanceof XPathType)) {
                throw new ClassCastException("not an XPathType: " + x);
            }
            copy.add((XPathType) x);
            i = i + 1;
        }
        this.xPathList = Collections.unmodifiableList(copy);
    }

    /** Los filtros, en orden. No modificable. */
    public List<XPathType> getXPathList() {
        return this.xPathList;
    }
}
