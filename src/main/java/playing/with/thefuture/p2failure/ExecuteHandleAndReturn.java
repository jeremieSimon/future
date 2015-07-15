package playing.with.thefuture.p2failure;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.base.Stopwatch;

/**
 * Same situation as ExecuteHandleAndBlock.
 * Here the retry logic is directly handled inside the task.
 * The task is constantly rerunning itself.
 * It has the advantage to make the function executeDangerousTasks much cleaner and non-blocking.
 * The problem with that approach is that if you're having some network issues,
 * retrying right after a failure might not help.
 * Usually we want to wait a little bit before retrying
 * eg: https://en.wikipedia.org/wiki/Exponential_backoff
 */
public class ExecuteHandleAndReturn {

    public static void main(final String[] args) throws InterruptedException, ExecutionException {

        final Stopwatch stopwatch = Stopwatch.createStarted();

        final int numberOfTasks = 100;
        final ExecutorService service = Executors.newFixedThreadPool(5);

        // populate with dummy tasks
        // here, a task has a change to fail
        final Queue<Callable<Boolean>> tasks = new LinkedList<>();
        final CountDownLatch latch = new CountDownLatch(numberOfTasks);
        for (int i = 0; i < numberOfTasks; i++) {
            tasks.add(newCallable(latch));
        }

        // run blocking execution
        final List<Future<Boolean>> futures = executeDangerousTasks(service, tasks);

        System.out.println("futures received "
                + stopwatch.elapsed(TimeUnit.MILLISECONDS)
                + " size " + futures.size());
        latch.await();
        System.out.println("done: " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
        service.shutdown();
    }

    /**
     * Block until all tasks have succeeded.
     * @param service
     * @param tasks
     * @return
     * @throws InterruptedException
     */
    public static List<Future<Boolean>> executeDangerousTasks(
            final ExecutorService service,
            final Queue<Callable<Boolean>> tasks)
            throws InterruptedException {

        return tasks.stream()
            .map(t -> service.submit(t))
            .collect(Collectors.toList());
    }

    // on each failure, keep retrying.
    static Callable<Boolean> newCallable(final CountDownLatch latch) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                try {
                    if (new Random().nextBoolean()) {
                        throw new Exception();
                    }
                } catch (final Throwable t) {
                    return call();
                }
             Thread.sleep(100);
             latch.countDown();
            return new Random().nextBoolean() ? true : false;
            }
        };
    }
}
