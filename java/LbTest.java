import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.Chronology;
import java.time.chrono.ThaiBuddhistChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DecimalStyle;
import java.util.Locale;

/**
 * `localizedBy` contra `withLocale`, que es donde esta toda la diferencia.
 *
 * <p>La comprobacion que importa no es que `localizedBy` ponga el locale --eso lo hace tambien
 * `withLocale`-- sino **que sobreescriba** la cronologia puesta a mano y **que NO** toque la zona.
 * Esos dos comportamientos son opuestos entre si y se midieron contra el JDK real; una version que
 * limpiara la zona o que conservara la cronologia pasaria cualquier prueba mas floja que esta.
 *
 * <p>El mismo archivo compila contra el JDK 25 y da el mismo entero.
 */
public class LbTest {

    static int fallas = 0;

    static void igual(String que, Object esperado, Object dio) {
        if (esperado == null ? dio != null : !esperado.equals(dio)) {
            System.out.println("FALLA " + que + ": esperaba " + esperado + " y dio " + dio);
            fallas = fallas + 1;
        }
    }

    public static int run() {
        fallas = 0;
        DateTimeFormatter base = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // Un desplazamiento fijo y no una zona por region: `ZoneOffset` es un `ZoneId` y no
        // necesita tzdb, que esta biblioteca no trae. Lo que se prueba es si `localizedBy`
        // conserva la zona, y para eso cualquier zona sirve.
        ZoneId ba = ZoneOffset.ofHours(-3);
        DateTimeFormatter con = base.withChronology(ThaiBuddhistChronology.INSTANCE).withZone(ba);

        // withLocale conserva lo puesto a mano.
        DateTimeFormatter w = con.withLocale(Locale.FRANCE);
        igual("withLocale locale", Locale.FRANCE, w.getLocale());
        igual("withLocale conserva la cronologia", ThaiBuddhistChronology.INSTANCE, w.getChronology());
        igual("withLocale conserva la zona", ba, w.getZone());

        // localizedBy la sobreescribe -- y deja ISO, no null.
        DateTimeFormatter l = con.localizedBy(Locale.FRANCE);
        igual("localizedBy locale", Locale.FRANCE, l.getLocale());
        igual("localizedBy pisa la cronologia", Chronology.ofLocale(Locale.FRANCE), l.getChronology());
        if (l.getChronology() == null) {
            System.out.println("FALLA localizedBy dejo la cronologia en null");
            fallas = fallas + 1;
        }
        // Y la zona NO se limpia: es la asimetria que hay que fijar.
        igual("localizedBy conserva la zona", ba, l.getZone());

        // Sobre un formateador sin zona, sigue sin zona.
        igual("sin zona sigue sin zona", null, base.localizedBy(Locale.FRANCE).getZone());

        // Los simbolos salen del locale.
        igual("simbolos del locale", DecimalStyle.of(Locale.FRANCE),
                base.localizedBy(Locale.FRANCE).getDecimalStyle());

        // El resultado sigue formateando igual: el patron no tiene nombres.
        igual("formatea igual", base.format(java.time.LocalDate.of(2023, 7, 14)),
                base.localizedBy(Locale.FRANCE).format(java.time.LocalDate.of(2023, 7, 14)));

        // Nulo -> NPE.
        boolean tiro = false;
        try {
            base.localizedBy(null);
        } catch (NullPointerException e) {
            tiro = true;
        }
        if (!tiro) {
            System.out.println("FALLA localizedBy(null) no tiro NPE");
            fallas = fallas + 1;
        }

        if (fallas == 0) {
            return -1;
        }
        return fallas;
    }

    public static void main(String[] a) {
        System.out.println("LbTest " + LbTest.run());
    }
}
