package java.text;

import java.io.InvalidObjectException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * El formateador de mensajes con huecos: {@code "Hay {0} archivos en {1}"}.
 *
 * <p>Existe por una razón de traducción, no de comodidad. Concatenar
 * {@code "Hay " + n + " archivos en " + d} deja el ORDEN de las piezas fijado en el código, y hay
 * idiomas que lo quieren distinto; con un patrón, el traductor mueve {@code {0}} y {@code {1}} sin
 * tocar nada. Por eso el índice va explícito y no implícito en la posición.
 *
 * <p><b>La representación interna es la del JDK y conviene entenderla</b>: el patrón no se guarda
 * como se escribió. Se guarda todo el texto literal concatenado en una sola cadena, más una lista de
 * "en el offset {@code o} va el argumento {@code a}, formateado con {@code f}". Formatear es
 * intercalar; parsear es reconocer los pedazos literales y dejar que cada subformato lea lo del
 * medio. {@link #toPattern()} vuelve a sintetizar el patrón desde ahí, así que después de un
 * {@link #setFormat} devuelve el patrón nuevo y no el original.
 *
 * <p>El entrecomillado es la parte que más sorprende: una comilla simple abre texto literal
 * ({@code 'no {0} se sustituye'}) y dos comillas seguidas son una comilla. Es lo que permite que un
 * mensaje hable de llaves sin que se lo interprete.
 *
 * @implNote Subconjunto declarado, y son dos cosas distintas. (1) De la SINTAXIS de patrón están
 *           {@code number}, {@code date}, {@code time} y {@code choice} con sus estilos; los tipos
 *           {@code dtf_date}/{@code dtf_time}/{@code dtf_datetime} (que delegan en
 *           {@code java.time.format}) y {@code compact_short}/{@code compact_long} no están, y un
 *           patrón que los use es RECHAZADO con {@code IllegalArgumentException} — no se ignoran en
 *           silencio. (2) De {@link #formatToCharacterIterator}, el iterador marca los argumentos
 *           con {@link java.text.MessageFormat.Field#ARGUMENT} pero no reexporta los atributos que
 *           cada subformato pone dentro de su pedazo. Marcar de menos es un subconjunto; marcar mal
 *           no lo sería.
 */
public class MessageFormat extends Format {

    /**
     * La clave con la que se marca cada pedazo del resultado que salió de un argumento.
     *
     * <p>Tiene una sola constante porque un mensaje tiene una sola pregunta interesante: qué parte
     * del texto es texto fijo y qué parte se sustituyó.
     */
    public static class Field extends java.text.Format.Field {

        private static final Map<String, java.text.MessageFormat.Field> INSTANCIAS =
                new HashMap<String, java.text.MessageFormat.Field>();

        protected Field(String name) {
            super(name);
            if (this.getClass() == java.text.MessageFormat.Field.class) {
                INSTANCIAS.put(name, this);
            }
        }

        protected Object readResolve() throws InvalidObjectException {
            if (this.getClass() != java.text.MessageFormat.Field.class) {
                throw new InvalidObjectException("subclass didn't correctly implement readResolve");
            }
            java.text.MessageFormat.Field f = INSTANCIAS.get(this.getName());
            if (f != null) {
                return f;
            }
            throw new InvalidObjectException("unknown attribute name");
        }

        public static final java.text.MessageFormat.Field ARGUMENT =
                new java.text.MessageFormat.Field("message argument field");
    }

    private Locale locale;
    // Todo el texto literal, sin los elementos de formato. Los huecos viven en `offsets`.
    private String pattern;
    private int[] offsets;
    private int[] argumentNumbers;
    private Format[] formats;
    private int cantidad;
    private int maxArgumento;

    public MessageFormat(String pattern) {
        this.locale = Locale.getDefault();
        this.reiniciar();
        this.applyPattern(pattern);
    }

    public MessageFormat(String pattern, Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        this.locale = locale;
        this.reiniciar();
        this.applyPattern(pattern);
    }

    private void reiniciar() {
        this.pattern = "";
        this.offsets = new int[8];
        this.argumentNumbers = new int[8];
        this.formats = new Format[8];
        this.cantidad = 0;
        this.maxArgumento = -1;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return this.locale;
    }

    // ---- patrón ----

    public void applyPattern(String pattern) {
        if (pattern == null) {
            throw new NullPointerException();
        }
        StringBuilder crudo = new StringBuilder();
        StringBuilder indice = new StringBuilder();
        StringBuilder tipo = new StringBuilder();
        StringBuilder estilo = new StringBuilder();
        this.reiniciar();

        // parte 0 = texto literal, 1 = índice, 2 = tipo, 3 = estilo. El autómata es el del JDK:
        // las comas separan partes sólo hasta la 3, porque el estilo puede contener comas propias
        // (un subpatrón de choice las usa).
        int parte = 0;
        boolean entreComillas = false;
        int llaves = 0;
        int i = 0;
        int n = pattern.length();
        while (i < n) {
            char ch = pattern.charAt(i);
            if (parte == 0) {
                if (ch == '\'') {
                    if (i + 1 < n && pattern.charAt(i + 1) == '\'') {
                        crudo.append('\'');
                        i = i + 1;
                    } else {
                        entreComillas = !entreComillas;
                    }
                } else if (ch == '{' && !entreComillas) {
                    parte = 1;
                } else {
                    crudo.append(ch);
                }
            } else if (entreComillas) {
                this.aParte(parte, indice, tipo, estilo).append(ch);
                if (ch == '\'') {
                    entreComillas = false;
                }
            } else if (ch == ',' && parte < 3) {
                parte = parte + 1;
            } else if (ch == '{') {
                llaves = llaves + 1;
                this.aParte(parte, indice, tipo, estilo).append(ch);
            } else if (ch == '}') {
                if (llaves == 0) {
                    this.agregarElemento(crudo.length(), indice.toString(), tipo.toString(),
                            estilo.toString());
                    indice.setLength(0);
                    tipo.setLength(0);
                    estilo.setLength(0);
                    parte = 0;
                } else {
                    llaves = llaves - 1;
                    this.aParte(parte, indice, tipo, estilo).append(ch);
                }
            } else {
                if (ch == '\'') {
                    entreComillas = true;
                }
                this.aParte(parte, indice, tipo, estilo).append(ch);
            }
            i = i + 1;
        }
        if (parte != 0 || llaves != 0) {
            throw new IllegalArgumentException("Unmatched braces in the pattern.");
        }
        this.pattern = crudo.toString();
    }

    private StringBuilder aParte(int parte, StringBuilder indice, StringBuilder tipo,
                                 StringBuilder estilo) {
        if (parte == 1) {
            return indice;
        }
        if (parte == 2) {
            return tipo;
        }
        return estilo;
    }

    private void agregarElemento(int offset, String indice, String tipo, String estilo) {
        // El indice es SOLO digitos: ni vacio, ni con espacios alrededor, ni con signo. `{ 0 }` no es
        // `{0}` con adornos sino un patron mal formado, y aceptarlo haria que un `{0}` mal tipeado
        // funcionara aca y fallara contra cualquier otra implementacion.
        int arg;
        if (!soloDigitos(indice)) {
            throw new IllegalArgumentException("can't parse argument number: " + indice);
        }
        try {
            arg = Integer.parseInt(indice);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("can't parse argument number: " + indice);
        }
        if (arg < 0) {
            throw new IllegalArgumentException("negative argument number: " + arg);
        }
        if (this.cantidad == this.offsets.length) {
            int nuevo = this.cantidad * 2;
            int[] o = new int[nuevo];
            int[] a = new int[nuevo];
            Format[] f = new Format[nuevo];
            for (int k = 0; k < this.cantidad; k = k + 1) {
                o[k] = this.offsets[k];
                a[k] = this.argumentNumbers[k];
                f[k] = this.formats[k];
            }
            this.offsets = o;
            this.argumentNumbers = a;
            this.formats = f;
        }
        this.offsets[this.cantidad] = offset;
        this.argumentNumbers[this.cantidad] = arg;
        this.formats[this.cantidad] = this.armarFormato(tipo.trim(), estilo.trim());
        this.cantidad = this.cantidad + 1;
        if (arg > this.maxArgumento) {
            this.maxArgumento = arg;
        }
    }

    private static boolean soloDigitos(String s) {
        if (s.length() == 0) {
            return false;
        }
        for (int i = 0; i < s.length(); i = i + 1) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    private Format armarFormato(String tipo, String estilo) {
        if (tipo.length() == 0) {
            // Sin tipo el argumento no lleva formateador fijo: se decide al formatear, según la
            // clase del valor. Es lo que hace que {0} sirva para un número y para un texto.
            return null;
        }
        if (tipo.equals("number")) {
            if (estilo.length() == 0) {
                return NumberFormat.getInstance(this.locale);
            }
            if (estilo.equals("currency")) {
                return NumberFormat.getCurrencyInstance(this.locale);
            }
            if (estilo.equals("percent")) {
                return NumberFormat.getPercentInstance(this.locale);
            }
            if (estilo.equals("integer")) {
                return NumberFormat.getIntegerInstance(this.locale);
            }
            return new DecimalFormat(estilo, new DecimalFormatSymbols(this.locale));
        }
        if (tipo.equals("date")) {
            return this.formatoDeFecha(estilo, true);
        }
        if (tipo.equals("time")) {
            return this.formatoDeFecha(estilo, false);
        }
        if (tipo.equals("choice")) {
            return new ChoiceFormat(estilo);
        }
        // Un tipo desconocido es un error del patrón, no algo para ignorar: si se aceptara en
        // silencio, {0,dtf_date} saldría como el toString() del Date y nadie se enteraría.
        throw new IllegalArgumentException("unknown format type: " + tipo);
    }

    private Format formatoDeFecha(String estilo, boolean fecha) {
        int st = -1;
        if (estilo.length() == 0 || estilo.equals("medium")) {
            st = DateFormat.DEFAULT;
        } else if (estilo.equals("short")) {
            st = DateFormat.SHORT;
        } else if (estilo.equals("long")) {
            st = DateFormat.LONG;
        } else if (estilo.equals("full")) {
            st = DateFormat.FULL;
        }
        if (st < 0) {
            return new SimpleDateFormat(estilo, this.locale);
        }
        if (fecha) {
            return DateFormat.getDateInstance(st, this.locale);
        }
        return DateFormat.getTimeInstance(st, this.locale);
    }

    /**
     * Sintetiza el patrón que describe el estado actual.
     *
     * <p>Los subformatos se reconocen comparándolos con los que las fábricas del locale devuelven:
     * si uno es igual al {@code getCurrencyInstance} de este locale, se escribe
     * {@code ,number,currency} y no el patrón crudo. Un formateador puesto a mano que no se parezca
     * a ninguno se escribe con su propio patrón; uno que no sepa dar patrón sale como {@code {n}} a
     * secas — que es lo que hace el JDK, y es preferible a inventarle una sintaxis.
     */
    public String toPattern() {
        StringBuilder r = new StringBuilder();
        int ultimo = 0;
        for (int i = 0; i < this.cantidad; i = i + 1) {
            this.copiarConComillas(this.pattern, ultimo, this.offsets[i], r);
            ultimo = this.offsets[i];
            r.append('{');
            r.append(Integer.toString(this.argumentNumbers[i]));
            this.describirFormato(this.formats[i], r);
            r.append('}');
        }
        this.copiarConComillas(this.pattern, ultimo, this.pattern.length(), r);
        return r.toString();
    }

    private void describirFormato(Format f, StringBuilder r) {
        if (f == null) {
            return;
        }
        if (f instanceof ChoiceFormat) {
            r.append(",choice,");
            r.append(((ChoiceFormat) f).toPattern());
            return;
        }
        if (f instanceof NumberFormat) {
            if (f.equals(NumberFormat.getInstance(this.locale))) {
                r.append(",number");
            } else if (f.equals(NumberFormat.getCurrencyInstance(this.locale))) {
                r.append(",number,currency");
            } else if (f.equals(NumberFormat.getPercentInstance(this.locale))) {
                r.append(",number,percent");
            } else if (f.equals(NumberFormat.getIntegerInstance(this.locale))) {
                r.append(",number,integer");
            } else if (f instanceof DecimalFormat) {
                r.append(",number,");
                r.append(((DecimalFormat) f).toPattern());
            }
            return;
        }
        if (f instanceof DateFormat) {
            for (int k = DateFormat.FULL; k <= DateFormat.SHORT; k = k + 1) {
                if (f.equals(DateFormat.getDateInstance(k, this.locale))) {
                    r.append(",date");
                    this.describirEstilo(k, r);
                    return;
                }
            }
            for (int k = DateFormat.FULL; k <= DateFormat.SHORT; k = k + 1) {
                if (f.equals(DateFormat.getTimeInstance(k, this.locale))) {
                    r.append(",time");
                    this.describirEstilo(k, r);
                    return;
                }
            }
            if (f instanceof SimpleDateFormat) {
                r.append(",date,");
                r.append(((SimpleDateFormat) f).toPattern());
            }
        }
    }

    private void describirEstilo(int estilo, StringBuilder r) {
        if (estilo == DateFormat.FULL) {
            r.append(",full");
        } else if (estilo == DateFormat.LONG) {
            r.append(",long");
        } else if (estilo == DateFormat.SHORT) {
            r.append(",short");
        }
        // MEDIUM es el estilo por omisión: escribirlo sería ruido, y el patrón sin estilo lo
        // vuelve a dar.
    }

    // El texto literal vuelve al patrón con las comillas dobladas y las llaves de APERTURA
    // entrecomilladas. Sólo las de apertura: una '}' suelta en texto literal no abre nada, así que
    // no hace falta protegerla, y protegerla igual daría un patrón distinto del que devuelve el
    // JDK para el mismo mensaje. Sin esto, un texto con '{' dejaría de round-tripear: el segundo
    // applyPattern leería un elemento de formato donde había texto.
    private void copiarConComillas(String s, int desde, int hasta, StringBuilder r) {
        boolean abierta = false;
        for (int i = desde; i < hasta; i = i + 1) {
            char c = s.charAt(i);
            if (c == '{') {
                if (!abierta) {
                    r.append('\'');
                    abierta = true;
                }
                r.append(c);
            } else if (c == '\'') {
                r.append("''");
            } else {
                if (abierta) {
                    r.append('\'');
                    abierta = false;
                }
                r.append(c);
            }
        }
        if (abierta) {
            r.append('\'');
        }
    }

    // ---- subformatos ----

    public void setFormatsByArgumentIndex(Format[] newFormats) {
        for (int i = 0; i < this.cantidad; i = i + 1) {
            int arg = this.argumentNumbers[i];
            if (arg < newFormats.length) {
                this.formats[i] = newFormats[arg];
            }
        }
    }

    public void setFormats(Format[] newFormats) {
        int n = this.cantidad;
        if (newFormats.length < n) {
            n = newFormats.length;
        }
        for (int i = 0; i < n; i = i + 1) {
            this.formats[i] = newFormats[i];
        }
    }

    public void setFormatByArgumentIndex(int argumentIndex, Format newFormat) {
        for (int i = 0; i < this.cantidad; i = i + 1) {
            if (this.argumentNumbers[i] == argumentIndex) {
                this.formats[i] = newFormat;
            }
        }
    }

    public void setFormat(int formatElementIndex, Format newFormat) {
        if (formatElementIndex < 0 || formatElementIndex >= this.cantidad) {
            throw new ArrayIndexOutOfBoundsException(formatElementIndex);
        }
        this.formats[formatElementIndex] = newFormat;
    }

    /**
     * Los subformatos indexados por número de argumento.
     *
     * <p>Si un argumento aparece dos veces en el patrón gana el ÚLTIMO, que es lo que documenta el
     * JDK: el arreglo tiene una casilla por argumento y las apariciones repetidas no caben.
     */
    public Format[] getFormatsByArgumentIndex() {
        Format[] out = new Format[this.maxArgumento + 1];
        for (int i = 0; i < this.cantidad; i = i + 1) {
            out[this.argumentNumbers[i]] = this.formats[i];
        }
        return out;
    }

    /** Los subformatos en el orden en que aparecen en el patrón, uno por elemento. */
    public Format[] getFormats() {
        Format[] out = new Format[this.cantidad];
        for (int i = 0; i < this.cantidad; i = i + 1) {
            out[i] = this.formats[i];
        }
        return out;
    }

    // ---- formateo ----

    public final StringBuffer format(Object[] arguments, StringBuffer result, FieldPosition pos) {
        return this.escribir(arguments, result, pos, null);
    }

    public final StringBuffer format(Object arguments, StringBuffer result, FieldPosition pos) {
        return this.escribir((Object[]) arguments, result, pos, null);
    }

    /** El atajo de un solo uso: arma el formateador, formatea y lo tira. */
    public static String format(String pattern, Object... arguments) {
        MessageFormat temp = new MessageFormat(pattern);
        return temp.format(arguments, new StringBuffer(), new FieldPosition(0)).toString();
    }

    public AttributedCharacterIterator formatToCharacterIterator(Object arguments) {
        if (arguments == null) {
            throw new NullPointerException();
        }
        MarcasDeCampo marcas = new MarcasDeCampo();
        StringBuffer sb = new StringBuffer();
        this.escribir((Object[]) arguments, sb, null, marcas);
        return marcas.iterador(sb.toString());
    }

    private StringBuffer escribir(Object[] arguments, StringBuffer result, FieldPosition pos,
                                  MarcasDeCampo marcas) {
        MarcasDeCampo m = marcas;
        if (m == null) {
            m = new MarcasDeCampo();
        }
        int ultimo = 0;
        for (int i = 0; i < this.cantidad; i = i + 1) {
            result.append(this.pattern.substring(ultimo, this.offsets[i]));
            ultimo = this.offsets[i];
            int arg = this.argumentNumbers[i];
            int d = result.length();
            if (arguments == null || arg >= arguments.length) {
                // Un argumento que no vino se escribe como {n}, sin sustituir. Es información:
                // dice exactamente qué faltó, en vez de dejar un hueco vacío o reventar.
                result.append('{');
                result.append(Integer.toString(arg));
                result.append('}');
            } else {
                this.escribirArgumento(arguments[arg], this.formats[i], result);
            }
            // El valor del atributo es el NÚMERO de argumento, no la clave: en un mensaje con
            // varios huecos, "acá va un argumento" no dice cuál, y ese es justamente el dato.
            m.marcar((AttributedCharacterIterator.Attribute) java.text.MessageFormat.Field.ARGUMENT,
                    Integer.valueOf(arg), -1, d, result.length());
        }
        result.append(this.pattern.substring(ultimo, this.pattern.length()));
        m.aplicar(pos);
        return result;
    }

    private void escribirArgumento(Object valor, Format formato, StringBuffer result) {
        if (valor == null) {
            result.append("null");
            return;
        }
        Format f = formato;
        if (f == null) {
            // Sin formateador declarado el tipo del valor decide. Un Number va por el formateador
            // de números del locale y un Date por el de fecha y hora, porque su toString() no
            // respeta ningún locale.
            if (valor instanceof Number) {
                f = NumberFormat.getInstance(this.locale);
            } else if (valor instanceof Date) {
                f = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, this.locale);
            } else {
                result.append(valor.toString());
                return;
            }
        }
        String texto = f.format(valor);
        if (f instanceof ChoiceFormat && texto.indexOf('{') >= 0) {
            // Un ChoiceFormat puede devolver un patrón de mensaje ("{0} archivos"): se vuelve a
            // formatear con los mismos argumentos. Es lo que permite escribir plurales.
            result.append(new MessageFormat(texto, this.locale).format(new Object[] {valor}));
            return;
        }
        result.append(texto);
    }

    // ---- parseo ----

    /**
     * Lee los argumentos de un texto que sigue este patrón.
     *
     * <p>El algoritmo es el del JDK y su límite conviene decirlo: los pedazos literales se buscan
     * de izquierda a derecha y no se prueban alternativas. Un patrón cuyos literales sean ambiguos
     * ({@code "{0}{1}"}) no se puede parsear, y el resultado es un fallo, no una adivinanza.
     *
     * @return un arreglo con una casilla por argumento; las que el patrón no nombra quedan en null
     */
    public Object[] parse(String source, ParsePosition pos) {
        if (source == null) {
            return null;
        }
        Object[] resultado = new Object[this.maxArgumento + 1];
        int patronOffset = 0;
        int fuenteOffset = pos.getIndex();
        ParsePosition temp = new ParsePosition(0);
        for (int i = 0; i < this.cantidad; i = i + 1) {
            int largo = this.offsets[i] - patronOffset;
            if (largo == 0 || this.pattern.regionMatches(patronOffset, source, fuenteOffset, largo)) {
                fuenteOffset = fuenteOffset + largo;
                patronOffset = patronOffset + largo;
            } else {
                pos.setErrorIndex(fuenteOffset);
                return null;
            }
            if (this.formats[i] == null) {
                // Argumento sin formateador: se toma todo lo que haya hasta el próximo literal.
                // Si es el último, hasta el final — de ahí que el más largo posible sea la regla.
                int hasta = this.pattern.length();
                if (i + 1 < this.cantidad) {
                    hasta = this.offsets[i + 1];
                }
                int siguiente;
                if (patronOffset >= hasta) {
                    siguiente = source.length();
                } else {
                    siguiente = source.indexOf(this.pattern.substring(patronOffset, hasta),
                            fuenteOffset);
                }
                if (siguiente < 0) {
                    pos.setErrorIndex(fuenteOffset);
                    return null;
                }
                String valor = source.substring(fuenteOffset, siguiente);
                // Un "{n}" literal en la entrada es la marca de "este argumento no vino" que pone
                // el formateo: se lee como ausente y no como la cadena "{n}".
                if (!valor.equals("{" + Integer.toString(this.argumentNumbers[i]) + "}")) {
                    resultado[this.argumentNumbers[i]] = valor;
                }
                fuenteOffset = siguiente;
            } else {
                temp.setIndex(fuenteOffset);
                resultado[this.argumentNumbers[i]] = this.formats[i].parseObject(source, temp);
                if (temp.getIndex() == fuenteOffset) {
                    pos.setErrorIndex(fuenteOffset);
                    return null;
                }
                fuenteOffset = temp.getIndex();
            }
        }
        int largo = this.pattern.length() - patronOffset;
        if (largo == 0 || this.pattern.regionMatches(patronOffset, source, fuenteOffset, largo)) {
            pos.setIndex(fuenteOffset + largo);
        } else {
            pos.setErrorIndex(fuenteOffset);
            return null;
        }
        return resultado;
    }

    public Object[] parse(String source) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Object[] result = this.parse(source, pos);
        if (pos.getIndex() == 0) {
            throw new ParseException("MessageFormat parse error!", pos.getErrorIndex());
        }
        return result;
    }

    public Object parseObject(String source, ParsePosition pos) {
        return this.parse(source, pos);
    }

    // ---- identidad ----

    public Object clone() {
        MessageFormat copia = new MessageFormat(this.toPattern(), this.locale);
        for (int i = 0; i < this.cantidad && i < copia.cantidad; i = i + 1) {
            copia.formats[i] = this.formats[i];
        }
        return copia;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        MessageFormat other = (MessageFormat) obj;
        if (this.cantidad != other.cantidad || !this.pattern.equals(other.pattern)
                || !this.locale.equals(other.locale)) {
            return false;
        }
        for (int i = 0; i < this.cantidad; i = i + 1) {
            if (this.offsets[i] != other.offsets[i]
                    || this.argumentNumbers[i] != other.argumentNumbers[i]) {
                return false;
            }
            if (this.formats[i] == null) {
                if (other.formats[i] != null) {
                    return false;
                }
            } else if (!this.formats[i].equals(other.formats[i])) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        return this.pattern.hashCode();
    }
}
