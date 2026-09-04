package java.time.format;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.Chronology;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalQueries;
import java.util.Iterator;
import java.util.Set;

// El desplazamiento de zona escrito como numero: `+01:00`, `-0330`, `Z`.
//
// El patron que recibe --`"+HH:MM:ss"` y los otros veintiuno del JDK-- no se guarda como cadena: se
// descompone en tres decisiones (cuantos digitos de hora, si los minutos salen siempre o solo cuando
// no son cero, idem los segundos) y son esas tres las que manejan el escribir y el leer. Guardarlo
// como cadena obligaria a reinterpretarlo en cada llamada y, peor, a mantener dos interpretaciones
// separadas para las dos direcciones.
//
// **La mayuscula manda.** `MM` sale siempre; `mm` sale solo si hay algo que decir. Esa es toda la
// diferencia entre `"+HH:MM"` (que escribe `+05:00`) y `"+HH:mm"` (que escribe `+05`).
final class PiezaOffset extends Pieza {

    static final int AUSENTE = 0;
    static final int CONDICIONAL = 1;
    static final int SIEMPRE = 2;

    private final int anchoHora;
    private final boolean dosPuntos;
    private final int modoMinuto;
    private final int modoSegundo;
    private final String sinOffset;

    PiezaOffset(String patron, String sinOffset) {
        this.sinOffset = sinOffset;
        if (patron == null || patron.length() < 2 || patron.charAt(0) != '+') {
            throw new IllegalArgumentException("Invalid zone offset pattern: " + patron);
        }
        int i = 1;
        int horas = 0;
        while (i < patron.length() && patron.charAt(i) == 'H') {
            horas = horas + 1;
            i = i + 1;
        }
        if (horas < 1 || horas > 2) {
            throw new IllegalArgumentException("Invalid zone offset pattern: " + patron);
        }
        boolean colon = false;
        if (i < patron.length() && patron.charAt(i) == ':') {
            colon = true;
            i = i + 1;
        }
        int min = AUSENTE;
        if (i < patron.length() && (patron.charAt(i) == 'm' || patron.charAt(i) == 'M')) {
            min = patron.charAt(i) == 'M' ? SIEMPRE : CONDICIONAL;
            char c = patron.charAt(i);
            int n = 0;
            while (i < patron.length() && patron.charAt(i) == c) {
                n = n + 1;
                i = i + 1;
            }
            if (n != 2) {
                throw new IllegalArgumentException("Invalid zone offset pattern: " + patron);
            }
        }
        if (i < patron.length() && patron.charAt(i) == ':') {
            if (!colon) {
                throw new IllegalArgumentException("Invalid zone offset pattern: " + patron);
            }
            i = i + 1;
        }
        int seg = AUSENTE;
        if (i < patron.length() && (patron.charAt(i) == 's' || patron.charAt(i) == 'S')) {
            seg = patron.charAt(i) == 'S' ? SIEMPRE : CONDICIONAL;
            char c = patron.charAt(i);
            int n = 0;
            while (i < patron.length() && patron.charAt(i) == c) {
                n = n + 1;
                i = i + 1;
            }
            if (n != 2) {
                throw new IllegalArgumentException("Invalid zone offset pattern: " + patron);
            }
        }
        if (i != patron.length() || (seg != AUSENTE && min == AUSENTE)) {
            throw new IllegalArgumentException("Invalid zone offset pattern: " + patron);
        }
        this.anchoHora = horas;
        this.dosPuntos = colon;
        this.modoMinuto = min;
        this.modoSegundo = seg;
    }

    boolean imprimir(CtxImprimir ctx, StringBuilder salida) {
        Long v = ctx.valor(ChronoField.OFFSET_SECONDS);
        if (v == null) {
            return false;
        }
        long total = v.longValue();
        if (total == 0L && this.sinOffset.length() > 0) {
            salida.append(this.sinOffset);
            return true;
        }
        long abs = total < 0L ? -total : total;
        long h = abs / 3600L;
        long m = abs / 60L % 60L;
        long s = abs % 60L;
        boolean saleSeg = this.modoSegundo == SIEMPRE || (this.modoSegundo == CONDICIONAL && s != 0L);
        boolean saleMin = this.modoMinuto == SIEMPRE
                || (this.modoMinuto == CONDICIONAL && (m != 0L || saleSeg));
        salida.append(total < 0L ? '-' : '+');
        Pieza.escribirDigitos(ctx, salida, Long.toString(h), this.anchoHora);
        if (saleMin) {
            if (this.dosPuntos) {
                salida.append(':');
            }
            Pieza.escribirDigitos(ctx, salida, Long.toString(m), 2);
            if (saleSeg) {
                if (this.dosPuntos) {
                    salida.append(':');
                }
                Pieza.escribirDigitos(ctx, salida, Long.toString(s), 2);
            }
        }
        return true;
    }

    int parsear(CtxParseo ctx, String texto, int pos) {
        int largoSin = this.sinOffset.length();
        if (largoSin == 0) {
            // `appendOffset(patron, "")` no tiene texto para el cero: el cero se escribe con numeros.
            // Solo el final del texto se toma como "no habia offset".
            if (pos == texto.length()) {
                ctx.offset = ZoneOffset.UTC;
                ctx.poner(ChronoField.OFFSET_SECONDS, 0L);
                return pos;
            }
        } else if (texto.regionMatches(!ctx.sensible, pos, this.sinOffset, 0, largoSin)) {
            ctx.offset = ZoneOffset.UTC;
            ctx.poner(ChronoField.OFFSET_SECONDS, 0L);
            return pos + largoSin;
        }
        if (pos >= texto.length()) {
            return ~pos;
        }
        char signo = texto.charAt(pos);
        if (signo != '+' && signo != '-') {
            return ~pos;
        }
        int negativo = signo == '-' ? -1 : 1;
        int[] partes = {0, 0, 0};
        int p = pos + 1;
        int leidos = this.leerPar(texto, p, this.anchoHora == 1, partes, 0);
        if (leidos < 0) {
            return ~pos;
        }
        p = leidos;
        if (this.modoMinuto != AUSENTE) {
            int q = p;
            if (this.dosPuntos) {
                if (q < texto.length() && texto.charAt(q) == ':') {
                    q = q + 1;
                } else {
                    q = -1;
                }
            }
            if (q >= 0) {
                int r = this.leerPar(texto, q, false, partes, 1);
                if (r >= 0) {
                    p = r;
                    if (this.modoSegundo != AUSENTE) {
                        int q2 = p;
                        if (this.dosPuntos) {
                            if (q2 < texto.length() && texto.charAt(q2) == ':') {
                                q2 = q2 + 1;
                            } else {
                                q2 = -1;
                            }
                        }
                        if (q2 >= 0) {
                            int r2 = this.leerPar(texto, q2, false, partes, 2);
                            if (r2 >= 0) {
                                p = r2;
                            }
                        }
                    }
                } else if (ctx.estricto && this.modoMinuto == SIEMPRE) {
                    return ~pos;
                }
            } else if (ctx.estricto && this.modoMinuto == SIEMPRE) {
                return ~pos;
            }
        }
        if (partes[1] > 59 || partes[2] > 59) {
            return ~pos;
        }
        int total = negativo * (partes[0] * 3600 + partes[1] * 60 + partes[2]);
        ZoneOffset off;
        try {
            off = ZoneOffset.ofTotalSeconds(total);
        } catch (DateTimeException e) {
            return ~pos;
        }
        ctx.offset = off;
        ctx.poner(ChronoField.OFFSET_SECONDS, (long) total);
        return p;
    }

    // Dos digitos --o uno, cuando el patron es `+H` y el segundo no esta--. Devuelve el indice
    // siguiente, o -1.
    private int leerPar(String texto, int pos, boolean unoAlcanza, int[] partes, int cual) {
        if (pos >= texto.length()) {
            return -1;
        }
        int d1 = texto.charAt(pos) - '0';
        if (d1 < 0 || d1 > 9) {
            return -1;
        }
        if (pos + 1 < texto.length()) {
            int d2 = texto.charAt(pos + 1) - '0';
            if (d2 >= 0 && d2 <= 9) {
                partes[cual] = d1 * 10 + d2;
                return pos + 2;
            }
        }
        if (unoAlcanza || cual == 0) {
            partes[cual] = d1;
            return pos + 1;
        }
        return -1;
    }
}

// El identificador de la zona: `Europe/Paris`, `Z`, `+05:00`.
//
// Tres modos, que son los tres `append*` del JDK y se distinguen **por lo que preguntan**, no por lo
// que escriben:
//   - `ZONA` (`appendZoneId`) pregunta por la zona declarada. Un `OffsetDateTime` no tiene una, asi
//     que no imprime.
//   - `ZONA_U_OFFSET` (`appendZoneOrOffsetId`) acepta que el desplazamiento haga de zona, que es lo
//     unico que se sabe del lugar cuando no hay region.
//   - `REGION` (`appendZoneRegionId`) escribe solo si es una region de verdad: es la pieza del
//     `[Europe/Paris]` de `ISO_ZONED_DATE_TIME`, donde un `[+02:00]` seria informacion repetida.
final class PiezaZonaId extends Pieza {

    static final int ZONA = 0;
    static final int ZONA_U_OFFSET = 1;
    static final int REGION = 2;

    private final int modo;

    PiezaZonaId(int modo) {
        this.modo = modo;
    }

    boolean imprimir(CtxImprimir ctx, StringBuilder salida) {
        ZoneId z;
        if (this.modo == ZONA_U_OFFSET) {
            z = ctx.consultar(TemporalQueries.zone());
        } else {
            z = ctx.consultar(TemporalQueries.zoneId());
        }
        if (z == null) {
            return ctx.faltaOTira("ZoneId");
        }
        if (this.modo == REGION && z instanceof ZoneOffset) {
            // Una region que no lo es. `ISO_ZONED_DATE_TIME` la tiene dentro de una seccion opcional
            // justamente para esto: `2024-01-01T00:00+02:00` sale sin `[...]`, y no con `[+02:00]`.
            return ctx.faltaOTira("ZoneRegionId");
        }
        salida.append(z.getId());
        return true;
    }

    // Los caracteres que un identificador de zona admite. El `+`/`-` estan porque `+05:00` tambien es
    // un identificador legal de `ZoneId`.
    private static boolean esDeId(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                || c == '/' || c == '_' || c == '.' || c == '-' || c == '+' || c == ':' || c == '~';
    }

    int parsear(CtxParseo ctx, String texto, int pos) {
        if (pos >= texto.length()) {
            return ~pos;
        }
        char c = texto.charAt(pos);
        if (this.modo != REGION && (c == '+' || c == '-')) {
            PiezaOffset off = new PiezaOffset("+HH:MM:ss", "");
            int r = off.parsear(ctx, texto, pos);
            if (r < 0) {
                return r;
            }
            ctx.zona = ctx.offset;
            return r;
        }
        if (this.modo != REGION && (c == 'Z' || c == 'z')) {
            // Solo si `Z` esta suelta: `Zulu` es una zona con nombre y no el offset cero.
            if (pos + 1 >= texto.length() || !esDeId(texto.charAt(pos + 1))) {
                ctx.zona = ZoneOffset.UTC;
                ctx.offset = ZoneOffset.UTC;
                ctx.poner(ChronoField.OFFSET_SECONDS, 0L);
                return pos + 1;
            }
        }
        int fin = pos;
        while (fin < texto.length() && esDeId(texto.charAt(fin))) {
            fin = fin + 1;
        }
        // Del candidato mas largo hacia atras: `Europe/Paris]` tiene que dar `Europe/Paris`, y
        // `America/New_York` no puede quedarse en `America`.
        Set<String> conocidas = ZoneId.getAvailableZoneIds();
        int largo = fin;
        while (largo > pos) {
            String id = texto.substring(pos, largo);
            if (conocidas.contains(id)) {
                ctx.zona = ZoneId.of(id);
                return largo;
            }
            largo = largo - 1;
        }
        // `UTC`, `GMT` y `UT` no estan en la lista de regiones pero son prefijos legales, con o sin
        // desplazamiento pegado.
        String[] prefijos = {"UTC", "GMT", "UT"};
        int i = 0;
        while (i < prefijos.length) {
            String pre = prefijos[i];
            if (texto.regionMatches(!ctx.sensible, pos, pre, 0, pre.length())) {
                int p = pos + pre.length();
                if (p < texto.length() && (texto.charAt(p) == '+' || texto.charAt(p) == '-')) {
                    PiezaOffset off = new PiezaOffset("+HH:MM:ss", "");
                    int r = off.parsear(ctx, texto, p);
                    if (r >= 0) {
                        ctx.zona = ZoneId.of(texto.substring(pos, r));
                        return r;
                    }
                }
                ctx.zona = ZoneId.of(pre);
                return p;
            }
            i = i + 1;
        }
        return ~pos;
    }
}

// El identificador del calendario: `ISO`, `ThaiBuddhist`.
//
// Es el **id**, no el nombre: `appendChronologyText` --que si necesita CLDR-- no esta. Este si puede
// estar porque el id no es texto traducible, es la clave.
final class PiezaCronologiaId extends Pieza {

    boolean imprimir(CtxImprimir ctx, StringBuilder salida) {
        Chronology c = ctx.consultar(TemporalQueries.chronology());
        if (c == null) {
            return false;
        }
        salida.append(c.getId());
        return true;
    }

    int parsear(CtxParseo ctx, String texto, int pos) {
        Chronology mejor = null;
        int largoMejor = 0;
        Iterator<Chronology> it = Chronology.getAvailableChronologies().iterator();
        while (it.hasNext()) {
            Chronology c = it.next();
            String id = c.getId();
            if (id.length() > largoMejor
                    && texto.regionMatches(!ctx.sensible, pos, id, 0, id.length())) {
                mejor = c;
                largoMejor = id.length();
            }
        }
        if (mejor == null) {
            return ~pos;
        }
        ctx.cronologia = mejor;
        return pos + largoMejor;
    }
}

// `appendInstant`: el instante en UTC, siempre, con `Z` al final.
//
// **Por que es una pieza y no una composicion de las otras.** Un instante no tiene fecha ni hora
// local hasta que se le pone una zona; lo que tiene es `INSTANT_SECONDS`. Escribirlo requiere
// convertir a UTC primero, y --sobre todo-- leerlo requiere aceptar dos cosas que ningun `LocalTime`
// admite: `24:00`, que es la medianoche del dia siguiente, y `:60`, el segundo intercalar. ISO-8601
// las escribe y el JDK las lee; una composicion de piezas normales las rechazaria al resolver.
//
// `digitos`: -2 escribe 0, 3, 6 o 9 --lo que haga falta, en grupos de tres, que es lo que ISO pide--;
// -1 los minimos; 0..9 esa cantidad exacta.
final class PiezaInstante extends Pieza {

    private static final long SEGUNDOS_POR_DIA = 86400L;

    private final int digitos;
    private final Pieza[] lector;

    PiezaInstante(int digitos) {
        this.digitos = digitos;
        // El lector se arma una sola vez. Las horas y los segundos van con `max` 2 pero **sin** tope
        // de rango: el rango se chequea despues, cuando ya se sabe si `24` y `60` son los casos
        // especiales o un error.
        Pieza[] p = new Pieza[11];
        p[0] = new PiezaNumero(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD);
        p[1] = new PiezaLiteral("-");
        p[2] = new PiezaNumero(ChronoField.MONTH_OF_YEAR, 2, 2, SignStyle.NOT_NEGATIVE);
        p[3] = new PiezaLiteral("-");
        p[4] = new PiezaNumero(ChronoField.DAY_OF_MONTH, 2, 2, SignStyle.NOT_NEGATIVE);
        p[5] = new PiezaLiteral("T");
        p[6] = new PiezaNumero(ChronoField.HOUR_OF_DAY, 2, 2, SignStyle.NOT_NEGATIVE);
        p[7] = new PiezaLiteral(":");
        p[8] = new PiezaNumero(ChronoField.MINUTE_OF_HOUR, 2, 2, SignStyle.NOT_NEGATIVE);
        p[9] = new PiezaCompuesta(new Pieza[] {
            new PiezaLiteral(":"),
            new PiezaNumero(ChronoField.SECOND_OF_MINUTE, 2, 2, SignStyle.NOT_NEGATIVE),
            new PiezaCompuesta(new Pieza[] {
                new PiezaFraccion(ChronoField.NANO_OF_SECOND, 0, 9, true),
            }, true),
        }, true);
        p[10] = new PiezaOffset("+HH:MM:ss", "Z");
        this.lector = p;
    }

    boolean imprimir(CtxImprimir ctx, StringBuilder salida) {
        Long segundos = ctx.valor(ChronoField.INSTANT_SECONDS);
        if (segundos == null) {
            return false;
        }
        long nanos = 0L;
        if (ctx.temporal().isSupported(ChronoField.NANO_OF_SECOND)) {
            nanos = ctx.temporal().getLong(ChronoField.NANO_OF_SECOND);
        }
        LocalDateTime ldt = LocalDateTime.ofEpochSecond(segundos.longValue(), 0, ZoneOffset.UTC);
        StringBuilder tmp = new StringBuilder();
        long anio = (long) ldt.getYear();
        String d = Long.toString(anio < 0L ? -anio : anio);
        if (anio > 9999L) {
            tmp.append('+');
        } else if (anio < 0L) {
            tmp.append('-');
        }
        Pieza.escribirDigitos(ctx, tmp, d, 4);
        tmp.append('-');
        Pieza.escribirDigitos(ctx, tmp, Long.toString((long) ldt.getMonthValue()), 2);
        tmp.append('-');
        Pieza.escribirDigitos(ctx, tmp, Long.toString((long) ldt.getDayOfMonth()), 2);
        tmp.append('T');
        Pieza.escribirDigitos(ctx, tmp, Long.toString((long) ldt.getHour()), 2);
        tmp.append(':');
        Pieza.escribirDigitos(ctx, tmp, Long.toString((long) ldt.getMinute()), 2);
        tmp.append(':');
        Pieza.escribirDigitos(ctx, tmp, Long.toString((long) ldt.getSecond()), 2);
        this.fraccion(ctx, tmp, nanos);
        tmp.append('Z');
        salida.append(tmp.toString());
        return true;
    }

    private void fraccion(CtxImprimir ctx, StringBuilder salida, long nanos) {
        if (this.digitos == 0) {
            return;
        }
        String nueve = Long.toString(nanos);
        while (nueve.length() < 9) {
            nueve = "0" + nueve;
        }
        int cuantos;
        if (this.digitos > 0) {
            cuantos = this.digitos;
        } else if (this.digitos == -1) {
            cuantos = 9;
            while (cuantos > 0 && nueve.charAt(cuantos - 1) == '0') {
                cuantos = cuantos - 1;
            }
        } else {
            // -2: en grupos de tres, que es como ISO-8601 escribe milis, micros y nanos.
            if (nanos == 0L) {
                cuantos = 0;
            } else if (nanos % 1000000L == 0L) {
                cuantos = 3;
            } else if (nanos % 1000L == 0L) {
                cuantos = 6;
            } else {
                cuantos = 9;
            }
        }
        if (cuantos == 0) {
            return;
        }
        salida.append(ctx.simbolos.getDecimalSeparator());
        Pieza.escribirDigitos(ctx, salida, nueve.substring(0, cuantos), 0);
    }

    int parsear(CtxParseo ctx, String texto, int pos) {
        CtxParseo interno = new CtxParseo(ctx.locale, ctx.simbolos, false, null, null);
        interno.sensible = ctx.sensible;
        int p = pos;
        int i = 0;
        while (i < this.lector.length) {
            p = this.lector[i].parsear(interno, texto, p);
            if (p < 0) {
                return ~pos;
            }
            i = i + 1;
        }
        Long anio = interno.campos.get(ChronoField.YEAR);
        Long mes = interno.campos.get(ChronoField.MONTH_OF_YEAR);
        Long dia = interno.campos.get(ChronoField.DAY_OF_MONTH);
        Long hora = interno.campos.get(ChronoField.HOUR_OF_DAY);
        Long minuto = interno.campos.get(ChronoField.MINUTE_OF_HOUR);
        Long segundo = interno.campos.get(ChronoField.SECOND_OF_MINUTE);
        Long nano = interno.campos.get(ChronoField.NANO_OF_SECOND);
        Long off = interno.campos.get(ChronoField.OFFSET_SECONDS);
        if (anio == null || mes == null || dia == null || hora == null || minuto == null
                || off == null) {
            return ~pos;
        }
        long h = hora.longValue();
        long s = segundo == null ? 0L : segundo.longValue();
        // Los dos que ninguna hora local admite. Se normalizan **aca** --24:00 pasa a 00:00 del dia
        // siguiente, :60 a :59-- y se anota lo que se hizo, porque el instante resultante es el
        // correcto y el que llame por `parsedLeapSecond` merece saber que el texto decia 60.
        int diaDeMas = 0;
        boolean bisiesto = false;
        if (h == 24L && minuto.longValue() == 0L && s == 0L && (nano == null || nano.longValue() == 0L)) {
            h = 0L;
            diaDeMas = 1;
        } else if (h > 23L) {
            return ~pos;
        }
        if (s == 60L) {
            s = 59L;
            bisiesto = true;
        } else if (s > 59L) {
            return ~pos;
        }
        LocalDate fecha;
        try {
            fecha = LocalDate.of((int) anio.longValue(), (int) mes.longValue(),
                    (int) dia.longValue());
        } catch (java.time.DateTimeException e) {
            return ~pos;
        }
        long epochDia = fecha.toEpochDay() + (long) diaDeMas;
        long segundosDelDia = h * 3600L + minuto.longValue() * 60L + s;
        long instante = epochDia * SEGUNDOS_POR_DIA + segundosDelDia - off.longValue();
        ctx.poner(ChronoField.INSTANT_SECONDS, instante);
        ctx.poner(ChronoField.NANO_OF_SECOND, nano == null ? 0L : nano.longValue());
        ctx.poner(ChronoField.OFFSET_SECONDS, off.longValue());
        ctx.offset = ZoneOffset.ofTotalSeconds((int) off.longValue());
        if (bisiesto) {
            ctx.segundoBisiesto = true;
        }
        return p;
    }
}
