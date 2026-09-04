import java.beans.XMLEncoder;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

// Volcador diferencial de java.beans.XMLEncoder.
//
// Corre igual en las dos VMs: en la nuestra usa nuestro XMLEncoder, en la JVM real usa el del JDK
// (el classpath no puede aportar clases java.*). Lo que imprime se compara byte a byte; la unica
// linea que no se puede comparar es la del `<java version=...>`, que dice la version de cada VM.

public class KajiBeansXmlDump {

    static String xml(Object o) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        XMLEncoder e = new XMLEncoder(b);
        e.writeObject(o);
        e.close();
        return normalizar(new String(b.toByteArray()));
    }

    // Saca la linea del preambulo que lleva la version de la VM, y unifica fines de linea.
    static String normalizar(String s) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            int fin = s.indexOf('\n', i);
            if (fin < 0) { fin = s.length(); }
            String linea = s.substring(i, fin);
            if (linea.length() > 0 && linea.charAt(linea.length() - 1) == '\r') {
                linea = linea.substring(0, linea.length() - 1);
            }
            if (linea.indexOf("<java version=") < 0) {
                sb.append(linea).append('\n');
            } else {
                sb.append("<java VERSION class=\"java.beans.XMLDecoder\">").append('\n');
            }
            i = fin + 1;
        }
        return sb.toString();
    }

    static void caso(String nombre, Object o) {
        System.out.println("##### " + nombre);
        System.out.print(xml(o));
    }

    public static int run() throws Exception {
        caso("1-defaults", new XPunto());

        XPunto p = new XPunto();
        p.setX(3);
        p.setEtiqueta("hola");
        caso("2-parcial", p);

        caso("3-intArray", new int[] { 0, 7, 0 });

        caso("4-stringArray", new String[] { "a", null, "b" });

        List<Object> l = new ArrayList<Object>();
        l.add("uno");
        l.add(Integer.valueOf(2));
        l.add(Boolean.TRUE);
        caso("5-lista", l);

        // Aliasing: el MISMO punto en los dos lados de la caja tiene que salir una vez con id y
        // una vez como idref. Si saliera dos veces entero, el grafo leido tendria dos objetos.
        XPunto compartido = new XPunto();
        compartido.setX(9);
        XCaja caja = new XCaja();
        caja.setA(compartido);
        caja.setB(compartido);
        caso("6-aliasing", caja);

        // Dos puntos iguales pero distintos: tienen que salir los dos enteros, sin idref.
        XPunto q1 = new XPunto();
        q1.setX(9);
        XPunto q2 = new XPunto();
        q2.setX(9);
        XCaja caja2 = new XCaja();
        caja2.setA(q1);
        caja2.setB(q2);
        caso("7-iguales-distintos", caja2);

        caso("8-escapes", "a<b>c&d\"e'f");

        XVarios v = new XVarios();
        v.setBandera(true);
        v.setGrande(123456789012L);
        v.setLetra('<');
        v.setReal(2.5);
        v.setNulo(null);
        caso("9-primitivos", v);

        caso("10-anidado", cajaCon(1, 2));

        // Lista que se contiene a si misma: el recorrido no puede entrar en bucle.
        List<Object> ciclo = new ArrayList<Object>();
        ciclo.add("x");
        ciclo.add(ciclo);
        caso("11-ciclo", ciclo);

        caso("12-clase", String.class);

        caso("13-cadena-suelta", "simple");

        caso("14-array-de-arrays", new int[][] { { 1 }, { 2, 3 } });

        // Mutable Y con equals declarado. Si `mutatesTo` le creyera a `equals` para un objeto que
        // se construye sin argumentos, esto no terminaria nunca: la bolsa vacia recien creada
        // jamas es igual a la que tiene contenido.
        XBolsa bolsa = new XBolsa();
        bolsa.setContenido("algo");
        caso("15-mutable-con-equals", bolsa);

        java.util.Map<String, Object> mapa = new java.util.HashMap<String, Object>();
        mapa.put("k", "v");
        caso("16-mapa-una-entrada", mapa);

        XIndexado idx = new XIndexado();
        idx.setPartes(1, "z");
        caso("17-indexada", idx);

        // Constructor de cuatro argumentos: sin declaracion XML y con sangria inicial 2.
        java.io.ByteArrayOutputStream b18 = new java.io.ByteArrayOutputStream();
        XMLEncoder e18 = new XMLEncoder(b18, "UTF-8", false, 2);
        e18.writeObject(p);
        e18.close();
        System.out.println("##### 18-sin-declaracion-sangria2");
        System.out.print(normalizar(new String(b18.toByteArray())));

        // El owner: el documento le habla al objeto que el decodificador va a enchufar despues.
        java.io.ByteArrayOutputStream b19 = new java.io.ByteArrayOutputStream();
        XMLEncoder e19 = new XMLEncoder(b19);
        XDuenio duenio = new XDuenio();
        e19.setOwner(duenio);
        e19.writeStatement(new java.beans.Statement(duenio, "hacer", new Object[0]));
        e19.close();
        System.out.println("##### 19-owner");
        System.out.print(normalizar(new String(b19.toByteArray())));

        // Dos escrituras en el mismo codificador: la segunda no puede referirse por id a un objeto
        // de la primera, porque flush limpia los enlaces.
        java.io.ByteArrayOutputStream b20 = new java.io.ByteArrayOutputStream();
        XMLEncoder e20 = new XMLEncoder(b20);
        XPunto rep = new XPunto();
        rep.setX(1);
        e20.writeObject(rep);
        e20.flush();
        e20.writeObject(rep);
        e20.close();
        System.out.println("##### 20-dos-escrituras");
        System.out.print(normalizar(new String(b20.toByteArray())));

        // UTF-8 de verdad, comparado byte a byte. Es la unica forma de ver que el archivo dice la
        // verdad sobre su propia codificacion.
        java.io.ByteArrayOutputStream b21 = new java.io.ByteArrayOutputStream();
        XMLEncoder e21 = new XMLEncoder(b21);
        e21.writeObject("ñ€");
        e21.close();
        System.out.println("##### 21-utf8-hex");
        System.out.println(hexDeLinea(b21.toByteArray()));

        return -1;
    }

    // Los bytes de la linea que contiene el texto no ASCII, en hexadecimal.
    static String hexDeLinea(byte[] r) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < r.length; i++) {
            int c = r[i] & 0xff;
            if (c > 127) { sb.append(Integer.toHexString(c)).append(' '); }
        }
        return "no-ascii: " + sb.toString().trim();
    }

    static XCaja cajaCon(int a, int b) {
        XPunto p1 = new XPunto();
        p1.setX(a);
        XPunto p2 = new XPunto();
        p2.setY(b);
        XCaja c = new XCaja();
        c.setA(p1);
        c.setB(p2);
        return c;
    }

    public static void main(String[] x) throws Exception { run(); }
}
