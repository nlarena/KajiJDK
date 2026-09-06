package java.time.format;

import java.util.Locale;

// Las plantillas de formato --`yMMMd`, `Hm`-- y el patron que a cada una le corresponde en cada
// idioma.
//
// ===============================================================================================
// QUE ES UNA PLANTILLA Y EN QUE SE DIFERENCIA DE UN PATRON
// ===============================================================================================
//
// Una plantilla dice QUE campos se quieren y con cuanto detalle: `yMMMd` es "el ano, el mes con
// nombre corto, y el dia". Un patron dice ADEMAS en que orden y con que separadores, y eso cambia
// de idioma en idioma: la misma plantilla da `MMM d, y` en ingles y `d. MMM y` en aleman. La
// plantilla es lo que un programa puede escribir sin saber en que idioma va a salir.
//
// ===============================================================================================
// COMO SE RESUELVE
// ===============================================================================================
//
// Por tabla, no por algoritmo. Se parte la plantilla en su mitad de fecha y su mitad de hora --el
// orden de los simbolos esta fijado, asi que el corte es unico-- se busca cada mitad, y se pegan
// con el pegamento del idioma. Las combinaciones que no salen de ese pegado estan aparte, en la
// tabla de excepciones: son las que empiezan con el dia de la semana solo, donde el idioma usa la
// forma de contexto (`E`) en vez de la suelta (`ccc`).
//
// Una plantilla que no esta en la tabla no se inventa: se contesta con `DateTimeException`, que es
// lo que hace el JDK. La tabla se extrajo corriendo el JDK 25 --se le pidieron las 38.880
// plantillas de fecha y las 8.910 de hora que la gramatica admite, y se anotaron las que resolvio
// junto con las 2.600 a 3.550 combinaciones de cada idioma-- que es la misma metodologia de las
// otras tablas de datos de la biblioteca.
//
// Las seis filas son las de `DecimalFormatSymbols`, en el mismo orden, y se resuelven con la misma
// regla que `PeriodosDelDia`.
final class PlantillasLocales {

    private PlantillasLocales() {
    }

    /** El pegamento de cada fila: lo que va entre la fecha y la hora. */
    private static final String[] PEGAMENTO = {" ", ", ", ", ", ", ", " ", " "};

    // ---- und ----

    private static String[] fechasK0() {
        return new String[] {
            "E", "EEE", "EEEE", "EEEEE", "EEEEEd", "EEEEd", "EEEd", "Ed", "Gy", "GyM",
            "GyMEEEEEd", "GyMEEEEd", "GyMEEEd", "GyMEd", "GyMM", "GyMMEEEEEd", "GyMMEEEEd",
            "GyMMEEEd", "GyMMEd", "GyMMM", "GyMMMEEEEEd", "GyMMMEEEEd", "GyMMMEEEd", "GyMMMEd",
            "GyMMMM", "GyMMMMEEEEEd", "GyMMMMEEEEd", "GyMMMMEEEd", "GyMMMMEd", "GyMMMMM",
            "GyMMMMMEEEEEd", "GyMMMMMEEEEd", "GyMMMMMEEEd", "GyMMMMMEd", "GyMMMMMd", "GyMMMMd",
            "GyMMMd", "GyMMd", "GyMd", "M", "MEEEEEd", "MEEEEd", "MEEEd", "MEd", "MM",
            "MMEEEEEd", "MMEEEEd", "MMEEEd", "MMEd", "MMM", "MMMEEEEEd", "MMMEEEEd", "MMMEEEd",
            "MMMEd", "MMMM", "MMMMEEEEEd", "MMMMEEEEd", "MMMMEEEd", "MMMMEd", "MMMMM",
            "MMMMMEEEEEd", "MMMMMEEEEd", "MMMMMEEEd", "MMMMMEd", "MMMMMd", "MMMMd", "MMMd",
            "MMd", "Md", "d", "y", "yM", "yMEEEEEd", "yMEEEEd", "yMEEEd", "yMEd", "yMM",
            "yMMEEEEEd", "yMMEEEEd", "yMMEEEd", "yMMEd", "yMMM", "yMMMEEEEEd", "yMMMEEEEd",
            "yMMMEEEd", "yMMMEd", "yMMMM", "yMMMMEEEEEd", "yMMMMEEEEd", "yMMMMEEEd", "yMMMMEd",
            "yMMMMM", "yMMMMMEEEEEd", "yMMMMMEEEEd", "yMMMMMEEEd", "yMMMMMEd", "yMMMMMd",
            "yMMMMd", "yMMMd", "yMMd", "yMd", "yQQQ", "yQQQQ", "yw", "yyyy", "yyyyM",
            "yyyyMEEEEEd", "yyyyMEEEEd", "yyyyMEEEd", "yyyyMEd", "yyyyMM", "yyyyMMEEEEEd",
            "yyyyMMEEEEd", "yyyyMMEEEd", "yyyyMMEd", "yyyyMMM", "yyyyMMMEEEEEd", "yyyyMMMEEEEd",
            "yyyyMMMEEEd", "yyyyMMMEd", "yyyyMMMM", "yyyyMMMMEEEEEd", "yyyyMMMMEEEEd",
            "yyyyMMMMEEEd", "yyyyMMMMEd", "yyyyMMMMM", "yyyyMMMMMEEEEEd", "yyyyMMMMMEEEEd",
            "yyyyMMMMMEEEd", "yyyyMMMMMEd", "yyyyMMMMMd", "yyyyMMMMd", "yyyyMMMd", "yyyyMMd",
            "yyyyMd", "yyyyQQQ", "yyyyQQQQ",
        };
    }

    private static String[] fechasP0() {
        return new String[] {
            "ccc", "ccc", "ccc", "ccc", "d, E", "d, E", "d, E", "d, E", "G y", "G y MMM",
            "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM",
            "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM",
            "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM",
            "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM",
            "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM d",
            "G y MMM d", "G y MMM d", "GGGGG y-MM-dd", "GGGGG y-MM-dd", "L", "MM-dd, E",
            "MM-dd, E", "MM-dd, E", "MM-dd, E", "L", "MM-dd, E", "MM-dd, E", "MM-dd, E",
            "MM-dd, E", "LLL", "MMM d, E", "MMM d, E", "MMM d, E", "MMM d, E", "LLL",
            "MMM d, E", "MMM d, E", "MMM d, E", "MMM d, E", "LLL", "MMM d, E", "MMM d, E",
            "MMM d, E", "MMM d, E", "MMMM d", "MMMM d", "MMM d", "MM-dd", "MM-dd", "d", "y",
            "y-MM", "y-MM-dd, E", "y-MM-dd, E", "y-MM-dd, E", "y-MM-dd, E", "y-MM",
            "y-MM-dd, E", "y-MM-dd, E", "y-MM-dd, E", "y-MM-dd, E", "y MMM", "y MMM d, E",
            "y MMM d, E", "y MMM d, E", "y MMM d, E", "y MMMM", "y MMM d, E", "y MMM d, E",
            "y MMM d, E", "y MMM d, E", "y MMMM", "y MMM d, E", "y MMM d, E", "y MMM d, E",
            "y MMM d, E", "y MMM d", "y MMM d", "y MMM d", "y-MM-dd", "y-MM-dd", "y QQQ",
            "y QQQQ", "'week' w 'of' Y", "G y", "GGGGG y-MM", "GGGGG y-MM-dd, E",
            "GGGGG y-MM-dd, E", "GGGGG y-MM-dd, E", "GGGGG y-MM-dd, E", "GGGGG y-MM",
            "GGGGG y-MM-dd, E", "GGGGG y-MM-dd, E", "GGGGG y-MM-dd, E", "GGGGG y-MM-dd, E",
            "G y MMM", "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM d, E",
            "G y MMMM", "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM d, E",
            "G y MMMM", "G y MMM d, E", "G y MMM d, E", "G y MMM d, E", "G y MMM d, E",
            "G y MMM d", "G y MMM d", "G y MMM d", "GGGGG y-MM-dd", "GGGGG y-MM-dd", "G y QQQ",
            "G y QQQQ",
        };
    }

    private static String[] horasK0() {
        return new String[] {
            "Bh", "Bhm", "Bhms", "H", "Hm", "Hms", "Hmsv", "Hmv", "h", "hm", "hms", "hmsv",
            "hmv", "j", "jm", "jms", "jmsv", "jmv", "ms",
        };
    }

    private static String[] horasP0() {
        return new String[] {
            "h B", "h:mm B", "h:mm:ss B", "HH", "HH:mm", "HH:mm:ss", "HH:mm:ss v", "HH:mm v",
            "h\u202fa", "h:mm a", "h:mm:ss a", "h:mm:ss a v", "h:mm a v", "HH", "HH:mm",
            "HH:mm:ss", "HH:mm:ss v", "HH:mm v", "mm:ss",
        };
    }

    private static String[] excK0() {
        return new String[] {
            "E+Bhm", "E+Bhms", "E+Hm", "E+Hms", "E+hm", "E+hms", "E+jm", "E+jms", "EEE+Bhm",
            "EEE+Bhms", "EEE+Hm", "EEE+Hms", "EEE+hm", "EEE+hms", "EEE+jm", "EEE+jms",
            "EEEE+Bhm", "EEEE+Bhms", "EEEE+Hm", "EEEE+Hms", "EEEE+hm", "EEEE+hms", "EEEE+jm",
            "EEEE+jms", "EEEEE+Bhm", "EEEEE+Bhms", "EEEEE+Hm", "EEEEE+Hms", "EEEEE+hm",
            "EEEEE+hms", "EEEEE+jm", "EEEEE+jms",
        };
    }

    private static String[] excP0() {
        return new String[] {
            "E h:mm B", "E h:mm:ss B", "E HH:mm", "E HH:mm:ss", "E h:mm a", "E h:mm:ss a",
            "E HH:mm", "E HH:mm:ss", "E h:mm B", "E h:mm:ss B", "E HH:mm", "E HH:mm:ss",
            "E h:mm a", "E h:mm:ss a", "E HH:mm", "E HH:mm:ss", "E h:mm B", "E h:mm:ss B",
            "E HH:mm", "E HH:mm:ss", "E h:mm a", "E h:mm:ss a", "E HH:mm", "E HH:mm:ss",
            "E h:mm B", "E h:mm:ss B", "E HH:mm", "E HH:mm:ss", "E h:mm a", "E h:mm:ss a",
            "E HH:mm", "E HH:mm:ss",
        };
    }

    // ---- en-US ----

    private static String[] fechasK1() {
        return new String[] {
            "E", "EEE", "EEEE", "EEEEE", "EEEEEd", "EEEEd", "EEEd", "Ed", "Gy", "GyM",
            "GyMEEEEEd", "GyMEEEEd", "GyMEEEd", "GyMEd", "GyMM", "GyMMEEEEEd", "GyMMEEEEd",
            "GyMMEEEd", "GyMMEd", "GyMMM", "GyMMMEEEEEd", "GyMMMEEEEd", "GyMMMEEEd", "GyMMMEd",
            "GyMMMM", "GyMMMMEEEEEd", "GyMMMMEEEEd", "GyMMMMEEEd", "GyMMMMEd", "GyMMMMM",
            "GyMMMMMEEEEEd", "GyMMMMMEEEEd", "GyMMMMMEEEd", "GyMMMMMEd", "GyMMMMMd", "GyMMMMd",
            "GyMMMd", "GyMMd", "GyMd", "M", "MEEEEEd", "MEEEEd", "MEEEd", "MEd", "MM",
            "MMEEEEEd", "MMEEEEd", "MMEEEd", "MMEd", "MMM", "MMMEEEEEd", "MMMEEEEd", "MMMEEEd",
            "MMMEd", "MMMM", "MMMMEEEEEd", "MMMMEEEEd", "MMMMEEEd", "MMMMEd", "MMMMM",
            "MMMMMEEEEEd", "MMMMMEEEEd", "MMMMMEEEd", "MMMMMEd", "MMMMMd", "MMMMd", "MMMd",
            "MMd", "Md", "d", "y", "yM", "yMEEEEEd", "yMEEEEd", "yMEEEd", "yMEd", "yMM",
            "yMMEEEEEd", "yMMEEEEd", "yMMEEEd", "yMMEd", "yMMM", "yMMMEEEEEd", "yMMMEEEEd",
            "yMMMEEEd", "yMMMEd", "yMMMM", "yMMMMEEEEEd", "yMMMMEEEEd", "yMMMMEEEd", "yMMMMEd",
            "yMMMMM", "yMMMMMEEEEEd", "yMMMMMEEEEd", "yMMMMMEEEd", "yMMMMMEd", "yMMMMMd",
            "yMMMMd", "yMMMd", "yMMd", "yMd", "yQQQ", "yQQQQ", "yw", "yyyy", "yyyyM",
            "yyyyMEEEEEd", "yyyyMEEEEd", "yyyyMEEEd", "yyyyMEd", "yyyyMM", "yyyyMMEEEEEd",
            "yyyyMMEEEEd", "yyyyMMEEEd", "yyyyMMEd", "yyyyMMM", "yyyyMMMEEEEEd", "yyyyMMMEEEEd",
            "yyyyMMMEEEd", "yyyyMMMEd", "yyyyMMMM", "yyyyMMMMEEEEEd", "yyyyMMMMEEEEd",
            "yyyyMMMMEEEd", "yyyyMMMMEd", "yyyyMMMMM", "yyyyMMMMMEEEEEd", "yyyyMMMMMEEEEd",
            "yyyyMMMMMEEEd", "yyyyMMMMMEd", "yyyyMMMMMd", "yyyyMMMMd", "yyyyMMMd", "yyyyMMd",
            "yyyyMd", "yyyyQQQ", "yyyyQQQQ",
        };
    }

    private static String[] fechasP1() {
        return new String[] {
            "ccc", "ccc", "ccc", "ccc", "d E", "d E", "d E", "d E", "y G", "MMM y G",
            "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "MMM y G",
            "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "MMM y G",
            "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "MMM y G",
            "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "MMM y G",
            "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "MMM d, y G",
            "MMM d, y G", "MMM d, y G", "M/d/y G", "M/d/y G", "L", "E, M/d", "E, M/d", "E, M/d",
            "E, M/d", "L", "E, M/d", "E, M/d", "E, M/d", "E, M/d", "LLL", "E, MMM d",
            "E, MMM d", "E, MMM d", "E, MMM d", "LLL", "E, MMM d", "E, MMM d", "E, MMM d",
            "E, MMM d", "LLL", "E, MMM d", "E, MMM d", "E, MMM d", "E, MMM d", "MMMM d",
            "MMMM d", "MMM d", "M/d", "M/d", "d", "y", "M/y", "E, M/d/y", "E, M/d/y",
            "E, M/d/y", "E, M/d/y", "M/y", "E, M/d/y", "E, M/d/y", "E, M/d/y", "E, M/d/y",
            "MMM y", "E, MMM d, y", "E, MMM d, y", "E, MMM d, y", "E, MMM d, y", "MMMM y",
            "E, MMM d, y", "E, MMM d, y", "E, MMM d, y", "E, MMM d, y", "MMMM y", "E, MMM d, y",
            "E, MMM d, y", "E, MMM d, y", "E, MMM d, y", "MMM d, y", "MMM d, y", "MMM d, y",
            "M/d/y", "M/d/y", "QQQ y", "QQQQ y", "'week' w 'of' Y", "y G", "M/y GGGGG",
            "E, M/d/y GGGGG", "E, M/d/y GGGGG", "E, M/d/y GGGGG", "E, M/d/y GGGGG", "M/y GGGGG",
            "E, M/d/y GGGGG", "E, M/d/y GGGGG", "E, M/d/y GGGGG", "E, M/d/y GGGGG", "MMM y G",
            "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "MMMM y G",
            "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "MMMM y G",
            "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "E, MMM d, y G", "MMM d, y G",
            "MMM d, y G", "MMM d, y G", "M/d/y GGGGG", "M/d/y GGGGG", "QQQ y G", "QQQQ y G",
        };
    }

    private static String[] horasK1() {
        return new String[] {
            "Bh", "Bhm", "Bhms", "Bj", "Bjm", "Bjms", "H", "Hm", "Hms", "Hmsv", "Hmv", "h",
            "hm", "hms", "hmsv", "hmv", "j", "jm", "jms", "jmsv", "jmv", "ms",
        };
    }

    private static String[] horasP1() {
        return new String[] {
            "h B", "h:mm B", "h:mm:ss B", "h B", "h:mm B", "h:mm:ss B", "HH", "HH:mm",
            "HH:mm:ss", "HH:mm:ss v", "HH:mm v", "h\u202fa", "h:mm\u202fa", "h:mm:ss\u202fa",
            "h:mm:ss\u202fa v", "h:mm\u202fa v", "h\u202fa", "h:mm\u202fa", "h:mm:ss\u202fa",
            "h:mm:ss\u202fa v", "h:mm\u202fa v", "mm:ss",
        };
    }

    private static String[] excK1() {
        return new String[] {
            "E+Bhm", "E+Bhms", "E+Bjm", "E+Bjms", "E+Hm", "E+Hms", "E+hm", "E+hms", "E+jm",
            "E+jms", "EEE+Bhm", "EEE+Bhms", "EEE+Bjm", "EEE+Bjms", "EEE+Hm", "EEE+Hms",
            "EEE+hm", "EEE+hms", "EEE+jm", "EEE+jms", "EEEE+Bhm", "EEEE+Bhms", "EEEE+Bjm",
            "EEEE+Bjms", "EEEE+Hm", "EEEE+Hms", "EEEE+hm", "EEEE+hms", "EEEE+jm", "EEEE+jms",
            "EEEEE+Bhm", "EEEEE+Bhms", "EEEEE+Bjm", "EEEEE+Bjms", "EEEEE+Hm", "EEEEE+Hms",
            "EEEEE+hm", "EEEEE+hms", "EEEEE+jm", "EEEEE+jms",
        };
    }

    private static String[] excP1() {
        return new String[] {
            "E h:mm B", "E h:mm:ss B", "E h:mm B", "E h:mm:ss B", "E HH:mm", "E HH:mm:ss",
            "E h:mm\u202fa", "E h:mm:ss\u202fa", "E h:mm\u202fa", "E h:mm:ss\u202fa",
            "E h:mm B", "E h:mm:ss B", "E h:mm B", "E h:mm:ss B", "E HH:mm", "E HH:mm:ss",
            "E h:mm\u202fa", "E h:mm:ss\u202fa", "E h:mm\u202fa", "E h:mm:ss\u202fa",
            "E h:mm B", "E h:mm:ss B", "E h:mm B", "E h:mm:ss B", "E HH:mm", "E HH:mm:ss",
            "E h:mm\u202fa", "E h:mm:ss\u202fa", "E h:mm\u202fa", "E h:mm:ss\u202fa",
            "E h:mm B", "E h:mm:ss B", "E h:mm B", "E h:mm:ss B", "E HH:mm", "E HH:mm:ss",
            "E h:mm\u202fa", "E h:mm:ss\u202fa", "E h:mm\u202fa", "E h:mm:ss\u202fa",
        };
    }

    // ---- es-AR ----

    private static String[] fechasK2() {
        return new String[] {
            "E", "EEE", "EEEE", "EEEEE", "EEEEEd", "EEEEd", "EEEd", "Ed", "Gy", "GyM",
            "GyMEEEEEd", "GyMEEEEd", "GyMEEEd", "GyMEd", "GyMM", "GyMMEEEEEd", "GyMMEEEEd",
            "GyMMEEEd", "GyMMEd", "GyMMM", "GyMMMEEEEEd", "GyMMMEEEEd", "GyMMMEEEd", "GyMMMEd",
            "GyMMMM", "GyMMMMEEEEEd", "GyMMMMEEEEd", "GyMMMMEEEd", "GyMMMMEd", "GyMMMMM",
            "GyMMMMMEEEEEd", "GyMMMMMEEEEd", "GyMMMMMEEEd", "GyMMMMMEd", "GyMMMMMd", "GyMMMMd",
            "GyMMMd", "GyMMd", "GyMd", "M", "MEEEEEd", "MEEEEd", "MEEEd", "MEd", "MM",
            "MMEEEEEd", "MMEEEEd", "MMEEEd", "MMEd", "MMM", "MMMEEEEEd", "MMMEEEEd", "MMMEEEd",
            "MMMEd", "MMMM", "MMMMEEEEEd", "MMMMEEEEd", "MMMMEEEd", "MMMMEd", "MMMMM",
            "MMMMMEEEEEd", "MMMMMEEEEd", "MMMMMEEEd", "MMMMMEd", "MMMMMd", "MMMMMdd", "MMMMd",
            "MMMMdd", "MMMd", "MMMdd", "MMd", "MMdd", "Md", "Mdd", "d", "y", "yM", "yMEEEEEd",
            "yMEEEEd", "yMEEEd", "yMEd", "yMM", "yMMEEEEEd", "yMMEEEEd", "yMMEEEd", "yMMEd",
            "yMMM", "yMMMEEEEEd", "yMMMEEEEd", "yMMMEEEd", "yMMMEd", "yMMMM", "yMMMMEEEEEd",
            "yMMMMEEEEd", "yMMMMEEEd", "yMMMMEd", "yMMMMM", "yMMMMMEEEEEd", "yMMMMMEEEEd",
            "yMMMMMEEEd", "yMMMMMEd", "yMMMMMd", "yMMMMd", "yMMMd", "yMMd", "yMd", "yQQQ",
            "yQQQQ", "yw", "yyyy", "yyyyM", "yyyyMEEEEEd", "yyyyMEEEEd", "yyyyMEEEd", "yyyyMEd",
            "yyyyMM", "yyyyMMEEEEEd", "yyyyMMEEEEd", "yyyyMMEEEd", "yyyyMMEd", "yyyyMMM",
            "yyyyMMMEEEEEd", "yyyyMMMEEEEd", "yyyyMMMEEEd", "yyyyMMMEd", "yyyyMMMM",
            "yyyyMMMMEEEEEd", "yyyyMMMMEEEEd", "yyyyMMMMEEEd", "yyyyMMMMEd", "yyyyMMMMM",
            "yyyyMMMMMEEEEEd", "yyyyMMMMMEEEEd", "yyyyMMMMMEEEd", "yyyyMMMMMEd", "yyyyMMMMMd",
            "yyyyMMMMd", "yyyyMMMd", "yyyyMMd", "yyyyMd", "yyyyQQQ", "yyyyQQQQ",
        };
    }

    private static String[] fechasP2() {
        return new String[] {
            "ccc", "ccc", "ccc", "ccc", "E d", "E d", "E d", "E d", "y G", "MMMM 'de' y G",
            "E, d 'de' MMMM 'de' y G", "E, d 'de' MMMM 'de' y G", "E, d 'de' MMMM 'de' y G",
            "E, d 'de' MMMM 'de' y G", "MMM 'de' y G", "E, d 'de' MMM 'de' y G",
            "E, d 'de' MMM 'de' y G", "E, d 'de' MMM 'de' y G", "E, d 'de' MMM 'de' y G",
            "MMM 'de' y G", "E, d 'de' MMM 'de' y G", "E, d 'de' MMM 'de' y G",
            "E, d 'de' MMM 'de' y G", "E, d 'de' MMM 'de' y G", "MMMM 'de' y G",
            "E, d 'de' MMMM 'de' y G", "E, d 'de' MMMM 'de' y G", "E, d 'de' MMMM 'de' y G",
            "E, d 'de' MMMM 'de' y G", "MMMM 'de' y G", "E, d 'de' MMMM 'de' y G",
            "E, d 'de' MMMM 'de' y G", "E, d 'de' MMMM 'de' y G", "E, d 'de' MMMM 'de' y G",
            "d 'de' MMMM 'de' y G", "d 'de' MMMM 'de' y G", "d MMM y G", "d/M/y GGGGG",
            "d/M/y GGGGG", "L", "E d-M", "E d-M", "E d-M", "E d-M", "L", "E d-M", "E d-M",
            "E d-M", "E d-M", "LLL", "E, d 'de' MMM", "E, d 'de' MMM", "E, d 'de' MMM",
            "E, d 'de' MMM", "LLL", "E, d 'de' MMMM", "E, d 'de' MMMM", "E, d 'de' MMMM",
            "E, d 'de' MMMM", "LLL", "E, d 'de' MMMM", "E, d 'de' MMMM", "E, d 'de' MMMM",
            "E, d 'de' MMMM", "d 'de' MMMM", "dd-MMM", "d 'de' MMMM", "dd-MMM", "d 'de' MMM",
            "dd-MMM", "d/M", "d/M", "d/M", "dd-MMM", "d", "y G", "M-y", "E, d/M/y", "E, d/M/y",
            "E, d/M/y", "E, d/M/y", "M/y", "E, d/M/y", "E, d/M/y", "E, d/M/y", "E, d/M/y",
            "MMM y", "E, d MMM y", "E, d MMM y", "E, d MMM y", "E, d MMM y", "MMMM 'de' y",
            "EEE, d 'de' MMMM 'de' y", "EEE, d 'de' MMMM 'de' y", "EEE, d 'de' MMMM 'de' y",
            "EEE, d 'de' MMMM 'de' y", "MMMM 'de' y", "EEE, d 'de' MMMM 'de' y",
            "EEE, d 'de' MMMM 'de' y", "EEE, d 'de' MMMM 'de' y", "EEE, d 'de' MMMM 'de' y",
            "d 'de' MMMM 'de' y", "d 'de' MMMM 'de' y", "d 'de' MMM 'de' y", "d/M/y", "d/M/y",
            "QQQ 'de' y", "QQQQ 'de' y", "'semana' w 'de' Y", "y G", "M-y G", "E d/M/y GGGGG",
            "E d/M/y GGGGG", "E d/M/y GGGGG", "E d/M/y GGGGG", "M-y G", "E d/M/y GGGGG",
            "E d/M/y GGGGG", "E d/M/y GGGGG", "E d/M/y GGGGG", "MMM 'de' y G",
            "EEE, d 'de' MMM 'de' y G", "EEE, d 'de' MMM 'de' y G", "EEE, d 'de' MMM 'de' y G",
            "EEE, d 'de' MMM 'de' y G", "MMMM 'de' y G", "E, d 'de' MMMM 'de' y G",
            "E, d 'de' MMMM 'de' y G", "E, d 'de' MMMM 'de' y G", "E, d 'de' MMMM 'de' y G",
            "MMMM 'de' y G", "E, d 'de' MMMM 'de' y G", "E, d 'de' MMMM 'de' y G",
            "E, d 'de' MMMM 'de' y G", "E, d 'de' MMMM 'de' y G", "d 'de' MMMM 'de' y G",
            "d 'de' MMMM 'de' y G", "d 'de' MMM 'de' y G", "d/M/y GGGGG", "d/M/y GGGGG",
            "QQQ 'de' y G", "QQQQ 'de' y G",
        };
    }

    private static String[] horasK2() {
        return new String[] {
            "Bh", "Bhm", "Bhms", "Bj", "Bjm", "Bjms", "H", "Hm", "Hms", "Hmsv", "Hmsvvvv",
            "Hmv", "h", "hm", "hms", "hmsv", "hmsvvvv", "hmv", "j", "jm", "jms", "jmsv",
            "jmsvvvv", "jmv", "ms",
        };
    }

    private static String[] horasP2() {
        return new String[] {
            "h B", "h:mm B", "h:mm:ss B", "h B", "h:mm B", "h:mm:ss B", "H", "H:mm", "H:mm:ss",
            "HH:mm:ss v", "HH:mm:ss (vvvv)", "HH:mm v", "h\u202fa", "h:mm\u202fa", "hh:mm:ss",
            "h:mm:ss\u202fa v", "h:mm:ss\u202fa (vvvv)", "h:mm\u202fa v", "h\u202fa",
            "h:mm\u202fa", "hh:mm:ss", "h:mm:ss\u202fa v", "h:mm:ss\u202fa (vvvv)",
            "h:mm\u202fa v", "mm:ss",
        };
    }

    private static String[] excK2() {
        return new String[] {
            "E+Bhm", "E+Bhms", "E+Bjm", "E+Bjms", "E+Hm", "E+Hms", "E+hm", "E+hms", "E+jm",
            "E+jms", "EEE+Bhm", "EEE+Bhms", "EEE+Bjm", "EEE+Bjms", "EEE+Hm", "EEE+Hms",
            "EEE+hm", "EEE+hms", "EEE+jm", "EEE+jms", "EEEE+Bhm", "EEEE+Bhms", "EEEE+Bjm",
            "EEEE+Bjms", "EEEE+Hm", "EEEE+Hms", "EEEE+hm", "EEEE+hms", "EEEE+jm", "EEEE+jms",
            "EEEEE+Bhm", "EEEEE+Bhms", "EEEEE+Bjm", "EEEEE+Bjms", "EEEEE+Hm", "EEEEE+Hms",
            "EEEEE+hm", "EEEEE+hms", "EEEEE+jm", "EEEEE+jms",
        };
    }

    private static String[] excP2() {
        return new String[] {
            "E h:mm B", "E h:mm:ss B", "E h:mm B", "E h:mm:ss B", "E, HH:mm", "E, HH:mm:ss",
            "E, h:mm\u202fa", "E, h:mm:ss\u202fa", "E, h:mm\u202fa", "E, h:mm:ss\u202fa",
            "E h:mm B", "E h:mm:ss B", "E h:mm B", "E h:mm:ss B", "E, HH:mm", "E, HH:mm:ss",
            "E, h:mm\u202fa", "E, h:mm:ss\u202fa", "E, h:mm\u202fa", "E, h:mm:ss\u202fa",
            "E h:mm B", "E h:mm:ss B", "E h:mm B", "E h:mm:ss B", "E, HH:mm", "E, HH:mm:ss",
            "E, h:mm\u202fa", "E, h:mm:ss\u202fa", "E, h:mm\u202fa", "E, h:mm:ss\u202fa",
            "E h:mm B", "E h:mm:ss B", "E h:mm B", "E h:mm:ss B", "E, HH:mm", "E, HH:mm:ss",
            "E, h:mm\u202fa", "E, h:mm:ss\u202fa", "E, h:mm\u202fa", "E, h:mm:ss\u202fa",
        };
    }

    // ---- de-DE ----

    private static String[] fechasK3() {
        return new String[] {
            "E", "EEE", "EEEE", "EEEEE", "EEEEEd", "EEEEd", "EEEd", "Ed", "Gy", "GyM",
            "GyMEEEEEd", "GyMEEEEd", "GyMEEEd", "GyMEd", "GyMM", "GyMMEEEEEd", "GyMMEEEEd",
            "GyMMEEEd", "GyMMEd", "GyMMM", "GyMMMEEEEEd", "GyMMMEEEEd", "GyMMMEEEd", "GyMMMEd",
            "GyMMMM", "GyMMMMEEEEEd", "GyMMMMEEEEd", "GyMMMMEEEd", "GyMMMMEd", "GyMMMMM",
            "GyMMMMMEEEEEd", "GyMMMMMEEEEd", "GyMMMMMEEEd", "GyMMMMMEd", "GyMMMMMd", "GyMMMMd",
            "GyMMMd", "GyMMd", "GyMd", "M", "MEEEEEd", "MEEEEd", "MEEEd", "MEd", "MM",
            "MMEEEEEd", "MMEEEEd", "MMEEEd", "MMEd", "MMM", "MMMEEEEEd", "MMMEEEEd", "MMMEEEd",
            "MMMEd", "MMMM", "MMMMEEEEEd", "MMMMEEEEd", "MMMMEEEd", "MMMMEd", "MMMMM",
            "MMMMMEEEEEd", "MMMMMEEEEd", "MMMMMEEEd", "MMMMMEd", "MMMMMd", "MMMMMdd", "MMMMd",
            "MMMMdd", "MMMd", "MMMdd", "MMd", "MMdd", "Md", "Mdd", "d", "y", "yM", "yMEEEEEd",
            "yMEEEEd", "yMEEEd", "yMEd", "yMM", "yMMEEEEEd", "yMMEEEEd", "yMMEEEd", "yMMEd",
            "yMMM", "yMMMEEEEEd", "yMMMEEEEd", "yMMMEEEd", "yMMMEd", "yMMMM", "yMMMMEEEEEd",
            "yMMMMEEEEd", "yMMMMEEEd", "yMMMMEd", "yMMMMM", "yMMMMMEEEEEd", "yMMMMMEEEEd",
            "yMMMMMEEEd", "yMMMMMEd", "yMMMMMd", "yMMMMMdd", "yMMMMd", "yMMMMdd", "yMMMd",
            "yMMMdd", "yMMd", "yMMdd", "yMd", "yMdd", "yQQQ", "yQQQQ", "yw", "yyyy", "yyyyM",
            "yyyyMEEEEEd", "yyyyMEEEEd", "yyyyMEEEd", "yyyyMEd", "yyyyMM", "yyyyMMEEEEEd",
            "yyyyMMEEEEd", "yyyyMMEEEd", "yyyyMMEd", "yyyyMMM", "yyyyMMMEEEEEd", "yyyyMMMEEEEd",
            "yyyyMMMEEEd", "yyyyMMMEd", "yyyyMMMM", "yyyyMMMMEEEEEd", "yyyyMMMMEEEEd",
            "yyyyMMMMEEEd", "yyyyMMMMEd", "yyyyMMMMM", "yyyyMMMMMEEEEEd", "yyyyMMMMMEEEEd",
            "yyyyMMMMMEEEd", "yyyyMMMMMEd", "yyyyMMMMMd", "yyyyMMMMd", "yyyyMMMd", "yyyyMMd",
            "yyyyMd", "yyyyQQQ", "yyyyQQQQ",
        };
    }

    private static String[] fechasP3() {
        return new String[] {
            "ccc", "ccc", "ccc", "ccc", "E, d.", "E, d.", "E, d.", "E, d.", "y G", "MMM y G",
            "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "MMM y G",
            "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "MMM y G",
            "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "MMM y G",
            "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "MMM y G",
            "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "d. MMM y G",
            "d. MMM y G", "d. MMM y G", "dd.MM.y G", "dd.MM.y G", "L", "E, d.M.", "E, d.M.",
            "E, d.M.", "E, d.M.", "L", "E, d.M.", "E, d.M.", "E, d.M.", "E, d.M.", "LLL",
            "E, d. MMM", "E, d. MMM", "E, d. MMM", "E, d. MMM", "LLL", "E, d. MMMM",
            "E, d. MMMM", "E, d. MMMM", "E, d. MMMM", "LLL", "E, d. MMMM", "E, d. MMMM",
            "E, d. MMMM", "E, d. MMMM", "d. MMMM", "dd.MM.", "d. MMMM", "dd.MM.", "d. MMM",
            "dd.MM.", "d.MM.", "dd.MM.", "d.M.", "dd.MM.", "d", "y G", "M/y", "E, d.M.y",
            "E, d.M.y", "E, d.M.y", "E, d.M.y", "MM.y", "E, d.M.y", "E, d.M.y", "E, d.M.y",
            "E, d.M.y", "MMM y", "E, d. MMM y", "E, d. MMM y", "E, d. MMM y", "E, d. MMM y",
            "MMMM y", "E, d. MMM y", "E, d. MMM y", "E, d. MMM y", "E, d. MMM y", "MMMM y",
            "E, d. MMM y", "E, d. MMM y", "E, d. MMM y", "E, d. MMM y", "d. MMM y", "dd.MM.y",
            "d. MMM y", "dd.MM.y", "d. MMM y", "dd.MM.y", "d.M.y", "dd.MM.y", "d.M.y",
            "dd.MM.y", "QQQ y", "QQQQ y", "'Woche' w 'des' 'Jahres' Y", "y G", "M/y GGGGG",
            "E, d.M.y GGGGG", "E, d.M.y GGGGG", "E, d.M.y GGGGG", "E, d.M.y GGGGG", "M/y GGGGG",
            "E, d.M.y GGGGG", "E, d.M.y GGGGG", "E, d.M.y GGGGG", "E, d.M.y GGGGG", "MMM y G",
            "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "MMMM y G",
            "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "MMMM y G",
            "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "E, d. MMM y G", "d. MMM y G",
            "d. MMM y G", "d. MMM y G", "d.M.y GGGGG", "d.M.y GGGGG", "QQQ y G", "QQQQ y G",
        };
    }

    private static String[] horasK3() {
        return new String[] {
            "Bh", "Bhm", "Bhms", "H", "Hm", "Hms", "Hmsv", "Hmv", "h", "hm", "hms", "hmsv",
            "hmv", "j", "jm", "jms", "jmsv", "jmv", "ms",
        };
    }

    private static String[] horasP3() {
        return new String[] {
            "h 'Uhr' B", "h:mm B", "h:mm:ss B", "HH 'Uhr'", "HH:mm", "HH:mm:ss", "HH:mm:ss v",
            "HH:mm v", "h 'Uhr' a", "h:mm\u202fa", "h:mm:ss\u202fa", "h:mm:ss\u202fa v",
            "h:mm\u202fa v", "HH 'Uhr'", "HH:mm", "HH:mm:ss", "HH:mm:ss v", "HH:mm v", "mm:ss",
        };
    }

    private static String[] excK3() {
        return new String[] {
            "E+Bhm", "E+Bhms", "E+Hm", "E+Hms", "E+hm", "E+hms", "E+jm", "E+jms", "EEE+Bhm",
            "EEE+Bhms", "EEE+Hm", "EEE+Hms", "EEE+hm", "EEE+hms", "EEE+jm", "EEE+jms",
            "EEEE+Bhm", "EEEE+Bhms", "EEEE+Hm", "EEEE+Hms", "EEEE+hm", "EEEE+hms", "EEEE+jm",
            "EEEE+jms", "EEEEE+Bhm", "EEEEE+Bhms", "EEEEE+Hm", "EEEEE+Hms", "EEEEE+hm",
            "EEEEE+hms", "EEEEE+jm", "EEEEE+jms",
        };
    }

    private static String[] excP3() {
        return new String[] {
            "E h:mm B", "E h:mm:ss B", "E, HH:mm", "E, HH:mm:ss", "E h:mm\u202fa",
            "E, h:mm:ss\u202fa", "E, HH:mm", "E, HH:mm:ss", "E h:mm B", "E h:mm:ss B",
            "E, HH:mm", "E, HH:mm:ss", "E h:mm\u202fa", "E, h:mm:ss\u202fa", "E, HH:mm",
            "E, HH:mm:ss", "E h:mm B", "E h:mm:ss B", "E, HH:mm", "E, HH:mm:ss",
            "E h:mm\u202fa", "E, h:mm:ss\u202fa", "E, HH:mm", "E, HH:mm:ss", "E h:mm B",
            "E h:mm:ss B", "E, HH:mm", "E, HH:mm:ss", "E h:mm\u202fa", "E, h:mm:ss\u202fa",
            "E, HH:mm", "E, HH:mm:ss",
        };
    }

    // ---- fr-FR ----

    private static String[] fechasK4() {
        return new String[] {
            "E", "EEE", "EEEE", "EEEEE", "EEEEEd", "EEEEd", "EEEd", "Ed", "Gy", "GyM",
            "GyMEEEEEd", "GyMEEEEd", "GyMEEEd", "GyMEd", "GyMM", "GyMMEEEEEd", "GyMMEEEEd",
            "GyMMEEEd", "GyMMEd", "GyMMM", "GyMMMEEEEEd", "GyMMMEEEEd", "GyMMMEEEd", "GyMMMEd",
            "GyMMMM", "GyMMMMEEEEEd", "GyMMMMEEEEd", "GyMMMMEEEd", "GyMMMMEd", "GyMMMMM",
            "GyMMMMMEEEEEd", "GyMMMMMEEEEd", "GyMMMMMEEEd", "GyMMMMMEd", "GyMMMMMd", "GyMMMMd",
            "GyMMMd", "GyMMd", "GyMd", "M", "MEEEEEd", "MEEEEd", "MEEEd", "MEd", "MM",
            "MMEEEEEd", "MMEEEEd", "MMEEEd", "MMEd", "MMM", "MMMEEEEEd", "MMMEEEEd", "MMMEEEd",
            "MMMEd", "MMMM", "MMMMEEEEEd", "MMMMEEEEd", "MMMMEEEd", "MMMMEd", "MMMMM",
            "MMMMMEEEEEd", "MMMMMEEEEd", "MMMMMEEEd", "MMMMMEd", "MMMMMd", "MMMMd", "MMMd",
            "MMd", "Md", "d", "y", "yM", "yMEEEEEd", "yMEEEEd", "yMEEEd", "yMEd", "yMM",
            "yMMEEEEEd", "yMMEEEEd", "yMMEEEd", "yMMEd", "yMMM", "yMMMEEEEEd", "yMMMEEEEd",
            "yMMMEEEd", "yMMMEd", "yMMMM", "yMMMMEEEEEd", "yMMMMEEEEd", "yMMMMEEEd", "yMMMMEd",
            "yMMMMM", "yMMMMMEEEEEd", "yMMMMMEEEEd", "yMMMMMEEEd", "yMMMMMEd", "yMMMMMd",
            "yMMMMd", "yMMMd", "yMMd", "yMd", "yQQQ", "yQQQQ", "yw", "yyyy", "yyyyM",
            "yyyyMEEEEEd", "yyyyMEEEEd", "yyyyMEEEd", "yyyyMEd", "yyyyMM", "yyyyMMEEEEEd",
            "yyyyMMEEEEd", "yyyyMMEEEd", "yyyyMMEd", "yyyyMMM", "yyyyMMMEEEEEd", "yyyyMMMEEEEd",
            "yyyyMMMEEEd", "yyyyMMMEd", "yyyyMMMM", "yyyyMMMMEEEEEd", "yyyyMMMMEEEEd",
            "yyyyMMMMEEEd", "yyyyMMMMEd", "yyyyMMMMM", "yyyyMMMMMEEEEEd", "yyyyMMMMMEEEEd",
            "yyyyMMMMMEEEd", "yyyyMMMMMEd", "yyyyMMMMMd", "yyyyMMMMd", "yyyyMMMd", "yyyyMMd",
            "yyyyMd", "yyyyQQQ", "yyyyQQQQ",
        };
    }

    private static String[] fechasP4() {
        return new String[] {
            "E", "E", "E", "E", "E d", "E d", "E d", "E d", "y G", "MMM y G", "E d MMM y G",
            "E d MMM y G", "E d MMM y G", "E d MMM y G", "MMM y G", "E d MMM y G",
            "E d MMM y G", "E d MMM y G", "E d MMM y G", "MMM y G", "E d MMM y G",
            "E d MMM y G", "E d MMM y G", "E d MMM y G", "MMM y G", "E d MMM y G",
            "E d MMM y G", "E d MMM y G", "E d MMM y G", "MMM y G", "E d MMM y G",
            "E d MMM y G", "E d MMM y G", "E d MMM y G", "d MMM y G", "d MMM y G", "d MMM y G",
            "dd/MM/y GGGGG", "dd/MM/y GGGGG", "L", "E dd/MM", "E dd/MM", "E dd/MM", "E dd/MM",
            "L", "E dd/MM", "E dd/MM", "E dd/MM", "E dd/MM", "LLL", "E d MMM", "E d MMM",
            "E d MMM", "E d MMM", "LLL", "E d MMM", "E d MMM", "E d MMM", "E d MMM", "LLL",
            "E d MMM", "E d MMM", "E d MMM", "E d MMM", "d MMMM", "d MMMM", "d MMM", "dd/MM",
            "dd/MM", "d", "y G", "MM/y", "E dd/MM/y", "E dd/MM/y", "E dd/MM/y", "E dd/MM/y",
            "MM/y", "E dd/MM/y", "E dd/MM/y", "E dd/MM/y", "E dd/MM/y", "MMM y", "E d MMM y",
            "E d MMM y", "E d MMM y", "E d MMM y", "MMMM y", "E d MMM y", "E d MMM y",
            "E d MMM y", "E d MMM y", "MMMM y", "E d MMM y", "E d MMM y", "E d MMM y",
            "E d MMM y", "d MMM y", "d MMM y", "d MMM y", "dd/MM/y", "dd/MM/y", "QQQ y",
            "QQQQ y", "'semaine' w 'de' Y", "y G", "MM/y GGGGG", "E dd/MM/y GGGGG",
            "E dd/MM/y GGGGG", "E dd/MM/y GGGGG", "E dd/MM/y GGGGG", "MM/y GGGGG",
            "E dd/MM/y GGGGG", "E dd/MM/y GGGGG", "E dd/MM/y GGGGG", "E dd/MM/y GGGGG",
            "MMM y G", "E d MMM y G", "E d MMM y G", "E d MMM y G", "E d MMM y G", "MMMM y G",
            "E d MMM y G", "E d MMM y G", "E d MMM y G", "E d MMM y G", "MMMM y G",
            "E d MMM y G", "E d MMM y G", "E d MMM y G", "E d MMM y G", "d MMM y G",
            "d MMM y G", "d MMM y G", "dd/MM/y GGGGG", "dd/MM/y GGGGG", "QQQ y G", "QQQQ y G",
        };
    }

    private static String[] horasK4() {
        return new String[] {
            "Bh", "Bhm", "Bhms", "H", "Hm", "Hms", "Hmsv", "Hmv", "h", "hm", "hms", "hmsv",
            "hmv", "j", "jm", "jms", "jmsv", "jmv", "ms",
        };
    }

    private static String[] horasP4() {
        return new String[] {
            "h B", "h:mm B", "h:mm:ss B", "HH 'h'", "HH:mm", "HH:mm:ss", "HH:mm:ss v",
            "HH:mm v", "h\u202fa", "h:mm\u202fa", "h:mm:ss\u202fa", "h:mm:ss\u202fa v",
            "h:mm\u202fa v", "HH 'h'", "HH:mm", "HH:mm:ss", "HH:mm:ss v", "HH:mm v", "mm:ss",
        };
    }

    private static String[] excK4() {
        return new String[] {

        };
    }

    private static String[] excP4() {
        return new String[] {

        };
    }

    // ---- ja-JP ----

    private static String[] fechasK5() {
        return new String[] {
            "E", "EEE", "EEEE", "EEEEE", "EEEEEd", "EEEEd", "EEEd", "Ed", "Gy", "GyM",
            "GyMEEEEEd", "GyMEEEEd", "GyMEEEd", "GyMEd", "GyMM", "GyMMEEEEEd", "GyMMEEEEd",
            "GyMMEEEd", "GyMMEd", "GyMMM", "GyMMMEEEEEd", "GyMMMEEEEd", "GyMMMEEEd", "GyMMMEd",
            "GyMMMM", "GyMMMMEEEEEd", "GyMMMMEEEEd", "GyMMMMEEEd", "GyMMMMEd", "GyMMMMM",
            "GyMMMMMEEEEEd", "GyMMMMMEEEEd", "GyMMMMMEEEd", "GyMMMMMEd", "GyMMMMMd", "GyMMMMd",
            "GyMMMd", "GyMMd", "GyMd", "M", "MEEEEEd", "MEEEEd", "MEEEd", "MEd", "MM",
            "MMEEEEEd", "MMEEEEd", "MMEEEd", "MMEd", "MMM", "MMMEEEEEd", "MMMEEEEd", "MMMEEEd",
            "MMMEd", "MMMM", "MMMMEEEEEd", "MMMMEEEEd", "MMMMEEEd", "MMMMEd", "MMMMM",
            "MMMMMEEEEEd", "MMMMMEEEEd", "MMMMMEEEd", "MMMMMEd", "MMMMMd", "MMMMd", "MMMd",
            "MMd", "Md", "d", "y", "yM", "yMEEEEEd", "yMEEEEd", "yMEEEd", "yMEd", "yMM",
            "yMMEEEEEd", "yMMEEEEd", "yMMEEEd", "yMMEd", "yMMM", "yMMMEEEEEd", "yMMMEEEEd",
            "yMMMEEEd", "yMMMEd", "yMMMM", "yMMMMEEEEEd", "yMMMMEEEEd", "yMMMMEEEd", "yMMMMEd",
            "yMMMMM", "yMMMMMEEEEEd", "yMMMMMEEEEd", "yMMMMMEEEd", "yMMMMMEd", "yMMMMMd",
            "yMMMMd", "yMMMd", "yMMd", "yMd", "yQQQ", "yQQQQ", "yw", "yyyy", "yyyyM",
            "yyyyMEEEEEd", "yyyyMEEEEd", "yyyyMEEEd", "yyyyMEd", "yyyyMM", "yyyyMMEEEEEd",
            "yyyyMMEEEEd", "yyyyMMEEEd", "yyyyMMEd", "yyyyMMM", "yyyyMMMEEEEEd", "yyyyMMMEEEEd",
            "yyyyMMMEEEd", "yyyyMMMEd", "yyyyMMMM", "yyyyMMMMEEEEEd", "yyyyMMMMEEEEd",
            "yyyyMMMMEEEd", "yyyyMMMMEd", "yyyyMMMMM", "yyyyMMMMMEEEEEd", "yyyyMMMMMEEEEd",
            "yyyyMMMMMEEEd", "yyyyMMMMMEd", "yyyyMMMMMd", "yyyyMMMMd", "yyyyMMMd", "yyyyMMd",
            "yyyyMd", "yyyyQQQ", "yyyyQQQQ",
        };
    }

    private static String[] fechasP5() {
        return new String[] {
            "ccc", "ccc", "ccc", "ccc", "d\u65e5EEEE", "d\u65e5EEEE", "d\u65e5(E)",
            "d\u65e5(E)", "Gy\u5e74", "Gy\u5e74M\u6708", "Gy\u5e74M\u6708d\u65e5EEEE",
            "Gy\u5e74M\u6708d\u65e5EEEE", "Gy\u5e74M\u6708d\u65e5(E)",
            "Gy\u5e74M\u6708d\u65e5(E)", "Gy\u5e74M\u6708", "Gy\u5e74M\u6708d\u65e5EEEE",
            "Gy\u5e74M\u6708d\u65e5EEEE", "Gy\u5e74M\u6708d\u65e5(E)",
            "Gy\u5e74M\u6708d\u65e5(E)", "Gy\u5e74M\u6708", "Gy\u5e74M\u6708d\u65e5EEEE",
            "Gy\u5e74M\u6708d\u65e5EEEE", "Gy\u5e74M\u6708d\u65e5(E)",
            "Gy\u5e74M\u6708d\u65e5(E)", "Gy\u5e74M\u6708", "Gy\u5e74M\u6708d\u65e5EEEE",
            "Gy\u5e74M\u6708d\u65e5EEEE", "Gy\u5e74M\u6708d\u65e5(E)",
            "Gy\u5e74M\u6708d\u65e5(E)", "Gy\u5e74M\u6708", "Gy\u5e74M\u6708d\u65e5EEEE",
            "Gy\u5e74M\u6708d\u65e5EEEE", "Gy\u5e74M\u6708d\u65e5(E)",
            "Gy\u5e74M\u6708d\u65e5(E)", "Gy\u5e74M\u6708d\u65e5", "Gy\u5e74M\u6708d\u65e5",
            "Gy\u5e74M\u6708d\u65e5", "Gy/M/d", "Gy/M/d", "M\u6708", "M/dEEEE", "M/dEEEE",
            "M/d(E)", "M/d(E)", "M\u6708", "M/dEEEE", "M/dEEEE", "M/d(E)", "M/d(E)", "M\u6708",
            "M\u6708d\u65e5EEEE", "M\u6708d\u65e5EEEE", "M\u6708d\u65e5(E)",
            "M\u6708d\u65e5(E)", "M\u6708", "M\u6708d\u65e5EEEE", "M\u6708d\u65e5EEEE",
            "M\u6708d\u65e5(E)", "M\u6708d\u65e5(E)", "M\u6708", "M\u6708d\u65e5EEEE",
            "M\u6708d\u65e5EEEE", "M\u6708d\u65e5(E)", "M\u6708d\u65e5(E)", "M\u6708d\u65e5",
            "M\u6708d\u65e5", "M\u6708d\u65e5", "M/d", "M/d", "d\u65e5", "y\u5e74", "y/M",
            "y/M/dEEEE", "y/M/dEEEE", "y/M/d(E)", "y/M/d(E)", "y/MM", "y/M/dEEEE", "y/M/dEEEE",
            "y/M/d(E)", "y/M/d(E)", "y\u5e74M\u6708", "y\u5e74M\u6708d\u65e5EEEE",
            "y\u5e74M\u6708d\u65e5EEEE", "y\u5e74M\u6708d\u65e5(E)", "y\u5e74M\u6708d\u65e5(E)",
            "y\u5e74M\u6708", "y\u5e74M\u6708d\u65e5EEEE", "y\u5e74M\u6708d\u65e5EEEE",
            "y\u5e74M\u6708d\u65e5(E)", "y\u5e74M\u6708d\u65e5(E)", "y\u5e74M\u6708",
            "y\u5e74M\u6708d\u65e5EEEE", "y\u5e74M\u6708d\u65e5EEEE",
            "y\u5e74M\u6708d\u65e5(E)", "y\u5e74M\u6708d\u65e5(E)", "y\u5e74M\u6708d\u65e5",
            "y\u5e74M\u6708d\u65e5", "y\u5e74M\u6708d\u65e5", "y/M/d", "y/M/d", "y/QQQ",
            "y\u5e74QQQQ", "Y\u5e74\u7b2cw\u9031", "Gy\u5e74", "GGGGGy/M", "GGGGGy/M/d(EEEE)",
            "GGGGGy/M/d(EEEE)", "GGGGGy/M/d(E)", "GGGGGy/M/d(E)", "GGGGGy/M",
            "GGGGGy/M/d(EEEE)", "GGGGGy/M/d(EEEE)", "GGGGGy/M/d(E)", "GGGGGy/M/d(E)",
            "Gy\u5e74M\u6708", "Gy\u5e74M\u6708d\u65e5(EEEE)", "Gy\u5e74M\u6708d\u65e5(EEEE)",
            "Gy\u5e74M\u6708d\u65e5(E)", "Gy\u5e74M\u6708d\u65e5(E)", "Gy\u5e74M\u6708",
            "Gy\u5e74M\u6708d\u65e5(EEEE)", "Gy\u5e74M\u6708d\u65e5(EEEE)",
            "Gy\u5e74M\u6708d\u65e5(E)", "Gy\u5e74M\u6708d\u65e5(E)", "Gy\u5e74M\u6708",
            "Gy\u5e74M\u6708d\u65e5(EEEE)", "Gy\u5e74M\u6708d\u65e5(EEEE)",
            "Gy\u5e74M\u6708d\u65e5(E)", "Gy\u5e74M\u6708d\u65e5(E)", "Gy\u5e74M\u6708d\u65e5",
            "Gy\u5e74M\u6708d\u65e5", "Gy\u5e74M\u6708d\u65e5", "GGGGGy/M/d", "GGGGGy/M/d",
            "Gy/QQQ", "Gy\u5e74QQQQ",
        };
    }

    private static String[] horasK5() {
        return new String[] {
            "Bh", "Bhm", "Bhms", "H", "Hm", "Hms", "Hmsv", "Hmv", "h", "hm", "hms", "hmsv",
            "hmv", "j", "jm", "jms", "jmsv", "jmv", "ms",
        };
    }

    private static String[] horasP5() {
        return new String[] {
            "BK\u6642", "BK:mm", "BK:mm:ss", "H\u6642", "H:mm", "H:mm:ss", "H:mm:ss v",
            "H:mm v", "aK\u6642", "aK:mm", "aK:mm:ss", "aK:mm:ss v", "aK:mm v", "H\u6642",
            "H:mm", "H:mm:ss", "H:mm:ss v", "H:mm v", "mm:ss",
        };
    }

    private static String[] excK5() {
        return new String[] {
            "E+Bhm", "E+Bhms", "E+Hm", "E+Hms", "E+hm", "E+hms", "E+jm", "E+jms", "EEE+Bhm",
            "EEE+Bhms", "EEE+Hm", "EEE+Hms", "EEE+hm", "EEE+hms", "EEE+jm", "EEE+jms",
            "EEEE+Bhm", "EEEE+Bhms", "EEEE+Hm", "EEEE+Hms", "EEEE+hm", "EEEE+hms", "EEEE+jm",
            "EEEE+jms", "EEEEE+Bhm", "EEEEE+Bhms", "EEEEE+Hm", "EEEEE+Hms", "EEEEE+hm",
            "EEEEE+hms", "EEEEE+jm", "EEEEE+jms",
        };
    }

    private static String[] excP5() {
        return new String[] {
            "BK:mm (E)", "BK:mm:ss (E)", "H:mm (E)", "H:mm:ss (E)", "aK:mm (E)", "aK:mm:ss (E)",
            "H:mm (E)", "H:mm:ss (E)", "BK:mm (E)", "BK:mm:ss (E)", "H:mm (E)", "H:mm:ss (E)",
            "aK:mm (E)", "aK:mm:ss (E)", "H:mm (E)", "H:mm:ss (E)", "BK:mm (E)", "BK:mm:ss (E)",
            "H:mm (E)", "H:mm:ss (E)", "aK:mm (E)", "aK:mm:ss (E)", "H:mm (E)", "H:mm:ss (E)",
            "BK:mm (E)", "BK:mm:ss (E)", "H:mm (E)", "H:mm:ss (E)", "aK:mm (E)", "aK:mm:ss (E)",
            "H:mm (E)", "H:mm:ss (E)",
        };
    }

    private static String[] fechasK(int i) {
        if (i == 0) {
            return fechasK0();
        }
        if (i == 1) {
            return fechasK1();
        }
        if (i == 2) {
            return fechasK2();
        }
        if (i == 3) {
            return fechasK3();
        }
        if (i == 4) {
            return fechasK4();
        }
        return fechasK5();
    }

    private static String[] fechasP(int i) {
        if (i == 0) {
            return fechasP0();
        }
        if (i == 1) {
            return fechasP1();
        }
        if (i == 2) {
            return fechasP2();
        }
        if (i == 3) {
            return fechasP3();
        }
        if (i == 4) {
            return fechasP4();
        }
        return fechasP5();
    }

    private static String[] horasK(int i) {
        if (i == 0) {
            return horasK0();
        }
        if (i == 1) {
            return horasK1();
        }
        if (i == 2) {
            return horasK2();
        }
        if (i == 3) {
            return horasK3();
        }
        if (i == 4) {
            return horasK4();
        }
        return horasK5();
    }

    private static String[] horasP(int i) {
        if (i == 0) {
            return horasP0();
        }
        if (i == 1) {
            return horasP1();
        }
        if (i == 2) {
            return horasP2();
        }
        if (i == 3) {
            return horasP3();
        }
        if (i == 4) {
            return horasP4();
        }
        return horasP5();
    }

    private static String[] excK(int i) {
        if (i == 0) {
            return excK0();
        }
        if (i == 1) {
            return excK1();
        }
        if (i == 2) {
            return excK2();
        }
        if (i == 3) {
            return excK3();
        }
        if (i == 4) {
            return excK4();
        }
        return excK5();
    }

    private static String[] excP(int i) {
        if (i == 0) {
            return excP0();
        }
        if (i == 1) {
            return excP1();
        }
        if (i == 2) {
            return excP2();
        }
        if (i == 3) {
            return excP3();
        }
        if (i == 4) {
            return excP4();
        }
        return excP5();
    }

    /** Las etiquetas de las seis filas, en el orden en que estan. */
    private static final String[] ETIQUETAS = {"und", "en-US", "es-AR", "de-DE", "fr-FR", "ja-JP"};

    /** Que fila le toca a ese locale: etiqueta exacta, si no el idioma solo, si no la fila cero. */
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

    private static String buscar(String[] claves, String[] patrones, String clave) {
        for (int i = 0; i < claves.length; i++) {
            if (claves[i].equals(clave)) {
                return patrones[i];
            }
        }
        return null;
    }

    /**
     * El patron que ese idioma usa para esa plantilla.
     *
     * @param plantilla la plantilla, ya comprobada por {@link Plantilla#comprobar}
     * @param locale en que idioma
     * @return el patron, o {@code null} si ese idioma no tiene ninguno para esa plantilla
     */
    static String patron(String plantilla, Locale locale) {
        int i = indice(locale);
        int corte = Plantilla.corte(plantilla);
        String fecha = plantilla.substring(0, corte);
        String hora = plantilla.substring(corte);
        if (hora.isEmpty()) {
            return buscar(fechasK(i), fechasP(i), fecha);
        }
        if (fecha.isEmpty()) {
            return buscar(horasK(i), horasP(i), hora);
        }
        String excepcion = buscar(excK(i), excP(i), fecha + "+" + hora);
        if (excepcion != null) {
            return excepcion;
        }
        String pf = buscar(fechasK(i), fechasP(i), fecha);
        String ph = buscar(horasK(i), horasP(i), hora);
        if (pf == null || ph == null) {
            return null;
        }
        return pf + PEGAMENTO[i] + ph;
    }
}
