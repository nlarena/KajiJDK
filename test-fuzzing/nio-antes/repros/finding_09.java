// Repro de #09 - el override covariante generico no sustituia las variables de tipo.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_09.java
//
// ANTES: para `class MyList<E> implements List<E>` con `E get(int)`, el chequeo comparaba el `E`
// de MyList contra el `E` de List **sin ligar** el parametro de tipo de List, y fallaba:
//
//   error: el retorno de `get` no es compatible con el de List: E no es un subtipo de E
//
// Un mensaje que se contradice solo — "E no es un subtipo de E" — y que era la pista de que los
// dos `E` eran simbolos distintos sin sustituir. Familia de #5 / #7 / #9.
//
// AHORA: **compila**. Comprobado aparte con `abstract class X<E> implements List<E>` y
// `public E get(int)`: pasa sin una queja.
//
// OJO, que este archivo tuvo que cambiar: `MyList` se declara `abstract`, por lo mismo que en
// #04 — el chequeo de completitud (#08) hoy funciona y `List` crecio, asi que la clase concreta
// era rechazada por no implementarla entera, tapando lo que el archivo viene a probar.
//
// Queda como REGRESION del override covariante generico.
package java.util;

import java.util.List;

public abstract class MyList<E> implements List<E> {
    public E get(int index) { return null; }
}
