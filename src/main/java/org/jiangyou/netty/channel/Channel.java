package org.jiangyou.netty.channel;

import net.gleamynode.netty.logging.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

/**
 * <title>Channel</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         1/14/14
 */
public class Channel {

    private static final Logger LOGGER = Logger.getLogger(Channel.class);
    private SocketChannel socketChannel;
    private Selector selector;
    private Queue<ByteBuffer> writeQueue = new LinkedList<ByteBuffer>();


    public Channel(SocketChannel socketChannel, Selector selector) {
        this.socketChannel = socketChannel;
        this.selector = selector;
    }



    public void write(ByteBuffer byteBuffer) throws IOException {
        writeQueue.offer(byteBuffer);
        try {
            socketChannel.register(selector, SelectionKey.OP_WRITE);
        } catch (ClosedChannelException e) {
            LOGGER.error("Register OP_Write error", e);
            socketChannel.close();
        }
    }

    public Selector getSelector() {
        return this.selector;
    }

    public SocketChannel getSocketChannel() {
        return this.socketChannel;
    }

    public Queue<ByteBuffer>getWriteQueue() {
        return this.writeQueue;
    }

}