package it.petretiandrea;

import it.petretiandrea.client.MQTTClient;
import it.petretiandrea.core.ConnectionSettings;
import it.petretiandrea.core.ConnectionSettingsBuilder;
import it.petretiandrea.core.Message;
import it.petretiandrea.core.Qos;
import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.exception.MQTTProtocolException;
import org.apache.commons.cli.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;

public class Main {

    public static void main(String[] args) throws ParseException, IOException, NoSuchAlgorithmException, MQTTProtocolException, MQTTParseException {

        Options options = new Options();
        // hostname option
        options.addOption("h", true, "hostname");
        // port option
        options.addOption("p", true, "port");
        // clientid
        options.addOption("id", true, "Set the client id");
        // keepalive
        options.addOption("k", true, "Set the keepalive");
        // clean session flag
        Option tmp = new Option("cl", false, "Set clean sessione to false");
        tmp.setOptionalArg(true);
        options.addOption(tmp);
        // username
        tmp = new Option("u", true, "Set the username");
        tmp.setOptionalArg(true);
        options.addOption(tmp);
        // password
        tmp = new Option("passwd", true, "Set the password");
        tmp.setOptionalArg(true);
        options.addOption(tmp);
        // will message
        tmp = new Option("wt", true, "Set the will topic");
        tmp.setOptionalArg(true);
        options.addOption(tmp);
        // will message
        tmp = new Option("wm", true, "Set the will message");
        tmp.setOptionalArg(false);
        options.addOption(tmp);
        // will qos
        tmp = new Option("wq", true , "Set the will qos");
        tmp.setOptionalArg(false);
        options.addOption(tmp);


        CommandLineParser commandLineParser = new DefaultParser();
        CommandLine cmd = commandLineParser.parse(options, args);

        if(cmd.hasOption("u") && !cmd.hasOption("passwd")) {
            System.out.println("Missing password!");
            return;
        }

        if((cmd.hasOption("wt") && !cmd.hasOption("wm")) || (!cmd.hasOption("wt") && cmd.hasOption("wm"))) {
            System.out.println("Missing will topic or will message");
            return;
        }

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
                .build();

        MQTTClient client = new MQTTClient(settings);

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
                    String[] pieces = line.split(" ");
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
                        }
                    }
                }
            }
        } else
            System.out.println("Fail Connection");
    }

    private static void printHelp() {
        System.out.println("Help:");
        System.out.println("\tComando di uscita: exit");
        System.out.println("\tComando di pubblicazione: pub topic message qos rt");
        System.out.println("\t\trt e qos non sono obbligatori, con rt impostato il messaggio è retain");
        System.out.println("\tComando di sottoscrizione: sub topic qos");
        System.out.println("\t\tqos non è obbligatorio, default qos è 0");
    }
}
