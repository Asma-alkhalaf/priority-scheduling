import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyPriority;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyRoundRobin;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyAbstract;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.asmaUtil.Constants;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerAbstract;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerScalable;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudbus.cloudsim.vms.VmStateHistoryEntry;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.builders.tables.CsvTable;
import org.cloudsimplus.builders.tables.VmsTableBuilder;
import org.cloudsimplus.listeners.EventInfo;
import org.cloudsimplus.util.Log;

// Excel File imports
  


public class ObjectiveTwo {
	private static int HOSTS;
	private static final int HOST_PES =8;

//	private static final int VMS = 20; //original 2
//	private static final int VM_PES = 2; // original 4

//	private static final int CLOUDLETS = 6; 
//	private static final int CLOUDLET_PES = 2;
//	private static final int CLOUDLET_LENGTH = 10000;
    private static final double TIME_TO_TERMINATE_SIMULATION = 403181; //4377974; //4377974 ; //4379000;
    private static final double MIN_TIME_BETWEEN_EVENT = 0.01;
    private static int currentVmIndex = 0;
    private static boolean secondAttempt = false;
    
	private final CloudSim simulation;
//	private final VmAllocationPolicyPriority vmAllocation = new VmAllocationPolicyPriority();
//	private final VmAllocationPolicySimple vmAllocation = new VmAllocationPolicySimple();

	private DatacenterBroker broker0;
	private List<Vm> vmList;
	private List<Vm> failedVmList = new ArrayList<>();
	private List<Cloudlet> cloudletList;
	private Datacenter datacenter0;
	private Datacenter datacenter1;

	
	private double [][] vmFactory;
	private final Random RAND = new Random();
	private double time = 0.0;
	private boolean firstTime = true;

	/* arguments set by the user */
	private String dataset;
	private VmAllocationPolicy vmAllocation;
	private String algorithm;

	public static void main(String[] args) {
//		if (args.length < 2) {
//			System.out.println("ERROR:no arguments, exiting...");
//			System.exit(0);
//		}else 
//			new LargeDatasetExample(args[0],args[1], args[2]);
//		
		String dataset= "";
		String alg = "";
		String hosts = "";
		Scanner sc= new Scanner(System.in);
		// Option#1: use default data
		// Option#2: get dataset, algorithm, and number of hosts from the user
		System.out.println("1.Use default data or 2. Enter your data");
		int option= sc.nextInt();
		sc.nextLine();
		
		if(option == 1) {
			dataset = Constants.simple;
			alg = "fcfs";
			hosts = "1";
			 System.out.printf("The default settings are: %s for alg and %s for number of hosts\n\n", alg, hosts);
		}else if (option == 2) {
			System.out.println("Enter datasetPath, Algorithm, then hosts (no)");
			 dataset= sc.nextLine();
			 alg = sc.nextLine();
			 hosts = sc.next();
		}else {
			System.out.print("ERROR, No data entered");
			System.exit(0);
		}
		new ObjectiveTwo(dataset, alg,hosts);
	}

	private ObjectiveTwo(String datasetPath, String schedulingAlg, String hosts) {
		/*Enables just some level of log messages.
	          Make sure to import org.cloudsimplus.util.Log;*/
		Log.setLevel(ch.qos.logback.classic.Level.WARN);
		
		
		/* set the dataset and allocation policy */
		this.dataset = datasetPath;
		switch (schedulingAlg) {
		case "p": vmAllocation = new VmAllocationPolicyPriority(); algorithm = Constants.PRIORITY; break;
		case "fcfs": vmAllocation = new VmAllocationPolicySimple(); algorithm = Constants.FCFS; break;
		case "rr": vmAllocation = new VmAllocationPolicyRoundRobin(); algorithm = Constants.RR; break;
		default:
		}

		this.HOSTS = Integer.parseInt(hosts);
		
		simulation = new CloudSim(MIN_TIME_BETWEEN_EVENT);
        simulation.terminateAt(TIME_TO_TERMINATE_SIMULATION);
		datacenter0 = createDatacenter();
		datacenter1 = createDatacenter();


		//Creates a broker that is a software acting on behalf a cloud customer to manage his/her VMs and Cloudlets
		broker0 = new DatacenterBrokerScalable(simulation);
        broker0.setVmDestructionDelayFunction(vm -> 10.0);
        


		createVmAndCloudlet();

		simulation.start();
				// all statements after this line will not be executed until the simultion ends (or aborted)
		System.out.println("\ncalling the prinintg methods..."+time);
		postTerminationJobs();
		System.out.println("\nConstoctor - calling the prinintg methods is done");
	}

	/**
	 * Creates a Datacenter and its Hosts.
	 */
	private Datacenter createDatacenter() {
		final List<Host> hostList = new ArrayList<>(HOSTS);
		for(int i = 0; i < HOSTS; i++) {
			Host host = createHost();
			host.enableStateHistory();
			hostList.add(host);
		}
		VmAllocationPolicy datacenterVmAllocation = null;
		switch (algorithm) {
		case Constants.PRIORITY: datacenterVmAllocation = new VmAllocationPolicyPriority(); break;
		case Constants.FCFS: datacenterVmAllocation = new VmAllocationPolicySimple(); break;
		case Constants.RR: datacenterVmAllocation = new VmAllocationPolicyRoundRobin(); break;
		}

		//Uses a VmAllocationPolicyPriority to allocate VMs
		//return new DatacenterSimple(simulation, hostList);
		final Datacenter dc = new DatacenterSimple(simulation, hostList, datacenterVmAllocation);
		return dc;
	}

	private Host createHost() {
		final List<Pe> peList = new ArrayList<>(HOST_PES);
		//List of Host's CPUs (Processing Elements, PEs)
		
		int pesCount = RAND.nextInt(4)+1;
		int limit = pesCount;
		for (int i = 0; i < HOST_PES; i++) {
			//Uses a PeProvisionerSimple by default to provision PEs for VMs
			peList.add(new PeSimple(4500));
		}

		final long ram = 2048; //in Megabytes
		final long bw = 10000; //in Megabits/s
		final long storage = 1000000; //in Megabytes

		/*
	        Uses ResourceProvisionerSimple by default for RAM and BW provisioning
	        and VmSchedulerSpaceShared for VM scheduling.
		 */
		return new HostSimple(ram, bw, storage, peList);
	}

	
	private void createVmAndCloudlet() {
		System.out.println("creadAndSubmit is working");
		//1. create vms and cloudlets
		EntityCreator ec = new EntityCreator(dataset);
		//2. add them to the list
		if (this.algorithm == Constants.PRIORITY)
			vmList = ec.readVmAndCloudletAndSort();
		else
			vmList = ec.readVmAndCloudlet();
		cloudletList = ec.getCloudlet();
        simulation.addOnClockTickListener(this::submitDynamicCloudletAndVm);

	}
	
    private void submitDynamicCloudletAndVm(final EventInfo evt) {
    	// submitting new (or migrated) vm and its cloudlet
    	time = evt.getTime();
    		if(currentVmIndex < vmList.size()) {
    			placeVm(vmList.get(currentVmIndex));
    			currentVmIndex++;
    		}
    		// no more vms to submit, stop the listener
    		else if((int)evt.getTime() == 900 && !secondAttempt){
    			secondAttempt = true;

    			createDynamicCloudletAndVm();
    			simulation.removeOnClockTickListener(this::submitDynamicCloudletAndVm);
    		}
    		else if(currentVmIndex == vmList.size()) {
//    			simulation.removeOnClockTickListener(this::submitDynamicCloudletAndVm);
//    	    	System.out.println("\nAll vms submitted..current time from clock is: "+ simulation.clock());
//    			final List<Vm> executedVms = broker0.getVmExecList();
//    			new VmsTableBuilder(executedVms).build();
//    			currentVmIndex++;
    		}

    }
    
    private void placeVm(Vm vm) {
    	VmSimple currentVm = (VmSimple) vm;
    	resetSubmissionDelay(currentVm);
    	currentVm.addOnHostAllocationListener(eventInfo -> printVms(currentVm, "Allocation"));
    	//currentVm.addOnUpdateProcessingListener(eventInfo -> printVms(currentVm, "Processing updated"));
//		currentVm.addOnHostDeallocationListener(eventInfo -> placeVmAtHost(eventInfo.getVm().getHost()));
//		currentVm.addOnCreationFailureListener(eventInfo -> replaceFailedVm(eventInfo.getVm()));
		Cloudlet currentCloudlet = cloudletList.get(currentVm.getToken());
//		currentCloudlet.addOnStartListener(eventInfo -> printVms(currentVm, "its cloudlet"));
		currentCloudlet.addOnFinishListener(eventInfo -> System.out.println("\nfinished "+eventInfo.getCloudlet()));
		System.out.println("Vm: "+ currentVm.getToken() + " submission Time: " + time);
		broker0.submitVm(currentVm.setSubmissionTime(time));
		broker0.submitCloudlet(currentCloudlet);
		broker0.bindCloudletToVm(currentCloudlet, currentVm);
    }
    
    private void replaceFailedVm(Vm vm) {
    	VmSimple currentVm = (VmSimple) vm;
    	// print a msg
    	System.out.println("A vm is placed in the failed list " + currentVm.getToken());
    	//2. add it to the list of failedVms
    	failedVmList.add(vm);
    	//# option 1
    	// 3. listen to empty hosts
//    	vm.addOnHostDeallocationListener(eventInfo -> System.out.printf(
//                "%n\t#EventListener: Vm %d moved/removed from Host %d at time %.2f%n",
//                eventInfo.getVm().getId(), eventInfo.getHost().getId(), eventInfo.getTime()));
    	// 4. try to place it if possible
    	
    	// # option2
    	// place it within outer nodes network (maybe different allocation algorithm)
    }
    
    private void placeVmAtHost(Host host) {
    	System.out.printf(
                "%n\t#EventListener: Vm moved/removed from Host %d %n",
                host.getId());
    	if(failedVmList.size() > 0) {
    		secondAttempt = true;
    		//place the first vm at the host
    		placeVm(failedVmList.get(0));
    	}
    }
    
    // calling this method after the simulation terminates to print logs and any other job.
    public void postTerminationJobs() {
    	List<Cloudlet> finishedCloudlets = broker0.getCloudletFinishedList();
    	List<Cloudlet> createdCloudlets = broker0.getCloudletCreatedList();
    	

		new CloudletsTableBuilder(finishedCloudlets).build();
		new CloudletsTableBuilder(createdCloudlets).build();
		
		final List<Vm> createdVms = broker0.getVmCreatedList();
		final List<VmSimple> vms = broker0.getVmExecList();
        final List<VmSimple> vmsWaiting = broker0.getVmWaitingList();
        final List<VmSimple> vmsFailed = broker0.getVmFailedList();
		
		System.out.println("Created Vms");
        new VmsTableBuilder(createdVms).build();
//		System.out.println("Vms in Execution");
//		new VmsTableBuilder(vms).build();
//		System.out.println("Vms in Waiting");
//		new VmsTableBuilder(vmsWaiting).build();
//		System.out.println("Failed Vms");
//		new VmsTableBuilder(vmsFailed).build();

		
		try {
		    CsvTable csv = new CsvTable();
		    csv.setPrintStream(new PrintStream(new java.io.File("C:\\Users\\13635281\\OneDrive - UTS\\Documents\\implementation\\dataset_17_4_21\\results\\temp\\results.csv")));
		    new VmsTableBuilder(broker0.getVmCreatedList(), csv).build();
		} catch (IOException e) {
		    System.err.println(e.getMessage());
		}

    }
    
    public void printVms(VmSimple vm, String msg) {
  
		final List<Vm> createdVms = broker0.getVmCreatedList();
		final List<VmSimple> vms = broker0.getVmExecList();
        final List<VmSimple> vmsWaiting = broker0.getVmWaitingList();
        final List<VmSimple> vmsFailed = broker0.getVmFailedList();
		
		System.out.println(msg +" - Created Vms "+ vm.getToken() + " "+vm.hasStartedSomeCloudlet());
        new VmsTableBuilder(createdVms).build();
//		System.out.println(msg +" - Vms in Execution");
//		new VmsTableBuilder(vms).build();
//		System.out.println(msg +" - Vms in Waiting");
//		new VmsTableBuilder(vmsWaiting).build();
//		System.out.println(msg +" - Failed Vms");
//		new VmsTableBuilder(vmsFailed).build();
	}
    
    private void resetSubmissionDelay(Vm vm) {
    	double submissionDelay = vm.getSubmissionDelay();
    	if (submissionDelay == 0) {
    		return ;
    	}else {
    		vm.setSubmissionDelay(Math.abs(submissionDelay - time));
    		vm.setOldSubmissionDelay(submissionDelay);
    	}
    }
    
    // This method creates an extra cloudlet to stop the simulation from terminating before processing all cloudlets
    private void createDynamicCloudletAndVm() {
        
    	for (int i=0; i<3; i++) {
            System.out.printf("%n# Dynamically creating 1 Cloudlet at time %n");
//            Vm vm = createVm();
//            vmList.add(vm);
            Cloudlet cloudlet = createCloudlet();
            cloudletList.add(cloudlet);
            
            //broker0.submitVm(vm);
            broker0.submitCloudlet(cloudlet);
    	}
    	broker0.bindCloudletToVm(cloudletList.get(cloudletList.size()-1), vmList.get(3));
    }

    private Vm createVm() {
    	//Uses a CloudletSchedulerTimeShared by default to schedule Cloudlets
        final Vm vm = new VmSimple(1000, 4);
        vm.setRam(512).setBw(1000).setSize(10000);
        return vm;
    }

    private Cloudlet createCloudlet() {
        //UtilizationModel defining the Cloudlets use only 50% of any resource all the time
        final UtilizationModelDynamic utilizationModel = new UtilizationModelDynamic(0.9);
    	final Cloudlet cloudlet = new CloudletSimple(1000, 2, utilizationModel);
        cloudlet.setSizes(1024);
        return cloudlet;
    }
    
}



//        return vmWaitingList.stream().noneMatch(vm -> vm.getSubmissionDelay() == 0);

