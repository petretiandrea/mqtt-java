package it.petretiandrea;

import it.petretiandrea.server.Broker;
import it.petretiandrea.server.security.AccountManager;
import it.petretiandrea.server.security.TLSProvider;

import java.io.IOException;
import java.net.URISyntaxException;

public class BrokerLauncher {

    public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {

        BrokerLauncher brokerLauncher = new BrokerLauncher();

        // Aggiunta account per autenticazione
        AccountManager accountManager = new AccountManager();
        // Due utenti di prova
        accountManager.addUser("admin", "admin");
        accountManager.addUser("user", "user");

        // Nuovo Broker con uno specifico account manager.
        // Garantirà l'accesso a tutti i client, non solo a quelli con Account.
        Broker broker = new Broker(accountManager);

        // use TLS
        TLSProvider tlsProvider = new TLSProvider(brokerLauncher.getClass().getResource("server.jks"),
                brokerLauncher.getClass().getResource("cacert.jks"), "rc2018");


        // Server in ascolto di connessioni, su porta specifica
        // Porta 1883 è la porta di default per MQTT.
        if(args.length > 0) {
            if(args[0].equalsIgnoreCase("-tls")) {
                broker.listenTLS(tlsProvider, 8883);
            }
        } else {
            broker.listen(1883);
        }

        System.out.println("CTRL + C for terminate.");

        // Attende il thread broker.
        broker.waitEnd();
    }


}
