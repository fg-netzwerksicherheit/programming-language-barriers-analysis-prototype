package data_consumer.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import data_consumer.config.Constants;

public class CommandExecutionHelper {

    /*
     * Evil hack to execute our data_generator command line application from
     * within Java.
     */
    public static void delayedDataGeneratorExec(final String args, int delay) {
        ScheduledExecutorService execService = new ScheduledThreadPoolExecutor(1);
        execService.schedule(new Runnable() {

            @Override
            public void run() {
                try {
                    ProcessBuilder builder = new ProcessBuilder(new String[] {
                            "/bin/sh",
                            "-c",
                            "LD_LIBRARY_PATH=" + Constants.DATA_GENERATOR_CWD_PATH + " " + Constants.DATA_GENERATOR_EXEC_CWD_PATH + " "
                                    + args });
                    builder.redirectErrorStream(true);
                    final Process p = builder.start();

                    @SuppressWarnings("unused")
                    Thread stdoutThread = new Thread(new Runnable() {
                        public void run() {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                            String line;
                            try {
                                while ((line = reader.readLine()) != null) {
                                    System.out.println("dg: " + line);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
//                    stdoutThread.start();

                    p.waitFor();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    public static void forceCreateNewFifo(String fifoName) {
        rm(fifoName);

        try {
            Process p = Runtime.getRuntime().exec(new String[] { "mkfifo", fifoName });
            p.waitFor();
            /*
             * Below call injects some dummy data into the pipe. This is done to
             * avoid calls like "FileChannel.open" to block which it would does
             * otherwise. However, this has to be taken into account when
             * reading from the fifi.
             */
            Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", "echo x > " + fifoName });
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void rm(String name) {
        File f = new File(name);
        if (f.exists()) {
            f.delete();
        }
    }

}
