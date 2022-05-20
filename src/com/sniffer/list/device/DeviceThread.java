/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer.list.device;

import com.sniffer.list.networkOperations.DataManager;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONException;

/**
 * This class startup a device and synchronize the devices behaviour with the internal broker. It runs the sniffers main loop.
 * TODO: Revise class description with more exact definition of word "device".
 * @author eliseu
 */
public class DeviceThread implements Runnable{
    private final String snifferID;
    private final String resourceCollectionPath;
    private final Boolean startedBySniffer;
    private final String[] deviceToken = new String[4];
    //expected topic for receiving device information on
    private static final String TOPIC_REGIST_DEVICE = "/registdevice";
    //expected message content for starting device thread
    private static final String THREAD_NEW_BROKER = "newbroker";
    //expected message content for killing broker thread
    private static final String THREAD_KILL = "threadkill";

    /**
     * Constructor of this class
     * @param resourceCollectionPath resourceCollectionPath
     * @param snifferID associated snifferID this thread was started from
     * @param deviceID associated deviceID
     * @param deviceIP deviceIP
     * @param devicePort devicePort
     * @param startedBySniffer if this device (broker) was started by sniffer
     */
    public DeviceThread(String resourceCollectionPath, String snifferID, String deviceID, String deviceIP, String devicePort, Boolean startedBySniffer) {
        this.resourceCollectionPath = resourceCollectionPath;
        this.snifferID = snifferID;
        
        this.deviceToken[1] = deviceID;
        this.deviceToken[2] = deviceIP;
        this.deviceToken[3] = devicePort;
        
        this.startedBySniffer = startedBySniffer;
    }

    /**
     * This method loads this device (sniffer) by setting up an associated broker and then manages the Broker threads signals on the system states.
     */
    @Override
    public void run() {
        try {
            System.out.println("\nStarting the device local broker...");
            BrokerDevice brokerDevice = new BrokerDevice(resourceCollectionPath, snifferID, deviceToken);
            //set sniffer type mode
            brokerDevice.setBrokerType(startedBySniffer);
            //inject the message processing callback
            brokerDevice.connectInternalBroker(deviceToken[1] + "DeviceBroker", deviceToken[2], deviceToken[3]);
            //subscribe to regist topic
            brokerDevice.subscribe(TOPIC_REGIST_DEVICE);
            //if there is already a sniffer, we can set up the datamanager
            if (startedBySniffer) {
                DataManager manager = new DataManager(resourceCollectionPath, snifferID);
                //subscribe to all currently registered deviceTopics
                manager.subscribeDeviceTopics(snifferID, deviceToken[1], brokerDevice);                
            }
            
            while (true) {
                //only allow to enter one device thread this code area at a moment, wait for deviceToken.notify() on connection lost
                synchronized(deviceToken){
                    deviceToken.wait();
                }
                
                switch (deviceToken[0]) {
                    //if a new broker registers it spawns another sniffer ?
                    case THREAD_NEW_BROKER:
                        (new Thread(new DeviceThread(resourceCollectionPath, snifferID, deviceToken[1], deviceToken[2], deviceToken[3], true), "DeviceThread")).start();
                        break;
                    //kill this device thread
                    case THREAD_KILL:
                        brokerDevice.getClient().close();
                        Thread.currentThread().interrupt();
                        break;
                    default:
                        break;
                }
            }         
            
        } catch (JSONException | InterruptedException | MqttException ex) {
            Logger.getLogger(DeviceThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
