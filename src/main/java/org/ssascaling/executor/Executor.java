package org.ssascaling.executor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ssascaling.Service;
import org.ssascaling.actuator.Actuator;
import org.ssascaling.actuator.linux.CPUNoActuator;
import org.ssascaling.actuator.linux.CPUPinActuator;
import org.ssascaling.primitive.ControlPrimitive;
import org.ssascaling.primitive.HardwareControlPrimitive;
import org.ssascaling.primitive.SoftwareControlPrimitive;
import org.ssascaling.primitive.Type;
import org.ssascaling.util.Repository;

public class Executor {
	
	/*Mainly need to record the provision of hardware CPs*/
	private static int totalMemory; /*Mb*/
	private static int remainingMemory;
	
	private static Object lock = new Object();
	
	private static Actuator cpuNoActuator = new CPUNoActuator();
	private static Actuator cpuPinActuator = new CPUPinActuator();
	
	private static List<CPUCore> cores = new ArrayList<CPUCore>();
	
	public final static boolean isTest = true;
	public final static long memoryThreshold = 100;
	public final static long CPUThreshold = 10;
	
	public static void init(HardwareControlPrimitive... primitives) {
		totalMemory = 2048 - 500;
		remainingMemory = totalMemory;
		
		/*This is a testing setup, the real setup should come from property files*/
		Repository.setService("jeos-test.service1", new Service());
		Repository.setService("jeos-test.service2", new Service());  
		
		/*2 thread software CP, 6 CPU/memory hardware CP*/
		
		VM jeos = new VM("jeos", new HardwareControlPrimitive[]{primitives[0], primitives[1]});
		VM kitty = new VM("kitty", new HardwareControlPrimitive[]{primitives[2], primitives[3]});
		VM miku = new VM("miku", new HardwareControlPrimitive[]{primitives[4], primitives[5]});
		
		remainingMemory -= 600;
		
		
		Repository.setVM("jeos", jeos);
		Repository.setVM("kitty", kitty);
		Repository.setVM("miku", miku);
		
		cores.add(new CPUCore(1, new VM[]{jeos}));
		cores.add(new CPUCore(2, new VM[]{kitty}));
		cores.add(new CPUCore(3, new VM[]{miku}));
	}
	
	/**
	 * 
	 * Double value here should be the denormalized ones, which should be fine as the normalized data
	 * is only done and used within QualityOfService class.
	 * @param decisions
	 */
	public static void execute(LinkedHashMap<ControlPrimitive, Double> decisions){
		long value  = 0;
		
		
		LinkedHashMap<ControlPrimitive, Double> listMap = orderDecision(decisions);
		for (Map.Entry<ControlPrimitive, Double> entry :  listMap.entrySet()){
			

			// If not a responsible service nor VM, then return
			if (!Repository.isContainService(entry.getKey().getAlias()) && !Repository.isContainVM(entry.getKey().getAlias())){
				return;
			}
			
			value = Math.round(entry.getValue());
			
			//TODO if value is smaller than a threshold, should trigger scale in.
			/**
			 * Hardware CP allocation require special treatments.
			 */
			if (entry.getKey().isHardware()) {
				// Need to sync in order to consistent on the utilization of resource on the PM.
				synchronized (lock) {
					// CPU is sepecial as its denormalized value is still %
					if (Type.CPU.equals(entry.getKey().getType())) {
						long finalValue = value;
						if (value < CPUThreshold) {
							System.out.print("Scale in due to CPU on " + entry.getKey().getAlias() + " \n");
							// TODO do scale in
							return;
						}
						
						VM vm = Repository.getVM(entry.getKey().getAlias());
						// Scale down, always remove the core with higher ID first.
						if (!vm.isScaleUp(value)) {
							long count = 0;
							int no = 0;
							for (CPUCore core : cores) {
								
								if (core.getVMs().containsKey(vm)) {
									count+=core.getVMs().get(vm);
									// If more CPU allocation on a core.
									if (count > value) {
										long minus = count - value;
										long change = core.getVMs().get(vm) - minus;
										
										core.getVMs().put(vm,  change);
										core.update(minus);
										// if can remove the core.
										if (change == 0) {
											core.getVMs().remove(vm);									
										}  else {
											no ++;	
										}
										//TODO do pin cpu (we do not really need this? as if no core needed to be removed then
										// nothing change, if there is, then it is simply removed by cpu_set as a core with higher ID) *************
										
										count = value;
									} else {
										no ++;
									}
									
									
							    }
							}
							
							if (vm.getCPUNo() != no) {
							// Set the CPU core number
							cpuNoActuator.execute(entry.getKey().getAlias(), no);
							}
							


							// this is only the cap one.
							entry.getKey().triggerActuator(new long[] { value });
						} else /*Scale up*/ {
							long add = value - vm.getCpuCap();
							// new cpu core number
							long newNo = 0;
							
							final List<Integer> newCoreIndex = new ArrayList<Integer>();
							
							for (CPUCore core : cores) {
								
								if (add == 0) {
									break;
								}
									
								
								long allocated = core.allocate(add);
								if (allocated != 0) {
									
									if (!core.getVMs().containsKey(vm)) {
									     newCoreIndex.add(core.getPhysicalID());
									     newNo ++;
									}
									core.getVMs().put(vm,  core.getVMs().containsKey(vm)?
											core.getVMs().get(vm) + allocated : allocated);
									core.update(0 - allocated);
									
								}
								
								add -= allocated;
							}
							
							if (add > 0) {
								//TODO should trigger scale out.
								System.out.print("Scale out due to CPU on " + entry.getKey().getAlias() + " \n");
							}
							
							
							if (newNo != 0) {
								// Set the CPU core number
								cpuNoActuator.execute(entry.getKey().getAlias(), newNo + vm.getCPUNo());
							}
							
							

							finalValue = add > 0? (value-add) : value;
							// This is only the cap one.
							entry.getKey().triggerActuator(new long[] { finalValue });
							// Do pin cpu
							int start = ((int)vm.getCPUNo()) + 1;
							for (int index : newCoreIndex) {
								cpuPinActuator.execute(entry.getKey().getAlias(), new long[]{start, index});
								start++;
							}
							 
						}
						
						
						entry.getKey().setProvision(finalValue);

					} else if (Type.Memory.equals(entry.getKey().getType())) {

						if (value < memoryThreshold) {
							System.out.print("Scale in due to memory on " + entry.getKey().getAlias() + " \n");
							// TODO do scale in
							return;
						}
						
						if (remainingMemory >= value) {
							remainingMemory -= value - entry.getKey().getProvision();
							
							entry.getKey().setProvision(value);
							entry.getKey()
									.triggerActuator(new long[] { value });
						} else {
							// TODO should trigger migration or replication for scale out as
							// the memory is insufficient.
							System.out.print("Scale out due to memory on " + entry.getKey().getAlias() + " \n");
						}
					}
				}
				
			} else {
				entry.getKey().triggerActuator(new long[]{value});
				entry.getKey().setProvision(value);
			}	
			

		
		}
		
		
	}
	
	public static void print(){
		for (CPUCore core : cores) {
			core.print();
		}
		
		System.out.print("Remaining memory=" + remainingMemory+"\n");
	}
	
	private static LinkedHashMap<ControlPrimitive, Double> orderDecision(final LinkedHashMap<ControlPrimitive, Double> decisions){
	
		List<ControlPrimitive> list = new ArrayList<ControlPrimitive>(decisions.keySet());
		Collections.sort(list, new Comparator<ControlPrimitive>(){

			@Override
			public int compare(ControlPrimitive cp1, ControlPrimitive cp2) {
				if (cp1 instanceof SoftwareControlPrimitive && cp2 instanceof HardwareControlPrimitive) {
					return 1;
				}
				
				if (cp1 instanceof HardwareControlPrimitive && cp2 instanceof SoftwareControlPrimitive) {
					return -1;
				}
				
				// else, both should be either software CP or hardware CP.
				double cp1Value = decisions.get(cp1) ;
				double cp2Value = decisions.get(cp2) ;
				if (cp1.getProvision() > cp1Value && cp2.getProvision() < cp2Value) {
					return -1;
				}
				
				
				
				if (cp1.getProvision() < cp1Value && cp2.getProvision() > cp2Value) {
					return 1;
				}
				
				// If both require scale up, then the one that requires less go before.
				if (cp1.getProvision() < cp1Value && cp2.getProvision() < cp2Value) {
					return  cp1Value > cp2Value? 1 : -1;
				}
				
				return 0;
			}

			
			
		});
		
		LinkedHashMap<ControlPrimitive, Double> newDecisions = new LinkedHashMap<ControlPrimitive, Double>();
		for (ControlPrimitive p : list) {
			newDecisions.put(p, decisions.get(p));
		}
		
		return newDecisions;
	}
}
