
/*
 * CloudSim Plus: A modern, highly-extensible and easier-to-use Framework for
 * Modeling and Simulation of Cloud Computing Infrastructures and Services.
 * http://cloudsimplus.org
 *
 *     Copyright (C) 2015-2018 Universidade da Beira Interior (UBI, Portugal) and
 *     the Instituto Federal de Educação Ciência e Tecnologia do Tocantins (IFTO, Brazil).
 *
 *     This file is part of CloudSim Plus.
 *
 *     CloudSim Plus is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     CloudSim Plus is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with CloudSim Plus. If not, see <http://www.gnu.org/licenses/>.
 */

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyFirstFit;
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
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.builders.tables.VmsTableBuilder;
import org.cloudsimplus.listeners.CloudletVmEventInfo;
import org.cloudsimplus.listeners.EventInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * An example showing how the {@link DatacenterBrokerSimple}
 * selects the next {@link Datacenter} when the previous one
 * is not able to create all {@link Vm}s due to lack
 * of suitable Hosts.
 *
 * <p>Each created Datacenter has hosts with more PEs than the previous Datacenter.
 * This way, only the Datacenter 4 will have Hosts to place the
 * created VMs.</p>
 *
 * @author Asma
 * @since CloudSim Plus 4.8.0
 */
public class MultibleDatacentersExample {
    private static final int DATACENTERS = 4;
    private static final int HOSTS = 1;

    private static final int VMS = 4;
    private static final int VM_PES = 2;

    private static final int CLOUDLETS = 8;
    private static final int CLOUDLET_PES = 2;
    private static final int CLOUDLET_LENGTH = 1000;

    private static final double MIN_TIME_BETWEEN_EVENT = 0.001;

    private final CloudSim simulation;
    private DatacenterBroker broker0;
    private List<Vm> vmList;
    private List<Cloudlet> cloudletList;
    private List<Datacenter> datacenterList;
    private long lastHostId;
    
    private int submissionDelay = 0;

    public static void main(String[] args) {
        new MultibleDatacentersExample();
    }

    private MultibleDatacentersExample() {
        /*Enables just some level of log messages.
          Make sure to import org.cloudsimplus.util.Log;*/
        //Log.setLevel(ch.qos.logback.classic.Level.WARN);

        simulation = new CloudSim(MIN_TIME_BETWEEN_EVENT);
        simulation.terminateAt(10000);
        datacenterList = createDatacenters();

        //Creates a broker that is a software acting on behalf a cloud customer to manage his/her VMs and Cloudlets
        broker0 = new DatacenterBrokerSimple(simulation);
        broker0.setVmDestructionDelayFunction(vm -> 0.0);

        vmList = createVms();
        cloudletList = createCloudlets();
//        broker0.submitVmList(vmList);
//        broker0.submitCloudletList(cloudletList);
        submitToBroker();

        //simulation.addOnClockTickListener(this::createDynamicCloudletAndVm);

        
        simulation.start();

        List<Cloudlet> createdCloudlets = broker0.getCloudletCreatedList();
		new CloudletsTableBuilder(createdCloudlets).build();
        final List<Cloudlet> finishedCloudlets = broker0.getCloudletFinishedList();
        new CloudletsTableBuilder(finishedCloudlets).build();
        final List<VmSimple> vms = broker0.getVmExecList();
        final List<VmSimple> vmsWaiting = broker0.getVmWaitingList();
        final List<VmSimple> vmsFailed = broker0.getVmFailedList();
        System.out.println("Vms in Execution");
        new VmsTableBuilder(vms).build();
//        System.out.println("Vms in Waiting");
//        new VmsTableBuilder(vmsWaiting).build();
//        System.out.println("Failed Vms");
//        new VmsTableBuilder(vmsFailed).build();
        
        for (int i=0;i<createdCloudlets.size();i++) {
        	Cloudlet c = createdCloudlets.get(i);
        	System.out.print("id "+c.getId());
        	System.out.println(" bound to VM " + c.isBoundToVm());
        }

    }
    
    private void submitToBroker() {
    	for(int i=0; i < vmList.size(); i++) {
    		broker0.submitVm(vmList.get(i));
    		broker0.submitCloudlet(cloudletList.get(i));
    		cloudletList.get(i).addOnUpdateProcessingListener(this::onClouletProcessingUpdate);
    		broker0.bindCloudletToVm(cloudletList.get(i), vmList.get(i));
    		//cloudletList.get(i).setExecStartTime(1);
    	}
    }
    
    private void onClouletProcessingUpdate(CloudletVmEventInfo event) {
        if(event.getCloudlet().getFinishedLengthSoFar() >= event.getCloudlet().getLength()){
            System.out.printf(
                "%s reached 50%% of execution. Intentionally requesting termination of the simulation at time %.2f%n",
                event.getCloudlet(), simulation.clock());
            simulation.terminate();
        }
    }
    
    private void createDynamicCloudletAndVm(final EventInfo evt) {
        if((int)evt.getTime() == 9000){
            System.out.printf("%n# Dynamically creating 1 Cloudlet and 1 VM at time %.2f%n", evt.getTime());
            Vm vm = createVm();
            vm.setId(8);
            vmList.add(vm);
            Cloudlet cloudlet = createCloudlet();
            cloudletList.add(cloudlet);

            broker0.submitVm(vm);
            broker0.submitCloudlet(cloudlet);
        }
    }

    

    /**
     * Creates a List of Datacenters, each Datacenter having
     * Hosts with a number of PEs higher than the previous Datacenter.
     * @return
     */
    private List<Datacenter> createDatacenters(){
        final List<Datacenter> list = new ArrayList<>(DATACENTERS);
        for (int i = 1; i <= DATACENTERS; i++) {
            list.add(createDatacenter(4));
        }

        return list;
    }

    /**
     * Creates a Datacenter and its Hosts.
     * @param hostsPes the number of PEs for the Hosts in the Datacenter created
     */
    private Datacenter createDatacenter(final int hostsPes) {
        final List<Host> hostList = new ArrayList<>(HOSTS);
        for(int i = 0; i < HOSTS; i++) {
            Host host = createHost(hostsPes);
            hostList.add(host);
        }

        //Uses a VmAllocationPolicySimple by default to allocate VMs
        DatacenterSimple dc = new DatacenterSimple(simulation, hostList);
        dc.setSchedulingInterval(1.0);
        dc.setVmAllocationPolicy(new VmAllocationPolicyFirstFit());
        return dc;
    }

    private Host createHost(final int pes) {
        final List<Pe> peList = new ArrayList<>(pes);
        //List of Host's CPUs (Processing Elements, PEs)
        for (int i = 0; i < pes; i++) {
            //Uses a PeProvisionerSimple by default to provision PEs for VMs
            peList.add(new PeSimple(1000));
        }

        final long ram = 2048; //in Megabytes
        final long bw = 10000; //in Megabits/s
        final long storage = 1000000; //in Megabytes

        /*
        Uses ResourceProvisionerSimple by default for RAM and BW provisioning
        and VmSchedulerSpaceShared for VM scheduling.
        */
        final Host host = new HostSimple(ram, bw, storage, peList);
        host.setId(++lastHostId);
        return host;
    }

    /**
     * Creates a list of VMs.
     */
    private List<Vm> createVms() {
        final List<Vm> list = new ArrayList<>(VMS);
        for (int i = 0; i < VMS; i++) {
            list.add(createVm());
        }
        return list;
    }
    
    private Vm createVm() {
    	//Uses a CloudletSchedulerTimeShared by default to schedule Cloudlets
        final Vm vm = new VmSimple(1000, VM_PES);
        vm.setRam(512).setBw(1000).setSize(10000);
        vm.setSubmissionDelay(submissionDelay);
        //submissionDelay+=9;
        return vm;
    }

    /**
     * Creates a list of Cloudlets.
     */
    private List<Cloudlet> createCloudlets() {
        final List<Cloudlet> list = new ArrayList<>(CLOUDLETS);

        for (int i = 0; i < CLOUDLETS; i++) {
            
            list.add(createCloudlet());
        }

        return list;
    }
    
    private Cloudlet createCloudlet() {
        //UtilizationModel defining the Cloudlets use only 50% of any resource all the time
        final UtilizationModelDynamic utilizationModel = new UtilizationModelDynamic(0.9);
    	final Cloudlet cloudlet = new CloudletSimple(CLOUDLET_LENGTH, CLOUDLET_PES, utilizationModel);
        cloudlet.setSizes(1024);
        return cloudlet;
    }
}
