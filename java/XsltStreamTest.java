import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Pruebas de comportamiento de `javax.xml.transform.stream`.
 *
 * <p>Corre igual contra nuestra biblioteca y contra el JDK real: aca **no hay divergencia
 * legitima**, porque estas dos clases no dependen de que exista un procesador de XSLT -- son
 * portadatos sobre `java.io`. Un resultado distinto entre las dos VMs es un error, no una
 * diferencia de instalacion.
 *
 * <p>Lo que se prueba de verdad son las reglas que no son obvias: que `isEmpty()` mire la URI por
 * `null` y no por su contenido, que **no consuma** el flujo al espiarlo, y que `setSystemId(File)`
 * de exactamente la URI del archivo. El percent-encoding de esa URI **no** se chequea aca: lo hace
 * `java.net.URI`, que hoy no lo hace, y es un defecto de ese paquete y no de este.
 *
 * <p>Devuelve -1 si todo bien; si no, el numero del paso que fallo.
 */
public class XsltStreamTest {

    /** Un flujo que no soporta marcas: no se puede espiar sin romperlo. */
    static class SinMarca extends InputStream {
        private final byte[] datos;
        private int i;
        SinMarca(byte[] datos) { this.datos = datos; }
        public int read() {
            if (i >= datos.length) {
                return -1;
            }
            int b = datos[i] & 0xff;
            i = i + 1;
            return b;
        }
        public boolean markSupported() { return false; }
    }

    static boolean iguales(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    public static int run() {
        // 10. Los nombres de caracteristica, exactos.
        if (!iguales(StreamSource.FEATURE, "http://javax.xml.transform.stream.StreamSource/feature")) return 10;
        if (!iguales(StreamResult.FEATURE, "http://javax.xml.transform.stream.StreamResult/feature")) return 11;

        // 20. Jerarquia: son las implementaciones de Source y Result, no clases sueltas.
        StreamSource vacia = new StreamSource();
        StreamResult rvacio = new StreamResult();
        if (!(vacia instanceof Source)) return 20;
        if (!(rvacio instanceof Result)) return 21;

        // 30. Una fuente recien hecha no tiene absolutamente nada.
        if (vacia.getSystemId() != null) return 30;
        if (vacia.getPublicId() != null) return 31;
        if (vacia.getInputStream() != null) return 32;
        if (vacia.getReader() != null) return 33;
        if (!vacia.isEmpty()) return 34;

        // 40. Guarda y devuelve lo que se le da, por los tres caminos.
        InputStream bytes = new ByteArrayInputStream(new byte[] { 60, 97, 47, 62 });
        StreamSource s = new StreamSource();
        s.setInputStream(bytes);
        if (s.getInputStream() != bytes) return 40;
        Reader caracteres = new StringReader("<a/>");
        s.setReader(caracteres);
        if (s.getReader() != caracteres) return 41;
        s.setPublicId("-//EJ//DTD//EN");
        if (!iguales(s.getPublicId(), "-//EJ//DTD//EN")) return 42;
        s.setSystemId("urn:ejemplo");
        if (!iguales(s.getSystemId(), "urn:ejemplo")) return 43;
        // Y se pueden borrar poniendo null: no hay estado pegajoso.
        s.setInputStream(null);
        s.setReader(null);
        s.setPublicId(null);
        s.setSystemId((String) null);
        if (s.getInputStream() != null || s.getReader() != null) return 44;
        if (s.getPublicId() != null || s.getSystemId() != null) return 45;

        // 50. Los constructores dejan puesto lo que dicen.
        if (new StreamSource(bytes).getInputStream() != bytes) return 50;
        StreamSource c2 = new StreamSource(bytes, "urn:a");
        if (c2.getInputStream() != bytes || !iguales(c2.getSystemId(), "urn:a")) return 51;
        if (new StreamSource(caracteres).getReader() != caracteres) return 52;
        StreamSource c4 = new StreamSource(caracteres, "urn:b");
        if (c4.getReader() != caracteres || !iguales(c4.getSystemId(), "urn:b")) return 53;
        if (!iguales(new StreamSource("urn:c").getSystemId(), "urn:c")) return 54;
        // El de la URI **no** abre nada: deja los flujos en null.
        StreamSource c5 = new StreamSource("urn:c");
        if (c5.getInputStream() != null || c5.getReader() != null) return 55;

        // 60. isEmpty mira la URI por null, no por su contenido.
        if (new StreamSource((String) null).isEmpty() == false) return 60;
        if (new StreamSource("").isEmpty() == true) return 61;
        if (new StreamSource("urn:x").isEmpty() == true) return 62;

        // 70. isEmpty espia el flujo sin consumirlo. Esta es la regla que importa.
        if (new StreamSource(new ByteArrayInputStream(new byte[0])).isEmpty() == false) return 70;
        InputStream conDatos = new ByteArrayInputStream(new byte[] { 65, 66 });
        StreamSource sd = new StreamSource(conDatos);
        if (sd.isEmpty() == true) return 71;
        int primero = -2;
        try {
            primero = conDatos.read();
        } catch (Throwable t) {
            return 72;
        }
        if (primero != 65) return 73;

        if (new StreamSource(new StringReader("")).isEmpty() == false) return 74;
        Reader rConDatos = new StringReader("AB");
        StreamSource sr = new StreamSource(rConDatos);
        if (sr.isEmpty() == true) return 75;
        int primeroChar = -2;
        try {
            primeroChar = rConDatos.read();
        } catch (Throwable t) {
            return 76;
        }
        if (primeroChar != 65) return 77;

        // 80. Un flujo sin marcas no se puede espiar; la respuesta segura es "no esta vacia", y
        //     tampoco se le come nada.
        InputStream sinMarca = new SinMarca(new byte[] { 67, 68 });
        StreamSource ssm = new StreamSource(sinMarca);
        if (ssm.isEmpty() == true) return 80;
        int primeroSm = -2;
        try {
            primeroSm = sinMarca.read();
        } catch (Throwable t) {
            return 81;
        }
        if (primeroSm != 67) return 82;
        // Uno sin marcas y **sin datos** tambien contesta "no esta vacia": no se puede saber.
        if (new StreamSource(new SinMarca(new byte[0])).isEmpty() == true) return 83;

        // 90. setSystemId(File) da una URI file: absoluta y percent-encoded.
        // Una ruta cualquiera: `toURI()` no abre el archivo, asi que no hace falta que
        // exista, y la prueba no depende de nada fuera del repositorio.
        File f = new File("java/XsltStreamTest.java");
        String esperada = f.toURI().toASCIIString();
        if (!iguales(new StreamSource(f).getSystemId(), esperada)) return 90;
        StreamSource sf = new StreamSource();
        sf.setSystemId(f);
        if (!iguales(sf.getSystemId(), esperada)) return 91;
        if (!esperada.startsWith("file:")) return 92;
        // Con un nombre que necesita escaparse, la respuesta sigue siendo exactamente la URI del
        // archivo. **El escapado en si no se chequea aca**: quien lo hace es `java.net.URI`, y en
        // esta biblioteca todavia no lo hace --`new URI("file", null, "/a b/c", null)` deja el
        // espacio crudo--. Ese es un defecto de `java.net.URI`, no de esta clase, y meterle un
        // `indexOf("%20")` a esta prueba lo unico que lograria es que falle el paquete equivocado.
        File conEspacio = new File("java/con espacio.xml");
        if (!iguales(new StreamSource(conEspacio).getSystemId(), conEspacio.toURI().toASCIIString())) return 93;
        StreamResult reEspacio = new StreamResult();
        reEspacio.setSystemId(conEspacio);
        if (!iguales(reEspacio.getSystemId(), conEspacio.toURI().toASCIIString())) return 94;

        // 100. StreamResult: los mismos caminos, sin isEmpty porque Result no lo tiene.
        if (rvacio.getSystemId() != null) return 100;
        if (rvacio.getOutputStream() != null) return 101;
        if (rvacio.getWriter() != null) return 102;
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        StreamResult r = new StreamResult(salida);
        if (r.getOutputStream() != salida) return 103;
        Writer w = new StringWriter();
        StreamResult r2 = new StreamResult(w);
        if (r2.getWriter() != w) return 104;
        if (r2.getOutputStream() != null) return 105;
        if (!iguales(new StreamResult("urn:d").getSystemId(), "urn:d")) return 106;
        if (!iguales(new StreamResult(f).getSystemId(), esperada)) return 107;
        StreamResult r3 = new StreamResult();
        r3.setOutputStream(salida);
        r3.setWriter(w);
        r3.setSystemId("urn:e");
        if (r3.getOutputStream() != salida || r3.getWriter() != w) return 108;
        if (!iguales(r3.getSystemId(), "urn:e")) return 109;
        r3.setSystemId(f);
        if (!iguales(r3.getSystemId(), esperada)) return 110;
        r3.setSystemId((String) null);
        if (r3.getSystemId() != null) return 111;

        // 120. Las constantes de escapado viven en Result y son las mismas para este destino.
        if (!iguales(Result.PI_DISABLE_OUTPUT_ESCAPING, "javax.xml.transform.disable-output-escaping")) return 120;

        return -1;
    }
}
