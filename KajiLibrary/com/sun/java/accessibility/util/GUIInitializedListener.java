package com.sun.java.accessibility.util;

import java.util.EventListener;

/**
 * Se entera cuando la interfaz grafica ya existe.
 *
 * <h2>Por que hace falta esperar</h2>
 *
 * <p>Una tecnologia de asistencia —un lector de pantalla— arranca <strong>antes</strong> que la
 * aplicacion tenga ventanas: la VM la carga al principio, y en ese momento no hay nada que leer.
 * Consultar la interfaz ahi da vacio, y volver a consultar en un bucle es desperdiciar tiempo.
 *
 * <p>Este aviso resuelve eso: llega una sola vez, cuando aparece la primera ventana de primer nivel.
 * Registrarse despues de que eso ya paso no da nada, y por eso conviene consultar antes
 * {@link EventQueueMonitor#isGUIInitialized}.
 */
public interface GUIInitializedListener extends EventListener {

    /** Ya hay interfaz grafica. */
    void guiInitialized();
}
