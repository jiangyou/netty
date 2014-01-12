package org.jiangyou.netty.channel;

/**
 * <title>NioServer</title>
 * <p></p>
 *
 * @author zhuwei
 *         1/12/14
 */

import net.gleamynode.netty.logging.Logger;
import org.jiangyou.netty.channel.NioWorker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <title>NioServer</title>
 * <p></p>
 *
 * @author zhuwei
 *         1/12/14
 */
public class NioServer {

    private static final Logger LOGGER = Logger.getLogger(NioServer.class);
    private String host;
    private int port;
    private volatile boolean stopped = false;
    private NioWorker nioWorkers;
    private NioBoss nioBoss;
    private ExecutorService boosExecutor;
    private ExecutorService workerExecutor;
    public NioServer(String host, int port,ExecutorService boosExecutor,ExecutorService workerExecutor) {
        this.host = host;
        this.port = port;
        ServerSocketChannel serverSocketChannel = null;
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(host, port));
        } catch (IOException e) {
            LOGGER.error("ServerSocketChannel open error,exit.");
            System.exit(1);
        }
        this.boosExecutor = boosExecutor;
        this.workerExecutor = workerExecutor;
        nioWorkers = new NioWorker();
        nioBoss = new NioBoss(new NioWorker[]{nioWorkers});
        nioBoss.register(serverSocketChannel);
        boosExecutor.submit(nioBoss);
        workerExecutor.submit(nioWorkers);
        LOGGER.info("NioServer already start.");
    }
    public void start() {

    }
    public static void main(String[] args) {
        NioServer server = new NioServer("localhost", 8080, Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
        server.start();
    }
}