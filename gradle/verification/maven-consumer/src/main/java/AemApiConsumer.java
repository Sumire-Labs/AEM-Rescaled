import com.yogpc.qp.modules.RepeatTickModule;

/** Compile an actual AEM API call, including a Scala collection in its public signature. */
public final class AemApiConsumer {
    public static boolean hasNoTickActions() {
        return new RepeatTickModule(1).calledWhen().isEmpty();
    }
}
