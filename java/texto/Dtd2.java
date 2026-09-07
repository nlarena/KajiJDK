import java.util.Vector;

import javax.swing.text.html.parser.AttributeList;
import javax.swing.text.html.parser.ContentModel;
import javax.swing.text.html.parser.DTD;
import javax.swing.text.html.parser.Element;
import javax.swing.text.html.parser.Entity;

/**
 * La DTD de HTML 3.2 que arma cada biblioteca, comparada elemento por elemento.
 *
 * <p>El JDK la lee de un archivo binario de su imagen; esta biblioteca la tiene escrita como datos
 * en `Html32`. Si las dos dan lo mismo -- los 88 elementos con sus modelos de contenido,
 * exclusiones, inclusiones y atributos, y las 255 entidades -- entonces la tabla escrita a mano
 * quedo bien.
 */
public class Dtd2 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    static String texto(ContentModel m) {
        return (m == null) ? "-" : m.toString();
    }

    static String bits(java.util.BitSet b, DTD d) {
        if (b == null) {
            return "-";
        }
        String s = "";
        for (int i = 0; i < b.size(); i++) {
            if (b.get(i)) {
                s = s + d.getElement(i).getName() + ",";
            }
        }
        return s.length() == 0 ? "-" : s;
    }

    public static int run() {
        try {
            // El analizador de siempre arma la DTD y la deja registrada; las dos bibliotecas
            // la dejan con el mismo nombre, asi que este archivo sirve igual en las dos.
            new javax.swing.text.html.parser.ParserDelegator();
            DTD d = DTD.getDTD("html32");

            linea("nombre=" + d.getName() + " elementos=" + d.elements.size()
                    + " entidades=" + d.entityHash.size());

            for (int i = 0; i < d.elements.size(); i++) {
                Element e = d.getElement(i);
                linea("elem " + i + " " + e.getName() + " tipo=" + e.getType()
                        + " oStart=" + e.omitStart() + " oEnd=" + e.omitEnd()
                        + " vacio=" + e.isEmpty());
                linea("     contenido=" + texto(e.getContent()));
                linea("     excl=" + bits(e.exclusions, d) + " incl=" + bits(e.inclusions, d));
                String at = "";
                for (AttributeList a = e.getAttributes(); a != null; a = a.getNext()) {
                    at = at + a.getName() + ":" + a.getType() + ":" + a.getModifier()
                            + ":" + a.getValue();
                    if (a.getValues() != null) {
                        at = at + ":{";
                        java.util.Enumeration<?> v = a.getValues();
                        while (v.hasMoreElements()) {
                            at = at + v.nextElement() + " ";
                        }
                        at = at + "}";
                    }
                    at = at + " | ";
                }
                linea("     atts=" + (at.length() == 0 ? "-" : at));
            }

            // Las entidades, ordenadas por nombre para que el orden de la tabla no cuente.
            Vector<String> nombres = new Vector<String>();
            java.util.Enumeration<Object> k = d.entityHash.keys();
            while (k.hasMoreElements()) {
                Object o = k.nextElement();
                if (o instanceof String) {
                    nombres.addElement((String) o);
                }
            }
            String[] arr = new String[nombres.size()];
            nombres.copyInto(arr);
            java.util.Arrays.sort(arr);
            linea("entidades con nombre=" + arr.length);
            for (int i = 0; i < arr.length; i++) {
                Entity en = d.getEntity(arr[i]);
                String datos = "";
                char[] c = en.getData();
                for (int j = 0; j < c.length; j++) {
                    datos = datos + ((int) c[j]) + ".";
                }
                linea("  ent " + en.getName() + " tipo=" + en.getType()
                        + " gen=" + en.isGeneral() + " par=" + en.isParameter()
                        + " datos=" + datos);
            }

            // Los elementos que la DTD tiene como campos.
            linea("pcdata=" + d.pcdata + " html=" + d.html + " head=" + d.head + " body=" + d.body
                    + " p=" + d.p + " title=" + d.title + " applet=" + d.applet
                    + " param=" + d.param + " meta=" + d.meta + " base=" + d.base
                    + " isindex=" + d.isindex);
        } catch (Exception e) {
            linea("EXCEPCION " + e.getClass().getName() + " " + e.getMessage());
        }
        return 0;
    }
}
