package tourGuide;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import gpsUtil.location.Location;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.jsoniter.output.JsonStream;

import gpsUtil.location.VisitedLocation;
import tourGuide.service.TourGuideService;
import tourGuide.user.AttractionDTO;
import tourGuide.user.User;
import tourGuide.user.UserPreferences;
import tripPricer.Provider;

import javax.validation.Valid;

@Slf4j
@RestController
public class TourGuideController {

	@Autowired
	TourGuideService tourGuideService;
	
    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }
    
    @RequestMapping("/getLocation") 
    public String getLocation(@RequestParam String userName) {
    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));
		return JsonStream.serialize(visitedLocation.location);
    }
    
    //  TODO: Change this method to no longer return a List of Attractions.
 	//  Instead: Get the closest five tourist attractions to the user - no matter how far away they are.
 	//  Return a new JSON object that contains:
    	// Name of Tourist attraction, 
        // Tourist attractions lat/long, 
        // The user's location lat/long, 
        // The distance in miles between the user's location and each of the attractions.
        // The reward points for visiting each Attraction.
        //    Note: Attraction reward points can be gathered from RewardsCentral
    @RequestMapping("/getNearbyAttractions") 
    public ResponseEntity<List<AttractionDTO>> getNearbyAttractions(@RequestParam String userName) {
        User user = getUser(userName);
    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(user);
        return new ResponseEntity<>(tourGuideService.get5NearByAttractions(visitedLocation, user), HttpStatus.OK);

    }

    
    @RequestMapping("/getRewards") 
    public String getRewards(@RequestParam String userName) {
    	return JsonStream.serialize(tourGuideService.getUserRewards(getUser(userName)));
    }
    
    @RequestMapping("/getAllCurrentLocations")
    public ResponseEntity<Map<UUID, Location>> getAllCurrentLocations() {
    	// TODO: Get a list of every user's most recent location as JSON
    	//- Note: does not use gpsUtil to query for their current location, 
    	//        but rather gathers the user's current location from their stored location history.
    	//
    	// Return object should be the just a JSON mapping of userId to Locations similar to:
    	//     {
    	//        "019b04a9-067a-4c76-8817-ee75088c3822": {"longitude":-48.188821,"latitude":74.84371} 
    	//        ...
    	//     }
        return new ResponseEntity<>(tourGuideService.getAllCurrentLocations(), HttpStatus.OK);

    }
    
    @RequestMapping("/getTripDeals")
    public ResponseEntity<List<Provider>> getTripDeals(@RequestParam String userName) {
    	List<Provider> providers = tourGuideService.getTripDeals(getUser(userName));
        return new ResponseEntity<>(providers, HttpStatus.OK);
    }
    
    private User getUser(String userName) {
    	return tourGuideService.getUser(userName);
    }

    //Ajouter une méthode getUser pour récupérer un user et afficher ses infos
    @RequestMapping("/getUser")
    public ResponseEntity<User> getUserByUserName(@RequestParam String userName){
        User userFromDB = getUser(userName);

        //S'il n'éxiste pas, envoie statut 404
        if(userFromDB == null) {
            //log.error("Error : id Patient doesn't exist in the Data Base.");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        //log.info("Returning the patient's informations");
        //Sinon, retourner patient
        return new ResponseEntity<>(userFromDB, HttpStatus.OK);
    }

    //Ajouter une méthode getUsers pour récupérer la liste des users
    @GetMapping("/getUsers")
    public List<User> getUsers() {
        return tourGuideService.getAllUsers();
    }

    //AddUser pour créer un nouvel user
    @PostMapping("/addUser")
    public ResponseEntity<User>  addUser(@RequestBody User user) {
        //Ajout d'un user
        User userFromDB = tourGuideService.addUser(user);
        if(userFromDB != null ){
            //Si true, récupérer user en BD
            return new ResponseEntity<>(userFromDB, HttpStatus.CREATED);
        }

        //sinon message d'erreur
        log.error("Error : Couldn't create user.");
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }


    //G
    //UpdateUserPreferences pour ajouter les préférences d'un user
    @PutMapping("/updateUserPreferences")
    public ResponseEntity updateUserPreferences(@RequestParam String userName, @RequestBody UserPreferences userPreferences) {
        if (tourGuideService.updateUserPreferences(userName, userPreferences)){
            return new ResponseEntity<>(HttpStatus.OK);
        }

        //sinon message d'erreur
        log.error("Error : Couldn't update user's preferences.");
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

   

}