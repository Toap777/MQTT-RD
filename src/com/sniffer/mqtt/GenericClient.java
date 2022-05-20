/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer.mqtt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;

/**
 * This class provides logging functionalities, stores message buffers and allows the user to implement on
 * code logic on message receiving by implementing {@see processMessage} and {@see lostConnection}.
 * @author eliseu
 */
public abstract class GenericClient extends BasicClientMQTT{
    //dictionary storing all topic-message buffer associations
    private final Map<String, Queue<String>> topicsMap = new HashMap<>();
    //log file to write messages to
    private String logPath = "/src/com/sniffer/resources/logMessages.csv";

    /**
     * Constructor
     */
    public GenericClient() {
        String rootPath = new File("").getAbsolutePath();
        logPath = rootPath.concat(logPath);
    }

    //anonymous class implementing MQTTCallback interface
    /**
     * This is an implementation of MQTTCallback interface which uses class specific methods to process messages.
     */
    private final MqttCallback callback = new MqttCallback() {

        /**
         * Calls "lostConnection" method of this class and logs to console.
         * @param cause the reason behind the loss of connection.
         */
        @Override
        public void connectionLost(Throwable cause) {
            System.out.println("Connection lost on instance \"" + getClient().getClientId()
                    + "\" with cause \"" + cause.getMessage() + "\" Reason code "
                    + ((MqttException) cause).getReasonCode() + "\" Cause \""
                    + ((MqttException) cause).getCause() + "\"");
            
            lostConnection();
            try {
                System.out.println("Reconnecting to the server...");
                getClient().reconnect();
                System.out.println("Done.");
            } catch (MqttException ex) {
                System.out.println("Can't reconnect to the broker.");
            }
        }

        /**
         * Processes and logs messages by using {@see processMessage} and {@see saveMessage}.
         * @param string name of the topic on the message was published to
         * @param mm the actual message.
         * @throws JSONException
         */
        @Override
        public void messageArrived(String string, MqttMessage mm) throws JSONException {
            //System.out.println("Message arrived: \"" + mm.toString()
            //        + "\" on topic \"" + string + "\" for instance \""
            //        + getClient().getClientId() + "\"");
            
            processMessage(mm.toString(), string);
            saveMessage(string, mm.toString());
        }

        /**
         * Do nothing.
         * @param imdt the delivery token associated with the message.
         */
        @Override
        public void deliveryComplete(IMqttDeliveryToken imdt) {
            //System.out.println("Delivery token \"" + imdt.hashCode()
            //        + "\" received by instance \"" + getClient().getClientId() + "\"");
        }
    };

    /**
     * Gets a dictionary of all topic-message buffer associations
     * @return a dictionary with key is topic name and value is a message queue
     */
    public Map<String, Queue<String>> getTopicsMap(){
        return topicsMap;
    }

    /**
     * Connects to an mqtt broker using tcp and IP address setting the specified username and password as connection defaults. Registers a callback for incoming messages
     * @param clientID clientID
     * @param ip broker ip address
     * @param port broker port
     * @param user connection user name
     * @param pass connection password
     */
    public void connectExternalBroker(String clientID, String ip, String port, String user, String pass) {
        String brokerURL = "tcp://" + ip + ":" + port;
        setUserPass(user, pass);
        connectWithCallback(brokerURL, clientID, callback);
    }

    /**
     * Connects to an mqtt broker using tcp and IP address. Registers a predefined callback for incoming messages
     * @param clientID clientID
     * @param ip broker ip address
     * @param port broker port
     */
    public void connectInternalBroker(String clientID, String ip, String port){
        String brokerURL = "tcp://" + ip + ":" + port;
        connectWithCallback(brokerURL, clientID, callback);    
    }

    /**
     * Writes the topic and message in one line to the log file.
     * @param topic the messages topic
     * @param message message
     */
    private void saveMessage(String topic, String message){
        StringBuilder line = new StringBuilder();
        line.append(topic);
        line.append(",");
        line.append(message);
        line.append("\n"); //e. g.: /house/mylightsensor,Hallo mein Name ist siemens hue.
        
        writeMessage(line.toString());
    }

    /**
     * Appends a line of text to the file at internal logPath.
     * @param line the text.
     */
    private synchronized void writeMessage(String line){
        try {
            Writer output = new BufferedWriter(new FileWriter(logPath, true));
            output.append(line);
            output.close();
            
        } catch (IOException ex) {
            Logger.getLogger(GenericClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    /**
     * Implements how to handle incoming mqtt messages.
     * @param message message
     * @param topic the topic the message was published to
     */
    abstract public void processMessage(String message, String topic);

    /**
     * Implements how to handle a loss of connection to the mqtt broker.
     */
    abstract public void lostConnection();
}
