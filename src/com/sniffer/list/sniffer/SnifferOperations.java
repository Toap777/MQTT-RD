/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer.list.sniffer;

import com.sniffer.list.networkOperations.TimeoutsChecker;
import com.sniffer.list.networkOperations.AddThings;
import com.sniffer.list.networkOperations.DataManager;
import com.sniffer.list.networkOperations.DeleteThings;
import com.sniffer.list.json.ResourceCollectionReader;
import com.sniffer.mqtt.BroadcastSniffers;
import com.sniffer.mqtt.GenericClient;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class implements the main actions a sniffer will execute when receiving special operational messages.
 * @author eliseu
 */
public class SnifferOperations {
    private final String snifferID;
    private final String resourceCollectionPath;
    private final String snifferType;

    //message operations
    private static final String REGIST_OPERATION = "regist";
    private static final String DELETE_DEVICE_OPERATION = "deletedevice";
    private static final String DELETE_SNIFFER_OPERATION = "deletesniffer";
    private static final String PING_ALIVE = "pingalive";

    //flags
    private static final String TYPE_INTERNET = "internet";
    private static final String CONF_BROKER_EXTERNAL = "internet"; //broker type
    private static final String CONF_BROKER_LOCAL = "local"; //broker type
    //broker type of this instance
    private final String confBroker;
    
    private GenericClient internetBroker;
    private Map<String,GenericClient> dataBrokers;
    private final TimeoutsChecker timer;

    /**
     * Class constructor. Initializes Timeout thread and read 7 write object to resource collection.
     * @param resourceCollectionPath resourceCollectionPath
     * @param snifferID snifferID of associated sniffer
     * @param confBroker json representation of config file, values "internet" or "local"
     * @throws JSONException thrown on error on reading resource collection
     */
    public SnifferOperations(String resourceCollectionPath, String snifferID, String confBroker) throws JSONException {
        this.resourceCollectionPath = resourceCollectionPath;
        this.snifferID = snifferID;
        this.confBroker = confBroker;
        
        ResourceCollectionReader reader = new ResourceCollectionReader(resourceCollectionPath);
        this.snifferType = reader.getSnifferType(snifferID);
        
        timer = new TimeoutsChecker(resourceCollectionPath, snifferID, confBroker);
        timer.resetTimeouts();
    }

    /**
     * Initializes subscriptions and timeouts to all brokers contained in dataBrokers.
     * @param internetBroker connection to internet broker
     * @param dataBrokers dictionary sniffer - broker associations
     * @throws JSONException thrown on error on reading resource collection
     */
    protected void setSubscriptionsActive(GenericClient internetBroker, Map<String,GenericClient> dataBrokers) throws JSONException{
        this.internetBroker = internetBroker;
        this.dataBrokers = dataBrokers;
        
        DataManager manager = new DataManager(resourceCollectionPath, snifferID);
        manager.initDataBrokers(internetBroker, dataBrokers);
        
        timer.setSubscriptionsActive(dataBrokers);
    }

    /**
     * This method selects an sniffer action to execute depending on the operation attribute of the message. Available actions are: start ping, delete device, delete sniffer, register device/sniffer
     * @param message mqtt message containing operation attribute and data attribute
     * @throws JSONException thrown on read write error of resource collection
     */
    public void demultiplexAndProcess(String message) throws JSONException{
        JSONObject messageJSON = new JSONObject(message);
        String operation = messageJSON.getString("operation");
        JSONObject objectJSON = (JSONObject) messageJSON.get("object");
        
        switch (operation) {
            case REGIST_OPERATION:
                //regist message received at sniffer sets subscriptions active, forwards message to all sniffers and reset timer
                System.out.println("\nNew external operation in the " + confBroker +" broker.");
                System.out.println("New request for regist operation.");
                AddThings add = new AddThings(resourceCollectionPath, snifferID);
                
                if (snifferType.equals(TYPE_INTERNET)) {
                    add.setSubscriptionActive(internetBroker, dataBrokers);
                    forwardMessageByBroadcast(operation, objectJSON);
                }
                add.addThing(objectJSON);
                timer.resetTimeouts();                
                break;

            case DELETE_DEVICE_OPERATION:
                //delete message received at sniffer triggers a deletion of thing from resource collection. IF internet broker it broadcasts it to all internet brokers.
                System.out.println("\nNew external operation in the " + confBroker +" broker.");
                String rSnifferID = objectJSON.getString("sniffer_id");
                String rDeviceID = objectJSON.getString("device_id");
                System.out.println("New request to delete device: " + rDeviceID + ", at sniffer: " + rSnifferID);
                
                ResourceCollectionReader reader = new ResourceCollectionReader(resourceCollectionPath);
                String rSnifferNetwork = reader.getSnifferNetwork(rSnifferID);
                String snifferNetwork = reader.getSnifferNetwork(snifferID);
                if (snifferType.equals(TYPE_INTERNET) && rSnifferNetwork.equals(snifferNetwork)) {
                    DataManager manager = new DataManager(resourceCollectionPath, snifferID);
                    manager.deleteDataDevice(rSnifferID, rDeviceID, dataBrokers); //delete from databrokers dictionary
                }
                
                DeleteThings deleteDevice = new DeleteThings(resourceCollectionPath, snifferID);
                deleteDevice.deleteDevice(rSnifferID, rDeviceID); //delete from resourcecollection
                timer.resetTimeouts(); //rest timers
                
                if (snifferType.equals(TYPE_INTERNET)) {
                    forwardMessageByBroadcast(operation, objectJSON);
                }              
                break;

            case DELETE_SNIFFER_OPERATION:
                //
                System.out.println("\nNew external operation in the " + confBroker +" broker.");               
                String removeSnifferID = objectJSON.getString("sniffer_id");
                System.out.println("New request to delete: " + removeSnifferID);
                
                ResourceCollectionReader reader1 = new ResourceCollectionReader(resourceCollectionPath);
                
                if(removeSnifferID.equals(snifferID)){ //if this associateds sniffer should be removed TODO: why there is no deletion code
                    System.out.println("Trying to remove this sniffer.");
                    
                    int snifferPosition = reader1.findSniffer(snifferID);
                    JSONArray sniffers = reader1.getList().getJSONArray("sniffers");
                    JSONObject mySniffer = sniffers.getJSONObject(snifferPosition);
                    
                    System.out.println("Registering this sniffer in the network...");
                    //register on other sniffers
                    forwardMessageByBroadcast(REGIST_OPERATION, mySniffer);
                    
                }else {
                    //if its other sniffer
                    DeleteThings deleteSniffer = new DeleteThings(resourceCollectionPath, snifferID);
                    
                    String removeSnifferNetwork = reader1.getSnifferNetwork(removeSnifferID);
                    String snifferNetwork1 = reader1.getSnifferNetwork(snifferID);
                    if (snifferType.equals(TYPE_INTERNET) && removeSnifferNetwork.equals(snifferNetwork1)) {
                        DataManager manager = new DataManager(resourceCollectionPath, snifferID);
                        manager.deleteDataSniffer(removeSnifferID, dataBrokers); //remove from data brokers
                    }
                    
                    deleteSniffer.deleteSniffer(removeSnifferID); //remove from resource collection
                    timer.resetTimeouts();
                    
                    if (snifferType.equals(TYPE_INTERNET)){
                        forwardMessageByBroadcast(operation, objectJSON);
                    }
                }
                break;
                
            case PING_ALIVE:
                //
                String pSnifferID = objectJSON.getString("sniffer_id");
                timer.setTimeout(pSnifferID); //timer does pinging for us
                break;

            default:
                break;
        }
    }

    /**
     * Broadcasts a message further into the network. Broadcast to all local or all local and internet sniffers depending on the broker type of this instance.
     * @param operation operation
     * @param objectJSON operation data
     * @throws JSONException thrown on error on read or write resource collection
     */
    private void forwardMessageByBroadcast(String operation, JSONObject objectJSON) throws JSONException {
        BroadcastSniffers broadcast = new BroadcastSniffers(resourceCollectionPath, snifferID);

        switch (confBroker) {
            case CONF_BROKER_EXTERNAL:
                //broadcast to the rest of the network
                System.out.println("Sending the new thing registration to the rest of local network.");
                broadcast.sendLocal(operation, objectJSON);
                break;
                
            case CONF_BROKER_LOCAL:
                //send to the internet
                System.out.println("Sending the new thing registration to the rest of internet sniffers.");
                broadcast.sendInternet(operation, objectJSON);
                break;
                
            default:
                break;
        }
    }
}
