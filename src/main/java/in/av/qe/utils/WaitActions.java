package in.av.qe.utils;

import in.av.qe.helpers.QaLogger;
import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.with;

public class WaitActions {
    public static final Logger LOGGER = QaLogger.getQaLogger();

    public static void waitFor(Callable<Boolean> callable) {
        waitFor(callable, 90, 1);
    }

    public static void waitFor(Callable<Boolean> callable, int howManySeconds) {
        waitFor(callable, howManySeconds, 1);
    }

    public static void waitFor(Callable<Boolean> callable, int howManyseconds, int pollInterval) {
        await()
                .atMost(howManyseconds, TimeUnit.SECONDS)
                .pollInterval(pollInterval, TimeUnit.SECONDS)
                .until(callable);
    }

    public static void waitForWithMessage(Callable<Boolean> callable, int howManySeconds, int pollInterval, String message) {
        try {
            waitFor(callable, howManySeconds, pollInterval);
        } catch (ConditionTimeoutException e) {
            throw new ConditionTimeoutException(message);
        }
    }

    public static void waitFor(Callable<Boolean> callable, int pollDelay, int atMost, int pollInterval) {
        with()
                .pollDelay(pollDelay, TimeUnit.SECONDS)
                .and()
                .pollInterval(pollInterval, TimeUnit.SECONDS)
                .await()
                .atMost(atMost, TimeUnit.SECONDS)
                .until(callable);
    }

    public static void waitPreciseFor(Callable<Boolean> callable, int howManyseconds, int pollInterval) {
        with()
                .pollInterval(pollInterval, TimeUnit.MILLISECONDS)
                .await()
                .atMost(howManyseconds, TimeUnit.SECONDS)
                .until(callable);
    }

    public static void sleepSeconds(int howLongInSeconds) {
        try {
            LOGGER.info("Sleep for " + howLongInSeconds + " seconds...");
            Thread.sleep(howLongInSeconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String currentTime() {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
    }
}
