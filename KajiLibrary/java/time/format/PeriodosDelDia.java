package java.time.format;

import java.util.Locale;

// Los nombres de los periodos del dia --"in the morning", "nachmittags", "madrugada"-- y los
// minutos en que empieza cada uno.
//
// ===============================================================================================
// POR QUE ES UNA TABLA Y NO UNA REGLA
// ===============================================================================================
//
// Porque los cortes no son universales y ni siquiera son parejos. El aleman parte la manana en dos
// --`morgens` hasta las diez, `vormittags` hasta el mediodia-- y el ingles no; el espanol tiene
// `madrugada` de cero a seis y el frances no tiene nada equivalente; el japones vuelve a `noche`
// a las 23. Y `mediodia` dura UN minuto en casi todos los idiomas, pero en aleman dura una hora.
// Cualquier regla que se escriba a ojo es falsa en la mitad de las filas.
//
// Las seis filas son las mismas de `DecimalFormatSymbols`, en el mismo orden, y se resuelven con
// su mismo `indexOf`. Se extrajeron corriendo el JDK 25 --se le pidieron los 1440 minutos del dia
// en cada locale y en cada estilo, y se anotaron los cortes-- que es la misma metodologia de las
// otras tablas de datos de la biblioteca, no una transcripcion.
//
// `SHORT` da lo mismo que `FULL` en las seis filas, y las formas `_STANDALONE` dan lo mismo que su
// base: se comprobo, y por eso hay dos tablas y no seis.
//
// La resolucion del locale esta escrita aca --`indice`-- y no se llama a la de
// `DecimalFormatSymbols`, que es de paquete y esta en `java.text`. La regla es la misma y es corta:
// etiqueta exacta, si no el idioma solo, si no la fila cero.
final class PeriodosDelDia {

    private PeriodosDelDia() {
    }

    // Los nombres largos; SHORT da lo mismo que FULL.
    private static final int[][] ANCHOS_FIN = {
        {720, 1440},   // und
        {1, 720, 721, 1080, 1260, 1440},   // en-US
        {360, 720, 721, 1200, 1440},   // es-AR
        {1, 300, 600, 720, 780, 1080, 1440},   // de-DE
        {1, 720, 721, 1080, 1440},   // fr-FR
        {1, 240, 720, 721, 960, 1140, 1380, 1440},   // ja-JP
    };

    private static final String[][] ANCHOS_NOMBRE = {
        {"AM", "PM"},   // und
        {"midnight", "in the morning", "noon", "in the afternoon", "in the evening", "at night"},   // en-US
        {"madrugada", "ma\u00f1ana", "mediod\u00eda", "tarde", "noche"},   // es-AR
        {"Mitternacht", "nachts", "morgens", "vormittags", "mittags", "nachmittags", "abends"},   // de-DE
        {"minuit", "du matin", "midi", "de l\u2019apr\u00e8s-midi", "du soir"},   // fr-FR
        {"\u771f\u591c\u4e2d", "\u591c\u4e2d", "\u671d", "\u6b63\u5348", "\u663c", "\u5915\u65b9", "\u591c", "\u591c\u4e2d"},   // ja-JP
    };

    // Los nombres angostos.
    private static final int[][] ANGOSTOS_FIN = {
        {720, 1440},   // und
        {1, 720, 721, 1080, 1260, 1440},   // en-US
        {360, 720, 721, 1200, 1440},   // es-AR
        {1, 300, 600, 720, 780, 1080, 1440},   // de-DE
        {1, 240, 720, 721, 1080, 1440},   // fr-FR
        {1, 240, 720, 721, 960, 1140, 1380, 1440},   // ja-JP
    };

    private static final String[][] ANGOSTOS_NOMBRE = {
        {"AM", "PM"},   // und
        {"mi", "in the morning", "n", "in the afternoon", "in the evening", "at night"},   // en-US
        {"madrugada", "ma\u00f1ana", "mediod\u00eda", "tarde", "noche"},   // es-AR
        {"Mitternacht", "nachts", "morgens", "vorm.", "mittags", "nachm.", "abends"},   // de-DE
        {"minuit", "matin", "mat.", "midi", "ap.m.", "soir"},   // fr-FR
        {"\u771f\u591c\u4e2d", "\u591c\u4e2d", "\u671d", "\u6b63\u5348", "\u663c", "\u5915\u65b9", "\u591c", "\u591c\u4e2d"},   // ja-JP
    };

    /** Las etiquetas de las seis filas, en el orden en que estan. */
    private static final String[] ETIQUETAS = {"und", "en-US", "es-AR", "de-DE", "fr-FR", "ja-JP"};

    /** Que fila le toca a ese locale; ver la nota de la clase. */
    private static int indice(Locale locale) {
        String lang = locale.getLanguage();
        String pais = locale.getCountry();
        String completa = pais.length() > 0 ? lang + "-" + pais : lang;
        for (int i = 0; i < ETIQUETAS.length; i++) {
            if (ETIQUETAS[i].equals(completa)) {
                return i;
            }
        }
        if (lang.length() > 0) {
            for (int i = 0; i < ETIQUETAS.length; i++) {
                String e = ETIQUETAS[i];
                int guion = e.indexOf('-');
                String soloIdioma = guion > 0 ? e.substring(0, guion) : e;
                if (soloIdioma.equals(lang)) {
                    return i;
                }
            }
        }
        return 0;
    }

    /**
     * El nombre del periodo en que cae ese minuto del dia.
     *
     * @param locale en que idioma
     * @param estilo con que estilo
     * @param minutoDelDia el minuto, de 0 a 1439
     * @return el nombre
     */
    static String nombre(Locale locale, TextStyle estilo, int minutoDelDia) {
        int i = indice(locale);
        boolean angosto = estilo == TextStyle.NARROW || estilo == TextStyle.NARROW_STANDALONE;
        int[] fines = angosto ? ANGOSTOS_FIN[i] : ANCHOS_FIN[i];
        String[] nombres = angosto ? ANGOSTOS_NOMBRE[i] : ANCHOS_NOMBRE[i];
        int k = 0;
        while (k < fines.length - 1 && minutoDelDia >= fines[k]) {
            k = k + 1;
        }
        return nombres[k];
    }

    /**
     * Los nombres de esa fila, para reconocerlos al parsear.
     *
     * @param locale en que idioma
     * @param estilo con que estilo
     * @return los nombres, en el orden de los tramos
     */
    static String[] nombres(Locale locale, TextStyle estilo) {
        int i = indice(locale);
        boolean angosto = estilo == TextStyle.NARROW || estilo == TextStyle.NARROW_STANDALONE;
        return angosto ? ANGOSTOS_NOMBRE[i] : ANCHOS_NOMBRE[i];
    }

    /**
     * El minuto del medio del tramo numero `k`, que es a donde resuelve un periodo al parsearlo.
     *
     * <p>Es lo que hace el JDK: "in the morning" no dice que hora es, asi que se toma el punto
     * medio de su tramo. Con division entera hacia abajo, igual que alla.
     *
     * @param locale en que idioma
     * @param estilo con que estilo
     * @param k que tramo
     * @return el minuto del dia
     */
    static int medio(Locale locale, TextStyle estilo, int k) {
        int i = indice(locale);
        boolean angosto = estilo == TextStyle.NARROW || estilo == TextStyle.NARROW_STANDALONE;
        int[] fines = angosto ? ANGOSTOS_FIN[i] : ANCHOS_FIN[i];
        int desde = k == 0 ? 0 : fines[k - 1];
        return (desde + fines[k]) / 2;
    }
}
