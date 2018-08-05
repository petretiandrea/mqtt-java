package it.petretiandrea;

import it.petretiandrea.server.Broker;
import it.petretiandrea.server.security.AccountManager;

import java.io.IOException;

public class BrokerLauncher {

    public static void main(String[] args) throws IOException, InterruptedException {

        // Aggiunta account per autenticazione
        AccountManager accountManager = new AccountManager();
        // Due utenti di prova
        accountManager.addUser("admin", "admin");
        accountManager.addUser("user", "user");

        // Nuovo Broker con uno specifico account manager.
        // Garantirà l'accesso a tutti i client, non solo a quelli con Account.
        Broker broker = new Broker(accountManager);

        // Server in ascolto di connessioni, su porta specifica
        // Porta 1883 è la porta di default per MQTT.
        broker.listen(1883);

        // Attende il thread broker.
        broker.waitEnd();
    }


}
