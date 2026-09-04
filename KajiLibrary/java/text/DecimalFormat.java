package java.text;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Currency;

// KajiLibrary's java.text.DecimalFormat — pattern-based number formatting and parsing. A pattern
// like `#,##0.00` is parsed into a prefix/suffix, a minimum integer-digit count, grouping, and a
// min/max fraction-digit count; format(double) then renders a number to match, and parse() reads it
// back.
//
// The PATTERN and the SYMBOLS are separate concerns, and keeping them apart is the design:
// the pattern says the structure and is always written in the standard alphabet ('#', '0',
// ',', '.', '%', '\u00a4'), while DecimalFormatSymbols says which characters draw it. So the same
// `#,##0.00` renders 1,234.50 with US symbols and 1.234,50 with German ones, and applyPattern
// never has to know which locale it is in. `applyLocalizedPattern` es la puerta de atrás para el
// caso contrario —un patrón escrito ya en el alfabeto del locale— y lo único que hace es traducirlo
// al estándar antes de aplicarlo, para que no haya dos analizadores de patrones que mantener.
//
// Los AFIJOS se guardan dos veces, y hay que saber por qué: una como PATRÓN (con el `¤` sin
// resolver) y otra ya EXPANDIDA. Si se guardara sólo lo expandido, cambiar la moneda con
// setCurrency dejaría el símbolo viejo pegado; si se guardara sólo el patrón, getPositivePrefix()
// tendría que expandir en cada llamada y no podría devolver un prefijo puesto a mano con
// setPositivePrefix, que no viene de ningún patrón.
//
// Rounding goes through java.math.BigDecimal, not through double arithmetic. That matters twice:
//   - a value on an exact rounding boundary now agrees with the JDK. format(2.675) with "0.00"
//     gives "2.67", because `new BigDecimal(double)` sees the double's EXACT binary value
//     (2.674999999999999822...), which is the same thing the JDK's formatter rounds;
//   - magnitudes past ~9.2e18 work. Computing the integer part with `(long) magnitude` SATURATES at
//     Long.MAX_VALUE and produces structurally corrupt output; digits come from
//     BigDecimal.toPlainString(), which has no such ceiling.
//
// Subset que queda: la notación científica (patrones con 'E') no está — ni al formatear ni al
// parsear— y el patrón la rechaza en lugar de ignorarla en silencio.
public class DecimalFormat extends NumberFormat {

    private static final char PAT_CERO = '0';
    private static final char PAT_DIGITO = '#';
    private static final char PAT_GRUPO = ',';
    private static final char PAT_DECIMAL = '.';
    private static final char PAT_SEPARADOR = ';';
    private static final char PAT_PORCIENTO = '%';
    // Escritos como escape, igual que en las tablas de símbolos: son dos caracteres que un editor
    // puede romper sin que se note, y el '\u00a4' además es invisible en varias fuentes.
    private static final char PAT_PERMIL = '\u2030';
    private static final char PAT_MONEDA = '\u00a4';
    private static final char PAT_MENOS = '-';

    // El patrón del afijo (null si el afijo se puso a mano) y el afijo ya expandido.
    private String posPrefijoPat;
    private String posSufijoPat;
    private String negPrefijoPat;
    private String negSufijoPat;
    private String posPrefijo;
    private String posSufijo;
    private String negPrefijo;
    private String negSufijo;

    private int groupingSize;
    private int multiplier;
    private boolean decimalSeparatorAlwaysShown;
    private boolean parseBigDecimal;
    private boolean strict;
    private RoundingMode roundingMode;
    private DecimalFormatSymbols symbols;

    public DecimalFormat() {
        this.symbols = new DecimalFormatSymbols();
        this.inicializar();
        this.applyPattern("#,##0.###");
    }

    public DecimalFormat(String pattern) {
        this.symbols = new DecimalFormatSymbols();
        this.inicializar();
        this.applyPattern(pattern);
    }

    // The symbols are assigned first: applyPattern expands the affixes through them, so a pattern
    // applied before them would bake in the wrong currency symbol.
    public DecimalFormat(String pattern, DecimalFormatSymbols symbols) {
        this.symbols = (DecimalFormatSymbols) symbols.clone();
        this.inicializar();
        this.applyPattern(pattern);
    }

    private void inicializar() {
        this.groupingSize = 3;
        this.multiplier = 1;
        this.decimalSeparatorAlwaysShown = false;
        this.parseBigDecimal = false;
        this.strict = false;
        this.roundingMode = RoundingMode.HALF_EVEN;
    }

    public DecimalFormatSymbols getDecimalFormatSymbols() {
        return (DecimalFormatSymbols) this.symbols.clone();
    }

    // Copied on the way in and out, so a caller mutating its own instance cannot reach inside a
    // live formatter. Re-expands the affixes because the currency symbol and the minus sign salen
    // de acá.
    public void setDecimalFormatSymbols(DecimalFormatSymbols newSymbols) {
        this.symbols = (DecimalFormatSymbols) newSymbols.clone();
        this.expandirAfijos();
    }

    // ---- patrón ----

    public void applyPattern(String pattern) {
        this.aplicar(pattern, false);
    }

    /**
     * Aplica un patrón escrito en el alfabeto del locale ({@code #.##0,00} en alemán).
     *
     * <p>Traduce al alfabeto estándar y delega: mantener dos analizadores de patrones —uno por
     * alfabeto— es la forma segura de que los dos se separen con el tiempo.
     */
    public void applyLocalizedPattern(String pattern) {
        this.aplicar(pattern, true);
    }

    private void aplicar(String pattern, boolean localizado) {
        if (pattern == null) {
            throw new NullPointerException();
        }
        String pat = pattern;
        if (localizado) {
            pat = this.aEstandar(pattern);
        }
        this.parsearPatron(pat);
    }

    // Traduce del alfabeto del locale al estándar, respetando el entrecomillado: un separador
    // decimal DENTRO de comillas es texto literal del afijo y no hay que tocarlo.
    private String aEstandar(String pat) {
        StringBuilder sb = new StringBuilder();
        boolean citado = false;
        for (int i = 0; i < pat.length(); i = i + 1) {
            char c = pat.charAt(i);
            if (c == '\'') {
                citado = !citado;
                sb.append(c);
            } else if (citado) {
                sb.append(c);
            } else if (c == this.symbols.getZeroDigit()) {
                sb.append(DecimalFormat.PAT_CERO);
            } else if (c == this.symbols.getDigit()) {
                sb.append(DecimalFormat.PAT_DIGITO);
            } else if (c == this.symbols.getGroupingSeparator()) {
                sb.append(DecimalFormat.PAT_GRUPO);
            } else if (c == this.symbols.getDecimalSeparator()) {
                sb.append(DecimalFormat.PAT_DECIMAL);
            } else if (c == this.symbols.getPatternSeparator()) {
                sb.append(DecimalFormat.PAT_SEPARADOR);
            } else if (c == this.symbols.getPercent()) {
                sb.append(DecimalFormat.PAT_PORCIENTO);
            } else if (c == this.symbols.getPerMill()) {
                sb.append(DecimalFormat.PAT_PERMIL);
            } else if (c == this.symbols.getMinusSign()) {
                sb.append(DecimalFormat.PAT_MENOS);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void parsearPatron(String pat) {
        int semi = this.finDeSubpatron(pat);
        String pos = pat;
        String neg = null;
        if (semi >= 0) {
            pos = pat.substring(0, semi);
            neg = pat.substring(semi + 1, pat.length());
        }

        int ini = this.inicioDelNumero(pos);
        int fin = this.finDelNumero(pos, ini);
        this.posPrefijoPat = pos.substring(0, ini);
        this.posSufijoPat = pos.substring(fin, pos.length());
        String num = pos.substring(ini, fin);

        // Los dígitos NO se cuentan "los de la izquierda del punto y los de la derecha", que es lo
        // intuitivo y da mal. Se cuentan en tres tramos —los '#' antes del primer '0', los '0', y
        // los '#' después del último '0'— más la posición del punto DENTRO de esa secuencia. Es el
        // modelo del JDK y es el único que resuelve "###.###" y "#." como corresponde: en los dos
        // no hay ningún '0' y aun así el mínimo de dígitos enteros es 1, no 0.
        int digitosIzq = 0;
        int ceros = 0;
        int digitosDer = 0;
        int posPunto = -1;
        int enGrupo = -1;
        for (int i = 0; i < num.length(); i = i + 1) {
            char c = num.charAt(i);
            if (c == DecimalFormat.PAT_DIGITO) {
                if (ceros > 0) {
                    digitosDer = digitosDer + 1;
                } else {
                    digitosIzq = digitosIzq + 1;
                }
                if (enGrupo >= 0 && posPunto < 0) {
                    enGrupo = enGrupo + 1;
                }
            } else if (c == DecimalFormat.PAT_CERO) {
                if (digitosDer > 0) {
                    throw new IllegalArgumentException("Unexpected '0' in pattern: " + pat);
                }
                ceros = ceros + 1;
                if (enGrupo >= 0 && posPunto < 0) {
                    enGrupo = enGrupo + 1;
                }
            } else if (c == DecimalFormat.PAT_GRUPO) {
                enGrupo = 0;
            } else if (c == DecimalFormat.PAT_DECIMAL) {
                if (posPunto < 0) {
                    posPunto = digitosIzq + ceros + digitosDer;
                }
            }
        }
        int digitosTotal = digitosIzq + ceros + digitosDer;

        // Un patrón sin ningún '0' y con punto ("###.###", "#.", ".###") se reescribe como si
        // tuviera un cero en la posición del punto. Sin esto el mínimo de enteros daría 0 y el
        // formateo perdería el dígito de las unidades.
        if (ceros == 0 && digitosIzq > 0 && posPunto >= 0) {
            int n = posPunto;
            if (n == 0) {
                n = 1;
            }
            digitosDer = digitosIzq - n;
            digitosIzq = n - 1;
            ceros = 1;
        }

        int puntoEfectivo = digitosTotal;
        if (posPunto >= 0) {
            puntoEfectivo = posPunto;
        }
        int minEnteros = puntoEfectivo - digitosIzq;
        int maxFrac = 0;
        int minFrac = 0;
        if (posPunto >= 0) {
            maxFrac = digitosTotal - posPunto;
            minFrac = digitosIzq + ceros - posPunto;
        }

        // El tamaño del grupo son los dígitos que siguieron a la ÚLTIMA coma: "#,##,##0" agrupa de
        // a tres porque eso es lo que dice la última, no el promedio de las dos.
        this.setGroupingUsed(enGrupo > 0);
        if (enGrupo > 0) {
            this.groupingSize = enGrupo;
        } else {
            this.groupingSize = 0;
        }

        // El orden importa: primero los máximos, después los mínimos. Los setters se ajustan entre
        // sí y hacerlo al revés dejaría el máximo pisado por un mínimo transitorio.
        this.setMaximumIntegerDigits(Integer.MAX_VALUE);
        this.setMinimumIntegerDigits(minEnteros);
        this.setMaximumFractionDigits(maxFrac);
        this.setMinimumFractionDigits(minFrac);
        // El punto se muestra siempre cuando está en un extremo de la secuencia de dígitos: "#."
        // pide el separador aunque no haya decimales, y ".##" también.
        this.decimalSeparatorAlwaysShown = posPunto == 0 || posPunto == digitosTotal;

        if (neg != null) {
            // Del subpatrón negativo el JDK sólo usa los AFIJOS: la forma del número la manda el
            // positivo, y aceptar dos formas distintas daría un formateador con dos gramáticas.
            int ni = this.inicioDelNumero(neg);
            int nf = this.finDelNumero(neg, ni);
            this.negPrefijoPat = neg.substring(0, ni);
            this.negSufijoPat = neg.substring(nf, neg.length());
        } else {
            this.negPrefijoPat = DecimalFormat.PAT_MENOS + this.posPrefijoPat;
            this.negSufijoPat = this.posSufijoPat;
        }

        this.multiplier = 1;
        if (this.tiene(this.posPrefijoPat, DecimalFormat.PAT_PORCIENTO)
                || this.tiene(this.posSufijoPat, DecimalFormat.PAT_PORCIENTO)) {
            this.multiplier = 100;
        } else if (this.tiene(this.posPrefijoPat, DecimalFormat.PAT_PERMIL)
                || this.tiene(this.posSufijoPat, DecimalFormat.PAT_PERMIL)) {
            this.multiplier = 1000;
        }

        this.expandirAfijos();
    }

    // El ';' que separa subpatrones, saltando el que esté entrecomillado.
    private int finDeSubpatron(String pat) {
        boolean citado = false;
        for (int i = 0; i < pat.length(); i = i + 1) {
            char c = pat.charAt(i);
            if (c == '\'') {
                citado = !citado;
            } else if (!citado && c == DecimalFormat.PAT_SEPARADOR) {
                return i;
            }
        }
        return -1;
    }

    private int inicioDelNumero(String s) {
        boolean citado = false;
        for (int i = 0; i < s.length(); i = i + 1) {
            char c = s.charAt(i);
            if (c == '\'') {
                citado = !citado;
            } else if (!citado && (c == DecimalFormat.PAT_DIGITO || c == DecimalFormat.PAT_CERO)) {
                return i;
            } else if (!citado && (c == 'E' || c == 'e')) {
                throw new IllegalArgumentException("Scientific notation is not supported: " + s);
            }
        }
        return s.length();
    }

    private int finDelNumero(String s, int desde) {
        int i = desde;
        while (i < s.length() && DecimalFormat.esDelNumero(s.charAt(i))) {
            i = i + 1;
        }
        if (i < s.length() && (s.charAt(i) == 'E' || s.charAt(i) == 'e')) {
            throw new IllegalArgumentException("Scientific notation is not supported: " + s);
        }
        return i;
    }

    private static boolean esDelNumero(char c) {
        return c == DecimalFormat.PAT_DIGITO || c == DecimalFormat.PAT_CERO
                || c == DecimalFormat.PAT_GRUPO || c == DecimalFormat.PAT_DECIMAL;
    }

    private boolean tiene(String s, char c) {
        if (s == null) {
            return false;
        }
        boolean citado = false;
        for (int i = 0; i < s.length(); i = i + 1) {
            char x = s.charAt(i);
            if (x == '\'') {
                citado = !citado;
            } else if (!citado && x == c) {
                return true;
            }
        }
        return false;
    }

    private void expandirAfijos() {
        if (this.posPrefijoPat != null) {
            this.posPrefijo = this.expandir(this.posPrefijoPat, null, 0);
        }
        if (this.posSufijoPat != null) {
            this.posSufijo = this.expandir(this.posSufijoPat, null, 0);
        }
        if (this.negPrefijoPat != null) {
            this.negPrefijo = this.expandir(this.negPrefijoPat, null, 0);
        }
        if (this.negSufijoPat != null) {
            this.negSufijo = this.expandir(this.negSufijoPat, null, 0);
        }
    }

    // Resuelve el patrón de un afijo contra los símbolos, y de paso marca los pedazos que son un
    // campo (moneda, signo, porcentaje). Las marcas se piden sólo al formatear; para el getter del
    // afijo se pasa `marcas` en null.
    private String expandir(String patron, MarcasDeCampo marcas, int base) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < patron.length()) {
            char c = patron.charAt(i);
            if (c == '\'') {
                i = i + 1;
                if (i < patron.length() && patron.charAt(i) == '\'') {
                    sb.append('\'');
                    i = i + 1;
                } else {
                    while (i < patron.length() && patron.charAt(i) != '\'') {
                        sb.append(patron.charAt(i));
                        i = i + 1;
                    }
                    i = i + 1;
                }
            } else if (c == DecimalFormat.PAT_MONEDA) {
                int d = sb.length();
                if (i + 1 < patron.length() && patron.charAt(i + 1) == DecimalFormat.PAT_MONEDA) {
                    sb.append(this.symbols.getInternationalCurrencySymbol());
                    i = i + 2;
                } else {
                    sb.append(this.symbols.getCurrencySymbol());
                    i = i + 1;
                }
                this.marcar(marcas, (AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.CURRENCY, base + d, base + sb.length());
            } else if (c == DecimalFormat.PAT_PORCIENTO) {
                int d = sb.length();
                sb.append(this.symbols.getPercent());
                this.marcar(marcas, (AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.PERCENT, base + d, base + sb.length());
                i = i + 1;
            } else if (c == DecimalFormat.PAT_PERMIL) {
                int d = sb.length();
                sb.append(this.symbols.getPerMill());
                this.marcar(marcas, (AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.PERMILLE, base + d, base + sb.length());
                i = i + 1;
            } else if (c == DecimalFormat.PAT_MENOS) {
                int d = sb.length();
                sb.append(this.symbols.getMinusSign());
                this.marcar(marcas, (AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.SIGN, base + d, base + sb.length());
                i = i + 1;
            } else {
                sb.append(c);
                i = i + 1;
            }
        }
        return sb.toString();
    }

    private void marcar(MarcasDeCampo marcas, AttributedCharacterIterator.Attribute campo,
                        int d, int h) {
        if (marcas != null) {
            marcas.marcar((AttributedCharacterIterator.Attribute) campo, -1, d, h);
        }
    }

    /**
     * Sintetiza el patrón que describe el estado ACTUAL, que no es necesariamente el que se aplicó:
     * después de un {@code setMinimumFractionDigits(5)} el patrón devuelto lleva cinco ceros.
     * Devolver la cadena original sería más fácil y mentiría en cuanto alguien toque un setter.
     */
    public String toPattern() {
        return this.sintetizar(false);
    }

    public String toLocalizedPattern() {
        return this.sintetizar(true);
    }

    private String sintetizar(boolean localizado) {
        char cero = DecimalFormat.PAT_CERO;
        char digito = DecimalFormat.PAT_DIGITO;
        char grupo = DecimalFormat.PAT_GRUPO;
        char decimal = DecimalFormat.PAT_DECIMAL;
        char separador = DecimalFormat.PAT_SEPARADOR;
        if (localizado) {
            cero = this.symbols.getZeroDigit();
            digito = this.symbols.getDigit();
            grupo = this.symbols.getGroupingSeparator();
            decimal = this.symbols.getDecimalSeparator();
            separador = this.symbols.getPatternSeparator();
        }
        StringBuilder r = new StringBuilder();
        int j = 1;
        while (j >= 0) {
            if (j == 1) {
                r.append(this.afijoParaPatron(this.posPrefijoPat, this.posPrefijo, localizado));
            } else {
                r.append(this.afijoParaPatron(this.negPrefijoPat, this.negPrefijo, localizado));
            }
            int cuantos = Math.max(this.groupingSize, this.getMinimumIntegerDigits()) + 1;
            int i = cuantos;
            while (i > 0) {
                if (i != cuantos && this.isGroupingUsed() && this.groupingSize != 0
                        && i % this.groupingSize == 0) {
                    r.append(grupo);
                }
                if (i <= this.getMinimumIntegerDigits()) {
                    r.append(cero);
                } else {
                    r.append(digito);
                }
                i = i - 1;
            }
            if (this.getMaximumFractionDigits() > 0 || this.decimalSeparatorAlwaysShown) {
                r.append(decimal);
            }
            i = 0;
            while (i < this.getMaximumFractionDigits()) {
                if (i < this.getMinimumFractionDigits()) {
                    r.append(cero);
                } else {
                    r.append(digito);
                }
                i = i + 1;
            }
            if (j == 1) {
                r.append(this.afijoParaPatron(this.posSufijoPat, this.posSufijo, localizado));
                // El subpatrón negativo se omite cuando no dice nada nuevo: sólo "menos y lo mismo".
                if (this.negativoEsElDefault()) {
                    j = -1;
                } else {
                    r.append(separador);
                    j = 0;
                }
            } else {
                r.append(this.afijoParaPatron(this.negSufijoPat, this.negSufijo, localizado));
                j = -1;
            }
        }
        return r.toString();
    }

    private boolean negativoEsElDefault() {
        String esperadoPrefijo = DecimalFormat.PAT_MENOS + this.textoDe(this.posPrefijoPat, this.posPrefijo);
        return this.textoDe(this.negPrefijoPat, this.negPrefijo).equals(esperadoPrefijo)
                && this.textoDe(this.negSufijoPat, this.negSufijo)
                        .equals(this.textoDe(this.posSufijoPat, this.posSufijo));
    }

    private String textoDe(String patron, String literal) {
        if (patron != null) {
            return patron;
        }
        if (literal == null) {
            return "";
        }
        return literal;
    }

    // Un afijo puesto a mano no tiene patrón: se reentrecomilla para que volver a aplicar el patrón
    // sintetizado dé el mismo afijo y no lo reinterprete como moneda o porcentaje.
    private String afijoParaPatron(String patron, String literal, boolean localizado) {
        if (patron != null) {
            if (!localizado) {
                return patron;
            }
            return this.aLocalizado(patron);
        }
        if (literal == null || literal.length() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < literal.length(); i = i + 1) {
            char c = literal.charAt(i);
            if (c == '\'') {
                sb.append("''");
            } else if (DecimalFormat.esDelNumero(c) || c == DecimalFormat.PAT_SEPARADOR
                    || c == DecimalFormat.PAT_PORCIENTO || c == DecimalFormat.PAT_PERMIL
                    || c == DecimalFormat.PAT_MONEDA || c == DecimalFormat.PAT_MENOS) {
                sb.append('\'').append(c).append('\'');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String aLocalizado(String patron) {
        StringBuilder sb = new StringBuilder();
        boolean citado = false;
        for (int i = 0; i < patron.length(); i = i + 1) {
            char c = patron.charAt(i);
            if (c == '\'') {
                citado = !citado;
                sb.append(c);
            } else if (citado) {
                sb.append(c);
            } else if (c == DecimalFormat.PAT_PORCIENTO) {
                sb.append(this.symbols.getPercent());
            } else if (c == DecimalFormat.PAT_PERMIL) {
                sb.append(this.symbols.getPerMill());
            } else if (c == DecimalFormat.PAT_MENOS) {
                sb.append(this.symbols.getMinusSign());
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ---- formateo ----

    // Se redefine para que BigDecimal y BigInteger NO pasen por double: son justamente los tipos que
    // el llamador eligió para no perder dígitos, y convertirlos a double los perdería.
    public StringBuffer format(Object number, StringBuffer toAppendTo, FieldPosition pos) {
        if (number instanceof BigDecimal) {
            return this.escribir((BigDecimal) number, toAppendTo, pos, null);
        }
        if (number instanceof BigInteger) {
            return this.escribir(new BigDecimal((BigInteger) number), toAppendTo, pos, null);
        }
        return super.format(number, toAppendTo, pos);
    }

    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
        if (Double.isNaN(number)) {
            return this.escribirEspecial(this.symbols.getNaN(), false, toAppendTo, pos, null);
        }
        if (Double.isInfinite(number)) {
            return this.escribirEspecial(this.symbols.getInfinity(), number < 0.0, toAppendTo, pos, null);
        }
        // new BigDecimal(double), NOT valueOf: rounding has to see the double's EXACT binary value.
        return this.escribir(new BigDecimal(number), toAppendTo, pos, null);
    }

    // A long is exact all the way through — no double ever appears, so values past 2^53 keep every
    // digit. That is the whole reason NumberFormat declares a separate long seam.
    public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
        return this.escribir(BigDecimal.valueOf(number), toAppendTo, pos, null);
    }

    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        MarcasDeCampo marcas = new MarcasDeCampo();
        StringBuffer sb = new StringBuffer();
        if (obj instanceof BigDecimal) {
            this.escribir((BigDecimal) obj, sb, null, marcas);
        } else if (obj instanceof BigInteger) {
            this.escribir(new BigDecimal((BigInteger) obj), sb, null, marcas);
        } else if (obj instanceof Long || obj instanceof Integer
                || obj instanceof Short || obj instanceof Byte) {
            this.escribir(BigDecimal.valueOf(((Number) obj).longValue()), sb, null, marcas);
        } else if (obj instanceof Number) {
            double d = ((Number) obj).doubleValue();
            if (Double.isNaN(d)) {
                this.escribirEspecial(this.symbols.getNaN(), false, sb, null, marcas);
            } else if (Double.isInfinite(d)) {
                this.escribirEspecial(this.symbols.getInfinity(), d < 0.0, sb, null, marcas);
            } else {
                this.escribir(new BigDecimal(d), sb, null, marcas);
            }
        } else {
            throw new IllegalArgumentException("Cannot format given Object as a Number");
        }
        return marcas.iterador(sb.toString());
    }

    private StringBuffer escribirEspecial(String texto, boolean negativo, StringBuffer out,
                                          FieldPosition pos, MarcasDeCampo marcas) {
        int base = out.length();
        MarcasDeCampo m = marcas;
        if (m == null) {
            m = new MarcasDeCampo();
        }
        StringBuilder sb = new StringBuilder();
        this.ponerAfijo(sb, negativo, true, m, base);
        int d = sb.length();
        sb.append(texto);
        m.marcar((AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.INTEGER, NumberFormat.INTEGER_FIELD, base + d, base + sb.length());
        this.ponerAfijo(sb, negativo, false, m, base);
        out.append(sb.toString());
        m.aplicar(pos);
        return out;
    }

    private StringBuffer escribir(BigDecimal valor, StringBuffer out, FieldPosition pos,
                                  MarcasDeCampo marcas) {
        int base = out.length();
        MarcasDeCampo m = marcas;
        if (m == null) {
            m = new MarcasDeCampo();
        }
        BigDecimal escalado = valor;
        if (this.multiplier != 1) {
            escalado = escalado.multiply(BigDecimal.valueOf((long) this.multiplier));
        }
        boolean negativo = escalado.signum() < 0;
        BigDecimal redondeado = escalado.abs().setScale(this.getMaximumFractionDigits(),
                this.roundingMode);
        // A escala maxFrac, toPlainString() es exactamente "<enteros>.<maxFrac dígitos>" — o sólo
        // los enteros cuando maxFrac es 0. Partir por el punto ES la separación de dígitos.
        String plano = redondeado.toPlainString();
        int punto = -1;
        for (int i = 0; i < plano.length(); i = i + 1) {
            if (plano.charAt(i) == '.') {
                punto = i;
                i = plano.length();
            }
        }
        String enteros = plano;
        String fraccion = "";
        if (punto >= 0) {
            enteros = plano.substring(0, punto);
            fraccion = plano.substring(punto + 1, plano.length());
        }

        StringBuilder sb = new StringBuilder();
        this.ponerAfijo(sb, negativo, true, m, base);

        String ent = enteros;
        while (ent.length() < this.getMinimumIntegerDigits()) {
            ent = "0" + ent;
        }
        // Un máximo de dígitos enteros por debajo del número recorta por la IZQUIERDA: el JDK
        // conserva los dígitos menos significativos, que son los que el patrón pidió mostrar.
        if (ent.length() > this.getMaximumIntegerDigits()) {
            ent = ent.substring(ent.length() - this.getMaximumIntegerDigits(), ent.length());
        }

        int inicioEntero = sb.length();
        boolean agrupa = this.isGroupingUsed() && this.groupingSize > 0;
        for (int i = 0; i < ent.length(); i = i + 1) {
            if (i > 0 && agrupa && (ent.length() - i) % this.groupingSize == 0) {
                int d = sb.length();
                sb.append(this.separadorAgrupacion());
                m.marcar((AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.GROUPING_SEPARATOR, -1, base + d, base + sb.length());
            }
            sb.append(this.digito(ent.charAt(i)));
        }
        // El campo entero abarca TAMBIÉN los separadores de miles, y por eso se marca al final
        // sobre el rango completo: es lo que informa el JDK, y es lo que un llamador que quiere
        // resaltar "la parte entera" necesita.
        m.marcar((AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.INTEGER, NumberFormat.INTEGER_FIELD,
                base + inicioEntero, base + sb.length());

        String frac = "";
        if (this.getMaximumFractionDigits() > 0) {
            frac = fraccion;
            while (frac.length() < this.getMaximumFractionDigits()) {
                frac = frac + "0";
            }
            int fin = frac.length();
            while (fin > this.getMinimumFractionDigits() && frac.charAt(fin - 1) == '0') {
                fin = fin - 1;
            }
            frac = frac.substring(0, fin);
        }
        if (frac.length() > 0 || this.decimalSeparatorAlwaysShown) {
            int d = sb.length();
            sb.append(this.separadorDecimal());
            m.marcar((AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.DECIMAL_SEPARATOR, -1, base + d, base + sb.length());
        }
        if (frac.length() > 0) {
            int d = sb.length();
            for (int i = 0; i < frac.length(); i = i + 1) {
                sb.append(this.digito(frac.charAt(i)));
            }
            m.marcar((AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.FRACTION, NumberFormat.FRACTION_FIELD,
                    base + d, base + sb.length());
        }

        this.ponerAfijo(sb, negativo, false, m, base);
        out.append(sb.toString());
        m.aplicar(pos);
        return out;
    }

    // Los dígitos del locale no tienen por qué empezar en '0' (árabe-índico, devanagari): la
    // conversión es por desplazamiento desde el cero del locale.
    private char digito(char ascii) {
        return (char) (this.symbols.getZeroDigit() + (ascii - '0'));
    }

    private char separadorDecimal() {
        if (this.esDeMoneda()) {
            return this.symbols.getMonetaryDecimalSeparator();
        }
        return this.symbols.getDecimalSeparator();
    }

    private char separadorAgrupacion() {
        if (this.esDeMoneda()) {
            return this.symbols.getMonetaryGroupingSeparator();
        }
        return this.symbols.getGroupingSeparator();
    }

    // Los separadores monetarios se usan sólo si el patrón habla de moneda; un patrón puesto a mano
    // sin '\u00a4' no los quiere aunque el locale los tenga distintos.
    private boolean esDeMoneda() {
        return this.tiene(this.posPrefijoPat, DecimalFormat.PAT_MONEDA)
                || this.tiene(this.posSufijoPat, DecimalFormat.PAT_MONEDA);
    }

    private void ponerAfijo(StringBuilder sb, boolean negativo, boolean prefijo,
                            MarcasDeCampo m, int base) {
        String patron;
        String literal;
        if (negativo) {
            patron = prefijo ? this.negPrefijoPat : this.negSufijoPat;
            literal = prefijo ? this.negPrefijo : this.negSufijo;
        } else {
            patron = prefijo ? this.posPrefijoPat : this.posSufijoPat;
            literal = prefijo ? this.posPrefijo : this.posSufijo;
        }
        if (patron != null) {
            sb.append(this.expandir(patron, m, base + sb.length()));
        } else if (literal != null) {
            sb.append(literal);
        }
    }

    // ---- parseo ----

    /**
     * Lee un número escrito con este patrón.
     *
     * <p>Devuelve {@code Long} cuando el valor es entero y entra en 64 bits, y {@code Double} si no
     * — o {@code BigDecimal} si se pidió con {@link #setParseBigDecimal}. La distinción no es
     * cosmética: devolver siempre {@code Double} perdería dígitos de un entero grande, que es
     * exactamente lo que el formateo se cuidó de no hacer.
     *
     * <p>En modo estricto se rechaza lo que el modo tolerante acepta: separadores de miles mal
     * ubicados, un sufijo que falta, más de un separador decimal. El modo tolerante, como el del
     * JDK, para en el primer carácter que no entiende y devuelve lo leído hasta ahí.
     */
    public Number parse(String text, ParsePosition pos) {
        if (text == null) {
            throw new NullPointerException();
        }
        int i = pos.getIndex();
        int n = text.length();
        if (i < 0 || i > n) {
            pos.setErrorIndex(i);
            return null;
        }

        boolean negativo = false;
        int tras = -1;
        // Se prueban los dos prefijos y gana el más largo: con "#;(#)" el prefijo positivo es vacío
        // y siempre "coincide", así que quedarse con el primero nunca vería el negativo.
        int posPos = this.coincide(text, i, this.posPrefijo);
        int posNeg = this.coincide(text, i, this.negPrefijo);
        if (posNeg > posPos) {
            negativo = true;
            tras = posNeg;
        } else if (posPos >= 0) {
            tras = posPos;
        } else if (posNeg >= 0) {
            negativo = true;
            tras = posNeg;
        }
        if (tras < 0) {
            pos.setErrorIndex(i);
            return null;
        }

        StringBuilder digitos = new StringBuilder();
        int escala = 0;
        boolean vistoPunto = false;
        boolean hayDigito = false;
        int desdeUltimoGrupo = -1;
        int gruposVistos = 0;
        int primerGrupo = -1;
        boolean grupoInvalido = false;
        char cero = this.symbols.getZeroDigit();
        char sepDec = this.separadorDecimal();
        char sepGrupo = this.separadorAgrupacion();

        int j = tras;
        while (j < n) {
            char c = text.charAt(j);
            int v = c - cero;
            if (v >= 0 && v <= 9) {
                digitos.append((char) ('0' + v));
                hayDigito = true;
                if (vistoPunto) {
                    escala = escala + 1;
                } else if (desdeUltimoGrupo >= 0) {
                    desdeUltimoGrupo = desdeUltimoGrupo + 1;
                }
                j = j + 1;
            } else if (c == sepDec && !vistoPunto && !this.isParseIntegerOnly()) {
                // El último grupo antes del punto tiene que estar completo; si no, la agrupación
                // era decorativa y en modo estricto eso es un error.
                if (desdeUltimoGrupo >= 0 && desdeUltimoGrupo != this.groupingSize) {
                    grupoInvalido = true;
                }
                vistoPunto = true;
                j = j + 1;
            } else if (c == sepGrupo && !vistoPunto) {
                if (!hayDigito) {
                    grupoInvalido = true;
                }
                if (gruposVistos == 0) {
                    primerGrupo = digitos.length();
                } else if (desdeUltimoGrupo != this.groupingSize) {
                    grupoInvalido = true;
                }
                gruposVistos = gruposVistos + 1;
                desdeUltimoGrupo = 0;
                j = j + 1;
            } else {
                break;
            }
        }

        if (!hayDigito) {
            pos.setErrorIndex(j);
            return null;
        }
        if (gruposVistos > 0) {
            if (!this.isGroupingUsed() || this.groupingSize <= 0) {
                grupoInvalido = true;
            } else {
                if (primerGrupo < 1 || primerGrupo > this.groupingSize) {
                    grupoInvalido = true;
                }
                if (!vistoPunto && desdeUltimoGrupo != this.groupingSize) {
                    grupoInvalido = true;
                }
            }
        }

        String sufijo = negativo ? this.negSufijo : this.posSufijo;
        int trasSufijo = this.coincide(text, j, sufijo);
        if (this.strict) {
            if (grupoInvalido || trasSufijo < 0) {
                pos.setErrorIndex(j);
                return null;
            }
            j = trasSufijo;
        } else if (trasSufijo >= 0) {
            j = trasSufijo;
        }

        BigDecimal valor = new BigDecimal(new BigInteger(digitos.toString()), escala);
        if (negativo) {
            valor = valor.negate();
        }
        if (this.multiplier != 1) {
            // La división se hace con escala suficiente para no perder dígitos del cociente: un
            // 123450% dividido por 100 es 1234.5 exacto, y redondearlo acá sería inventar.
            valor = valor.divide(BigDecimal.valueOf((long) this.multiplier), escala + 4,
                    RoundingMode.HALF_EVEN).stripTrailingZeros();
        }
        pos.setIndex(j);
        return this.envolver(valor);
    }

    private Number envolver(BigDecimal valor) {
        if (this.parseBigDecimal) {
            return valor;
        }
        BigDecimal limpio = valor.stripTrailingZeros();
        if (limpio.scale() <= 0) {
            try {
                return Long.valueOf(limpio.longValueExact());
            } catch (ArithmeticException e) {
                return Double.valueOf(valor.doubleValue());
            }
        }
        return Double.valueOf(valor.doubleValue());
    }

    // -1 si no coincide; si coincide, el índice justo después. Un afijo vacío coincide siempre y
    // devuelve el mismo índice, que es lo que hace que un patrón sin prefijo funcione.
    private int coincide(String text, int desde, String afijo) {
        if (afijo == null || afijo.length() == 0) {
            return desde;
        }
        if (desde + afijo.length() > text.length()) {
            return -1;
        }
        for (int k = 0; k < afijo.length(); k = k + 1) {
            if (text.charAt(desde + k) != afijo.charAt(k)) {
                return -1;
            }
        }
        return desde + afijo.length();
    }

    // ---- estado ----

    public String getPositivePrefix() {
        return this.posPrefijo;
    }

    public void setPositivePrefix(String newValue) {
        this.posPrefijo = newValue;
        this.posPrefijoPat = null;
    }

    public String getPositiveSuffix() {
        return this.posSufijo;
    }

    public void setPositiveSuffix(String newValue) {
        this.posSufijo = newValue;
        this.posSufijoPat = null;
    }

    public String getNegativePrefix() {
        return this.negPrefijo;
    }

    public void setNegativePrefix(String newValue) {
        this.negPrefijo = newValue;
        this.negPrefijoPat = null;
    }

    public String getNegativeSuffix() {
        return this.negSufijo;
    }

    public void setNegativeSuffix(String newValue) {
        this.negSufijo = newValue;
        this.negSufijoPat = null;
    }

    public int getMultiplier() {
        return this.multiplier;
    }

    public void setMultiplier(int newValue) {
        this.multiplier = newValue;
    }

    public int getGroupingSize() {
        return this.groupingSize;
    }

    public void setGroupingSize(int newValue) {
        this.groupingSize = newValue;
    }

    public boolean isDecimalSeparatorAlwaysShown() {
        return this.decimalSeparatorAlwaysShown;
    }

    public void setDecimalSeparatorAlwaysShown(boolean newValue) {
        this.decimalSeparatorAlwaysShown = newValue;
    }

    public boolean isParseBigDecimal() {
        return this.parseBigDecimal;
    }

    public void setParseBigDecimal(boolean newValue) {
        this.parseBigDecimal = newValue;
    }

    public boolean isStrict() {
        return this.strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
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

    // Se redefine porque MessageFormat.toPattern() distingue los subformatos comparandolos con los
    // que devuelven las fabricas del locale: con el equals heredado —que solo mira las cuentas de
    // digitos— un "#,##0.00" puesto a mano se haria pasar por el getNumberInstance del locale.
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        DecimalFormat other = (DecimalFormat) obj;
        return this.toPattern().equals(other.toPattern())
                && this.multiplier == other.multiplier
                && this.groupingSize == other.groupingSize
                && this.decimalSeparatorAlwaysShown == other.decimalSeparatorAlwaysShown
                && this.parseBigDecimal == other.parseBigDecimal
                && this.strict == other.strict
                && this.roundingMode == other.roundingMode
                && this.symbols.equals(other.symbols);
    }

    public int hashCode() {
        return super.hashCode() * 37 + this.toPattern().hashCode();
    }

    public Currency getCurrency() {
        return this.symbols.getCurrency();
    }

    /**
     * Cambia la moneda, y con ella el símbolo que el {@code ¤} del patrón resuelve.
     *
     * <p>NO toca la cantidad de decimales: el patrón ya la fijó, y el yen o el dinar tienen la suya
     * propia. Cambiarla en silencio haría que un patrón explícito dejara de valer.
     */
    public void setCurrency(Currency currency) {
        if (currency == null) {
            throw new NullPointerException();
        }
        this.symbols.setCurrency(currency);
        this.expandirAfijos();
    }
}
