package jdk.internal.apt;

import javax.lang.model.element.Name;

// La reificación de un **nombre** del modelo `javax.lang.model` (`javax.lang.model.element.Name`):
// una envoltura mínima sobre el `String` que el VM ya internó. El contrato de `Element` devuelve
// `Name` (un `CharSequence` con `contentEquals`), no `String`, así que `SymElement.getSimpleName`
// / `getQualifiedName` construyen uno de estos: el native aloca el objeto y le escribe el campo
// `value` (el `String` internado), y los accesores de `CharSequence` delegan en ese `String`.
public final class SymName implements Name {
    // El texto del nombre (`String` internado). Lo escribe el VM al construir el objeto.
    String value;

    public int length() {
        return value.length();
    }

    public char charAt(int index) {
        return value.charAt(index);
    }

    public CharSequence subSequence(int start, int end) {
        return value.subSequence(start, end);
    }

    public String toString() {
        return value;
    }

    // Igualdad de contenido con cualquier `CharSequence` (contrato de `Name`): compara el texto.
    // No hay `String.contentEquals` en KajiLibrary, así que se materializa la otra secuencia con
    // `toString()` (parte del contrato de `CharSequence`) y se compara por `String.equals`.
    public boolean contentEquals(CharSequence cs) {
        return value.equals(cs.toString());
    }
}
