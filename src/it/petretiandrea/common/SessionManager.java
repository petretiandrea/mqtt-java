package it.petretiandrea.common;

import it.petretiandrea.common.session.BrokerSession;
import it.petretiandrea.common.session.ClientSession;
import it.petretiandrea.core.packet.Subscribe;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Class for manage the session for each client ID.
 */
public class SessionManager {

    /**
     * The map that contains the session for each client id.
     */
    private final ConcurrentMap<String, BrokerSession> mSessions;

    /**
     * Constructor Method.
     */
    public SessionManager() {
        mSessions = new ConcurrentHashMap<>();
    }

    /**
     * Add a Broker Session to Session Manager, starting from client session and list of subscription
     * @param clientSession Client Session
     * @param subscriptionList List of subscription of client.
     */
    public void addSession(ClientSession clientSession, List<Subscribe> subscriptionList) {
        BrokerSession brokerSession = new BrokerSession(clientSession, subscriptionList);
        synchronized (mSessions) {
            mSessions.remove(clientSession.getClientID());
            mSessions.put(clientSession.getClientID(), brokerSession);
        }
    }

    /**
     * Search a Broker Session starting from Client ID
     * @param clientID The client id
     * @return The session if found, Null otherwise.
     */
    public BrokerSession getSession(String clientID) {
        if(mSessions.containsKey(clientID))
            return mSessions.get(clientID);
        return null;
    }

    /**
     * Return a list of Broker Sessions.
     * @return
     */
    public Collection<BrokerSession> getSessions() {
        synchronized (mSessions) {
            return mSessions.values();
        }
    }

    /**
     * Remove a session from Session Manager
     * @param clientID Client ID of session.
     */
    public void removeSession(String clientID) {
        synchronized (mSessions) {
            mSessions.remove(clientID);
        }
    }


}
