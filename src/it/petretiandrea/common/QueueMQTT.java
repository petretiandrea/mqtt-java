package it.petretiandrea.common;

import it.petretiandrea.core.packet.base.MQTTPacket;

import java.util.concurrent.ConcurrentLinkedQueue;

public class QueueMQTT<T extends MQTTPacket> extends ConcurrentLinkedQueue<T> {




}
