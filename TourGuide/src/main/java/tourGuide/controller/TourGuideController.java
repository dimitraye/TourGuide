package tourGuide.controller;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jsoniter.output.JsonStream;

import gpsUtil.location.VisitedLocation;
import tourGuide.service.TourGuideService;
import tourGuide.service.UserService;
import tourGuide.DTO.AttractionDTO;
import tourGuide.model.user.User;
import tripPricer.Provider;

@Slf4j
@RestController
public class TourGuideController {

	@Autowired
	TourGuideService tourGuideService;

    @Autowired
    UserService userService;
	
    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }
    
    @RequestMapping("/getLocation") 
    public String getLocation(@RequestParam String userName) {
    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(userService.getUser(userName));
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
    @RequestMapping("/get5ClosestAttractions")
    public ResponseEntity<List<AttractionDTO>> get5ClosestAttractions(@RequestParam String userName) {
        User user = userService.getUser(userName);
    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(user);
        return new ResponseEntity<>(tourGuideService.get5ClosestAttractions(visitedLocation, user), HttpStatus.OK);

    }
    
    @RequestMapping("/getTripDeals")
    public ResponseEntity<List<Provider>> getTripDeals(@RequestParam String userName) {
    	List<Provider> providers = tourGuideService.getTripDeals(userService.getUser(userName));
        return new ResponseEntity<>(providers, HttpStatus.OK);
    }

}