package java.beans;

// Distingue "me estan editando en una herramienta" de "estoy corriendo de verdad". Un bean en
// modo diseno no deberia abrir conexiones ni arrancar hilos: se lo esta dibujando, no usando.
public interface DesignMode {

    // El nombre de la propiedad que se dispara al cambiar el modo.
    String PROPERTYNAME = "designTime";

    void setDesignTime(boolean designTime);

    boolean isDesignTime();
}
