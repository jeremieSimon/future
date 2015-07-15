package playing.with.thefuture.p2failure;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Here we simulate some possible failure in the task.
 * On failure of the task, we want to rerun until the task is successful.
 * There are several strategies to do so.
 * Here we use a blocking strategy.
 * We execute all the dangerous tasks and only return when we see that all tasks ran successfully.
 * To do so, we attach a callback to each task.
 * OnSuccess of the task, we decrement a latch.
 * OnFailure of the task, we put back the task in a queue.
 *
 * This approach has several drawbacks.
 * 1. The code responsible for execution of the task is bit tricky.
 * 2. It blocks the caller.
 */
public class ExecuteHandleAndBlock {

    public static void main(final String[] args) throws InterruptedException {
        final int numberOfTasks = 100;

        // populate with dummy tasks
        // here, a task has a chance to fail
        final Queue<Callable<Boolean>> tasks = new LinkedList<>();
        for (int i = 0; i < numberOfTasks; i++) {
            tasks.add(() -> {
                if (new Random().nextBoolean()) throw new Exception();
                return new Random().nextBoolean() ? true: false;});
        }

        // run blocking execution
        executeDangerousTasks(tasks)
            .forEach((b) -> System.out.println("status of the task " + b));
    }

    /**
     * Block until all tasks have succeeded.
     * @param service
     * @param tasks
     * @return
     * @throws InterruptedException
     */
    public static Collection<Boolean> executeDangerousTasks(
            final Queue<Callable<Boolean>> tasks)
            throws InterruptedException {

        final ListeningExecutorService service = MoreExecutors
                .listeningDecorator(Executors.newFixedThreadPool(5));
        final CountDownLatch latch = new CountDownLatch(tasks.size());
        final BlockingDeque<Callable<Boolean>> replayedTasks = new LinkedBlockingDeque<>();
        final AtomicInteger numberOfRetried = new AtomicInteger();
        final Collection<Boolean> statuses = new ConcurrentLinkedQueue<>();

        //Execute all tasks, re-execute the one that have already failed.
        while (latch.getCount() != 0) {
            Callable<Boolean> task = tasks.isEmpty() ? replayedTasks.poll() : tasks.poll();
            if (task == null) {
                Thread.sleep(100);
                continue;
            }
            ListenableFuture<Boolean> listenableFuture = service.submit(task);
            Futures.addCallback(listenableFuture,
                    newCallback(latch, task, numberOfRetried, replayedTasks, statuses));
        }
        latch.await();
        service.shutdown();
        return statuses;
    }

    // on success simply count down the latch, and add the result to the collection.
    // on failure add the task to the replay queue
    static FutureCallback<Boolean> newCallback(final CountDownLatch latch,
            final Callable<Boolean> task,
            final AtomicInteger numberOfRetried,
            final BlockingDeque<Callable<Boolean>> queue,
            final Collection<Boolean> statuses) {
        return new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                statuses.add(result);
                latch.countDown();
            }
            @Override
            public void onFailure(Throwable t) {
                queue.add(task);
                System.out.println("replay " + numberOfRetried.incrementAndGet());
            }
        };
    }
}
