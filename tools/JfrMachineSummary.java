import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import jdk.jfr.consumer.RecordingFile;

/** Run with: java tools/JfrMachineSummary.java recording.jfr */
public class JfrMachineSummary {
    private record Window(Instant start, Instant end) {
        boolean contains(Instant time) { return !time.isBefore(start) && !time.isAfter(end); }
    }

    public static void main(String[] args) throws Exception {
        for (String filename : args) summarize(Path.of(filename));
    }

    private static void summarize(Path file) throws Exception {
        List<RecordedEvent> events = RecordingFile.readAllEvents(file);
        List<Window> windows = new ArrayList<>();
        System.out.println(file);
        for (RecordedEvent event : events) {
            if (event.getEventType().getName().equals("quarryplus.MachineWork")) {
                windows.add(new Window(event.getStartTime(), event.getEndTime()));
                System.out.printf("%s operations=%d ms=%.3f allocatedMiB=%.3f%n", event.getString("workload"),
                        event.getInt("operations"), event.getLong("elapsedNanos") / 1e6,
                        event.getLong("allocatedBytes") / 1048576.0);
            }
        }
        Map<String, Long> allocations = new HashMap<>(), sites = new HashMap<>(), cpu = new HashMap<>();
        Map<String, Long> allocationStacks = new HashMap<>();
        int gc = 0;
        double pauses = 0;
        for (RecordedEvent event : events) {
            if (windows.stream().noneMatch(window -> window.contains(event.getStartTime()))) continue;
            String type = event.getEventType().getName();
            if (type.equals("jdk.GarbageCollection")) gc++;
            if (type.equals("jdk.GCPhasePause")) pauses += event.getDuration().toNanos() / 1e6;
            RecordedThread thread = type.equals("jdk.ExecutionSample") ? event.getThread("sampledThread") : event.getThread();
            if (thread == null || !"Server thread".equals(thread.getJavaName())) continue;
            if (type.equals("jdk.ObjectAllocationSample")) {
                long weight = event.getLong("weight");
                allocations.merge(event.getClass("objectClass").getName(), weight, Long::sum);
                sites.merge(aemFrame(event), weight, Long::sum);
                if (Boolean.getBoolean("aem.profile.stacks") && event.getStackTrace() != null) {
                    String stack = event.getClass("objectClass").getName() + "\n" + event.getStackTrace().getFrames().stream()
                            .limit(12).map(frame -> "  " + frame.getMethod().getType().getName() + "." + frame.getMethod().getName())
                            .collect(java.util.stream.Collectors.joining("\n"));
                    allocationStacks.merge(stack, weight, Long::sum);
                }
            } else if (type.equals("jdk.ExecutionSample")) {
                cpu.merge(aemFrame(event), 1L, Long::sum);
            }
        }
        System.out.printf("GC events starting within work windows=%d, pauseMs=%.3f%n", gc, pauses);
        print("Allocation samples by class (weighted bytes)", allocations);
        print("Allocation samples by AEM caller (weighted bytes)", sites);
        print("CPU samples by AEM caller", cpu);
        if (!allocationStacks.isEmpty()) print("Allocation stacks (weighted bytes)", allocationStacks);
    }

    private static String aemFrame(RecordedEvent event) {
        if (event.getStackTrace() != null) {
            for (var frame : event.getStackTrace().getFrames()) {
                var method = frame.getMethod();
                String name = method.getType().getName();
                if (name.startsWith("com.yogpc.qp.")) return name + "." + method.getName();
            }
        }
        return "(no AEM frame in recorded stack)";
    }

    private static void print(String title, Map<String, Long> values) {
        System.out.println(title);
        values.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(12)
                .forEach(entry -> System.out.println(entry.getValue() + " " + entry.getKey()));
    }
}
