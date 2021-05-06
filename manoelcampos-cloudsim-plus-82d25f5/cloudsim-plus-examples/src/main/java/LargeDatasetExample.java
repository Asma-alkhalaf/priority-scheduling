import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyPriority;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyAbstract;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
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
import org.cloudbus.cloudsim.vms.VmStateHistoryEntry;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.builders.tables.VmsTableBuilder;
import org.cloudsimplus.listeners.EventInfo;
import org.cloudsimplus.util.Log;

// Excel File imports
  


public class LargeDatasetExample {
	private static final int HOSTS = 10;
	private static final int HOST_PES = 8;

	private static final int VMS = 20; //original 2
	private static final int VM_PES = 2; // original 4

	private static final int CLOUDLETS = 6; 
	private static final int CLOUDLET_PES = 2;
	private static final int CLOUDLET_LENGTH = 10000;
    private static final double TIME_TO_TERMINATE_SIMULATION =  5000000; //4377974 ; //4379000;
    private static final double TIME_TO_CREATE_NEW_CLOUDLET = 1;
    private static int currentVmIndex = 0;
    
	private final CloudSim simulation;
	private final VmAllocationPolicyPriority vmAllocation = new VmAllocationPolicyPriority();
//	private final VmAllocationPolicySimple vmAllocation = new VmAllocationPolicySimple();

	private DatacenterBroker broker0;
	private List<Vm> vmList;
	private List<Cloudlet> cloudletList;
	private Datacenter datacenter0;
	
	private double [][] vmFactory;
	private final Random RAND = new Random();
	private double time = 0;
	private boolean firstTime = true;


	public static void main(String[] args) {
		new LargeDatasetExample();
	}

	private LargeDatasetExample() {
		/*Enables just some level of log messages.
	          Make sure to import org.cloudsimplus.util.Log;*/
		Log.setLevel(ch.qos.logback.classic.Level.WARN);
		
		
		
		simulation = new CloudSim();
        simulation.terminateAt(TIME_TO_TERMINATE_SIMULATION);
		datacenter0 = createDatacenter();

		//Creates a broker that is a software acting on behalf a cloud customer to manage his/her VMs and Cloudlets
		broker0 = new DatacenterBrokerSimple(simulation);
		
		createVmAndCloudlet();

		simulation.start();
				// all statements after this line will not be executed until the simultion ends (or aborted)
		System.out.println("\ncalling the prinintg methods..."+time);
		postTerminationJobs();
		System.out.println("\ncalling the prinintg methods is done");
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

		//Uses a VmAllocationPolicyPriority to allocate VMs
		//return new DatacenterSimple(simulation, hostList);
		final Datacenter dc = new DatacenterSimple(simulation, hostList, vmAllocation);
		//dc.setSchedulingInterval(1);
	
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

	/**
	 * Creates a list of VMs.
	 */
	private List<Vm> createVms() {
		
		int count = 32;
		int submissionDelay = RAND.nextInt(10);
		return (new EntityCreator()).readVms(count, submissionDelay);
		

	}

	/**
	 * Creates a list of Cloudlets.
	 */
	private List<Cloudlet> createCloudlets() {
		
		int count = 66;
		return (new EntityCreator()).readCloudlets(count);
		
	}
	
	private void createVmAndCloudlet() {
		System.out.println("creadAndSubmit is working");
		//1. create vms and cloudlets
		EntityCreator ec = new EntityCreator();
		//2. add them to the list
		vmList = ec.readVmAndCloudlet(32);
		cloudletList = ec.getCloudlet();
        simulation.addOnClockTickListener(this::submitDynamicCloudletAndVm);
        broker0.addOnVmsCreatedListener(this::getLastCreatedVm); //not used now


	}
	
    private void submitDynamicCloudletAndVm(final EventInfo evt) {
    	// submitting new (migrated) vm and its cloudlet
    	time = evt.getTime();
    		if(currentVmIndex < vmList.size()) {
    			//3. submit to the broker
        		System.out.println("submitting vm: " + currentVmIndex + " current time: " + time);
    			broker0.submitVm(vmList.get(currentVmIndex).setSubmissionTime(time));
    			broker0.submitCloudlet(cloudletList.get(currentVmIndex));
    			currentVmIndex++;
    		}// no more vms to submit, stop the listener
    		else if(currentVmIndex == vmList.size()) {
//    			//3. submit to the broker
//        		System.out.println("submitting vm: " + currentVmIndex);
//    			broker0.submitVm(vmList.get(currentVmIndex));
//    			broker0.submitCloudlet(cloudletList.get(currentVmIndex));
//    			currentVmIndex++;
    			simulation.removeOnClockTickListener(this::submitDynamicCloudletAndVm);
//    			final List<Cloudlet> finishedCloudlets = broker0.getCloudletFinishedList();
//    			new CloudletsTableBuilder(finishedCloudlets).build();
    	    	System.out.println("\nAll vms submitted..current time from clock is: "+ simulation.clock());
    			final List<Vm> executedVms = broker0.getVmExecList();
    			new VmsTableBuilder(executedVms).build();
    			currentVmIndex++;
    		}
//    		else if(vmList.get(2).isIdleEnough(500) && firstTime) {
//    			firstTime = false;
//    			simulation.terminateAt(time);
//    			postTerminationJobs();
//
//    		}
    		
    }
    
    private void getLastCreatedVm(final EventInfo evt) {
    	System.out.println("");
    	// still working on this - not needed for now
    }
    
    // calling this method after the simulation terminates to print logs and any other job.
    public void postTerminationJobs() {
    	List<Cloudlet> finishedCloudlets = broker0.getCloudletFinishedList();
    	while (finishedCloudlets.size() != 20) {
    		System.out.println("\ncalling the prinintg methods is done");
    		finishedCloudlets = broker0.getCloudletFinishedList();
    	}

		new CloudletsTableBuilder(finishedCloudlets).build();
		
		final List<Vm> createdVms = broker0.getVmCreatedList();
		new VmsTableBuilder(createdVms).build();
		
		printVM();
		simulation.terminateAt(time);
    	
		// checking if we have the information of all the finished cloudlets before we terminate
//		if(finishedCloudlets.size() == 20) {
//			simulation.abort();
//			printVM();
//		}
//		else {
//			firstTime = true;
//			System.out.println("Still processing some cloudlets...");
//		}
    }
    
    public void printVM() {
    	double teminationTime = simulation.getTerminationTime();
    	System.out.println("The termination time is: " + teminationTime);
    	System.out.println("the current time from event is: " + time);
    	System.out.println("current time from clock is: "+ simulation.clock());
    	List<Vm> createdVms = broker0.getVmCreatedList();
    	System.out.println("The list of created VMs is: ");
//    	for (Vm vm : createdVms) {
//        	System.out.println("The states list of the VMs: " + vm.getId());
//    		List<VmStateHistoryEntry> states = vm.getStateHistory();
//    		for (VmStateHistoryEntry stateEntry : states) {
//        		System.out.println("the state entry for the vm is " + stateEntry.getTime());
//
//    		}
//    	}
    }
	
    private double roundTime(double time) {
        final double startFraction = time - (int) time;
        return startFraction;
    }
}


