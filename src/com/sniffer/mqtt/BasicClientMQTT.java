/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer.mqtt;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * This class represents a MQTT-client connecting with specific connections options (username, password, qos = {@value QOS}) to a broker. The object stores one connection at a time.
 * @author eliseu
 */
public class BasicClientMQTT {
    //The client instance
    private MqttClient client;
    //The qos level
    private static final int QOS = 1;
    //configuration object containing all connection parameters. See: https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html
    private final MqttConnectOptions conOptions = new MqttConnectOptions();

    //Time to rety a connection or network action in ms.
    private final int reconnectTime = 500;

    /**
     * Constructor of BasicClientMQTT. Set some Connection options.
     */
    public BasicClientMQTT(){
        conOptions.setCleanSession(true);
        conOptions.setKeepAliveInterval(60);
        conOptions.setAutomaticReconnect(true);
    }
    /**
     * Sets the Username and Password for the mqtt connection. After that conenction to be refreshed with {@see connectWithCallback}.
     * @param user username
     * @param pass password
     */
    public void setUserPass(String user, String pass){
        conOptions.setUserName(user);
        conOptions.setPassword(pass.toCharArray());
    }

    /**
     * Connect to MQTT Broker as a client on {brokerURL} using {clientID} and message callback function {callback}.
     * @param brokerURL brokerURL address of broker
     * @param clientID clientID unique name of this client at the broker
     * @param callback callback function that is executed on exchanged mqtt message
     */
    public void connectWithCallback(String brokerURL, String clientID, MqttCallback callback) {
        //Represents a persistent data store, used to store outbound and inbound messages while they are in flight, enabling delivery to the QoS specified.
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            client = new MqttClient(brokerURL, clientID, persistence);//Blocking mqtt client
            client.setCallback(callback);
            client.connect(conOptions);
            System.out.println("Connected to broker using clientID: " + clientID + ", at URL: "+ brokerURL);
            
        } catch (MqttException ex) {
            System.err.println("Can't connect to broker using clientID: " + clientID + ", at URL: "+ brokerURL);
        }
    }

    /**
     * Returns internal instance of MQTTClient
     * @return MQTTClient
     */
    public MqttClient getClient(){
        return client;
    }

    /**
     * Connect to MQTT Broker as a client on {brokerURL} using {clientID}
     * @param brokerURL brokerURL address of broker
     * @param clientID clientID unique name of this client at the broker
     * @return 1 on successful connection, -1 if connection failed
     */
    public int connect(String brokerURL, String clientID) {
        //Represents a persistent data store, used to store outbound and inbound messages while they are in flight, enabling delivery to the QoS specified.
        MemoryPersistence persistence = new MemoryPersistence();
        try {
            client = new MqttClient(brokerURL, clientID, persistence);
            client.connect(conOptions);
            return 1;
        } catch (MqttException ex) {
            System.err.println("Can't connect to broker: " + clientID + ", at URL: "+ brokerURL);
            return -1;
        }
    }

    /**
     * Publishes a message to a specified topic with QOS {@value QOS}. If it could not be send try again with QOS = 0.
     * @param msg message
     * @param topicString topic
     */
    public void publish(String msg, String topicString) {
        //System.out.println("Publishing message: " + msg);
        MqttMessage message = new MqttMessage(msg.getBytes());
        message.setQos(QOS);
        
        try {
            client.publish(topicString, message);
        } catch (MqttException ex) {
            System.err.println("Can't publish in this topic: " + topicString);
            try {
                System.out.println("Trying again... with QOS 1"); //TODO: Find out why he changes QOS
                Thread.sleep(reconnectTime);
                MqttMessage message1 = new MqttMessage(msg.getBytes());
                message1.setQos(0);
                client.publish(topicString, message1);
            } catch (InterruptedException | MqttException ex1) {
                System.err.println("Error trying again to publish...");
            }
            
        }
    }

    /**
     * Subscribes to topic using QOS {@value QOS}.
     * @param topicString topic
     */
    public void subscribe(String topicString) {
        System.out.println("Subscribing to topic \"" + topicString
                + "\" for client instance \"" + client.getClientId()
                + "\" using QoS " + QOS + ".");

        try {
            client.subscribe(topicString, QOS);
        } catch (MqttException ex) {
            System.err.println("Can't subscribe this topic: " + topicString);
        }
    }

    /**
     * Unsubscribe from topic.
     * @param topicString topic
     */
    public void unsubscribe(String topicString){
        try {
            client.unsubscribe(topicString);
        } catch (MqttException ex) {
            System.err.println("Can't unsubscribe this topic: " + topicString);
        }
    }

    /**
     * Waits for max 30 seconds to finish work and then disconnect.
     */
    public void disconnect() {
        try {
            client.disconnect();
            client.close();
        } catch (MqttException ex) {
            System.err.println("Can't disconnect from this broker: " + client.getClientId());
            try {
                System.out.println("Trying again...");
                Thread.sleep(reconnectTime);
                client.close();
            } catch (InterruptedException | MqttException ex1) {
                Logger.getLogger(BasicClientMQTT.class.getName()).log(Level.SEVERE, null, ex1);
            }            
        }
    }
}
