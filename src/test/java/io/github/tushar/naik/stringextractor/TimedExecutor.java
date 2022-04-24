package io.github.tushar.naik.stringextractor;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * A Class that will wrap any {@link Function} with a custom timer, in a fancy way :P
 */
public class TimedExecutor {

    /**
     * this method will execute the function and calculate the time that the function took to respond
     *
     * @param function function that needs to be executed
     * @param arg      arguments of the function
     * @param <T>      Type of Arguments
     * @param <R>      Return type of function
     * @return TimedResponse which contains the response and the time the function took to execute the response
     */
    public static <T, R> TimedResponse<R> execute(Function<T, R> function, T arg) {
        final Instant start = Instant.now();
        final R apply = function.apply(arg);
        return new TimedResponse<>(getElapsedDuration(start).toMillis(), apply);
    }

    /**
     * this method will execute the callable and calculate the time that the callable took to respond
     *
     * @param callable callable that needs to be executed
     * @param <V>      Return type of callable
     * @return TimedResponse which contains the response and the time the callable took to execute the response
     */
    public static <V> TimedResponse<V> execute(Callable<V> callable) throws Exception {
        final Instant start = Instant.now();
        final V apply = callable.call();
        return new TimedResponse<>(getElapsedDuration(start).toMillis(), apply);
    }


    /**
     * get elapsed duration between an older instant and now
     *
     * @param instant older instant
     * @return duration
     */
    public static Duration getElapsedDuration(Instant instant) {
        return Duration.between(instant, Instant.now());
    }
}