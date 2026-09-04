package jdk.internal.vm;

/**
 * KajiLibrary's jdk.internal.vm.JcmdVThreadCommands — el registro de comandos de `jcmd` sobre hilos
 * virtuales.
 *
 * <p>**No tiene ningún miembro público, y ésa es toda la clase.** En el JDK su trabajo entero lo hace
 * el inicializador estático: registra los comandos `Thread.vthread_scheduler` y `Thread.vthread_dump`
 * que la herramienta `jcmd` después invoca desde afuera del proceso. Nadie la llama desde Java.
 *
 * <p>Acá el inicializador no registra nada, porque no hay canal de diagnóstico al que registrarse ni
 * planificador de hilos virtuales que reportar. El tipo existe con la forma que el JDK declara.
 */
public class JcmdVThreadCommands {

    private JcmdVThreadCommands() {
    }
}
