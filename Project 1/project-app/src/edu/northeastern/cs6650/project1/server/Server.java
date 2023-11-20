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
import java.net.SocketException;
import java.util.logging.Logger;

import edu.northeastern.cs6650.project1.common.ServerType;
import edu.northeastern.cs6650.project1.server.BaseServer;
import edu.northeastern.cs6650.project1.server.TCPServer;
import edu.northeastern.cs6650.project1.server.UDPServer;

/*
 * A factory that creates a TCP or a UDP server object
 */
class ServerFactory {
    public static BaseServer createServer(int serverPort, ServerType serverType) throws IOException, SocketException {

        if(serverType == ServerType.TCP) {
            return new TCPServer(serverPort);
        } else if(serverType == ServerType.UDP) {
            return new UDPServer(serverPort);
        }
        return null;
        
    }
}


/*
 * The main server application class
 * 
 * Based on the arguments provided, either a TCP or a UDP server is created and 
 * started. The servers listen for one request at a time from the client application
 * and responds to a request. The server application is terminated on the press of
 * Control-C.
 */
public class Server {
    public static void main(String[] arguments) {  

        if(arguments.length != 2) {
            System.err.println("Incorrect number of arguments. Correct usage: java -classpath classes edu.northeastern.cs6650.project1.server.Server <port number> <server type>.");
        } else {
            try {
                int serverPort = Integer.parseInt(arguments[0]);
                ServerType serverType = ServerType.valueOf(arguments[1].toUpperCase());
                BaseServer server = ServerFactory.createServer(serverPort, serverType);
                server.spin();
            } catch (NumberFormatException nfe) {
                System.err.println("The port number should be an integer.");
            } catch(IllegalArgumentException iae) {
                System.err.println("The server type should be either tcp or udp.");
            } catch(Exception exp) {
                System.err.println("Problem encountered while starting the server.");
            }
        }
        
    }
}
