package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/*
 * CABECERA DE FAMILIA -- los atributos {@code EnumSyntax} de este paquete.
 *
 * <p>Dos tercios de {@code javax.print.attribute.standard} son la misma clase escrita muchas
 * veces: un entero con nombre. El mecanismo esta entero en
 * {@link javax.print.attribute.EnumSyntax EnumSyntax} y cada subclase solo lo parametriza con tres
 * cosas.
 *
 * <ul>
 * <li>{@code getStringTable()} -- el nombre IPP de cada valor. Es lo que imprime
 *     {@code toString()}, y el JDK lo especifica al caracter: {@code "two-sided-long-edge"}, no
 *     {@code "TWO_SIDED_LONG_EDGE"}. Una entrada puede ser {@code null} cuando IPP reservo un
 *     numero que Java no expone; ahi {@code toString()} cae al entero pelado.</li>
 * <li>{@code getEnumValueTable()} -- las constantes en el mismo orden, para que
 *     {@code readResolve()} pueda convertir un entero de vuelta en <em>la</em> constante y que
 *     {@code ==} siga funcionando despues de un viaje por un stream.</li>
 * <li>{@code getOffset()} -- el entero de la primera fila. Vale cero salvo donde IPP arranco la
 *     numeracion en 3 ({@code Finishings}, {@code OrientationRequested}, {@code PrintQuality}).</li>
 * </ul>
 *
 * <p>Los valores son <b>singletons</b>: el constructor es {@code protected} para que una impresora
 * pueda declarar valores propios, pero nadie fabrica los estandar dos veces. Por eso
 * {@code equals()} se hereda de {@code Object} --identidad-- salvo en {@link Media}, donde hace
 * falta comparar tambien la clase concreta.
 *
 * <p>Las tablas de nombres son <b>datos de norma</b> (RFC 2911 / IPP), no de locale: no dependen
 * del CLDR ni de ninguna impresora, asi que van completas.
 */

/**
 * Si el trabajo se imprime en color o en blanco y negro.
 *
 * <p>Es una peticion sobre el <em>documento</em>, no sobre la impresora: {@code MONOCHROME} sobre
 * una impresora color le pide que no use tinta de color, y no dice nada sobre lo que la impresora
 * puede hacer --eso lo contesta {@link ColorSupported}.
 */
public final class Chromaticity extends EnumSyntax implements DocAttribute, PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = 4660543931355214012L;

    public static final Chromaticity MONOCHROME = new Chromaticity(0);

    public static final Chromaticity COLOR = new Chromaticity(1);

    private static final String[] myStringTable = {
        "monochrome",
        "color",
    };

    private static final Chromaticity[] myEnumValueTable = {
        MONOCHROME,
        COLOR,
    };

    protected Chromaticity(int value) {
        super(value);
    }

    protected String[] getStringTable() {
        return myStringTable;
    }

    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    public final Class<? extends Attribute> getCategory() {
        return Chromaticity.class;
    }

    public final String getName() {
        return "chromaticity";
    }
}
