package nz.auckland.arch.planner.object;

import nz.auckland.arch.Component;
import nz.auckland.arch.Connector;

public class ComponentLinkage {

	private Connector connector;
	private Component origin;
	private Component destination;
	
	
	
	public ComponentLinkage(Connector connector, Component source, Component destination) {
		super();
		this.connector = connector;
		this.origin = source;
		this.destination = destination;
	}
	public Connector getConnector() {
		return connector;
	}
	public void setConnector(Connector connector) {
		this.connector = connector;
	}

	public Component getOrigin() {
		return origin;
	}
	public void setOrigin(Component origin) {
		this.origin = origin;
	}
	public Component getDestination() {
		return destination;
	}
	public void setDestination(Component destination) {
		this.destination = destination;
	}
	
	
}
