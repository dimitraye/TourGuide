package tourGuide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.user.User;
import tourGuide.user.UserReward;

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
    private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;

	ExecutorService executorService = Executors.newFixedThreadPool(44);


	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}
	
	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}
	
	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}

	public void calculateRewards(User user) {
		List<Attraction> attractions = new CopyOnWriteArrayList<>();
		List<VisitedLocation> visitedLocations = new CopyOnWriteArrayList<>();
		List<UserReward> userRewards = new CopyOnWriteArrayList<>();


		attractions.addAll(gpsUtil.getAttractions());
		visitedLocations.addAll(user.getVisitedLocations());
		userRewards.addAll(user.getUserRewards());

		List<CompletableFuture<User>> completableFutures = new ArrayList<>(); //List to hold all the completable futures

		visitedLocations.forEach(visitedLocation -> {
			attractions.parallelStream().forEach(attraction -> {
				if (user.attractionCouldBeRewarded(attraction)) {

					if (nearAttraction(visitedLocation, attraction)) {
						CompletableFuture<User> future = CompletableFuture.supplyAsync(() ->
										getRewardPoints(attraction, user), executorService)
								.thenApply(points -> {
									UserReward userReward = new UserReward(visitedLocation, attraction, points);
									user.addUserReward(userReward);
									return user;
								});
						completableFutures.add(future);
					}
				}
			});
		});

		CompletableFuture.allOf(completableFutures
						.stream().filter(Objects::nonNull)
						.collect(Collectors.toList())
						.toArray(new CompletableFuture[0]))
				.join();
	}
	
	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}
	
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}
	
	public int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUSER_ID());
	}
	
	public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        return statuteMiles;
	}

}
