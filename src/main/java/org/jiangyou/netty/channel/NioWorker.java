package org.jiangyou.netty.channel;

/**
 * <title>NioWorker</title>
 * <p></p>
 *
 *
 * @author zhuwei
 *         1/12/14
 */

import net.gleamynode.netty.logging.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * <title>NioWorker</title>
 * <p></p>
 *
 * @author zhuwei
 *         1/12/14
 */
public class NioWorker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(NioWorker.class);
    private Selector selector;
    private volatile boolean stopped = false;
    private final int DEFAULT_BUFFER_SIZE = 1024;
    private Object lock = new Object();
    Queue<ByteBuffer> queue = new LinkedList<ByteBuffer>();
    Thread thread;
    public NioWorker() {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            LOGGER.error("NioWorker selector open failed.");
        }
    }

    public void register(SocketChannel socketChannel) {
        try {
            socketChannel.configureBlocking(false);
            synchronized (lock){
                selector.wakeup();
                socketChannel.register(selector, SelectionKey.OP_READ);
            }
            LOGGER.info("NioWorker register Success,Wake up selector.");

        } catch (IOException e) {
            LOGGER.error("NioWorker register error.");
        }
    }


    public void run() {
        thread = Thread.currentThread();
        thread.setName("Worker Thread[]");
        while (!stopped) {
            int n = 0;
            synchronized (lock) {

            }
            try {
                n = selector.select();
            } catch (IOException e) {
                LOGGER.error("NioWorker select error.");
                continue;
            }
            if (n > 0) {
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    handel(key);
                }
            }
        }
    }

    private void handel(SelectionKey key) {
        if (key.isReadable()) {
            read(key);
        }
        if (key.isWritable()) {
            write(key);
        }
    }

    private void write(SelectionKey key) {

    }

    private void read(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer readBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
        int readBytes = 0;
        int n = 0;
        boolean failure = true;
        try {
            while ((n = socketChannel.read(readBuffer)) > 0) {
                readBytes+= n;
                if (!readBuffer.hasRemaining()) {
                    break;
                }
            }
            failure = false;
        } catch (IOException e) {
            LOGGER.error("Read error.");
        }

        if (readBytes > 0) {
            String text = new String(readBuffer.array(), 0, readBytes);
            System.out.println("Receive Text:"+text);
        }
        if (n < 0 || failure) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                LOGGER.error("SocketChannel close error.");
            }
        }
    }
}