package org.jiangyou.netty.channel;

/**
 * <title>NioBoss</title>
 * <p></p>
 *
 *
 * @author zhuwei
 *         1/12/14
 */


import net.gleamynode.netty.logging.Logger;


import java.io.IOException;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <title>NioBoss</title>
 * <p></p>
 *
 * @author zhuwei
 *         1/12/14
 */
public class NioBoss implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(NioBoss.class);
    private Selector selector;
    private volatile boolean stopped = false;
    private NioWorker[] workers;
    private AtomicInteger workerId = new AtomicInteger(0);
    private ServerSocketChannel serverSocketChannel;
    public NioBoss(NioWorker[] workers) {
        this.workers = workers;
        try {
            selector = Selector.open();
        } catch (IOException e) {
            LOGGER.error("Boss selector open error");
        }
    }

    public void register(ServerSocketChannel serverSocketChannel) {
        if (selector == null) {
            throw new NullPointerException("selector=Null");
        }
        this.serverSocketChannel = serverSocketChannel;
        try {
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            LOGGER.info("Boss register OP_ACCEPT success.");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void run() {
        Thread.currentThread().setName("Boss Thread");
        while (!stopped) {
            int n = 0;
            try {
                n = selector.select();
            } catch (IOException e) {
                LOGGER.error("selector select error");
                continue;
            }

            if (n > 0) {
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey selectionKey = it.next();
                    it.remove();
                    if (selectionKey.isAcceptable()) {
                        SocketChannel socketChannel = null;
                        try {
                            socketChannel = serverSocketChannel.accept();
                        } catch (IOException e) {
                            LOGGER.error("Accept error");
                            continue;
                        }
                        LOGGER.info("Accept channel:" + socketChannel);
                        allocateWorker(socketChannel);

                    }
                }
            }
        }
    }


    private void allocateWorker(SocketChannel socketChannel) {
        NioWorker worker = workers[workerId.incrementAndGet() % workers.length];
        worker.register(socketChannel);
    }
}