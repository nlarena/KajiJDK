package javax.xml.datatype;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.GregorianCalendar;

/**
 * This library's concrete {@link DatatypeFactory}: the one {@link
 * DatatypeFactory#newDefaultInstance()} returns.
 *
 * <p>Internal, and very short on purpose. The factory's seven abstract methods are the general
 * ones, and the fourteen concrete ones are already written in {@code DatatypeFactory} in terms of
 * them. So all there is here is plugging in the two implementations --{@link KajiDuration} and
 * {@link KajiXMLGregorianCalendar}-- and the rest of the API works by inheritance.
 */
final class KajiDatatypeFactory extends DatatypeFactory {

    /** Public within the package: {@link DatatypeFactory#newDefaultInstance()} instantiates it. */
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
