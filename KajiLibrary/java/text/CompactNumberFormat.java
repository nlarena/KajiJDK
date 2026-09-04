package java.text;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * El formateador compacto: {@code 1234} sale {@code 1K} y {@code 1234567} sale {@code 1M}.
 *
 * <p><b>Por qué esta clase SÍ está y las fábricas de {@link NumberFormat} no.</b> Los sufijos
 * compactos son datos del CLDR —"K", "mil", "万"— y esta biblioteca no los trae. Pero los
 * constructores de esta clase <em>reciben los patrones del llamador</em>: el que la usa dice
 * {@code {"", "", "", "0K", "00K", "000K", "0M", ...}} y la clase no tiene que inventar nada. Por
 * eso puede funcionar honestamente sin tabla, mientras que
 * {@code NumberFormat.getCompactNumberInstance(locale, style)} —que tiene que producir esos
 * patrones por locale— no puede y quedó afuera.
 *
 * <p><b>Cómo se lee el arreglo de patrones.</b> La posición ES la magnitud: el índice {@code i}
 * gobierna los números de {@code i+1} dígitos. Un patrón vacío quiere decir "en esta magnitud no se
 * compacta" y el número sale entero. La cantidad de ceros del patrón dice cuánto se divide: en el
 * índice 3, {@code "0K"} (un cero) divide por {@code 10^3}, y en el índice 5, {@code "000K"} (tres
 * ceros) divide por {@code 10^(5-3+1)}, o sea también por mil. Es lo que hace que 1.000, 12.000 y
 * 999.000 se escriban todos en miles con tres patrones distintos.
 *
 * <p><b>El redondeo puede cambiar la magnitud, y hay que volver a elegir.</b> 999.999 cae en el
 * índice 5, se divide por mil y redondea a 1000 — escribirlo ahí daría "1000K". Por eso, después de
 * redondear, se vuelve a mirar en qué magnitud quedó el número y se repite: sale "1M", que es lo
 * que devuelve el JDK.
 *
 * <p>El cuarto argumento del constructor largo son las reglas de plural del locale, en la sintaxis
 * del CLDR ({@code "one:i = 1 and v = 0"}). Sirven para los patrones que traen variantes
 * ({@code "{one:0 mil other:0 mil}"}): se evalúa la regla contra el número ya dividido y se elige la
 * categoría. Si ninguna regla da, se usa {@code other}, que es la que el CLDR garantiza en todos los
 * locales.
 */
public final class CompactNumberFormat extends NumberFormat {

    private final String decimalPattern;
    private final DecimalFormatSymbols symbols;
    private final String[] compactPatterns;
    private final String pluralRules;
    private final DecimalFormat base;

    private RoundingMode roundingMode;
    private int groupingSize;
    private boolean strict;
    private boolean parseBigDecimal;

    public CompactNumberFormat(String decimalPattern, DecimalFormatSymbols symbols,
                               String[] compactPatterns) {
        this(decimalPattern, symbols, compactPatterns, "");
    }

    public CompactNumberFormat(String decimalPattern, DecimalFormatSymbols symbols,
                               String[] compactPatterns, String pluralRules) {
        if (decimalPattern == null || symbols == null || compactPatterns == null
                || pluralRules == null) {
            throw new NullPointerException();
        }
        this.decimalPattern = decimalPattern;
        this.symbols = (DecimalFormatSymbols) symbols.clone();
        this.compactPatterns = new String[compactPatterns.length];
        for (int i = 0; i < compactPatterns.length; i = i + 1) {
            this.compactPatterns[i] = compactPatterns[i];
        }
        this.pluralRules = pluralRules;
        this.base = new DecimalFormat(decimalPattern, this.symbols);
        this.roundingMode = RoundingMode.HALF_EVEN;
        this.groupingSize = 0;
        this.strict = false;
        this.parseBigDecimal = false;
        // Un formateador compacto no agrupa ni muestra decimales por omisión: "1K" y no "1.234K"
        // ni "1,2K". El patrón decimal que recibe se usa para los símbolos y para el caso en que la
        // magnitud NO se compacta, no para decidir esto.
        this.setGroupingUsed(false);
        this.setMinimumFractionDigits(0);
        this.setMaximumFractionDigits(0);
    }

    // ---- formateo ----

    public final StringBuffer format(Object number, StringBuffer toAppendTo, FieldPosition pos) {
        if (number instanceof BigDecimal) {
            return this.escribir((BigDecimal) number, toAppendTo, pos, null);
        }
        if (number instanceof BigInteger) {
            return this.escribir(new BigDecimal((BigInteger) number), toAppendTo, pos, null);
        }
        if (number instanceof Long || number instanceof Integer
                || number instanceof Short || number instanceof Byte) {
            return this.format(((Number) number).longValue(), toAppendTo, pos);
        }
        if (number instanceof Number) {
            return this.format(((Number) number).doubleValue(), toAppendTo, pos);
        }
        throw new IllegalArgumentException("Cannot format given Object as a Number");
    }

    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
        if (Double.isNaN(number) || Double.isInfinite(number)) {
            // Ni NaN ni infinito tienen magnitud, así que no hay índice compacto que elegir: los
            // escribe el formateador decimal, que sí sabe cómo se llaman en este locale.
            return this.base.format(number, toAppendTo, pos);
        }
        return this.escribir(new BigDecimal(number), toAppendTo, pos, null);
    }

    public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
        return this.escribir(BigDecimal.valueOf(number), toAppendTo, pos, null);
    }

    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        if (!(obj instanceof Number)) {
            throw new IllegalArgumentException("Cannot format given Object as a Number");
        }
        MarcasDeCampo marcas = new MarcasDeCampo();
        StringBuffer sb = new StringBuffer();
        BigDecimal v;
        if (obj instanceof BigDecimal) {
            v = (BigDecimal) obj;
        } else if (obj instanceof BigInteger) {
            v = new BigDecimal((BigInteger) obj);
        } else if (obj instanceof Long || obj instanceof Integer
                || obj instanceof Short || obj instanceof Byte) {
            v = BigDecimal.valueOf(((Number) obj).longValue());
        } else {
            v = new BigDecimal(((Number) obj).doubleValue());
        }
        this.escribir(v, sb, null, marcas);
        return marcas.iterador(sb.toString());
    }

    private StringBuffer escribir(BigDecimal valor, StringBuffer out, FieldPosition pos,
                                  MarcasDeCampo marcas) {
        MarcasDeCampo m = marcas;
        if (m == null) {
            m = new MarcasDeCampo();
        }
        int indice = this.indiceDe(valor);
        String patron = null;
        if (indice >= 0 && indice < this.compactPatterns.length) {
            patron = this.compactPatterns[indice];
        }
        if (patron == null || patron.length() == 0) {
            return this.base.format(valor, out, pos);
        }

        BigDecimal dividido = valor;
        int vueltas = 0;
        // Como máximo dos vueltas: la primera elige por la magnitud original, la segunda por la que
        // quedó después de redondear. Una tercera no puede cambiar nada — redondear un número ya
        // redondeado a la misma escala lo deja igual.
        while (vueltas < 2) {
            int ceros = CompactNumberFormat.cerosDe(patron);
            int exp = indice - ceros + 1;
            if (exp < 0) {
                exp = 0;
            }
            dividido = valor.movePointLeft(exp).setScale(this.getMaximumFractionDigits(),
                    this.roundingMode);
            BigDecimal reconstruido = dividido.movePointRight(exp);
            int nuevo = this.indiceDe(reconstruido);
            if (nuevo == indice || nuevo < 0 || nuevo >= this.compactPatterns.length) {
                break;
            }
            String otro = this.compactPatterns[nuevo];
            if (otro == null || otro.length() == 0) {
                break;
            }
            indice = nuevo;
            patron = otro;
            vueltas = vueltas + 1;
        }

        String elegido = this.elegirVariante(patron, dividido);
        String prefijo = CompactNumberFormat.afijo(elegido, true);
        String sufijo = CompactNumberFormat.afijo(elegido, false);

        int base0 = out.length();
        StringBuilder sb = new StringBuilder();
        boolean negativo = dividido.signum() < 0;
        if (negativo) {
            int d = sb.length();
            sb.append(this.symbols.getMinusSign());
            m.marcar((AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.SIGN, -1, base0 + d, base0 + sb.length());
        }
        int dp = sb.length();
        sb.append(prefijo);
        m.marcar((AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.PREFIX, -1, base0 + dp, base0 + sb.length());

        DecimalFormat cuerpo = new DecimalFormat(this.decimalPattern, this.symbols);
        cuerpo.setGroupingUsed(this.isGroupingUsed());
        if (this.groupingSize > 0) {
            cuerpo.setGroupingSize(this.groupingSize);
        }
        cuerpo.setMaximumIntegerDigits(this.getMaximumIntegerDigits());
        cuerpo.setMinimumIntegerDigits(this.getMinimumIntegerDigits());
        cuerpo.setMaximumFractionDigits(this.getMaximumFractionDigits());
        cuerpo.setMinimumFractionDigits(this.getMinimumFractionDigits());
        cuerpo.setRoundingMode(this.roundingMode);
        int dn = sb.length();
        sb.append(cuerpo.format(dividido.abs()));
        m.marcar((AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.INTEGER, NumberFormat.INTEGER_FIELD,
                base0 + dn, base0 + sb.length());

        int ds = sb.length();
        sb.append(sufijo);
        m.marcar((AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.SUFFIX, -1, base0 + ds, base0 + sb.length());

        out.append(sb.toString());
        m.aplicar(pos);
        return out;
    }

    // El índice es "cuántos dígitos enteros tiene, menos uno". Un cero cae en el índice 0 igual que
    // un uno: los dos tienen un solo dígito.
    private int indiceDe(BigDecimal valor) {
        BigDecimal abs = valor.abs();
        if (abs.signum() == 0) {
            return 0;
        }
        String enteros = abs.setScale(0, RoundingMode.DOWN).toPlainString();
        int digitos = enteros.length();
        if (enteros.equals("0")) {
            digitos = 1;
        }
        int i = digitos - 1;
        if (i >= this.compactPatterns.length) {
            i = this.compactPatterns.length - 1;
        }
        return i;
    }

    // Los ceros se cuentan sobre UNA variante, no sobre el patrón entero: "{one:0 mil other:0
    // miles}" tiene dos ceros escritos pero divide por mil, no por cien. Todas las variantes de un
    // patrón compacto comparten la magnitud —es lo que las hace variantes de un mismo patrón— así
    // que alcanza con la primera.
    private static int cerosDe(String patron) {
        String p = CompactNumberFormat.variante(patron, null);
        int n = 0;
        boolean citado = false;
        for (int i = 0; i < p.length(); i = i + 1) {
            char c = p.charAt(i);
            if (c == '\'') {
                citado = !citado;
            } else if (!citado && c == '0') {
                n = n + 1;
            }
        }
        if (n == 0) {
            return 1;
        }
        return n;
    }

    private static String afijo(String patron, boolean prefijo) {
        StringBuilder sb = new StringBuilder();
        boolean citado = false;
        boolean vistoDigito = false;
        for (int i = 0; i < patron.length(); i = i + 1) {
            char c = patron.charAt(i);
            if (c == '\'') {
                citado = !citado;
                continue;
            }
            if (!citado && c == '0') {
                vistoDigito = true;
                continue;
            }
            if (prefijo && !vistoDigito) {
                sb.append(c);
            } else if (!prefijo && vistoDigito) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // Un patrón con variantes se escribe "{cat:patrón cat:patrón}". Sin variantes se devuelve tal
    // cual, que es el caso de los constructores de tres argumentos.
    private String elegirVariante(String patron, BigDecimal valor) {
        if (patron.length() == 0 || patron.charAt(0) != '{') {
            return patron;
        }
        return CompactNumberFormat.variante(patron,
                ReglasDePlural.categoria(this.pluralRules, valor));
    }

    /**
     * La variante de una categoría dentro de un patrón con variantes.
     *
     * @param categoria la categoría buscada, o {@code null} para quedarse con la primera. El
     *                  {@code null} no es un "no sé" disfrazado: lo usa {@code cerosDe}, al que
     *                  sólo le interesa la magnitud, y ésa es la misma en todas las variantes.
     */
    private static String variante(String patron, String categoria) {
        if (patron.length() == 0 || patron.charAt(0) != '{') {
            return patron;
        }
        String cuerpo = patron.substring(1, patron.length() - 1);
        String otra = null;
        int i = 0;
        while (i < cuerpo.length()) {
            int dosPuntos = cuerpo.indexOf(':', i);
            if (dosPuntos < 0) {
                break;
            }
            String cat = cuerpo.substring(i, dosPuntos).trim();
            int fin = cuerpo.length();
            // El patrón de una categoría termina donde arranca la siguiente, que se reconoce por
            // "palabra:" — el espacio solo no alcanza, porque un patrón puede tener espacios.
            for (int k = dosPuntos + 1; k < cuerpo.length(); k = k + 1) {
                if (cuerpo.charAt(k) == ':') {
                    int atras = k - 1;
                    while (atras > dosPuntos && cuerpo.charAt(atras) != ' ') {
                        atras = atras - 1;
                    }
                    if (atras > dosPuntos) {
                        fin = atras;
                        break;
                    }
                }
            }
            String valorPat = cuerpo.substring(dosPuntos + 1, fin).trim();
            if (categoria == null || cat.equals(categoria)) {
                return valorPat;
            }
            if (cat.equals("other")) {
                otra = valorPat;
            }
            i = fin + 1;
            if (fin >= cuerpo.length()) {
                break;
            }
        }
        if (otra != null) {
            return otra;
        }
        return "";
    }

    // ---- parseo ----

    /**
     * Lee un número compacto.
     *
     * <p>Se busca el sufijo más largo que coincida y se multiplica por su magnitud: {@code "12K"}
     * da 12000. El más largo y no el primero, porque los sufijos de una misma tabla se contienen
     * ({@code "mil"} y {@code "millones"} empiezan igual) y quedarse con el primero daría un factor
     * mil veces menor sin que nadie se entere.
     */
    public Number parse(String text, ParsePosition pos) {
        if (text == null) {
            throw new NullPointerException();
        }
        int inicio = pos.getIndex();
        ParsePosition tmp = new ParsePosition(inicio);
        Number n = this.base.parse(text, tmp);
        if (n == null || tmp.getIndex() == inicio) {
            pos.setErrorIndex(inicio);
            return null;
        }
        int tras = tmp.getIndex();
        int mejorExp = -1;
        int mejorLargo = -1;
        for (int i = 0; i < this.compactPatterns.length; i = i + 1) {
            String p = this.compactPatterns[i];
            if (p == null || p.length() == 0) {
                continue;
            }
            String elegido = this.elegirVariante(p, BigDecimal.valueOf(n.longValue()));
            String sufijo = CompactNumberFormat.afijo(elegido, false);
            if (sufijo.length() == 0) {
                continue;
            }
            if (sufijo.length() > mejorLargo && text.startsWith(sufijo, tras)) {
                mejorLargo = sufijo.length();
                mejorExp = i - CompactNumberFormat.cerosDe(elegido) + 1;
            }
        }
        BigDecimal v = new BigDecimal(n.toString());
        if (mejorExp >= 0) {
            v = v.movePointRight(mejorExp);
            tras = tras + mejorLargo;
        }
        pos.setIndex(tras);
        if (this.parseBigDecimal) {
            return v;
        }
        BigDecimal limpio = v.stripTrailingZeros();
        if (limpio.scale() <= 0) {
            try {
                return Long.valueOf(limpio.longValueExact());
            } catch (ArithmeticException e) {
                return Double.valueOf(v.doubleValue());
            }
        }
        return Double.valueOf(v.doubleValue());
    }

    // ---- estado ----

    public void setMaximumIntegerDigits(int newValue) {
        super.setMaximumIntegerDigits(newValue);
    }

    public void setMinimumIntegerDigits(int newValue) {
        super.setMinimumIntegerDigits(newValue);
    }

    public void setMinimumFractionDigits(int newValue) {
        super.setMinimumFractionDigits(newValue);
    }

    public void setMaximumFractionDigits(int newValue) {
        super.setMaximumFractionDigits(newValue);
    }

    public RoundingMode getRoundingMode() {
        return this.roundingMode;
    }

    public void setRoundingMode(RoundingMode roundingMode) {
        if (roundingMode == null) {
            throw new NullPointerException();
        }
        this.roundingMode = roundingMode;
    }

    public int getGroupingSize() {
        return this.groupingSize;
    }

    public void setGroupingSize(int newValue) {
        this.groupingSize = newValue;
    }

    public boolean isGroupingUsed() {
        return super.isGroupingUsed();
    }

    public void setGroupingUsed(boolean newValue) {
        super.setGroupingUsed(newValue);
    }

    public boolean isParseIntegerOnly() {
        return super.isParseIntegerOnly();
    }

    public void setParseIntegerOnly(boolean value) {
        super.setParseIntegerOnly(value);
    }

    public boolean isStrict() {
        return this.strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public boolean isParseBigDecimal() {
        return this.parseBigDecimal;
    }

    public void setParseBigDecimal(boolean newValue) {
        this.parseBigDecimal = newValue;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        CompactNumberFormat other = (CompactNumberFormat) obj;
        if (!this.decimalPattern.equals(other.decimalPattern)
                || !this.symbols.equals(other.symbols)
                || !this.pluralRules.equals(other.pluralRules)
                || this.roundingMode != other.roundingMode
                || this.groupingSize != other.groupingSize
                || this.strict != other.strict
                || this.parseBigDecimal != other.parseBigDecimal
                || this.compactPatterns.length != other.compactPatterns.length) {
            return false;
        }
        for (int i = 0; i < this.compactPatterns.length; i = i + 1) {
            if (!this.compactPatterns[i].equals(other.compactPatterns[i])) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        return this.decimalPattern.hashCode() * 31 + this.compactPatterns.length;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CompactNumberFormat [decimal pattern: \"");
        sb.append(this.decimalPattern);
        sb.append("\", compact patterns: \"[");
        for (int i = 0; i < this.compactPatterns.length; i = i + 1) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(this.compactPatterns[i]);
        }
        sb.append("]\"]");
        return sb.toString();
    }

    public CompactNumberFormat clone() {
        CompactNumberFormat c = new CompactNumberFormat(this.decimalPattern, this.symbols,
                this.compactPatterns, this.pluralRules);
        c.roundingMode = this.roundingMode;
        c.groupingSize = this.groupingSize;
        c.strict = this.strict;
        c.parseBigDecimal = this.parseBigDecimal;
        c.setGroupingUsed(this.isGroupingUsed());
        c.setParseIntegerOnly(this.isParseIntegerOnly());
        c.setMaximumIntegerDigits(this.getMaximumIntegerDigits());
        c.setMinimumIntegerDigits(this.getMinimumIntegerDigits());
        c.setMaximumFractionDigits(this.getMaximumFractionDigits());
        c.setMinimumFractionDigits(this.getMinimumFractionDigits());
        return c;
    }
}
