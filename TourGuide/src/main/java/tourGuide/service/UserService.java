package tourGuide.service;

import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tourGuide.helper.InternalTestHelper;
import tourGuide.model.user.User;
import tourGuide.model.user.UserPreferences;
import tourGuide.model.user.UserReward;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class UserService {
	private Logger logger = LoggerFactory.getLogger(UserService.class);

	public UserService() {}



	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	public boolean updateUserPreferences(String userName, UserPreferences userPreferences) {
		User user = getUser(userName);
		user.setUserPreferences(userPreferences);
		return true;
	}
	
	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}
	
	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}
	

	public User addUser(User user) {
		if(!internalUserMap.containsKey(user.getUserName())) {
			user.setUserId(UUID.randomUUID());
			generateUserLocationHistory(user);

			internalUserMap.put(user.getUserName(), user);

			return user;
		}
		return null;
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
				allCurrentLocations.put(user.getUserId(), location);
			}
		});

		return allCurrentLocations;
	}
	
	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/

	// Database connection will be used for external users, but for testing purposes internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();
	void initializeInternalUsers() {
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
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(), new Location(generateRandomLatitude(),
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
