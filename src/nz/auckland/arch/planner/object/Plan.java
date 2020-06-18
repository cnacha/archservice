package nz.auckland.arch.planner.object;

import java.util.ArrayList;
import java.util.List;

public class Plan {

	List<Action> steps;
	
	public Plan() {
		steps = new ArrayList<Action>();
	}
	
	public void addStep(Action s) {
		steps.add(s);
	}
	
	public Action getStep(int ind) {
		return steps.get(ind);
	}
	public int getStepCount() {
		return steps.size();
	}

	public List<Action> getSteps() {
		return steps;
	}

	public void setSteps(List<Action> steps) {
		this.steps = steps;
	}
	
	

}
