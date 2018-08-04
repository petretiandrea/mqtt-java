package it.petretiandrea.server.security;

import java.util.HashMap;
import java.util.Map;

public class AccountManager {

    private Map<String, String> mMap;

    private static AccountManager mInstance;

    public AccountManager() {
        mMap = new HashMap<>();
    }

    public boolean existUsername(String username) {
        return mMap.containsKey(username);
    }

    public void addUser(String username, String password) {
        mMap.putIfAbsent(username, password);
    }

    public boolean grantAccess(String username, String password) {
        if(mMap.containsKey(username))
            return mMap.get(username).equals(password);
        return false;
    }
}
