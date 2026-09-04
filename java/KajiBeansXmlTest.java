import java.beans.Statement;
import java.beans.XMLEncoder;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Prueba diferencial de java.beans.XMLEncoder.
//
// La gracia es la misma que la de KajiBeansIntrospeccionTest: este MISMO archivo corre en las dos
// VMs, y en la JVM real usa el XMLEncoder del JDK porque el classpath no puede aportar clases
// java.*. El texto esperado que esta mas abajo es, literalmente, lo que imprime el JDK 25. Que las
// dos VMs devuelvan -1 quiere decir que nuestro codificador produce los MISMOS BYTES que el del
// JDK para los veintiun grafos de la bateria, no algo parecido.
//
// Los beans son clases anidadas publicas y no archivos aparte por dos razones que se cruzan: el
// XMLEncoder del JDK solo introspecciona clases publicas, y la suite compila cada Test.java solo,
// asi que un tipo en otro archivo no resuelve al generar bytecode.
//
// Cada caso de la bateria esta por algo:
//   - 1/2      el bean minimo: lo que NO se escribe (los valores por defecto) importa tanto como
//              lo que si.
//   - 3/4/14   arreglos, incluso de arreglos, y los huecos que no se emiten.
//   - 5/16     colecciones y mapas, cuyo contenido no vive en ninguna propiedad.
//   - 6/7      aliasing: el mismo objeto dos veces sale con id/idref; dos objetos iguales pero
//              distintos salen enteros los dos. Confundirlos cambia el grafo.
//   - 11       una lista que se contiene a si misma.
//   - 15       un objeto MUTABLE que ademas declara equals: destapa la regla de `mutatesTo`.
//   - 19       el owner.
//   - 20       dos escrituras seguidas: la segunda no puede referirse a la primera.
//   - 21       UTF-8 comparado byte a byte.
public class KajiBeansXmlTest {

    public static class Punto {
        private int x;
        private int y;
        private String etiqueta;
        public int getX() { return x; }
        public void setX(int v) { x = v; }
        public int getY() { return y; }
        public void setY(int v) { y = v; }
        public String getEtiqueta() { return etiqueta; }
        public void setEtiqueta(String v) { etiqueta = v; }
    }

    public static class Caja {
        private Punto a;
        private Punto b;
        public Punto getA() { return a; }
        public void setA(Punto v) { a = v; }
        public Punto getB() { return b; }
        public void setB(Punto v) { b = v; }
    }

    public static class Varios {
        private boolean bandera;
        private long grande;
        private char letra;
        private double real;
        private Object nulo = "algo";
        public boolean isBandera() { return bandera; }
        public void setBandera(boolean v) { bandera = v; }
        public long getGrande() { return grande; }
        public void setGrande(long v) { grande = v; }
        public char getLetra() { return letra; }
        public void setLetra(char v) { letra = v; }
        public double getReal() { return real; }
        public void setReal(double v) { real = v; }
        public Object getNulo() { return nulo; }
        public void setNulo(Object v) { nulo = v; }
    }

    // Mutable y con equals declarado a la vez. Si el codificador le creyera a `equals` para decidir
    // si el objeto que ya tiene armado sirve, la bolsa vacia recien creada nunca seria igual a la
    // que tiene contenido y volveria a crearla sin parar.
    public static class Bolsa {
        private String contenido = "";
        public String getContenido() { return contenido; }
        public void setContenido(String v) { contenido = v; }
        public boolean equals(Object o) {
            return o instanceof Bolsa && ((Bolsa) o).contenido.equals(this.contenido);
        }
        public int hashCode() { return contenido.hashCode(); }
    }

    public static class Indexado {
        private String[] partes = new String[] { "a", "b" };
        public String[] getPartes() { return partes; }
        public void setPartes(String[] v) { partes = v; }
        public String getPartes(int i) { return partes[i]; }
        public void setPartes(int i, String v) { partes[i] = v; }
    }

    public static class Duenio {
        private int veces;
        public int getVeces() { return veces; }
        public void setVeces(int v) { veces = v; }
        public void hacer() { veces++; }
    }

    static StringBuilder salida;

    static void linea(String s) { salida.append(s).append('\n'); }

    static void caso(String nombre, Object o) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        XMLEncoder e = new XMLEncoder(b);
        e.writeObject(o);
        e.close();
        linea("##### " + nombre);
        volcar(b.toByteArray());
    }

    // Normaliza fines de linea y tapa la version de la VM, que es lo unico del preambulo que no
    // puede ser igual en las dos.
    static void volcar(byte[] bytes) {
        String s = new String(bytes);
        int i = 0;
        while (i < s.length()) {
            int fin = s.indexOf('\n', i);
            if (fin < 0) { fin = s.length(); }
            String l = s.substring(i, fin);
            if (l.length() > 0 && l.charAt(l.length() - 1) == '\r') {
                l = l.substring(0, l.length() - 1);
            }
            linea(l.indexOf("<java version=") < 0
                ? l
                : "<java VERSION class=\"java.beans.XMLDecoder\">");
            i = fin + 1;
        }
    }

    static Caja cajaCon(int a, int b) {
        Punto p1 = new Punto();
        p1.setX(a);
        Punto p2 = new Punto();
        p2.setY(b);
        Caja c = new Caja();
        c.setA(p1);
        c.setB(p2);
        return c;
    }

    static String transcripcion() throws Exception {
        salida = new StringBuilder();

        caso("1-defaults", new Punto());

        Punto p = new Punto();
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

        Punto compartido = new Punto();
        compartido.setX(9);
        Caja caja = new Caja();
        caja.setA(compartido);
        caja.setB(compartido);
        caso("6-aliasing", caja);

        Punto q1 = new Punto();
        q1.setX(9);
        Punto q2 = new Punto();
        q2.setX(9);
        Caja caja2 = new Caja();
        caja2.setA(q1);
        caja2.setB(q2);
        caso("7-iguales-distintos", caja2);

        caso("8-escapes", "a<b>c&d\"e'f");

        Varios v = new Varios();
        v.setBandera(true);
        v.setGrande(123456789012L);
        v.setLetra('<');
        v.setReal(2.5);
        v.setNulo(null);
        caso("9-primitivos", v);

        caso("10-anidado", cajaCon(1, 2));

        List<Object> ciclo = new ArrayList<Object>();
        ciclo.add("x");
        ciclo.add(ciclo);
        caso("11-ciclo", ciclo);

        caso("12-clase", String.class);
        caso("13-cadena-suelta", "simple");
        caso("14-array-de-arrays", new int[][] { { 1 }, { 2, 3 } });

        Bolsa bolsa = new Bolsa();
        bolsa.setContenido("algo");
        caso("15-mutable-con-equals", bolsa);

        Map<String, Object> mapa = new HashMap<String, Object>();
        mapa.put("k", "v");
        caso("16-mapa-una-entrada", mapa);

        Indexado idx = new Indexado();
        idx.setPartes(1, "z");
        caso("17-indexada", idx);

        ByteArrayOutputStream b18 = new ByteArrayOutputStream();
        XMLEncoder e18 = new XMLEncoder(b18, "UTF-8", false, 2);
        e18.writeObject(p);
        e18.close();
        linea("##### 18-sin-declaracion-sangria2");
        volcar(b18.toByteArray());

        ByteArrayOutputStream b19 = new ByteArrayOutputStream();
        XMLEncoder e19 = new XMLEncoder(b19);
        Duenio duenio = new Duenio();
        e19.setOwner(duenio);
        e19.writeStatement(new Statement(duenio, "hacer", new Object[0]));
        e19.close();
        linea("##### 19-owner");
        volcar(b19.toByteArray());

        ByteArrayOutputStream b20 = new ByteArrayOutputStream();
        XMLEncoder e20 = new XMLEncoder(b20);
        Punto rep = new Punto();
        rep.setX(1);
        e20.writeObject(rep);
        e20.flush();
        e20.writeObject(rep);
        e20.close();
        linea("##### 20-dos-escrituras");
        volcar(b20.toByteArray());

        ByteArrayOutputStream b21 = new ByteArrayOutputStream();
        XMLEncoder e21 = new XMLEncoder(b21);
        e21.writeObject("ñ€");
        e21.close();
        linea("##### 21-utf8-hex");
        byte[] r21 = b21.toByteArray();
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < r21.length; i++) {
            int c = r21[i] & 0xff;
            if (c > 127) { hex.append(Integer.toHexString(c)).append(' '); }
        }
        linea("no-ascii: " + hex.toString().trim());

        return salida.toString();
    }

    public static int run() throws Exception {
        String real = transcripcion();
        String esperado = esperado();
        if (real.equals(esperado)) {
            return -1;
        }
        // Decir CUAL linea difiere; un diff de doscientas lineas no sirve de nada.
        String[] a = esperado.split("\n", -1);
        String[] b = real.split("\n", -1);
        int i = 0;
        while (i < a.length && i < b.length && a[i].equals(b[i])) { i++; }
        System.out.println("XMLEncoder difiere en la linea " + (i + 1));
        System.out.println("  esperado: " + (i < a.length ? a[i] : "<fin>"));
        System.out.println("  obtenido: " + (i < b.length ? b[i] : "<fin>"));
        return i + 1;
    }

    // Lo que imprime el java.beans.XMLEncoder del JDK 25 para esta misma bateria.
    static String esperado() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ESPERADO.length; i++) { sb.append(ESPERADO[i]).append('\n'); }
        return sb.toString();
    }

    static final String[] ESPERADO = {
        "##### 1-defaults",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<java VERSION class=\"java.beans.XMLDecoder\">",
        " <object class=\"KajiBeansXmlTest$Punto\"/>",
        "</java>",
        "##### 2-parcial",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<java VERSION class=\"java.beans.XMLDecoder\">",
        " <object class=\"KajiBeansXmlTest$Punto\">",
        "  <void property=\"etiqueta\">",
        "   <string>hola</string>",
        "  </void>",
        "  <void property=\"x\">",
        "   <int>3</int>",
        "  </void>",
        " </object>",
        "</java>",
        "##### 3-intArray",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<java VERSION class=\"java.beans.XMLDecoder\">",
        " <array class=\"int\" length=\"3\">",
        "  <void index=\"1\">",
        "   <int>7</int>",
        "  </void>",
        " </array>",
        "</java>",
        "##### 4-stringArray",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<java VERSION class=\"java.beans.XMLDecoder\">",
        " <array class=\"java.lang.String\" length=\"3\">",
        "  <void index=\"0\">",
        "   <string>a</string>",
        "  </void>",
        "  <void index=\"2\">",
        "   <string>b</string>",
        "  </void>",
        " </array>",
        "</java>",
        "##### 5-lista",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<java VERSION class=\"java.beans.XMLDecoder\">",
        " <object class=\"java.util.ArrayList\">",
        "  <void method=\"add\">",
        "   <string>uno</string>",
        "  </void>",
        "  <void method=\"add\">",
        "   <int>2</int>",
        "  </void>",
        "  <void method=\"add\">",
        "   <boolean>true</boolean>",
        "  </void>",
        " </object>",
        "</java>",
        "##### 6-aliasing",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<java VERSION class=\"java.beans.XMLDecoder\">",
        " <object class=\"KajiBeansXmlTest$Caja\">",
        "  <void property=\"a\">",
        "   <object class=\"KajiBeansXmlTest$Punto\" id=\"KajiBeansXmlTest$Punto0\">",
        "    <void property=\"x\">",
        "     <int>9</int>",
        "    </void>",
        "   </object>",
        "  </void>",
        "  <void property=\"b\">",
        "   <object idref=\"KajiBeansXmlTest$Punto0\"/>",
        "  </void>",
        " </object>",
        "</java>",
        "##### 7-iguales-distintos",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<java VERSION class=\"java.beans.XMLDecoder\">",
        " <object class=\"KajiBeansXmlTest$Caja\">",
        "  <void property=\"a\">",
        "   <object class=\"KajiBeansXmlTest$Punto\">",
        "    <void property=\"x\">",
        "     <int>9</int>",
        "    </void>",
        "   </object>",
        "  </void>",
        "  <void property=\"b\">",
        "   <object class=\"KajiBeansXmlTest$Punto\">",
        "    <void property=\"x\">",
        "     <int>9</int>",
        "    </void>",
        "   </object>",
        "  </void>",
        " </object>",
        "</java>",
        "##### 8-escapes",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<java VERSION class=\"java.beans.XMLDecoder\">",
        " <string>a&lt;b&gt;c&amp;d&quot;e&apos;f</string>",
        "</java>",
        "##### 9-primitivos",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<java VERSION class=\"java.beans.XMLDecoder\">",
        " <object class=\"KajiBeansXmlTest$Varios\">",
        "  <void property=\"bandera\">",
        "   <boolean>true</boolean>",
        "  </void>",
        "  <void property=\"grande\">",
        "   <long>123456789012</long>",
        "  </void>",
        "  <void property=\"letra\">",
        "   <char>&lt;</char>",
        "  </void>",
        "  <void property=\"nulo\">",
        "   <null/>",
        "  </void>",
        "  <void property=\"real\">",
        "   <double>2.5</double>",
        "  </void>",
        " </object>",
        "</java>",
        "##### 10-anidado",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<java VERSION class=\"java.beans.XMLDecoder\">",
        " <object class=\"KajiBeansXmlTest$Caja\">",
        "  <void property=\"a\">",
        "   <object class=\"KajiBeansXmlTest$Punto\">",
        "    <void property=\"x\">",
        "     <int>1</int>",
        "    </void>",
        "   </object>",
        "  </void>",
        "  <void property=\"b\">",
        "   <object class=\"KajiBeansXmlTest$Punto\">",
        "    <void property=\"y\">",
        "     <int>2</int>",
        "    </void>",
        "   </object>",
        "  </void>",
        " </object>",
        "</java>",
        "##### 11-ciclo",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<java VERSION class=\"java.beans.XMLDecoder\">",
        " <object class=\"java.util.ArrayList\" id=\"ArrayList0\">",
        "  <void method=\"add\">",
        "   <string>x</string>",
        "  </void>",
        "  <void method=\"add\">",
        "   <object idref=\"ArrayList0\"/>",
        "  </void>",
        " </object>",
        "</java>",
        "##### 12-clase",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<java VERSION class=\"java.beans.XMLDecoder\">",
        " <class>java.lang.String</class>",
        "</java>",
        "##### 13-cadena-suelta",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<java VERSION class=\"java.beans.XMLDecoder\">",
        " <string>simple</string>",
        "</java>",
        "##### 14-array-de-arrays",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<java VERSION class=\"java.beans.XMLDecoder\">",
        " <array class=\"[I\" length=\"2\">",
        "  <void index=\"0\">",
        "   <array class=\"int\" length=\"1\">",
        "    <void index=\"0\">",
        "     <int>1</int>",
        "    </void>",
        "   </array>",
        "  </void>",
        "  <void index=\"1\">",
        "   <array class=\"int\" length=\"2\">",
        "    <void index=\"0\">",
        "     <int>2</int>",
        "    </void>",
        "    <void index=\"1\">",
        "     <int>3</int>",
        "    </void>",
        "   </array>",
        "  </void>",
        " </array>",
        "</java>",
        "##### 15-mutable-con-equals",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<java VERSION class=\"java.beans.XMLDecoder\">",
        " <object class=\"KajiBeansXmlTest$Bolsa\">",
        "  <void property=\"contenido\">",
        "   <string>algo</string>",
        "  </void>",
        " </object>",
        "</java>",
        "##### 16-mapa-una-entrada",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<java VERSION class=\"java.beans.XMLDecoder\">",
        " <object class=\"java.util.HashMap\">",
        "  <void method=\"put\">",
        "   <string>k</string>",
        "   <string>v</string>",
        "  </void>",
        " </object>",
        "</java>",
        "##### 17-indexada",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<java VERSION class=\"java.beans.XMLDecoder\">",
        " <object class=\"KajiBeansXmlTest$Indexado\" id=\"KajiBeansXmlTest$Indexado0\">",
        "  <void property=\"partes\">",
        "   <void index=\"1\">",
        "    <string>z</string>",
        "   </void>",
        "  </void>",
        " </object>",
        "</java>",
        "##### 18-sin-declaracion-sangria2",
        "<java VERSION class=\"java.beans.XMLDecoder\">",
        "   <object class=\"KajiBeansXmlTest$Punto\">",
        "    <void property=\"etiqueta\">",
        "     <string>hola</string>",
        "    </void>",
        "    <void property=\"x\">",
        "     <int>3</int>",
        "    </void>",
        "   </object>",
        "  </java>",
        "##### 19-owner",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<java VERSION class=\"java.beans.XMLDecoder\">",
        " <void property=\"owner\">",
        "  <void method=\"hacer\"/>",
        " </void>",
        "</java>",
        "##### 20-dos-escrituras",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<java VERSION class=\"java.beans.XMLDecoder\">",
        " <object class=\"KajiBeansXmlTest$Punto\">",
        "  <void property=\"x\">",
        "   <int>1</int>",
        "  </void>",
        " </object>",
        " <object class=\"KajiBeansXmlTest$Punto\">",
        "  <void property=\"x\">",
        "   <int>1</int>",
        "  </void>",
        " </object>",
        "</java>",
        "##### 21-utf8-hex",
        "no-ascii: c3 b1 e2 82 ac"
    };

    // Imprime la transcripcion, para regenerar ESPERADO desde la JVM real.
    public static void main(String[] x) throws Exception {
        System.out.print(transcripcion());
    }
}
