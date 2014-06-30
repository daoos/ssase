package org.ssascaling.model.selection;

import org.ssascaling.ModelFunction;
import org.ssascaling.model.ann.NeuralNetwork;
import org.ssascaling.qos.QualityOfService;

public class AIC  {

	

	/*@Override
	public double evaluate(ModelFunction model, boolean isUseTimeSeries) {
		int k = isUseTimeSeries? model.getNumOfInput()*2 + 2 : model.getNumOfInput();
		int degree = 0;
		if (model instanceof NeuralNetwork) {
			for (int i = 0; i < model.getDegree().length; i++) {
				
				if((i+1) >  model.getDegree().length -1) {
					break;
				}
				
				degree += model.getDegree()[i] * model.getDegree()[i+1];
			}
			
			k = k * model.getDegree()[0] + degree +  model.getDegree()[model.getDegree().length - 1];
		}
		return estimate(model.getResidualSumOfSquares(), model.getSampleSize(), k);
	}
	*/
	
	public static void main (String[] a){
		System.out.print(estimate(0.3,220,2));
	}
	
	private static double estimate (double rss, long n, int k) {
		if (n/k < 40) {
			return n * Math.log(rss/n) + 2*k + (2*k*(k+1))/(n-k-1);
		} else {
			return n * Math.log(rss/n) + 2*k;
		}
	}

}
