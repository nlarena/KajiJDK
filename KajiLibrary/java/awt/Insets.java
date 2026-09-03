package java.awt;

/**
 * Los cuatro margenes de un contenedor: cuanto hay que dejar libre arriba, a la izquierda, abajo y a
 * la derecha.
 *
 * <p>No sabe nada de ventanas --son cuatro enteros publicos-- y por eso se puede escribir entera.
 * Esta aca porque {@code GridBagConstraints} la tiene como campo publico y porque es el tipo de
 * retorno de {@code Container.getInsets()}.
 *
 * <p>El {@code hashCode()} no es el obvio. El JDK usa el emparejamiento de Cantor dos veces --una
 * para (izquierda, abajo) y otra para (derecha, arriba)-- y despues una tercera sobre los dos
 * resultados. La razon es que los margenes tipicos son numeros chicos y muy repetidos: con un
 * {@code top*31+left*31...} el par (1,2) y el par (2,1) colisionarian todo el tiempo. El
 * emparejamiento de Cantor es inyectivo sobre los naturales, asi que en el rango en que se usan de
 * verdad los margenes no hay colisiones en absoluto.
 */
public class Insets implements Cloneable, java.io.Serializable {

    private static final long serialVersionUID = -2272572637695466749L;

    public int top;

    public int left;

    public int bottom;

    public int right;

    public Insets(int top, int left, int bottom, int right) {
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
    }

    public void set(int top, int left, int bottom, int right) {
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
    }

    public boolean equals(Object obj) {
        if (obj instanceof Insets) {
            Insets insets = (Insets) obj;
            return ((top == insets.top) && (left == insets.left)
                    && (bottom == insets.bottom) && (right == insets.right));
        }
        return false;
    }

    public int hashCode() {
        int sum1 = left + bottom;
        int sum2 = right + top;
        int val1 = sum1 * (sum1 + 1) / 2 + left;
        int val2 = sum2 * (sum2 + 1) / 2 + top;
        int sum3 = val1 + val2;
        return sum3 * (sum3 + 1) / 2 + val2;
    }

    public String toString() {
        return getClass().getName() + "[top=" + top + ",left=" + left
                + ",bottom=" + bottom + ",right=" + right + "]";
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // Insets implementa Cloneable, asi que esto no puede pasar.
            throw new InternalError(e);
        }
    }
}
