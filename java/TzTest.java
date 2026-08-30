import java.util.TimeZone;

// Comportamiento de TimeZone en el subconjunto donde declaramos paridad con el JDK: ids GMT/UTC,
// offsets custom "GMT+HH:MM" y sus variantes, ids no reconocidos, y hasSameRules.
//
// NO se prueba tzdb (region ids) porque ahi el subconjunto es explicito y divergente: el JDK
// conoce Europe/Paris y nosotros devolvemos GMT, que es lo que el propio JDK hace con un id que
// no reconoce. Probar eso seria probar la diferencia, no la paridad.
public class TzTest {

    static int min(String id) {
        return TimeZone.getTimeZone(id).getRawOffset() / 60000;
    }

    static int idEs(String id, String esperado) {
        return TimeZone.getTimeZone(id).getID().equals(esperado) ? 1 : 0;
    }

    public static int run() {
        int r = 0;

        // offsets: GMT, y las cuatro formas custom
        r = r + min("GMT");                       // 0
        r = r + min("GMT+05:30");                 // 330
        r = r + min("GMT-08:00");                 // -480
        r = r + min("GMT+5");                     // 300
        r = r + min("GMT+0530");                  // 330

        // ids: no reconocido -> GMT; fuera de rango -> GMT; custom -> normalizado
        r = r + idEs("Zzz", "GMT") * 10000;
        r = r + idEs("GMT+24:00", "GMT") * 100000;
        r = r + idEs("GMT+5", "GMT+05:00") * 1000000;
        r = r + idEs("GMT-08:00", "GMT-08:00") * 10000000;

        // un zona de offset fijo no observa DST
        TimeZone z = TimeZone.getTimeZone("GMT+05:30");
        r = r + z.getDSTSavings();
        r = r + (z.useDaylightTime() ? 7777 : 0);
        r = r + z.getOffset(0L) / 60000;          // 330, sin DST

        // hasSameRules ignora el id
        r = r + (TimeZone.getTimeZone("GMT").hasSameRules(TimeZone.getTimeZone("UTC")) ? 1 : 0);
        r = r + (TimeZone.getTimeZone("GMT").hasSameRules(TimeZone.getTimeZone("GMT+05:30")) ? 7777 : 0);

        // las constantes de estilo
        r = r + TimeZone.SHORT + TimeZone.LONG;

        // clone independiente: mutar la copia no toca el original
        TimeZone a = TimeZone.getTimeZone("GMT");
        TimeZone b = (TimeZone) a.clone();
        b.setRawOffset(3600000);
        r = r + (a.getRawOffset() == 0 ? 100000000 : 0);
        r = r + b.getRawOffset() / 60000;         // 60

        return r;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
