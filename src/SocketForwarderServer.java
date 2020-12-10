
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SocketForwarderServer {
	
	private static boolean ENABLE_LOGGING = true;
    public static final String SETTINGS_FILE_NAME = "SocketForwardServer.properties";
    public static int numberOfConnectedClients = 0; //number of wirex connected

    
    private int serverListeningTcpPort = 0; //Server listening port
    private int remoteTcpPort = 0;
    private int remoteTimeOut = 0;
    private String ipAddr_buff;
    
    private ServerSocket serverSocket = null; //server listening socket 
    private Socket InWirexSocket = null; // incoming wirex connection
    private Socket OutWirexSocket = null; // remote wirex connection
        
    /**
     * Prints given log message on the standart output if logging is enabled,
     * otherwise ignores it
     */
    public static void log(String aMessage)
    {
        if (ENABLE_LOGGING)
           System.out.println(aMessage);
    }
    
    /**
     * check IP format pattern
     */
    public static boolean isValidIP(String ipAddr){
        
        Pattern ptn = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
        Matcher mtch = ptn.matcher(ipAddr);
        return mtch.find();
    }
    
    
    /**
     * Reads the configuration file "SocketForwardServer.properties"
     * and load user preferences. This method is called once during the server startup.
     */
    public void readSettings() throws Exception
    {
        // Read properties file in a Property object
        Properties props = new Properties();        
        props.load(new FileInputStream(SETTINGS_FILE_NAME));
        
        // Read and parse the serverListeningTcpPort
        serverListeningTcpPort = Integer.parseInt(props.getProperty("ListeningServerPort"));      
        if (serverListeningTcpPort == 0)
            log("The ListeningServerPort can not be empty.");
        // Read and parse the remoteTcpPort
        remoteTcpPort = Integer.parseInt(props.getProperty("remoteTcpPort"));
        if (remoteTcpPort == 0)
        	log("The remoteTcpPort can not be empty.");
     // Read and parse the remoteTimeOut
        remoteTimeOut = Integer.parseInt(props.getProperty("remoteTimeOut"));
        if (remoteTimeOut == 0)
        	log("The remoteTimeOut can not be empty."); 
    }
    
    
   	/**
     * Starts the forward server - binds on a given port and starts serving
   	 * @throws IOException 
     */
    public void startForwardServer() throws Exception
    {
        // Bind server on given TCP port
    	try {
    		serverSocket = new ServerSocket(serverListeningTcpPort);
        }catch (BindException e) {
     	   log("Server connection failed on localhost and port: " + serverListeningTcpPort);
        }             
        log("Socket Forward Server started on TCP port " + serverListeningTcpPort);
        
        // Accept client connections and process them until stopped
        while(true) {
        	   InWirexSocket = serverSocket.accept();
               String clientHostPort = InWirexSocket.getInetAddress().getHostAddress() + ":" + InWirexSocket.getPort();
               log("Accepted client from " + clientHostPort);  
               
               log("waiting ip address");         
               //Buffer read for a line
               BufferedReader readfromsoc = new BufferedReader(new InputStreamReader(InWirexSocket.getInputStream()));                             
               ipAddr_buff = readfromsoc.readLine();
               
               if (isValidIP(ipAddr_buff) == true) {                                                          
	               log("IP address sent = " + ipAddr_buff); 

	               OutWirexSocket = new Socket();
	               try {
	            	   OutWirexSocket.connect(new InetSocketAddress(ipAddr_buff, remoteTcpPort), remoteTimeOut);
	               }
	               catch (SocketTimeoutException e) {
	            	   log("connection failed with ip: " + ipAddr_buff + " and port: " + remoteTcpPort);
	            	   OutWirexSocket = null; InWirexSocket.close();
	            	   continue;
	               }
	               
		           ForwardServerClientThread forwardThread = new ForwardServerClientThread(InWirexSocket, OutWirexSocket);
		           forwardThread.start();
		                
		           numberOfConnectedClients = numberOfConnectedClients +1;
		           log("Number of clients connected " + numberOfConnectedClients);
		               
               } else {
            	   log("the IP address sent = " + ipAddr_buff + " is not correct"); 
            	   InWirexSocket.close();
            	   continue;
               }          
        }
    }        


	public static void main(String[] args) {
		SocketForwarderServer srv = new SocketForwarderServer();
        try {
        srv.readSettings();
        srv.startForwardServer();
        } catch (Exception e) {
           e.printStackTrace();
        }

	}

}
