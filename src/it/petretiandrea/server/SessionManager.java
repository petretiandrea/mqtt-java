package it.petretiandrea.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * Class for manage the session for each client ID.
 */
public class SessionManager {

    /**
     * The map that contains the session for each client id.
     */
    private ConcurrentMap<String, Session> mSessions;

    /**
     * Constructor Method.
     */
    public SessionManager() {
        mSessions = new ConcurrentHashMap<>();
    }

    /**
     * Search a session for clientid
     * @param clientID The client id
     * @return The session if found, Null otherwise.
     */
    public Session searchSession(String clientID) {
        if(mSessions.containsKey(clientID))
            return mSessions.get(clientID);
        return null;
    }

    /**
     * Add a session to collection.
     * @param session The session.
     * @return True if added, False if already exist, or fail.
     */
    private boolean addSession(Session session) {
        if(mSessions.containsKey(session.getClientID()))
           return false;
        mSessions.putIfAbsent(session.getClientID(), session);
        return true;
    }

    /**
     * Create a new session, and add it to this manager's collection.
     * @param clientID The clientID of session
     * @param cleanSession If the session need to be cleaned at end of communication
     * @return The new session.
     */
    public Session createNewSession(String clientID, boolean cleanSession) {
        Session s = new Session(clientID, cleanSession);
        addSession(s);
        return s;
    }

    /**
     * Remove a session of specific clientid, remove only if cleanSession flag is set to true.
     * @param clientID The clientid.
     */
    public void cleanSession(String clientID) {
        if(mSessions.containsKey(clientID)) {
            Session s = mSessions.get(clientID);
            if(s.isCleanSession())
                mSessions.remove(clientID);
        }
    }
}
