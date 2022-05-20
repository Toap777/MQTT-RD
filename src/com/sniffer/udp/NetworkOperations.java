/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


/**
 * This class provides sniffer discovery functionality over UDP multicast request.
 * @author eliseu
 */
public class NetworkOperations {
    //expected request body of message to discover sniffers
    private static final String REQUEST_SNIFFER = "anyonesniffer?";
    //expected response body from discovered sniffers message
    private static final String RESPONSE_SNIFFER = "yes";
    //Discovery message packet size in Byte
    private static final int PACKET_SIZE = 300;
    //Specifies how long to wait for an incoming response after sniffer discovery request
    private static final int TIMEOUT = 5000;

    /**
     * Gets the IP4 address of the local machine
     * @return ipAddress else null if there was an error
     */
    public String getLocalIP(){
        String ipAddress = "";
        try {
            Enumeration netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) netInterfaces.nextElement();
                Enumeration ipAddresses = netInterface.getInetAddresses();
                while (ipAddresses.hasMoreElements()) {
                    InetAddress ip = (InetAddress)ipAddresses.nextElement();
                    if(ip instanceof Inet4Address){
                        Inet4Address ipV4 = (Inet4Address) ip;
                        if(!ipV4.isLoopbackAddress()){
                            ipAddress = ipV4.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException ex) {        
            System.err.println("Can not get the IP from this machine.");
            return null;
        }
        
        return ipAddress;
    }

    /**
     * Sends a multicast UDP request to the network to discover all available sniffers in the local network. If there is no sniffer wait for {@value TIMEOUT} ms.
     * @param mAddress multicast address
     * @param port port to send and recieve
     * @return sniffers IP if there is another sniffer, else null
     */
    public String sendPacketNetwork(String mAddress, int port){
        System.out.println("\nChecking sniffers in the network...");
        
        String rcvMSG = "";
        String deviceIP = "";
        DatagramSocket sockUDP = null;
        try {
            InetAddress addr = InetAddress.getByName(mAddress);
            
            DatagramSocket sendSock = new DatagramSocket();
            
            byte[] requestMessage = REQUEST_SNIFFER.getBytes();
            DatagramPacket requestPacket = new DatagramPacket(requestMessage, requestMessage.length);
            requestPacket.setAddress(addr);
            requestPacket.setPort(port);
            
            sockUDP = new DatagramSocket(port + 1);
            sockUDP.setSoTimeout(TIMEOUT);
            DatagramPacket rcPacket = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
            
            sendSock.send(requestPacket);            
            sockUDP.receive(rcPacket);
            
            rcvMSG = new String(rcPacket.getData()).trim();
            deviceIP = rcPacket.getAddress().getHostAddress();
            
        } catch (IOException ex) {
            System.err.println("Timeout.\nNo sniffers in that network.");
        }
        
        if(sockUDP != null){
            sockUDP.close();
        }
        
        if ((rcvMSG.equals(RESPONSE_SNIFFER))) {
            System.out.println("Detected sniffer at ip: " + deviceIP);
            return deviceIP;
        } else {
            System.err.println("Error getting the sniffer IP address.");
            return null;
        }      
    }    
}
