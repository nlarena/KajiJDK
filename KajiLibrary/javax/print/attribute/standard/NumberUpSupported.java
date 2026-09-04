package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.SetOfIntegerSyntax;
import javax.print.attribute.SupportedValuesAttribute;

/**
 * Que valores de {@link NumberUp} acepta la impresora.
 *
 * <p>Ver la cabecera de familia en {@link CopiesSupported} para el mecanismo. Este es el caso donde
 * el conjunto gana de verdad: lo tipico es soportar 1, 2, 4, 6, 9 y 16 --las potencias y los
 * cuadrados que parten bien la hoja-- y no un rango corrido, asi que hace falta el constructor de
 * {@code int[][]}.
 *
 * <p>El minimo es 1, igual que en {@link NumberUp}.
 */
public final class NumberUpSupported extends SetOfIntegerSyntax
    implements SupportedValuesAttribute {

    private static final long serialVersionUID = -1041573395759141805L;

    /**
     * Los rangos crudos, en cualquier orden y con solapamientos: la base los canonicaliza antes de
     * que este constructor los revise.
     */
    public NumberUpSupported(int[][] members) {
        super(members);
        if (members == null) {
            throw new NullPointerException("members is null");
        }
        int[][] myMembers = getMembers();
        int n = myMembers.length;
        if (n == 0) {
            throw new IllegalArgumentException("members is zero-length");
        }
        for (int i = 0; i < n; i++) {
            if (myMembers[i][0] < 1) {
                throw new IllegalArgumentException("Number up value must be > 0");
            }
        }
    }

    public NumberUpSupported(int member) {
        super(member);
        if (member < 1) {
            throw new IllegalArgumentException("Number up value must be > 0");
        }
    }

    public NumberUpSupported(int lowerBound, int upperBound) {
        super(lowerBound, upperBound);
        if (lowerBound > upperBound) {
            throw new IllegalArgumentException("Null range specified");
        } else if (lowerBound < 1) {
            throw new IllegalArgumentException("Number up value must be > 0");
        }
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof NumberUpSupported;
    }

    public final Class<? extends Attribute> getCategory() {
        return NumberUpSupported.class;
    }

    public final String getName() {
        return "number-up-supported";
    }
}
