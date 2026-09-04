import java.util.HashMap;
import java.util.Map;
import javax.security.auth.x500.X500Principal;

/**
 * X500Principal: parseo RFC 2253, DER en los dos sentidos, y los tres formatos.
 *
 * <p>Las expectativas salieron de preguntarle al JDK 25, no de leer el RFC -- que en varios puntos
 * deja lugar a mas de una lectura. Las tres que no son obvias y por las que vale la pena tener la
 * prueba: un tipo sin palabra clave fuerza el valor a hexadecimal, RFC 1779 cita en vez de escapar, y
 * el DER lista los pasos al reves que el texto.
 */
public class X500Test {

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            int v = b[i] & 0xff;
            sb.append(Character.forDigit(v >> 4, 16)).append(Character.forDigit(v & 0xf, 16));
        }
        return sb.toString().toUpperCase();
    }

    public static int run() {
        int i = 0;

        // -- lo basico
        X500Principal p = new X500Principal("CN=Juan, OU=Ventas, O=Acme, C=AR");
        if (!p.getName().equals("CN=Juan,OU=Ventas,O=Acme,C=AR")) { return i; } i++;
        if (!p.getName(X500Principal.RFC2253).equals("CN=Juan,OU=Ventas,O=Acme,C=AR")) { return i; } i++;
        if (!p.getName(X500Principal.RFC1779).equals("CN=Juan, OU=Ventas, O=Acme, C=AR")) { return i; } i++;
        if (!p.getName(X500Principal.CANONICAL).equals("cn=juan,ou=ventas,o=acme,c=ar")) { return i; } i++;
        if (!p.toString().equals("CN=Juan, OU=Ventas, O=Acme, C=AR")) { return i; } i++;

        // -- el DER da vuelta el orden, y volver a leerlo lo restaura
        byte[] der = p.getEncoded();
        X500Principal q = new X500Principal(der);
        if (!q.getName().equals(p.getName())) { return i; } i++;
        if (!q.equals(p)) { return i; } i++;
        if (q.hashCode() != p.hashCode()) { return i; } i++;
        // El primer RDN del DER es el ULTIMO del texto: C=AR, OID 2.5.4.6 -> 0603550406.
        String h = hex(der);
        if (h.indexOf("0603550406") < 0) { return i; } i++;
        if (h.indexOf("0603550406") > h.indexOf("0603550403")) { return i; } i++;

        // -- igualdad por forma canonica, no por texto ni por bytes
        X500Principal a = new X500Principal("CN=Juan,O=Acme");
        X500Principal b = new X500Principal("cn=JUAN,  o=acme");
        if (!a.equals(b)) { return i; } i++;
        if (a.hashCode() != b.hashCode()) { return i; } i++;
        if (a.equals(new X500Principal("CN=Juana,O=Acme"))) { return i; } i++;

        // -- espacios internos: colapsados en canonico, intactos en RFC 2253, citados en RFC 1779
        X500Principal d = new X500Principal("CN=Juan  Perez");
        if (!d.getName(X500Principal.CANONICAL).equals("cn=juan perez")) { return i; } i++;
        if (!d.getName().equals("CN=Juan  Perez")) { return i; } i++;
        if (!d.getName(X500Principal.RFC1779).equals("CN=\"Juan  Perez\"")) { return i; } i++;

        // -- la coma adentro de un valor: RFC 2253 la escapa, RFC 1779 cita
        X500Principal e = new X500Principal("CN=Perez\\, Juan,O=Acme");
        if (!e.getName().equals("CN=Perez\\, Juan,O=Acme")) { return i; } i++;
        if (!e.getName(X500Principal.RFC1779).equals("CN=\"Perez, Juan\", O=Acme")) { return i; } i++;
        if (!new X500Principal(e.getEncoded()).getName().equals(e.getName())) { return i; } i++;

        // -- entre comillas al entrar es lo mismo que escapado
        X500Principal g = new X500Principal("CN=\"Perez, Juan\",O=Acme");
        if (!g.getName().equals("CN=Perez\\, Juan,O=Acme")) { return i; } i++;
        if (!g.equals(e)) { return i; } i++;

        // -- un tipo SIN palabra clave fuerza el valor a hexadecimal en RFC 2253 y en canonico,
        //    pero no en RFC 1779, que lo escribe como texto con el prefijo OID.
        X500Principal k = new X500Principal("1.2.3.4=algo,CN=x");
        if (!k.getName().equals("1.2.3.4=#1304616c676f,CN=x")) { return i; } i++;
        if (!k.getName(X500Principal.RFC1779).equals("OID.1.2.3.4=algo, CN=x")) { return i; } i++;
        if (!k.getName(X500Principal.CANONICAL).equals("1.2.3.4=#1304616c676f,cn=x")) { return i; } i++;

        // -- OID.x.y al entrar es lo mismo que x.y
        if (!new X500Principal("OID.1.2.3.4=algo,CN=x").equals(k)) { return i; } i++;

        // -- diccionario propio, en las dos direcciones. Con palabra clave el valor vuelve a texto.
        Map<String, String> palabras = new HashMap<String, String>();
        palabras.put("MIO", "1.2.3.4");
        X500Principal m = new X500Principal("MIO=algo,CN=x", palabras);
        if (!m.equals(k)) { return i; } i++;
        Map<String, String> oids = new HashMap<String, String>();
        oids.put("1.2.3.4", "MIO");
        if (!m.getName(X500Principal.RFC2253, oids).equals("MIO=algo,CN=x")) { return i; } i++;
        boolean rechaza = false;
        try { m.getName(X500Principal.CANONICAL, oids); }
        catch (IllegalArgumentException ex) { rechaza = true; }
        if (!rechaza) { return i; } i++;

        // -- RDN multivaluado: `+` pelado en RFC 2253, ` + ` en RFC 1779
        X500Principal mv = new X500Principal("CN=Juan+OU=Ventas,O=Acme");
        if (!mv.getName().equals("CN=Juan+OU=Ventas,O=Acme")) { return i; } i++;
        if (!mv.getName(X500Principal.RFC1779).equals("CN=Juan + OU=Ventas, O=Acme")) { return i; } i++;
        if (!mv.getName(X500Principal.CANONICAL).equals("cn=juan+ou=ventas,o=acme")) { return i; } i++;
        X500Principal mv2 = new X500Principal(mv.getEncoded());
        if (!mv2.getName(X500Principal.CANONICAL).equals(mv.getName(X500Principal.CANONICAL))) { return i; } i++;

        // -- un DN vacio es valido: designa la raiz del directorio
        X500Principal vacio = new X500Principal("");
        if (!vacio.getName().equals("")) { return i; } i++;
        if (!hex(vacio.getEncoded()).equals("3000")) { return i; } i++;

        // -- no ASCII fuerza UTF8String (0x0c) en vez de PrintableString (0x13)
        X500Principal acento = new X500Principal("CN=José");
        if (hex(acento.getEncoded()).indexOf("0C05") < 0) { return i; } i++;
        if (!new X500Principal(acento.getEncoded()).getName().equals("CN=José")) { return i; } i++;

        // -- getEncoded devuelve una copia
        byte[] copia = p.getEncoded();
        copia[0] = (byte) 0xFF;
        if (p.getEncoded()[0] == (byte) 0xFF) { return i; } i++;

        // -- entradas invalidas
        boolean malo = false;
        try { new X500Principal("no-hay-igual"); } catch (IllegalArgumentException ex) { malo = true; }
        if (!malo) { return i; } i++;
        malo = false;
        try { p.getName("INVENTADO"); } catch (IllegalArgumentException ex) { malo = true; }
        if (!malo) { return i; } i++;
        malo = false;
        try { new X500Principal(new byte[] {1, 2, 3}); } catch (IllegalArgumentException ex) { malo = true; }
        if (!malo) { return i; } i++;

        // -- desde un flujo
        java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(p.getEncoded());
        if (!new X500Principal(in).getName().equals(p.getName())) { return i; } i++;

        return -1;
    }

    public static void main(String[] args) { System.out.println(run()); }
}
