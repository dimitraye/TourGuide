package tourGuide.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import tourGuide.helper.InternalTestHelper;
import tourGuide.tracker.Tracker;
import tourGuide.user.AttractionDTO;
import tourGuide.user.User;
import tourGuide.user.UserPreferences;
import tourGuide.user.UserReward;
import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;
	
	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;
		
		if(testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}
	
	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	public boolean updateUserPreferences(String userName, UserPreferences userPreferences) {
		//Récuperer un user par son userName
		User user = getUser(userName);

		//Update ses préférences depuis user
		user.setUserPreferences(userPreferences);

		//update la map


		return true;
	}
	
	public VisitedLocation getUserLocation(User user) {
		VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ?
			user.getLastVisitedLocation() :
			trackUserLocation(user);
		return visitedLocation;
	}
	
	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}
	
	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}
	

	public User addUser(User user) {
		if(!internalUserMap.containsKey(user.getUserName())) {
			user.setUSER_ID(UUID.randomUUID());
			generateUserLocationHistory(user);

			internalUserMap.put(user.getUserName(), user);

			return user;
		}
		return null;
	}
	
	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(
				tripPricerApiKey,
				user.getUSER_ID(),
				user.getUserPreferences().getNumberOfAdults(),
				user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(),
				cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}
	
	public VisitedLocation trackUserLocation(User user) {
		VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUSER_ID());
		user.addToVisitedLocations(visitedLocation);
		rewardsService.calculateRewards(user);
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

	public Map<UUID, Location> getAllCurrentLocations() {
		//Créer la map de retour
		Map<UUID, Location> allCurrentLocations = new HashMap<>();

		//Créer la liste des users
		List<User> users = new ArrayList<>();
		users.addAll(getAllUsers());

		//Pour chaque user, ajouter son id à la clef de la map et sa localisation à sa valeur
		users.forEach(user -> {
			Location location = user.getLastVisitedLocation().location != null ? user.getLastVisitedLocation().location : null;

			if(location != null) {
				allCurrentLocations.put(user.getUSER_ID(), location);
			}
		});

		return allCurrentLocations;
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
	private final Map<String, User> internalUserMap = new HashMap<>();
	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);
			
			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}
	
	public void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i-> {
			user.addToVisitedLocations(new VisitedLocation(user.getUSER_ID(), new Location(generateRandomLatitude(),
					generateRandomLongitude()), getRandomTime()));
		});
	}
	
	private double generateRandomLongitude() {
		double leftLimit = -180;
	    double rightLimit = 180;
	    return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}
	
	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
	    double rightLimit = 85.05112878;
	    return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}
	
	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
	    return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}
	
}
