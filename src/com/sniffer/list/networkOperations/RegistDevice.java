/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer.list.networkOperations;

import com.sniffer.mqtt.BroadcastSniffers;
import com.sniffer.list.json.ResourceCollectionReader;
import com.sniffer.mqtt.GenericClient;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class builds JSON object for device, publishes its registration and eventually sub to devices topics. TODO: Determine device definition
 * @author eliseu
 */
public class RegistDevice {
    private final String resourceCollectionPath;
    private final String snifferID;

    //expected message values
    private static final String REGIST_OPERATION = "regist";
    private static final String DEVICE_BROKER = "broker";
    private static final String DEVICE_NO_BROKER = "nobroker";
    private static final String TYPE_INTERNET = "internet";
    private static final String THREAD_NEW_BROKER = "newbroker";
    
    private Map<String,GenericClient> dataBrokers;

    /**
     * Constructor of the class
     * @param resourceCollectionPath resource collection file path
     * @param snifferID id of associated sniffer
     */
    public RegistDevice(String resourceCollectionPath, String snifferID) {
        this.resourceCollectionPath = resourceCollectionPath;
        this.snifferID = snifferID;
    }

    /**
     * Sets the internal dataBrokers list. As suggested of the method name the subscriptions will NOT be set instantly active.
     * Eventually they will be set active if addDevice() is called.
     * @param dataBrokers dictionary of snifferID - mqttClient (as broker) associations
     */
    public void setSubscriptionsActive(Map<String,GenericClient> dataBrokers) {
        this.dataBrokers = dataBrokers;        
    }

    /**
     * Handles device registration of type DEVICE_BROKER, DEVICE_BROKER (discovered by network checker) AND DEVICE_NO_BROKER.
     * Subscribes to all device topics if this instance is associated with Internet sniffer OR DEVICE_Broker (discovered by network checker)
     * @param message mqtt message containing device description at the same format like resource collection
     * @param mqtt mqtt client object to subscribe to topics
     * @param firstRegist Seems to expect following interdependence: firstRegister: true -> brokerIP is specified; firstRegister: false -> brokerIP is not specified in message
     * @param deviceToken Stores information about new device, gets written in this method
     * @throws JSONException thrown on reading or writing error of resource collection
     */
    public void newDevice(String message, GenericClient mqtt, Boolean firstRegist, String[] deviceToken) throws JSONException {
        //create json object from message
        JSONObject newDeviceJSON = new JSONObject(message);
        ResourceCollectionReader reader = new ResourceCollectionReader(resourceCollectionPath);

        //add device id and type
        String newDeviceID = newDeviceJSON.getString("device_id");
        String newDeviceType = newDeviceJSON.getString("device_type");
        System.out.println("Registering the device: " + newDeviceID + " of type: " + newDeviceType);
        
        switch (newDeviceType) {
            // CASE I: there is a device with associated broker, maybe that represents broker change -> store it and notify all observers
            case DEVICE_BROKER:
                try {
                    System.out.println("Registration of a device with broker.");
                    
                    // if is a device broker registered by this sniffer
                    deviceToken[0] = THREAD_NEW_BROKER; //set to create a new thread
                    deviceToken[1] = newDeviceID;
                    JSONObject newBroker = newDeviceJSON.getJSONObject("local_broker"); //throws an exception if no local_broker attribute is specified.
                    deviceToken[2] = newBroker.getString("ip");
                    deviceToken[3] = newBroker.getString("port");
                    registDevice(newDeviceJSON); //all sniffers should know about
                    System.out.println("Registration request using the sniffer broker.");
                    
                    synchronized (deviceToken) {
                        deviceToken.notify();
                    }
                //CASE II: The device don't have associated broker and was discovered via UDP -> register on in message specified broker and setup subscription to all device topics.
                //These handles devices without broker.
                } catch (JSONException ex) {
                    //if its a device broker (broker device ?) discovered by the network checker
                    System.out.println("The JSON doesn't have specify the broker IP.");
                    //TODO: find out how this gets set: True if calling BrokerDevice was started by sniffer or it comes from a new topic_Regist Device request.
                    if (!firstRegist) {
                        System.out.println("Registration request sent by UDP.");
                        deviceToken[1] = newDeviceID;
                        //add message contained broker object to representation
                        registDeviceWithIP(newDeviceJSON, deviceToken[2], deviceToken[3]);
                        //setup datamanager
                        DataManager data = new DataManager(resourceCollectionPath, snifferID);
                        //subscribe to all device topics
                        data.subscribeDeviceTopics(snifferID, deviceToken[1], mqtt);
                
                    } else {
                        System.err.println("Error you must specify the broker parameters.");
                        // The catch block seems to expect following interdependence: firstRegister: true -> brokerIP is specified; firstRegister: false -> brokerIP is not specified
                    }
                }
                break;
                //CASE III: Device with no associated broker -> register on local broker
            case DEVICE_NO_BROKER:
                // set the ip off the sniffer
                System.out.println("Registration of a device without broker.");
                //register device at local sniffer
                String[] ipAndPort = reader.getSnifferLocalBroker(snifferID);
                //save and broadcast everything
                registDeviceWithIP(newDeviceJSON, ipAndPort[0], ipAndPort[1]);
                break;
                
            default:
                break;
        }
        //OPTIONAL: If this instance is associated with internet sniffer than
        //If this is a internet sniffer add new device to dataBrokers list and subscribe to all device topics.
        //TODO: Find out why we dont do this if device is a broker / sniffer ?
        String snifferType = reader.getSnifferType(snifferID);
        if (snifferType.equals(TYPE_INTERNET)) {
            // create new datamanager
            DataManager data = new DataManager(resourceCollectionPath, snifferID);
            //subscribe and update data broker list
            data.addDataDevice(newDeviceID, snifferID, dataBrokers);
        }
    }

    /**
     * Finishes the device JSON representation by adding local_broker object to it AND registers the device.
     * @param deviceJSON device JSON representation
     * @param deviceIP local brokers IP
     * @param port local brokers port
     * @throws JSONException thrown on JSON read or write error
     */
    private void registDeviceWithIP(JSONObject deviceJSON, String deviceIP, String port) throws JSONException{
        JSONObject lBroker = new JSONObject();
        lBroker.put("ip", deviceIP);
        lBroker.put("port", port);
        deviceJSON.put("local_broker", lBroker); 
        registDevice(deviceJSON);
    }

    /**
     * Stores a device at resource collection and notify all sniffers about its registration via broadcast.
     * @param newDeviceJSON device representation
     * @throws JSONException thrown on JSON read or write error
     */
    private void registDevice(JSONObject newDeviceJSON) throws JSONException {
        ResourceCollectionReader reader = new ResourceCollectionReader(resourceCollectionPath);
        
        int positionSniffer = reader.findSniffer(snifferID);
        String newDeviceID = newDeviceJSON.getString("device_id");
        int positionDevice = reader.findDevice(positionSniffer, newDeviceID);
        if (positionDevice < 0) {
            //regist the device in fiware
            //RegistHTTP http = new RegistHTTP(resourceCollectionPath);
            //http.registDeviceWithIP(new JSONObject(deviceJSON, JSONObject.getNames(deviceJSON)), snifferID);
            
            //add device to the list
            AddThings add = new AddThings(resourceCollectionPath, snifferID);
            add.addDevice(snifferID, newDeviceJSON);

            //rereads the just saved sniffer object
            reader.readFile();
            JSONArray sniffersArray = reader.getList().getJSONArray("sniffers");
            JSONObject snifferJSON = (JSONObject) sniffersArray.get(positionSniffer);
            
            //regist the sniffer (with the new device) in the others sniffers
            System.out.println("Sending the new device to the other sniffers.");
            BroadcastSniffers broad = new BroadcastSniffers(resourceCollectionPath, snifferID);
            broad.sendObject(REGIST_OPERATION, snifferJSON);
        }
    }
}
