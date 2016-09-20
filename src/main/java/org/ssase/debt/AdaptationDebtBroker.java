package org.ssase.debt;

import java.util.List;

import org.ssase.debt.classification.OnlineClassifier;
import org.ssase.debt.elements.Interest;
import org.ssase.debt.elements.Principal;
import org.ssase.objective.QualityOfService;
import org.ssase.primitive.Primitive;

public class AdaptationDebtBroker {

	private List<QualityOfService> qos;
	private List<Primitive> primitives;
	private double noAdaptationUtility = Double.NaN;
	// 0 = adapt, 1 = no adapt
	private int latestJudgement = Integer.MIN_VALUE;
	
	// The post-adaptation unit for adaptation cost
	private double postUnit = 0.0;
	// The pre-adaptation unit for adaptation cost
	private double preUnit = 0.0;
	// The price for adaptation cost
	private static double cost = 0.0;

	private static AdaptationDebtBroker instance;
	
	public static void setAdaptationUnit(double cost){
		AdaptationDebtBroker.cost = cost; 
	}
	
	public static AdaptationDebtBroker getInstance(List<QualityOfService> qos, List<Primitive> primitives){
		if(instance == null) {
			instance = new AdaptationDebtBroker(qos, primitives);
		}
		
		return instance;
	}
	
	// 0=if adapt - 1=the index of time step in qos
	//private int[][] historicalJudgement;
	
	private OnlineClassifier classifier;
	
	private AdaptationDebtBroker (List<QualityOfService> qos, List<Primitive> primitives){
		this.qos = qos;
		this.primitives = primitives;
		classifier = new OnlineClassifier(qos, primitives);
	}
	
	/**
	 * Only trigger this when is has been decided to adapt.
	 * 
	 * Before adaptation, if trigger
	 */
	public void doPriorDebtAnalysis(){
		preUnit = getUnitForAdaptationCost();
		noAdaptationUtility = new Interest(qos).getMonetaryUtility();
	}
	
	public void doPosteriorDebtAnalysis(){
		
		if(Double.isNaN(noAdaptationUtility)) {
			return;
		}
		
		postUnit = getUnitForAdaptationCost();
		doPosteriorDebtAnalysis(postUnit - preUnit, cost);
		
		
		int judge = getExpertDebtJudgement();
		System.out.print("Training with judge: " + judge + "\n");
		classifier.trainOnInstance(judge, qos, primitives);
//		if(historicalJudgement == null) {
//			historicalJudgement = new int[1][1];
//			historicalJudgement[0] = new int[]{judge, qos.get(0).getArray().length-1};
//		} else {
//			int[][] copyHistoricalJudgement = new int[historicalJudgement.length+1][1];
//			
//			System.arraycopy(historicalJudgement, 0, copyHistoricalJudgement, 0, historicalJudgement.length);
//			copyHistoricalJudgement[copyHistoricalJudgement.length-1] = new int[]{judge, qos.get(0).getArray().length-1}; 
//			
//		}
//		
	}
	
	/**
	 * 
	 * Just after adaptation
	 */
	protected void doPosteriorDebtAnalysis(double unit, double cost){
		
		
		
		double adaptationUtility = new Interest(qos).getMonetaryUtility();
		Principal p = new Principal();
		p.setMonetaryUnit(unit);
		adaptationUtility = adaptationUtility - p.getMonetaryUtility(cost);
		
		System.out.print("adaptationUtility: " + adaptationUtility + " : noAdaptationUtility " + noAdaptationUtility + "\n");
		
		latestJudgement = adaptationUtility > noAdaptationUtility? 0 : 1;
		
		noAdaptationUtility = Double.NaN;
	}
	
	
	protected int getExpertDebtJudgement(){
		int r = latestJudgement;
		// clean it immediately
		latestJudgement = Integer.MIN_VALUE;
		return r;
	}
	
	public boolean isTrigger(){
		return 0 == classifier.predict(qos, primitives);
	}
	
	public double getUnitForAdaptationCost(){
		// TODO add recording of unit for measuring adaptation cost
		return System.currentTimeMillis();
	}
		 
}
