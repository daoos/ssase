package org.ssase.primitive;

import org.ssase.actuator.Actuator;
import org.ssase.objective.Objective;

public class HardwareControlPrimitive  extends ControlPrimitive {

	public HardwareControlPrimitive(String name, String VM_ID, boolean isHardware,
			Type type, Actuator actuator, double provision, double constraint,
			int a,
			double b,
			double g,
			double h,
			double maxProvision) {
		super(name, VM_ID, isHardware, type, actuator, provision, constraint, a, b, g, h , maxProvision);
		// TODO Auto-generated constructor stub
	}
	@Deprecated
	public HardwareControlPrimitive(double[] array) {
		super(array);
		// TODO Auto-generated constructor stub
	}
	@Deprecated
	public HardwareControlPrimitive(double[] array, Objective... objs) {
		super(array, objs);
		// TODO Auto-generated constructor stub
	}
	@Override
	public int compareTo(Object o) {
		// TODO Auto-generated method stub
		return this.hashCode();
	}

}
