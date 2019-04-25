package org.ssase.requirement.froas.fuzzy;

public class RP4Function implements FuzzyFunction {

	
	private MathFunction func;
	
	public RP4Function(MathFunction func) {
		this.func = func;
	}
	@Override
	public double fuzzilize(double original, double d) {
		if (original == d) {
			return 1;
		} else if (original == 0 || original == 1) {
			return 0;
		} else {
			if (func instanceof ErrorFunction) {
				
				if(original < d) {
					double v = func.calculate((6*original/d) - 3);			
					return 1 - (0.5 * v);
				} else {
					double v = func.calculate((6 * (original - d) / (1 - d)) - 3);
					return 0.5 * v;
				}
				
				
			}
		}
		return original;

	}

}
