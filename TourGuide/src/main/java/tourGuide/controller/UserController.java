package tourGuide.controller;

import com.jsoniter.output.JsonStream;
import gpsUtil.location.Location;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tourGuide.model.user.User;
import tourGuide.model.user.UserPreferences;
import tourGuide.service.TourGuideService;
import tourGuide.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
public class UserController {

	@Autowired
	TourGuideService tourGuideService;

    @Autowired
    UserService userService;
    
    @RequestMapping("/getRewards") 
    public String getRewards(@RequestParam String userName) {
    	return JsonStream.serialize(userService.getUserRewards(getUser(userName)));
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
        return new ResponseEntity<>(userService.getAllCurrentLocations(), HttpStatus.OK);

    }
    
    private User getUser(String userName) {
    	return userService.getUser(userName);
    }

    @RequestMapping("/getUser")
    public ResponseEntity<User> getUserByUserName(@RequestParam String userName){
        User userFromDB = getUser(userName);

        //S'il n'éxiste pas, envoie statut 404
        if(userFromDB == null) {
            log.error("Error : User doesn't exist in the Data Base.");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        log.info("Returning the user's informations");
        //Sinon, retourner patient
        return new ResponseEntity<>(userFromDB, HttpStatus.OK);
    }

    @GetMapping("/getUsers")
    public List<User> getUsers() {
        return userService.getAllUsers();
    }

    @PostMapping("/addUser")
    public ResponseEntity<User>  addUser(@RequestBody User user) {
        //Ajout d'un user
        User userFromDB = userService.addUser(user);
        if(userFromDB != null ){
            //Si true, récupérer user en BD
            return new ResponseEntity<>(userFromDB, HttpStatus.CREATED);
        }

        //sinon message d'erreur
        log.error("Error : Couldn't create user.");
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @PutMapping("/updateUserPreferences")
    public ResponseEntity updateUserPreferences(@RequestParam String userName, @RequestBody UserPreferences userPreferences) {
        if (userService.updateUserPreferences(userName, userPreferences)){
            return new ResponseEntity<>(HttpStatus.OK);
        }

        //sinon message d'erreur
        log.error("Error : Couldn't update user's preferences.");
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
}