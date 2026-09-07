package com.sun.jdi.request;

import com.sun.jdi.Field;
import com.sun.jdi.Location;
import com.sun.jdi.Mirror;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import java.util.List;

/**
 * De donde salen todos los pedidos, y quien lleva la lista de los que hay.
 *
 * <p>Se obtiene de {@code VirtualMachine.eventRequestManager()}. Hay un {@code createXxx} por tipo
 * de evento y un {@code xxxRequests()} para enumerar los vivos.
 *
 * <p>{@link #deleteEventRequest} no es opcional: los pedidos viven en la otra VM y cuestan ahi. Un
 * depurador que crea pedidos y no los borra degrada al programa depurado.
 *
 * @since 1.3
 */
public interface EventRequestManager extends Mirror {

    /**
     * Crea un class prepare request.
     *
     * @return el resultado
     */
    ClassPrepareRequest createClassPrepareRequest();

    /**
     * Crea un class unload request.
     *
     * @return el resultado
     */
    ClassUnloadRequest createClassUnloadRequest();

    /**
     * Crea un thread start request.
     *
     * @return el resultado
     */
    ThreadStartRequest createThreadStartRequest();

    /**
     * Crea un thread death request.
     *
     * @return el resultado
     */
    ThreadDeathRequest createThreadDeathRequest();

    /**
     * Crea un exception request.
     *
     * @param type el ReferenceType
     * @param flag el boolean
     * @param flag2 el boolean
     * @return el resultado
     */
    ExceptionRequest createExceptionRequest(ReferenceType type, boolean flag, boolean flag2);

    /**
     * Crea un method entry request.
     *
     * @return el resultado
     */
    MethodEntryRequest createMethodEntryRequest();

    /**
     * Crea un method exit request.
     *
     * @return el resultado
     */
    MethodExitRequest createMethodExitRequest();

    /**
     * Crea un monitor contended enter request.
     *
     * @return el resultado
     */
    MonitorContendedEnterRequest createMonitorContendedEnterRequest();

    /**
     * Crea un monitor contended entered request.
     *
     * @return el resultado
     */
    MonitorContendedEnteredRequest createMonitorContendedEnteredRequest();

    /**
     * Crea un monitor wait request.
     *
     * @return el resultado
     */
    MonitorWaitRequest createMonitorWaitRequest();

    /**
     * Crea un monitor waited request.
     *
     * @return el resultado
     */
    MonitorWaitedRequest createMonitorWaitedRequest();

    /**
     * Crea un step request.
     *
     * @param thread el ThreadReference
     * @param index el int
     * @param index2 el int
     * @return el resultado
     */
    StepRequest createStepRequest(ThreadReference thread, int index, int index2);

    /**
     * Crea un breakpoint request.
     *
     * @param location el Location
     * @return el resultado
     */
    BreakpointRequest createBreakpointRequest(Location location);

    /**
     * Crea un access watchpoint request.
     *
     * @param field el Field
     * @return el resultado
     */
    AccessWatchpointRequest createAccessWatchpointRequest(Field field);

    /**
     * Crea un modification watchpoint request.
     *
     * @param field el Field
     * @return el resultado
     */
    ModificationWatchpointRequest createModificationWatchpointRequest(Field field);

    /**
     * Crea un v m death request.
     *
     * @return el resultado
     */
    VMDeathRequest createVMDeathRequest();

    /**
     * El delete event request.
     *
     * @param request el EventRequest
     */
    void deleteEventRequest(EventRequest request);

    /**
     * El delete event requests.
     *
     * @param values el List<? extends EventRequest>
     */
    void deleteEventRequests(List<? extends EventRequest> values);

    /**
     * El delete all breakpoints.
     */
    void deleteAllBreakpoints();

    /**
     * El step requests.
     *
     * @return el resultado
     */
    List<StepRequest> stepRequests();

    /**
     * El class prepare requests.
     *
     * @return el resultado
     */
    List<ClassPrepareRequest> classPrepareRequests();

    /**
     * El class unload requests.
     *
     * @return el resultado
     */
    List<ClassUnloadRequest> classUnloadRequests();

    /**
     * El thread start requests.
     *
     * @return el resultado
     */
    List<ThreadStartRequest> threadStartRequests();

    /**
     * El thread death requests.
     *
     * @return el resultado
     */
    List<ThreadDeathRequest> threadDeathRequests();

    /**
     * El exception requests.
     *
     * @return el resultado
     */
    List<ExceptionRequest> exceptionRequests();

    /**
     * El breakpoint requests.
     *
     * @return el resultado
     */
    List<BreakpointRequest> breakpointRequests();

    /**
     * El access watchpoint requests.
     *
     * @return el resultado
     */
    List<AccessWatchpointRequest> accessWatchpointRequests();

    /**
     * El modification watchpoint requests.
     *
     * @return el resultado
     */
    List<ModificationWatchpointRequest> modificationWatchpointRequests();

    /**
     * El method entry requests.
     *
     * @return el resultado
     */
    List<MethodEntryRequest> methodEntryRequests();

    /**
     * El method exit requests.
     *
     * @return el resultado
     */
    List<MethodExitRequest> methodExitRequests();

    /**
     * El monitor contended enter requests.
     *
     * @return el resultado
     */
    List<MonitorContendedEnterRequest> monitorContendedEnterRequests();

    /**
     * El monitor contended entered requests.
     *
     * @return el resultado
     */
    List<MonitorContendedEnteredRequest> monitorContendedEnteredRequests();

    /**
     * El monitor wait requests.
     *
     * @return el resultado
     */
    List<MonitorWaitRequest> monitorWaitRequests();

    /**
     * El monitor waited requests.
     *
     * @return el resultado
     */
    List<MonitorWaitedRequest> monitorWaitedRequests();

    /**
     * El vm death requests.
     *
     * @return el resultado
     */
    List<VMDeathRequest> vmDeathRequests();
}
