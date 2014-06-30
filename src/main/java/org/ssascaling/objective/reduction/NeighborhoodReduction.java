package org.ssascaling.objective.reduction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ssascaling.objective.Objective;
import org.ssascaling.objective.correlation.Spearmans;
import org.ssascaling.qos.QualityOfService;


/**
 * The reduction and the updating of models within the region should be synchronized.
 * the ant colony should be tied to the models as well, so that it can decide whether to
 * restart or memory when dynamics occur.
 * @author tao
 *
 */
public abstract class NeighborhoodReduction implements Reduction {

	private static Map<Objective, Map<Objective, Double>> map = new HashMap<Objective, Map<Objective, Double>>();
	

	@SuppressWarnings("rawtypes")
	public List<List<Objective>>  doReduction (List<Objective> objectives) {
		Spearmans spear = new Spearmans();
		long time = System.currentTimeMillis();
		for (Objective obj : objectives) {
			for (Objective subobj : objectives) {
				if (!obj.equals(subobj)) {
					if (!map.containsKey(obj)) {
						map.put(obj, new HashMap<Objective, Double>());
					}
					
					map.get(obj).put(subobj, 1 - spear.doCorrelation(obj.getArray(), subobj.getArray()));
				}
			}
				
		}
		System.out.print("Time taken for matrixing: " + ( System.currentTimeMillis() - time) + "\n");
		
		
		final List[] clusters = clustering(objectives);
		
		
		reduce(clusters);
		
		System.out.print("Time taken of all objectives reduction: " + ( System.currentTimeMillis() - time) + "\n");
		
		List<List<Objective>> result = new ArrayList<List<Objective>>(); 
		for (List d : clusters){
			System.out.println("Cluster *****" + d.size()+"\n");
			List<Objective> cluster = new ArrayList<Objective>();
			result.add(cluster);
        	for (Object i : d){
        		cluster.add((Objective)getClassValue(i));
        		System.out.print("Selected: " + ((QualityOfService)getClassValue(i)).getName()+ "\n");
        		
        	}
        }
		
	
		return result;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void reduce(List[] clusters) {
		int q = 0;
		List<Object> centroids = new ArrayList<Object>();
		for (List<?> cluster : clusters) {
			q = ((cluster.size() - 1) > q)? (cluster.size() - 1) : q;
			centroids.add(centroid(cluster));
		}
		
		
		int compact = -1;
		for (; q > 0; q--) {
			double distance = 0.0;
			// Reset
			compact = -1;
			for (int i = 0; i < clusters.length; i ++) {
				
				// Removed neighborhood
				if (clusters[i].size() == 1){
					continue;
				}
				
				final Object temp = kNearest(q, centroids.get(i), clusters[i]);
				
				// The q is larger than the size of cluster.
				if (temp == null) {
					continue;
				}
				
				final double currentDistance = map.get(getClassValue(centroids.get(i)))
				.get(getClassValue(temp));
				
				
				if (distance == 0.0 || currentDistance < distance){
					distance = currentDistance;
					compact = i;
				}
			}
			
			// Means no clusters has members more than the current q.
			if (compact == -1) {
				continue;
			}
			
			clusters[compact].clear();
			clusters[compact].add(centroids.get(compact));
		}
	}

	private Object centroid(List<?> set) {

		Object instance = null;
		double fValue = 0.0;
		for (Object ins : set) {
			double value = 0.0;
			for (Object subIns : set) {
				if (!ins.equals(subIns)) {
					value += map.get(getClassValue(ins)).get(
							getClassValue(subIns));
				}
			}

			if (instance == null || value < fValue) {
				instance = ins;
				fValue = value;
			}
		}

		return instance;
	}

	private Object kNearest(int k, Object inst, List<?> set) {
		if (k > set.size()) {
			return null;
		}

		Map<Object, Double> closest = new HashMap<Object, Double>();
		@SuppressWarnings("unused")
		double max = Double.POSITIVE_INFINITY;
		for (Object tmp : set) {
			if (!inst.equals(tmp)) {
				double d = map.get(getClassValue(inst)).get(getClassValue(tmp)); // dm.measure(inst,
																					// tmp);
				closest.put(tmp, d);
				if (closest.size() > k)
					max = removeFarthest(closest);
			}

		}

		double distance = 0.0;
		Object instance = null;
		for (Object ins : closest.keySet()) {
			if (instance == null || closest.get(ins) > distance) {
				instance = ins;
				distance = closest.get(ins);
			}
		}

		// System.out.print(inst.classValue() + "\n");
		// System.out.print(instance.classValue() + "\n");

		return instance;
	}

	private double removeFarthest(Map<Object, Double> vector) {
		Object tmp = null;// ; = vector.get(0);
		double max = 0;
		for (Object inst : vector.keySet()) {
			double d = vector.get(inst);
			if (d > max) {
				max = d;
				tmp = inst;
			}
		}
		vector.remove(tmp);
		return max;

	}

	@SuppressWarnings("rawtypes")
	public abstract List[] clustering (List<Objective> objectives);
		
	public abstract Object getClassValue (Object instance);
}
