import com.dyptan.StreamTransformer;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.TimeoutException;

public class TransformerTest {

    @Ignore
    @Test
    public void runTransformer() throws InterruptedException, TimeoutException {
        StreamTransformer transformer = new StreamTransformer(1);
        Thread detatchedTransformer = new Thread(transformer);
        detatchedTransformer.start();
        Thread.sleep(30000);
        transformer.query.stop();
    }
}
