package UeSimulator.Amarisoft;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import EnodeB.EnodeB;
import UeSimulator.Amarisoft.JsonObjects.Actions.UEAction;
import UeSimulator.Amarisoft.JsonObjects.Actions.UEAction.Actions;
import UeSimulator.Amarisoft.JsonObjects.ConfigFile.*;
import UeSimulator.Amarisoft.JsonObjects.Status.UeStatus;
import jsystem.framework.system.SystemManagerImpl;
import jsystem.framework.system.SystemObjectImpl;
import systemobject.terminal.SSH;
import systemobject.terminal.Terminal;

/**
 * ChatServer Client
 *
 * @author Jiji_Sasidharan
 */
@ClientEndpoint
public class AmariSoftServer extends SystemObjectImpl{

	private Terminal sshTerminal;
	private Terminal lteUeTerminal;
	private String ip;
	private String port;
	private String userName;
	private String password;
	private double txgain;
	private double rxgain;
	private String ueConfigFileName = "automationConfigFile";
	public ConfigObject configObject;
    private Session userSession = null;
    private MessageHandler messageHandler;
    private String[] sdrList;
    private String[] imsiStartList;
    private String[] imsiStopList;

    @Override
	public void init() throws Exception {
		super.init();
		port = 900 + sdrList[0];
	}
	
	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}
	
	public String getusername() {
		return userName;
	}

	public void setuserName(String userName) {
		this.userName = userName;
	}
	
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	public double getTxgain() {
		return txgain;
	}

	public void setTxgain(String txgain) {
		this.txgain = Double.valueOf(txgain);
	}

	public double getRxgain() {
		return rxgain;
	}

	public void setRxgain(String rxgain) {
		this.rxgain = Double.valueOf(rxgain);
	}
	
	public String[] getImsiStartList() {
		return sdrList;
	}

	public void setImsiStartList(String imsiStartList) {
		this.imsiStartList = imsiStartList.split(",");
	}
	
	public String[] getImsiStopList() {
		return imsiStopList;
	}

	public void setImsiStopList(String imsiStopList) {
		this.imsiStopList = imsiStopList.split(",");
	} 
    
	public static AmariSoftServer getInstance() throws Exception {
		AmariSoftServer ns = (AmariSoftServer) SystemManagerImpl.getInstance().getSystemObject("AmariSoftServer");
		return ns;
	}
	
    private AmariSoftServer() { 
    	connect();
    }

    public boolean startServer(ArrayList<EnodeB> duts){
    	setConfig(duts);
    	return startServer("automationConfiguration");
    }
    
    public boolean startServer(String configFile){
    	try {   
    		boolean ans = sendCommands(lteUeTerminal, "/root/ue/lteue /root/ue/config/" + ueConfigFileName,"(ue)");
    		if (!ans) {
    			System.out.println("Failed starting server with config file: " + ueConfigFileName);
    			return false;
			}
        	URI endpointURI = new URI("ws://"+ip+":"+port);
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, endpointURI);
            System.out.println(container.getDefaultAsyncSendTimeout());
            System.out.println(container.getDefaultMaxBinaryMessageBufferSize());
            System.out.println(container.getDefaultMaxSessionIdleTimeout());
            System.out.println(container.getDefaultMaxTextMessageBufferSize());
            startMessageHandler();
        } catch (Exception e) {
            System.out.println("Failed starting server with config file: " + ueConfigFileName);
            System.out.println(e.getMessage());
            return false;
        }
    	return true;
    }

	private void startMessageHandler() {
		addMessageHandler(new AmariSoftServer.MessageHandler() {
			public void handleMessage(String message) {
				System.out.println("Message recieved: " + message);
				ObjectMapper mapper = new ObjectMapper();

				// Convert JSON string to Object
				UeStatus stat = null;
				try {
					stat = mapper.readValue(message, UeStatus.class);
					Double ulRate = stat.getUeList().get(0).getUlBitrate() / 1000;
					Double dlRate = stat.getUeList().get(0).getDlBitrate() / 1000;
					String emmState = stat.getUeList().get(0).getEmmState();
					int ueID = stat.getUeList().get(0).getUeId();
					System.out.println("UE ID: " + ueID + "\tulRate (kbit): " + ulRate.intValue() + "\tdlRate (kbit): "
							+ dlRate.intValue() + "\temmState:" + emmState);

				} catch (JsonParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JsonMappingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});
	}

	public void closeSocket() {
		
		
	}

	public boolean sendCommands(Terminal terminal, String cmd, String response) {
		String privateBuffer = "";
		String ans = "";
		sendRawCommand(terminal, cmd);
		long startTime = System.currentTimeMillis(); // fetch starting time
		while ((System.currentTimeMillis() - startTime) < 3000) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				privateBuffer += terminal.readInputBuffer();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			ans += privateBuffer;
			if (ans.contains(response))
				return true;			
		}

		return false;
	}
	
	public void startTraffic(int rateKB, int port){		
		for (int i = 1; i <= configObject.getUeList().size(); i++) {
			sendCommands(sshTerminal, "ip netns exec ue"+i+" iperf -c 91.99."+i+".240 -i 5 -p 500"+port+" -t 99999 &","");
		}		
		//ip netns exec ue1 iperf -c 91.99.1.240 -u -b 3m  -i 5 -p 5009 -t 99999 
	}
		
	public void sendRawCommand(Terminal terminal, String command){
		if (terminal == null) {
			return;
		}
		try {
			terminal.sendString(command + "\n", false);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
    
    /**
     * Callback hook for Connection open events.
     *
     * @param userSession the userSession which is opened.
     */
    @OnOpen
    public void onOpen(Session userSession) {
        System.out.println("opening websocket");
        this.userSession = userSession;
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param userSession the userSession which is getting closed.
     * @param reason the reason for connection close
     * @throws IOException 
     */
    @OnClose
    public void onClose(Session userSession, CloseReason reason) throws IOException {
        System.out.println("closing websocket");
        this.userSession.close();
        this.userSession = null;
    }

    /**
     * Callback hook for Message Events. This method will be invoked when a client send a message.
     *
     * @param message The text message
     */
    @OnMessage
    public void onMessage(String message) {
        if (this.messageHandler != null) {
            this.messageHandler.handleMessage(message);
        }
    }

    /**
     * register message handler
     *
     * @param msgHandler
     */
    public void addMessageHandler(MessageHandler msgHandler) {
        this.messageHandler = msgHandler;
    }

    /**
     * Send a message.
     *
     * @param message
     */
    public void sendMessage(String message) {
        this.userSession.getAsyncRemote().sendText(message);
    }

    /**
     * Message handler.
     *
     * @author Jiji_Sasidharan
     */
    public static interface MessageHandler {

        public void handleMessage(String message);
    }

    private void connect() { 
		this.sshTerminal = new SSH(ip, userName, password);
		this.lteUeTerminal = new SSH(ip, userName, password);
		try {
			this.sshTerminal.connect();
			this.lteUeTerminal.connect();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setConfigFile(String fileName) {
		ueConfigFileName = fileName;
	}
	
	public void writeConfigFile() {
		ObjectMapper mapper = new ObjectMapper();
    	mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
    	try {    		
			String stat = mapper.writeValueAsString(configObject);
			String newStat = stat.replace("\"", "\\\"");
			sendCommands(sshTerminal ,"echo \"" + newStat + "\" > /root/ue/config/" + ueConfigFileName,"");
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		
	}

	public void startIperfServer(int ueId, int port) {
		sendCommands(sshTerminal, "ip netns exec ue"+ueId+" iperf -s -i 1 -p 500"+port+" -f k &","");		
	}
	
	public void AddUe(String imsi, int release, int category, int ueId) throws JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ArrayList<UeList> ueLists = new ArrayList<UeList>();
		UeList ueList = new UeList();
		ueList.setAsRelease(release);
		ueList.setUeCategory(category);
		ueList.setForcedCqi(15);
		ueList.setForcedRi(2);
		ueList.setSimAlgo("milenage");
		ueList.setImsi(imsi);
		ueList.setImeisv("1234567891234567");
		ueList.setK("5C95978B5E89488CB7DB44381E237809");
		ueList.setOp("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
		ueList.setTunSetupScript("ue-ifup_TCP");
		ueList.setUeId(ueId);
		//ueList.setAdditionalProperty("ue_count", 5);
		ueLists.add(ueList);
		UEAction addUE = new UEAction();
		addUE.setMessage(Actions.UE_ADD);
		addUE.setUeList(ueLists);
		String message = mapper.writeValueAsString(addUE);
		System.out.println("Sending message: " + message);
		sendMessage(message);
	}
	
	public boolean uePowerOn(int ueId)
	{
		ObjectMapper mapper = new ObjectMapper();
		UEAction getUE = new UEAction();
		getUE.setUeId(ueId);
		getUE.setMessage(Actions.POWER_ON);
		try {
			sendMessage(mapper.writeValueAsString(getUE));
		} catch (JsonProcessingException e) {
			System.out.println("Failed uePowerOn to ue " + ueId);
			System.out.println(e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean uePowerOff(int ueId)
	{
		ObjectMapper mapper = new ObjectMapper();
		UEAction getUE = new UEAction();
		getUE.setUeId(ueId);
		getUE.setMessage(Actions.POWER_OFF);
		try {
			sendMessage(mapper.writeValueAsString(getUE));
		} catch (JsonProcessingException e) {
			System.out.println("Failed uePowerOff to ue " + ueId);
			System.out.println(e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public void setConfig(ArrayList<EnodeB> duts) {
		configObject.setLogOptions("all.level=none,all.max_size=0");
		configObject.setLogFilename("/tmp/ue0.log");
		configObject.setComAddr("0.0.0.0:"+port);
		ArrayList<Cell> cells = new ArrayList<Cell>();
		String rfDriver = "";
		for (int i = 0; i < sdrList.length; i++) {
			Cell cell = new Cell();
			int earfcn;
			if (duts.size() > i) 
				earfcn = duts.get(i).getEarfcn();			
			else
				earfcn = duts.get(duts.size()-1).getEarfcn();
			cell.setDlEarfcn(earfcn);
			cell.setNAntennaDl(2);
			cell.setNAntennaUl(1);
			cell.setGlobalTimingAdvance(2);
			cells.add(cell);
			rfDriver += "dev"+i+"=/dev/sdr"+sdrList[i]+",";
		}
		int ind = rfDriver.lastIndexOf(",");
		if( ind>=0 )
			rfDriver = new StringBuilder(rfDriver).replace(ind, ind+1,"").toString();
		System.out.println("rfDriver String: " + rfDriver);
		configObject.setBandwidth(duts.get(0).getBandwidth().getBw());
		configObject.setCells(cells);
		configObject.setTxGain(txgain);
		configObject.setRxGain(rxgain);
		configObject.setMultiUe(true);
		configObject.getRfDriver().setName("sdr");
		configObject.getRfDriver().setArgs(rfDriver);
		ArrayList<UeList> ueLists = new ArrayList<UeList>();
		UeList ueList = new UeList();
		ueList.setAsRelease(13);
		ueList.setUeCategory(4);
		ueList.setForcedCqi(15);
		ueList.setForcedRi(2);
		ueList.setHalfDuplex(false);
		ueList.setSimAlgo("milenage");
		ueList.setImsi(imsiStartList[0]);
		ueList.setK("5C95978B5E89488CB7DB44381E237809");
		ueList.setOp("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
		ueList.setTunSetupScript("ue-ifup");
		ueLists.add(ueList);
		configObject.setUeList(ueLists);
		setConfigFile("automationConfiguration");
		writeConfigFile();
	}
}