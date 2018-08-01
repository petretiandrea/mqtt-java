package it.petretiandrea.common;

import it.petretiandrea.core.packet.Subscribe;
import it.petretiandrea.core.packet.Unsubscribe;

import java.util.*;
import java.util.function.Predicate;

public class SubscribeManager {

    /**
     * Subscription List for each ClientID
     */
    private final Map<String, List<Subscribe>> mSubscribes;


    public SubscribeManager() {
        mSubscribes = new HashMap<>();
    }

    public void subscribe(String clientID, Subscribe subscribe) {
        synchronized (mSubscribes) {
            if(!mSubscribes.containsKey(clientID))
                mSubscribes.put(clientID, new ArrayList<>());
            mSubscribes.get(clientID).add(subscribe);
        }
    }

    public void unsubscribe(String clientID, Unsubscribe unsubscribe) {
        synchronized (mSubscribes) {
            if(mSubscribes.containsKey(clientID)) {
                mSubscribes.get(clientID).removeIf(subscribe -> subscribe.getTopic().equals(unsubscribe.getTopic()));
            }
        }
    }

    public List<Subscribe> getSubscriptions(String clientID) {
        synchronized (mSubscribes) {
            return mSubscribes.getOrDefault(clientID, Collections.emptyList());
        }
    }
}
