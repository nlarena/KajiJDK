package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.SetOfIntegerSyntax;
import javax.print.attribute.SupportedValuesAttribute;

/**
 * Que valores de {@link JobMediaSheets} acepta la impresora.
 *
 * <p>Ver la cabecera de familia en {@link CopiesSupported} para el mecanismo. A diferencia de
 * aquel el minimo legal es cero, por la misma razon que en {@link JobMediaSheets}: es una medida, no una
 * cantidad que se pide.
 *
 * <p>Solo tiene el constructor de rango. Un unico valor soportado se declara con los dos extremos
 * iguales.
 */
public final class JobMediaSheetsSupported extends SetOfIntegerSyntax implements SupportedValuesAttribute {

    private static final long serialVersionUID = 2953685470388672940L;

    public JobMediaSheetsSupported(int lowerBound, int upperBound) {
        super(lowerBound, upperBound);
        if (lowerBound > upperBound) {
            throw new IllegalArgumentException("Null range specified");
        } else if (lowerBound < 0) {
            throw new IllegalArgumentException("Job media sheets value < 0 specified");
        }
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof JobMediaSheetsSupported;
    }

    public final Class<? extends Attribute> getCategory() {
        return JobMediaSheetsSupported.class;
    }

    public final String getName() {
        return "job-media-sheets-supported";
    }
}
