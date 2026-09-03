package org.xml.sax.helpers;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.AttributeList;

// KajiLibrary's org.xml.sax.helpers.AttributeListImpl -- la lista de atributos de SAX1, guardable
// y mutable.
//
// Existe por la misma razon que LocatorImpl: el AttributeList que un parser SAX1 pasa a
// startElement vale solo mientras dura esa llamada, y un manejador que quiera quedarse con los
// atributos tiene que copiarlos. `new AttributeListImpl(atts)` es esa copia.
//
// El otro uso es el reflejo del anterior: codigo que tiene que *producir* eventos startElement
// necesita un AttributeList para entregar, y armarlo con addAttribute/clear es mas facil que
// implementar la interfaz cada vez.
//
// Tres listas paralelas en vez de una lista de ternas, porque esa es la forma que quieren los
// getters: getName(i)/getType(i)/getValue(i) son tres busquedas independientes por el mismo
// indice. El reemplazo de SAX2, AttributesImpl, empaqueta todo en un solo String[] plano con un
// paso de cinco; son la misma idea con distinta aritmetica.
//
// La busqueda por nombre (getType(String)/getValue(String)) es un recorrido lineal de los nombres
// y devuelve la *primera* coincidencia. SAX1 no tenia nocion de espacios de nombres, asi que aca
// un nombre es el nombre literal del atributo tal como estaba escrito, con el prefijo pegado.
//
// Esta clase esta deprecada en el JDK, junto con toda la capa SAX1; esta aca porque el contrato
// la sigue listando y porque ParserAdapter necesita algo con esta forma.
public class AttributeListImpl implements AttributeList {

    List<String> names = new ArrayList<String>();
    List<String> types = new ArrayList<String>();
    List<String> values = new ArrayList<String>();

    public AttributeListImpl() {
    }

    // El constructor de copia descrito arriba.
    public AttributeListImpl(AttributeList atts) {
        setAttributeList(atts);
    }

    ////////////////////////////////////////////////////////////////////
    // Construccion
    ////////////////////////////////////////////////////////////////////

    // Reemplaza todo por una copia de `atts`. Lee getLength() una sola vez y despues la recorre,
    // asi que es una copia fija incluso de una lista que esta por cambiar.
    public void setAttributeList(AttributeList atts) {
        int count = atts.getLength();

        clear();

        for (int i = 0; i < count; i++) {
            addAttribute(atts.getName(i), atts.getType(i), atts.getValue(i));
        }
    }

    public void addAttribute(String name, String type, String value) {
        names.add(name);
        types.add(type);
        values.add(value);
    }

    // Saca el primer atributo con este nombre, si hay alguno; un nombre que no esta no es un
    // error, simplemente no hay nada que hacer.
    public void removeAttribute(String name) {
        int i = names.indexOf(name);
        if (i >= 0) {
            names.remove(i);
            types.remove(i);
            values.remove(i);
        }
    }

    public void clear() {
        names.clear();
        types.clear();
        values.clear();
    }

    ////////////////////////////////////////////////////////////////////
    // AttributeList
    ////////////////////////////////////////////////////////////////////

    public int getLength() {
        return names.size();
    }

    // Los getters por indice contestan null ante un indice fuera de rango en vez de tirar
    // excepcion, que es lo que pide el contrato de SAX1.
    public String getName(int i) {
        if (i < 0 || i >= names.size()) {
            return null;
        }
        return names.get(i);
    }

    public String getType(int i) {
        if (i < 0 || i >= types.size()) {
            return null;
        }
        return types.get(i);
    }

    public String getValue(int i) {
        if (i < 0 || i >= values.size()) {
            return null;
        }
        return values.get(i);
    }

    public String getType(String name) {
        return getType(names.indexOf(name));
    }

    public String getValue(String name) {
        return getValue(names.indexOf(name));
    }
}
