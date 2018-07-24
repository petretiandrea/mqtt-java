package it.petretiandrea.server;

import it.petretiandrea.core.Message;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

public class MessageDispatcher {

    private ReentrantLock mLock;

    private ConcurrentMap<String, ClientMonitor> mClients;
    private LinkedList<Message> mRetains;

    public MessageDispatcher(ConcurrentMap<String, ClientMonitor> clients) {
        mLock = new ReentrantLock(true);
        mRetains = new LinkedList<>();
        mClients = clients;
    }

    public void addRetainMessage(Message message) {
        if(message.isRetain()) {
            mLock.lock();
            try {
                mRetains.add(message);
            } finally {
                mLock.unlock();
            }
        }
    }

    public void removeRetainMessages(String topic) {
        if(topic != null && !topic.trim().isEmpty()) {
            mLock.lock();
            try {
                mRetains.stream().filter(message -> message.getTopic().equals(topic)).findAny().ifPresent(message -> mRetains.remove(message));
            } finally {
                mLock.unlock();
            }
        }
    }


}
