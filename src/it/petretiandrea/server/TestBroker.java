package it.petretiandrea.server;

import it.petretiandrea.server.security.AccountManager;

import java.io.IOException;

public class TestBroker {

    public static void main(String[] args) throws IOException {

        AccountManager accountManager = new AccountManager();
        accountManager.addUser("admin", "admin");

        Broker broker = new Broker(accountManager);

        broker.listen(1883);

        System.in.read();
    }

}
