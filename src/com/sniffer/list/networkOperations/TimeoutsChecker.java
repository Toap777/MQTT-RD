/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer.list.networkOperations;

import com.sniffer.list.sniffer.BrokerInternal;
import com.sniffer.list.json.ResourceCollectionReader;
import com.sniffer.mqtt.GenericClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;

/**
 * This class provides methods to start and manage periodical timeout checks on connected sniffers.
 * @author eliseu
 */
public class TimeoutsChecker {
    private final String snifferID;
    private final String resourceCollectionPath;
    private final String confBroker;
    //timer interval defines how often all sniffers get checked in ms.
    private static final int TIMER_INTERVAL = 40 * 1000;
    private static final String TYPE_INTERNET = "internet";
    private Timer timer = new Timer();
    private final Map<String,Boolean> timeouts = new HashMap<>();
    private Map<String,GenericClient> dataBrokers;

    /**
     * Constructor of TimeoutChecker class
     * @param resourceCollectionPath resourceCollectionPath
     * @param snifferID snifferID of associated sniffer
     * @param confBroker sniffer type of associated sniffer
     */
    public TimeoutsChecker(String resourceCollectionPath, String snifferID, String confBroker) {
        this.resourceCollectionPath = resourceCollectionPath;
        this.snifferID = snifferID;
        this.confBroker = confBroker;
        System.out.println("Configuring the timer responsible by the sniffers timeouts...");
    }

    /**
     * Set this.dataBrokers
     * @param dataBrokers dictionary of snifferId - mqtt client (broker) associations
     */
    public void setSubscriptionsActive(Map<String,GenericClient> dataBrokers){
        this.dataBrokers = dataBrokers;
    }

    /**
     * Set the sniffer with pSnifferID to timeout state.
     * @param pSnifferID id of sniffer to ping to.
     */
    public void setTimeout(String pSnifferID){
        timeouts.put(pSnifferID, true);
    }

    /**
     * Cancel the timer to stop all sniffer timeout checks. Then reread all sniffers from resource collection and restart timer at rate {@value TIMER_INTERVAL} ms.
     * @throws JSONException thrown on error on reading resource collection
     */
    public void resetTimeouts() throws JSONException{
        timeouts.clear();
        timer.cancel();
        timer.purge();
        
        ResourceCollectionReader reader = new ResourceCollectionReader(resourceCollectionPath);
        List<String[]> sniffersList = reader.getSniffers(snifferID, confBroker);
        for (String[] iterator : sniffersList) {
            // check only in the network
            if(!iterator[0].equals(snifferID)){
                timeouts.put(iterator[0], false);
            }
        }
        
        timer = new Timer();
        timer.scheduleAtFixedRate(createTask(), TIMER_INTERVAL, TIMER_INTERVAL);
    }

    /**
     * This task checks if any sniffer is in timeout state. IF yes it deletes it from resource collection and broadcast delete message to all sniffers in the same network.
     * @return A timer task that can be easily repeatedly executed.
     */
    private TimerTask createTask(){
        
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    //Cheack if all timeouts
                    for (Map.Entry<String, Boolean> entry : timeouts.entrySet()) {
                        String rSnifferID = entry.getKey();
                        Boolean timeoutState = entry.getValue();

                        if (!timeoutState) { //maybe this line should be if(timeoutState)
                            System.out.println("\nTimeout in sniffer: " + rSnifferID);
                            System.out.println("Starting delete sniffer operation...");

                            ResourceCollectionReader reader = new ResourceCollectionReader(resourceCollectionPath);
                            String snifferType = reader.getSnifferType(snifferID);
                            String rSnifferNetwork = reader.getSnifferNetwork(rSnifferID);
                            String snifferNetwork = reader.getSnifferNetwork(snifferID);
                            if (snifferType.equals(TYPE_INTERNET) && rSnifferNetwork.equals(snifferNetwork)) {
                                DataManager manager = new DataManager(resourceCollectionPath, snifferID);
                                manager.deleteDataSniffer(rSnifferID, dataBrokers);
                            }

                            DeleteThings del = new DeleteThings(resourceCollectionPath, snifferID);
                            del.deleteSniffer(rSnifferID);
                            timeouts.remove(rSnifferID);
                            del.sendDeleteSniffer(rSnifferID);
                            break;
                        }
                    }
                    resetTimeouts();

                } catch (JSONException ex) {
                    Logger.getLogger(BrokerInternal.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        
        return task;
    }
}
