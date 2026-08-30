public interface QualI<E> {
    // Homonimos en la propia interfaz.
    static <E> QualI<E> of(E a, E b) { return null; }

    // Deberia llamar a QualC.of(Object[], int). Si resuelve contra QualI.of(E,E), recursa.
    static <E> QualI<E> of() {
        return QualC.of(new Object[0], 0);
    }
}
