import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Prueba de comportamiento de javax.xml.stream, javax.xml.namespace y javax.xml.datatype.
 *
 * <p>El mismo fuente corre en las dos VMs. Contra el JDK real los paquetes {@code javax.xml.*} salen
 * del modulo {@code java.xml} y no de {@code KajiLibrary} --un modulo le gana al classpath-- asi que
 * lo que este archivo comprueba es que las dos implementaciones contestan lo mismo. Cuando no
 * coinciden, la que esta mal es la de aca hasta que se demuestre lo contrario.
 *
 * <p>Devuelve -1 si paso todo, o la cantidad de fallas.
 */
public class StaxTest {

    static int fallas;

    static void check(boolean ok, String que) {
        if (!ok) {
            fallas++;
            System.out.println("FALLA: " + que);
        }
    }

    static void eq(Object a, Object b, String que) {
        boolean ok;
        if (a == null) {
            ok = b == null;
        } else {
            ok = a.equals(b);
        }
        if (!ok) {
            fallas++;
            System.out.println("FALLA: " + que + " -- esperaba [" + b + "] y vino [" + a + "]");
        }
    }

    static void eqi(int a, int b, String que) {
        if (a != b) {
            fallas++;
            System.out.println("FALLA: " + que + " -- esperaba " + b + " y vino " + a);
        }
    }

    static final String DOC =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!-- c -->"
            + "<r xmlns=\"urn:d\" xmlns:a=\"urn:a\" a:x=\"1\" y=\"2\">"
            + "<p>hola &amp; chau</p>"
            + "<a:q/>"
            + "<![CDATA[<crudo>]]>"
            + "</r>";

    public static int run() {
        fallas = 0;
        try {
            qname();
            constantes();
            duracion();
            calendario();
            cursor();
            escritor();
            eventos();
            fabricaDeEventos();
            filtrado();
        } catch (Throwable t) {
            fallas++;
            System.out.println("FALLA: excepcion inesperada " + t);
        }
        if (fallas == 0) {
            return -1;
        }
        return fallas;
    }

    // ---- javax.xml.namespace.QName -----------------------------------------------------------

    static void qname() {
        QName q = new QName("http://t", "p");
        eq(q.toString(), "{http://t}p", "QName.toString con espacio de nombres");
        eq(new QName("x").toString(), "x", "QName.toString sin espacio de nombres");
        eq(QName.valueOf("{http://t}p"), q, "valueOf deshace toString");
        eq(QName.valueOf("{http://t}p").getPrefix(), "", "valueOf no transporta el prefijo");
        eq(QName.valueOf("suelto"), new QName("suelto"), "valueOf de un nombre pelado");

        QName a = new QName("u", "l", "aa");
        QName b = new QName("u", "l", "bb");
        check(a.equals(b), "el prefijo no entra en equals");
        check(b.equals(a), "equals es simetrico");
        eqi(a.hashCode(), b.hashCode(), "el prefijo no entra en hashCode");
        check(!a.equals(new QName("otro", "l", "aa")), "otro espacio de nombres es otro nombre");
        check(!a.equals(new QName("u", "otro", "aa")), "otro nombre local es otro nombre");
        eq(new QName(null, "l").getNamespaceURI(), "", "null se normaliza a la cadena vacia");

        boolean tiro = false;
        try {
            QName.valueOf("{}x");
        } catch (IllegalArgumentException e) {
            tiro = true;
        }
        check(tiro, "valueOf de {}x tiene que fallar");

        tiro = false;
        try {
            QName.valueOf(null);
        } catch (IllegalArgumentException e) {
            tiro = true;
        }
        check(tiro, "valueOf de null tiene que fallar");

        tiro = false;
        try {
            new QName("u", null);
        } catch (IllegalArgumentException e) {
            tiro = true;
        }
        check(tiro, "un nombre local null tiene que fallar");
    }

    // ---- javax.xml.stream.XMLStreamConstants --------------------------------------------------

    static void constantes() {
        eqi(XMLStreamConstants.START_ELEMENT, 1, "START_ELEMENT");
        eqi(XMLStreamConstants.END_ELEMENT, 2, "END_ELEMENT");
        eqi(XMLStreamConstants.PROCESSING_INSTRUCTION, 3, "PROCESSING_INSTRUCTION");
        eqi(XMLStreamConstants.CHARACTERS, 4, "CHARACTERS");
        eqi(XMLStreamConstants.COMMENT, 5, "COMMENT");
        eqi(XMLStreamConstants.SPACE, 6, "SPACE");
        eqi(XMLStreamConstants.START_DOCUMENT, 7, "START_DOCUMENT");
        eqi(XMLStreamConstants.END_DOCUMENT, 8, "END_DOCUMENT");
        eqi(XMLStreamConstants.ENTITY_REFERENCE, 9, "ENTITY_REFERENCE");
        eqi(XMLStreamConstants.ATTRIBUTE, 10, "ATTRIBUTE");
        eqi(XMLStreamConstants.DTD, 11, "DTD");
        eqi(XMLStreamConstants.CDATA, 12, "CDATA");
        eqi(XMLStreamConstants.NAMESPACE, 13, "NAMESPACE");
        eqi(XMLStreamConstants.NOTATION_DECLARATION, 14, "NOTATION_DECLARATION");
        eqi(XMLStreamConstants.ENTITY_DECLARATION, 15, "ENTITY_DECLARATION");
    }

    // ---- javax.xml.datatype.Duration -----------------------------------------------------------

    static void duracion() throws Exception {
        DatatypeFactory f = DatatypeFactory.newInstance();
        Duration d = f.newDuration("P1Y2M3DT4H5M6S");
        eqi(d.getYears(), 1, "Duration.getYears");
        eqi(d.getMonths(), 2, "Duration.getMonths");
        eqi(d.getDays(), 3, "Duration.getDays");
        eqi(d.getHours(), 4, "Duration.getHours");
        eqi(d.getMinutes(), 5, "Duration.getMinutes");
        eqi(d.getSeconds(), 6, "Duration.getSeconds");
        eqi(d.getSign(), 1, "Duration.getSign");
        eq(d.toString(), "P1Y2M3DT4H5M6S", "Duration.toString es la forma lexica");
        check(d.equals(f.newDuration(true, 1, 2, 3, 4, 5, 6)), "Duration.equals por campos");

        eqi(f.newDuration("-P1D").getSign(), -1, "una duracion negativa");
        eq(f.newDuration("P1Y").add(f.newDuration("P1M")).toString(), "P1Y1M", "Duration.add");
        eq(f.newDuration("P1Y").negate().toString(), "-P1Y", "Duration.negate");
        eq(f.newDuration("P2Y").multiply(2).toString(), "P4Y", "Duration.multiply");
        eqi(f.newDuration("P1M").compare(f.newDuration("P30D")),
                DatatypeConstants.INDETERMINATE, "P1M contra P30D es indeterminado");
        eqi(f.newDuration("P1Y").compare(f.newDuration("P12M")),
                DatatypeConstants.EQUAL, "P1Y es P12M");
        check(f.newDuration("PT25H").isLongerThan(f.newDuration("P1D")), "PT25H > P1D");
        eqi(f.newDurationDayTime(86400000L).getDays(), 1, "newDurationDayTime de un dia");
        eq(f.newDuration("P1Y2M3DT4H5M6S").getXMLSchemaType(), DatatypeConstants.DURATION,
                "el tipo de esquema de una duracion completa");
    }

    // ---- javax.xml.datatype.XMLGregorianCalendar ------------------------------------------------

    static void calendario() throws Exception {
        DatatypeFactory f = DatatypeFactory.newInstance();
        XMLGregorianCalendar c = f.newXMLGregorianCalendar("2024-05-25T12:00:00-03:00");
        eqi(c.getYear(), 2024, "getYear");
        eqi(c.getMonth(), 5, "getMonth");
        eqi(c.getDay(), 25, "getDay");
        eqi(c.getHour(), 12, "getHour");
        eqi(c.getMinute(), 0, "getMinute");
        eqi(c.getSecond(), 0, "getSecond");
        eqi(c.getTimezone(), -180, "getTimezone en minutos");
        eq(c.toXMLFormat(), "2024-05-25T12:00:00-03:00", "toXMLFormat");
        eq(c.getXMLSchemaType(), DatatypeConstants.DATETIME, "getXMLSchemaType");
        eq(c.normalize().toXMLFormat(), "2024-05-25T15:00:00Z", "normalize lleva a UTC");

        XMLGregorianCalendar fecha = f.newXMLGregorianCalendar("2024-05-25");
        eq(fecha.getXMLSchemaType(), DatatypeConstants.DATE, "una fecha sola es xs:date");
        eqi(fecha.getTimezone(), DatatypeConstants.FIELD_UNDEFINED, "sin zona horaria");
        eqi(c.compare(f.newXMLGregorianCalendar("2024-05-25T15:00:00Z")),
                DatatypeConstants.EQUAL, "el mismo instante en dos zonas");
        eqi(c.compare(f.newXMLGregorianCalendar("2024-05-25T16:00:00Z")),
                DatatypeConstants.LESSER, "una hora despues es mayor");
    }

    // ---- el lector de cursor ---------------------------------------------------------------------

    static void cursor() throws Exception {
        XMLInputFactory f = XMLInputFactory.newInstance();
        f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        XMLStreamReader r = f.createXMLStreamReader(new StringReader(DOC));

        eqi(r.getEventType(), XMLStreamConstants.START_DOCUMENT, "arranca en START_DOCUMENT");
        eq(r.getVersion(), "1.0", "la version declarada");
        eq(r.getCharacterEncodingScheme(), "UTF-8", "la codificacion declarada");

        eqi(r.next(), XMLStreamConstants.COMMENT, "el comentario del prologo");
        eq(r.getText(), " c ", "el texto del comentario");

        eqi(r.next(), XMLStreamConstants.START_ELEMENT, "la raiz");
        eq(r.getLocalName(), "r", "el nombre de la raiz");
        eq(r.getNamespaceURI(), "urn:d", "la raiz esta en el espacio por omision");
        eq(r.getPrefix(), null, "la raiz no tiene prefijo");
        eqi(r.getAttributeCount(), 2, "dos atributos, sin contar los xmlns");
        eqi(r.getNamespaceCount(), 2, "dos declaraciones de espacio de nombres");
        eq(r.getAttributeValue("urn:a", "x"), "1", "el atributo calificado");
        eq(r.getAttributeValue(null, "y"), "2", "el atributo sin calificar, sin mirar el espacio");
        eq(r.getAttributeValue("", "y"), "2", "un atributo sin prefijo no esta en el espacio por omision");
        eq(r.getAttributeValue("urn:d", "y"), null, "y no esta en urn:d");
        eq(r.getNamespaceURI("a"), "urn:a", "resolver el prefijo a");
        eq(r.getName(), new QName("urn:d", "r"), "getName de la raiz");

        eqi(r.nextTag(), XMLStreamConstants.START_ELEMENT, "el primer hijo");
        eq(r.getLocalName(), "p", "se llama p");
        eq(r.getElementText(), "hola & chau", "getElementText resuelve la entidad");
        eqi(r.getEventType(), XMLStreamConstants.END_ELEMENT, "getElementText deja en END_ELEMENT");

        eqi(r.nextTag(), XMLStreamConstants.START_ELEMENT, "el elemento vacio");
        eq(r.getLocalName(), "q", "se llama q");
        eq(r.getNamespaceURI(), "urn:a", "q esta en urn:a");
        eq(r.getPrefix(), "a", "q se escribio con prefijo a");
        eqi(r.getNamespaceCount(), 0, "q no declara nada");
        eqi(r.nextTag(), XMLStreamConstants.END_ELEMENT, "el cierre del elemento vacio");
        eq(r.getLocalName(), "q", "cierra q");

        eqi(r.next(), XMLStreamConstants.CHARACTERS, "el CDATA se junta como texto");
        check(r.getText().indexOf("<crudo>") >= 0, "el CDATA entrega su contenido crudo");
        check(r.getText().indexOf("CDATA") < 0, "sin la envoltura");

        eqi(r.next(), XMLStreamConstants.END_ELEMENT, "cierra la raiz");
        eq(r.getLocalName(), "r", "cierra r");
        eqi(r.getNamespaceCount(), 2, "los dos espacios salen de alcance aca");
        eqi(r.next(), XMLStreamConstants.END_DOCUMENT, "el final");
        check(!r.hasNext(), "despues del final no hay mas");
        r.close();

        // Un documento mal formado tiene que fallar, no pasar de largo.
        boolean tiro = false;
        try {
            XMLStreamReader malo = f.createXMLStreamReader(new StringReader("<a><b></a>"));
            while (malo.hasNext()) {
                malo.next();
            }
        } catch (Exception e) {
            tiro = true;
        }
        check(tiro, "un cierre que no corresponde tiene que fallar");

        tiro = false;
        try {
            XMLStreamReader malo = f.createXMLStreamReader(new StringReader("<a>&nada;</a>"));
            while (malo.hasNext()) {
                malo.next();
            }
        } catch (Exception e) {
            tiro = true;
        }
        check(tiro, "una entidad no declarada tiene que fallar");
    }

    // ---- el escritor de cursor --------------------------------------------------------------------

    static void escritor() throws Exception {
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter w = f.createXMLStreamWriter(sw);
        w.writeStartElement("r");
        w.writeAttribute("a", "1<2");
        w.writeCharacters("x & y");
        w.writeEndElement();
        w.flush();
        eq(sw.toString(), "<r a=\"1&lt;2\">x &amp; y</r>", "el escritor escapa lo que hay que escapar");

        sw = new StringWriter();
        w = f.createXMLStreamWriter(sw);
        w.writeStartElement("r");
        w.writeEmptyElement("v");
        w.writeEndElement();
        w.flush();
        eq(sw.toString(), "<r><v/></r>", "un elemento vacio se cierra solo");

        sw = new StringWriter();
        w = f.createXMLStreamWriter(sw);
        w.writeStartElement("p", "e", "urn:z");
        w.writeNamespace("p", "urn:z");
        w.writeEndElement();
        w.flush();
        eq(sw.toString(), "<p:e xmlns:p=\"urn:z\"></p:e>", "el prefijo declarado a mano");

        // Lo que se escribe se tiene que poder volver a leer.
        sw = new StringWriter();
        w = f.createXMLStreamWriter(sw);
        w.writeStartElement("raiz");
        w.writeAttribute("k", "v & w");
        w.writeCharacters("<texto>");
        w.writeEndElement();
        w.flush();
        XMLStreamReader r = XMLInputFactory.newInstance()
                .createXMLStreamReader(new StringReader(sw.toString()));
        r.nextTag();
        eq(r.getLocalName(), "raiz", "ida y vuelta: el nombre");
        eq(r.getAttributeValue(null, "k"), "v & w", "ida y vuelta: el atributo");
        eq(r.getElementText(), "<texto>", "ida y vuelta: el texto");
    }

    // ---- el lector de eventos -----------------------------------------------------------------------

    static void eventos() throws Exception {
        XMLInputFactory f = XMLInputFactory.newInstance();
        f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        XMLEventReader r = f.createXMLEventReader(new StringReader(DOC));

        XMLEvent e = r.nextEvent();
        check(e.isStartDocument(), "el primer evento es el comienzo del documento");
        eqi(e.getEventType(), XMLStreamConstants.START_DOCUMENT, "y su tipo lo dice");

        e = r.nextEvent();
        eqi(e.getEventType(), XMLStreamConstants.COMMENT, "despues el comentario");

        XMLEvent mirado = r.peek();
        check(mirado.isStartElement(), "peek ve la raiz");
        e = r.nextEvent();
        check(e == mirado || e.getEventType() == mirado.getEventType(),
                "peek no consume: el proximo es el que se habia visto");
        StartElement se = e.asStartElement();
        eq(se.getName(), new QName("urn:d", "r"), "el nombre del evento de apertura");
        eq(se.getNamespaceURI("a"), "urn:a", "el contexto del evento resuelve el prefijo");

        Attribute at = se.getAttributeByName(new QName("urn:a", "x"));
        check(at != null, "getAttributeByName encuentra el atributo calificado");
        if (at != null) {
            eq(at.getValue(), "1", "y su valor");
            check(at.isAttribute(), "un atributo es un evento de atributo");
        }
        check(se.getAttributeByName(new QName("urn:d", "x")) == null,
                "y no lo encuentra en otro espacio de nombres");

        int cuentaAtributos = 0;
        java.util.Iterator it = se.getAttributes();
        while (it.hasNext()) {
            it.next();
            cuentaAtributos++;
        }
        eqi(cuentaAtributos, 2, "getAttributes no incluye las declaraciones xmlns");

        int cuentaNs = 0;
        it = se.getNamespaces();
        while (it.hasNext()) {
            it.next();
            cuentaNs++;
        }
        eqi(cuentaNs, 2, "getNamespaces trae las dos declaraciones");

        e = r.nextEvent();
        check(e.isStartElement(), "el hijo p");
        eq(r.getElementText(), "hola & chau", "getElementText del lector de eventos");

        // Hasta el final, contando lo que aparece.
        int cierres = 0;
        int textos = 0;
        while (r.hasNext()) {
            e = r.nextEvent();
            if (e.isEndElement()) {
                cierres++;
            } else if (e.isCharacters()) {
                textos++;
                Characters ch = e.asCharacters();
                check(!ch.isIgnorableWhiteSpace(),
                        "sin DTD nada se puede declarar espacio ignorable");
            }
        }
        eqi(cierres, 2, "quedaban el cierre de q y el de la raiz");
        eqi(textos, 1, "y un solo tramo de texto");
        r.close();
    }

    // ---- la fabrica de eventos ------------------------------------------------------------------------

    static void fabricaDeEventos() throws Exception {
        XMLEventFactory f = XMLEventFactory.newInstance();

        Characters ch = f.createCharacters("hola");
        eq(ch.getData(), "hola", "createCharacters");
        eqi(ch.getEventType(), XMLStreamConstants.CHARACTERS, "y es texto comun");
        check(!ch.isCData(), "no es CDATA");
        check(ch.isCharacters(), "isCharacters");
        StringWriter sw = new StringWriter();
        ch.writeAsEncodedUnicode(sw);
        eq(sw.toString(), "hola", "writeAsEncodedUnicode de texto sin nada que escapar");

        Characters cd = f.createCData("x");
        eqi(cd.getEventType(), XMLStreamConstants.CDATA, "createCData");
        check(cd.isCData(), "y lo dice");
        check(cd.isCharacters(), "un CDATA tambien es texto");

        Characters sp = f.createIgnorableSpace("  ");
        eqi(sp.getEventType(), XMLStreamConstants.SPACE, "createIgnorableSpace");
        check(sp.isIgnorableWhiteSpace(), "el espacio ignorable lo dice");
        check(sp.isWhiteSpace(), "y ademas es espacio");

        Attribute at = f.createAttribute("k", "v");
        eq(at.getName(), new QName("k"), "createAttribute sin espacio de nombres");
        eq(at.getValue(), "v", "y su valor");
        eqi(at.getEventType(), XMLStreamConstants.ATTRIBUTE, "es un evento de atributo");
        sw = new StringWriter();
        at.writeAsEncodedUnicode(sw);
        eq(sw.toString(), "k=\"v\"", "writeAsEncodedUnicode de un atributo");

        javax.xml.stream.events.Namespace ns = f.createNamespace("urn:z");
        check(ns.isDefaultNamespaceDeclaration(), "createNamespace sin prefijo es la de por omision");
        eq(ns.getNamespaceURI(), "urn:z", "y su URI");
        eq(ns.getPrefix(), "", "sin prefijo");
        eqi(ns.getEventType(), XMLStreamConstants.NAMESPACE, "es un evento de espacio de nombres");

        javax.xml.stream.events.Namespace np = f.createNamespace("p", "urn:z");
        check(!np.isDefaultNamespaceDeclaration(), "con prefijo no es la de por omision");
        eq(np.getPrefix(), "p", "y el prefijo se guarda");

        javax.xml.stream.events.EndDocument ed = f.createEndDocument();
        check(ed.isEndDocument(), "createEndDocument");
        eqi(ed.getEventType(), XMLStreamConstants.END_DOCUMENT, "y su tipo");
    }

    // ---- los filtros -------------------------------------------------------------------------------------

    static void filtrado() throws Exception {
        XMLInputFactory f = XMLInputFactory.newInstance();
        XMLEventReader base = f.createXMLEventReader(new StringReader(DOC));
        XMLEventReader soloAperturas = f.createFilteredReader(base, new SoloAperturas());
        int n = 0;
        while (soloAperturas.hasNext()) {
            XMLEvent e = soloAperturas.nextEvent();
            check(e.isStartElement(), "el filtro solo deja pasar aperturas");
            n++;
        }
        eqi(n, 3, "hay tres elementos: r, p y q");
    }

    static class SoloAperturas implements EventFilter {
        public boolean accept(XMLEvent event) {
            return event.isStartElement();
        }
    }

    public static void main(String[] args) {
        System.out.println("StaxTest -> " + run());
    }
}
