package java.lang.foreign;

import java.util.List;

/**
 * KajiLibrary's java.lang.foreign.GroupLayout -- varios layouts juntos.
 *
 * <p>Lo comun a un `struct` y a una `union`; lo que los diferencia es **donde empieza cada
 * miembro**, y eso lo deciden {@link StructLayout} y {@link UnionLayout}. Esta interfaz solo dice
 * que hay miembros.
 */
public interface GroupLayout extends MemoryLayout {

    /** Los miembros, en orden. */
    List<MemoryLayout> memberLayouts();

    GroupLayout withName(String name);

    GroupLayout withoutName();

    GroupLayout withByteAlignment(long byteAlignment);
}
