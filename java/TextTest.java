import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.Bidi;
import java.text.CharacterIterator;
import java.text.CompactNumberFormat;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ListFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

// Comportamiento de java.text, para correr con las dos VMs y comparar.
//
// Todo lo que se compara acá es INDEPENDIENTE DEL LOCALE POR OMISION: cada formateador se arma con
// un Locale explicito y cada fecha con una zona explicita. El `java` real de esta maquina corre en
// espanol y en la zona local, asi que cualquier comparacion contra texto que dependa de eso daria
// distinto por el entorno y no por el codigo.
//
// Tampoco se comparan cosas donde las dos bibliotecas honestamente difieren: la cantidad de locales
// disponibles (la nuestra cubre seis), los nombres de zona horaria (nuestra tzdb es minima) ni los
// casos de corchetes del algoritmo bidireccional (la regla N0 no esta, y esta documentado por que).
public class TextTest {

    static List<String> lista(String a, String b, String c) {
        List<String> r = new ArrayList<String>();
        if (a != null) {
            r.add(a);
        }
        if (b != null) {
            r.add(b);
        }
        if (c != null) {
            r.add(c);
        }
        return r;
    }

    public static int run() throws Exception {
        int n = 0;

        // ---- DecimalFormat: patron, simbolos, redondeo ----
        DecimalFormat f = new DecimalFormat("#,##0.###", new DecimalFormatSymbols(Locale.US));
        n = 1;
        if (!f.format(1234.5).equals("1,234.5")) {
            return n;
        }
        n = 2;
        if (!f.toPattern().equals("#,##0.###")) {
            return n;
        }
        n = 3;
        if (f.getMaximumFractionDigits() != 3 || f.getMinimumIntegerDigits() != 1) {
            return n;
        }
        n = 4;
        if (f.getGroupingSize() != 3 || !f.isGroupingUsed()) {
            return n;
        }
        n = 5;
        // El redondeo mira el valor binario EXACTO del double: 2.675 es 2.67499999..., asi que da
        // 2.67 y no 2.68. Es la diferencia entre redondear sobre el double y sobre su forma corta.
        if (!new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US)).format(2.675)
                .equals("2.67")) {
            return n;
        }
        n = 6;
        // Un long grande no puede pasar por double sin perder digitos: por eso hay dos seams.
        if (!f.format(9007199254740993L).equals("9,007,199,254,740,993")) {
            return n;
        }
        n = 7;
        if (!new DecimalFormat("000.0", new DecimalFormatSymbols(Locale.US)).toPattern()
                .equals("#000.0")) {
            return n;
        }
        n = 8;
        if (!new DecimalFormat("#.", new DecimalFormatSymbols(Locale.US)).toPattern()
                .equals("#0.")) {
            return n;
        }
        n = 9;
        DecimalFormat neg = new DecimalFormat("#,##0.00;(#,##0.00)",
                new DecimalFormatSymbols(Locale.US));
        if (!neg.format(-1234.5).equals("(1,234.50)")) {
            return n;
        }
        n = 10;
        if (!neg.toPattern().equals("#,##0.00;(#,##0.00)")) {
            return n;
        }
        n = 11;
        DecimalFormat ale = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.GERMANY));
        if (!ale.format(1234.5).equals("1.234,50")) {
            return n;
        }
        n = 12;
        if (!ale.toLocalizedPattern().equals("#.##0,00")) {
            return n;
        }
        n = 13;
        DecimalFormat ale2 = new DecimalFormat("0", new DecimalFormatSymbols(Locale.GERMANY));
        ale2.applyLocalizedPattern("#.##0,00");
        if (!ale2.toPattern().equals("#,##0.00")) {
            return n;
        }
        n = 14;
        DecimalFormat pct = new DecimalFormat("#,##0%", new DecimalFormatSymbols(Locale.US));
        if (!pct.format(1234.5).equals("123,450%")) {
            return n;
        }
        n = 15;
        DecimalFormat cur = new DecimalFormat("\u00a4#,##0.00", new DecimalFormatSymbols(Locale.US));
        if (!cur.format(-1234.5).equals("-$1,234.50")) {
            return n;
        }
        n = 16;
        if (cur.getCurrency() == null || !cur.getCurrency().getCurrencyCode().equals("USD")) {
            return n;
        }
        n = 17;
        DecimalFormat r5 = new DecimalFormat("0", new DecimalFormatSymbols(Locale.US));
        r5.setRoundingMode(RoundingMode.HALF_UP);
        if (!r5.format(0.5).equals("1")) {
            return n;
        }
        n = 18;
        r5.setRoundingMode(RoundingMode.FLOOR);
        if (!r5.format(0.9).equals("0")) {
            return n;
        }

        // ---- DecimalFormat: parseo, ida y vuelta ----
        n = 19;
        Object v = f.parse("1,234");
        if (!(v instanceof Long) || ((Long) v).longValue() != 1234L) {
            return n;
        }
        n = 20;
        Object v2 = f.parse("1,234.5");
        if (!(v2 instanceof Double) || ((Double) v2).doubleValue() != 1234.5) {
            return n;
        }
        n = 21;
        // Ida y vuelta: no depende del texto, solo de que las dos mitades se entiendan.
        double[] valores = {0.0, 1.0, -1.0, 1234.5, -0.25, 1000000.125};
        for (int i = 0; i < valores.length; i = i + 1) {
            Object w = f.parse(f.format(valores[i]));
            if (((Number) w).doubleValue() != valores[i]) {
                return n;
            }
        }
        n = 22;
        Object pc = pct.parse("123,450%");
        if (((Number) pc).doubleValue() != 1234.5) {
            return n;
        }
        n = 23;
        ParsePosition pp = new ParsePosition(0);
        if (f.parse("nada", pp) != null || pp.getIndex() != 0) {
            return n;
        }
        n = 24;
        ParsePosition pp2 = new ParsePosition(0);
        f.parse("1,234xyz", pp2);
        if (pp2.getIndex() != 5) {
            return n;
        }
        n = 25;
        DecimalFormat bd = new DecimalFormat("#,##0.###", new DecimalFormatSymbols(Locale.US));
        bd.setParseBigDecimal(true);
        Object big = bd.parse("1,234.5");
        if (!(big instanceof BigDecimal)) {
            return n;
        }

        // ---- FieldPosition y atributos ----
        n = 26;
        FieldPosition fp = new FieldPosition(NumberFormat.INTEGER_FIELD);
        StringBuffer sb = new StringBuffer();
        cur.format(Double.valueOf(-1234.5), sb, fp);
        if (fp.getBeginIndex() != 2 || fp.getEndIndex() != 7) {
            return n;
        }
        n = 27;
        FieldPosition fp2 = new FieldPosition(NumberFormat.FRACTION_FIELD);
        cur.format(Double.valueOf(-1234.5), new StringBuffer(), fp2);
        if (fp2.getBeginIndex() != 8 || fp2.getEndIndex() != 10) {
            return n;
        }
        n = 28;
        AttributedCharacterIterator it = cur.formatToCharacterIterator(Double.valueOf(-1234.5));
        it.setIndex(0);
        if (it.getAttribute(NumberFormat.Field.SIGN) != NumberFormat.Field.SIGN) {
            return n;
        }
        n = 29;
        it.setIndex(1);
        if (it.getAttribute(NumberFormat.Field.CURRENCY) != NumberFormat.Field.CURRENCY) {
            return n;
        }
        n = 30;
        it.setIndex(3);
        if (it.getAttribute(NumberFormat.Field.GROUPING_SEPARATOR) == null
                || it.getAttribute(NumberFormat.Field.INTEGER) == null) {
            return n;
        }
        n = 31;
        it.setIndex(8);
        if (it.getAttribute(NumberFormat.Field.FRACTION) == null) {
            return n;
        }

        // ---- fabricas de NumberFormat ----
        n = 32;
        if (!NumberFormat.getCurrencyInstance(Locale.GERMANY).format(1234.5)
                .equals("1.234,50\u00a0\u20ac")) {
            return n;
        }
        n = 33;
        if (!NumberFormat.getCurrencyInstance(Locale.US).format(1234.5).equals("$1,234.50")) {
            return n;
        }
        n = 34;
        NumberFormat ent = NumberFormat.getIntegerInstance(Locale.US);
        if (!ent.isParseIntegerOnly()) {
            return n;
        }
        n = 35;
        ParsePosition pi = new ParsePosition(0);
        Object iv = ent.parse("12.75", pi);
        if (pi.getIndex() != 2 || ((Number) iv).longValue() != 12L) {
            return n;
        }
        n = 36;
        if (NumberFormat.getPercentInstance(Locale.US).isParseIntegerOnly()) {
            return n;
        }
        n = 37;
        if (!NumberFormat.getNumberInstance(Locale.FRANCE).format(1234.5)
                .equals("1\u202f234,5")) {
            return n;
        }

        // ---- CompactNumberFormat ----
        n = 38;
        String[] compactos = {"", "", "", "0K", "00K", "000K", "0M", "00M", "000M", "0B"};
        CompactNumberFormat cnf = new CompactNumberFormat("#,##0.###",
                new DecimalFormatSymbols(Locale.US), compactos);
        long[] entradas = {0L, 999L, 1000L, 1234L, 9999L, 12345L, 999999L, 1234567L};
        String[] esperados = {"0", "999", "1K", "1K", "10K", "12K", "1M", "1M"};
        for (int i = 0; i < entradas.length; i = i + 1) {
            if (!cnf.format(entradas[i]).equals(esperados[i])) {
                return n;
            }
        }
        n = 39;
        if (cnf.getMaximumFractionDigits() != 0 || cnf.isGroupingUsed()) {
            return n;
        }
        n = 40;
        cnf.setMaximumFractionDigits(1);
        if (!cnf.format(1234L).equals("1.2K")) {
            return n;
        }
        cnf.setMaximumFractionDigits(0);
        n = 41;
        Object cp = cnf.parse("12K", new ParsePosition(0));
        if (((Number) cp).longValue() != 12000L) {
            return n;
        }

        // ---- MessageFormat ----
        n = 42;
        MessageFormat mf = new MessageFormat("Hay {0} archivos en {1}", Locale.US);
        if (!mf.format(new Object[] {Integer.valueOf(1234), "C:"})
                .equals("Hay 1,234 archivos en C:")) {
            return n;
        }
        n = 43;
        if (!mf.toPattern().equals("Hay {0} archivos en {1}")) {
            return n;
        }
        n = 44;
        if (!new MessageFormat("{1} y {0}", Locale.US).format(new Object[] {"a", "b"})
                .equals("b y a")) {
            return n;
        }
        n = 45;
        MessageFormat q = new MessageFormat("no '{0}' se toca pero {0} si", Locale.US);
        if (!q.format(new Object[] {"X"}).equals("no {0} se toca pero X si")) {
            return n;
        }
        n = 46;
        if (!q.toPattern().equals("no '{'0} se toca pero {0} si")) {
            return n;
        }
        n = 47;
        MessageFormat mc = new MessageFormat("{0,number,currency}", Locale.US);
        if (!mc.format(new Object[] {Double.valueOf(1234.5)}).equals("$1,234.50")) {
            return n;
        }
        n = 48;
        if (!mc.toPattern().equals("{0,number,currency}")) {
            return n;
        }
        n = 49;
        MessageFormat ch = new MessageFormat(
                "{0,choice,0#ningun archivo|1#un archivo|2#{0} archivos}", Locale.US);
        if (!ch.format(new Object[] {Integer.valueOf(0)}).equals("ningun archivo")
                || !ch.format(new Object[] {Integer.valueOf(1)}).equals("un archivo")
                || !ch.format(new Object[] {Integer.valueOf(5)}).equals("5 archivos")) {
            return n;
        }
        n = 50;
        MessageFormat mp = new MessageFormat("Hay {0,number,integer} archivos en {1}", Locale.US);
        Object[] leido = mp.parse("Hay 1,234 archivos en C:");
        if (leido == null || leido.length != 2 || ((Number) leido[0]).longValue() != 1234L
                || !"C:".equals(leido[1])) {
            return n;
        }
        n = 51;
        // Un argumento que no vino se escribe {n}: es informacion, no un hueco silencioso.
        if (!mf.format(new Object[] {Integer.valueOf(1)}).equals("Hay 1 archivos en {1}")) {
            return n;
        }
        n = 52;
        if (!MessageFormat.format("a{0}b", "X").equals("aXb")) {
            return n;
        }
        n = 53;
        AttributedCharacterIterator mi = mf.formatToCharacterIterator(
                new Object[] {Integer.valueOf(7), "D:"});
        mi.setIndex(4);
        Object arg = mi.getAttribute(MessageFormat.Field.ARGUMENT);
        if (!(arg instanceof Integer) || ((Integer) arg).intValue() != 0) {
            return n;
        }
        n = 54;
        mi.setIndex(0);
        if (mi.getAttribute(MessageFormat.Field.ARGUMENT) != null) {
            return n;
        }

        // ---- DateFormat / SimpleDateFormat ----
        TimeZone gmt = TimeZone.getTimeZone("GMT");
        Date cero = new Date(0L);
        n = 55;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        sdf.setTimeZone(gmt);
        if (!sdf.format(cero).equals("1970-01-01 00:00:00")) {
            return n;
        }
        n = 56;
        if (sdf.parse("1970-01-01 00:00:00").getTime() != 0L) {
            return n;
        }
        n = 57;
        SimpleDateFormat larga = new SimpleDateFormat("EEEE, MMMM d, y", Locale.US);
        larga.setTimeZone(gmt);
        if (!larga.format(cero).equals("Thursday, January 1, 1970")) {
            return n;
        }
        n = 58;
        SimpleDateFormat esp = new SimpleDateFormat("d 'de' MMMM 'de' y",
                Locale.forLanguageTag("es-AR"));
        esp.setTimeZone(gmt);
        if (!esp.format(cero).equals("1 de enero de 1970")) {
            return n;
        }
        n = 59;
        SimpleDateFormat doce = new SimpleDateFormat("h:mm a", Locale.US);
        doce.setTimeZone(gmt);
        if (!doce.format(new Date(13L * 3600000L + 5L * 60000L)).equals("1:05 PM")) {
            return n;
        }
        n = 60;
        if (!doce.format(cero).equals("12:00 AM")) {
            return n;
        }
        n = 61;
        SimpleDateFormat corto = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT,
                Locale.US);
        if (!corto.toPattern().equals("M/d/yy")) {
            return n;
        }
        n = 62;
        SimpleDateFormat dt = (SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.SHORT,
                DateFormat.SHORT, Locale.US);
        if (!dt.toPattern().equals("M/d/yy, h:mm\u202fa")) {
            return n;
        }
        n = 63;
        SimpleDateFormat fr = (SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.FULL,
                DateFormat.SHORT, Locale.FRANCE);
        if (!fr.toPattern().equals("EEEE d MMMM y HH:mm")) {
            return n;
        }
        n = 64;
        SimpleDateFormat de = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.MEDIUM,
                Locale.GERMANY);
        if (!de.toPattern().equals("dd.MM.y")) {
            return n;
        }
        n = 65;
        // Ida y vuelta con el patron del locale, que no depende del texto que salga.
        DateFormat rt = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US);
        rt.setTimeZone(gmt);
        long dia = 20000L * 86400000L;
        if (rt.parse(rt.format(new Date(dia))).getTime() != dia) {
            return n;
        }
        n = 66;
        SimpleDateFormat y2 = new SimpleDateFormat("MM/dd/yy", Locale.US);
        y2.setTimeZone(gmt);
        if (!y2.format(cero).equals("01/01/70")) {
            return n;
        }
        n = 67;
        SimpleDateFormat cit = new SimpleDateFormat("'hoy es' EEEE", Locale.US);
        cit.setTimeZone(gmt);
        if (!cit.format(cero).equals("hoy es Thursday")) {
            return n;
        }
        n = 68;
        FieldPosition fmes = new FieldPosition(DateFormat.MONTH_FIELD);
        sdf.format(cero, new StringBuffer(), fmes);
        if (fmes.getBeginIndex() != 5 || fmes.getEndIndex() != 7) {
            return n;
        }
        n = 69;
        AttributedCharacterIterator di = sdf.formatToCharacterIterator(cero);
        di.setIndex(0);
        if (di.getAttribute(DateFormat.Field.YEAR) != DateFormat.Field.YEAR) {
            return n;
        }
        n = 70;
        if (DateFormat.Field.ofCalendarField(Calendar.MONTH) != DateFormat.Field.MONTH
                || DateFormat.Field.MONTH.getCalendarField() != Calendar.MONTH) {
            return n;
        }
        n = 71;
        // Los nombres puestos a mano mandan sobre los de la zona.
        DateFormatSymbols sim = new DateFormatSymbols(Locale.US);
        sim.setZoneStrings(new String[][] {{"GMT", "Larga", "L", "LargaV", "LV"}});
        SimpleDateFormat zf = new SimpleDateFormat("z zzzz", sim);
        zf.setTimeZone(gmt);
        if (!zf.format(cero).equals("L Larga")) {
            return n;
        }
        n = 72;
        try {
            sim.setZoneStrings(new String[][] {{"GMT", "a", "b", "c"}});
            return n;
        } catch (IllegalArgumentException e) {
            // esperado: menos de cinco columnas
        }
        n = 73;
        // applyPattern cambia lo que devuelve toPattern: el patron no es solo el de construccion.
        SimpleDateFormat ap = new SimpleDateFormat("yyyy", Locale.US);
        ap.applyPattern("MM/dd");
        ap.setTimeZone(gmt);
        if (!ap.toPattern().equals("MM/dd") || !ap.format(cero).equals("01/01")) {
            return n;
        }

        // ---- AttributedString ----
        n = 74;
        AttributedString as = new AttributedString("hola mundo");
        as.addAttribute(AttributedCharacterIterator.Attribute.LANGUAGE, Locale.US, 0, 4);
        AttributedCharacterIterator ai = as.getIterator();
        ai.setIndex(0);
        if (ai.getAttribute(AttributedCharacterIterator.Attribute.LANGUAGE) != Locale.US) {
            return n;
        }
        n = 75;
        if (ai.getRunLimit() != 4) {
            return n;
        }
        n = 76;
        ai.setIndex(5);
        if (ai.getAttribute(AttributedCharacterIterator.Attribute.LANGUAGE) != null) {
            return n;
        }
        n = 77;
        if (ai.first() != 'h' || ai.last() != 'o' || ai.getBeginIndex() != 0
                || ai.getEndIndex() != 10) {
            return n;
        }
        n = 78;
        ai.setIndex(10);
        // CharacterIterator.DONE y no AttributedCharacterIterator.DONE: nuestro javac todavia
        // no ve una constante de interfaz heredada a traves de la subinterfaz (finding #321).
        if (ai.current() != CharacterIterator.DONE) {
            return n;
        }
        n = 79;
        if (as.getIterator(null, 2, 6).getBeginIndex() != 2) {
            return n;
        }

        // ---- ListFormat ----
        n = 80;
        String[] patrones = {"{0}, {1}", "{0}, {1}", "{0} y {1}", "{0} y {1}", "{0}, {1} y {2}"};
        ListFormat lf = ListFormat.getInstance(patrones);
        if (!lf.format(TextTest.lista("a", null, null)).equals("a")) {
            return n;
        }
        n = 81;
        if (!lf.format(TextTest.lista("a", "b", null)).equals("a y b")) {
            return n;
        }
        n = 82;
        if (!lf.format(TextTest.lista("a", "b", "c")).equals("a, b y c")) {
            return n;
        }
        n = 83;
        List<String> vuelta = lf.parse("a, b y c");
        if (vuelta.size() != 3 || !vuelta.get(1).equals("b")) {
            return n;
        }
        n = 84;
        if (lf.getPatterns().length != 5 || !lf.getLocale().equals(Locale.ROOT)) {
            return n;
        }

        // ---- Bidi ----
        n = 85;
        Bidi b1 = new Bidi("abc", Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        if (!b1.isLeftToRight() || b1.getBaseLevel() != 0 || b1.getRunCount() != 1) {
            return n;
        }
        n = 86;
        Bidi b2 = new Bidi("\u05d0\u05d1\u05d2", Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        if (!b2.isRightToLeft() || b2.getBaseLevel() != 1) {
            return n;
        }
        n = 87;
        Bidi b3 = new Bidi("abc \u05d0\u05d1\u05d2 def", Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        if (!b3.isMixed() || b3.getBaseLevel() != 0 || b3.getRunCount() != 3) {
            return n;
        }
        n = 88;
        if (b3.getRunStart(1) != 4 || b3.getRunLimit(1) != 7 || b3.getRunLevel(1) != 1) {
            return n;
        }
        n = 89;
        if (b3.getLevelAt(0) != 0 || b3.getLevelAt(4) != 1 || b3.getLevelAt(10) != 0) {
            return n;
        }
        n = 90;
        Bidi b4 = new Bidi("\u05d0\u05d1\u05d2 abc \u05d3\u05d4",
                Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        if (b4.getBaseLevel() != 1 || b4.getRunCount() != 3 || b4.getRunLevel(1) != 2) {
            return n;
        }
        n = 91;
        if (!Bidi.requiresBidi("\u05d0".toCharArray(), 0, 1)
                || Bidi.requiresBidi("abc".toCharArray(), 0, 3)) {
            return n;
        }
        n = 92;
        byte[] niveles = new byte[6];
        niveles[0] = (byte) 0;
        niveles[1] = (byte) 0;
        niveles[2] = (byte) 1;
        niveles[3] = (byte) 2;
        niveles[4] = (byte) 1;
        niveles[5] = (byte) 0;
        Object[] objetos = new Object[6];
        for (int i = 0; i < 6; i = i + 1) {
            objetos[i] = Integer.valueOf(i);
        }
        Bidi.reorderVisually(niveles, 0, objetos, 0, 6);
        StringBuilder orden = new StringBuilder();
        for (int i = 0; i < 6; i = i + 1) {
            orden.append(objetos[i].toString());
        }
        if (!orden.toString().equals("014325")) {
            return n;
        }
        n = 93;
        Bidi linea = b3.createLineBidi(0, 6);
        if (linea.getLength() != 6 || linea.getBaseLevel() != 0) {
            return n;
        }

        // ---- Format: la mitad de parseo ----
        n = 94;
        Format fmt = f;
        Object po = fmt.parseObject("1,234");
        if (((Number) po).longValue() != 1234L) {
            return n;
        }
        n = 95;
        AttributedCharacterIterator plano = new MessageFormat("abc", Locale.US)
                .formatToCharacterIterator(new Object[0]);
        if (plano.getEndIndex() != 3 || !plano.getAllAttributeKeys().isEmpty()) {
            return n;
        }

        return -1;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(run());
    }
}
