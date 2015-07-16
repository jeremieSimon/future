package playing.with.thefuture.p4shuttingdown;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Here we want to make sure that even if we already returned our results,
 * We'll still shutdown our executor on completion and the shutdown action is not blocking our client.
 * In fact, we want to hide as much as possible to caller.
 * We don't want the caller to have any knowledge about how those tasks are being scheduled
 * And we don't him to have to shutdown the service nor that we want him to create the service.
 *
 */
public class ShutdownAfterReturning {

    public static void main(String[] args) {
        final int numberOfTasks = 20;
        //create some dummy tasks
        final Queue<Callable<Boolean>> tasks = new LinkedList<>();
        for (int i = 0; i < numberOfTasks; i++) {
            tasks.add(newCallable());
        }
        //execute
        System.out.println("executed tasks: " + executeTasks(tasks));
        System.out.println("hand on there");
    }

    static List<Future<Boolean>> executeTasks(
            final Queue<Callable<Boolean>> tasks) {

        final ListeningExecutorService service = MoreExecutors
                .listeningDecorator(Executors.newFixedThreadPool(5));
        final CountDownLatch latch = new CountDownLatch(tasks.size());
        final List<Future<Boolean>> results = new ArrayList<>();
        while (!tasks.isEmpty()) {
            Callable<Boolean> task = tasks.poll();
            ListenableFuture<Boolean> future = service.submit(task);
            Futures.addCallback(future, newCallback(latch, service));
            results.add(future);
        }
        return results;
    }

    static Callable<Boolean> newCallable() {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Thread.sleep(1000);
                return true;
            }
        };
    }

    static FutureCallback<Boolean> newCallback(
            final CountDownLatch latch,
            final ExecutorService service) {
        return new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                latch.countDown();
                if (latch.getCount() == 0) {
                    service.shutdown();
                }
            }
            @Override
            public void onFailure(Throwable t) {
                latch.countDown();
                if (latch.getCount() == 0) {
                    service.shutdown();
                }
            }
        };
    }
}
