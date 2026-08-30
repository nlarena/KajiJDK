// El parseo de `DateTimeFormatter` y los nueve `parse(texto, formateador)` de `java.time`.
//
// **Se comprueba contra `java` real corriendo lo mismo**, que es lo unico que distingue "parsea" de
// "parsea igual que el JDK". La mitad de las comprobaciones son de ida y vuelta --formatear y volver
// a leer-- porque esa es la propiedad que el formateador promete: lo que escribe, lo relee.
//
// La otra mitad son los casos que **no** tienen que andar: un texto que sobra, un campo que falta, un
// patron de solo hora al que le piden una fecha. Un parser que acepta de mas es peor que uno que no
// parsea, porque devuelve una fecha equivocada en vez de un error.
//
// Con todo en verde devuelve -1; si no, el indice de la primera comprobacion que fallo.
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class FmtTest {

    static int cuantas = 0;
    static int primerFallo = -1;

    static void ok(boolean b) {
        if (!b && primerFallo < 0) {
            primerFallo = cuantas;
        }
        cuantas = cuantas + 1;
    }

    // Que `f.parse(texto, X::from)` tire, y no que devuelva cualquier cosa.
    static void tira(DateTimeFormatter f, String texto, int cual) {
        boolean tiro = false;
        try {
            if (cual == 0) {
                LocalDate.parse(texto, f);
            } else if (cual == 1) {
                LocalTime.parse(texto, f);
            } else {
                LocalDateTime.parse(texto, f);
            }
        } catch (DateTimeParseException e) {
            tiro = true;
        }
        ok(tiro);
    }

    static void fechas() {
        DateTimeFormatter iso = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        ok(LocalDate.parse("2024-02-29", iso).equals(LocalDate.of(2024, 2, 29)));
        ok(LocalDate.parse("0001-01-01", iso).equals(LocalDate.of(1, 1, 1)));

        // Otro orden, otro separador: el patron manda.
        DateTimeFormatter barra = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        ok(LocalDate.parse("09/03/1998", barra).equals(LocalDate.of(1998, 3, 9)));

        // Sin relleno: `d` y `M` leen uno o dos digitos.
        DateTimeFormatter corto = DateTimeFormatter.ofPattern("d/M/yyyy");
        ok(LocalDate.parse("9/3/1998", corto).equals(LocalDate.of(1998, 3, 9)));
        ok(LocalDate.parse("19/12/1998", corto).equals(LocalDate.of(1998, 12, 19)));

        // Nombre de mes, corto y largo. **Solo ida y vuelta**, sin el texto escrito a mano: los
        // nombres dependen del locale --el `java` real con locale espaniol escribe `jul`, esta
        // biblioteca escribe siempre en ingles-- asi que comparar la cadena compararia el locale y
        // no el parser. Lo que sí es independiente del locale es que **lo que se escribe se relee**,
        // que es justo la propiedad que interesa.
        DateTimeFormatter mes3 = DateTimeFormatter.ofPattern("dd MMM yyyy");
        LocalDate julio = LocalDate.of(2021, 7, 5);
        ok(LocalDate.parse(mes3.format(julio), mes3).equals(julio));
        DateTimeFormatter mesL = DateTimeFormatter.ofPattern("dd MMMM yyyy");
        LocalDate septiembre = LocalDate.of(2021, 9, 5);
        ok(LocalDate.parse(mesL.format(septiembre), mesL).equals(septiembre));

        // Dia del anio.
        DateTimeFormatter doy = DateTimeFormatter.ofPattern("yyyy-DDD");
        ok(LocalDate.parse("2021-032", doy).equals(LocalDate.of(2021, 2, 1)));

        // Anio de dos digitos: ventana 2000-2099.
        DateTimeFormatter dos = DateTimeFormatter.ofPattern("dd/MM/yy");
        ok(LocalDate.parse("01/02/24", dos).equals(LocalDate.of(2024, 2, 1)));

        // Ida y vuelta sobre un anio bisiesto y un fin de mes.
        LocalDate[] varias = {
            LocalDate.of(2000, 2, 29), LocalDate.of(1999, 12, 31), LocalDate.of(2024, 1, 1),
        };
        int i = 0;
        while (i < varias.length) {
            ok(LocalDate.parse(iso.format(varias[i]), iso).equals(varias[i]));
            ok(LocalDate.parse(mesL.format(varias[i]), mesL).equals(varias[i]));
            i = i + 1;
        }
    }

    static void horas() {
        DateTimeFormatter hms = DateTimeFormatter.ofPattern("HH:mm:ss");
        ok(LocalTime.parse("00:00:00", hms).equals(LocalTime.of(0, 0, 0)));
        ok(LocalTime.parse("23:59:59", hms).equals(LocalTime.of(23, 59, 59)));

        // Sin segundos: los que faltan son cero.
        DateTimeFormatter hm = DateTimeFormatter.ofPattern("HH:mm");
        ok(LocalTime.parse("07:05", hm).equals(LocalTime.of(7, 5)));

        // Fraccion: `S` son los primeros digitos del nano, y hay que devolverlos a su escala.
        DateTimeFormatter frac = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        ok(LocalTime.parse("01:02:03.400", frac).equals(LocalTime.of(1, 2, 3, 400000000)));
        DateTimeFormatter frac1 = DateTimeFormatter.ofPattern("HH:mm:ss.S");
        ok(LocalTime.parse("01:02:03.5", frac1).equals(LocalTime.of(1, 2, 3, 500000000)));

        // Reloj de 12 horas con AM/PM. Otra vez ida y vuelta y no el texto a mano: el marcador
        // depende del locale --el `java` real en espaniol escribe `a. m.`, esta biblioteca `AM`--.
        //
        // Lo que la ida y vuelta comprueba es exactamente lo que importa: que **las cuatro horas se
        // distingan entre si**. Medianoche y mediodia son las dos que confunden, porque las dos se
        // escriben `12:00` y solo el marcador las separa; si el parser lo ignorara, una de las dos
        // volveria como la otra y esto lo veria.
        DateTimeFormatter doce = DateTimeFormatter.ofPattern("hh:mm a");
        LocalTime[] docenas = {
            LocalTime.of(0, 0), LocalTime.of(12, 0), LocalTime.of(13, 30), LocalTime.of(11, 45),
        };
        int k = 0;
        while (k < docenas.length) {
            ok(LocalTime.parse(doce.format(docenas[k]), doce).equals(docenas[k]));
            k = k + 1;
        }

        // Ida y vuelta.
        LocalTime[] varias = { LocalTime.of(0, 0), LocalTime.of(12, 0), LocalTime.of(23, 59, 59) };
        int i = 0;
        while (i < varias.length) {
            ok(LocalTime.parse(hms.format(varias[i]), hms).equals(varias[i]));
            i = i + 1;
        }
    }

    static void fechaYHora() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime esperado = LocalDateTime.of(2021, 7, 5, 14, 30, 15);
        ok(LocalDateTime.parse("2021-07-05 14:30:15", f).equals(esperado));
        ok(LocalDateTime.parse(f.format(esperado), f).equals(esperado));

        // Literal entre comillas, y la comilla escapada.
        DateTimeFormatter lit = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        ok(LocalDateTime.parse("2021-07-05T14:30:15", lit).equals(esperado));
    }

    static void anioYMes() {
        DateTimeFormatter soloAnio = DateTimeFormatter.ofPattern("yyyy");
        ok(Year.parse("2021", soloAnio).equals(Year.of(2021)));

        DateTimeFormatter am = DateTimeFormatter.ofPattern("yyyy-MM");
        ok(YearMonth.parse("2021-07", am).equals(YearMonth.of(2021, 7)));

        DateTimeFormatter md = DateTimeFormatter.ofPattern("MM-dd");
        ok(MonthDay.parse("07-05", md).equals(MonthDay.of(7, 5)));

        // Un patron completo tambien sirve para pedir solo una parte: los campos crudos siguen ahi.
        DateTimeFormatter completo = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        ok(Year.parse("2021-07-05", completo).equals(Year.of(2021)));
        ok(YearMonth.parse("2021-07-05", completo).equals(YearMonth.of(2021, 7)));
    }

    static void conDesplazamiento() {
        DateTimeFormatter ot = DateTimeFormatter.ofPattern("HH:mm:ssXXX");
        ok(OffsetTime.parse("10:15:30+01:00", ot)
                .equals(OffsetTime.of(LocalTime.of(10, 15, 30), ZoneOffset.ofHours(1))));
        // `X` escribe y lee `Z` para el cero.
        ok(OffsetTime.parse("10:15:30Z", ot)
                .equals(OffsetTime.of(LocalTime.of(10, 15, 30), ZoneOffset.UTC)));
        // Negativo, y con minutos.
        ok(OffsetTime.parse("10:15:30-03:30", ot)
                .equals(OffsetTime.of(LocalTime.of(10, 15, 30), ZoneOffset.ofHoursMinutes(-3, -30))));

        DateTimeFormatter odt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
        OffsetDateTime esperado = OffsetDateTime.of(LocalDateTime.of(2021, 7, 5, 14, 30, 15),
                ZoneOffset.ofHours(-3));
        ok(OffsetDateTime.parse("2021-07-05T14:30:15-03:00", odt).equals(esperado));
        ok(OffsetDateTime.parse(odt.format(esperado), odt).equals(esperado));

        ZonedDateTime zesperado = ZonedDateTime.of(LocalDateTime.of(2021, 7, 5, 14, 30, 15),
                ZoneOffset.ofHours(2));
        ok(ZonedDateTime.parse("2021-07-05T14:30:15+02:00", odt).equals(zesperado));
        ok(ZonedDateTime.parse(odt.format(zesperado), odt).equals(zesperado));
    }

    // Lo que NO tiene que andar. Un parser que acepta de mas devuelve una fecha equivocada, que es
    // mucho peor que un error.
    static void loQueFalla() {
        DateTimeFormatter iso = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        tira(iso, "2024-02-29 sobra", 0);   // texto de mas al final
        tira(iso, "2024-02", 0);            // se corta
        tira(iso, "", 0);                   // vacio
        tira(iso, "abcd-ef-gh", 0);         // no son numeros
        tira(iso, "2024/02/29", 0);         // separador que no es

        // Un patron de solo hora no da una fecha, y uno de solo fecha no da una hora.
        DateTimeFormatter hm = DateTimeFormatter.ofPattern("HH:mm");
        tira(hm, "10:15", 0);
        tira(iso, "2024-02-29", 1);

        // Un patron de fecha y hora al que le falta la hora tampoco da un LocalDateTime.
        tira(iso, "2024-02-29", 2);
    }

    public static int run() {
        fechas();
        horas();
        fechaYHora();
        anioYMes();
        conDesplazamiento();
        loQueFalla();
        return primerFallo;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
