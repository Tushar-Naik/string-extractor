package io.github.tushar.naik.stringextractor;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class PerformanceEvaluator {
    private final MetricRegistry metricRegistry;
    private final ConsoleReporter reporter;

    public PerformanceEvaluator() {
        this.metricRegistry = new MetricRegistry();
        this.reporter = ConsoleReporter.forRegistry(metricRegistry).convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS).build();
    }

    /**
     * evaluate the time consumed by a runnable to perform x operations
     *
     * @param numOperations number of times the runnable is to be executed
     * @param runnable      runnable to be executed
     * @return time the runnable takes to run numOperations times
     */
    public Timer evaluate(long numOperations, Runnable runnable) {
        return evaluate(numOperations, runnable, "perf");
    }

    public Timer evaluate(long numOperations, Runnable runnable, String name) {
        Timer timer = metricRegistry.timer(name);
        Instant start = Instant.now();
        for (long i = 0; i < numOperations; i++) {
            printStatus(numOperations, i);
            try {
                timer.time(() -> {
                    runnable.run();
                    return true;
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println();
        final long l = TimedExecutor.getElapsedDuration(start).toMillis();
        reporter.report();
        System.out.println("Total time:" + l + "ms");
        return timer;
    }

    /**
     * evaluate the time consumed by a runnable to perform x operations
     *
     * @param numOperations number of times the runnable is to be executed
     * @param runnable      runnable to be executed
     * @return time the runnable takes to run numOperations times
     */
    public long evaluateTime(long numOperations, Runnable runnable) {
        Instant start = Instant.now();
        for (long i = 0; i < numOperations; i++) {
            printStatus(numOperations, i);
            runnable.run();
        }
        System.out.println();
        return TimedExecutor.getElapsedDuration(start).toMillis();
    }

    /**
     * evaluate the average time consumed by a runnable to perform x operations
     *
     * @param numOperations number of times the runnable is to be executed
     * @param runnable      runnable to be executed
     * @return average time the runnable takes to run numOperations times
     */
    public float evaluateAndAvg(long numOperations, Runnable runnable) {
        return (float) evaluateTime(numOperations, runnable) / numOperations;
    }

    /**
     * evaluate the time consumed by a supplier to supply x times
     *
     * @param numOperations number of times the runnable is to be executed
     * @param supplier      supplier
     * @param <V>           type of value
     * @return time the supplier takes to supply numOperations times
     */
    public <V> TimedResponse<V> evaluate(int numOperations, Supplier<V> supplier) {
        Instant start = Instant.now();
        V result = null;
        for (int i = 0; i < numOperations; i++) {
            printStatus(numOperations, i);
            result = supplier.get();
        }
        System.out.println();
        final long l = TimedExecutor.getElapsedDuration(start).toMillis();
        return new TimedResponse<>(l, result);
    }

    /**
     * evaluate the average time consumed by a supplier to supply x times
     *
     * @param numOperations number of times the runnable is to be executed
     * @param supplier      supplier
     * @param <V>           type of value
     * @return average time the supplier takes to supply numOperations times
     */
    public <V> TimedResponse<V> evaluateAndAvg(int numOperations, Supplier<V> supplier) {
        TimedResponse<V> evaluate = evaluate(numOperations, supplier);
        return new TimedResponse<>(evaluate.time / numOperations, evaluate.response);
    }

    private void printStatus(long numOperations, long iteration) {
        if (numOperations < 100) {
            final long i1 = (100 / numOperations);
            for (long j = 0; j < i1; j++) {
                System.out.print("#");
            }
        } else {
            final long i1 = (numOperations / 100);
            if (iteration % i1 == 0) {
                System.out.print("#");
            }
        }
    }
}
