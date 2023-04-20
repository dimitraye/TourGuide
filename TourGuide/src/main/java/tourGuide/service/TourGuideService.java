package tourGuide.service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import tourGuide.tracker.Tracker;
import tourGuide.DTO.AttractionDTO;
import tourGuide.model.user.User;
import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final UserService userService;

	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;
	ExecutorService executorService = Executors.newFixedThreadPool(64);


	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService, UserService userService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;
		this.userService = userService;

		if(testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			userService.initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}
	
	public VisitedLocation getUserLocation(User user) {
		VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ?
			user.getLastVisitedLocation() :
			trackUserLocation(user);
		return visitedLocation;
	}
	
	public List<User> getAllUsers() {
		return userService.getAllUsers();
	}
	
	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(
				tripPricerApiKey,
				user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(),
				user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(),
				cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}

	public VisitedLocation trackUserLocation(User user) {

		assert user.getLastVisitedLocation() != null : "last visit should not be null";

		CompletableFuture<VisitedLocation> future = CompletableFuture.supplyAsync(() ->
						getUserLocation(user), executorService)
				.thenApply(visitedLocation -> {
					user.addToVisitedLocations(visitedLocation);
					rewardsService.calculateRewards(user);
					return visitedLocation;
				});

		VisitedLocation visitedLocation = future.join();
		return visitedLocation;
	}


	public List<AttractionDTO> get5ClosestAttractions(VisitedLocation visitedLocation, User user) {
		//Récupérer la liste de toutes les attractions
		List<Attraction> nearbyAttractions = new ArrayList<>();
		nearbyAttractions.addAll(gpsUtil.getAttractions());

		//Récupérer la dernière location du user
		VisitedLocation lastVisitedLocation = user.getLastVisitedLocation();
		Location userLocation = lastVisitedLocation.location;

		//Créer une map clef(distance de l'attraction par rapport à l'utilisateur) valeur(l'attraction)
		Map<Double, Attraction> attractionMap = new HashMap<>();

		for (Attraction attraction : nearbyAttractions) {
			//Location de l'attraction
			Location attractionLocation = new Location(attraction.latitude, attraction.longitude);

			//Calcul de la distance
			double distance = rewardsService.getDistance(userLocation , attractionLocation);

			//Ajout de la distance et de l'attraction sous forme paire clef/valeur
			attractionMap.put(distance, attraction);
		}

		//Ordonner la liste des attractions en fonciton de la distance du user
		Map<Double, Attraction> oredredMapAttraction = new TreeMap<>(attractionMap);

		//Récupérer les 5 premières attractions
		/*Map<Double, Attraction> mapOf5Attractions = oredredMapAttraction.entrySet().stream().limit(5)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));*/

		Map<Double, Attraction> map5Attractions = new TreeMap<>();
		int i = 0;
		for (Map.Entry mapentry : oredredMapAttraction.entrySet()) {
			map5Attractions.put((Double) mapentry.getKey(), (Attraction) mapentry.getValue());
			i++;
			if (i>=5) {
				break;
			}
		}

		//Convertir les attractions en attractionDTO
		List<AttractionDTO> attractionDTOList = new ArrayList<>();


		for (Map.Entry attractionDistance : map5Attractions.entrySet()) {
			AttractionDTO attractionDTO = new AttractionDTO();
			Attraction attraction = (Attraction) attractionDistance.getValue();

			attractionDTO.setAttractionName(attraction.attractionName);
			attractionDTO.setUserLocation(userLocation);
			attractionDTO.setAttractionLocation(attraction);
			attractionDTO.setDistanceMiles((Double) attractionDistance.getKey());
			int rewardPoints = rewardsService.getRewardPoints(attraction, user);
			attractionDTO.setRewardPoints(rewardPoints);

			attractionDTOList.add(attractionDTO);
		}

		return attractionDTOList;
	}

	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() { 
		      public void run() {
		        tracker.stopTracking();
		      } 
		    }); 
	}
	
	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes internal users are provided and stored in memory
}
