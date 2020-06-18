package nz.auckland.arch.service.owl.factory;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;

import nz.auckland.arch.Component;
import nz.auckland.arch.Connector;
import nz.auckland.arch.DesignModel;
import nz.auckland.arch.Port;

public class OWLSmellChecker extends OWLChecker {

	public OWLSmellChecker(DesignModel model) {
		super(model);
	}

	@Override
	public void check() {
		
		//preprocess structure
		parseLogicalView();
		precheckStructure();
		
		// save for debugging
		saveToFile();
		
		try {
			// set invalid for ports that are UnusedInterface
			OWLClass uiClass = factory.getOWLClass(IRI.create(ontologyIRI + "#UnusedInterface"));
			// use reasoner to query class
			NodeSet<OWLNamedIndividual> resultUIIndvList = reasoner.getInstances(uiClass, false);
			for (Node<OWLNamedIndividual> node : resultUIIndvList) {
				String portName = node.getRepresentativeElement().getIRI().getShortForm();
				if (portInstHash.containsKey(portName)) {
					Port port = portInstHash.get(portName);
					port.setValid(false);
					port.setType((port.getType() == null ? "" : port.getType() + ",") + "UnusedInterface");
				}
			}
			// set invalid for components that are LavaFlow
			OWLClass lfClass = factory.getOWLClass(IRI.create(ontologyIRI + "#LavaFlow"));
			NodeSet<OWLNamedIndividual> resultLFIndvList = reasoner.getInstances(lfClass, false);
			for (Node<OWLNamedIndividual> node : resultLFIndvList) {
				String compName = node.getRepresentativeElement().getIRI().getShortForm();
				if (compInstHash.containsKey(compName)) {
					Component comp = compInstHash.get(compName);
					comp.setValid(false);
					comp.setType((comp.getType() == null ? "" : comp.getType() + ",") + "LavaFlow");
				}
			}
			// set invalid for components that are LinkOverload
			OWLClass loClass = factory.getOWLClass(IRI.create(ontologyIRI + "#LinkOverload"));
			NodeSet<OWLNamedIndividual> resultLOIndvList = reasoner.getInstances(loClass, false);
			for (Node<OWLNamedIndividual> node : resultLOIndvList) {
				String compName = node.getRepresentativeElement().getIRI().getShortForm();
				if (compInstHash.containsKey(compName)) {
					Component comp = compInstHash.get(compName);
					comp.setValid(false);
					comp.setType((comp.getType() == null ? "" : comp.getType() + ",") + "LinkOverload");
				}
			}
			// set invalid for connectos that are EA
			OWLClass eaClass = factory.getOWLClass(IRI.create(ontologyIRI + "#EAConnector"));
			NodeSet<OWLNamedIndividual> resultEAIndvList = reasoner.getInstances(eaClass, false);
			System.out.println("### found EAConnector count: " + resultEAIndvList.toString());
			for (Node<OWLNamedIndividual> node : resultEAIndvList) {
				String connName = node.getRepresentativeElement().getIRI().getShortForm();
				// System.out.println("\t connObj"+conn);
				if (connInstHash.containsKey(connName)) {

					Connector conn = connInstHash.get(connName);
					System.out.println("\t connObj" + conn);
					conn.setValid(false);
					conn.setType((conn.getType() == null ? "" : conn.getType() + ",") + "EAConnector");
				}
			} 
		} finally {
			dispose();
		}
	}

}
