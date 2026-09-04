package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.SetOfIntegerSyntax;
import javax.print.attribute.SupportedValuesAttribute;

/*
 * CABECERA DE FAMILIA -- los atributos {@code SetOfIntegerSyntax} de este paquete.
 *
 * <p>Estos no llevan un numero sino un <b>conjunto</b> de numeros, guardado como lista de rangos.
 * Toda la aritmetica --ordenar, fusionar los rangos que se tocan, descartar los vacios-- esta en
 * {@link javax.print.attribute.SetOfIntegerSyntax SetOfIntegerSyntax}, que canonicaliza en el
 * constructor. Lo unico que agrega cada subclase de aca es <b>que valores son legales</b>, y lo
 * hace despues de llamar a {@code super}, mirando el resultado ya canonico.
 *
 * <p>Ese orden importa y es observable: {@code new PageRanges("5-1")} no falla por el 5 ni por el
 * 1 sino porque {@code 5-1} es un rango vacio, la canonicalizacion lo descarta y queda un conjunto
 * sin elementos --que es lo que la subclase rechaza. Por eso el mensaje habla de longitud cero y
 * no de un valor fuera de rango.
 *
 * <p>Cinco de las seis son atributos de <em>valores soportados</em>: la respuesta de la impresora a
 * "que numeros puedo pedir", el conjunto que corresponde a un {@code IntegerSyntax} suelto
 * ({@link CopiesSupported} contra {@link Copies}). La sexta, {@link PageRanges}, no: esa es una
 * peticion, y es la unica de la familia que ademas se puede construir desde texto.
 */

/**
 * Que cantidades de {@link Copies} acepta la impresora.
 *
 * <p>Rara vez es un rango corrido de verdad: una impresora que soporta de 1 a 99 lo dice asi, pero
 * el conjunto existe para las que solo aceptan algunos valores sueltos.
 */
public final class CopiesSupported extends SetOfIntegerSyntax implements SupportedValuesAttribute {

    private static final long serialVersionUID = 6927711687034846001L;

    /** El conjunto de un solo elemento: la impresora acepta exactamente esa cantidad. */
    public CopiesSupported(int member) {
        super(member);
        if (member < 1) {
            throw new IllegalArgumentException("Copies value < 1 specified");
        }
    }

    public CopiesSupported(int lowerBound, int upperBound) {
        super(lowerBound, upperBound);
        if (lowerBound > upperBound) {
            throw new IllegalArgumentException("Null range specified");
        } else if (lowerBound < 1) {
            throw new IllegalArgumentException("Copies value < 1 specified");
        }
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof CopiesSupported;
    }

    public final Class<? extends Attribute> getCategory() {
        return CopiesSupported.class;
    }

    public final String getName() {
        return "copies-supported";
    }
}
