import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import org.cloudbus.cloudsim.asmaUtil.Constants;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;

public class EntityCreator {
	
    private final static String COMMA_DELIMITER = ",";
    private final Random RAND = new Random();
	private List<Cloudlet> cloudletList;

	
    List<Vm> readVms(int vmCount, int submissionDelay1) {

    	final List<Vm> list = new ArrayList<>(20);

		String fileName = Constants.VM_DATASET;
		try {
		//parsing a CSV file into Scanner class constructor  
		//Scanner sc = new Scanner(new File("C:\\Users\\13635281\\OneDrive - UTS\\Documents\\implementation\\dataset_17_4_21\\vm.csv"));  
		//sc.useDelimiter(",");   //sets the delimiter pattern  

		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String nextLine;
		while((nextLine = br.readLine()) != null) {
			String[] values = nextLine.split(COMMA_DELIMITER);
			double mips = parseDouble(values[0]);
			int pes = parseInt(values[1]);
			int ram = parseInt(values[2]);
			int bw = parseInt(values[3]);
			int size = parseInt(values[4]);
			int priority = RAND.nextInt(2);
			int submissionDelay = parseInt(values[5]);
			
			final Vm vm = new VmSimple(mips, pes, priority, 'a');
			vm.setRam(ram).setBw(bw).setSize(size)
			.setSubmissionDelay(submissionDelay);
			list.add(vm);
			
			//if(list.size()==5) break;

		}

		//sc.close();
		
		}catch(Exception e) {e.printStackTrace();}
		
		return list;
    	
//    	final List<Vm> list = new ArrayList<>(20);
//		for (int i = 0; i < 20; i++) {
//			//Uses a CloudletSchedulerTimeShared by default to schedule Cloudlets
//
//			// Obtain a number between [0 - 4].
//			int mips = RAND.nextInt(4);
//
//			int priority = RAND.nextInt(2);
//			final Vm vm = new VmSimple(1000, 4, priority, 'a');
//			vm.setRam(512).setBw(1000).setSize(10000);
//			list.add(vm);
//		}
//
//		return list;

	}
	
	void readHosts() {
		

	}
	
	List<Cloudlet> readCloudlets(int cloudletCount) {

    	cloudletList = new ArrayList<>(cloudletCount);

		String fileName = Constants.CLOUDLET_DATASET;
		try {  

		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String nextLine; 
		while((nextLine = br.readLine()) != null && cloudletList.size() < cloudletCount) {
			String[] values = nextLine.split(COMMA_DELIMITER);
			long mips = (long) parseDouble(values[0]);
			int size = (int) parseDouble(values[1]);
			int pes = parseInt(values[2]);
			
			 //UtilizationModel defining the Cloudlets use only 50% of any resource all the time
	        // this sets RAM, bandwidth
	        final UtilizationModelDynamic utilizationModel = new UtilizationModelDynamic(1);

			final Cloudlet cloudlet = new CloudletSimple(mips, pes, utilizationModel);
            cloudlet.setSizes(size);

			//.setSubmissionDelay(submissionDelay);
            cloudletList.add(cloudlet);			

		}
		
		}catch(Exception e) {e.printStackTrace();}
		
		return cloudletList;
	}
	
	public List<Vm> readVmAndCloudlet(int count){
		System.out.println("readVmandCloudlet is called");
		final List<Vm> list = new ArrayList<>(count);
		cloudletList = new ArrayList<>(count);
		
		String fileName = Constants.VM_CLOUDLET_DATASET;
		try {  

		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String nextLine = br.readLine();
		while((nextLine = br.readLine()) != null) {
			String[] values = nextLine.split(COMMA_DELIMITER);
			
			//read vm values
			double mips = parseDouble(values[0]);
			int pes = parseInt(values[1]);
			int ram = parseInt(values[2]);
			int bw = parseInt(values[3]);
			int size = parseInt(values[4]);
			//int priority = RAND.nextInt(2); // assign random priority
			int submissionDelay = parseInt(values[5]);
			int priority = parseInt(values[9]);
			
			// read cloudlet values
			long clMips = (long) parseDouble(values[6]);
			int fileSize = (int) parseDouble(values[7]);
			int clPes = parseInt(values[8]);
			
			final Vm vm = new VmSimple(mips, pes, priority, 'a');
			vm.setRam(ram).setBw(bw).setSize(size)
			.setSubmissionDelay(submissionDelay);
			list.add(vm);

			// now add the cloudlet to the vm
			cloudletList.add(createCloudlet(vm, clMips, fileSize, clPes));
		}

		
		}catch(Exception e) {e.printStackTrace();}
		
		return list;
	}
	
	private Cloudlet createCloudlet(Vm vm, long mips, int size, int pes) {
        UtilizationModel utilizationModel = new UtilizationModelFull();
        Cloudlet cloudlet = new CloudletSimple(mips, pes, utilizationModel);
        cloudlet.setVm(vm);
        
        return cloudlet;
	}
	
	public List<Cloudlet> getCloudlet(){
		return cloudletList;
	}
	
	private double parseDouble(String s) {
		return  Double.parseDouble(s);
	}
	
	private int parseInt(String s) {
		return  Integer.parseInt(s);
	}

}
