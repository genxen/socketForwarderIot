/**
 * ForwardServerClientThread handles the clients of Socket Forward Server. It
 * finds suitable server from the server pool, connects to it and starts
 * the TCP forwarding between given client and its assigned server. After
 * the forwarding is failed and the two threads are stopped, closes the sockets.
 */
 
import java.net.Socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
 
public class ForwardServerClientThread extends Thread
{
    private Socket mClientSocket = null;
    private Socket mremoteClientSocket = null;
    private String mClientHostPort;
    private String mRemoteClientHostPort;
    public boolean mBothConnectionsAreAlive = false;
 
    /**
     * Creates a client for handling socket
     */
    public ForwardServerClientThread(Socket aClientSocket, Socket aremoteClientSocket)
    {
        this.mClientSocket = aClientSocket;
    	this.mremoteClientSocket = aremoteClientSocket;
    }
 
    /**
     * Obtains a destination server socket to some of the servers in the list.
     * Starts two threads for forwarding : "client in <--> dest server out" and
     * "dest server in <--> client out", waits until one of these threads stop
     * due to read/write failure or connection closure. Closes opened connections.
     */
    public void run()
    {
        try {
           mClientHostPort = mClientSocket.getInetAddress().getHostAddress() + ":" + mClientSocket.getPort();
           mRemoteClientHostPort = mremoteClientSocket.getInetAddress().getHostAddress() + ":" + mremoteClientSocket.getPort();
           
           // Obtain input and output streams of server and client
           InputStream clientIn = this.mClientSocket.getInputStream();
           OutputStream clientOut = this.mClientSocket.getOutputStream();
           InputStream remoteClientIn = this.mremoteClientSocket.getInputStream();
           OutputStream remoteClientOut = this.mremoteClientSocket.getOutputStream();
           
           SocketForwarderServer.log("TCP Forwarding  " + mClientHostPort + " <--> " + mRemoteClientHostPort + "  started.");
 
           // Start forwarding of socket data between server and client
           ForwardThread clientForward = new ForwardThread(this, clientIn, remoteClientOut);
           ForwardThread remoteClient = new ForwardThread(this, remoteClientIn, clientOut);
           mBothConnectionsAreAlive = true;
           clientForward.start();
           remoteClient.start();
 
        } catch (IOException ioe) {
           ioe.printStackTrace();
        }
    }
 
    /**
     * connectionBroken() method is called by forwarding child threads to notify
     * this thread (their parent thread) that one of the connections (server or client)
     * is broken (a read/write failure occured). This method disconnects both server
     * and client sockets causing both threads to stop forwarding.
     */
    public synchronized void connectionBroken()
    {
        if (mBothConnectionsAreAlive) {
           // One of the connections is broken. Close the other connection and stop forwarding
           // Closing these socket connections will close their input/output streams
           // and that way will stop the threads that read from these streams
           try { mremoteClientSocket.close(); } catch (IOException e) {}
           try { mClientSocket.close(); } catch (IOException e) {}
 
           SocketForwarderServer.log("TCP Forwarding  " + mClientHostPort + " <--> " + mRemoteClientHostPort + "  stopped.");
           SocketForwarderServer.numberOfConnectedClients--;
        }
    }
 
   
    }
 