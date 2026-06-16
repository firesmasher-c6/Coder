package me.coder.codedsl.libraries;

/**
 * Async execution library for CodeDSL
 * Allows scripts to run on different threads
 */
public class AsyncLibrary implements LibraryRegistry.CodeDSLLibrary {

    @Override
    public String getName() {
        return "async";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public void onLoad() {
        // Initialize async library
    }

    @Override
    public void onUnload() {
        // Cleanup
    }

    /**
     * Run task asynchronously
     */
    public static void runAsync(Runnable runnable) {
        // This would be implemented with the plugin scheduler
        new Thread(runnable).start();
    }

    /**
     * Run task with delay
     */
    public static void runAsyncDelayed(Runnable runnable, long delayMillis) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMillis);
                runnable.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Run task repeatedly
     */
    public static void runAsyncTimer(Runnable runnable, long delayMillis, long periodMillis) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMillis);
                while (true) {
                    runnable.run();
                    Thread.sleep(periodMillis);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}