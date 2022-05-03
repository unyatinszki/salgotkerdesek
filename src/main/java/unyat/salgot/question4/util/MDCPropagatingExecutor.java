package unyat.salgot.question4.util;

import org.slf4j.MDC;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public class MDCPropagatingExecutor implements Executor {

    private final ExecutorService targetExecutor;

    private MDCPropagatingExecutor(ExecutorService targetExecutor) {
        this.targetExecutor = targetExecutor;
    }

    public static MDCPropagatingExecutor of(ExecutorService targetExecutor) {
        return new MDCPropagatingExecutor(targetExecutor);
    }

    @Override
    public void execute(Runnable command) {
        var mdcContext = MDC.getCopyOfContextMap();
        Runnable delegate = () -> {
            MDC.setContextMap(mdcContext);
            try {
                command.run();
            } finally {
                MDC.clear();
            }
        };
        targetExecutor.execute(delegate);
    }

    public void shutdown() {
        targetExecutor.shutdown();
    }
}
