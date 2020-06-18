package nz.auckland.arch.planner.object;

import java.util.ArrayList;
import java.util.List;

public class Action {

	private int id;
	private String name;
	
	List<Parameter> parameters = new ArrayList<Parameter>();
	
	public Action(int id, String name) {
		super();
		this.id = id;
		this.name = name;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<Parameter> getParameters() {
		return parameters;
	}
	public void setParameters(List<Parameter> parameters) {
		this.parameters = parameters;
	}
	
}
