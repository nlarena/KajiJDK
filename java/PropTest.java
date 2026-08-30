import java.io.StringReader;
import java.util.Properties;
import java.util.PropertyResourceBundle;

// El parser de `.properties`, que tiene mas reglas de las que parece. Cada una se prueba aparte
// porque cada una es una forma distinta de leer mal un archivo de configuracion.
public class PropTest {

    static final String TEXTO =
        "# un comentario\n"
        + "! otro comentario\n"
        + "\n"
        + "simple=uno\n"
        + "conEspacios  =  dos  \n"
        + "conDosPuntos: tres\n"
        + "soloBlanco cuatro\n"
        + "sinValor=\n"
        + "sinSeparador\n"
        + "continua=cin\\\n"
        + "    co\n"
        + "escapes=a\\tb\\nc\\\\d\n"
        + "unicode=\\u0041\\u00e9\n"
        + "clave\\=rara=seis\n"
        + "terminaEnBarra=x\\\\\n";

    static Properties cargar() throws Exception {
        Properties p = new Properties();
        p.load(new StringReader(TEXTO));
        return p;
    }

    public static int run() throws Exception {
        Properties p = cargar();
        int r = 0;

        r = r + (p.getProperty("simple").equals("uno") ? 1 : 0);
        // los blancos alrededor del separador se descartan; los del final del valor NO
        r = r + (p.getProperty("conEspacios").equals("dos  ") ? 10 : 0);
        r = r + (p.getProperty("conDosPuntos").equals("tres") ? 100 : 0);
        // un blanco tambien separa
        r = r + (p.getProperty("soloBlanco").equals("cuatro") ? 1000 : 0);
        r = r + (p.getProperty("sinValor").equals("") ? 10000 : 0);
        // una clave sin separador vale, con valor vacio
        r = r + (p.getProperty("sinSeparador").equals("") ? 100000 : 0);
        // continuacion: la barra final une, y los blancos iniciales de la siguiente se van
        r = r + (p.getProperty("continua").equals("cinco") ? 1000000 : 0);
        r = r + (p.getProperty("escapes").equals("a\tb\nc\\d") ? 10000000 : 0);
        r = r + (p.getProperty("unicode").equals("Aé") ? 100000000 : 0);
        // \= no parte la clave
        r = r + (p.getProperty("clave=rara").equals("seis") ? 2 : 0);
        // barra escapada al final: NO es continuacion
        r = r + (p.getProperty("terminaEnBarra").equals("x\\") ? 20 : 0);
        // los comentarios no entran
        r = r + (p.getProperty("# un comentario") == null ? 200 : 0);

        // ---- PropertyResourceBundle sobre el mismo texto ---------------------------------------
        PropertyResourceBundle b = new PropertyResourceBundle(new StringReader(TEXTO));
        r = r + (b.getString("simple").equals("uno") ? 2000 : 0);
        r = r + (b.containsKey("continua") ? 20000 : 0);
        r = r + (b.keySet().size() == 12 ? 200000 : 0);

        return r;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(run());
    }
}
