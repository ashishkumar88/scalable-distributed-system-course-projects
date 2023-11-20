package edu.northeastern.cs6650.project1.client;
/**
 * Copyright 2023 Ashish Kumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;

import edu.northeastern.cs6650.project1.client.BaseClient;
import edu.northeastern.cs6650.project1.common.Utils;
import edu.northeastern.cs6650.project1.common.ServerResponseCode;

/*
 * Implementation of the UDP client 
 * 
 * The UDP client sends a request to the UDP server, waits for a response 
 * and processes the response.
 */
public class UDPClient extends BaseClient {
    private static final Logger LOGGER = Logger.getLogger(UDPClient.class.getName());
    
    private String serverIPAddress;
    private int serverPort;

    public UDPClient(String serverIPAddress, int serverPort) {
        this.serverIPAddress = serverIPAddress;
        this.serverPort = serverPort;
    }
    
    /*
     * Sends a request to the UDP server and process the server response
     */
    public void makeServerRequest(StringBuffer requestBody) {

        try {
            DatagramSocket serverSocket = new DatagramSocket();

            byte[] requestBytes = requestBody.toString().getBytes();
            
            InetAddress serverHost = InetAddress.getByName(this.serverIPAddress);
            DatagramPacket requestPacket = new DatagramPacket(requestBytes, requestBody.length(), serverHost, this.serverPort);
            serverSocket.send(requestPacket);

            // set timeout for server response
            serverSocket.setSoTimeout(Utils.SERVER_TIMEOUT);

            processServerResponse(serverSocket);    
        } catch (Exception exp){
            LOGGER.severe("Error sending message to the server.");
        }

    }

    /*
     * Process response from the UDP server
     */
    protected void processServerResponse(DatagramSocket serverSocket) {

        try {
            byte[] buffer = new byte[10000]; 
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
            serverSocket.receive(reply);

            this.processServerResponse(new StringBuffer(new String(reply.getData())));
        } catch (SocketTimeoutException e) {
            LOGGER.severe("Connection to server timed out.");
        } catch(Exception exp) {
            LOGGER.severe("Error processing server response.");
        }
        
    }

}
