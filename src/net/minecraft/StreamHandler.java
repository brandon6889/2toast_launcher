package net.minecraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.function.Consumer;

public class StreamHandler implements Runnable {

    private static final String SYSTEM_ENCODING = System.getProperty("sun.jnu.encoding", Charset.defaultCharset().name());
    private static final Charset SYSTEM_CHARSET = Charset.forName(SYSTEM_ENCODING);
    
    private final InputStream inStream;
    private final PrintStream outStream;
    private final Consumer<String> callback;
    
    public StreamHandler(InputStream is, PrintStream ps, Consumer<String> cb) {
        inStream = is;
        outStream = ps;
        callback = cb;
    }
    
    @Override
    public void run() {
        try {
            InputStreamReader inStreamReader = new InputStreamReader(inStream, SYSTEM_CHARSET);
            BufferedReader br = new BufferedReader(inStreamReader);
            String line;
            while ((line = br.readLine()) != null) {
                outStream.println(line);
                if (Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (callback != null)
                    callback.accept(line);
            }
        }
        catch (IOException ex) {
            System.err.println("Stream error: " + ex);
        }
    }
    
}
