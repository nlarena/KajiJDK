package javax.xml.datatype;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.namespace.QName;

/**
 * KajiLibrary's javax.xml.datatype.XMLGregorianCalendar -- una fecha y hora de XML Schema, donde lo
 * caracteristico es que <b>casi cualquier campo puede faltar</b>.
 *
 * <h2>Ocho tipos en una clase</h2>
 *
 * <p>XML Schema no tiene un tipo de fecha sino ocho, y la diferencia entre ellos es exactamente que
 * campos traen: {@code xs:date} no tiene hora, {@code xs:time} no tiene fecha, {@code xs:gMonth} es
 * "mayo" sin anio ni dia, {@code xs:gMonthDay} es "25 de mayo" de cualquier anio. Todos entran aca,
 * y {@link #getXMLSchemaType} contesta cual es mirando que campos estan puestos.
 *
 * <p>Los campos son {@code int}, asi que la ausencia se marca con
 * {@link DatatypeConstants#FIELD_UNDEFINED} y no con null. Es la trampa numero uno de la clase:
 * {@code getYear()} devuelve {@code Integer.MIN_VALUE} para un {@code gMonth} y no levanta ninguna
 * excepcion, asi que quien no compare contra la constante se lleva ese numero adentro de una cuenta.
 *
 * <h2>La zona horaria tambien puede faltar, y eso arruina el orden</h2>
 *
 * <p>Una fecha sin zona horaria no es un instante: es una fecha "en algun lado". Comparar
 * {@code 2024-05-25T12:00:00} (sin zona) con {@code 2024-05-25T14:00:00Z} no tiene respuesta,
 * porque la primera puede caer antes o despues segun donde se lea. Por eso {@link #compare} tambien
 * puede devolver {@link DatatypeConstants#INDETERMINATE}, y por eso esta clase, igual que
 * {@link Duration}, <b>no</b> implementa {@code Comparable}.
 *
 * <p>{@link #normalize} es la salida cuando hay zona: lleva todo a UTC y ahi si se puede comparar.
 *
 * <h2>El anio no entra en un {@code int}</h2>
 *
 * <p>XML Schema no le pone tope al anio, asi que hay dos accesores. {@link #getYear} devuelve un
 * {@code int} y alcanza para todo lo que existio; {@link #getEon} devuelve los miles de millones que
 * sobran, y {@link #getEonAndYear} los junta en un {@link BigInteger}. Para fechas normales
 * {@code getEon()} es null y solo hace falta {@code getYear()}.
 *
 * <p>Y el anio cero: en XML Schema 1.0 <b>no existe</b> --se va del -1 al 1--, y esta API lo permite
 * igual porque 1.1 lo agrego. {@link #isValid} es el que decide segun el caso.
 *
 * <h2>Que hay aca</h2>
 *
 * <p>La clase entera, con la misma division que el original: lo abstracto es lo que depende de como
 * se guarden los campos, y lo concreto --los tres {@code setTime}, {@code getMillisecond},
 * {@code equals}, {@code hashCode}, {@code toString}-- esta escrito en terminos de lo abstracto y
 * funciona para cualquier subclase.
 *
 * <p>{@link DatatypeFactory#newInstance()} de esta biblioteca devuelve una fabrica que produce
 * instancias reales de esta clase: parseo de las ocho formas lexicas, comparacion con
 * normalizacion, suma de duraciones y conversion a {@link GregorianCalendar}. Nada de eso necesita
 * un parser de XML.
 */
public abstract class XMLGregorianCalendar implements Cloneable {

    /**
     * Para las subclases.
     *
     * <p>Publico como en el original, aunque la clase sea abstracta.
     */
    public XMLGregorianCalendar() {
    }

    /**
     * Deja todos los campos en {@link DatatypeConstants#FIELD_UNDEFINED}.
     *
     * <p>Distinto de {@link #reset}: esto vacia, aquello vuelve a como estaba cuando se creo.
     */
    public abstract void clear();

    /**
     * Vuelve a los valores que tenia recien construida.
     *
     * <p>Existe para reusar la instancia en un bucle sin volver a pedirsela a la fabrica, que es el
     * tipo de optimizacion que tiene sentido cuando se procesan documentos grandes.
     */
    public abstract void reset();

    /**
     * El anio, sin tope.
     *
     * <p>Null deja el anio sin definir. Un valor que entre en un {@code int} se guarda en el campo
     * chico con {@code eon} en null; uno mas grande se parte entre los dos.
     *
     * @param year el anio, o null para borrarlo
     */
    public abstract void setYear(BigInteger year);

    /**
     * El anio, en el rango de un {@code int}.
     *
     * @param year el anio, o {@link DatatypeConstants#FIELD_UNDEFINED} para borrarlo
     */
    public abstract void setYear(int year);

    /**
     * El mes, de {@link DatatypeConstants#JANUARY} a {@link DatatypeConstants#DECEMBER}.
     *
     * <p>Contado desde <b>uno</b>. {@code java.util.Calendar} lo cuenta desde cero, y esa
     * diferencia de uno entre las dos APIs es una fuente clasica de errores.
     *
     * @param month de 1 a 12, o {@link DatatypeConstants#FIELD_UNDEFINED}
     * @throws IllegalArgumentException si esta fuera de rango
     */
    public abstract void setMonth(int month);

    /**
     * El dia del mes, de 1 a 31.
     *
     * @param day de 1 a 31, o {@link DatatypeConstants#FIELD_UNDEFINED}
     * @throws IllegalArgumentException si esta fuera de rango
     */
    public abstract void setDay(int day);

    /**
     * La zona horaria, en minutos y con el mismo signo con que se escribe.
     *
     * <p>{@code -03:00} son -180 y {@code +05:30} son 330. Para ir a UTC hay que <b>restarlo</b> de
     * la hora local.
     *
     * <p>El rango admitido es de -840 a 840 inclusive, o sea de {@code -14:00} a {@code +14:00}.
     * Ojo con los nombres de las constantes, que estan al reves de los numeros:
     * {@link DatatypeConstants#MAX_TIMEZONE_OFFSET} guarda el minimo y
     * {@link DatatypeConstants#MIN_TIMEZONE_OFFSET} el maximo.
     *
     * @param offset de -840 a 840, o {@link DatatypeConstants#FIELD_UNDEFINED}
     * @throws IllegalArgumentException si esta fuera de rango
     */
    public abstract void setTimezone(int offset);

    /**
     * Hora, minuto y segundo de una.
     *
     * <p>Atajo de los tres setters; los segundos fraccionarios quedan como estaban.
     *
     * @param hour la hora
     * @param minute el minuto
     * @param second el segundo
     * @throws IllegalArgumentException si alguno esta fuera de rango
     */
    public void setTime(int hour, int minute, int second) {
        setHour(hour);
        setMinute(minute);
        setSecond(second);
    }

    /**
     * La hora, de 0 a 23.
     *
     * <p>El 24 se acepta solo en el valor lexico {@code 24:00:00}, que la implementacion normaliza
     * al dia siguiente; por esta via no.
     *
     * @param hour de 0 a 23, o {@link DatatypeConstants#FIELD_UNDEFINED}
     * @throws IllegalArgumentException si esta fuera de rango
     */
    public abstract void setHour(int hour);

    /**
     * El minuto, de 0 a 59.
     *
     * @param minute de 0 a 59, o {@link DatatypeConstants#FIELD_UNDEFINED}
     * @throws IllegalArgumentException si esta fuera de rango
     */
    public abstract void setMinute(int minute);

    /**
     * El segundo, de 0 a 60.
     *
     * <p>Sesenta, no cincuenta y nueve: XML Schema deja lugar al segundo intercalar.
     *
     * @param second de 0 a 60, o {@link DatatypeConstants#FIELD_UNDEFINED}
     * @throws IllegalArgumentException si esta fuera de rango
     */
    public abstract void setSecond(int second);

    /**
     * Los milisegundos, que son la parte fraccionaria del segundo con tres decimales.
     *
     * @param millisecond de 0 a 999, o {@link DatatypeConstants#FIELD_UNDEFINED}
     * @throws IllegalArgumentException si esta fuera de rango
     */
    public abstract void setMillisecond(int millisecond);

    /**
     * La parte fraccionaria del segundo, con la precision que sea.
     *
     * <p>Es la forma general de {@link #setMillisecond}: XML Schema no le pone limite a los
     * decimales, asi que un {@link BigDecimal} es lo unico que no pierde nada.
     *
     * @param fractional de 0 inclusive a 1 exclusive, o null para borrarlo
     * @throws IllegalArgumentException si esta fuera de ese rango
     */
    public abstract void setFractionalSecond(BigDecimal fractional);

    /**
     * Hora, minuto, segundo y fraccion de segundo.
     *
     * @param hour la hora
     * @param minute el minuto
     * @param second el segundo
     * @param fractional la fraccion, de 0 inclusive a 1 exclusive
     * @throws IllegalArgumentException si alguno esta fuera de rango
     */
    public void setTime(int hour, int minute, int second, BigDecimal fractional) {
        setHour(hour);
        setMinute(minute);
        setSecond(second);
        setFractionalSecond(fractional);
    }

    /**
     * Hora, minuto, segundo y milisegundo.
     *
     * @param hour la hora
     * @param minute el minuto
     * @param second el segundo
     * @param millisecond de 0 a 999
     * @throws IllegalArgumentException si alguno esta fuera de rango
     */
    public void setTime(int hour, int minute, int second, int millisecond) {
        setHour(hour);
        setMinute(minute);
        setSecond(second);
        setMillisecond(millisecond);
    }

    /**
     * Los miles de millones del anio, o null si el anio entra en un {@code int}.
     *
     * <p>Siempre multiplo de mil millones: la parte que no entra en {@link #getYear}.
     *
     * @return el eon, o null
     */
    public abstract BigInteger getEon();

    /**
     * El anio, sin el eon.
     *
     * @return el anio, o {@link DatatypeConstants#FIELD_UNDEFINED} si no esta puesto
     */
    public abstract int getYear();

    /**
     * El anio completo, eon incluido.
     *
     * @return el anio, o null si no esta puesto
     */
    public abstract BigInteger getEonAndYear();

    /**
     * El mes, contado desde uno.
     *
     * @return de 1 a 12, o {@link DatatypeConstants#FIELD_UNDEFINED}
     */
    public abstract int getMonth();

    /**
     * El dia del mes.
     *
     * @return de 1 a 31, o {@link DatatypeConstants#FIELD_UNDEFINED}
     */
    public abstract int getDay();

    /**
     * La zona horaria en minutos, con el mismo signo con que se escribe; ver {@link #setTimezone}.
     *
     * @return los minutos, o {@link DatatypeConstants#FIELD_UNDEFINED} si la fecha no tiene zona
     */
    public abstract int getTimezone();

    /**
     * La hora.
     *
     * @return de 0 a 23, o {@link DatatypeConstants#FIELD_UNDEFINED}
     */
    public abstract int getHour();

    /**
     * El minuto.
     *
     * @return de 0 a 59, o {@link DatatypeConstants#FIELD_UNDEFINED}
     */
    public abstract int getMinute();

    /**
     * El segundo entero; la fraccion esta en {@link #getFractionalSecond}.
     *
     * @return de 0 a 60, o {@link DatatypeConstants#FIELD_UNDEFINED}
     */
    public abstract int getSecond();

    /**
     * Los milisegundos, sacados de la fraccion de segundo.
     *
     * <p>Corre la coma tres lugares y trunca: una fraccion con mas de tres decimales pierde lo que
     * sobra, que es lo que tiene que pasar cuando el que pregunta pide milisegundos.
     *
     * @return de 0 a 999, o {@link DatatypeConstants#FIELD_UNDEFINED} si no hay fraccion
     */
    public int getMillisecond() {
        BigDecimal fractionValue = getFractionalSecond();
        if (fractionValue == null) {
            return DatatypeConstants.FIELD_UNDEFINED;
        }
        return fractionValue.movePointRight(3).intValue();
    }

    /**
     * La parte fraccionaria del segundo, con toda su precision.
     *
     * @return de 0 inclusive a 1 exclusive, o null si no esta puesta
     */
    public abstract BigDecimal getFractionalSecond();

    /**
     * Compara las dos fechas, y puede contestar que no se pueden comparar.
     *
     * <p>Los cuatro resultados son {@link DatatypeConstants#LESSER},
     * {@link DatatypeConstants#EQUAL}, {@link DatatypeConstants#GREATER} y
     * {@link DatatypeConstants#INDETERMINATE}. El ultimo aparece cuando una de las dos tiene zona
     * horaria y la otra no --y la que no la tiene podria caer de los dos lados--, o cuando les
     * faltan campos distintos.
     *
     * @param xmlGregorianCalendar la otra; no puede ser null
     * @return uno de los cuatro
     * @throws NullPointerException si es null
     */
    public abstract int compare(XMLGregorianCalendar xmlGregorianCalendar);

    /**
     * La misma fecha llevada a UTC.
     *
     * <p>Es lo que hace comparables dos fechas con zonas distintas. Una fecha <b>sin</b> zona se
     * devuelve igual: no hay a que normalizarla, y suponerle UTC seria inventar el dato que falta.
     *
     * @return una instancia nueva en UTC
     */
    public abstract XMLGregorianCalendar normalize();

    /**
     * Dos fechas son iguales si {@link #compare} dice {@link DatatypeConstants#EQUAL}.
     *
     * <p>O sea que {@code 2024-05-25T12:00:00-03:00} y {@code 2024-05-25T15:00:00Z} <b>son</b>
     * iguales aunque no tengan un solo campo en comun: son el mismo instante. Y dos fechas que
     * comparan indeterminado no son iguales.
     *
     * @param obj el otro objeto
     * @return true si es un {@code XMLGregorianCalendar} que compara igual
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof XMLGregorianCalendar)) {
            return false;
        }
        return compare((XMLGregorianCalendar) obj) == DatatypeConstants.EQUAL;
    }

    /**
     * El hash, calculado sobre la fecha normalizada a UTC.
     *
     * <p>La normalizacion no es un detalle: sin ella {@code 12:00-03:00} y {@code 15:00Z} --que son
     * iguales por {@link #equals}-- darian hashes distintos, y un {@code HashMap} perderia una de
     * las dos. Se normaliza solo cuando hay zona y no es cero, para no pagar el costo de mas.
     *
     * @return el hash
     */
    public int hashCode() {
        int timezoneValue = getTimezone();
        if (timezoneValue == DatatypeConstants.FIELD_UNDEFINED) {
            timezoneValue = 0;
        }
        XMLGregorianCalendar gc = this;
        if (timezoneValue != 0) {
            gc = normalize();
        }
        return gc.getYear() + gc.getMonth() + gc.getDay()
                + gc.getHour() + gc.getMinute() + gc.getSecond();
    }

    /**
     * La fecha en la forma lexica de XML Schema que corresponda a los campos que tenga.
     *
     * @return el texto, por ejemplo {@code 2024-05-25T12:00:00-03:00}
     * @throws IllegalStateException si los campos puestos no forman ninguno de los ocho tipos
     */
    public abstract String toXMLFormat();

    /**
     * Cual de los ocho tipos de fecha de XML Schema es esta, segun que campos tenga.
     *
     * @return uno de {@link DatatypeConstants#DATETIME}, {@link DatatypeConstants#DATE},
     *     {@link DatatypeConstants#TIME}, {@link DatatypeConstants#GYEARMONTH},
     *     {@link DatatypeConstants#GMONTHDAY}, {@link DatatypeConstants#GYEAR},
     *     {@link DatatypeConstants#GMONTH} o {@link DatatypeConstants#GDAY}
     * @throws IllegalStateException si los campos puestos no forman ninguno
     */
    public abstract QName getXMLSchemaType();

    /**
     * Lo mismo que {@link #toXMLFormat}.
     *
     * @return el texto
     * @throws IllegalStateException si los campos puestos no forman ninguno de los ocho tipos
     */
    public String toString() {
        return toXMLFormat();
    }

    /**
     * Si los campos puestos forman una fecha que existe.
     *
     * <p>Mira lo que los setters no pueden mirar de a uno: el 31 de febrero pasa los dos controles
     * de rango por separado y no es una fecha. Tambien decide sobre el anio cero, que XML Schema 1.0
     * no admite.
     *
     * @return true si es valida
     */
    public abstract boolean isValid();

    /**
     * Le suma una duracion, en el lugar.
     *
     * <p>El orden de los campos esta fijado por la especificacion --anios, meses, dias, horas,
     * minutos, segundos-- y hay un ajuste que sorprende: si sumar meses deja un dia que no existe
     * en el mes de destino, el dia se <b>recorta</b> al ultimo del mes. El 31 de enero mas un mes es
     * el 28 de febrero, no el 3 de marzo.
     *
     * @param duration la duracion a sumar; no puede ser null
     * @throws NullPointerException si es null
     */
    public abstract void add(Duration duration);

    /**
     * La misma fecha como {@link GregorianCalendar}.
     *
     * <p>Es una conversion con perdida y hay que saberlo: {@code GregorianCalendar} no tiene campos
     * ausentes, asi que los que falten se completan con los de la epoca por omision. Un
     * {@code xs:time} convertido trae una fecha que nadie puso.
     *
     * @return el calendario equivalente
     */
    public abstract GregorianCalendar toGregorianCalendar();

    /**
     * Lo mismo, eligiendo con que completar lo que falta.
     *
     * <p>Es la version honesta de la anterior: {@code defaults} dice explicitamente que valores
     * usar para los campos ausentes, en vez de que los invente la implementacion.
     *
     * @param timezone la zona a usar si esta fecha no tiene; puede ser null
     * @param aLocale la region para el calendario; puede ser null
     * @param defaults de donde sacar los campos que falten; puede ser null
     * @return el calendario equivalente
     */
    public abstract GregorianCalendar toGregorianCalendar(
            TimeZone timezone, Locale aLocale, XMLGregorianCalendar defaults);

    /**
     * La zona horaria de esta fecha como {@link TimeZone}.
     *
     * @param defaultZoneoffset que usar si esta fecha no tiene zona; puede ser
     *     {@link DatatypeConstants#FIELD_UNDEFINED}
     * @return la zona, o null si no hay ninguna ni por omision
     */
    public abstract TimeZone getTimeZone(int defaultZoneoffset);

    /**
     * Una copia independiente.
     *
     * <p>Es abstracto y no hereda el de {@link Object} porque la clase es mutable: una copia
     * superficial compartiria el estado y modificar una cambiaria la otra.
     *
     * @return la copia
     */
    public abstract Object clone();
}
