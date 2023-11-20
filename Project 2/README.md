### Project 2

This project contains the implementation of a RMI server and RMI client that perform Key-Value store operation using RMI.

#### Compiling the project

Compilation of all the classes written in this project is required before running the application. To compile all the classes in this project, please run the following commands.
```bash
cd project-app
rm -rf classes
javac -d classes @sources.txt
```

#### Running the project

After the compilation step is performed, the server and the client applications can be started in any order. However, if the client application is started before the server application, the RMI call will fail. 

To start the server, run the following command. The third and the fourth arguments are optional. 

Required command line arguments:

1. IP address of the server. It could be 127.0.0.1 only when the server and client applications are running on the same computer.
2. Port number on the server that will be used by the RMI registry for socket connections. 

Optional command line arguments:

3. true/false - The third argument when set to false will not prepopulate the server with data. Default behavior is that the client prepopulates the server with data.
4. true/false - The fourth argument when set to false will not run the required 5 operations. Default behavior is that the client run the default 5 operations.

```bash
cd project-app
java -classpath classes edu.northeastern.cs6650.project2.server.Server <server ip address> <server port> <true/false> <true/false>
```

```bash
cd project-app
java -classpath classes edu.northeastern.cs6650.project2.client.Client <server ip address> <server port> 
```
