/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer.list.networkOperations;

import com.sniffer.list.sniffer.BrokerData;
import com.sniffer.list.json.ResourceCollectionReader;
import com.sniffer.mqtt.BasicClientMQTT;
import com.sniffer.mqtt.GenericClient;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class is responsible for managing the systems mqtt connection objects, sending and receiving data. 
 *  It coordinates the cooperation of resourceCollection read/ write classes and mqtt connection classes.
 *  Per active device the system instantiates one data manager at the sniffer. See {@link AddThings#addThing(JSONObject)}
 * @author eliseu
 */
public class DataManager {
    private final String resourceCollectionPath;
    private final String snifferID;
    //object to read and write from resource collection file
    private final ResourceCollectionReader reader;
    //expected name for internet sniffer type
    private static final String TYPE_INTERNET = "internet";
    //expected name for local sniffer type
    private static final String TYPE_LOCAL = "local";

    /**
     * Constructor of class
     * @param resourceCollectionPath path to resource collection file
     * @param snifferID snifferID
     * @throws JSONException thrown if resource collection is not read or writeable
     */
    public DataManager(String resourceCollectionPath, String snifferID) throws JSONException {
        this.resourceCollectionPath = resourceCollectionPath;
        this.snifferID = snifferID;
        
        reader = new ResourceCollectionReader(resourceCollectionPath);
    }

    /**
     * Initializes the connections to all local available brokers. The system will be able to receive mqtt messages from all device topics after successful call of this method.
     * @param internetBroker this MQTT Client
     * @param dataBrokers empty (!) list of remote brokers. Will be writen in the method.
     * @throws JSONException thrown on JSON read error
     */
    public void initDataBrokers(GenericClient internetBroker, Map<String,GenericClient> dataBrokers) throws JSONException{
        List<String[]> networkSniffers = reader.getSniffers(snifferID, TYPE_LOCAL);
        for (String[] localSniffer : networkSniffers) {
            addDataSniffer(localSniffer[0], internetBroker, dataBrokers);
        }
    }

    /**
     * Precondition: snifferID is already defined.Initializes a sniffer with snifferID from resource collection and makes them connect to all of their specified devices topics. Saves to dataBrokers list.
     * @param newSnifferID snifferID
     * @param internetBroker mqtt Client representing a broker TODO: clarify the usage of this object.
     * @param dataBrokers dictionary of sniffer-broker associations. Gets updated by this method.
     * @throws JSONException thrown on JSON read error
     */
    public void addDataSniffer(String newSnifferID, GenericClient internetBroker, Map<String,GenericClient> dataBrokers) throws JSONException{
        System.out.println("Subscribing data from sniffer: " + newSnifferID);
        String snifferType = reader.getSnifferType(snifferID);
        String snifferNetwork = reader.getSnifferNetwork(snifferID);
        String newSnifferNetwork = reader.getSnifferNetwork(newSnifferID);

        //if the new sniffer is associated to the same network as this dataManager snifferID than initialize broker and subscibe topics
        if (snifferType.equals(TYPE_INTERNET) && newSnifferNetwork.equals(snifferNetwork)) {
            //create object for message buffering / Sending
            BrokerData dataSniffer = new BrokerData(resourceCollectionPath, newSnifferID, internetBroker);
            //get broker network address
            String[] brokerConn = reader.getSnifferLocalBroker(newSnifferID);
            //connect and register BrokerData's callback to incoming messages
            dataSniffer.connectInternalBroker(newSnifferID + "DataSubscriber", brokerConn[0], brokerConn[1]);
            //subscribe to all devices with their attributes
            subscribeSnifferDevices(newSnifferID, dataSniffer);
            //put broker into broker list associated by snifferID
            dataBrokers.put(newSnifferID, dataSniffer);
        }
    }

    /**
     * Precondition: newDeviceID and newSnifferID is already specified in resource collection. Subscribes to all attributes / topics of device with deviceID. Saves to dataBrokers list.
     * @param newDeviceID new devices id
     * @param newSnifferID snifferID TODO: Clarify if this sniffer is really interpreted as new at method call time.
     * @param dataBrokers dictionary of sniffer-broker associations. Gets updated by this method.
     * @throws JSONException thrown on JSON read error
     */
    public void addDataDevice(String newDeviceID, String newSnifferID, Map<String,GenericClient> dataBrokers) throws JSONException{
        System.out.println("Subscribing data from device: " + newDeviceID + ", at sniffer: " + newSnifferID);
        String snifferType = reader.getSnifferType(snifferID);
        String snifferNetwork = reader.getSnifferNetwork(snifferID);
        String newSnifferNetwork = reader.getSnifferNetwork(newSnifferID);
        //if the new sniffer is associated to the same network as this dataManager snifferID than initialize broker and subscibe topics
        if (snifferType.equals(TYPE_INTERNET) && newSnifferNetwork.equals(snifferNetwork)) {
            //get specific broker associated with new sniffers ID from databroker list
            GenericClient dataSniffer = dataBrokers.get(newSnifferID);
            //subscribe to all topics for a new device on broker associated with internal snifferID
            subscribeDeviceTopics(newSnifferID, newDeviceID, dataSniffer);
            //update sniffer-broker associations
            dataBrokers.put(newSnifferID, dataSniffer);
        }
    }

    /**
     * Subscribes to all attributes of all devices of specified snifferID.
     * @param subSnifferID snifferID
     * @param mqtt MQTT Client
     * @throws JSONException
     */
    private void subscribeSnifferDevices(String subSnifferID, GenericClient mqtt) throws JSONException{
        String [][] devices = reader.getSnifferDevices(subSnifferID);
        for (String[] device : devices) {
            subscribeDeviceTopics(subSnifferID, device[0], mqtt);
        }
    }

    /**
     * This method subscribes to all device attributes of device with deviceID
     * @param subSnifferID ID of sniffer with the selected device
     * @param subDeviceID ID of selected device
     * @param mqtt MQTT-Client
     * @throws JSONException thrown on read / write error
     */
    public void subscribeDeviceTopics(String subSnifferID, String subDeviceID, GenericClient mqtt) throws JSONException {
        String[] deviceAttributes = reader.getDeviceAttributes(subSnifferID, subDeviceID);
        for (String deviceAttribute : deviceAttributes) {
            String topicString = "/" + subDeviceID + "/attrs/" + deviceAttribute; //build topic names e.g. /temp-2/attrs/temperature
            mqtt.subscribe(topicString); // subscribe to the topic with mqtt client
            Queue<String> topicData = new LinkedList<>(); //create new topic message buffer
            mqtt.getTopicsMap().put(topicString, topicData); // add it to the list of all topic buffers.
        }
    }

    /**
     * This method handles incoming message to this instance and send it to sniffer with internal snifferID. It manages the topic's message buffers and always publishes the latest message.
     * @param message MQTT message
     * @param topic topic of the message
     * @param topicsMap a dictionary containing topic names and associated buffers
     * @param limitMessages defines how many messages get stored in a topics buffer
     * @throws JSONException thrown on read / write error
     */
    public void newDataMessageDevice(String message, String topic, Map<String, Queue<String>> topicsMap, int limitMessages) throws JSONException{
        if (topicsMap.containsKey(topic)) {
            Queue<String> topicData = topicsMap.get(topic);
            if (topicData.size() >= limitMessages) {
                topicData.clear();
            }else
            {
                topicData.add(message);
            }
            sendData(message, topic);
           topicsMap.put(topic, topicData);
        }

           /*  if (topicsMap.containsKey(topic)) {
                      Queue<String> topicData = topicsMap.get(topic);
                      topicData.add(message);
                      if (topicData.size() > limitMessages) {
                          while (topicData.size() > 1) {
                              topicData.remove();
                          }
                          sendData(topicData.remove(), topic);
                     }
                     topicsMap.put(topic, topicData);
                 }*/

        // Version 2: weniger I/O
//        if (topicsMap.containsKey(topic)) {
//            Queue<String> topicData = topicsMap.get(topic);
//
//            if (topicData.size() >= limitMessages) {
//                topicData.clear();
//                sendData(message, topic);
//            }else
//            {
//                topicData.add(message);
//            }
//
//            topicsMap.put(topic, topicData);
//        }
    }

    /**
     * Sends an message to sniffer with internal snifferID of this class
     * @param message message to send
     * @param topic topic to send on
     * @throws JSONException
     */
    private synchronized void sendData(String message, String topic) throws JSONException {
        String ipAndPort[] = reader.getSnifferLocalBroker(snifferID);
        String snifferURL = "tcp://" + ipAndPort[0] + ":" + ipAndPort[1];
        //create new connection
        BasicClientMQTT mqtt = new BasicClientMQTT();
        mqtt.connect(snifferURL, snifferID + "Publisher");
        mqtt.publish(message, topic);

        //end connection
        mqtt.disconnect();
    }

    /**
     * This method handles incoming message to this instance and send it with internet broker. It manages the topic's message buffers and always publishes the latest message by using the "internetBroker".
     * @param message MQTT message
     * @param topic topic of the message
     * @param internetBroker MQTT-Client
     * @param topicsMap a dictionary containing topic names and associated buffers
     * @param limitMessages defines how many messages get stored in a topics buffer
     */
    public void newDataMessageSniffer(String message, String topic, GenericClient internetBroker, Map<String, Queue<String>> topicsMap, int limitMessages){
        
          if (topicsMap.containsKey(topic)) {
                      //get message queue (buffer) of topic
                      Queue<String> topicData = topicsMap.get(topic);
                      //add new message
                      topicData.add(message);
                      //if queue is full
                      if (topicData.size() > limitMessages) {
                          //delete until one message is left.
                          while (topicData.size() > 1) {
                              topicData.remove();
                          }
                          //remove and publish the last element. Should be the latest one.
                          internetBroker.publish(topicData.remove(), topic);
                      }
                      //overwrite old buffer at topic map
                      topicsMap.put(topic, topicData);
                }


//        //Changed code because of one avoidable IO Access
//        if (topicsMap.containsKey(topic)) {
//            //get message queue (buffer) of topic
//            Queue<String> topicData = topicsMap.get(topic);
//            //add new message
//            //if queue is full
//            if (topicData.size() >= limitMessages) {
//                //delete until one message is left.
//                topicData.clear();
//                //remove and publish the last element. Should be the latest one.
//                internetBroker.publish(message, topic);
//            }
//            else
//                topicData.add(message);
//
//            //overwrite old buffer at topic map
//            topicsMap.put(topic, topicData);
//        }
    }

    /**
     * Removes sniffer from dataBrokers and unsubscribe to all of sniffer's devices.
     * @param delSnifferID sniffer to delete
     * @param dataBrokers dictionary of sniffer-broker associations. Gets updated by this method.
     * @throws JSONException thrown on read error of resource collection
     */
    public void deleteDataSniffer(String delSnifferID, Map<String,GenericClient> dataBrokers) throws JSONException{
        System.out.println("Removing data subscriber from sniffer: " + delSnifferID);
        //remove data broker from association list
        GenericClient delDataBroker = dataBrokers.remove(delSnifferID);

        unsubscribeSnifferDevices(delSnifferID, delDataBroker);
        delDataBroker.getTopicsMap().clear();
        delDataBroker.disconnect();
    }

    /**
     * Removes subscribtions to device attributes and removes broker with delDeviceID from broker list. TODO: find out why this method is needed ?
     * @param delSnifferID sniffer to delete
     * @param delDeviceID device to unsubscribe
     * @param dataBrokers dictionary of sniffer-broker associations. Gets updated by this method.
     * @throws JSONException thrown on read error of resource collection
     */
    public void deleteDataDevice(String delSnifferID, String delDeviceID, Map<String,GenericClient> dataBrokers) throws JSONException{
        System.out.println("Removing data subscriber from device: " + delDeviceID + ", at sniffer: " + delSnifferID);
        GenericClient delDataBroker = dataBrokers.get(delSnifferID);
        unsubscribeDeviceTopics(delSnifferID, delDeviceID, delDataBroker);
    }

    /**
     * Unsubscribe from each each attribute of each device of specified sniffer.
     * @param unsubSnifferID sniffer id
     * @param mqtt mqtt client
     * @throws JSONException thrown on read error of resource collection
     */
    private void unsubscribeSnifferDevices(String unsubSnifferID, GenericClient mqtt) throws JSONException{
        String [][] devices = reader.getSnifferDevices(unsubSnifferID);
        for (String[] device : devices) {
            unsubscribeDeviceTopics(unsubSnifferID, device[0], mqtt);
        }
    }

    /**
     * Unsubscribe to each attribute of specified device (unsubDeviceID)
     * @param unsubSnifferID sniffer the device is associated to
     * @param unsubDeviceID deviceID
     * @param mqtt mqtt client
     * @throws JSONException thrown on read error of resource collection
     */
    private void unsubscribeDeviceTopics(String unsubSnifferID, String unsubDeviceID, GenericClient mqtt) throws JSONException{
        String[] deviceAttributes = reader.getDeviceAttributes(unsubSnifferID, unsubDeviceID);
        for (String deviceAttribute : deviceAttributes) {
            String topicString = "/" + unsubDeviceID + "/attrs/" + deviceAttribute;
            mqtt.unsubscribe(topicString);
        }        
    }

}
