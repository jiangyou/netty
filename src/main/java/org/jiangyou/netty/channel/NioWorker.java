package org.jiangyou.netty.channel;

/**
 * <title>NioWorker</title>
 * <p></p>
 *
 *
 * @author zhuwei
 *         1/12/14
 */

import net.gleamynode.netty.channel.ChannelFutureListener;
import net.gleamynode.netty.logging.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

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
            synchronized (lock) {
                selector.wakeup();
                SelectionKey key = socketChannel.register(selector, SelectionKey.OP_READ);
                key.attach(new Channel(socketChannel, this.selector));
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
                    handle(key);
                }
            }
        }
    }

    private void handle(SelectionKey key) {
        if (key.isReadable()) {
            read(key);
        }
        if (key.isWritable()) {
            write(key);
        }
    }

    private void write(SelectionKey key) {
        Channel channel = (Channel) key.attachment();
        Queue<ByteBuffer> queue = channel.getWriteQueue();
        if (queue == null) {
            return;
        }
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer buf = null;
        try {
            boolean enableWrite = false;
            while ((buf = queue.peek()) != null) {
                while (socketChannel.write(buf) > 0) {
                }
                if (buf.hasRemaining()) {
                    enableWrite = true;
                    break;
                } else {
                    queue.poll();
                }
            }
            if (enableWrite) {
                socketChannel.register(selector, SelectionKey.OP_WRITE);
            } else {
                if (key.isWritable()) {
                    int interestOps = key.interestOps();
                    key.interestOps(interestOps & ~SelectionKey.OP_WRITE);
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    private void read(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer readBuffer = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE);
        int readBytes = 0;
        int n = 0;
        boolean failure = true;
        try {
            while ((n = socketChannel.read(readBuffer)) > 0) {
                readBytes += n;
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
            System.out.println("Receive Text:" + text);
            Queue<ByteBuffer> queue = (Queue<ByteBuffer>) key.attachment();
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