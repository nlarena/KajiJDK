// `javax.xml.datatype.Duration` comprobado contra `java` real.
//
// La prueba se corre con las dos VMs: en la nuestra usa la implementacion de KajiLibrary y en
// `H:/jdk-25.0.2/bin/java.exe -cp java` usa la de Xerces del modulo `java.xml`, porque el paquete es
// de la plataforma y el classpath no lo puede tapar. El mismo codigo mide las dos.
//
// Lo que se cuida:
//
// **INDETERMINATE.** `P1M` contra `P30D` no da ni menor ni mayor ni igual: da 2, y eso es la
// respuesta y no una falla. Un mes dura 28, 29, 30 o 31 dias, asi que en febrero `P1M` es mas corta
// y en marzo es mas larga. Las comprobaciones 20 a 24, que ademas verifican la consecuencia que se
// cobra sola: `isLongerThan` y `isShorterThan` dan **las dos** false, y `equals` da false.
//
// **`equals` no es campo a campo.** Esta definido como `compare(otra) == EQUAL`, asi que `PT60S` y
// `PT1M` son iguales aunque no compartan un solo campo. Comprobacion 25.
//
// **Un campo ausente no es un campo en cero.** `P1Y` tiene cinco campos sin poner, y eso es lo que
// decide de que tipo de XML Schema es la duracion. Comprobaciones 8 a 14.
//
// **Las duraciones desde milisegundos cuentan sobre el calendario.** 31 dias dan `P0Y1M0D` y no
// `P1M1D`, porque enero tiene 31; 59 dan dos meses justos porque enero y febrero suman 59. Es la
// unica forma correcta de repartir milisegundos en campos de largo variable, y una implementacion
// que dividiera por 30 fallaria aca. Comprobaciones 30 a 34.
//
// Con todo en verde devuelve -1; si no, el indice de la primera comprobacion que fallo.
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

public class XmlDatatypeDurTest {

    static int cuantas = 0;

    static int chequear(boolean condicion) {
        cuantas++;
        return condicion ? 0 : cuantas;
    }

    public static int run() {
        int r;
        DatatypeFactory f;
        try {
            f = DatatypeFactory.newInstance();
        } catch (Exception e) {
            return 0;
        }

        // --- Las constantes ------------------------------------------------------------------
        if ((r = chequear(DatatypeConstants.LESSER == -1 && DatatypeConstants.EQUAL == 0
                && DatatypeConstants.GREATER == 1
                && DatatypeConstants.INDETERMINATE == 2)) != 0) return r;               // 1
        if ((r = chequear(DatatypeConstants.FIELD_UNDEFINED == Integer.MIN_VALUE)) != 0) return r; // 2
        if ((r = chequear(DatatypeConstants.JANUARY == 1
                && DatatypeConstants.DECEMBER == 12)) != 0) return r;                   // 3
        // Los nombres estan al reves de los numeros, y asi hay que replicarlos.
        if ((r = chequear(DatatypeConstants.MAX_TIMEZONE_OFFSET == -840
                && DatatypeConstants.MIN_TIMEZONE_OFFSET == 840)) != 0) return r;       // 4
        if ((r = chequear(DatatypeConstants.YEARS.getId() == 0
                && DatatypeConstants.SECONDS.getId() == 5)) != 0) return r;             // 5
        if ((r = chequear("YEARS".equals(DatatypeConstants.YEARS.toString()))) != 0) return r; // 6
        if ((r = chequear("{http://www.w3.org/2001/XMLSchema}duration"
                .equals(DatatypeConstants.DURATION.toString()))) != 0) return r;        // 7

        // --- Campos puestos y ausentes ---------------------------------------------------------
        Duration unMes = f.newDuration("P1M");
        if ((r = chequear(unMes.isSet(DatatypeConstants.MONTHS))) != 0) return r;       // 8
        if ((r = chequear(!unMes.isSet(DatatypeConstants.DAYS))) != 0) return r;        // 9
        // La implementacion de referencia contesta cero y no FIELD_UNDEFINED para un campo
        // ausente, apartandose de lo que documenta la clase abstracta. Quien necesite distinguir
        // "ausente" de "cero" tiene `isSet` y `getField`, que si lo dicen; ver la comprobacion 12.
        if ((r = chequear(unMes.getDays() == 0)) != 0) return r;                        // 10
        if ((r = chequear(unMes.getMonths() == 1)) != 0) return r;                      // 11
        if ((r = chequear(unMes.getField(DatatypeConstants.DAYS) == null)) != 0) return r; // 12
        if ((r = chequear(BigInteger.ONE.equals(
                unMes.getField(DatatypeConstants.MONTHS)))) != 0) return r;             // 13
        if ((r = chequear("P1M".equals(unMes.toString()))) != 0) return r;              // 14

        // --- getXMLSchemaType: el tipo son los campos que estan --------------------------------
        if ((r = chequear(DatatypeConstants.DURATION_YEARMONTH.equals(
                f.newDurationYearMonth(true, 1, 2).getXMLSchemaType()))) != 0) return r; // 15
        if ((r = chequear(DatatypeConstants.DURATION_DAYTIME.equals(
                f.newDurationDayTime(true, 1, 2, 3, 4).getXMLSchemaType()))) != 0) return r; // 16
        if ((r = chequear(DatatypeConstants.DURATION.equals(
                f.newDuration(true, 1, 2, 3, 4, 5, 6).getXMLSchemaType()))) != 0) return r; // 17
        // Una combinacion que no es ninguno de los tres levanta.
        boolean reboto = false;
        try {
            f.newDuration("P1Y1D").getXMLSchemaType();
        } catch (IllegalStateException e) {
            reboto = true;
        }
        if ((r = chequear(reboto)) != 0) return r;                                       // 18
        if ((r = chequear("P1Y2M3DT4H5M6S"
                .equals(f.newDuration(true, 1, 2, 3, 4, 5, 6).toString()))) != 0) return r; // 19

        // --- INDETERMINATE, que es el nucleo de la clase ---------------------------------------
        Duration treintaDias = f.newDuration("P30D");
        if ((r = chequear(unMes.compare(treintaDias)
                == DatatypeConstants.INDETERMINATE)) != 0) return r;                     // 20
        if ((r = chequear(treintaDias.compare(unMes)
                == DatatypeConstants.INDETERMINATE)) != 0) return r;                     // 21
        // Las dos direcciones dan false a la vez: eso es lo que hace que `!isLongerThan` no
        // signifique "es mas corta o igual".
        if ((r = chequear(!unMes.isLongerThan(treintaDias))) != 0) return r;             // 22
        if ((r = chequear(!unMes.isShorterThan(treintaDias))) != 0) return r;            // 23
        if ((r = chequear(!unMes.equals(treintaDias))) != 0) return r;                   // 24

        // Sin meses de por medio el orden si existe.
        if ((r = chequear(f.newDuration("PT60S").compare(f.newDuration("PT1M"))
                == DatatypeConstants.EQUAL)) != 0) return r;                             // 25
        if ((r = chequear(f.newDuration("P1D").compare(f.newDuration("PT23H"))
                == DatatypeConstants.GREATER)) != 0) return r;                           // 26
        if ((r = chequear(f.newDuration("P1D").compare(f.newDuration("PT25H"))
                == DatatypeConstants.LESSER)) != 0) return r;                            // 27
        if ((r = chequear(f.newDuration("P1Y").compare(f.newDuration("P12M"))
                == DatatypeConstants.EQUAL)) != 0) return r;                             // 28
        if ((r = chequear(f.newDuration("PT60S").equals(f.newDuration("PT1M")))) != 0) return r; // 29

        // --- Desde milisegundos: se cuenta sobre el calendario, no se divide --------------------
        long dia = 86400000L;
        if ((r = chequear("P0Y0M1DT1H1M1.000S"
                .equals(f.newDuration(90061000L).toString()))) != 0) return r;           // 30
        // 31 dias son un mes justo porque enero tiene 31.
        if ((r = chequear("P0Y1M0DT0H0M0.000S"
                .equals(f.newDuration(31L * dia).toString()))) != 0) return r;           // 31
        // 59 son dos meses justos: enero mas febrero de 1970, que no fue bisiesto.
        if ((r = chequear("P0Y2M0DT0H0M0.000S"
                .equals(f.newDuration(59L * dia).toString()))) != 0) return r;           // 32
        if ((r = chequear("P1Y0M0DT0H0M0.000S"
                .equals(f.newDuration(365L * dia).toString()))) != 0) return r;          // 33
        if ((r = chequear("-P0Y0M30DT0H0M0.000S"
                .equals(f.newDuration(-30L * dia).toString()))) != 0) return r;          // 34
        // Las dos variantes recortadas se quedan con su mitad y descartan la otra.
        if ((r = chequear("P1DT1H1M1.000S"
                .equals(f.newDurationDayTime(90061000L).toString()))) != 0) return r;    // 35
        if ((r = chequear("P0Y0M"
                .equals(f.newDurationYearMonth(90061000L).toString()))) != 0) return r;  // 36

        // --- Aritmetica -------------------------------------------------------------------------
        if ((r = chequear("P2M".equals(unMes.add(unMes).toString()))) != 0) return r;     // 37
        if ((r = chequear("P2Y4M"
                .equals(f.newDuration("P1Y2M").multiply(2).toString()))) != 0) return r;  // 38
        if ((r = chequear("-P1M".equals(unMes.negate().toString()))) != 0) return r;      // 39
        if ((r = chequear(unMes.negate().getSign() == -1
                && unMes.getSign() == 1)) != 0) return r;                                 // 40
        // Los campos no llevan el signo: `-P1M` tiene un mes, no menos un mes.
        if ((r = chequear(unMes.negate().getMonths() == 1)) != 0) return r;               // 41
        if ((r = chequear("PT1.5S"
                .equals(f.newDuration("PT1.5S").toString()))) != 0) return r;             // 42
        // `getSeconds` pierde la fraccion; `getField` no.
        if ((r = chequear(f.newDuration("PT1.5S").getSeconds() == 1)) != 0) return r;     // 43
        if ((r = chequear(new BigDecimal("1.5").compareTo(
                (BigDecimal) f.newDuration("PT1.5S")
                        .getField(DatatypeConstants.SECONDS)) == 0)) != 0) return r;      // 44

        // --- Con un instante de partida la ambigüedad desaparece ---------------------------------
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2024, java.util.Calendar.JANUARY, 31);
        // Del 31 de enero de 2024, un mes son 29 dias: 2024 fue bisiesto.
        if ((r = chequear(unMes.getTimeInMillis(cal) == 29L * dia)) != 0) return r;       // 45
        if ((r = chequear("P29D"
                .equals(unMes.normalizeWith(cal).toString()))) != 0) return r;            // 46

        // --- Las formas lexicas recortadas ------------------------------------------------------
        // `PT1M` es una dayTimeDuration valida: la `M` de minutos va despues de la `T`.
        if ((r = chequear("PT1M"
                .equals(f.newDurationDayTime("PT1M").toString()))) != 0) return r;        // 47
        // `P1M` no: esa `M` es de meses.
        boolean rebotoMes = false;
        try {
            f.newDurationDayTime("P1M");
        } catch (IllegalArgumentException e) {
            rebotoMes = true;
        }
        if ((r = chequear(rebotoMes)) != 0) return r;                                     // 48
        boolean rebotoDia = false;
        try {
            f.newDurationYearMonth("P1D");
        } catch (IllegalArgumentException e) {
            rebotoDia = true;
        }
        if ((r = chequear(rebotoDia)) != 0) return r;                                     // 49
        if ((r = chequear("P1Y2M"
                .equals(f.newDurationYearMonth("P1Y2M").toString()))) != 0) return r;     // 50

        // --- Formas lexicas invalidas -----------------------------------------------------------
        String[] malas = {"1Y", "P", "PT", "P1S", "PT1Y", "P1M2Y"};
        for (int i = 0; i < malas.length; i++) {
            boolean cayo = false;
            try {
                f.newDuration(malas[i]);
            } catch (IllegalArgumentException e) {
                cayo = true;
            }
            if ((r = chequear(cayo)) != 0) return r;                                      // 51..56
        }

        // --- El signo cero -----------------------------------------------------------------------
        if ((r = chequear(f.newDuration(0L).getSign() == 0)) != 0) return r;              // 57

        return -1;
    }

    public static void main(String[] args) {
        System.out.println("XmlDatatypeDurTest -> " + run() + " (de " + cuantas + ")");
    }
}
