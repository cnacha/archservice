package nz.auckland.arch.planner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import fr.uga.pddl4j.encoding.CodedProblem;
import fr.uga.pddl4j.heuristics.relaxation.Heuristic;
import fr.uga.pddl4j.parser.ErrorManager;
import fr.uga.pddl4j.planners.ProblemFactory;
import fr.uga.pddl4j.planners.statespace.StateSpacePlanner;
import fr.uga.pddl4j.planners.statespace.ff.FF;
import fr.uga.pddl4j.planners.statespace.generic.GenericPlanner;
import fr.uga.pddl4j.planners.statespace.search.strategy.AStar;
import fr.uga.pddl4j.planners.statespace.search.strategy.BreadthFirstSearch;
import fr.uga.pddl4j.planners.statespace.search.strategy.DepthFirstSearch;
import fr.uga.pddl4j.planners.statespace.search.strategy.EnforcedHillClimbing;
import fr.uga.pddl4j.planners.statespace.search.strategy.GreedyBestFirstSearch;
import fr.uga.pddl4j.util.BitOp;
import fr.uga.pddl4j.util.SequentialPlan;
import nz.auckland.arch.Component;
import nz.auckland.arch.Connector;
import nz.auckland.arch.DesignModel;
import nz.auckland.arch.Device;
import nz.auckland.arch.ExecutionEnvironment;
import nz.auckland.arch.HostType;
import nz.auckland.arch.NetAccessType;
import nz.auckland.arch.NodeType;
import nz.auckland.arch.Port;
import nz.auckland.arch.Role;
import nz.auckland.arch.planner.object.Action;
import nz.auckland.arch.planner.object.ComponentLinkage;
import nz.auckland.arch.planner.object.Parameter;
import nz.auckland.arch.planner.object.Plan;

public class MigrationPlanner {

	private DesignModel sourceModel;
	private DesignModel targetModel;

	private static final String PDDL_URL = "D:/config/pddl/";

	public MigrationPlanner(DesignModel sourceModel, DesignModel targetModel) {
		this.sourceModel = sourceModel;
		this.targetModel = targetModel;
	}

	public Plan run() {
		System.out.println("MigrationPlanner started...");
		// generate problem
		try {
			String problemStr = genereateProblem();
			System.out.println(problemStr);

			String domainStr = readDomain();
			
			final ProblemFactory factory = ProblemFactory.getInstance();
			System.out.println("Factory initiates....");
			ErrorManager errorManager = factory.parseFromString(domainStr, problemStr);
			if (!errorManager.isEmpty()) {
				errorManager.printAll();
			}

			// execute planner
			EnforcedHillClimbing stateSpaceStrategy = new EnforcedHillClimbing(15 * 1000, Heuristic.Type.FAST_FORWARD, 1);
			//AStar stateSpaceStrategy = new AStar(15 * 1000, Heuristic.Type.FAST_FORWARD, 1);
		   // GreedyBestFirstSearch stateSpaceStrategy = new   GreedyBestFirstSearch(150 * 1000, Heuristic.Type.FAST_FORWARD, 1);
			GenericPlanner  planner = new GenericPlanner(stateSpaceStrategy);
			
			CodedProblem pb = factory.encode();
			SequentialPlan plan = planner.search(pb);
			System.out.println(plan.toString());
			

			// print plan
			Plan migrationPlan = new Plan();
			int count =0;
			for (BitOp op : plan.actions()) {
				Action action = new Action(count, op.getName());
				
				 for (int i = 0; i < op.getArity(); i++) {
			             int index = op.getValueOfParameter(i);
			             String type = pb.getTypes().get(op.getTypeOfParameters(i));
			           
			             if (index != -1) {
			            	// System.out.println(type +":"+pb.getConstants().get(index));
			            	 action.getParameters().add(new Parameter(type, pb.getConstants().get(index)));
			             } else {
			            	// System.out.println(type +":"+ "?");
			            	 action.getParameters().add(new Parameter(type, "?"));
			             }
			             
				 }
				 migrationPlan.addStep(action);
				 count++;
			}
			return migrationPlan;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// read domain

		// execute planner

		Plan emptyPlan = new Plan();

		return emptyPlan;
	}

	private String readDomain() throws Exception {
		File domainFile = new File(PDDL_URL + "migration.pddl");
		BufferedReader reader = null;
		StringBuilder stringBuilder = new StringBuilder();
		try {
			reader = new BufferedReader(new FileReader(domainFile));

			String line = null;
			String ls = System.getProperty("line.separator");
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line);
				stringBuilder.append(ls);
			}
			// delete the last new line separator
			stringBuilder.deleteCharAt(stringBuilder.length() - 1);

		} finally {
			if (reader != null)
				reader.close();
		}

		return stringBuilder.toString();
	}

	private String genereateProblem() {
		// find added and updated connector
		findAddedUpdatedConnector();

		StringBuffer objectsStr = new StringBuffer();
		StringBuffer initStr = new StringBuffer();
		StringBuffer goalStr = new StringBuffer();

		// find involved component for predicates
		Set<Component> involvedComponents = new HashSet<Component>();
		Set<Connector> involvedConnectors = new HashSet<Connector>();
		involvedConnectors.addAll(addedConnectorList);
		involvedConnectors.addAll(updatedConnectorList);

		for (Connector conn : involvedConnectors) {
			for (Role role : conn.getRole()) {
				involvedComponents
						.addAll(findComponentByConnectorRoleName(targetModel, conn.getName(), role.getName()));
			}
			// generate an object for connector
			objectsStr.append("\t\t" + conn.getName() + " - connector\n");

			// generate connect predicate for init
			List<ComponentLinkage> initLinkages = this.findComponentLinkage(targetModel, conn.getName());
			for (ComponentLinkage linkage : initLinkages) {
				initStr.append("\t\t(connect " + conn.getName() + " " + linkage.getOrigin().getName() + " "
						+ linkage.getDestination().getName() + ")\n");
			}

			// generate netbound predicate for goal
			List<ComponentLinkage> goalLinkages = this.findComponentLinkage(targetModel, conn.getName());
			for (ComponentLinkage linkage : goalLinkages) {
				goalStr.append("\t\t(netbound " + linkage.getOrigin().getName() + " "
						+ linkage.getDestination().getName() + ")\n");
			}

		}

		// generate predicates for component
		for (Component comp : involvedComponents) {
			// component is data store
			if (comp.getType().indexOf("datastore") != -1) {
				objectsStr.append("\t\t" + comp.getName() + " - datastore\n");

				// component is application
			} else if (hasComponentType(comp, "OutboundPort") && !hasComponentType(comp, "InboundPort")) {
				objectsStr.append("\t\t" + comp.getName() + " - application\n");

				// component is service
			} else if (hasComponentType(comp, "InboundPort")) {
				objectsStr.append("\t\t" + comp.getName() + " - service\n");
			}
		}

		// generate predicates for deployment
		for (Component involveComp : involvedComponents) {
			System.out.println("	involve comp: " + involveComp.getName());

			Component scomp = this.findComponent(sourceModel, involveComp.getName());
			Component tcomp = this.findComponent(targetModel, involveComp.getName());
			// check if the deployment is changed.

			if (scomp != null && tcomp != null) {
				System.out.println("		====found in both models");

				// init the component in the existing environment
				String senvitest = tcomp.getDeploymentnode().getName();
				if (!scomp.getDeploymentnode().getName().equals(tcomp.getDeploymentnode().getName())) {
					senvitest = "Test" + tcomp.getDeploymentnode().getName();
					goalStr.append("\t\t(in " + tcomp.getName() + " " + tcomp.getDeploymentnode().getName() + ")\n");
					// init the container for revised source on test
					initStr.append("\t\t(in " + scomp.getName() + " " + senvitest + ")\n");

					// object definition for environment
					objectsStr.append("\t\t" + senvitest + " - " + "testsandbox" + "\n");

					// init for test environment for revised component
					initStr.append("\t\t(at " + senvitest + " " + "TestSys" + ")\n");

					if (getEnvironementObjectLabel(tcomp).equals("container"))
						initStr.append("\t\t(init " + tcomp.getDeploymentnode().getName() + ")\n");
				} else {
					// deployment does not change
					initStr.append("\t\t(in " + scomp.getName() + " " + scomp.getDeploymentnode().getName() + ")\n");

					// object definition for environment
					objectsStr.append("\t\t" + scomp.getDeploymentnode().getName() + " - "
							+ getEnvironementObjectLabel(scomp) + "\n");

					// init for same environment for component
					Device sdevice = findDeviceByEnvironment(sourceModel, scomp.getDeploymentnode());
					initStr.append("\t\t(at " + scomp.getDeploymentnode().getName() + " " + sdevice.getName() + ")\n");
				}

				// object definition for target environment
				objectsStr.append("\t\t" + tcomp.getDeploymentnode().getName() + " - "
						+ getEnvironementObjectLabel(tcomp) + "\n");

				Device tdevice = findDeviceByEnvironment(targetModel, tcomp.getDeploymentnode());
				if (tdevice != null) {
					if (objectsStr.indexOf(tdevice.getName()) == -1) {
						objectsStr.append("\t\t" + tdevice.getName() + " - " + getDeviceObjectLabel(tdevice) + "\n");
					}

					initStr.append("\t\t(at " + tcomp.getDeploymentnode().getName() + " " + tdevice.getName() + ")\n");

				}
				// }
			}
			// new added component
			else if (scomp == null) {
				// init the component in the test bed
				String testEnvi = "Test" + tcomp.getDeploymentnode().getName();
				initStr.append("\t\t(in " + tcomp.getName() + " " + testEnvi + ")\n");

				// goal the component in the target environment
				goalStr.append("\t\t(in " + tcomp.getName() + " " + tcomp.getDeploymentnode().getName() + ")\n");

				// init the container
				if (getEnvironementObjectLabel(tcomp).equals("container"))
					initStr.append("\t\t(init " + tcomp.getDeploymentnode().getName() + ")\n");

				// object definition and init for production environment
				Device tdevice = findDeviceByEnvironment(targetModel, tcomp.getDeploymentnode());
				if (tdevice != null) {
					if (objectsStr.indexOf(tcomp.getDeploymentnode().getName()) == -1) {
						objectsStr.append("\t\t" + tcomp.getDeploymentnode().getName() + " - "
								+ getEnvironementObjectLabel(tcomp) + "\n");
					}
					if (objectsStr.indexOf(tdevice.getName()) == -1) {
						objectsStr.append("\t\t" + tdevice.getName() + " - " + getDeviceObjectLabel(tdevice) + "\n");
					}
					initStr.append("\t\t(at " + tcomp.getDeploymentnode().getName() + " " + tdevice.getName() + ")\n");
				}

				objectsStr.append("\t\t" + testEnvi + " - testsandbox\n");
				initStr.append("\t\t(at " + testEnvi + " TestSys)\n");

			}
		}

		// add test system
		objectsStr.append("\t\tTestSys - prvserver\n");

		StringBuffer problemStr = new StringBuffer();
		problemStr.append("(define (problem pb_" + sourceModel.getName() + ")\n");
		problemStr.append("\t(:domain migration)\n");
		problemStr.append("\t(:objects\n");
		problemStr.append(objectsStr.toString());
		problemStr.append("\t)\n");
		problemStr.append("\t(:init\n");
		problemStr.append(initStr.toString());
		problemStr.append("\t)\n");
		problemStr.append("\t(:goal (and \n");
		problemStr.append(goalStr.toString());
		problemStr.append("\t))\n)");

		return problemStr.toString();
	}

	private Component findComponent(DesignModel model, String componentName) {
		for (Component comp : model.getComponent()) {
			if (componentName.equalsIgnoreCase(comp.getName())) {
				return comp;
			}
		}
		return null;
	}

	private String getDeviceObjectLabel(Device sdevice) {
		if (sdevice.getNetAccessType() == NetAccessType.PUBLIC) {
			if (sdevice.getHostType() == HostType.CLOUD_PLATFORM)
				return "cloud";
			else
				return "pubserver";
		} else {
			return "prvserver";
		}

	}

	private Device findDeviceByEnvironment(DesignModel model, ExecutionEnvironment envi) {
		for (Device device : model.getHost()) {
			if (device.getNode().contains(envi))
				return device;
		}
		return null;
	}

	private String getEnvironementObjectLabel(Component comp) {
		if (comp.getType().indexOf("datastore") != -1)
			return "database";
		if (comp.getDeploymentnode().getType() == NodeType.APPLICATION_CONTAINER
				|| comp.getDeploymentnode().getType() == NodeType.DOCKER_CONTAINER)
			return "container";
		if (comp.getDeploymentnode().getType() == NodeType.FILE_SYSTEM)
			return "os";
		return null;
	}

	List<Connector> addedConnectorList = new ArrayList<Connector>();
	List<Connector> updatedConnectorList = new ArrayList<Connector>();

	private void findAddedUpdatedConnector() {

		for (Connector tconn : targetModel.getConnector()) {
			boolean foundinSourceModel = false;
			for (Connector sconn : sourceModel.getConnector()) {
				if (tconn.getName().equals(sconn.getName())) {
					foundinSourceModel = true;

					// determine whether connector type is changed
					if (!tconn.getType().equals(sconn.getType())) {
						updatedConnectorList.add(tconn);

						// determine whether the component linkage is changed
					} else {
						for (Role trole : tconn.getRole()) {
							List<Component> attachedCompInTarget = findComponentByConnectorRoleName(targetModel,
									tconn.getName(), trole.getName());
							List<Component> attachedCompInSource = findComponentByConnectorRoleName(sourceModel,
									tconn.getName(), trole.getName());

							// compare two list of component
							if (!isSameComponentSet(attachedCompInTarget, attachedCompInSource)) {
								updatedConnectorList.add(tconn);
							}
						}
					}

					break;
				}

			}
			if (!foundinSourceModel) {
				addedConnectorList.add(tconn);
			}
		}
	}

	private List<Component> findComponentByConnectorRoleName(DesignModel model, String connName, String roleName) {
		List<Component> result = new ArrayList<Component>();
		for (Component comp : model.getComponent()) {
			for (Port port : comp.getPort()) {
				for (Role role : port.getRole()) {
					if (roleName.equalsIgnoreCase(role.getName())
							&& connName.equalsIgnoreCase(role.getConnector().getName())) {
						result.add(comp);
					}
				}
			}
		}
		return result;
	}

	// return out-comp -> list of in-comp
	private List<ComponentLinkage> findComponentLinkage(DesignModel model, String connectorName) {
		List<ComponentLinkage> result = new ArrayList<ComponentLinkage>();
		List<Role> inRoles = new ArrayList<Role>();
		List<Role> outRoles = new ArrayList<Role>();
		Connector conn = null;
		// find connector in the model
		for (Connector econn : model.getConnector()) {
			if (econn.getName().equalsIgnoreCase(connectorName)) {
				conn = econn;
			}
		}

		if (conn != null) {
			for (Role rle : conn.getRole()) {
				if (rle.getType()!=null && rle.getType().indexOf("in") != -1)
					inRoles.add(rle);
				if (rle.getType()!=null && rle.getType().indexOf("out") != -1)
					outRoles.add(rle);
			}

			for (Role outRole : outRoles) {
				List<Component> outComps = this.findComponentByConnectorRoleName(model, conn.getName(),
						outRole.getName());
				for (Role inRole : inRoles) {
					List<Component> inComps = this.findComponentByConnectorRoleName(model, conn.getName(),
							inRole.getName());
					for (Component outComp : outComps) {
						if (connectorName.equals("emupwire")) {
							System.out.println("###\t\t" + outComp.getName());
						}
						for (Component inComp : inComps) {
							if (connectorName.equals("emupwire")) {
								System.out.println("->\t\t" + inComp.getName());
							}
							result.add(new ComponentLinkage(conn, outComp, inComp));
						}
					}
				}
			}
		}

		return result;
	}

	private boolean hasComponentType(Component comp, String type) {
		for (Port port : comp.getPort()) {
			if (port.getType().indexOf(type) != -1)
				return true;
		}
		return false;
	}

	private boolean isSameComponentSet(List<Component> list1, List<Component> list2) {
		for (Component comp1 : list1) {
			boolean isfound = false;
			for (Component comp2 : list2) {
				if (comp1.getName().equalsIgnoreCase(comp2.getName()))
					isfound = true;
			}
			if (!isfound)
				return false;
		}

		for (Component comp2 : list2) {
			boolean isfound = false;
			for (Component comp1 : list1) {
				if (comp2.getName().equalsIgnoreCase(comp1.getName()))
					isfound = true;
			}
			if (!isfound)
				return false;
		}

		return true;
	}

}
