package it.petretiandrea;

import it.petretiandrea.client.MQTTClient;
import it.petretiandrea.common.Client;
import it.petretiandrea.common.MQTTClientCallback;
import it.petretiandrea.core.ConnectionSettings;
import it.petretiandrea.core.ConnectionSettingsBuilder;
import it.petretiandrea.core.Message;
import it.petretiandrea.core.Qos;
import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.exception.MQTTProtocolException;
import it.petretiandrea.core.packet.Publish;
import it.petretiandrea.core.packet.Subscribe;
import it.petretiandrea.core.packet.Unsubscribe;
import it.petretiandrea.server.security.TLSProvider;
import it.petretiandrea.utils.CustomLogger;
import org.apache.commons.cli.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientLauncher implements MQTTClientCallback {

    public static void main(String[] args) throws IOException, MQTTProtocolException, MQTTParseException {

        CustomLogger.LOGGER.setLevel(Level.OFF);

        Options options = new Options();
        // hostname option
        options.addOption(Option.builder("h").hasArg().longOpt("hostname").desc("hostname").required().build());
        // port option
        options.addOption(Option.builder("p").hasArg().desc("port").required(false).build());
        // clientid
        options.addOption(Option.builder("id").hasArg().required().desc("set a client ID").build());
        // keepalive
        options.addOption(Option.builder("k").desc("set the keepalive").hasArg().required(false).build());
        // clean session flag
        options.addOption(Option.builder("cl").hasArg().required(false).desc("set clean session false").build());
        // username
        options.addOption(Option.builder("u").hasArg().required(false).desc("set the username").build());
        // password
        options.addOption(Option.builder("passwd").hasArg().required(false).desc("set the password for user").build());
        // will message
        options.addOption(Option.builder("wt").hasArg().desc("Set the will topic").required(false).build());
        // will message
        options.addOption(Option.builder("wm").hasArg().required(false).desc("Set the will message").build());
        // will qos
        options.addOption(Option.builder("wq").hasArg().required(false).desc("Set the will qos").build());

        // tls options
        options.addOption(Option.builder("tls").hasArg(false).required(false).desc("Use TLSv1.2").build());

        CommandLineParser commandLineParser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = commandLineParser.parse(options, args, true);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar ClientMQTT.jar -h <hostname> -id <clientid>", options);
            return;
        }

        if(cmd.hasOption("u") && !cmd.hasOption("passwd")) {
            System.out.println("Missing password!");
            return;
        }

        if((cmd.hasOption("wt") && !cmd.hasOption("wm")) || (!cmd.hasOption("wt") && cmd.hasOption("wm"))) {
            System.out.println("Missing will topic or will message");
            return;
        }

        ClientLauncher clientLauncher = new ClientLauncher();

        Message willMessage = new Message(cmd.getOptionValue("wt"), cmd.getOptionValue("wm"), Qos.fromInteger(Integer.parseInt(cmd.getOptionValue("wq", "0"))), false);

        ConnectionSettings settings = new ConnectionSettingsBuilder()
                .setHostname(cmd.getOptionValue("h"))
                .setPort(Integer.parseInt(cmd.getOptionValue("p", "1883")))
                .setClientId(cmd.getOptionValue("id"))
                .setKeepAliveSeconds(Integer.parseInt(cmd.getOptionValue("k", "20")))
                .setCleanSession(cmd.hasOption("cl"))
                .setUsername(cmd.getOptionValue("u", null))
                .setPassword(cmd.getOptionValue("passwd", null))
                .setWillMessage(cmd.hasOption("wt") ? willMessage : null)
                .setSSLContextProvider(cmd.hasOption("tls") ?
                        new TLSProvider(clientLauncher.getClass().getResource("mqtt_client.jks"), clientLauncher.getClass().getResource("cacert.jks"), "rc2018") : null)
                .build();

        MQTTClient client = new MQTTClient(settings);
        client.setClientCallback(clientLauncher);

        System.out.println("Connecting...");
        if(client.connect()) {
            System.out.println("Connected");
            printHelp();
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while (client.isConnected()) {
                String line = reader.readLine();
                if(line.startsWith("help"))
                    printHelp();
                else if (line.startsWith("exit"))
                    client.disconnect();
                else {
                    String[] pieces = line.split("\"?( |$)(?=(([^\"]*\"){2})*[^\"]*$)\"?");
                    if(pieces.length > 0) {
                        // subscribe command received
                        if(pieces[0].equalsIgnoreCase("sub") && pieces.length >= 2) {
                            String topic = pieces[1];
                            String qos = (pieces.length >=3) ? pieces[2] : "0"; // default qos is 0
                            boolean sub = client.subscribe(topic, Qos.fromInteger(Integer.parseInt(qos)));
                            System.out.println("Subscribe command: " + (sub ? "ok" : "fail"));
                        }
                        // publish command recevied
                        else if(pieces[0].equalsIgnoreCase("pub") && pieces.length >= 3) {
                            //String topic, String message, Qos qos, boolean retain
                            String topic = pieces[1];
                            String msg = pieces[2];
                            String qos = (pieces.length >= 4) ? pieces[3] : "0";
                            boolean retain = (pieces.length >= 5) && pieces[4].equals("rt");
                            client.publish(new Message(topic, msg, Qos.fromInteger(Integer.parseInt(qos)), retain));
                        } if (pieces[0].equalsIgnoreCase("unsub") && pieces.length >= 2) {
                            String topic = pieces[1];
                            client.unsubscribe(topic);
                        }
                    }
                }
            }
        } else
            System.out.println("Fail Connection");
    }

    private static void printHelp() {
        System.out.println("Help:");
        System.out.println("\t- exit, comando di uscita.");
        System.out.println("\t- pub <topic> <message> <qos> <rt>, comando di pubblicazione.");
        System.out.println("\t\t<rt> e <qos> non sono obbligatori, con rt impostato il messaggio è retain.");
        System.out.println("\t- sub <topic> <qos>, comando di sottoscrizione.");
        System.out.println("\t\tqos non è obbligatorio, default qos è 0");
        System.out.println("\t- unsub <topic>, comando di cancellazione iscrizione");
    }

    @Override
    public void onMessageArrived(Client client, Message message) {
        CustomLogger.log(String.format("Message from: %s, Topic: %s, Content: %s, Qos: %d",
                client.getClientSession().getClientID(),
                message.getTopic(),
                message.getMessage(),
                message.getQos().ordinal()));
    }

    @Override
    public void onDeliveryComplete(Client client, Publish publish) {
        CustomLogger.log(String.format("Delivery complete, Topic: %s, Message: %s, Qos: %s, Message id: %d",
                publish.getMessage().getTopic(),
                publish.getMessage().getMessage(),
                publish.getMessage().getQos().ordinal(),
                publish.getMessage().getMessageID()));
    }

    @Override
    public void onConnectionLost(Client client, Throwable ex) {
        CustomLogger.log(String.format("Connection Lost: %s", ex.getMessage()));
    }

    @Override
    public void onDisconnect(Client client) {
        CustomLogger.log("Disconnected!");
    }

    @Override
    public void onSubscribeComplete(Client client, Subscribe subscribe) {
        CustomLogger.log(String.format("Subscribe complete, Topic: %s, Req Qos: %d, Message ID: %d",
                subscribe.getTopic(),
                subscribe.getQosSub().ordinal(),
                subscribe.getMessageID()));
    }

    @Override
    public void onUnsubscribeComplete(Client client, Unsubscribe unsubscribe) {
        CustomLogger.log(String.format("Unsubscribe complete, Topic: %s",
                unsubscribe.getTopic()));
    }
}
