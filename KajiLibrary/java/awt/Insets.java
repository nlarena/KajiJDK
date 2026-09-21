package java.awt;

/**
 * The four insets of a container: how much has to be left free at the top, left, bottom and right.
 *
 * <p>It knows nothing about windows --four public integers-- and that is why it could be written
 * whole. It is here because {@code GridBagConstraints} has it as a public field and because it is
 * the return type of {@code Container.getInsets()}.
 *
 * <p>The {@code hashCode()} is not the obvious one. The JDK uses the Cantor pairing twice --once
 * for (left, bottom) and once for (right, top)-- and then a third time over the two results. The
 * reason is that typical insets are small, often repeated numbers: with a plain weighted sum,
 * swapped values would collide all the time. The Cantor pairing is injective over the naturals, so
 * in the range insets are really used in there are no collisions at all.
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
            // Insets implements Cloneable, so this cannot happen.
            throw new InternalError(e);
        }
    }
}
