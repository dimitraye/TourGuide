package tourGuide.user;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import tripPricer.Provider;

public class User {
	private UUID USER_ID;
	private final String userName;
	private String phoneNumber;
	private String emailAddress;
	private Date latestLocationTimestamp;
	private List<VisitedLocation> visitedLocations = new ArrayList<>();
	private List<UserReward> userRewards = new ArrayList<>();
	private UserPreferences userPreferences = new UserPreferences();
	private List<Provider> tripDeals = new ArrayList<>();
	public User(UUID USER_ID, String userName, String phoneNumber, String emailAddress) {
		this.USER_ID = USER_ID;
		this.userName = userName;
		this.phoneNumber = phoneNumber;
		this.emailAddress = emailAddress;
	}
	
	public UUID getUSER_ID() {
		return USER_ID;
	}

	public void setUSER_ID(UUID user_id) {
		this.USER_ID = user_id;
	}
	
	public String getUserName() {
		return userName;
	}
	
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	
	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}
	
	public String getEmailAddress() {
		return emailAddress;
	}
	
	public void setLatestLocationTimestamp(Date latestLocationTimestamp) {
		this.latestLocationTimestamp = latestLocationTimestamp;
	}
	
	public Date getLatestLocationTimestamp() {
		return latestLocationTimestamp;
	}
	
	public void addToVisitedLocations(VisitedLocation visitedLocation) {
		visitedLocations.add(visitedLocation);
	}
	
	public List<VisitedLocation> getVisitedLocations() {
		return visitedLocations;
	}
	
	public void clearVisitedLocations() {
		visitedLocations.clear();
	}

	/**
	 * Ajout d'un userReward à la liste de reward d'un user
	 * @param newUserReward
	 */
	public void addUserReward(UserReward newUserReward) {
		if(attractionCouldBeRewarded(newUserReward.attraction)) {
			userRewards.add(newUserReward);
		}
	}

	/**
	 * if the attraction is not already in the list userRewards then add this attraction into the list
	 * @param attraction
	 * @return boolean
	 */
	public boolean attractionCouldBeRewarded(Attraction attraction) {
		List<UserReward> userRewardList = new CopyOnWriteArrayList(userRewards);
		return userRewardList.stream().noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName));
	}
	
	public List<UserReward> getUserRewards() {
		return userRewards;
	}
	
	public UserPreferences getUserPreferences() {
		return userPreferences;
	}
	
	public void setUserPreferences(UserPreferences userPreferences) {
		this.userPreferences = userPreferences;
	}

	public VisitedLocation getLastVisitedLocation() {
		return visitedLocations.get(visitedLocations.size() - 1);
	}
	
	public void setTripDeals(List<Provider> tripDeals) {
		this.tripDeals = tripDeals;
	}
	
	public List<Provider> getTripDeals() {
		return tripDeals;
	}

}
