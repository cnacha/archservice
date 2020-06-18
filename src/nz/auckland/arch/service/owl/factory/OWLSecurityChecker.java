package nz.auckland.arch.service.owl.factory;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;

import nz.auckland.arch.Component;
import nz.auckland.arch.Connector;
import nz.auckland.arch.DesignModel;

public class OWLSecurityChecker extends OWLChecker {

	public OWLSecurityChecker(DesignModel model) {
		super(model);
	}

	@Override
	public void check() {

		try {
			// preprocess
			long startTime = System.currentTimeMillis();
			parseLogicalView();
			long estimatedTime = System.currentTimeMillis() - startTime;
			System.out.println("========================= Parsing Logical View Elapse time"+estimatedTime);
			
			startTime = System.currentTimeMillis();
			parsePhysicalView();
			estimatedTime = System.currentTimeMillis() - startTime;
			System.out.println("========================= Parsing Physical View Elapse time"+estimatedTime);
			
		
			util.markAllIndividualDifferent();
			
			startTime = System.currentTimeMillis();
			precheckStructure();
			estimatedTime = System.currentTimeMillis() - startTime;
			System.out.println("========================= check Structure Elapse time"+estimatedTime);

			// save for debugging
			saveToFile();
			
			startTime = System.currentTimeMillis();
			// set type for attack surface
			setTypeForSecurityCharacter("AttackSurface");
			setTypeForSecurityCharacter("DefenseInDepth");
			setTypeForSecurityCharacter("LeastPriviledge");
			setTypeForSecurityCharacter("Compartmentalization");
			setTypeForSecurityCharacter("DenialOfService");
			setTypeForSecurityCharacter("DataTampering");
			
			setTypeForSecurityCharacter("ManInMiddleConnector");
			setTypeForSecurityCharacter("DataTamperingConnector");
			setTypeForSecurityCharacter("DenialOfServiceConnector");
			
			
			estimatedTime = System.currentTimeMillis() - startTime;
			System.out.println("========================= Type Setting Elapse time"+estimatedTime);
			
			// set link 
			System.out.println("========= checking connector & communication link "+connIndvHash.size());
			for(Map.Entry<String, OWLNamedIndividual> entry: connIndvHash.entrySet()) {
				
				NodeSet<OWLNamedIndividual> result = reasoner.getObjectPropertyValues(entry.getValue(), factory.getOWLObjectProperty(IRI.create(ontologyIRI + "#hasLinkVia")));
				System.out.println(" checking link for "+entry.getKey()+" props:"+result.getFlattened().size());
				for(OWLNamedIndividual link :result.getFlattened()) {
						System.out.println("		"+link.getIRI().getShortForm());
						connInstHash.get(entry.getKey()).getCommunicationlink().add(linkInstHash.get(link.getIRI().getShortForm()));
				}
				
			}

		} finally {

			dispose();
		}

	}

	private void setTypeForSecurityCharacter(String character) {

		OWLClass insecurePort = factory.getOWLClass(IRI.create(ontologyIRI + "#"+character));
		// use reasoner to query class
		NodeSet<OWLNamedIndividual> resulComp = reasoner.getInstances(insecurePort, false);
		System.out.println("found " +character+ " " + resulComp.getNodes().size() );
		for (Node<OWLNamedIndividual> node : resulComp) {

			String elementName = node.getRepresentativeElement().getIRI().getShortForm();
			System.out.println("		"+elementName);

			if (compInstHash.containsKey(elementName)) {
				Component comp = compInstHash.get(elementName);
				if (comp.getSecurityCharacters() == null)
					comp.setSecurityCharacters("");
				compInstHash.get(elementName).setSecurityCharacters(comp.getSecurityCharacters() + ((comp.getSecurityCharacters().length()==0)?"":",") + character );
				
			} else if(connInstHash.containsKey(elementName)) {
				Connector conn = connInstHash.get(elementName);
				if (conn.getSecurityCharacters() == null)
					conn.setSecurityCharacters("");
				connInstHash.get(elementName).setSecurityCharacters(conn.getSecurityCharacters() + ((conn.getSecurityCharacters().length()==0)?"":",") + character );
				

			}
		}
	}

}
