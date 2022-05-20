/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer;

import java.io.IOException;
import org.json.JSONException;

/**
 * Entry point of the program.
 * @author eliseu
 * @version 19.05.22.V1
 */
public class Main {
    
    /**
     * Creates InitSystem object and starts it.
     * @param args the command line arguments
     * @throws java.lang.InterruptedException An interrupt exception
     * @throws org.json.JSONException An parsing exception
     * @throws IOException An io exception
     */
    public static void main(String[] args) throws JSONException, InterruptedException, IOException{
        InitSystem init = new InitSystem();
        init.startSniffer();
    }
}
