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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import edu.northeastern.cs6650.project1.common.ServerResponseCode;
import edu.northeastern.cs6650.project1.common.Utils;
import edu.northeastern.cs6650.project1.server.BaseServer;

/*
 * Implementation of the TCP server 
 * 
 * The TCP server listens for a request from the TCP client and responds
 * to one request at a time. 
 */
public class TCPServer extends BaseServer {

    private static final Logger LOGGER = Logger.getLogger(TCPServer.class.getName());
    
    private ServerSocket serverSocket;
    private int serverPort;
    
    public TCPServer(int serverPort) throws IOException {
        this.serverSocket = new ServerSocket(serverPort);
        this.serverPort = serverPort;
    }

    /*
     * This spins the server to listen indefinitely for requests from the TCP client
     */
    public void spin() {

        while(true) {
            StringBuffer serverResponse = null;
            Socket clientSocket = null;

            try {
                clientSocket = this.serverSocket.accept();

                // Read input from the user
                InputStream clientSocketInputStream = clientSocket.getInputStream();
                StringBuffer requestBody = new StringBuffer();
                
                byte[] buffer = new byte[1024];
                int readLength;
                while((readLength = clientSocketInputStream.read(buffer)) != -1) {
                    String received = new String(buffer, 0, readLength);
                    requestBody.append(received);
                    
                    if(received.contains(Utils.END_OF_MESSAGE)) {
                        break;
                    } 
                };
                
                // Process the request
                String message = this.processRequest(requestBody, (((InetSocketAddress)clientSocket.getRemoteSocketAddress()).getAddress()).toString(), clientSocket.getPort());

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

            LOGGER.info("Server response is below.\n" + serverResponse.toString());
            
            // Sent response to the client
            try{
                sendResponseToClient(clientSocket, serverResponse);
                clientSocket.close();
            } catch (Exception exp) {
                LOGGER.severe("Error sending response to the client.");
            }
        }

    }


    /*
     * Send a response body to the client
     */
    protected void sendResponseToClient(Socket clientSocket, StringBuffer serverResponse) throws IOException {

        BufferedWriter clientWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        clientWriter.write(serverResponse.toString());
        clientWriter.newLine();
        clientWriter.flush();
        clientWriter.close();
        
    }

}
