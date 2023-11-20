### Project 1

This project contains the implementation of Single Server, Key-Value Store (TCP and UDP).

#### Compiling the project

Compilation of all the classes written in this project is required before running the application. To compile all the classes in this project, please run the following commands.
```bash
cd project-app
rm -rf classes
javac -d classes @sources.txt
```

#### Running the project

After the compilation step is performed, the server and the client applications can be started in any order. However, if the client application is started before the server application, there will be timeouts on the client application while performing a request.
```bash
cd project-app
java -classpath classes edu.northeastern.cs6650.project1.server.Server <server port> <server type>
```

```bash
cd project-app
java -classpath classes edu.northeastern.cs6650.project1.client.Client <server ip address> <server port> <server type> 
```
