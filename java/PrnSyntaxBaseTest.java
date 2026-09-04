import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import javax.print.attribute.DateTimeSyntax;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.ResolutionSyntax;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.TextSyntax;
import javax.print.attribute.URISyntax;

// Comportamiento de las clases de sintaxis de javax.print.attribute --todas menos
// SetOfIntegerSyntax, que tiene su propia prueba--, para correr con las dos VMs y comparar.
//
// Son las que ponen la mitad "valor" de un atributo: un entero, un texto con locale, una URI, un
// instante, un enum a mano, una resolucion y un tamano. Ninguna es un Attribute y ninguna imprime
// nada; son conversion de unidades, igualdad y validacion.
//
// Lo que NO se compara aca:
//   - `DateTimeSyntax.toString()`, que delega en `Date.toString()` y por lo tanto depende del
//     locale y de la zona horaria de la maquina;
//   - el valor literal de `TextSyntax.hashCode()`, porque lleva adentro `Locale.hashCode()`, que
//     no esta especificado. Se comprueba la formula contra si misma, que es lo que si esta.
public class PrnSyntaxBaseTest {

    static class Ent extends IntegerSyntax {
        Ent(int v) {
            super(v);
        }

        Ent(int v, int lo, int hi) {
            super(v, lo, hi);
        }
    }

    // Segunda subclase, para el detalle de que IntegerSyntax.equals cruza categorias.
    static class OtroEnt extends IntegerSyntax {
        OtroEnt(int v) {
            super(v);
        }
    }

    static class Texto extends TextSyntax {
        Texto(String v, Locale l) {
            super(v, l);
        }
    }

    static class Direccion extends URISyntax {
        Direccion(URI u) {
            super(u);
        }
    }

    static class Instante extends DateTimeSyntax {
        Instante(Date d) {
            super(d);
        }
    }

    // El enum a mano: constantes, tabla de nombres y tabla de valores.
    static class Lado extends EnumSyntax {
        static final Lado UNA_CARA = new Lado(0);
        static final Lado DOS_CARAS = new Lado(1);

        private static final String[] TABLA = {"one-sided", "two-sided-long-edge"};
        private static final Lado[] VALORES = {UNA_CARA, DOS_CARAS};

        Lado(int v) {
            super(v);
        }

        protected String[] getStringTable() {
            return TABLA;
        }

        protected EnumSyntax[] getEnumValueTable() {
            return VALORES;
        }

        // readResolve es protected: hace falta un puente para llamarlo desde la prueba.
        Object resolver() throws ObjectStreamException {
            return readResolve();
        }
    }

    // Con desplazamiento: la primera entrada de la tabla vale 3, no 0.
    static class Corrido extends EnumSyntax {
        static final Corrido TRES = new Corrido(3);
        static final Corrido CUATRO = new Corrido(4);

        private static final String[] TABLA = {"tres", "cuatro"};
        private static final Corrido[] VALORES = {TRES, CUATRO};

        Corrido(int v) {
            super(v);
        }

        protected String[] getStringTable() {
            return TABLA;
        }

        protected EnumSyntax[] getEnumValueTable() {
            return VALORES;
        }

        protected int getOffset() {
            return 3;
        }

        Object resolver() throws ObjectStreamException {
            return readResolve();
        }
    }

    // Sin tablas: la subclase que no se declaro como enum de verdad.
    static class Pelado extends EnumSyntax {
        Pelado(int v) {
            super(v);
        }

        Object resolver() throws ObjectStreamException {
            return readResolve();
        }
    }

    static class Reso extends ResolutionSyntax {
        Reso(int cf, int f, int u) {
            super(cf, f, u);
        }

        int cfDphi() {
            return getCrossFeedResolutionDphi();
        }

        int fDphi() {
            return getFeedResolutionDphi();
        }
    }

    static class Tam extends Size2DSyntax {
        Tam(float x, float y, int u) {
            super(x, y, u);
        }

        Tam(int x, int y, int u) {
            super(x, y, u);
        }

        int xUm() {
            return getXMicrometers();
        }

        int yUm() {
            return getYMicrometers();
        }
    }

    public static int run() throws Exception {
        int n;

        // ---- IntegerSyntax ----

        n = 1;
        if (new Ent(42).getValue() != 42 || new Ent(42).hashCode() != 42
                || !"42".equals(new Ent(42).toString())) {
            return n;
        }
        n = 2;
        if (!new Ent(42).equals(new Ent(42)) || new Ent(42).equals(new Ent(43))) {
            return n;
        }
        n = 3;
        // El detalle raro: equals pregunta por `instanceof IntegerSyntax`, no por la clase, asi
        // que dos atributos de CATEGORIAS distintas con el mismo entero salen iguales. Los
        // conjuntos no se confunden porque indexan por categoria, no por valor.
        if (!new Ent(7).equals(new OtroEnt(7))) {
            return n;
        }
        n = 4;
        if (new Ent(7).equals("7") || new Ent(7).equals(null)) {
            return n;
        }
        n = 5;
        // El constructor con rango: los dos extremos son inclusivos.
        if (new Ent(1, 1, 10).getValue() != 1 || new Ent(10, 1, 10).getValue() != 10) {
            return n;
        }
        n = 6;
        try {
            new Ent(0, 1, 10);
            return n;
        } catch (IllegalArgumentException e) {
            // esperado
        }
        n = 7;
        try {
            new Ent(11, 1, 10);
            return n;
        } catch (IllegalArgumentException e) {
            // esperado
        }
        n = 8;
        // Sin rango, un negativo es un valor legitimo.
        if (new Ent(-5).getValue() != -5 || new Ent(-5).hashCode() != -5) {
            return n;
        }

        // ---- TextSyntax ----

        n = 9;
        Texto t = new Texto("hola", Locale.US);
        if (!"hola".equals(t.getValue()) || !Locale.US.equals(t.getLocale())
                || !"hola".equals(t.toString())) {
            return n;
        }
        n = 10;
        // El locale entra en la igualdad: mismo texto, distinto idioma, distinto valor.
        if (new Texto("hola", Locale.US).equals(new Texto("hola", Locale.FRANCE))) {
            return n;
        }
        n = 11;
        if (!new Texto("hola", Locale.US).equals(new Texto("hola", Locale.US))) {
            return n;
        }
        n = 12;
        // La formula del hash, comprobada contra si misma: el valor literal depende de
        // Locale.hashCode(), que no esta especificado.
        if (t.hashCode() != ("hola".hashCode() ^ Locale.US.hashCode())) {
            return n;
        }
        n = 13;
        if (new Texto("hola", Locale.US).hashCode()
                != new Texto("hola", Locale.US).hashCode()) {
            return n;
        }
        n = 14;
        // Un locale null NO es error: significa "el de por aca".
        if (!Locale.getDefault().equals(new Texto("hola", null).getLocale())) {
            return n;
        }
        n = 15;
        // Un texto null si.
        try {
            new Texto(null, Locale.US);
            return n;
        } catch (NullPointerException e) {
            // esperado
        }
        n = 16;
        if (new Texto("a", Locale.US).equals("a")) {
            return n;
        }

        // ---- URISyntax ----

        n = 17;
        URI u = new URI("ipp://impresora.ejemplo/cola");
        Direccion d = new Direccion(u);
        if (d.getURI() != u || d.hashCode() != u.hashCode()
                || !u.toString().equals(d.toString())) {
            return n;
        }
        n = 18;
        if (!new Direccion(new URI("ipp://a/b")).equals(new Direccion(new URI("ipp://a/b")))) {
            return n;
        }
        n = 19;
        if (new Direccion(new URI("ipp://a/b")).equals(new Direccion(new URI("ipp://a/c")))) {
            return n;
        }
        n = 20;
        try {
            new Direccion(null);
            return n;
        } catch (NullPointerException e) {
            // esperado
        }

        // ---- DateTimeSyntax ----

        n = 21;
        Date fecha = new Date(1000L);
        Instante ins = new Instante(fecha);
        if (ins.getValue().getTime() != 1000L) {
            return n;
        }
        n = 22;
        // getValue devuelve una COPIA: tocarla no toca el atributo.
        Date sacada = ins.getValue();
        sacada.setTime(5000L);
        if (ins.getValue().getTime() != 1000L) {
            return n;
        }
        n = 23;
        // Pero el constructor NO copia: la Date que se le paso sigue siendo la de adentro, asi que
        // cambiarla por atras le cambia el valor al atributo. Es el comportamiento del JDK.
        fecha.setTime(2000L);
        if (ins.getValue().getTime() != 2000L) {
            return n;
        }
        n = 24;
        if (!new Instante(new Date(7L)).equals(new Instante(new Date(7L)))) {
            return n;
        }
        n = 25;
        if (new Instante(new Date(7L)).equals(new Instante(new Date(8L)))) {
            return n;
        }
        n = 26;
        if (new Instante(new Date(7L)).hashCode() != new Date(7L).hashCode()) {
            return n;
        }
        n = 27;
        try {
            new Instante(null);
            return n;
        } catch (NullPointerException e) {
            // esperado
        }

        // ---- EnumSyntax ----

        n = 28;
        if (Lado.UNA_CARA.getValue() != 0 || Lado.DOS_CARAS.getValue() != 1) {
            return n;
        }
        n = 29;
        if (!"one-sided".equals(Lado.UNA_CARA.toString())
                || !"two-sided-long-edge".equals(Lado.DOS_CARAS.toString())) {
            return n;
        }
        n = 30;
        // Fuera de la tabla: el entero pelado, no una excepcion.
        if (!"7".equals(new Lado(7).toString())) {
            return n;
        }
        n = 31;
        // Sin tabla: idem.
        if (!"3".equals(new Pelado(3).toString())) {
            return n;
        }
        n = 32;
        if (Lado.DOS_CARAS.hashCode() != 1 || new Pelado(9).hashCode() != 9) {
            return n;
        }
        n = 33;
        // clone() devuelve THIS: clonar un singleton lo dejaria de ser.
        if (Lado.UNA_CARA.clone() != Lado.UNA_CARA) {
            return n;
        }
        n = 34;
        // EnumSyntax NO redefine equals: la igualdad es la identidad. Dos objetos con el mismo
        // entero NO son iguales, y eso es a proposito.
        if (new Lado(0).equals(Lado.UNA_CARA) || Lado.UNA_CARA.equals(new Lado(0))) {
            return n;
        }
        n = 35;
        if (!Lado.UNA_CARA.equals(Lado.UNA_CARA)) {
            return n;
        }
        n = 36;
        // readResolve: el entero vuelve a ser LA constante, no una copia. Sin esto, un valor que
        // viajara por un stream volveria distinto y el `==` de un enum dejaria de andar.
        if (new Lado(1).resolver() != Lado.DOS_CARAS) {
            return n;
        }
        n = 37;
        if (new Lado(0).resolver() != Lado.UNA_CARA) {
            return n;
        }
        n = 38;
        // Fuera del rango de la tabla: InvalidObjectException.
        try {
            new Lado(9).resolver();
            return n;
        } catch (InvalidObjectException e) {
            // esperado
        }
        n = 39;
        try {
            new Lado(-1).resolver();
            return n;
        } catch (InvalidObjectException e) {
            // esperado
        }
        n = 40;
        // Sin tabla de valores: tambien InvalidObjectException, que es lo correcto para una
        // subclase que no se declaro como enum de verdad.
        try {
            new Pelado(0).resolver();
            return n;
        } catch (InvalidObjectException e) {
            // esperado
        }
        n = 41;
        // El desplazamiento: la primera entrada de la tabla vale 3.
        if (!"tres".equals(Corrido.TRES.toString())
                || !"cuatro".equals(Corrido.CUATRO.toString())) {
            return n;
        }
        n = 42;
        if (Corrido.TRES.getValue() != 3) {
            return n;
        }
        n = 43;
        if (new Corrido(4).resolver() != Corrido.CUATRO) {
            return n;
        }
        n = 44;
        // Debajo del desplazamiento: fuera de rango.
        try {
            new Corrido(2).resolver();
            return n;
        } catch (InvalidObjectException e) {
            // esperado
        }
        n = 45;
        if (!"2".equals(new Corrido(2).toString())) {
            return n;
        }
        n = 46;
        // InvalidObjectException es una ObjectStreamException.
        if (!(new InvalidObjectException("x") instanceof ObjectStreamException)) {
            return n;
        }

        // ---- ResolutionSyntax ----

        n = 47;
        // Adentro se guarda todo en dphi (puntos por cien pulgadas): 100 dphi = 1 dpi.
        Reso r = new Reso(300, 600, ResolutionSyntax.DPI);
        if (r.cfDphi() != 30000 || r.fDphi() != 60000) {
            return n;
        }
        n = 48;
        if (ResolutionSyntax.DPI != 100 || ResolutionSyntax.DPCM != 254) {
            return n;
        }
        n = 49;
        if (r.getCrossFeedResolution(ResolutionSyntax.DPI) != 300
                || r.getFeedResolution(ResolutionSyntax.DPI) != 600) {
            return n;
        }
        n = 50;
        int[] par = r.getResolution(ResolutionSyntax.DPI);
        if (par.length != 2 || par[0] != 300 || par[1] != 600) {
            return n;
        }
        n = 51;
        // La vuelta a otra unidad redondea al entero mas cercano: (30000 + 127) / 254 = 118.
        if (r.getCrossFeedResolution(ResolutionSyntax.DPCM) != 118
                || r.getFeedResolution(ResolutionSyntax.DPCM) != 236) {
            return n;
        }
        n = 52;
        // Guardar el entero en dphi es lo que hace que dos resoluciones construidas en unidades
        // distintas se comparen por igualdad exacta.
        if (!new Reso(254, 254, ResolutionSyntax.DPI)
                .equals(new Reso(100, 100, ResolutionSyntax.DPCM))) {
            return n;
        }
        n = 53;
        if (new Reso(300, 600, ResolutionSyntax.DPI)
                .equals(new Reso(600, 300, ResolutionSyntax.DPI))) {
            return n;
        }
        n = 54;
        if (new Reso(300, 600, ResolutionSyntax.DPI).equals("300x600")) {
            return n;
        }
        n = 55;
        if (r.hashCode() != ((30000 & 0x0000FFFF) | ((60000 & 0x0000FFFF) << 16))) {
            return n;
        }
        n = 56;
        if (!"30000x60000 dphi".equals(r.toString())) {
            return n;
        }
        n = 57;
        if (!"300x600 dpi".equals(r.toString(ResolutionSyntax.DPI, "dpi"))) {
            return n;
        }
        n = 58;
        // Con unitsName null se omite el sufijo Y el espacio.
        if (!"300x600".equals(r.toString(ResolutionSyntax.DPI, null))) {
            return n;
        }
        n = 59;
        // Orden PARCIAL: pide que las dos componentes sean menores o iguales.
        Reso chica = new Reso(300, 300, ResolutionSyntax.DPI);
        Reso grande = new Reso(600, 600, ResolutionSyntax.DPI);
        Reso mezcla = new Reso(600, 300, ResolutionSyntax.DPI);
        if (!chica.lessThanOrEquals(grande) || grande.lessThanOrEquals(chica)) {
            return n;
        }
        n = 60;
        // 300x600 y 600x300 no estan ordenadas entre si en ningun sentido.
        Reso otra = new Reso(300, 600, ResolutionSyntax.DPI);
        if (otra.lessThanOrEquals(mezcla) || mezcla.lessThanOrEquals(otra)) {
            return n;
        }
        n = 61;
        if (!chica.lessThanOrEquals(chica)) {
            return n;
        }
        n = 62;
        try {
            new Reso(0, 300, ResolutionSyntax.DPI);
            return n;
        } catch (IllegalArgumentException e) {
            // esperado
        }
        n = 63;
        try {
            new Reso(300, 0, ResolutionSyntax.DPI);
            return n;
        } catch (IllegalArgumentException e) {
            // esperado
        }
        n = 64;
        try {
            new Reso(300, 300, 0);
            return n;
        } catch (IllegalArgumentException e) {
            // esperado
        }
        n = 65;
        try {
            r.getCrossFeedResolution(0);
            return n;
        } catch (IllegalArgumentException e) {
            // esperado
        }

        // ---- Size2DSyntax ----

        n = 66;
        if (Size2DSyntax.INCH != 25400 || Size2DSyntax.MM != 1000) {
            return n;
        }
        n = 67;
        // Adentro, micrometros: 8.5 x 25400 = 215900.
        Tam carta = new Tam(8.5f, 11.0f, Size2DSyntax.INCH);
        if (carta.xUm() != 215900 || carta.yUm() != 279400) {
            return n;
        }
        n = 68;
        // La variante entera no redondea porque el producto ya es exacto.
        Tam a4 = new Tam(210, 297, Size2DSyntax.MM);
        if (a4.xUm() != 210000 || a4.yUm() != 297000) {
            return n;
        }
        n = 69;
        // La lectura devuelve float: un tamano en pulgadas casi nunca es entero.
        if (carta.getX(Size2DSyntax.INCH) != 8.5f || carta.getY(Size2DSyntax.INCH) != 11.0f) {
            return n;
        }
        n = 70;
        if (carta.getX(Size2DSyntax.MM) != 215.9f) {
            return n;
        }
        n = 71;
        float[] dos = carta.getSize(Size2DSyntax.INCH);
        if (dos.length != 2 || dos[0] != 8.5f || dos[1] != 11.0f) {
            return n;
        }
        n = 72;
        // Igualdad exacta entre unidades distintas, que es para lo que se guarda el entero.
        if (!new Tam(1, 1, Size2DSyntax.INCH).equals(new Tam(25400, 25400, 1))) {
            return n;
        }
        n = 73;
        if (new Tam(1, 1, Size2DSyntax.INCH).equals(new Tam(2, 1, Size2DSyntax.INCH))) {
            return n;
        }
        n = 74;
        if (new Tam(1, 1, Size2DSyntax.INCH).equals("1x1")) {
            return n;
        }
        n = 75;
        if (carta.hashCode() != ((215900 & 0x0000FFFF) | ((279400 & 0x0000FFFF) << 16))) {
            return n;
        }
        n = 76;
        if (!"215900x279400 um".equals(carta.toString())) {
            return n;
        }
        n = 77;
        if (!"8.5x11.0 in".equals(carta.toString(Size2DSyntax.INCH, "in"))) {
            return n;
        }
        n = 78;
        if (!"8.5x11.0".equals(carta.toString(Size2DSyntax.INCH, null))) {
            return n;
        }
        n = 79;
        // El redondeo del constructor float es "+0.5 y truncar".
        if (new Tam(0.00005f, 0.0f, Size2DSyntax.INCH).xUm() != 1) {
            return n;
        }
        n = 80;
        // Cero es un tamano valido; negativo no.
        if (new Tam(0.0f, 0.0f, Size2DSyntax.INCH).xUm() != 0) {
            return n;
        }
        n = 81;
        try {
            new Tam(-1.0f, 1.0f, Size2DSyntax.INCH);
            return n;
        } catch (IllegalArgumentException e) {
            // esperado
        }
        n = 82;
        try {
            new Tam(1.0f, -1.0f, Size2DSyntax.INCH);
            return n;
        } catch (IllegalArgumentException e) {
            // esperado
        }
        n = 83;
        try {
            new Tam(1, 1, 0);
            return n;
        } catch (IllegalArgumentException e) {
            // esperado
        }
        n = 84;
        try {
            new Tam(-1, 1, Size2DSyntax.MM);
            return n;
        } catch (IllegalArgumentException e) {
            // esperado
        }
        n = 85;
        try {
            carta.getX(0);
            return n;
        } catch (IllegalArgumentException e) {
            // esperado
        }

        return -1;
    }
}
