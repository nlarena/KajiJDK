package java.time;

// KajiLibrary's java.time.InstantSource — a source of the current instant, the time-zone-free
// supertype of Clock (a Clock is an InstantSource that also knows a zone).
@FunctionalInterface
public interface InstantSource {

    Instant instant();

    default long millis() {
        return instant().toEpochMilli();
    }

    /**
     * Un `Clock` con esta fuente y esa zona.
     *
     * <p>Es el puente de `InstantSource` a `Clock`: aquella solo sabe **cuando**, y un `Clock` sabe
     * ademas **donde**, que es lo que hace falta para `LocalDate.now()`.
     */
    default java.time.Clock withZone(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return new SourceClock(this, zone);
    }

    /** El reloj del sistema. */
    static InstantSource system() {
        return java.time.Clock.systemUTC();
    }

    /**
     * Una fuente **detenida** en ese instante.
     *
     * <p>Es la que hace testeable el codigo que mira la hora: con una fuente fija, "ahora" es un
     * valor que la prueba elige, y el resultado deja de depender de cuando se corra.
     */
    static InstantSource fixed(Instant fixedInstant) {
        if (fixedInstant == null) {
            throw new NullPointerException("fixedInstant");
        }
        return new FixedSource(fixedInstant);
    }

    /** La misma fuente, corrida `offsetDuration`. */
    static InstantSource offset(InstantSource baseSource, Duration offsetDuration) {
        if (baseSource == null || offsetDuration == null) {
            throw new NullPointerException();
        }
        if (offsetDuration.isZero()) {
            return baseSource;
        }
        return new OffsetSource(baseSource, offsetDuration);
    }

    /**
     * La misma fuente, **truncada** a multiplos de `tickDuration`.
     *
     * <p>Un reloj que avanza a saltos: con un tick de un segundo, los nanos salen siempre en cero.
     * Sirve para que dos lecturas cercanas den el mismo valor a proposito.
     *
     * @throws IllegalArgumentException si `tickDuration` es negativa o no divide un dia
     */
    static InstantSource tick(InstantSource baseSource, Duration tickDuration) {
        if (baseSource == null || tickDuration == null) {
            throw new NullPointerException();
        }
        if (tickDuration.isNegative()) {
            throw new IllegalArgumentException("Tick duration must not be negative");
        }
        long nanos = tickDuration.toNanos();
        if (nanos == 0L) {
            return baseSource;
        }
        if (nanos % 1000000L != 0L && 1000000000L % nanos != 0L) {
            throw new IllegalArgumentException("Invalid tick duration");
        }
        return new TickSource(baseSource, nanos);
    }
}

// Las tres fuentes derivadas y el reloj que las envuelve. Van como clases de paquete y no anidadas
// porque una clase anidada dentro de una interfaz generica no resuelve bien todavia.

final class FixedSource implements InstantSource {

    private final Instant fijo;

    FixedSource(Instant fijo) {
        this.fijo = fijo;
    }

    public Instant instant() {
        return this.fijo;
    }
}

final class OffsetSource implements InstantSource {

    private final InstantSource base;
    private final Duration corrimiento;

    OffsetSource(InstantSource base, Duration corrimiento) {
        this.base = base;
        this.corrimiento = corrimiento;
    }

    public Instant instant() {
        return this.base.instant().plus(this.corrimiento);
    }
}

final class TickSource implements InstantSource {

    private final InstantSource base;
    private final long tickNanos;

    TickSource(InstantSource base, long tickNanos) {
        this.base = base;
        this.tickNanos = tickNanos;
    }

    public Instant instant() {
        Instant i = this.base.instant();
        long nanos = i.getNano();
        // Se trunca **hacia abajo** dentro del segundo, que es lo que hace que el reloj no retroceda
        // nunca: cada lectura cae en el mismo tick o en uno posterior.
        long sobra = nanos % this.tickNanos;
        return i.minusNanos(sobra);
    }
}

// El `Clock` que devuelve `InstantSource.withZone`: la fuente pone el instante y la zona el lugar.
final class SourceClock extends java.time.Clock {

    private final InstantSource fuente;
    private final ZoneId zona;

    SourceClock(InstantSource fuente, ZoneId zona) {
        this.fuente = fuente;
        this.zona = zona;
    }

    public Instant instant() {
        return this.fuente.instant();
    }

    public ZoneId getZone() {
        return this.zona;
    }

    public java.time.Clock withZone(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return zone.equals(this.zona) ? this : new SourceClock(this.fuente, zone);
    }
}
