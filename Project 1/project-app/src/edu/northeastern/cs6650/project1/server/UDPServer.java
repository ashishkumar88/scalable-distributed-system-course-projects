package edu.northeastern.cs6650.project1.server;
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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import edu.northeastern.cs6650.project1.common.ServerResponseCode;
import edu.northeastern.cs6650.project1.common.Utils;
import edu.northeastern.cs6650.project1.server.BaseServer;

/*
 * Implementation of the UDP server 
 * 
 * The UDP server listens for a request from the UDP client and responds
 * to one request at a time. 
 */
public class UDPServer extends BaseServer{
    private static final Logger LOGGER = Logger.getLogger(UDPServer.class.getName());
    private DatagramSocket serverSocket;
    
    public UDPServer(int serverPort) throws SocketException {
        serverSocket = new DatagramSocket(serverPort);
    }    

    /*
     * This spins the server to listen indefinitely for requests from the TCP client
     */
    public void spin() {
        while(true) {
            byte[] buffer = new byte[256];  
            StringBuffer serverResponse = null;
            StringBuffer requestBody = new StringBuffer();
            InetAddress clientAdress = null;
            int clientPort = -1;

            try {

                // Read input from the user
                // Read performed by reading 256 bytes at a time
                do {
                    DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                    serverSocket.receive(request);
                    String received = new String(request.getData(), 0, request.getLength());
                    requestBody.append(received);

                    if(clientAdress == null) {
                        clientAdress = request.getAddress();
                    }

                    if(clientPort < 0) {
                        clientPort = request.getPort();
                    }

                    if(received.contains(Utils.END_OF_MESSAGE)) {
                        break;
                    } 
                } while(true);
                
                // Process the request
                String message = this.processRequest(requestBody, clientAdress.getHostAddress(), clientPort);

                // Prepare a response body
                serverResponse = Utils.createResponseBody(ServerResponseCode.SUCCESS, message);
            } catch(IOException ioe) {

                // Prepare a response body
                serverResponse = Utils.createResponseBody(ServerResponseCode.FAILED, ioe.getMessage());
            } catch(IllegalArgumentException iae) {

                // Prepare a response body
                serverResponse = Utils.createResponseBody(ServerResponseCode.BAD_REQUEST, iae.getMessage());
            } catch(NoSuchElementException nee) {

                // Prepare a response body
                serverResponse = Utils.createResponseBody(ServerResponseCode.FAILED, nee.getMessage());
            }    
            
            LOGGER.info("Server response is below.\n"+ serverResponse.toString());

            // Sent response to the client
            try {
                sendResponseToClient(clientAdress, clientPort, serverResponse.toString());
            } catch (Exception exp) {
                LOGGER.severe("Error sending response to the client.");
            }
        }
    }

    /*
     * Send a response body to the client
     */
    protected void sendResponseToClient(InetAddress clientAddress, int clientPort, String message) throws IOException {
        DatagramPacket responsePacket = new DatagramPacket(message.getBytes(), message.length(), clientAddress, clientPort);
        serverSocket.send(responsePacket);
    }
}
