package tourGuide;

import static org.junit.Assert.assertTrue;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import org.springframework.context.i18n.LocaleContextHolder;
import rewardCentral.RewardCentral;
import tourGuide.helper.InternalTestHelper;
import tourGuide.service.RewardsService;
import tourGuide.service.TourGuideService;
import tourGuide.user.User;
import tourGuide.user.UserReward;

public class TestPerformance {
	
	/*
	 * A note on performance improvements:
	 *     
	 *     The number of users generated for the high volume tests can be easily adjusted via this method:
	 *     
	 *     		InternalTestHelper.setInternalUserNumber(100000);
	 *     
	 *     
	 *     These tests can be modified to suit new solutions, just as long as the performance metrics
	 *     at the end of the tests remains consistent. 
	 * 
	 *     These are performance metrics that we are trying to hit:
	 *     
	 *     highVolumeTrackLocation: 100,000 users within 15 minutes:
	 *     		assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
     *
     *     highVolumeGetRewards: 100,000 users within 20 minutes:
	 *          assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	 */
	
	//@Ignore
	@Test
	public void highVolumeTrackLocation() {
		LocaleContextHolder.getLocale().setDefault(new Locale("en", "US"));

		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		// Users should be incremented up to 100,000, and test finishes within 15 minutes
		int nbUsers = 100;
		InternalTestHelper.setInternalUserNumber(nbUsers);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		List<User> allUsers = new ArrayList<>();
		allUsers = tourGuideService.getAllUsers();

	    StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		//Paralelisation de la tâche sur plusieurs threads afin de réduire le temps de calcule du résultat
		allUsers.parallelStream().forEach(user -> tourGuideService.trackUserLocation(user));

		stopWatch.stop();
		tourGuideService.tracker.stopTracking();

		System.out.println("highVolumeTrackLocation: Time Elapsed: " +
				TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");

		//On vérifie combien de temps cela nous a pris d'exécuter la tache par rapport au temps maximal donné
		long maxTime = TimeUnit.MINUTES.toSeconds(15);
		long endTime = TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime());
		boolean result = maxTime >= endTime;
		//long timeNUsers = endTime * ()
		long time100k = endTime * (100000 / nbUsers);
		boolean result100k = maxTime >= time100k;
		System.out.println(String.format("result100K (%s) : maxTime (%s)", time100k , maxTime));
		//System.out.println(String.format("result (%s) : (%s)", time100k , result));

		assertTrue(result);
	}


	@Test
	public void highVolumeGetRewards() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

		// Users should be incremented up to 100,000, and test finishes within 20 minutes
		long maxTime = TimeUnit.MINUTES.toSeconds(20);
		int nbUsers = 100;

		InternalTestHelper.setInternalUserNumber(nbUsers);

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		Attraction attraction = gpsUtil.getAttractions().get(0);
		List<User> allUsers = new ArrayList<>();
		allUsers.addAll(tourGuideService.getAllUsers());

		allUsers.forEach(u -> u.addToVisitedLocations(new VisitedLocation(u.getUSER_ID(), attraction, new Date())));

		allUsers.parallelStream().forEach(u -> rewardsService.calculateRewards(u));

		for(User user : allUsers) {
			assertTrue(user.getUserRewards().size() > 0);
		}
		stopWatch.stop();
		tourGuideService.tracker.stopTracking();

		long actualTime = TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime());
		boolean result = maxTime >= actualTime;
		long time100mill = actualTime * (100000/nbUsers);
		boolean result100mill = maxTime >= time100mill;
		System.out.println("highVolumeTrackLocation: Time Elapsed for " + allUsers.size() + ": " + actualTime + " seconds.");
		System.out.println("highVolumeTrackLocation: Time Elapsed for 100 000: " + time100mill + " seconds.");
		System.out.println("TimeUnit.MINUTES.toSeconds(20): " + maxTime + " seconds.");

		System.out.println("actual time for 10 000 user < to maxtime for 20mn : " + result);
		System.out.println("actual time for 100000 user < to maxtime for 20mn : " + result100mill);

	}
}
