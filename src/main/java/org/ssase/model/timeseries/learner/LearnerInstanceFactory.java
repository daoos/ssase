package org.ssase.model.timeseries.learner;

public class LearnerInstanceFactory {

	public static Learner getTranerInstance(){
		return new ApacheOLSLearner();
	}
}
