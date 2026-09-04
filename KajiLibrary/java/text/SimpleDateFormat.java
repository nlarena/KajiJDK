package java.text;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * El formateador de fechas por patrón: {@code "dd/MM/yyyy HH:mm"} y lo que se lea de ahí.
 *
 * <p>Misma separación que en {@link DecimalFormat}, un piso más arriba: el PATRÓN dice qué campos
 * salen y en qué orden, y {@link DateFormatSymbols} dice cómo se llaman. {@code MMMM} significa "el
 * mes, entero"; que eso imprima {@code enero} o {@code January} lo decide el otro lado.
 *
 * <p><b>La cantidad de letras no es decoración.</b> Es el argumento del campo: {@code M} da
 * {@code 1}, {@code MM} da {@code 01}, {@code MMM} da {@code ene} y {@code MMMM} da {@code enero}.
 * Para los campos numéricos el conteo es el ancho mínimo; para los de texto, el umbral entre la
 * forma corta y la larga (cuatro o más). Y {@code yy} es el caso especial de todos: significa
 * "dos dígitos", no "ancho dos", y al parsear se interpreta contra la ventana de cien años que fija
 * {@link #set2DigitYearStart}.
 *
 * <p>Al parsear, los campos se van cargando en el {@link Calendar} y es él quien calcula el
 * instante. Esa división de trabajo es la del JDK, y trae una consecuencia que conviene saber:
 * {@code setLenient(false)} no rechaza nada por sí mismo, sólo se lo pide al calendario. Y el
 * {@code java.util.GregorianCalendar} de esta biblioteca <b>no valida los campos en modo
 * estricto</b> —un 32 de enero desborda al 1 de febrero igual que en modo tolerante—, así que hoy
 * las dos modalidades dan lo mismo. El agujero está en {@code java.util}, no acá: cuando el
 * calendario valide, esto empieza a rechazar sin tocar una línea.
 *
 * @implNote Subconjunto declarado: las letras de patrón implementadas son
 *           {@code G y Y M d E u a H k K h m s S D F w W z Z X}. Las que faltan
 *           —{@code L} (mes suelto), {@code c} (día suelto), {@code B} (franja del día)— necesitan
 *           formas "standalone" y nombres de franja horaria que {@code DateFormatSymbols} no tiene
 *           y que son datos del CLDR; en vez de imprimir la forma de contexto haciéndola pasar por
 *           la suelta, el patrón las RECHAZA con {@code IllegalArgumentException}.
 */
public class SimpleDateFormat extends DateFormat {

    // El orden de estas letras ES la codificación: el índice de una letra acá es el "número de
    // campo" con el que se la nombra en un patrón localizado. Sale de DateFormatSymbols.
    private static final String LETRAS = "GyMdkHmsSEDFwWahKzZ";

    private String pattern;
    private DateFormatSymbols formatData;
    private Locale locale;
    private Date defaultCenturyStart;

    public SimpleDateFormat() {
        this(PatronesLocales.fechaHora(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault()),
                Locale.getDefault());
    }

    public SimpleDateFormat(String pattern) {
        this(pattern, Locale.getDefault());
    }

    public SimpleDateFormat(String pattern, Locale locale) {
        if (pattern == null || locale == null) {
            throw new NullPointerException();
        }
        this.locale = locale;
        this.formatData = new DateFormatSymbols(locale);
        this.inicializar(pattern);
    }

    public SimpleDateFormat(String pattern, DateFormatSymbols formatSymbols) {
        if (pattern == null || formatSymbols == null) {
            throw new NullPointerException();
        }
        this.locale = Locale.getDefault();
        this.formatData = (DateFormatSymbols) formatSymbols.clone();
        this.inicializar(pattern);
    }

    private void inicializar(String pattern) {
        this.calendar = Calendar.getInstance(TimeZone.getDefault(), this.locale);
        this.numberFormat = NumberFormat.getNumberInstance(this.locale);
        this.numberFormat.setGroupingUsed(false);
        this.numberFormat.setParseIntegerOnly(true);
        this.applyPattern(pattern);
        this.defaultCenturyStart = this.sigloPorDefecto();
    }

    // La ventana de dos dígitos arranca ochenta años atrás: es lo que hace el JDK, y la asimetría
    // (80 atrás, 20 adelante) es deliberada — las fechas de dos dígitos suelen ser pasadas.
    private Date sigloPorDefecto() {
        Calendar c = Calendar.getInstance(this.calendar.getTimeZone(), this.locale);
        c.setTime(new Date());
        c.set(Calendar.YEAR, c.get(Calendar.YEAR) - 80);
        return c.getTime();
    }

    public void set2DigitYearStart(Date startDate) {
        if (startDate == null) {
            throw new NullPointerException();
        }
        this.defaultCenturyStart = new Date(startDate.getTime());
    }

    public Date get2DigitYearStart() {
        return new Date(this.defaultCenturyStart.getTime());
    }

    public String toPattern() {
        return this.pattern;
    }

    /**
     * El patrón escrito con las letras del locale.
     *
     * <p>En los seis locales de la biblioteca las letras locales coinciden con las estándar, así
     * que hoy devuelve lo mismo que {@link #toPattern()}. Se implementa igual porque la traducción
     * es real —recorre la tabla de {@link DateFormatSymbols#getLocalPatternChars()}— y empieza a
     * dar distinto en cuanto se agregue una fila con otras letras.
     */
    public String toLocalizedPattern() {
        return this.traducir(this.pattern, SimpleDateFormat.LETRAS,
                this.formatData.getLocalPatternChars());
    }

    public void applyPattern(String pattern) {
        if (pattern == null) {
            throw new NullPointerException();
        }
        this.verificarPatron(pattern);
        this.pattern = pattern;
    }

    public void applyLocalizedPattern(String pattern) {
        if (pattern == null) {
            throw new NullPointerException();
        }
        this.applyPattern(this.traducir(pattern, this.formatData.getLocalPatternChars(),
                SimpleDateFormat.LETRAS));
    }

    private String traducir(String pat, String desde, String hacia) {
        StringBuilder sb = new StringBuilder();
        boolean citado = false;
        for (int i = 0; i < pat.length(); i = i + 1) {
            char c = pat.charAt(i);
            if (c == '\'') {
                citado = !citado;
                sb.append(c);
                continue;
            }
            if (citado) {
                sb.append(c);
                continue;
            }
            int k = desde.indexOf(c);
            if (k >= 0 && k < hacia.length()) {
                sb.append(hacia.charAt(k));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // Rechaza en la aplicación del patrón, no al formatear: un patrón inválido tiene que fallar
    // cuando se escribe, no la primera vez que alguien formatea con él en producción.
    private void verificarPatron(String pat) {
        boolean citado = false;
        for (int i = 0; i < pat.length(); i = i + 1) {
            char c = pat.charAt(i);
            if (c == '\'') {
                citado = !citado;
            } else if (!citado && SimpleDateFormat.esLetra(c)) {
                if (!SimpleDateFormat.soportada(c)) {
                    throw new IllegalArgumentException("Illegal pattern character '" + c + "'");
                }
            }
        }
        if (citado) {
            throw new IllegalArgumentException("Unterminated quote");
        }
    }

    private static boolean esLetra(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean soportada(char c) {
        return "GyYMdEuaHkKhmsSDFwWzZX".indexOf(c) >= 0;
    }

    public DateFormatSymbols getDateFormatSymbols() {
        return (DateFormatSymbols) this.formatData.clone();
    }

    public void setDateFormatSymbols(DateFormatSymbols newFormatSymbols) {
        if (newFormatSymbols == null) {
            throw new NullPointerException();
        }
        this.formatData = (DateFormatSymbols) newFormatSymbols.clone();
    }

    // ---- formateo ----

    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos) {
        this.escribir(date, toAppendTo, pos, null);
        return toAppendTo;
    }

    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        Date d;
        if (obj instanceof Date) {
            d = (Date) obj;
        } else if (obj instanceof Number) {
            d = new Date(((Number) obj).longValue());
        } else {
            throw new IllegalArgumentException("Cannot format given Object as a Date");
        }
        MarcasDeCampo marcas = new MarcasDeCampo();
        StringBuffer sb = new StringBuffer();
        this.escribir(d, sb, null, marcas);
        return marcas.iterador(sb.toString());
    }

    private void escribir(Date date, StringBuffer out, FieldPosition pos, MarcasDeCampo marcas) {
        if (date == null) {
            throw new NullPointerException();
        }
        MarcasDeCampo m = marcas;
        if (m == null) {
            m = new MarcasDeCampo();
        }
        this.calendar.setTime(date);
        int base = out.length();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int n = this.pattern.length();
        while (i < n) {
            char c = this.pattern.charAt(i);
            if (c == '\'') {
                i = i + 1;
                if (i < n && this.pattern.charAt(i) == '\'') {
                    sb.append('\'');
                    i = i + 1;
                    continue;
                }
                while (i < n && this.pattern.charAt(i) != '\'') {
                    sb.append(this.pattern.charAt(i));
                    i = i + 1;
                }
                i = i + 1;
                continue;
            }
            if (!SimpleDateFormat.esLetra(c)) {
                sb.append(c);
                i = i + 1;
                continue;
            }
            int cuenta = 0;
            while (i < n && this.pattern.charAt(i) == c) {
                cuenta = cuenta + 1;
                i = i + 1;
            }
            int d = sb.length();
            this.escribirCampo(c, cuenta, sb);
            m.marcar(SimpleDateFormat.campoDe(c), SimpleDateFormat.numeroDe(c),
                    base + d, base + sb.length());
        }
        out.append(sb.toString());
        m.aplicar(pos);
    }

    private void escribirCampo(char c, int cuenta, StringBuilder sb) {
        Calendar cal = this.calendar;
        if (c == 'G') {
            sb.append(this.formatData.getEras()[cal.get(Calendar.ERA)]);
        } else if (c == 'y' || c == 'Y') {
            int anio = cal.get(Calendar.YEAR);
            if (c == 'Y') {
                anio = cal.getWeekYear();
            }
            // `yy` NO es "ancho dos": es "los dos últimos dígitos". Tratarlo como ancho daría
            // 2026 en vez de 26, que es justo lo que el patrón corto pide evitar.
            if (cuenta == 2) {
                this.numero(anio % 100, 2, sb);
            } else {
                this.numero(anio, cuenta, sb);
            }
        } else if (c == 'M') {
            int mes = cal.get(Calendar.MONTH);
            if (cuenta >= 4) {
                sb.append(this.formatData.getMonths()[mes]);
            } else if (cuenta == 3) {
                sb.append(this.formatData.getShortMonths()[mes]);
            } else {
                this.numero(mes + 1, cuenta, sb);
            }
        } else if (c == 'E') {
            int dia = cal.get(Calendar.DAY_OF_WEEK);
            if (cuenta >= 4) {
                sb.append(this.formatData.getWeekdays()[dia]);
            } else {
                sb.append(this.formatData.getShortWeekdays()[dia]);
            }
        } else if (c == 'a') {
            sb.append(this.formatData.getAmPmStrings()[cal.get(Calendar.AM_PM)]);
        } else if (c == 'd') {
            this.numero(cal.get(Calendar.DAY_OF_MONTH), cuenta, sb);
        } else if (c == 'H') {
            this.numero(cal.get(Calendar.HOUR_OF_DAY), cuenta, sb);
        } else if (c == 'k') {
            // k es 1..24: la medianoche se escribe 24, no 0. La conversión vive acá y no en el
            // calendario porque es una convención de presentación, no de tiempo.
            int h = cal.get(Calendar.HOUR_OF_DAY);
            if (h == 0) {
                h = 24;
            }
            this.numero(h, cuenta, sb);
        } else if (c == 'K') {
            this.numero(cal.get(Calendar.HOUR), cuenta, sb);
        } else if (c == 'h') {
            int h = cal.get(Calendar.HOUR);
            if (h == 0) {
                h = 12;
            }
            this.numero(h, cuenta, sb);
        } else if (c == 'm') {
            this.numero(cal.get(Calendar.MINUTE), cuenta, sb);
        } else if (c == 's') {
            this.numero(cal.get(Calendar.SECOND), cuenta, sb);
        } else if (c == 'S') {
            this.numero(cal.get(Calendar.MILLISECOND), cuenta, sb);
        } else if (c == 'D') {
            this.numero(cal.get(Calendar.DAY_OF_YEAR), cuenta, sb);
        } else if (c == 'F') {
            this.numero(cal.get(Calendar.DAY_OF_WEEK_IN_MONTH), cuenta, sb);
        } else if (c == 'w') {
            this.numero(cal.get(Calendar.WEEK_OF_YEAR), cuenta, sb);
        } else if (c == 'W') {
            this.numero(cal.get(Calendar.WEEK_OF_MONTH), cuenta, sb);
        } else if (c == 'u') {
            // u numera de lunes(1) a domingo(7); Calendar numera de domingo(1) a sábado(7).
            int dia = cal.get(Calendar.DAY_OF_WEEK) - 1;
            if (dia == 0) {
                dia = 7;
            }
            this.numero(dia, cuenta, sb);
        } else if (c == 'z') {
            boolean verano = this.calendar.get(Calendar.DST_OFFSET) != 0;
            boolean largo = cuenta >= 4;
            // Los nombres puestos con setZoneStrings mandan sobre los de la zona: si no, ese setter
            // no cambiaría nada de lo que se ve y sería un miembro decorativo.
            String[] fila = this.formatData.filaDeZona(cal.getTimeZone().getID());
            if (fila != null) {
                int col = 2;
                if (largo) {
                    col = 1;
                }
                if (verano) {
                    col = col + 2;
                }
                sb.append(fila[col]);
                return;
            }
            int estilo = TimeZone.SHORT;
            if (largo) {
                estilo = TimeZone.LONG;
            }
            sb.append(cal.getTimeZone().getDisplayName(verano, estilo, this.locale));
        } else if (c == 'Z') {
            sb.append(this.desfase(false));
        } else if (c == 'X') {
            sb.append(this.desfase(true));
        } else {
            throw new IllegalArgumentException("Illegal pattern character '" + c + "'");
        }
    }

    // El desplazamiento total incluye el horario de verano: sumar sólo el desfase crudo daría una
    // hora de menos medio año, que es el error clásico de este campo.
    private String desfase(boolean conDosPuntos) {
        int ms = this.calendar.get(Calendar.ZONE_OFFSET) + this.calendar.get(Calendar.DST_OFFSET);
        String signo = "+";
        int abs = ms;
        if (ms < 0) {
            signo = "-";
            abs = -ms;
        }
        int minutos = abs / 60000;
        int hh = minutos / 60;
        int mm = minutos % 60;
        StringBuilder sb = new StringBuilder();
        sb.append(signo);
        this.numeroPlano(hh, 2, sb);
        if (conDosPuntos) {
            sb.append(':');
        }
        this.numeroPlano(mm, 2, sb);
        return sb.toString();
    }

    // Los dígitos salen del numberFormat porque el locale puede no usar los arábigos occidentales;
    // el ancho mínimo se fija en él y se restaura, para no dejarlo tocado entre campos.
    private void numero(int valor, int ancho, StringBuilder sb) {
        int previo = this.numberFormat.getMinimumIntegerDigits();
        this.numberFormat.setMinimumIntegerDigits(ancho);
        sb.append(this.numberFormat.format((long) valor));
        this.numberFormat.setMinimumIntegerDigits(previo);
    }

    private void numeroPlano(int valor, int ancho, StringBuilder sb) {
        String s = Integer.toString(valor);
        while (s.length() < ancho) {
            s = "0" + s;
        }
        sb.append(s);
    }

    private static java.text.DateFormat.Field campoDe(char c) {
        if (c == 'G') {
            return java.text.DateFormat.Field.ERA;
        }
        if (c == 'y' || c == 'Y') {
            return java.text.DateFormat.Field.YEAR;
        }
        if (c == 'M') {
            return java.text.DateFormat.Field.MONTH;
        }
        if (c == 'd') {
            return java.text.DateFormat.Field.DAY_OF_MONTH;
        }
        if (c == 'k') {
            return java.text.DateFormat.Field.HOUR_OF_DAY1;
        }
        if (c == 'H') {
            return java.text.DateFormat.Field.HOUR_OF_DAY0;
        }
        if (c == 'm') {
            return java.text.DateFormat.Field.MINUTE;
        }
        if (c == 's') {
            return java.text.DateFormat.Field.SECOND;
        }
        if (c == 'S') {
            return java.text.DateFormat.Field.MILLISECOND;
        }
        if (c == 'E' || c == 'u') {
            return java.text.DateFormat.Field.DAY_OF_WEEK;
        }
        if (c == 'D') {
            return java.text.DateFormat.Field.DAY_OF_YEAR;
        }
        if (c == 'F') {
            return java.text.DateFormat.Field.DAY_OF_WEEK_IN_MONTH;
        }
        if (c == 'w') {
            return java.text.DateFormat.Field.WEEK_OF_YEAR;
        }
        if (c == 'W') {
            return java.text.DateFormat.Field.WEEK_OF_MONTH;
        }
        if (c == 'a') {
            return java.text.DateFormat.Field.AM_PM;
        }
        if (c == 'h') {
            return java.text.DateFormat.Field.HOUR1;
        }
        if (c == 'K') {
            return java.text.DateFormat.Field.HOUR0;
        }
        return java.text.DateFormat.Field.TIME_ZONE;
    }

    private static int numeroDe(char c) {
        if (c == 'G') {
            return DateFormat.ERA_FIELD;
        }
        if (c == 'y' || c == 'Y') {
            return DateFormat.YEAR_FIELD;
        }
        if (c == 'M') {
            return DateFormat.MONTH_FIELD;
        }
        if (c == 'd') {
            return DateFormat.DATE_FIELD;
        }
        if (c == 'k') {
            return DateFormat.HOUR_OF_DAY1_FIELD;
        }
        if (c == 'H') {
            return DateFormat.HOUR_OF_DAY0_FIELD;
        }
        if (c == 'm') {
            return DateFormat.MINUTE_FIELD;
        }
        if (c == 's') {
            return DateFormat.SECOND_FIELD;
        }
        if (c == 'S') {
            return DateFormat.MILLISECOND_FIELD;
        }
        if (c == 'E' || c == 'u') {
            return DateFormat.DAY_OF_WEEK_FIELD;
        }
        if (c == 'D') {
            return DateFormat.DAY_OF_YEAR_FIELD;
        }
        if (c == 'F') {
            return DateFormat.DAY_OF_WEEK_IN_MONTH_FIELD;
        }
        if (c == 'w') {
            return DateFormat.WEEK_OF_YEAR_FIELD;
        }
        if (c == 'W') {
            return DateFormat.WEEK_OF_MONTH_FIELD;
        }
        if (c == 'a') {
            return DateFormat.AM_PM_FIELD;
        }
        if (c == 'h') {
            return DateFormat.HOUR1_FIELD;
        }
        if (c == 'K') {
            return DateFormat.HOUR0_FIELD;
        }
        return DateFormat.TIMEZONE_FIELD;
    }

    // ---- parseo ----

    /**
     * Lee una fecha escrita con este patrón.
     *
     * <p>Los campos no se combinan acá: se cargan en el {@link Calendar} y él calcula el instante.
     * Por eso {@code isLenient()} manda de verdad —un 32 de enero es un error o el 1 de febrero
     * según cómo esté el calendario— y por eso un patrón sin año da el año actual y no el año cero.
     */
    public Date parse(String text, ParsePosition pos) {
        if (text == null || pos == null) {
            throw new NullPointerException();
        }
        int inicio = pos.getIndex();
        int t = inicio;
        this.calendar.clear();
        int i = 0;
        int n = this.pattern.length();
        while (i < n) {
            char c = this.pattern.charAt(i);
            if (c == '\'') {
                i = i + 1;
                if (i < n && this.pattern.charAt(i) == '\'') {
                    if (t >= text.length() || text.charAt(t) != '\'') {
                        pos.setErrorIndex(t);
                        return null;
                    }
                    t = t + 1;
                    i = i + 1;
                    continue;
                }
                while (i < n && this.pattern.charAt(i) != '\'') {
                    if (t >= text.length() || text.charAt(t) != this.pattern.charAt(i)) {
                        pos.setErrorIndex(t);
                        return null;
                    }
                    t = t + 1;
                    i = i + 1;
                }
                i = i + 1;
                continue;
            }
            if (!SimpleDateFormat.esLetra(c)) {
                if (t >= text.length() || text.charAt(t) != c) {
                    pos.setErrorIndex(t);
                    return null;
                }
                t = t + 1;
                i = i + 1;
                continue;
            }
            int cuenta = 0;
            while (i < n && this.pattern.charAt(i) == c) {
                cuenta = cuenta + 1;
                i = i + 1;
            }
            boolean pegadoANumero = i < n && SimpleDateFormat.esLetra(this.pattern.charAt(i))
                    && SimpleDateFormat.esNumerico(this.pattern.charAt(i), 1);
            int siguiente = this.leerCampo(c, cuenta, text, t, pegadoANumero);
            if (siguiente < 0) {
                pos.setErrorIndex(t);
                return null;
            }
            t = siguiente;
        }
        Date d;
        try {
            d = this.calendar.getTime();
        } catch (IllegalArgumentException e) {
            // Modo estricto: el calendario rechaza un 32 de enero. Se informa como fallo de parseo
            // con el cursor SIN avanzar, que es como el contrato distingue "no pude" de "leí null".
            pos.setErrorIndex(inicio);
            return null;
        }
        pos.setIndex(t);
        return d;
    }

    private static boolean esNumerico(char c, int cuenta) {
        if (c == 'M' || c == 'E') {
            return cuenta < 3;
        }
        return "yYdHkKhmsSDFwWu".indexOf(c) >= 0;
    }

    // Devuelve el índice después del campo, o -1 si no se pudo leer.
    private int leerCampo(char c, int cuenta, String text, int desde, boolean pegadoANumero) {
        if (c == 'G') {
            return this.leerTexto(text, desde, this.formatData.getEras(), Calendar.ERA, 0);
        }
        if (c == 'M' && cuenta >= 3) {
            int r = this.leerTexto(text, desde, this.formatData.getMonths(), Calendar.MONTH, 0);
            if (r < 0) {
                r = this.leerTexto(text, desde, this.formatData.getShortMonths(), Calendar.MONTH, 0);
            }
            return r;
        }
        if (c == 'E') {
            int r = this.leerTexto(text, desde, this.formatData.getWeekdays(), Calendar.DAY_OF_WEEK, 0);
            if (r < 0) {
                r = this.leerTexto(text, desde, this.formatData.getShortWeekdays(),
                        Calendar.DAY_OF_WEEK, 0);
            }
            return r;
        }
        if (c == 'a') {
            return this.leerTexto(text, desde, this.formatData.getAmPmStrings(), Calendar.AM_PM, 0);
        }
        if (c == 'z' || c == 'Z' || c == 'X') {
            return this.leerZona(text, desde);
        }
        // El ancho fijo sólo se impone cuando el campo siguiente también es numérico: si no hay
        // separador entre dos números, la única forma de saber dónde termina el primero es el
        // conteo del patrón. Con separador conviene leer todos los dígitos que haya.
        int max = 10;
        if (pegadoANumero || (c == 'y' && cuenta == 2)) {
            max = cuenta;
        }
        int fin = desde;
        while (fin < text.length() && fin - desde < max && SimpleDateFormat.esDigito(text.charAt(fin))) {
            fin = fin + 1;
        }
        if (fin == desde) {
            return -1;
        }
        int valor = 0;
        for (int k = desde; k < fin; k = k + 1) {
            valor = valor * 10 + (text.charAt(k) - '0');
        }
        this.cargar(c, cuenta, valor, fin - desde);
        return fin;
    }

    private static boolean esDigito(char c) {
        return c >= '0' && c <= '9';
    }

    private void cargar(char c, int cuenta, int valor, int digitos) {
        if (c == 'y') {
            int anio = valor;
            // Dos dígitos escritos son dos dígitos leídos: se ubican en la ventana de cien años
            // que arranca en defaultCenturyStart. Con más dígitos el año es literal, y por eso
            // "0080" y "80" no significan lo mismo — que es exactamente lo que dice el JDK.
            if (cuenta == 2 && digitos == 2) {
                anio = this.enLaVentana(valor);
            }
            this.calendar.set(Calendar.YEAR, anio);
        } else if (c == 'Y') {
            this.calendar.set(Calendar.YEAR, valor);
        } else if (c == 'M') {
            this.calendar.set(Calendar.MONTH, valor - 1);
        } else if (c == 'd') {
            this.calendar.set(Calendar.DAY_OF_MONTH, valor);
        } else if (c == 'H') {
            this.calendar.set(Calendar.HOUR_OF_DAY, valor);
        } else if (c == 'k') {
            int h = valor;
            if (h == 24) {
                h = 0;
            }
            this.calendar.set(Calendar.HOUR_OF_DAY, h);
        } else if (c == 'K') {
            this.calendar.set(Calendar.HOUR, valor);
        } else if (c == 'h') {
            int h = valor;
            if (h == 12) {
                h = 0;
            }
            this.calendar.set(Calendar.HOUR, h);
        } else if (c == 'm') {
            this.calendar.set(Calendar.MINUTE, valor);
        } else if (c == 's') {
            this.calendar.set(Calendar.SECOND, valor);
        } else if (c == 'S') {
            this.calendar.set(Calendar.MILLISECOND, valor);
        } else if (c == 'D') {
            this.calendar.set(Calendar.DAY_OF_YEAR, valor);
        } else if (c == 'F') {
            this.calendar.set(Calendar.DAY_OF_WEEK_IN_MONTH, valor);
        } else if (c == 'w') {
            this.calendar.set(Calendar.WEEK_OF_YEAR, valor);
        } else if (c == 'W') {
            this.calendar.set(Calendar.WEEK_OF_MONTH, valor);
        } else if (c == 'u') {
            int dia = valor + 1;
            if (dia > 7) {
                dia = 1;
            }
            this.calendar.set(Calendar.DAY_OF_WEEK, dia);
        }
    }

    private int enLaVentana(int dosDigitos) {
        Calendar c = Calendar.getInstance(this.calendar.getTimeZone(), this.locale);
        c.setTime(this.defaultCenturyStart);
        int inicio = c.get(Calendar.YEAR);
        int candidato = (inicio / 100) * 100 + dosDigitos;
        if (candidato < inicio) {
            candidato = candidato + 100;
        }
        return candidato;
    }

    // Devuelve el índice tras el nombre más largo que coincida, y carga el campo con su posición.
    // El más largo y no el primero: "sept" y "sep" pueden convivir en la misma tabla, y quedarse
    // con el primero dejaría la "t" suelta para el literal siguiente.
    private int leerTexto(String text, int desde, String[] nombres, int campo, int base) {
        int mejor = -1;
        int mejorLargo = 0;
        for (int i = 0; i < nombres.length; i = i + 1) {
            String nombre = nombres[i];
            if (nombre == null || nombre.length() == 0) {
                continue;
            }
            if (nombre.length() > mejorLargo && this.coincideSinCaso(text, desde, nombre)) {
                mejor = i;
                mejorLargo = nombre.length();
            }
        }
        if (mejor < 0) {
            return -1;
        }
        this.calendar.set(campo, mejor + base);
        return desde + mejorLargo;
    }

    private boolean coincideSinCaso(String text, int desde, String nombre) {
        if (desde + nombre.length() > text.length()) {
            return false;
        }
        for (int i = 0; i < nombre.length(); i = i + 1) {
            char a = text.charAt(desde + i);
            char b = nombre.charAt(i);
            if (a != b && Character.toLowerCase(a) != Character.toLowerCase(b)) {
                return false;
            }
        }
        return true;
    }

    // Acepta "+HH:MM", "+HHMM", "GMT+H:MM" y los nombres que la zona actual sepa dar de sí misma.
    // No busca en la base de zonas por nombre: sin datos del CLDR no hay tabla de "EST -> America/
    // New_York", y adivinarla elegiría mal en cuanto dos zonas compartan abreviatura.
    private int leerZona(String text, int desde) {
        int t = desde;
        if (t + 3 <= text.length() && text.substring(t, t + 3).equals("GMT")) {
            t = t + 3;
            if (t >= text.length() || (text.charAt(t) != '+' && text.charAt(t) != '-')) {
                this.calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
                return t;
            }
        }
        if (t < text.length() && (text.charAt(t) == '+' || text.charAt(t) == '-')) {
            int signo = 1;
            if (text.charAt(t) == '-') {
                signo = -1;
            }
            t = t + 1;
            int hh = 0;
            int leidos = 0;
            while (t < text.length() && leidos < 2 && SimpleDateFormat.esDigito(text.charAt(t))) {
                hh = hh * 10 + (text.charAt(t) - '0');
                t = t + 1;
                leidos = leidos + 1;
            }
            if (leidos == 0) {
                return -1;
            }
            if (t < text.length() && text.charAt(t) == ':') {
                t = t + 1;
            }
            int mm = 0;
            leidos = 0;
            while (t < text.length() && leidos < 2 && SimpleDateFormat.esDigito(text.charAt(t))) {
                mm = mm * 10 + (text.charAt(t) - '0');
                t = t + 1;
                leidos = leidos + 1;
            }
            int total = signo * (hh * 3600000 + mm * 60000);
            this.calendar.set(Calendar.ZONE_OFFSET, total);
            this.calendar.set(Calendar.DST_OFFSET, 0);
            return t;
        }
        // Último recurso: el nombre que la zona del propio formateador declara. Alcanza para leer
        // lo que este mismo formateador escribió, que es el caso de ida y vuelta.
        TimeZone z = this.calendar.getTimeZone();
        String[] candidatos = new String[] {
            z.getID(),
            z.getDisplayName(false, TimeZone.LONG, this.locale),
            z.getDisplayName(true, TimeZone.LONG, this.locale),
            z.getDisplayName(false, TimeZone.SHORT, this.locale),
            z.getDisplayName(true, TimeZone.SHORT, this.locale),
        };
        int mejorLargo = 0;
        for (int i = 0; i < candidatos.length; i = i + 1) {
            String s = candidatos[i];
            if (s != null && s.length() > mejorLargo && this.coincideSinCaso(text, desde, s)) {
                mejorLargo = s.length();
            }
        }
        if (mejorLargo == 0) {
            return -1;
        }
        return desde + mejorLargo;
    }

    // ---- identidad ----

    public Object clone() {
        SimpleDateFormat copia = new SimpleDateFormat(this.pattern, this.locale);
        copia.formatData = (DateFormatSymbols) this.formatData.clone();
        copia.calendar = Calendar.getInstance(this.calendar.getTimeZone(), this.locale);
        copia.calendar.setLenient(this.calendar.isLenient());
        copia.numberFormat = this.numberFormat;
        copia.defaultCenturyStart = new Date(this.defaultCenturyStart.getTime());
        return copia;
    }

    public int hashCode() {
        return this.pattern.hashCode();
    }

    public String toString() {
        return "java.text.SimpleDateFormat[pattern=" + this.pattern + "]";
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        SimpleDateFormat other = (SimpleDateFormat) obj;
        return this.pattern.equals(other.pattern) && this.formatData.equals(other.formatData);
    }
}
