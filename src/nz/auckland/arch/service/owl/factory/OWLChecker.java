package nz.auckland.arch.service.owl.factory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import com.clarkparsia.owlapi.explanation.BlackBoxExplanation;
import com.clarkparsia.owlapi.explanation.HSTExplanationGenerator;

import nz.auckland.arch.ArchStyle;
import nz.auckland.arch.Channel;
import nz.auckland.arch.CommunicationLink;
import nz.auckland.arch.CommunicationPort;
import nz.auckland.arch.Component;
import nz.auckland.arch.ComponentType;
import nz.auckland.arch.Connector;
import nz.auckland.arch.ConnectorType;
import nz.auckland.arch.CriticalLevel;
import nz.auckland.arch.DesignModel;
import nz.auckland.arch.Device;
import nz.auckland.arch.Event;
import nz.auckland.arch.ExecutionEnvironment;
import nz.auckland.arch.HostType;
import nz.auckland.arch.LinkType;
import nz.auckland.arch.NetworkType;
import nz.auckland.arch.NodeType;
import nz.auckland.arch.OntologyLabel;
import nz.auckland.arch.Port;
import nz.auckland.arch.PortType;
import nz.auckland.arch.Role;
import nz.auckland.arch.RoleType;
import nz.auckland.arch.service.owl.utils.StringUtil;

public abstract class OWLChecker {

	private static final String DRIVE_LABEL = "D";

	private final String TEMPLATE_URL = DRIVE_LABEL + ":/config/";
	private static String OUTPUT_FILE_URL = DRIVE_LABEL + ":/config/arch";
	
	private String owlLibrary;

	DesignModel model;
	IRI ontologyIRI;
	OWLDataFactory factory;
	OWLReasoner reasoner;
	ReasonerFactory rf;
	Hashtable<String, Component> compInstHash = new Hashtable<String, Component>();
	Hashtable<String, OWLIndividual> compIndvHash = new Hashtable<String, OWLIndividual>();
	Hashtable<String, Connector> connInstHash = new Hashtable<String, Connector>();
	Hashtable<String, Port> portInstHash = new Hashtable<String, Port>();
	Hashtable<String, OWLNamedIndividual> connIndvHash = new Hashtable<String, OWLNamedIndividual>();
	Hashtable<String, OWLIndividual> portIndvHash = new Hashtable<String, OWLIndividual>();
	Hashtable<Role, OWLIndividual> roleIndvHash = new Hashtable<Role, OWLIndividual>();
	Hashtable<OWLIndividual, Role> roleInstHash = new Hashtable<OWLIndividual, Role>();
	Hashtable<String, CommunicationLink> linkInstHash = new Hashtable<String, CommunicationLink>();
	List<OWLClass> connClassList = new ArrayList<OWLClass>();
	List<OWLClass> compClassList = new ArrayList<OWLClass>();
	List<OWLClass> portClassList = new ArrayList<OWLClass>();
	List<OWLClass> inferLabelClassList = new ArrayList<OWLClass>(); // store class to be inferred
	List<OWLClass> forcedLabelClassList = new ArrayList<OWLClass>(); // store class to be enforced
	OWLOntology ontology = null;
	OWLOntologyManager manager;
	Configuration configuration;
	OWLUtil util;
	
	public void setTemplate(String name) {
		owlLibrary = TEMPLATE_URL + name;
	}

	public OWLChecker(DesignModel model) {
		super();
		this.model = model;
	}

	protected void parseLogicalView() {
		manager = OWLManager.createOWLOntologyManager();
		// load arch template
		ontologyIRI = IRI.create("http://www.auckland.ac.nz/ontologies/arch.owl");

		factory = manager.getOWLDataFactory();
		// call reasoner
		rf = new ReasonerFactory();

		try {

			// load ontology from template file
			ontology = manager.loadOntologyFromOntologyDocument(new File(owlLibrary));

			// setup reasoner and factory
			configuration = new Configuration();
			configuration.throwInconsistentOntologyException = false;
			reasoner = rf.createReasoner(ontology, configuration);
			rf = new Reasoner.ReasonerFactory() {
				protected OWLReasoner createHermiTOWLReasoner(org.semanticweb.HermiT.Configuration configuration,
						OWLOntology o) {
					configuration.throwInconsistentOntologyException = false;
					return new Reasoner(configuration, o);
				}
			};

			BlackBoxExplanation exp = new BlackBoxExplanation(ontology, rf, reasoner);
			HSTExplanationGenerator multExplanator = new HSTExplanationGenerator(exp);

			OWLClass connClass = factory.getOWLClass(IRI.create(ontologyIRI + "#Connector"));
			OWLClass compClass = factory.getOWLClass(IRI.create(ontologyIRI + "#Component"));
			OWLClass roleClass = factory.getOWLClass(IRI.create(ontologyIRI + "#Role"));
			OWLClass inRoleClass = factory.getOWLClass(IRI.create(ontologyIRI + "#InboundRole"));
			OWLClass outRoleClass = factory.getOWLClass(IRI.create(ontologyIRI + "#OutboundRole"));
			OWLClass portClass = factory.getOWLClass(IRI.create(ontologyIRI + "#Interface"));

			/********************************************/
			/**** create classes for architecture style ****/
			/********************************************/
			util = new OWLUtil(manager, factory, ontologyIRI, ontology, rf, multExplanator);
			Hashtable<String, OWLClass> roleClassHash = new Hashtable<String, OWLClass>();

			for (ArchStyle style : model.getArchstyle()) {

				Hashtable<String, OWLClass> portRoleHash = new Hashtable<String, OWLClass>();
				boolean isStyleValid = true;
				for (ConnectorType connTyp : style.getConnectortype()) {

					// create new class for connector type
					OWLClass newConnClass = util.createSubClass(connTyp.getName(), connClass);

					connClassList.add(newConnClass);
					HashSet<OWLClass> set = new HashSet<OWLClass>();

					for (RoleType roleTyp : connTyp.getRoletype()) {
						OWLClass setRoleCls = roleClass;

						// determine in/out role
						if (this.getInOutRole(roleTyp) == 'i')
							setRoleCls = inRoleClass;
						else if (this.getInOutRole(roleTyp) == 'o')
							setRoleCls = outRoleClass;

						// create new class for role type
						OWLClass newRoleClass = util.createSubClass(roleTyp.getName(), setRoleCls);

						set.add(newRoleClass);
						// record the port-role association to hash table
						if (roleTyp.getPorttype() != null) {
							portRoleHash.put(roleTyp.getPorttype().getName(), newRoleClass);
						}
						roleClassHash.put(roleTyp.getName(), newRoleClass);

						// check if new role class is satisfiable
						if (!util.isSatisfiable(newRoleClass)) {
							roleTyp.setValid(false);
							isStyleValid = false;
						} else {
							roleTyp.setValid(true);
						}
					}
					util.addClassIntersectSomeObjectProperties(newConnClass, set, "hasRole");

					// check if new connector class is satisfiable
					if (!util.isSatisfiable(newConnClass)) {
						connTyp.setValid(false);
						isStyleValid = false;
					} else {
						connTyp.setValid(true);
					}

					// generate SWRL rule for EAConnector
					if (connTyp.getEaConnector() != null) {
						util.createSWLRule(util.generateSWRLRuleForEA(connTyp, connTyp.getEaConnector(), false));
						util.createSWLRule(util.generateSWRLRuleForEA(connTyp, connTyp.getEaConnector(), true));
					}
				}

				for (ComponentType compTyp : style.getComponenttype()) {

					// create new class for component type
					OWLClass newCompClass = util.createSubClass(compTyp.getName(), compClass);

					compClassList.add(newCompClass);
					HashSet<OWLClass> portSet = new HashSet<OWLClass>();

					for (PortType portTyp : compTyp.getPorttype()) {

						// create new class for port type
						OWLClass newPortClass = util.createSubClass(portTyp.getName(), portClass);

						System.out.println("creating " + portTyp.getName());

						portClassList.add(newPortClass);
						portSet.add(newPortClass);
						if (portRoleHash.containsKey(portTyp.getName())) {
							System.out.println(portTyp.getName() + " attached to "
									+ portRoleHash.get(portTyp.getName()).getIRI().getShortForm());
							// create hasAttachment properties to port class
							util.addClassSomeObjectProperties(newPortClass, portRoleHash.get(portTyp.getName()),
									"hasAttachment");
						}

						// check if new class is satisfiable
						if (!util.isSatisfiable(newPortClass)) {
							portTyp.setValid(false);
							isStyleValid = false;
						} else {
							portTyp.setValid(true);
						}

					}

					util.addClassIntersectSomeObjectProperties(newCompClass, portSet, "hasPort");

					// check if new class is satisfiable
					if (!util.isSatisfiable(newCompClass)) {
						compTyp.setValid(false);
						isStyleValid = false;
					} else {
						compTyp.setValid(true);
					}

				}
				System.out.println("style " + style.getName() + " : " + isStyleValid);
				style.setValid(isStyleValid);

			}
			/********************************************/
			/**** create new class for ontology label ****/
			/********************************************/
			System.out.println("found ontology label: " + model.getOntologylabel().size());
			for (OntologyLabel ontLabel : model.getOntologylabel()) {
				// empty expression is the forced label class
				if (ontLabel.getExpression() == null || ontLabel.getExpression().length() == 0) {
					OWLClass superClass = factory.getOWLClass(IRI.create(ontologyIRI + "#" + ontLabel.getSubClassOf()));
					System.out.println("create class for label: " + ontLabel.getName());
					OWLClass labelClass = util.createSubClass(ontLabel.getName(), superClass);

					forcedLabelClassList.add(labelClass);
				}

			}
			for (OntologyLabel ontLabel : model.getOntologylabel()) {
				if (ontLabel.getExpression() != null && ontLabel.getExpression().length() > 0) {
					OWLClass superClass = factory.getOWLClass(IRI.create(ontologyIRI + "#" + ontLabel.getSubClassOf()));
					System.out.println("create class for label: " + ontLabel.getName());
					OWLClass labelClass = util.createSubClass(ontLabel.getName(), superClass);
					// non empty expression is a label to infer by reasoner
					util.addClassExpression(labelClass, ontLabel.getExpression());
					inferLabelClassList.add(labelClass);
				}

			}

			/********************************************/
			/**** create individuals for architecture design ****/
			/********************************************/
			for (Connector conn : model.getConnector()) {

				// create connector individual
				OWLNamedIndividual connIndv = util.createIndividual(conn.getName(), null);
				connInstHash.put(conn.getName(), conn);
				connIndvHash.put(conn.getName(), connIndv);
				// reset attributes
				conn.setValid(true);
				conn.setType(null);
				conn.setSecurityCharacters(null);

				for (Role rle : conn.getRole()) {

					// create role individual
					String rleIndvName = rle.getName() + "_" + StringUtil.randomNumber(4);
					OWLIndividual roleIndv = util.createIndividual(rleIndvName, roleClassHash.get(rle.getName()));
					rle.setName(rleIndvName);
//					if (rle.getType()!=null && rle.getType().indexOf(",") != -1) {
//						rle.setType(rle.getType().substring(0, rle.getType().indexOf(",")));
//					}
					rle.setType("");

					util.addObjectProperties(connIndv, roleIndv, "hasRole");
					// put in hash for recording
					roleIndvHash.put(rle, roleIndv);
					roleInstHash.put(roleIndv, rle);
				}
			}

			for (Component comp : model.getComponent()) {
				// reset type
				comp.setType(null);
				comp.setSecurityCharacters(null);
				comp.setValid(true);
				// create component individual
				OWLIndividual compIndv = util.createIndividual(comp.getName(), null);
				compInstHash.put(comp.getName(), comp);
				compIndvHash.put(comp.getName(), compIndv);

				// set critical value
				if (comp.getCriticalLevel() != null) {
					util.addDataProperty(compIndv, "hasCriticalValue", comp.getCriticalLevel().getValue());
				}

				HashSet<Component> dependencyComp = new HashSet<Component>();
				for (Port port : comp.getPort()) {

					// create port individual
					OWLIndividual portIndv = util.createIndividual(port.getName(), null);
					portInstHash.put(port.getName(), port);
					portIndvHash.put(port.getName(), portIndv);
					util.addObjectProperties(compIndv, portIndv, "hasPort");

					setClassforForcedLabelToPort(port, portIndv);

					// reset type
					port.setType(null);
					port.setValid(true);

					HashSet<Port> portLinkage = new HashSet<Port>();
					// loop through attached role
					for (Role rle : port.getRole()) {

						System.out.println(" roleName:" + rle.getName());
						if (roleIndvHash.containsKey(rle)) {
							util.addObjectProperties(portIndv, roleIndvHash.get(rle), "hasAttachment");
						}

						// find dependency

						for (Connector conn : model.getConnector()) {
							if (conn.getRole().contains(rle)) {
								// found connector
								System.out.println("\tfound conn " + conn);
								for (Role ascrole : conn.getRole()) {
									if (ascrole != rle) {
										// found attached role
										System.out.println("\tfound associated role " + ascrole);
										for (Component ccomp : model.getComponent()) {
											for (Port cport : ccomp.getPort()) {

												if (cport.getRole().contains(ascrole)) {
													System.out.println("\tfound related port " + cport);
													dependencyComp.add(ccomp);
													portLinkage.add(cport);
												}
											}
										}
									}
								}
							}
						} // end find dependency
					} // end loop through attached role
					util.addDataProperty(portIndv, "hasLinkages", portLinkage.size());
				} // end loop through port

				// insert data property
				util.addDataProperty(compIndv, "hasDependencies", dependencyComp.size());
			}

		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InconsistentOntologyException e) {
			System.out.println("inconsistent model...");
			e.printStackTrace();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void setClassforForcedLabelToPort(Port port, OWLIndividual indv) {
		for (OWLClass cls : this.forcedLabelClassList) {
			if (port.getType()!=null && port.getType().indexOf(cls.getIRI().getShortForm()) != -1) {
				util.addClassAssertion(indv, cls);
			}
		}
	}

	protected void precheckStructure() {
		// invoke reasoner
		try {
			reasoner = rf.createReasoner(ontology, configuration);
			System.out.println("Model is consistent? " + reasoner.isConsistent());
			if (!reasoner.isConsistent()) {
				// find the explanation where the model is not consistent

				// Set<Set<OWLAxiom>>
				// explanations=multExplanator.getExplanations(factory.getOWLThing(),5);
				// // loop through expalnation
				// for (Set<OWLAxiom> explanation : explanations) {
				// System.out.println("------------------");
				// System.out.println("Axioms causing the inconsistency: ");
				// // find problematic axiom
				// for (OWLAxiom causingAxiom : explanation) {
				//
				// if(causingAxiom instanceof OWLSubClassOfAxiom) {
				// OWLClass cls = (OWLClass)((OWLSubClassOfAxiom)causingAxiom).getSubClass();
				// System.out.println("problematic class: "+cls.getIRI().getShortForm());
				// }
				// System.out.println(causingAxiom);
				// }
				// System.out.println("------------------");
				// }

			} else {

				/********************************************/
				/**** Model is consistent, set all types ****/
				/********************************************/

				System.out.println("computing class .....");
				rf = new ReasonerFactory();
				reasoner = rf.createReasoner(ontology);
				reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
				reasoner.precomputeInferences(InferenceType.CLASS_ASSERTIONS);

				// check & set type for connector
				List<OWLClass> connCheckList = new ArrayList<OWLClass>();
				connCheckList.addAll(connClassList);
				connCheckList.addAll(inferLabelClassList);
				for (OWLClass connCls : connCheckList) {

					// query individual by connector class
					NodeSet<OWLNamedIndividual> resultIndvList = reasoner.getInstances(connCls, false);

					// loop through connector individual
					for (Node<OWLNamedIndividual> node : resultIndvList) {
						String connName = node.getRepresentativeElement().getIRI().getShortForm();
						if (connInstHash.containsKey(connName)) {
							System.out.println("set conn type for " + connName + " " + connCls.getIRI().getShortForm());
							Connector conn = connInstHash.get(connName);
							// compInstHash.remove(compName);
							// set type to inferred type of OWL individual
							String typeToSet = connCls.getIRI().getShortForm();
							if (conn.getType() != null && conn.getType().length() != 0) {
								if (conn.getType().indexOf(typeToSet) == -1)
									conn.setType(conn.getType() + "," + typeToSet);
							} else
								conn.setType(connCls.getIRI().getShortForm());

						}
					}

				}

				// check & set inbound type for role
				OWLClass inRoleClass = factory.getOWLClass(IRI.create(ontologyIRI + "#InboundRole"));

				NodeSet<OWLNamedIndividual> inroleIndvList = reasoner.getInstances(inRoleClass, false);
				for (OWLIndividual indv : inroleIndvList.getFlattened()) {
					if (this.roleInstHash.containsKey(indv)) {
						Role rle = this.roleInstHash.get(indv);
						rle.setType(rle.getType() + "in");
					}

				}
				// check & set outbound type for role
				OWLClass outRoleClass = factory.getOWLClass(IRI.create(ontologyIRI + "#OutboundRole"));
				NodeSet<OWLNamedIndividual> outroleIndvList = reasoner.getInstances(outRoleClass, false);
				for (OWLIndividual indv : outroleIndvList.getFlattened()) {
					if (this.roleInstHash.containsKey(indv)) {
						Role rle = this.roleInstHash.get(indv);
						rle.setType(rle.getType() + "out");
					}

				}

				// check & set type for component
				List<OWLClass> compCheckList = new ArrayList<OWLClass>();
				compCheckList.addAll(compClassList);
				compCheckList.addAll(inferLabelClassList);
				for (OWLClass compCls : compCheckList) {

					// query individual by component class
					NodeSet<OWLNamedIndividual> resultIndvList = reasoner.getInstances(compCls, false);

					// loop through component individual
					for (Node<OWLNamedIndividual> node : resultIndvList) {
						String compName = node.getRepresentativeElement().getIRI().getShortForm();
						if (compInstHash.containsKey(compName)) {
							System.out.println("set comp type for " + compName + " " + compCls.getIRI().getShortForm());
							Component comp = compInstHash.get(compName);
							// compInstHash.remove(compName);
							// set type to inferred type of OWL individual
							String typeToSet = compCls.getIRI().getShortForm();
							if (comp.getType() != null && comp.getType().length() != 0) {
								if (comp.getType().indexOf(typeToSet) == -1)
									comp.setType(comp.getType() + "," + compCls.getIRI().getShortForm());
							} else
								comp.setType(compCls.getIRI().getShortForm());

						}
					}

				}

				// add in/out bound port to set type
				portClassList.add(factory.getOWLClass(IRI.create(ontologyIRI + "#OutboundPort")));
				portClassList.add(factory.getOWLClass(IRI.create(ontologyIRI + "#InboundPort")));

				portClassList.addAll(forcedLabelClassList);

				// check & set type for port
				for (OWLClass portCls : portClassList) {

					// query individual by port class
					NodeSet<OWLNamedIndividual> resultIndvList = reasoner.getInstances(portCls, false);

					// loop through port individual
					for (Node<OWLNamedIndividual> node : resultIndvList) {
						String portName = node.getRepresentativeElement().getIRI().getShortForm();
						if (portInstHash.containsKey(portName)) {
							System.out.println("set port type for " + portName + " " + portCls.getIRI().getShortForm());
							Port port = portInstHash.get(portName);
							// portInstHash.remove(portName);
							if (port.getType() != null && port.getType().length() != 0) {
								String typeToSet = portCls.getIRI().getShortForm();
								if (port.getType().indexOf(typeToSet) == -1)
									port.setType(port.getType() + "," + typeToSet);
							}else {
								port.setType(portCls.getIRI().getShortForm());
							}
						}

					}

				}

			}

			// set invalid to component without type inferred
			// for(String name : compInstHash.keySet()) {
			// System.out.println("set comp "+ name +" invalid");
			// if(compInstHash.get(name).getType()==null ||
			// compInstHash.get(name).getType().equals(""))
			// compInstHash.get(name).setValid(false);
			// else
			// compInstHash.get(name).setValid(true);
			// }
			//
			// // set invalid to port without type inferred
			// for(String name : portInstHash.keySet()) {
			// System.out.println("set port "+ name +"
			// #"+portInstHash.get(name).getType()+"#");
			// if(portInstHash.get(name).getType() ==null ||
			// portInstHash.get(name).getType().equals(""))
			// portInstHash.get(name).setValid(false);
			// else
			// portInstHash.get(name).setValid(true);
			// }
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void parsePhysicalView() {
		// create individuals for deployment host
		OWLClass hostClass = factory.getOWLClass(IRI.create(ontologyIRI + "#Device"));
		OWLClass cloudPlatformClass = factory.getOWLClass(IRI.create(ontologyIRI + "#CloudPlatform"));
		OWLClass dedicatedServerClass = factory.getOWLClass(IRI.create(ontologyIRI + "#DedicatedServer"));
		OWLClass virtualmachineClass = factory.getOWLClass(IRI.create(ontologyIRI + "#VirtualMachine"));

		OWLClass appContainerClass = factory.getOWLClass(IRI.create(ontologyIRI + "#ApplicationContainer"));
		OWLClass dockerContainerClass = factory.getOWLClass(IRI.create(ontologyIRI + "#DockerContainer"));
		OWLClass osContainerClass = factory.getOWLClass(IRI.create(ontologyIRI + "#OperatingSystem"));

		OWLClass nodeClass = factory.getOWLClass(IRI.create(ontologyIRI + "#ExecutionEnvironment"));
		OWLClass linkClass = factory.getOWLClass(IRI.create(ontologyIRI + "#CommunicationLink"));
		OWLClass internetLinkClass = factory.getOWLClass(IRI.create(ontologyIRI + "#InternetLink"));
		OWLClass intranetLinkClass = factory.getOWLClass(IRI.create(ontologyIRI + "#IntranetLink"));
		OWLClass httpLinkClass = factory.getOWLClass(IRI.create(ontologyIRI + "#HTTPLink"));
		OWLClass httpsLinkClass = factory.getOWLClass(IRI.create(ontologyIRI + "#HTTPSLink"));
		OWLClass ftpLinkClass = factory.getOWLClass(IRI.create(ontologyIRI + "#FTPLink"));
		OWLClass ftpsLinkClass = factory.getOWLClass(IRI.create(ontologyIRI + "#FTPSLink"));
		OWLClass incommPortClass = factory.getOWLClass(IRI.create(ontologyIRI + "#InboundCommPort"));
		OWLClass outcommPortClass = factory.getOWLClass(IRI.create(ontologyIRI + "#OutboundCommPort"));
		for (Device host : model.getHost()) {
			System.out.println("create device " + host.getName());
			OWLIndividual hostIndv = null;
			if (host.getHostType() == HostType.CLOUD_PLATFORM) {
				hostIndv = util.createIndividual(host.getName(), cloudPlatformClass);
			} else if (host.getHostType() == HostType.DEDICATED_SERVER) {
				hostIndv = util.createIndividual(host.getName(), dedicatedServerClass);
			} else if (host.getHostType() == HostType.VIRTUAL_MACHINE) {
				hostIndv = util.createIndividual(host.getName(), virtualmachineClass);
			} else {
				hostIndv = util.createIndividual(host.getName(), hostClass);
			}
			util.addDataProperty(hostIndv, "hasNetAccessType", host.getNetAccessType().getValue());

			// create individuals for deployment node
			for (ExecutionEnvironment node : host.getNode()) {
				System.out.println("	create execution environment " + node.getName());
				OWLIndividual nodeIndv = null;
				if (node.getType() == NodeType.APPLICATION_CONTAINER) {
					nodeIndv = util.createIndividual(node.getName(), appContainerClass);
				} else if (node.getType() == NodeType.DOCKER_CONTAINER) {
					nodeIndv = util.createIndividual(node.getName(), dockerContainerClass);
				} else if (node.getType() == NodeType.FILE_SYSTEM) {
					nodeIndv = util.createIndividual(node.getName(), osContainerClass);
				} else
					nodeIndv = util.createIndividual(node.getName(), nodeClass);

				util.addObjectProperties(nodeIndv, hostIndv, "isNodeOf");

				// trace through component deployed on this node
				for (Component comp : node.getComponent()) {
					util.addObjectProperties(nodeIndv, compIndvHash.get(comp.getName()), "hasDeployment");
				}
			}
		}
		// create individuals for communication link
		for (CommunicationLink link : model.getLink()) {
			linkInstHash.put(link.getName(), link);
			OWLIndividual linkIndv = null;
			if (link.getLinkType() == LinkType.HTTP) {
				linkIndv = util.createIndividual(link.getName(), httpLinkClass);
			} else if (link.getLinkType() == LinkType.HTTPS) {
				linkIndv = util.createIndividual(link.getName(), httpsLinkClass);
			} else if (link.getLinkType() == LinkType.FTP) {
				linkIndv = util.createIndividual(link.getName(), ftpLinkClass);
			} else if (link.getLinkType() == LinkType.FTPS) {
				linkIndv = util.createIndividual(link.getName(), ftpsLinkClass);
			} else
				linkIndv = util.createIndividual(link.getName(), linkClass);

			if (link.getNetworkType() == NetworkType.INTERNET) {
				util.createIndividual(link.getName(), internetLinkClass);

			} else if (link.getNetworkType() == NetworkType.INTRANET) {
				util.createIndividual(link.getName(), intranetLinkClass);
			}

			if (link.getSource() != null) {
				OWLIndividual commPortindv = util.createIndividual(link.getSource().getName() + "-"
						+ StringUtil.randomNumber(3) + "-" + link.getSource().getPortNumber(), outcommPortClass);
				util.addObjectProperties(linkIndv, commPortindv, "hasCommPort");
				setSecurityControlToPortIndv(link.getSource(), commPortindv);
				for (Port port : link.getSource().getNamedport()) {
					util.addObjectProperties(commPortindv, portIndvHash.get(port.getName()), "hasBind");
				}
			}
			if (link.getTarget() != null) {
				OWLIndividual commPortindv = util.createIndividual(link.getSource().getName() + "-"
						+ StringUtil.randomNumber(3) + "-" + link.getTarget().getPortNumber(), incommPortClass);
				util.addObjectProperties(linkIndv, commPortindv, "hasCommPort");
				setSecurityControlToPortIndv(link.getTarget(), commPortindv);
				for (Port port : link.getTarget().getNamedport()) {
					util.addObjectProperties(commPortindv, portIndvHash.get(port.getName()), "hasBind");
				}
			}
		}

	}

	private char getInOutRole(RoleType roleTyp) {

		for (Event evt : roleTyp.getEvent()) {
			if (evt instanceof Channel) {
				if (((Channel) evt).getType().equals("input"))
					return 'i';
				if (((Channel) evt).getType().equals("output"))
					return 'o';
			}
		}
		return ' ';

	}

	private void setSecurityControlToPortIndv(CommunicationPort depPort, OWLIndividual indv) {
		if (depPort.isHasFirewall()) {
			util.addDataProperty(indv, "hasFirewall", 1);
		} else
			util.addDataProperty(indv, "hasFirewall", 0);

		if (depPort.isHasAuthentication()) {
			util.addDataProperty(indv, "hasAuthentication", 1);
		} else
			util.addDataProperty(indv, "hasAuthentication", 0);

		if (depPort.isHasAuthorization()) {
			util.addDataProperty(indv, "hasAuthorization", 1);
		} else
			util.addDataProperty(indv, "hasAuthorization", 0);

		if (depPort.isHasInputSanitization()) {
			util.addDataProperty(indv, "hasInputSanitization", 1);
		} else
			util.addDataProperty(indv, "hasInputSanitization", 0);
	}

	public void saveToFile() {
		// save ontology to file for debugging
		FileOutputStream fout = null;
		try {
			fout = new FileOutputStream(new File(OUTPUT_FILE_URL + StringUtil.randomNumber(5) + ".owl"));
			manager.saveOntology(ontology, fout);
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (fout != null) {
				try {
					fout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public abstract void check();

	public void dispose() {
		reasoner.dispose();
	}
}
