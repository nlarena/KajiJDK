package com.sun.java.accessibility.util;

import java.awt.Window;
import java.util.EventListener;

/**
 * Se entera cuando aparece o desaparece una ventana de primer nivel.
 *
 * <h2>Por que solo las de primer nivel</h2>
 *
 * <p>Porque son las raices del arbol de accesibilidad. Una tecnologia de asistencia recorre desde
 * ahi hacia abajo, asi que enterarse de las ventanas alcanza para saber que arboles hay — y
 * suscribirse a cada componente seria imposible en una aplicacion de tamano real.
 *
 * <p>El aviso de destruccion importa tanto como el de creacion: sin el, un lector de pantalla
 * conservaria referencias a arboles que ya no existen.
 */
public interface TopLevelWindowListener extends EventListener {

    /** Aparecio una ventana de primer nivel. */
    void topLevelWindowCreated(Window w);

    /** Desaparecio una. */
    void topLevelWindowDestroyed(Window w);
}
