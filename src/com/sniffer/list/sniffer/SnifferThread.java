/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer.list.sniffer;

import com.sniffer.list.device.DeviceThread;
import com.sniffer.list.sniffer.publish.PingThread;
import com.sniffer.list.sniffer.publish.PublishResourceCollectionThread;
import com.sniffer.list.json.ResourceCollectionReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;

/**
 * This class coordinates the initialization and runtime of a sniffer in the MQTT-RD network. Depending on sniffer types it connects to 1-n brokers.
 * @author eliseu
 */
public class SnifferThread implements Runnable{
    private final String snifferID;
    private final String resourceCollectionPath;
    private static final String TOPIC_SNIFFER = "/sniffercommunication";
    private static final String TOPIC_REGIST_DEVICE = "/registdevice";
    
    private static final String TYPE_INTERNET = "internet";
    private static final String CONF_BROKER_EXTERNAL = "internet";
    private static final String CONF_BROKER_LOCAL = "local";
    private static final String THREAD_NEW_BROKER = "newbroker";

    /**
     * Constructor of class.
     * @param resourceCollectionPath resourceCollectionPath
     * @param snifferID associated sniffer's id
     */
    public SnifferThread(String resourceCollectionPath, String snifferID) {
        this.resourceCollectionPath = resourceCollectionPath;
        this.snifferID = snifferID;
    }

    /**
     * Set up a sniffer by building a connection to internal broker and for type INTERNET_Broker also to an external broker. Starts timed tasks to ping and publish resource collection
     * in a periodically manner. This sniffer will subscribe to {@value TOPIC_SNIFFER} and {@value TOPIC_REGIST_DEVICE} by default.
     * After finishing all of this it will wait for incoming signals from sub threads and eventually open up new broker connections to newly joined devices.
     */
    @Override
    public void run() {
        try {
            //broker seems to be a specialized client.
            System.out.println("\nStarting the thread for the local Broker...");
            String[] deviceToken = new String[4];
            
            ResourceCollectionReader reader = new ResourceCollectionReader(resourceCollectionPath);
            String[] ipAndPort = reader.getSnifferLocalBroker(snifferID);
            
            BrokerInternal brokerInt = new BrokerInternal(resourceCollectionPath, snifferID, CONF_BROKER_LOCAL, deviceToken);
            brokerInt.connectInternalBroker(snifferID + "LocalBroker", ipAndPort[0], ipAndPort[1]);
            brokerInt.subscribe(TOPIC_REGIST_DEVICE);
            brokerInt.subscribe(TOPIC_SNIFFER);
            
            (new Thread(new PublishResourceCollectionThread(resourceCollectionPath, brokerInt), "LocalPublishList")).start();
            (new Thread(new PingThread(resourceCollectionPath, snifferID), "LocalPingDevices")).start();
            
            String snifferType = reader.getSnifferType(snifferID);

            if (snifferType.equals(TYPE_INTERNET)) {
                System.out.println("\nStarting the thread for the Internet Broker...");
                String[] brokerPar = reader.getSnifferRemoteBroker(snifferID);
                BrokerExternal brokerExt = new BrokerExternal(resourceCollectionPath, snifferID, CONF_BROKER_EXTERNAL);
                brokerExt.connectExternalBroker(snifferID + "ExternalBroker", brokerPar[0], brokerPar[1], brokerPar[2], brokerPar[3]);
                brokerExt.subscribe(TOPIC_SNIFFER);

                (new Thread(new PublishResourceCollectionThread(resourceCollectionPath, brokerExt), "ExternalPublishList")).start();
                
                //subscribing the data from others sniffers
                System.out.println("Starting subscribe data from local devices and sniffers...");
                brokerInt.setSubscriptionActive(brokerExt);
            }

            //wait on sub thread actions like KILL or new broker
            while (true) {
                synchronized(deviceToken){
                    deviceToken.wait();
                }
                
                switch (deviceToken[0]) {
                    //opens up a connection to new broker, if a device with associated broker joins.
                    case THREAD_NEW_BROKER:
                        (new Thread(new DeviceThread(resourceCollectionPath, snifferID, deviceToken[1], deviceToken[2], deviceToken[3], true), "DeviceThread")).start();
                        break;
                    default:
                        break;
                }
            }
            
        } catch (JSONException | InterruptedException ex) {
            Logger.getLogger(SnifferThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
