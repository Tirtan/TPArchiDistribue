package tp.rest;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Source;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.http.HTTPException;

import tp.model.Animal;
import tp.model.AnimalNotFoundException;
import tp.model.Cage;
import tp.model.Center;
import tp.model.Position;

@WebServiceProvider
@ServiceMode(value = Service.Mode.MESSAGE)
public class MyServiceTP implements Provider<Source> {

    public final static String url = "http://127.0.0.1:8084/";

    public static void main(String args[]) {
        Endpoint e = Endpoint.create(HTTPBinding.HTTP_BINDING, new MyServiceTP());

        e.publish(url);
        System.out.println("Service started, listening on " + url);
        // pour arrÃªter : e.stop();
    }

    private JAXBContext jc;

    @javax.annotation.Resource(type = Object.class)
    protected WebServiceContext wsContext;

    private Center center = new Center(new LinkedList<>(), new Position(49.30494d, 1.2170602d), "Biotropica");

    public MyServiceTP() {
        try {
            jc = JAXBContext.newInstance(Center.class, Cage.class, Animal.class, Position.class);
        } catch (JAXBException je) {
            System.out.println("Exception " + je);
            throw new WebServiceException("Cannot create JAXBContext", je);
        }

        // Fill our center with some animals
        Cage usa = new Cage(
                "usa",
                new Position(49.305d, 1.2157357d),
                25,
                new LinkedList<>(Arrays.asList(
                        new Animal("Tic", "usa", "Chipmunk", UUID.randomUUID()),
                        new Animal("Tac", "usa", "Chipmunk", UUID.randomUUID())
                ))
        );

        Cage amazon = new Cage(
                "amazon",
                new Position(49.305142d, 1.2154067d),
                15,
                new LinkedList<>(Arrays.asList(
                        new Animal("Canine", "amazon", "Piranha", UUID.randomUUID()),
                        new Animal("Incisive", "amazon", "Piranha", UUID.randomUUID()),
                        new Animal("Molaire", "amazon", "Piranha", UUID.randomUUID()),
                        new Animal("De lait", "amazon", "Piranha", UUID.randomUUID())
                ))
        );

        center.getCages().addAll(Arrays.asList(usa, amazon));
    }
    

    public Source invoke(Source source) {
        MessageContext mc = wsContext.getMessageContext();
        String path = (String) mc.get(MessageContext.PATH_INFO);
        String method = (String) mc.get(MessageContext.HTTP_REQUEST_METHOD);

        // determine the targeted ressource of the call
        try {
            // no target, throw a 404 exception.
            if (path == null) {
                throw new HTTPException(404);
            }
            // "/animals" target - Redirect to the method in charge of managing this sort of call.
            else if (path.startsWith("animals")) {
                String[] path_parts = path.split("/");
                switch (path_parts.length){
                    case 1 :
                        return this.animalsCrud(method, source);
                    case 2 :
                        return this.animalCrud(method, source, path_parts[1]);
                    default:
                        throw new HTTPException(404);
                }
            }
            else if (path.startsWith("find/")) {
                throw new HTTPException(503);
            }
            else if ("coffee".equals(path)) {
                throw new HTTPException(418);
            }
            else {
                throw new HTTPException(404);
            }
        } catch (JAXBException | AnimalNotFoundException e) {
            throw new HTTPException(500);
        }
    }

    /**
     * Method bound to calls on /animals/{something}
     * @throws AnimalNotFoundException 
     */
    private Source animalCrud(String method, Source source, String animal_id) throws JAXBException, AnimalNotFoundException {
        if("GET".equals(method)){
            try {
                return new JAXBSource(this.jc, center.findAnimalById(UUID.fromString(animal_id)));
            } catch (AnimalNotFoundException e) {
                throw new HTTPException(404);
            }
            // POST animal  créer l'animal identifié par animal_ID 
        }else if("POST".equals(method)) {
        		
        		Animal mouton = unmarshalAnimal(source);
        		mouton.setId(UUID.fromString(animal_id));
        		
        		this.center.getCages()
	        		.stream()
	                .filter(cage -> cage.getName().equals(mouton.getCage()))
	                .findFirst()
	                .orElseThrow(() -> new HTTPException(404))
	                .getResidents()
	                .add(mouton);
        		
        		return (new JAXBSource(this.jc,this.center)); 
        		
        	// PUT modifie l'animal identifié par animal_ID	
        }else if ("PUT".equals(method)){
        		Animal mouton = center.findAnimalById(UUID.fromString(animal_id));
        		Animal boby = unmarshalAnimal(source);
        		
        		this.center.getCages()
                	.stream()
	                .filter(cage -> cage.getName().equals(mouton.getCage()))
	                .findFirst()
	                .orElseThrow(() -> new HTTPException(404))
	                .getResidents()
	                .add(mouton);
	        		 
        		 
				this.center.getCages()
					.stream()
					.filter(cage -> cage.getName().equals(mouton.getCage()))
					.findFirst()
					.orElseThrow(() -> new HTTPException(404))
					.getResidents()
				 	.add(boby);
        		 
        		 
        		return (new JAXBSource(this.jc,this.center)); 
        }
        	
        else {     	
        	throw new HTTPException(405);
        }
    }

    /**
     * Method bound to calls on /animals
     */
    private Source animalsCrud(String method, Source source) throws JAXBException {
        if("GET".equals(method)){
            return (new JAXBSource(this.jc, this.center));
        }
        else if("POST".equals(method)){
            Animal animal = unmarshalAnimal(source);
            this.center.getCages()
                    .stream()
                    .filter(cage -> cage.getName().equals(animal.getCage()))
                    .findFirst()
                    .orElseThrow(() -> new HTTPException(404))
                    .getResidents()
                    .add(animal);
            return new JAXBSource(this.jc, this.center);
            
          //PUT animal    modifie l'ensemble des animaux
        } else if ("PUT".equals(method)) {
        	Center centre = unmarshalCenter(source);
        	this.center = centre ;
        	return (new JAXBSource(this.jc,this.center));
        
         // DELETE supprime l'ensemble des animaux
        } else if ("DELETE".equals(method)) {
        	Collection<Cage> collectionCages = this.center.getCages();
        	Iterator<Cage> i = collectionCages.iterator();
        	while (i.hasNext()) {
        		
        		((Cage) i).setResidents(new LinkedList<Animal>());
        	}
        	return (new JAXBSource(this.jc,this.center));
        	
        } else {
            throw new HTTPException(405);
        }
    }

    private Animal unmarshalAnimal(Source source) throws JAXBException {
        return (Animal) this.jc.createUnmarshaller().unmarshal(source);
    }
    private Center unmarshalCenter(Source source) throws JAXBException{
    	return (Center) this.jc.createUnmarshaller().unmarshal(source);
    }
}
