package nz.auckland.arch.service.owl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLParser;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxOntologyParserFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import com.clarkparsia.owlapi.explanation.BlackBoxExplanation;
import com.clarkparsia.owlapi.explanation.HSTExplanationGenerator;

import nz.auckland.arch.service.owl.factory.OWLUtil;
import nz.auckland.arch.service.owl.utils.StringUtil;

public class Tester {

	public static void main(String[] args) {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		IRI ontologyIRI = IRI.create("http://www.auckland.ac.nz/ontologies/arch.owl");;

		OWLDataFactory factory = manager.getOWLDataFactory();

		try {
			OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File("web/config/arch.owl"));
			
			
			ReasonerFactory rf = new Reasoner.ReasonerFactory() {
				protected OWLReasoner createHermiTOWLReasoner(org.semanticweb.HermiT.Configuration configuration,
						OWLOntology o) {
					configuration.throwInconsistentOntologyException = false;
					return new Reasoner(configuration, o);
				}
			};
			Configuration configuration = new Configuration();
			OWLReasoner reasoner = rf.createReasoner(ontology, configuration);
			BlackBoxExplanation exp = new BlackBoxExplanation(ontology, rf, reasoner);
			HSTExplanationGenerator multExplanator = new HSTExplanationGenerator(exp);
			
			OWLUtil util= new OWLUtil(manager, factory, ontologyIRI, ontology, rf, multExplanator);
			
			// create a class
			OWLClass refClass = factory.getOWLClass(IRI.create(ontologyIRI + "#RefactorRule"));
			OWLClass ruleCls = util.createSubClass("RuleEventCentre", refClass);
			StringBuffer exprstr = new StringBuffer();
			exprstr.append("Connector and hasRole some InboundRole");
		
					
			System.out.println(exprstr.toString());
			
			// call to add class expression to the class
			util.addClassExpression(ruleCls, exprstr.toString());
			
			// save file
			FileOutputStream fout = new FileOutputStream(new File("web/config/arch.owl"));
			manager.saveOntology(ontology, fout);


			System.out.println("Testing complete....");
		
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
