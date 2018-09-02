package testsNG.Actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import Action.TrafficAction.TrafficAction.ExpectedType;
import Action.TrafficAction.TrafficAction.LoadType;
import EnodeB.EnodeB;
import Entities.LoadParam;
import Entities.StreamParams;
import Netspan.NetspanServer;
import Netspan.Profiles.RadioParameters;
import TestingServices.TestConfig;
import Entities.ITrafficGenerator.Protocol;
import Entities.ITrafficGenerator.TransmitDirection;
import Utils.GeneralUtils;
import Utils.LteThroughputCalculator;
import Utils.Pair;
import Utils.SetupUtils;
import Utils.LteThroughputCalculator.ConfigurationEnum;
import jsystem.framework.report.ListenerstManager;
import jsystem.framework.report.Reporter;
import jsystem.framework.report.ReporterHelper;
import jsystem.sysobj.traffic.TrafficException;
import testsNG.Actions.Traffic.GeneratorType;
import testsNG.Actions.Utils.CalculatorMap;

public class TrafficManager {

	public static Reporter report = ListenerstManager.getInstance();
	private Boolean isTrafficInit = null;;
	private GeneratorType trafficType = null;
	private static ArrayList<TrafficSampler> samplerList; 
	private volatile Traffic trafficInstance;
	
	
	private static TrafficManager instance;
	//private ArrayList<StreamParams> streams = new ArrayList<StreamParams>();
	private Double loadStreamDl;
	private Double loadStreamUl;
	
	private Double dlExpected;
	private Double ulExpected;
	
	public static TrafficManager getInstance(GeneratorType type){
		if(instance == null){
			try{
				instance = new TrafficManager(type);
			}catch(Exception e){
				e.printStackTrace();
				return null;
			}			
		}
		if(instance == null){
			return null;
		}
		return instance;
	}

	private TrafficManager(GeneratorType type) throws Exception{
		if(isTrafficInit == null){
			isTrafficInit = false;
		}
		if(samplerList == null){
			samplerList = new ArrayList<TrafficSampler>();
		}
		if(trafficType == null){
			trafficType = type;
		}
		if(trafficInstance == null){
			if(type == GeneratorType.Default){
				trafficInstance = Traffic.getInstance(SetupUtils.getInstance().getAllUEs());
			}else{
				trafficInstance = Traffic.getInstanceWithSpecificGeneratorType(SetupUtils.getInstance().getAllUEs(), type);
			}			
		}
		if(trafficInstance == null){
			instance = null;
		}
		if(samplerList == null){
			samplerList = new ArrayList<TrafficSampler>();
		}
	}
	
	public boolean checkIfStreamsExist(ArrayList<String> streamsToCheck){
		for(TrafficSampler ts:samplerList){
			if(ts.checkIfStreamsExist(streamsToCheck)){
				return true;
			}
		}
		return false;
	}
	
	public void startTraffic(String name, ArrayList<String> ueList, ArrayList<Character> qci, LoadType loadType, 
			String UlLoad, String DlLoad,
			TransmitDirection direction, ExpectedType type,
			String ulExp, String dlExp, EnodeB enb, Integer numberParallelStreams,
			Double windowSizeInKbits, Integer mss, Integer packetSize, Protocol protocol, Integer timeout,
			ArrayList<String> streams){
		
		if(!isTrafficInit){
			if(!initTrafficWithNoStartTraffic(TrafficCapacity.FULLTPT, protocol)){
				return;
			}
		}
		try {
			trafficInstance.initStreams(protocol, ueList, qci, direction, timeout);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(trafficType == GeneratorType.Iperf){
			if(!enableAditionalStreams(protocol,ueList, qci, direction)){
				return;
			}
			initNumberParallelStreams(numberParallelStreams, new ArrayList<StreamParams>());
			initWindowSize(windowSizeInKbits);
			if(!initFrameSize(protocol, packetSize, mss)){
				return;
			}
			if(Protocol.UDP == protocol){
				setLoadPerStream(enb, loadType, DlLoad,UlLoad);					
			}
			
		}else{
			if(!enableAditionalStreams(protocol,ueList, qci, direction)){
				return;
			}
			if(!getLoadPerStream(enb, loadType, DlLoad,UlLoad)){
				return;
			}
			
			if(loadStreamDl != null){
				loadStreamDl/=ueList.size()*qci.size();
			}
			if(loadStreamUl != null){
				loadStreamUl/=ueList.size()*qci.size();
			}
			
			GeneralUtils.startLevel("Setting load per stream");
			configureLoad(ueList, qci, packetSize);
			GeneralUtils.stopLevel();				
		}
		if(!saveConfigFileAndStart()){
			return;
		}
		trafficInstance.addCommandFilesToReport();
		GeneralUtils.unSafeSleep(5*1000);
		isTrafficInit = true;
		getExpectedValues(enb,type,dlExp,ulExp);
		TrafficSampler current = new TrafficSampler(trafficInstance,name,ueList,qci, direction, type, ulExpected, dlExpected, enb, timeout, streams);
		current.start();
		samplerList.add(current);
	}
	
	private boolean initTrafficWithNoStartTraffic(TrafficCapacity tptCapacity, Protocol protocol) {
		try {
			if(!trafficInstance.initTrafficWithNoStartTraffic(tptCapacity)){
				report.report("Start traffic failed",Reporter.FAIL);
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			report.report("Start traffic failed",Reporter.FAIL);
			return false;
		}
		if(!initProtocol(protocol)){
			report.report("Failed to init protocol", Reporter.FAIL);
			return false;
		}
		trafficInstance.setAllStreamsToState(false);
		return true;
	}
	
	private boolean initProtocol(Protocol protocol) {
		GeneralUtils.startLevel("init Protocol ("+protocol+")");
		boolean action = trafficInstance.initProtocol(protocol);
		GeneralUtils.stopLevel();			
		return action;
	}
	
	private boolean enableAditionalStreams(Protocol protocol,ArrayList<String> ueNameListStc, ArrayList<Character> qci,
			TransmitDirection direction) {
		try {
			GeneralUtils.startLevel("Enabling wanted streams");
			trafficInstance.enableStreamsByNameAndQciAndDirection(protocol,ueNameListStc, qci, direction);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			report.report("Failed to enable streams", Reporter.FAIL);
		}finally{
			GeneralUtils.stopLevel();
		}
		return false;
	}
	
	private void initNumberParallelStreams(Integer numberParallelStreams, ArrayList<StreamParams> arrayList) {
		GeneralUtils.startLevel("init parallel streams");
		trafficInstance.initNumberParallelStreams(numberParallelStreams, null);
		GeneralUtils.stopLevel();
	}
	
	private void initWindowSize(Double windowSizeInKbits) {
		GeneralUtils.startLevel("init window size");
		trafficInstance.initWindowSize(windowSizeInKbits, null);
		GeneralUtils.stopLevel();
	}

	private boolean initFrameSize(Protocol protocol, Integer packetSize, Integer mss) {
		GeneralUtils.startLevel("init Frame Size");
		try {
			if(Protocol.UDP == protocol){
				trafficInstance.initFrameSize(packetSize, null);
			}else{
				trafficInstance.initFrameSize(mss, null);
			}
			return true;
		} catch (TrafficException e) {
			e.printStackTrace();
			report.report("Failed to set protocol", Reporter.FAIL);
		}finally{
			GeneralUtils.stopLevel();			
		}
		return false;
	}
	
	private void getExpectedValues(EnodeB enb, ExpectedType type, String dlExpected, String ulExpected){
		if(ExpectedType.Custom == type){
			if(dlExpected != null){
				this.dlExpected = Double.valueOf(dlExpected);
			}else{
				this.dlExpected = null;
			}
			if(ulExpected != null){
				this.ulExpected = Double.valueOf(ulExpected);
			}else{
				this.ulExpected = null;
			}
		}else{
			Pair<Double,Double> load = getCalculatorPassCriteria(enb,getRadioProfile(enb));
			if(load != null){
				if(dlExpected != null){
					this.dlExpected = (load.getElement0()*Double.valueOf(dlExpected))/100;
				}else{
					this.dlExpected = null;
				}
				if(ulExpected != null){
					this.ulExpected = (load.getElement1()*Double.valueOf(ulExpected))/100;
				}else{
					this.ulExpected = null;
				}
				int cells = EnodeBConfig.getInstance().getNumberOfActiveCells(enb);
				if(EnodeBConfig.getInstance().isCAEnableInNode(enb)){
					if(this.dlExpected != null){
						this.dlExpected = this.dlExpected* cells;
					}
				}else{
					if(this.dlExpected != null){
						this.dlExpected = this.dlExpected* cells;
					}
					if(this.ulExpected != null){
						this.ulExpected = this.ulExpected* cells;
					}					
				}
			}else{
				this.dlExpected = null;
				this.ulExpected = null;
			}
		}
	}
	
	private void setLoadPerStream(EnodeB enb, LoadType loadType, String dlLoad, String ulLoad) {
		GeneralUtils.startLevel("Set load per stream");
		getLoadPerStream(enb, loadType, dlLoad,ulLoad);
		if(loadStreamDl == null){
			loadStreamDl = 0D;
		}
		if(loadStreamUl == null){
			loadStreamUl = 0D;
		}
		trafficInstance.trafficLoadSet(Pair.createPair(loadStreamDl, loadStreamUl));
		GeneralUtils.stopLevel();
	}
	
	private void configureLoad(ArrayList<String> ueList, ArrayList<Character> qci, Integer packetSize){
		
		ArrayList<LoadParam> loadingParametersDl = null;
		ArrayList<LoadParam> loadingParametersUl = null;
		
		for(String str : ueList){
			String name = str;
			int ueNum = Integer.valueOf(name.replaceAll("\\D+", "").trim());
			for(Character qciChar : qci){
				if(loadStreamDl != null){
					if(loadingParametersDl == null){
						loadingParametersDl = new ArrayList<LoadParam>();
					}
					LoadParam lpQciDl = new LoadParam(loadStreamDl, qciChar, ueNum, LoadParam.LoadUnit.MEGABITS_PER_SECOND,packetSize);
					loadingParametersDl.add(lpQciDl);					
				}
				if(loadStreamUl != null){
					if(loadingParametersUl == null){
						loadingParametersUl = new ArrayList<LoadParam>();
					}
					LoadParam lpQciUl = new LoadParam(loadStreamUl, qciChar, ueNum, LoadParam.LoadUnit.MEGABITS_PER_SECOND,packetSize);
					loadingParametersUl.add(lpQciUl);					
				}
			}
		}
		if(loadingParametersDl != null){
			trafficInstance.setLoadStreamDL(loadingParametersDl);				
		}
		if(loadingParametersUl != null){
			trafficInstance.setLoadStreamUL(loadingParametersUl);			
		}
		try{
			if(loadingParametersDl != null){
				trafficInstance.setFrameSizeStreamDL(loadingParametersDl);
			}
			if(loadingParametersUl != null){
				trafficInstance.setFrameSizeStreamUL(loadingParametersUl);			
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		loadStreamDl = null;
		loadStreamUl = null;
	}
	
	private boolean saveConfigFileAndStart() {
		report.report("Save Config File and Start Traffic");
		uploadConfigFileToReport();
		try {
			trafficInstance.saveConfigFileAndStart();
		} catch (Exception e) {
			e.printStackTrace();
			report.report("Failed to start traffic", Reporter.FAIL);
			return false;
		}
		return true;
	}

	private boolean getLoadPerStream(EnodeB dut, LoadType loadType, String dlLoad, String ulLoad) {
		if(LoadType.Custom == loadType){
			if(dlLoad != null){
				loadStreamDl = Double.valueOf(dlLoad);
			}
			if(ulLoad != null){
				loadStreamUl = Double.valueOf(ulLoad);
			}
		}else{
			Pair<Double,Double> load = getCalculatorPassCriteria(dut,getRadioProfile(dut));
			if(load != null){
				if(dlLoad != null){
					loadStreamDl = (load.getElement0()*Double.valueOf(dlLoad))/100;
				}
				if(ulLoad != null){
					loadStreamUl = (load.getElement1()*Double.valueOf(ulLoad))/100;
				}
				int cells = EnodeBConfig.getInstance().getNumberOfActiveCells(dut);
				if(EnodeBConfig.getInstance().isCAEnableInNode(dut)){
					loadStreamDl = loadStreamDl* cells;
				}else{
					loadStreamDl = loadStreamDl* cells;
					loadStreamUl = loadStreamUl* cells;					
				}
			}else{
				loadStreamDl = null;
				loadStreamUl = null;
				return false;
			}
		}
		return true;
	}

	protected RadioParameters getRadioProfile(EnodeB dut) {
		GeneralUtils.startLevel("Radio Parameters Initialize:");
		RadioParameters radioParams = null;
		try {
			radioParams = EnodeBConfig.getInstance().getRadioProfile(dut);
			if (TestConfig.getInstace().getDynamicCFI()){
				radioParams.setCfi("1");
				report.report("Running with Dynamic CFI");
			}
			if(!radioParams.getDuplex().equals("TDD")){
				radioParams.setFrameConfig("1");
				radioParams.setSpecialSubFrame("7");
			}
			int maxUeSupported = NetspanServer.getInstance().getMaxUeSupported(dut);
			report.report("Number of max UE supported according to netspan: "+maxUeSupported);
			GeneralUtils.printToConsole(radioParams.getCalculatorString("PTP"));
		} catch (Exception e) {
			report.report("Error: " + e.getMessage());
		}
		GeneralUtils.stopLevel();
		return radioParams;
	}
	
	private Pair<Double,Double> getCalculatorPassCriteria(EnodeB enb,RadioParameters radioParams) {
		String calculatorStringKey = ParseRadioProfileToString(radioParams);
		if (calculatorStringKey == null) {
			report.report("calculator key value is empty - fail test", Reporter.FAIL);
			return null;
		}
		CalculatorMap calc = new CalculatorMap();

		Pair<Double, Double> trafficValuPair;
		trafficValuPair = getLoadsFromXml(enb,radioParams);
		if(trafficValuPair == null){
			try {
				trafficValuPair = calc.getDLandULconfiguration(radioParams);
			} catch (Exception e) {
				trafficValuPair = null;
				e.printStackTrace();
			}			
		}
		return trafficValuPair;
	}
	
	private Pair<Double,Double> getLoadsFromXml(EnodeB enb,RadioParameters radio){
		String dl_ul = null;
		int maxUeSupported = 0;
		try {
			maxUeSupported = NetspanServer.getInstance().getMaxUeSupported(enb);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(maxUeSupported > 0){
			dl_ul = LteThroughputCalculator.getInstance().getPassCriteriaFromStaticLteThroughputCalculator(radio, ConfigurationEnum.USER, maxUeSupported, "PTP");
		}
		if(dl_ul != null){
			String[] toReturn = dl_ul.split("_");
			Double dl = Double.valueOf(toReturn[0])*1.1;
			Double ul = Double.valueOf(toReturn[1])*1.1;
			Pair<Double,Double> response = Pair.createPair(doubleTo2DigitsAfterPoint(dl), doubleTo2DigitsAfterPoint(ul));
			return response;
		}
		return null;
	}
	
	private Double doubleTo2DigitsAfterPoint(Double doub){
		String toRet = String.format("%.2f",doub);
		return Double.valueOf(toRet);
	}
	
	protected String ParseRadioProfileToString(RadioParameters radioParams2) {
		// debug print
		GeneralUtils.printToConsole("TDD : FC_Dup_BW_CFI_Ssf\n" + "FDD: Dup_BW");
		String res = radioParams2.getCalculatorString("PTP");
		GeneralUtils.printToConsole("returning String line: " + res);
		return res;
	}
	
	protected void uploadConfigFileToReport() {
		// insert method to make tcc file or XML
		File uploadToReportConfigFile = trafficInstance.getUploadedConfigFile();
		try {
			ReporterHelper.copyFileToReporterAndAddLink(report, uploadToReportConfigFile, "ConfigFile");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean checkGeneratorType(GeneratorType generatorType) {
		if(!trafficType.equals(generatorType)){
			report.report("Can not start traffic with different generator type from before", Reporter.FAIL);
			return false;
		}
		return true;
	}

	public boolean checkIfNameExist(String semanticName) {
		for(TrafficSampler ts : samplerList){
			if(ts.getName().equals(semanticName)){
				return true;
			}
		}
		return false;
	}

	public void stopTraffic(ArrayList<String> trafficToStop) {
		if(trafficToStop.isEmpty()){
			Iterator<TrafficSampler> iter = samplerList.iterator();
			while(iter.hasNext()){
				TrafficSampler ts = iter.next();
				ts.getStatistics();
				ts.stopTraffic();
				iter.remove();
			}		
		}
		for(String nameToStop : trafficToStop){
			Iterator<TrafficSampler> iter = samplerList.iterator();
			while(iter.hasNext()){
				TrafficSampler ts = iter.next();
				if(ts.getName().equals(nameToStop)){
					ts.getStatistics();
					ts.stopTraffic();
					iter.remove();
				}
			}			
		}
	}
}
