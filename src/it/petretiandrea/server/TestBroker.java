package it.petretiandrea.server;

import java.io.IOException;

public class TestBroker {

    public static void main(String[] args) throws IOException {

        Broker broker = new Broker();

        broker.listen(1883);

        System.in.read();
    }

}
