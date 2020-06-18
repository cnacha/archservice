package nz.auckland.arch.service.owl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.clarkparsia.owlapi.explanation.BlackBoxExplanation;
import com.clarkparsia.owlapi.explanation.HSTExplanationGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fr.uga.pddl4j.encoding.CodedProblem;
import fr.uga.pddl4j.parser.ErrorManager;
import fr.uga.pddl4j.planners.ProblemFactory;
import fr.uga.pddl4j.planners.statespace.StateSpacePlannerFactory;
import fr.uga.pddl4j.planners.statespace.ff.FF;
import fr.uga.pddl4j.util.BitOp;
import fr.uga.pddl4j.util.SequentialPlan;

import org.emfjson.EMFJs;
import org.emfjson.jackson.annotations.EcoreTypeInfo;
import org.emfjson.jackson.databind.EMFContext;
import org.emfjson.jackson.handlers.IdentityURIHandler;
import org.emfjson.jackson.module.EMFModule;
import org.emfjson.jackson.resource.JsonResourceFactory;
import org.emfjson.jackson.utils.ValueReader;
import org.emfjson.jackson.utils.ValueWriter;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.InferredSubClassAxiomGenerator;

import nz.auckland.arch.ArchPackage;
import nz.auckland.arch.ArchStyle;
import nz.auckland.arch.CommunicationLink;
import nz.auckland.arch.Component;
import nz.auckland.arch.ComponentType;
import nz.auckland.arch.Connector;
import nz.auckland.arch.ConnectorType;
import nz.auckland.arch.DesignModel;
import nz.auckland.arch.MigrationModel;
import nz.auckland.arch.Port;
import nz.auckland.arch.PortType;
import nz.auckland.arch.Role;
import nz.auckland.arch.RoleType;
import nz.auckland.arch.planner.MigrationPlanner;
import nz.auckland.arch.planner.object.Action;
import nz.auckland.arch.planner.object.MigrationRequest;
import nz.auckland.arch.planner.object.Parameter;
import nz.auckland.arch.planner.object.Plan;
import nz.auckland.arch.service.owl.factory.OWLSecurityChecker;
import nz.auckland.arch.service.owl.factory.OWLSmellChecker;
import nz.auckland.arch.service.owl.factory.OWLUtil;
import nz.auckland.arch.service.owl.utils.StringUtil;


@Controller
public class ServiceController {
	
	private static String PDDL_URL = "D:/config/pddl/";

	
	@RequestMapping(value = "/api/owl/planMigration", method = RequestMethod.POST)
	@ResponseBody
	public String planMigration(@RequestBody String modelJson) {
		try {
			
			System.out.println(modelJson);
			GsonBuilder builder = new GsonBuilder(); 
		    builder.setPrettyPrinting(); 
		    Gson gson = builder.create();
		    MigrationRequest model = gson.fromJson(modelJson, MigrationRequest.class);
		    
		    DesignModel sourceModel = (DesignModel)this.loadDesignModelFromString(model.getSource(), ArchPackage.eINSTANCE);
		    DesignModel targetModel = (DesignModel)this.loadDesignModelFromString(model.getTarget(), ArchPackage.eINSTANCE);

		    
		    System.out.println("====================== Plan Migration ======================");
			System.out.println(" source: "+sourceModel.getComponent().size());
			System.out.println(" target: "+targetModel.getComponent().size());
			
			//generate plan
			MigrationPlanner mplanner = new MigrationPlanner(sourceModel, targetModel);
			Plan migrationPlan = mplanner.run();
			
			// TODO: generate plan based on this migration model
			
			
			
		     return gson.toJson(migrationPlan);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	@RequestMapping(value = "/api/owl/verifyStructure", method = RequestMethod.POST)
	@ResponseBody
	public String verifyStructure(@RequestBody String modelJson) {
		try {
			
			//System.out.println(modelJson);
			EObject obj = this.loadDesignModelFromString(modelJson, ArchPackage.eINSTANCE);
			DesignModel model = (DesignModel) obj;
			System.out.println(" arch styles: "+model.getArchstyle().size());
			System.out.println(" component: "+model.getComponent().size());
			
			OWLSmellChecker checker = new OWLSmellChecker(model);
			checker.setTemplate("arch.owl");
			checker.check();
			
			// convert model to JSON
			
			ResourceSet resourceSet = new ResourceSetImpl();
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new JsonResourceFactory());
			
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
					.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new JsonResourceFactory());
			resourceSet.getPackageRegistry().put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
			resourceSet.getPackageRegistry().put(ArchPackage.eINSTANCE.getNsURI(), ArchPackage.eINSTANCE);
	    /*
			Resource resource = resourceSet.createResource
					  (URI.createFileURI("data.json"));
			
			resource.getContents().add(model);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			resource.save(baos,null);
			String jsonString = baos.toString();
		    */
			ObjectMapper mapper = new ObjectMapper();
			EMFModule module = new EMFModule();
			/*
			module.setTypeInfo(new EcoreTypeInfo("type",
					  new ValueWriter<EClass, String>() {
					      @Override
					      public String writeValue(EClass value, SerializerProvider context) {
					          return value.getName();
					      }
					  }));
*/
			mapper.registerModule(module);
			
			//model.getHost().clear();
			//model.getLink().clear();
			model.getVerifyProperty().clear();
						
			Resource resource = resourceSet.createResource
					  (URI.createURI("data.json"));
			resource.getContents().add(model);
			
			
			
			String jsonString = mapper.writeValueAsString(resource);
			
			/** Start fix problem with eClass missing **/
			JsonNode rootNode = mapper.readTree(jsonString);
			rootNode = insertEClass(rootNode);
	        
	        /** End fix problem with eClass missing **/
			
			System.out.println( rootNode.toString());
			System.out.print("################################################");
			
			return rootNode.toString();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "error";
		}
	    
	}
	
	@RequestMapping(value = "/api/owl/checkSmell", method = RequestMethod.POST)
	@ResponseBody
	public String checkSmell(@RequestBody String modelJson) {
		try {
			
			//System.out.println(modelJson);
			EObject obj = this.loadDesignModelFromString(modelJson, ArchPackage.eINSTANCE);
			DesignModel model = (DesignModel) obj;
			System.out.println(" arch styles: "+model.getArchstyle().size());
			System.out.println(" component: "+model.getComponent().size());
			
			OWLSmellChecker checker = new OWLSmellChecker(model);
			checker.setTemplate("arch_smell.owl");
			checker.check();
			
			// convert model to JSON
			
			ResourceSet resourceSet = new ResourceSetImpl();
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new JsonResourceFactory());
			
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
					.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new JsonResourceFactory());
			resourceSet.getPackageRegistry().put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
			resourceSet.getPackageRegistry().put(ArchPackage.eINSTANCE.getNsURI(), ArchPackage.eINSTANCE);
	    /*
			Resource resource = resourceSet.createResource
					  (URI.createFileURI("data.json"));
			
			resource.getContents().add(model);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			resource.save(baos,null);
			String jsonString = baos.toString();
		    */
			ObjectMapper mapper = new ObjectMapper();
			EMFModule module = new EMFModule();
			/*
			module.setTypeInfo(new EcoreTypeInfo("type",
					  new ValueWriter<EClass, String>() {
					      @Override
					      public String writeValue(EClass value, SerializerProvider context) {
					          return value.getName();
					      }
					  }));
*/
			mapper.registerModule(module);
			
			//model.getHost().clear();
			//model.getLink().clear();
			model.getVerifyProperty().clear();
						
			Resource resource = resourceSet.createResource
					  (URI.createURI("data.json"));
			resource.getContents().add(model);
			
			
			
			String jsonString = mapper.writeValueAsString(resource);
			
			/** Start fix problem with eClass missing **/
			JsonNode rootNode = mapper.readTree(jsonString);
			rootNode = insertEClass(rootNode);
	        
	        /** End fix problem with eClass missing **/
			
			System.out.println( rootNode.toString());
			System.out.print("################################################");
			
			return rootNode.toString();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "error";
		}
	    
	}
	
	
	@RequestMapping(value = "/api/owl/checkSecurity", method = RequestMethod.POST)
	@ResponseBody
	public String checkSecurity(@RequestBody String modelJson) {
		try {
			
			//System.out.println(modelJson);
			EObject obj = this.loadDesignModelFromString(modelJson, ArchPackage.eINSTANCE);
			DesignModel model = (DesignModel) obj;
			System.out.println(" arch styles: "+model.getArchstyle().size());
			System.out.println(" component: "+model.getComponent().size());
			
			OWLSecurityChecker checker = new OWLSecurityChecker(model);
			checker.check();
			
			// convert model to JSON
			
			ResourceSet resourceSet = new ResourceSetImpl();
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new JsonResourceFactory());
			
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
					.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new JsonResourceFactory());
			resourceSet.getPackageRegistry().put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
			resourceSet.getPackageRegistry().put(ArchPackage.eINSTANCE.getNsURI(), ArchPackage.eINSTANCE);

			ObjectMapper mapper = new ObjectMapper();
			EMFModule module = new EMFModule();

			mapper.registerModule(module);
			
			model.getVerifyProperty().clear();
			for(CommunicationLink link: model.getLink()) {
				if(link.getSource()!=null)
					link.getSource().getNamedport().clear();
				if(link.getTarget()!=null)
					link.getTarget().getNamedport().clear();
			}
			//model.getComponent().clear();
			//model.getArchstyle().clear();
			//model.getConnector().clear();
						
			Resource resource = resourceSet.createResource
					  (URI.createURI("data.json"));
			resource.getContents().add(model);
			
			
			
			String jsonString = mapper.writeValueAsString(resource);
			
			/** Start fix problem with eClass missing **/
			JsonNode rootNode = mapper.readTree(jsonString);
			rootNode = insertEClass(rootNode);
			
			System.out.println( rootNode.toString());
			System.out.print("################################################");
			
			return rootNode.toString();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "error";
		}
	    
	}
	
	private JsonNode insertEClass(JsonNode rootNode) {
		JsonNode linkNodes = rootNode.path("link");
		 for (JsonNode linkNode : linkNodes) {
	        	((ObjectNode)linkNode).put("eClass", ArchPackage.eNS_URI +"#//CommunicationLink");
	        		
		 }
		 for(JsonNode hostNode : rootNode.path("host")) {
			 ((ObjectNode)hostNode).put("eClass", ArchPackage.eNS_URI +"#//Device");
			 
			 for(JsonNode nodeNodes : hostNode.path("node")) {
				 ((ObjectNode)nodeNodes).put("eClass", ArchPackage.eNS_URI +"#//ExecutionEnvironment");
				 
				for(JsonNode portNode : nodeNodes.path("port")) {
					((ObjectNode)portNode).put("eClass", ArchPackage.eNS_URI +"#//CommunicationPort");
				}
			 }
		 }
		 
       JsonNode componentNode = rootNode.path("component");
       for (JsonNode compNode : componentNode) {
       	((ObjectNode)compNode).put("eClass", ArchPackage.eNS_URI +"#//Component");
       	
       	for(JsonNode portNode : compNode.path("port")) {
       		((ObjectNode)portNode).put ("eClass", ArchPackage.eNS_URI +"#//Port");
       	}
       }
       JsonNode connectorNode = rootNode.path("connector");
       for (JsonNode connNode : connectorNode) {
       	((ObjectNode)connNode).put("eClass", ArchPackage.eNS_URI +"#//Connector");
       	
       	for(JsonNode roleNode : connNode.path("role")) {
       		((ObjectNode)roleNode).put("eClass", ArchPackage.eNS_URI +"#//Role");
       	}
       }
       JsonNode archStyleNode = rootNode.path("archstyle");
       for(JsonNode styleNode : archStyleNode) {
       	((ObjectNode)styleNode).put("eClass", ArchPackage.eNS_URI +"#//ArchStyle");
       	JsonNode componentTypNode = styleNode.path("componenttype");
	        for (JsonNode compNode : componentTypNode) {
	        	((ObjectNode)compNode).put("eClass", ArchPackage.eNS_URI +"#//ComponentType");
	        	
	        	for(JsonNode portNode : compNode.path("porttype")) {
	        		((ObjectNode)portNode).put ("eClass", ArchPackage.eNS_URI +"#//PortType");
	        	}
	        }
	        JsonNode connectorTypNode = styleNode.path("connectortype");
	        for (JsonNode connNode : connectorTypNode) {
	        	((ObjectNode)connNode).put("eClass", ArchPackage.eNS_URI +"#//ConnectorType");
	        	
	        	for(JsonNode roleNode : connNode.path("roletype")) {
	        		((ObjectNode)roleNode).put("eClass", ArchPackage.eNS_URI +"#//RoleType");
	        	}
	        }
       }
       
       return rootNode;
	}
	
	private EObject loadMigrationModelFromString(String modelStr, EPackage ePackage) throws IOException { 
		System.out.println("Parsing JSON to EObject");
		
	    ResourceSet resourceSet = new ResourceSetImpl();
	    resourceSet.getPackageRegistry().put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
	    resourceSet.getPackageRegistry().put(ePackage.getNsURI(), ePackage);
	    resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new JsonResourceFactory());
	
	    ObjectMapper mapper = new ObjectMapper();
	    EMFModule module = new EMFModule();

	    mapper.registerModule(module);
	  
		Resource resource = mapper
				  .reader()
				    .withAttribute(EMFContext.Attributes.RESOURCE_SET, resourceSet)
				    .forType(Resource.class)
				    .readValue(modelStr);
		 MigrationModel model = (MigrationModel)resource.getContents().get(0);
		 
		 return model;
	}
	private EObject loadDesignModelFromString(String modelStr, EPackage ePackage) throws IOException { 
		System.out.println("Parsing JSON to EObject");
		
	    ResourceSet resourceSet = new ResourceSetImpl();
	    resourceSet.getPackageRegistry().put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
	    resourceSet.getPackageRegistry().put(ePackage.getNsURI(), ePackage);
	    resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new JsonResourceFactory());
	    
	    /*
	    Resource resource = resourceSet.createResource(URI.createURI("http://www.example.org/arch"));
	    ByteArrayInputStream stream = new ByteArrayInputStream(modelStr.getBytes(StandardCharsets.UTF_8));
	    Map<String, Object> options = new HashMap<>();
	    options.put(EMFJs.OPTION_URI_HANDLER, new IdentityURIHandler());
	   
	    resource.load(stream, options);
	    return resource.getContents().get(0);
	     */
	   
	    ObjectMapper mapper = new ObjectMapper();
	    EMFModule module = new EMFModule();

	    mapper.registerModule(module);
	  
		Resource resource = mapper
				  .reader()
				    .withAttribute(EMFContext.Attributes.RESOURCE_SET, resourceSet)
				    .forType(Resource.class)
				    .readValue(modelStr);
		 DesignModel model = (DesignModel)resource.getContents().get(0);
		 
		/*
		JsonNode rootNode = mapper.readTree(modelStr);
        JsonNode componentNode = rootNode.path("component");
    //    model.getComponent().clear();
        int i=0;
        for (JsonNode node : componentNode) {
        	System.out.println("reading "+node.toString());
        	Component comp = mapper
			  .reader()
			    .withAttribute(EMFContext.Attributes.RESOURCE_SET, resourceSet)
			    .forType(Component.class)
			    .readValue(node);
        //	  model.getComponent().get(i).eIsSet(comp.eClass().getEStructuralFeature(0));
        	 comp.getPort().addAll(model.getComponent().get(i).getPort());
        	 System.out.println(mapper.writeValueAsString(comp));
        	 model.getComponent().set(i, comp);
        	 System.out.println(mapper.writeValueAsString( resource.getContents().get(0)));
        //	model.getComponent().add(comp);
        	i++;
        }

		
	  System.out.println("######### finish reading value");
	  System.out.println(mapper.writeValueAsString(model));
	  System.out.println("######### finish reading value");
	  */
		return model;
	}
	

	
}
