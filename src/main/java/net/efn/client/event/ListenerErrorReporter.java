package net.efn.client.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.IdentityHashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
final class ListenerErrorReporter {

    private static final int FULL_TRACE_LIMIT = 4;

    private static final int SUMMARY_INTERVAL = 256;

    private static final Map<ListenerHandle<?>, Integer> failureCounts = new IdentityHashMap<>();

    private ListenerErrorReporter() {}

    static synchronized void report(
            ListenerHandle<?> listenerHandle, Event event, Exception exception) {
        int merge = failureCounts.merge(listenerHandle, 1, Integer::sum);
        String string =
                listenerHandle.owner() == null
                        ? "<static>"
                        : listenerHandle.owner().getClass().getName();
        if (merge <= 4) {
            System.err.println(
                    "[EfnEventBus] Listener threw (failure #"
                            + merge
                            + "): owner="
                            + string
                            + " event="
                            + event.getClass().getSimpleName()
                            + " stage="
                            + listenerHandle.stage()
                            + " priority="
                            + listenerHandle.priority());
            exception.printStackTrace();
        } else if (merge % 256 == 0) {
            System.err.println(
                    "[EfnEventBus] Listener still failing ("
                            + merge
                            + " total): owner="
                            + string
                            + " event="
                            + event.getClass().getSimpleName()
                            + ": "
                            + exception);
        }
    }
}
