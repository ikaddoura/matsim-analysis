package org.matsim.analysis.modalSplitUserType;
/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.analysis.AgentAnalysisFilter;
import org.matsim.analysis.AgentFilter;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * 
 * @author ikaddoura
 *
 */
public class ModeAnalysis {
	
	private static final Logger log = Logger.getLogger(ModeAnalysis.class);	
	private Scenario scenario;
	private AgentFilter filter;
	
	private Map<String, Integer> mode2TripCounterFiltered = new HashMap<>();		
	private Map<String, List<Double>> mode2TripRouteDistancesFiltered = new HashMap<>();
	private Map<String, List<Double>> mode2TripEuclideanDistancesFiltered = new HashMap<>();		

	private double totalTripsFiltered = 0.;
	private final StageActivityTypes stageActivities;

	public ModeAnalysis(Scenario scenario, AgentFilter filter, StageActivityTypes stageActivities) {
		this.scenario = scenario;
		this.filter = filter;
		this.stageActivities = stageActivities;
	}

	public void run() {
		
		int counter = 0;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			
			if (counter % 1000 == 0) {
				log.info("Person #" + counter);
			}
			
			if (filter == null || filter.considerAgent(person)) {
												
				for (Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan().getPlanElements(), stageActivities)) {
					totalTripsFiltered++;

					double routeDistance = 0.;
					for (Leg leg : trip.getLegsOnly()) {
						routeDistance += leg.getRoute().getDistance();
					}
					
					AnalysisMainModeIdentifier modeIdentifier = new AnalysisMainModeIdentifier();
					String currentLegMode = modeIdentifier.identifyMainMode(trip.getTripElements());
					
					if (mode2TripCounterFiltered.containsKey(currentLegMode)) {
						
						mode2TripCounterFiltered.put(currentLegMode, mode2TripCounterFiltered.get(currentLegMode) + 1);
						mode2TripRouteDistancesFiltered.get(currentLegMode).add(routeDistance);
						
						double euclideanDistance = CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(), trip.getDestinationActivity().getCoord());
						mode2TripEuclideanDistancesFiltered.get(currentLegMode).add(euclideanDistance);
						
					} else {
						
						mode2TripCounterFiltered.put(currentLegMode, 1);
						
						List<Double> routeDistances = new ArrayList<>();
						routeDistances.add(routeDistance);
						mode2TripRouteDistancesFiltered.put(currentLegMode, routeDistances);
						
						List<Double> euclideanDistances = new ArrayList<>();
						double euclideanDistance = CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(), trip.getDestinationActivity().getCoord());
						euclideanDistances.add(euclideanDistance);
						mode2TripEuclideanDistancesFiltered.put(currentLegMode, euclideanDistances);
					}
				}
			} else {
				// skip person
			}			

			counter++;
		}
	}
	
	public Map<String, Integer> getMode2TripCounterFiltered() {
		return mode2TripCounterFiltered;
	}

	public void writeModeShares(String outputDirectory) {		
		String outputFileName;
		if (filter == null) {
			outputFileName = "tripModeAnalysis.csv";
		} else {
			outputFileName = "tripModeAnalysis" + filter.toFileName() + ".csv";
		}
		
		File file = new File(outputDirectory + outputFileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("mode ; number of trips ; trip share ");
			bw.newLine();
			
			for (String mode : this.mode2TripCounterFiltered.keySet()) {
				bw.write(mode + ";" + this.mode2TripCounterFiltered.get(mode) + ";" + (this.mode2TripCounterFiltered.get(mode) / this.totalTripsFiltered));
				bw.newLine();
			}
			
			bw.newLine();
			
			bw.close();
			log.info("Output written.");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeTripRouteDistances(String outputDirectory) {
		
		for (String mode : this.mode2TripRouteDistancesFiltered.keySet()) {
			String outputFileName;
			if (filter == null) {
				outputFileName = mode + "_tripRouteDistances.csv";
			} else {
				outputFileName = mode + "_tripRouteDistances" + filter.toFileName() + ".csv";
			}
			File file = new File(outputDirectory + outputFileName);

			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(file));
				bw.write("trip distance [m]");
				bw.newLine();
				
				for (Double distance : this.mode2TripRouteDistancesFiltered.get(mode)) {
					bw.write(String.valueOf(distance));
					bw.newLine();
				}
								
				bw.close();
				log.info("Output written.");
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void writeTripEuclideanDistances(String outputDirectory) {
		
		for (String mode : this.mode2TripEuclideanDistancesFiltered.keySet()) {
			String outputFileName;
			if (filter == null) {
				outputFileName = mode + "_tripEuclideanDistances.csv";
			} else {
				outputFileName = mode + "_tripEuclideanDistances" + filter.toFileName() + ".csv";
			}
			File file = new File(outputDirectory + outputFileName);

			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(file));
				bw.write("trip distance [m]");
				bw.newLine();
				
				for (Double distance : this.mode2TripEuclideanDistancesFiltered.get(mode)) {
					bw.write(String.valueOf(distance));
					bw.newLine();
				}
								
				bw.close();
				log.info("Output written.");
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void writeDistances(String outputDirectory, String outputFileName, List<Tuple<Double, Double>> distanceGroups, Map<String, List<Double>> mode2TripDistances) {
		
		if (mode2TripDistances == null || mode2TripDistances.isEmpty()) {
			log.info("mode2TripDistances is empty. " + outputDirectory + outputFileName + " will not be written.");
			
		} else {
			SortedMap<Integer, Map<String, Integer>> distanceGroupIndex2mode2trips = new TreeMap<>();	
			
			// initialize
			int index = 0;
			for (Tuple<Double, Double> distanceGroup : distanceGroups) {
				Map<String, Integer> mode2trips = new HashMap<>();

				for (String mode : mode2TripDistances.keySet()) {
					log.info("index: " + index + " - distance group: " + distanceGroup + " - mode: " + mode);

					mode2trips.put(mode, 0);
					distanceGroupIndex2mode2trips.put(index, mode2trips);
				}
				index++;
			}

			// fill
			for (String mode : mode2TripDistances.keySet()) {
				for (Double distance : mode2TripDistances.get(mode)) {
					
					for (Integer distanceGroupIndex : distanceGroupIndex2mode2trips.keySet()) {
				
						if (distance >= distanceGroups.get(distanceGroupIndex).getFirst() && distance < distanceGroups.get(distanceGroupIndex).getSecond()) {
							int tripsUpdated = distanceGroupIndex2mode2trips.get(distanceGroupIndex).get(mode) + 1;
							distanceGroupIndex2mode2trips.get(distanceGroupIndex).put(mode, tripsUpdated);
						}
					}
				}
			}

			// write
			
			File file = new File(outputDirectory + outputFileName);
			
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(file));
				bw.write("trip distance group index ; distance - from [m] ; distance to [m] ");
				
				for (String mode : distanceGroupIndex2mode2trips.get(0).keySet()) {
					bw.write(" ; " + mode);
				}
				bw.newLine();
				
				for (Integer distanceGroupIndex : distanceGroupIndex2mode2trips.keySet()) {
					bw.write( distanceGroupIndex + " ; " + distanceGroups.get(distanceGroupIndex).getFirst() + " ; " + distanceGroups.get(distanceGroupIndex).getSecond() );

					for (String mode : distanceGroupIndex2mode2trips.get(distanceGroupIndex).keySet()) {
						bw.write(" ; " + distanceGroupIndex2mode2trips.get(distanceGroupIndex).get(mode));
					}
					bw.newLine();
				}

				bw.close();
				log.info("Output written.");	
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void writeTripEuclideanDistances(String outputDirectory, List<Tuple<Double, Double>> distanceGroups) {
		
		String outputFileName;
		if (filter == null) {
			outputFileName = "tripsPerModeAndEuclideanDistanceGroup.csv";
		} else {
			outputFileName = "tripsPerModeAndEuclideanDistanceGroup" + filter.toFileName() + ".csv";
		}
		writeDistances(outputDirectory, outputFileName, distanceGroups, this.mode2TripEuclideanDistancesFiltered);
	}
	
	public void writeTripRouteDistances(String outputDirectory, List<Tuple<Double, Double>> distanceGroups) {
		
		String outputFileName;
		if (filter == null) {
			outputFileName = "tripsPerModeAndRouteDistanceGroup.csv";
		} else {
			outputFileName = "tripsPerModeAndRouteDistanceGroup" + filter.toFileName() + ".csv";
		}
		writeDistances(outputDirectory, outputFileName, distanceGroups, this.mode2TripRouteDistancesFiltered);

	}
		
}
