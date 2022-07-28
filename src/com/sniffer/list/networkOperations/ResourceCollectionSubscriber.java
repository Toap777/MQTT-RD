/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer.list.networkOperations;

import com.sniffer.list.json.ResourceCollectionWriter;
import com.sniffer.mqtt.GenericClient;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class manages connections and "resource Collection" retrival from local dns remote brokers and their associated sniffers.
 * @author eliseu
 */
public class ResourceCollectionSubscriber extends GenericClient{
    //"resource collection" file path
    private final String resourceCollectionPath;
    //expected topic name to puplish and receive "resource Collection" update messages on
    private static final String TOPIC_LIST = "/getlist";
    // Specifies how long to wait for and resourceCollection update message from local or remote broker in ms
    private static final int SERVER_TIMEOUT = 10000;

    /**
     * Constructor of class.
     * @param resourceCollectionPath resourceCollectionPath
     */
    public ResourceCollectionSubscriber(String resourceCollectionPath) {
        this.resourceCollectionPath = resourceCollectionPath;
    }

    /**
     * This method loads a resource collection into the system by connecting to a local (network) sniffer
     * The local sniffer keeps sending its current resource collection over the network. So we subscribe, wait for {@value SERVER_TIMEOUT} ms get data and unsubscribe.
     * @param snifferID local snifferID
     * @param ip local broker ip address
     * @param port local broker port
     */
    public void getLocalresourceCollection(String snifferID, String ip, String port) {
        System.out.println("\nGetting the list from a local sniffer...");
        connectInternalBroker(snifferID, ip, port);

        subscribe(TOPIC_LIST);
        try {
            Thread.sleep(SERVER_TIMEOUT);
        } catch (InterruptedException ex) {
            System.err.println("Can't get the list from the sniffer at that IP: " + ip);
        }
        finally{
            unsubscribe(TOPIC_LIST);
            disconnect();
        }
    }

    /**
     * This method loads a resource collection into the system by connecting to a remote (network) sniffer
     * The remote sniffer keeps sending its current resource collection over the network. So we subscribe, wait for {@value SERVER_TIMEOUT} ms get data and unsubscribe.
     * @param snifferID associated snifferID
     * @param config config file JSON object representation
     * @throws JSONException thrown on error on reading resource collection
     */
    public void getRemoteResourceCollection(String snifferID, JSONObject config) throws JSONException {
        System.out.println("\nGetting the list from a remote sniffer...");
        
        JSONObject brokers = config.getJSONObject("remote_brokers");

        //The method awaits that a server object (remote broker) is defined
        JSONObject serverBroker = brokers.getJSONObject("server");
        String serverIP = serverBroker.getString("ip");
        String serverPort = serverBroker.getString("port");
        String serverUser = serverBroker.getString("user");
        String serverPass = serverBroker.getString("pass");
        
        //Get the list from the server
        //TODO: Refactore broker usage
        ResourceCollectionSubscriber subList = new ResourceCollectionSubscriber(resourceCollectionPath); //We us new isntance because there is only one conneciton per instance AND username and password is saved in this connection. So using the current object will change password and username of current connection and make communication unavailable.
        subList.connectExternalBroker(snifferID, serverIP, serverPort, serverUser, serverPass);
        subList.subscribe(TOPIC_LIST);
        try {
            Thread.sleep(SERVER_TIMEOUT);
        } catch (InterruptedException ex) {
            System.err.println("Cann't get the list from the remote sniffer at that IP: " + serverIP);
        }
        finally {
            unsubscribe(TOPIC_LIST);
            subList.disconnect();
        }
    }

    /**
     * This method gets called if a messages arrives on any subscribed topic. Because of super class it could be only the {@value TOPIC_LIST} topic.
     * The message body is used to generate a "resource Collection" JSON Object and write it to {@see ListSubscriber#resourceCollectionPath}
     * @param message messagebody
     * @param topic topicName the message was received from
     */
    @Override
    public void processMessage(String message, String topic) {
        try {
            JSONObject resourceCollectionJSON = new JSONObject(message);
            System.out.println("Sucess getting the list.");
            ResourceCollectionWriter writer = new ResourceCollectionWriter(resourceCollectionPath);
            writer.setList(resourceCollectionJSON);
            writer.saveListFile();
            System.out.println("List saved at file: " + resourceCollectionPath);
        } catch (JSONException ex) {
            System.err.println("Error getting the list.\nCheck the received JSON file.");
        }     
    }

    /**
     * This method gets called if the connection to broker is lost. It prints an error message.
     */
    @Override
    public void lostConnection() {
        System.err.println("Error subscribing the list.");
    }    
}
