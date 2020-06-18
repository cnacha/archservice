package nz.auckland.arch.service.owl.factory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxPrefixNameShortFormProvider;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.IRIShortFormProvider;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleIRIShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.swrlapi.core.SWRLRuleEngine;
import org.swrlapi.exceptions.SWRLBuiltInException;
import org.swrlapi.factory.SWRLAPIFactory;
import org.swrlapi.parser.SWRLParseException;

import com.clarkparsia.owlapi.explanation.HSTExplanationGenerator;

import nz.auckland.arch.ConnectorType;

public class OWLUtil {
	private OWLOntologyManager manager;
	private OWLDataFactory factory;
	private IRI ontologyIRI;
	private OWLOntology ontology;
	private ReasonerFactory reasonerFactory;
	private HSTExplanationGenerator multExplanator;
	

	public OWLUtil(OWLOntologyManager manager, OWLDataFactory factory, IRI ontologyIRI, OWLOntology ontology,ReasonerFactory reasonerf,HSTExplanationGenerator multExplanator) {
		super();
		this.manager = manager;
		this.factory = factory;
		this.ontologyIRI = ontologyIRI;
		this.ontology = ontology;
		this.reasonerFactory = reasonerf;
		this.multExplanator = multExplanator;
	}

	public OWLClass createSubClass(String name, OWLClass superclass) {
		OWLClass newCompClass = factory.getOWLClass(IRI.create(ontologyIRI + "#" + name));
		OWLAxiom axiom = factory.getOWLSubClassOfAxiom(newCompClass, superclass);
		AddAxiom addAxiom = new AddAxiom(ontology, axiom);
		manager.applyChange(addAxiom);
		
	
		return newCompClass;
	}
	
	
	
	public OWLNamedIndividual createIndividual(String name, OWLClass cls) {
		OWLNamedIndividual indv = factory.getOWLNamedIndividual(IRI.create(ontologyIRI + "#" + name));
		if(cls!=null) {
			// assert class to individual
			OWLClassAssertionAxiom classAssertion = factory.getOWLClassAssertionAxiom(cls, indv);
			 manager.addAxiom(ontology, classAssertion);
		}
		
		return indv;
	}
	
	public OWLIndividual addClassAssertion(OWLIndividual indv, OWLClass cls) {

		OWLClassAssertionAxiom classAssertion = factory.getOWLClassAssertionAxiom(cls, indv);
		manager.addAxiom(ontology, classAssertion);
		
		return indv;
	}
	
	public void markAllIndividualDifferent() {
		Set<OWLNamedIndividual> indvSet = ontology.getIndividualsInSignature();
		
		manager.addAxiom(ontology, factory.getOWLDifferentIndividualsAxiom(indvSet));
		
	}
	
	public OWLIndividual addDataProperty(OWLIndividual indv, String propName, int value) {
		OWLDataPropertyExpression hasDeps = factory.getOWLDataProperty(IRI.create(ontologyIRI + "#"+propName));
		
		OWLDatatype integerDatatype = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
		OWLLiteral literal = factory.getOWLLiteral(Integer.toString(value), integerDatatype);
		OWLAxiom ax = factory.getOWLDataPropertyAssertionAxiom(hasDeps, indv, literal);
		manager.addAxiom(ontology, ax);
		
		return indv;
	}
	
	public void addObjectProperties(OWLIndividual origin, OWLIndividual target, String objPropName) {
		OWLObjectProperty prop = factory.getOWLObjectProperty(IRI.create(ontologyIRI + "#" + objPropName));
		OWLObjectPropertyAssertionAxiom ax = factory.getOWLObjectPropertyAssertionAxiom(prop, origin, target);
	
		manager.addAxiom(ontology,ax);
	}

	public void addClassMinObjectProperties(OWLClass origin, OWLClass target, String objPropName) {

		OWLObjectProperty prop = factory.getOWLObjectProperty(IRI.create(ontologyIRI + "#" + objPropName));
		
		OWLClassExpression propExpr = factory.getOWLObjectMinCardinality(1, prop, target);//factory.getOWLObjectSomeValuesFrom(prop, target);
		OWLEquivalentClassesAxiom ax = factory.getOWLEquivalentClassesAxiom(origin, propExpr);
		AddAxiom addAxiom = new AddAxiom(ontology, ax);
		manager.applyChange(addAxiom);

	}
	
	public void addClassSomeObjectProperties(OWLClass origin, OWLClass target, String objPropName) {

		OWLObjectProperty prop = factory.getOWLObjectProperty(IRI.create(ontologyIRI + "#" + objPropName));

		OWLClassExpression propExpr = factory.getOWLObjectSomeValuesFrom(prop, target);
		OWLEquivalentClassesAxiom ax = factory.getOWLEquivalentClassesAxiom(origin, propExpr);
		
		AddAxiom addAxiom = new AddAxiom(ontology, ax);
		
		manager.applyChange(addAxiom);

	}
	
	
	
	
	public void addClassIntersectSomeObjectProperties(OWLClass origin, Set<OWLClass> set, String objPropName) {
		// create properties with and 
		OWLObjectProperty prop = factory.getOWLObjectProperty(IRI.create(ontologyIRI + "#" + objPropName));
		
		Set<OWLClassExpression> exprSet = new HashSet<OWLClassExpression>();
		for(OWLClass item: set) {
			OWLClassExpression propExpr = factory.getOWLObjectSomeValuesFrom(prop, item);
			exprSet.add(propExpr);
		}
		
		OWLObjectIntersectionOf  intersect = factory.getOWLObjectIntersectionOf(exprSet);
		
		OWLEquivalentClassesAxiom ax = factory.getOWLEquivalentClassesAxiom(origin, intersect);
		
		AddAxiom addAxiom = new AddAxiom(ontology, ax);
		
		manager.applyChange(addAxiom);

	}
	
	public boolean isSatisfiable(OWLClass cls) {
		OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
		if(!reasoner.isSatisfiable(cls)) {
			 System.out.println(cls.getIRI().getShortForm() +" is invalid...");
			/*
			Set<Set<OWLAxiom>> explanations=multExplanator.getExplanations(factory.getOWLThing(),2);
			// loop through expalnation
	        for (Set<OWLAxiom> explanation : explanations) {
	            System.out.println("------------------");
	            System.out.println("Axioms causing the inconsistency: ");
	            // find problematic axiom
	            for (OWLAxiom causingAxiom : explanation) {
	            	
	            	if(causingAxiom instanceof OWLSubClassOfAxiom) {
	            		OWLClass pcls = (OWLClass)((OWLSubClassOfAxiom)causingAxiom).getSubClass();
	            		System.out.println("problematic class: "+pcls.getIRI().getShortForm());
	            	}
	               // System.out.println(causingAxiom);
	            }
	            System.out.println("------------------");
	        }
	        */
	        return false;
		} else
			return true;
	}
	
	private int ruleNumber = 1;
	
	public void createSWLRule(String ruleStr) {
		manager.getOntologyFormat(ontology).asPrefixOWLOntologyFormat().setDefaultPrefix(ontologyIRI + "#");
		SWRLRuleEngine ruleEngine = SWRLAPIFactory.createSWRLRuleEngine(ontology);
		try {
			ruleEngine.createSWRLRule("r"+ruleNumber, ruleStr);
			
		} catch (SWRLParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SWRLBuiltInException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ruleNumber++;
	}
	
	public String generateSWRLRuleForEA(ConnectorType conn1, ConnectorType conn2, boolean isInvert) {
		
		StringBuffer str = new StringBuffer();
		str.append(" Component(?comp2)^ Component(?comp1)^ "
					+"hasAttachment(?p2, ?pub)^ hasAttachment(?p4, ?sub)^"
					+"hasAttachment(?p1, ?con)^ hasAttachment(?p3, ?pro)^ "
					+"isPortOf(?p1, ?comp1)^ isPortOf(?p4, ?comp2)^ "
					+"isPortOf(?p2, ?comp1)^ isPortOf(?p3, ?comp2)^ "
					+conn1.getName()+ "(?pns)^ hasRole(?pns, ?sub)^ hasRole(?pns, ?pub)^ "
					+conn1.getRoletype().get(0).getName()+"(?pub)^ "+conn1.getRoletype().get(1).getName()+"(?sub)^ "
					+conn2.getName()+"(?cns)^ hasRole(?cns, ?con)^ hasRole(?cns, ?pro)^ ");
		
		
		if(isInvert) {		
			str.append(conn2.getRoletype().get(0).getName()+"(?con)^ "+conn2.getRoletype().get(1).getName()+"(?pro)");
		} else {
			str.append(conn2.getRoletype().get(0).getName()+"(?pro)^ "+conn2.getRoletype().get(1).getName()+"(?con)");
		}
			
		str.append(" -> EAConnector(?pns)^ EAConnector(?cns)");
					
		return str.toString();
	}
	
	public void addClassExpression(OWLClass cls, String def) {
		OWLClassExpression clsExpr = this.convertStringToClassExpression(def);
		OWLEquivalentClassesAxiom ax = factory.getOWLEquivalentClassesAxiom(cls, clsExpr);
		AddAxiom addAxiom = new AddAxiom(ontology, ax);
		manager.applyChange(addAxiom);
	}
	
	private OWLClassExpression convertStringToClassExpression(String expression) {
        ManchesterOWLSyntaxParser parser = OWLManager.createManchesterParser();

        Set<OWLOntology> importsClosure = ontology.getImportsClosure();
		OWLEntityChecker entityChecker = new ShortFormEntityChecker(
		        new BidirectionalShortFormProviderAdapter(manager, importsClosure, 
		            new SimpleShortFormProvider()));
        
        parser.setStringToParse(expression);
        parser.setDefaultOntology(ontology);
        parser.setOWLEntityChecker(entityChecker);
        
        return parser.parseClassExpression();
    }
	
	
}
