// Finding #9 — el override covariante genérico no sustituye las variables de tipo.
// Para `class MyList<E> implements List<E>` con `E get(int)`, el chequeo compara el `E` de
// MyList contra el `E` de List sin ligar el parámetro de tipo de List a `E`, y falla.
//
// Esperado (javac real): OK.
// Síntoma del bug:       "el retorno de `get` no es compatible con el de List: E no es un subtipo de E".
// Familia: #5 / #7 / #9. Estado vivo confirmado: SIGUE FALLANDO (✗).
//
// Repro: cargo run -- --emit KajiLibrary/repros/finding_09.java
package java.util;

import java.util.List;

public class MyList<E> implements List<E> {
    public E get(int index) { return null; }
}
