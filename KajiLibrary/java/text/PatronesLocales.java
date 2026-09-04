package java.text;

import java.util.Locale;

/**
 * Los patrones —no las palabras— con que cada locale escribe números y fechas.
 *
 * <p>Es el tercer archivo de datos del paquete, y el que faltaba. {@link DecimalFormatSymbols} dice
 * con qué caracteres se dibuja un número, {@link DateFormatSymbols} dice cómo se llaman los meses, y
 * acá está lo que ninguno de los dos guarda: el <em>orden</em>. Que Alemania escriba
 * {@code 1.234,50 EUR} y Estados Unidos {@code $1,234.50} no es cuestión de símbolos —los dos usan
 * los mismos cuatro— sino de dónde va cada cosa, y eso vive en el patrón.
 *
 * <p>No es pública: el JDK tampoco expone esta tabla. Se llega a ella por
 * {@code NumberFormat.getCurrencyInstance(locale)} y {@code DateFormat.getDateInstance(style,
 * locale)}, que es la forma en que un llamador la pide.
 *
 * <p>Las filas son las mismas seis de {@code DecimalFormatSymbols}, en el mismo orden, y se
 * resuelven con su mismo {@code indexOf} — un locale desconocido cae en ROOT, que es lo que hace el
 * JDK con un locale del que no tiene datos. Todos los patrones se extrajeron corriendo el JDK 25
 * (misma metodología que las otras dos tablas, no transcripción), y cada carácter no-ASCII está
 * escrito como {@code \\uXXXX}: varios de ellos son espacios que no se ven — el francés separa el
 * porcentaje con {@code U+00A0} y el inglés separa el a.m./p.m. con {@code U+202F}. Un espacio
 * común en su lugar no es un detalle cosmético: cambia el resultado de un {@code equals}.
 */
final class PatronesLocales {

    private PatronesLocales() {
    }

    static final int FULL = 0;
    static final int LONG = 1;
    static final int MEDIUM = 2;
    static final int SHORT = 3;

    // El patrón decimal y el entero son iguales en las seis filas. Se dejan como tabla igual, y no
    // como constante, porque la fila es la unidad de esta clase: colapsarlos escondería que son
    // datos de locale y no una regla universal.
    private static String[] numero() {
        return new String[] {"#,##0.###", "#,##0.###", "#,##0.###", "#,##0.###", "#,##0.###", "#,##0.###"};
    }

    private static String[] entero() {
        return new String[] {"#,##0", "#,##0", "#,##0", "#,##0", "#,##0", "#,##0"};
    }

    // Lo que distingue a estas filas es de qué lado va el signo de moneda (¤) y qué espacio
    // duro lo separa. El yen no lleva decimales: lo dice el patrón, no un caso especial en el
    // código.
    private static String[] moneda() {
        return new String[] {
            "\u00a4\u00a0#,##0.00",
            "\u00a4#,##0.00",
            "\u00a4\u00a0#,##0.00",
            "#,##0.00\u00a0\u00a4",
            "#,##0.00\u00a0\u00a4",
            "\u00a4#,##0",
        };
    }

    private static String[] porciento() {
        return new String[] {
            "#,##0%", "#,##0%", "#,##0%", "#,##0\u00a0%", "#,##0\u00a0%", "#,##0%",
        };
    }

    private static String[] fechaRow(int i) {
        if (i == 1) {
            return new String[] {"EEEE, MMMM d, y", "MMMM d, y", "MMM d, y", "M/d/yy"};
        }
        if (i == 2) {
            return new String[] {"EEEE, d 'de' MMMM 'de' y", "d 'de' MMMM 'de' y", "d MMM y", "d/M/yy"};
        }
        if (i == 3) {
            return new String[] {"EEEE, d. MMMM y", "d. MMMM y", "dd.MM.y", "dd.MM.yy"};
        }
        if (i == 4) {
            return new String[] {"EEEE d MMMM y", "d MMMM y", "d MMM y", "dd/MM/y"};
        }
        if (i == 5) {
            return new String[] {
                "y\u5e74M\u6708d\u65e5EEEE", "y\u5e74M\u6708d\u65e5", "y/MM/dd", "y/MM/dd",
            };
        }
        return new String[] {"y MMMM d, EEEE", "y MMMM d", "y MMM d", "y-MM-dd"};
    }

    private static String[] horaRow(int i) {
        if (i == 1 || i == 2) {
            // Reloj de 12 con U+202F (NARROW NO-BREAK SPACE) antes del a.m./p.m., no un espacio
            // común: quien compare el resultado contra un espacio ASCII no lo va a encontrar.
            return new String[] {
                "h:mm:ss\u202fa zzzz", "h:mm:ss\u202fa z", "h:mm:ss\u202fa", "h:mm\u202fa",
            };
        }
        if (i == 5) {
            return new String[] {
                "H\u6642mm\u5206ss\u79d2 zzzz", "H:mm:ss z", "H:mm:ss", "H:mm",
            };
        }
        return new String[] {"HH:mm:ss zzzz", "HH:mm:ss z", "HH:mm:ss", "HH:mm"};
    }

    // Lo que va entre la fecha y la hora al combinarlas. Es por locale Y por estilo: el francés usa
    // coma en los tres estilos largos y sólo espacio en el corto.
    private static String[] pegamentoRow(int i) {
        if (i == 1 || i == 2 || i == 3) {
            return new String[] {", ", ", ", ", ", ", "};
        }
        if (i == 4) {
            return new String[] {", ", ", ", ", ", " "};
        }
        return new String[] {" ", " ", " ", " "};
    }

    static String numero(Locale l) {
        return PatronesLocales.numero()[DecimalFormatSymbols.indexOf(l)];
    }

    static String entero(Locale l) {
        return PatronesLocales.entero()[DecimalFormatSymbols.indexOf(l)];
    }

    static String moneda(Locale l) {
        return PatronesLocales.moneda()[DecimalFormatSymbols.indexOf(l)];
    }

    static String porciento(Locale l) {
        return PatronesLocales.porciento()[DecimalFormatSymbols.indexOf(l)];
    }

    static String fecha(int estilo, Locale l) {
        return PatronesLocales.fechaRow(DecimalFormatSymbols.indexOf(l))[estilo];
    }

    static String hora(int estilo, Locale l) {
        return PatronesLocales.horaRow(DecimalFormatSymbols.indexOf(l))[estilo];
    }

    /**
     * Combina fecha y hora eligiendo el separador por el MAYOR de los dos estilos (recordando que
     * FULL es 0 y SHORT es 3, así que "el mayor" es el más corto).
     *
     * <p>La regla no es una invención: se verificó contra las dieciséis combinaciones de estilo en
     * los seis locales y reproduce exactamente lo que devuelve el JDK 25. Importa sólo donde el
     * pegamento varía por estilo, que de estas filas es el francés.
     */
    static String fechaHora(int estiloFecha, int estiloHora, Locale l) {
        int i = DecimalFormatSymbols.indexOf(l);
        int mayor = estiloFecha;
        if (estiloHora > mayor) {
            mayor = estiloHora;
        }
        return PatronesLocales.fechaRow(i)[estiloFecha]
                + PatronesLocales.pegamentoRow(i)[mayor]
                + PatronesLocales.horaRow(i)[estiloHora];
    }
}
