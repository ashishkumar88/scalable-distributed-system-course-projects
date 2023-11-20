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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import edu.northeastern.cs6650.project1.client.BaseClient;
import edu.northeastern.cs6650.project1.common.ServerResponseCode;
import edu.northeastern.cs6650.project1.common.Utils;

/*
 * Implementation of the TCP client 
 * 
 * The TCP client sends a request to the TCP server, waits for a response 
 * and processes the response.
 */
public class TCPClient extends BaseClient{

    private static final Logger LOGGER = Logger.getLogger(TCPClient.class.getName());

    private Socket clientSocket;
    private String serverIPAddress;
    private int serverPort;

    public TCPClient(String serverIPAddress, int serverPort) {
        this.serverIPAddress = serverIPAddress;
        this.serverPort = serverPort;
    }
    
    /*
     * Sends a request to the TCP server and process the server response
     */
    public void makeServerRequest(StringBuffer requestBody) {

        try {
            clientSocket = new Socket();
            clientSocket.connect(new InetSocketAddress(this.serverIPAddress, this.serverPort), 5000);

            // Send user input to the server, get the response and show to user
            BufferedWriter serverWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            serverWriter.write(requestBody.toString());
            serverWriter.newLine();
            serverWriter.flush();

            processServerResponse(clientSocket);
        } catch (IOException ioe) {
            LOGGER.severe("Error sending request to the server or timeout happened.");
        }

    }

    /*
     * Process response from the TCP server
     */
    protected void processServerResponse(Socket clientSocket) {

        try {
            StringBuffer responseBody = new StringBuffer();
            InputStream serverSocketInputStream = clientSocket.getInputStream();
            
            // Read the response by reading 256 bytes at a time
            byte[] buffer = new byte[256];
            int readLength;
            while((readLength = serverSocketInputStream.read(buffer)) != -1) {
                String received = new String(buffer, 0, readLength);
                responseBody.append(received);
                
                if(received.contains(Utils.END_OF_MESSAGE)) {
                    break;
                } 
            };

            // process the read response
            this.processServerResponse(responseBody);
        } catch(Exception exp) {
            LOGGER.severe("Error processing server response.");
        }

    }
}
