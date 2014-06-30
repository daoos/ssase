package org.ssascaling.model.selection;

import org.ssascaling.ModelFunction;

public interface StructureSelector {
	
	public void decideStructure (double[][] x, double[][] y, double meanIdeal, double[] functionConfig,
			double[] structureConfig);
	
	public void decideStructure(double[][] x, double[] y, double[] config);
	
	public int getOrder();
	
	public ModelFunction getModelFunction();

	public Object[] doDataSeparation (double[][] x, double[] y);
}
