package javax.xml.datatype;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.GregorianCalendar;

/**
 * La {@link DatatypeFactory} concreta de esta biblioteca: la que devuelve
 * {@link DatatypeFactory#newDefaultInstance()}.
 *
 * <p>Interna, y muy corta a proposito. Los siete metodos abstractos de la fabrica son los
 * generales, y los doce concretos ya estan escritos en {@code DatatypeFactory} en terminos de
 * ellos. Asi que todo lo que hay aca es enchufar las dos implementaciones --{@link KajiDuration} y
 * {@link KajiXMLGregorianCalendar}-- y el resto de la API queda funcionando por herencia.
 */
final class KajiDatatypeFactory extends DatatypeFactory {

    /** Publico dentro del paquete: lo instancia {@link DatatypeFactory#newDefaultInstance()}. */
    KajiDatatypeFactory() {
    }

    /** {@inheritDoc} */
    public Duration newDuration(String lexicalRepresentation) {
        return KajiDuration.parse(lexicalRepresentation);
    }

    /** {@inheritDoc} */
    public Duration newDuration(long durationInMilliseconds) {
        return KajiDuration.fromMillis(durationInMilliseconds);
    }

    /** {@inheritDoc} */
    public Duration newDuration(boolean isPositive, BigInteger years, BigInteger months,
            BigInteger days, BigInteger hours, BigInteger minutes, BigDecimal seconds) {
        return new KajiDuration(isPositive, years, months, days, hours, minutes, seconds);
    }

    /** {@inheritDoc} */
    public XMLGregorianCalendar newXMLGregorianCalendar() {
        return new KajiXMLGregorianCalendar();
    }

    /** {@inheritDoc} */
    public XMLGregorianCalendar newXMLGregorianCalendar(String lexicalRepresentation) {
        return KajiXMLGregorianCalendar.parse(lexicalRepresentation);
    }

    /** {@inheritDoc} */
    public XMLGregorianCalendar newXMLGregorianCalendar(GregorianCalendar cal) {
        return new KajiXMLGregorianCalendar(cal);
    }

    /** {@inheritDoc} */
    public XMLGregorianCalendar newXMLGregorianCalendar(BigInteger year, int month, int day,
            int hour, int minute, int second, BigDecimal fractionalSecond, int timezone) {
        return new KajiXMLGregorianCalendar(
                year, month, day, hour, minute, second, fractionalSecond, timezone);
    }
}
