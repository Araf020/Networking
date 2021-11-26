##File Sharing on LAN

This project aims to provide a simple way to share files on a `LAN`.

A client can request for file s from the server, and the server will broadcast the message to all.

Then, other client can upload the file to the server, and the server will broadcast the message to the client who requested.

Then the client can download the file with the proper fileId.

###Installation
    Clone this repository...
    
    Run the Server class first and set the `buffer` and `chunk` size
    
    Then you can edit the server address of client class with the ip of server machine.
    
    Then run the Client class and type any of below command 

###
Command:
======================
View messages`(Command: V)`

Look up clientList`(Command: L C)` 

Look up public files`(Command: L PF)`

Look up your file (`Command: L MF`)

Request for a file(`command: R enter_the_description`)

Upload a file(`Command: U filename filePath visibility`)

Upload against a request(`Command: UR filename filePath requestID`)

Download a file(`Command: D fileID`)

LOGOUT (`command: logout`)

HELP (`command: help`)