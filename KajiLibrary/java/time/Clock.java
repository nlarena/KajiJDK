package java.time;

// KajiLibrary's java.time.Clock — a supplier of the current instant. A KajiLibrary subset: the
// La superficie esta completa: instant()/millis(), la zona (getZone/withZone) y las seis fabricas
// (system/systemUTC/systemDefaultZone/fixed/offset/tick y sus tres variantes de tick).
public abstract class Clock implements InstantSource {

    protected Clock() {
    }

    public abstract Instant instant();

    /**
     * La zona de este reloj.
     *
     * <p>Es lo unico que un `Clock` agrega sobre un `InstantSource`: aquel sabe **cuando**, este sabe
     * ademas **donde**. `LocalDate.now(clock)` necesita las dos cosas -- el mismo instante es un dia
     * distinto en Tokio y en Buenos Aires.
     */
    public abstract ZoneId getZone();

    /** Este reloj con otra zona. El instante no cambia; cambia como se lo interpreta. */
    public abstract Clock withZone(ZoneId zone);

    /** El reloj del sistema en esa zona. */
    public static Clock system(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return new SystemClock(zone);
    }

    /** El reloj del sistema en la zona por defecto. */
    public static Clock systemDefaultZone() {
        return Clock.system(ZoneId.systemDefault());
    }

    /**
     * Un reloj **detenido**.
     *
     * <p>Es el que hace testeable el codigo que mira la hora: "ahora" pasa a ser un valor que la
     * prueba elige, y el resultado deja de depender de cuando se corra.
     */
    public static Clock fixed(Instant fixedInstant, ZoneId zone) {
        if (fixedInstant == null || zone == null) {
            throw new NullPointerException();
        }
        return new SourceClock(InstantSource.fixed(fixedInstant), zone);
    }

    /** El mismo reloj, corrido `offsetDuration`. */
    public static Clock offset(Clock baseClock, Duration offsetDuration) {
        if (baseClock == null || offsetDuration == null) {
            throw new NullPointerException();
        }
        if (offsetDuration.isZero()) {
            return baseClock;
        }
        return new SourceClock(InstantSource.offset(baseClock, offsetDuration), baseClock.getZone());
    }

    /**
     * El mismo reloj, avanzando **a saltos** de `tickDuration`.
     *
     * @throws IllegalArgumentException si la duracion es negativa o no divide un dia
     */
    public static Clock tick(Clock baseClock, Duration tickDuration) {
        if (baseClock == null || tickDuration == null) {
            throw new NullPointerException();
        }
        if (tickDuration.isZero()) {
            return baseClock;
        }
        return new SourceClock(InstantSource.tick(baseClock, tickDuration), baseClock.getZone());
    }

    /** Un reloj que avanza de a milisegundos enteros. */
    public static Clock tickMillis(ZoneId zone) {
        return Clock.tick(Clock.system(zone), Duration.ofMillis(1L));
    }

    /** De a segundos enteros. */
    public static Clock tickSeconds(ZoneId zone) {
        return Clock.tick(Clock.system(zone), Duration.ofSeconds(1L));
    }

    /** De a minutos enteros. */
    public static Clock tickMinutes(ZoneId zone) {
        return Clock.tick(Clock.system(zone), Duration.ofMinutes(1L));
    }

    public long millis() {
        return System.currentTimeMillis();
    }

    public static Clock systemUTC() {
        return new SystemClock(ZoneOffset.UTC);
    }
}

// The system clock, reading the VM's wall time. Package-private (the JDK nests it inside Clock;
// the API-shape gate skips it since there is no java.time.SystemClock in the JDK).
//
// Lleva la zona porque un `Clock` la tiene: el mismo instante es un dia distinto en Tokio y en Buenos
// Aires, y `LocalDate.now(clock)` necesita las dos cosas.
final class SystemClock extends Clock {

    private final ZoneId zona;

    SystemClock(ZoneId zona) {
        this.zona = zona;
    }

    public Instant instant() {
        return Instant.now();
    }

    public ZoneId getZone() {
        return this.zona;
    }

    public Clock withZone(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return zone.equals(this.zona) ? this : new SystemClock(zone);
    }
}
