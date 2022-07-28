/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer.list.networkOperations;

import com.sniffer.mqtt.BroadcastSniffers;
import com.sniffer.list.json.ResourceCollectionReader;
import com.sniffer.list.json.ResourceCollectionWriter;
import com.sniffer.udp.NetworkOperations;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class builds the JSON object for adding a sniffer to resource collection.
 * @author eliseu
 */
public class RegistSniffer {
    private final String resourceCollectionPath;
    private String snifferID;
    //expected operation name
    private static final String REGIST_OPERATION = "regist";
    //expected value at "ip" attribute at config to start autodetection
    private static final String IP_AUTO_DETECTED = "autodetected";
    private JSONObject snifferJSON = new JSONObject();

    /**
     * Constructor of class.
     * @param resourceCollectionPath resourceCollectionPath
     */
    public RegistSniffer(String resourceCollectionPath) {
        this.resourceCollectionPath = resourceCollectionPath;
    }

    /**
     * Constructs sniffer JSON object
     * @param snifferID snifferID
     * @param snifferType snifferType
     * @param transport transport method standard value is "mqtt"
     * @param network network JSON object containing port and ip
     * @throws JSONException thrown on error building JSONObject
     */
    public void createSnifferRegist(String snifferID, String snifferType, String transport, JSONObject network) throws JSONException{
        System.out.println("\nCreating the JSON file for sniffer registration...");
        this.snifferID = snifferID;
        
        snifferJSON.put("sniffer_id", snifferID);
        snifferJSON.put("sniffer_type", snifferType);
        snifferJSON.put("transport", transport);
        snifferJSON.put("network", network);
        JSONArray devicesArray = new JSONArray();
        snifferJSON.put("devices", devicesArray);
    }

    /**
     * Adds remote broker information to the internal sniffer JSON object
     * @param config expects the json object of this systems config file
     * @throws JSONException thrown on error building JSONObject
     */
    public void setRemoteBroker(JSONObject config) throws JSONException{ //TODO: Find out why he called the method setRemoteBROKER, when he is using SNIFFER values ??
        System.out.println("Setting remote broker...");
        JSONObject brokers = config.getJSONObject("remote_brokers");
        JSONObject rBroker = brokers.getJSONObject("sniffer");
        snifferJSON.put("remote_broker", rBroker);
    }

    /**
     * Adds remote broker information to the internal sniffer JSON object. IF IP is autodetected query the ip from local maschine
     * @param config expects the json object of this systems config file
     * @throws JSONException thrown on error building JSONObject
     */
    public void setLocalBroker(JSONObject config) throws JSONException{
        System.out.println("Setting local broker...");
        JSONObject configBroker = config.getJSONObject("local_broker");
        String port = configBroker.getString("port");
        String ip = configBroker.getString("ip");
        if (ip.equals(IP_AUTO_DETECTED)) {
            System.out.println("Autodetecting the IP off that machine...");
            NetworkOperations net = new NetworkOperations();
            ip = net.getLocalIP();
        }
        JSONObject localBroker = new JSONObject();
        localBroker.put("ip", ip);
        localBroker.put("port", port);
        snifferJSON.put("local_broker", localBroker);
        System.out.println("Sniffer IP: " + ip + ", Port: " + port);
    }

    /**
     * Registers a sniffer in resource collection and broadcast registration to local or internet sniffers depending on type of internal snifferIDs sniffer
     * @throws JSONException thrown on read write error of resource collection
     */
    public void registSniffer() throws JSONException{
        ResourceCollectionReader reader = new ResourceCollectionReader(resourceCollectionPath);

        if(reader.findSniffer(snifferID) < 0){
            System.out.println(String.format("Sniffer with id %s wasn't registered in the resource collection.",snifferID));
            
            //add sniffer to list
            System.out.println("Adding sniffer to the list...");
            ResourceCollectionWriter writer = new ResourceCollectionWriter(resourceCollectionPath);
            writer.setList(reader.getList());
            writer.addSniffer(snifferJSON);
            writer.saveListFile();
            
            //register on fiware server Whats this ? TODO: Find out what he wanted to do ?
            //RegistHTTP fiwareHTTP = new RegistHTTP(resourceCollectionPath);
            //fiwareHTTP.registService(new JSONObject(snifferJSON, JSONObject.getNames(snifferJSON)));
            
        } else {
            System.out.println(String.format("Sniffer with %s was already registered in the resource collection.",snifferID));
        }
        
        //register on other sniffers
        System.out.println("Sending the new sniffer registration to the network...");
        BroadcastSniffers send = new BroadcastSniffers(resourceCollectionPath, snifferID);
        send.sendObject(REGIST_OPERATION, snifferJSON);       
    }
}
