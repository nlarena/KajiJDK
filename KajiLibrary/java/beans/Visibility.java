package java.beans;

// Como un bean negocia con su entorno si puede contar con una interfaz grafica. Un bean que corre
// en un servidor sin pantalla necesita enterarse para no intentar dibujarse.
//
// Los cuatro metodos son dos preguntas y dos avisos: needsGui/avoidingGui las contesta el bean,
// dontUseGui/okToUseGui se las dice el entorno.
public interface Visibility {

    // Si el bean NO puede funcionar sin interfaz grafica.
    boolean needsGui();

    // El entorno le avisa que no la use, aunque la haya.
    void dontUseGui();

    // El entorno le avisa que puede usarla.
    void okToUseGui();

    // Si el bean esta evitando usarla ahora mismo.
    boolean avoidingGui();
}
