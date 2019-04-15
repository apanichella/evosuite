/**
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.ga.metaheuristics.mosa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.evosuite.Properties;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.comparators.OnlyCrowdingComparator;
import org.evosuite.ga.metaheuristics.mosa.structural.MultiCriteriaManager;
import org.evosuite.ga.metaheuristics.mosa.structural.StructuralGoalManager;
import org.evosuite.ga.operators.ranking.*;
import org.evosuite.utils.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.evosuite.Properties.SecondaryRankingType.CROWDING_DISTANCE;

/**
 * Implementation of the DynaMOSA (Many Objective Sorting Algorithm) described in the paper
 * "Automated Test Case Generation as a Many-Objective Optimisation Problem with Dynamic Selection
 * of the Targets".
 * 
 * @author Annibale Panichella, Fitsum M. Kifetew, Paolo Tonella
 */
public class DynaMOSA<T extends Chromosome> extends AbstractMOSA<T> {

	private static final long serialVersionUID = 146182080947267628L;

	private static final Logger logger = LoggerFactory.getLogger(DynaMOSA.class);

	/** Manager to determine the test goals to consider at each generation */
	protected StructuralGoalManager<T> goalsManager = null;

	protected SecondaryRanking distance = null;

	/**
	 * Constructor based on the abstract class {@link AbstractMOSA}.
	 * 
	 * @param factory
	 */
	public DynaMOSA(ChromosomeFactory<T> factory) {
		super(factory);
	}

	/** {@inheritDoc} */
	@Override
	protected void evolve() {
		List<T> offspringPopulation = this.breedNextGeneration();

		// Create the union of parents and offSpring
		List<T> union = new ArrayList<T>(this.population.size() + offspringPopulation.size());
		union.addAll(this.population);
		union.addAll(offspringPopulation);

		// Ranking the union
		logger.debug("Union Size = {}", union.size());

		// Ranking the union using the best rank algorithm (modified version of the non dominated sorting algorithm
		this.rankingFunction.computeRankingAssignment(union, this.goalsManager.getCurrentGoals());

		// let's form the next population using "preference sorting and non-dominated sorting" on the
		// updated set of goals
		int remain = Math.max(Properties.POPULATION, this.rankingFunction.getSubfront(0).size());
		int index = 0;
		List<T> front = null;
		this.population.clear();

		// Obtain the next front
		front = this.rankingFunction.getSubfront(index);

		while ((remain > 0) && (remain >= front.size()) && !front.isEmpty()) {
			// Assign crowding distance to individuals
			this.distance.assignSecondaryRank(front, this.goalsManager.getCurrentGoals());

			// Add the individuals of this front
			this.population.addAll(front);

			// Decrement remain
			remain = remain - front.size();

			// Obtain the next front
			index++;
			if (remain > 0) {
				front = this.rankingFunction.getSubfront(index);
			}
		}

		// Remain is less than front(index).size, insert only the best one
		if (remain > 0 && !front.isEmpty()) { // front contains individuals to insert
			this.distance.assignSecondaryRank(front, this.goalsManager.getCurrentGoals());
			Collections.sort(front, new OnlyCrowdingComparator());
			for (int k = 0; k < remain; k++) {
				this.population.add(front.get(k));
			}

			remain = 0;
		}

		this.currentIteration++;
		//logger.debug("N. fronts = {}", rankingFunction.getNumberOfSubfronts());
		//logger.debug("1* front size = {}", rankingFunction.getSubfront(0).size());
		logger.error("Covered goals = {}", goalsManager.getCoveredGoals().size());
		logger.error("Current goals = {}", goalsManager.getCurrentGoals().size());
		logger.error("Uncovered goals = {}", goalsManager.getUncoveredGoals().size());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void generateSolution() {
		logger.debug("executing generateSolution function");

		this.goalsManager = new MultiCriteriaManager<>(this.fitnessFunctions);

		setSecondaryRanking();

		LoggingUtils.getEvoLogger().info("* Initial Number of Goals in DynMOSA = " +
				this.goalsManager.getCurrentGoals().size() +" / "+ this.getUncoveredGoals().size());

		logger.debug("Initial Number of Goals = " + this.goalsManager.getCurrentGoals().size());

		//initialize population
		if (this.population.isEmpty()) {
			this.initializePopulation();
		}

		// update current goals
		this.calculateFitness();

		// Calculate dominance ranks and crowding distance
		this.rankingFunction.computeRankingAssignment(this.population, this.goalsManager.getCurrentGoals());

		for (int i = 0; i < this.rankingFunction.getNumberOfSubfronts(); i++){
			this.distance.assignSecondaryRank(this.rankingFunction.getSubfront(i), this.goalsManager.getCurrentGoals());
		}

		// next generations
		while (!isFinished() && this.goalsManager.getUncoveredGoals().size() > 0) {
			this.evolve();
			this.notifyIteration();
		}

		this.notifySearchFinished();
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	protected void calculateFitness(T c) {
		this.goalsManager.calculateFitness(c);
		this.notifyEvaluation(c);
	}

	protected void setSecondaryRanking(){
		switch (Properties.SECONDARY_RANKING_TYPE){
			case CROWDING_DISTANCE: {
				this.distance = new CrowdingDistance();
				break;
			}
			case SUBVECTOR_DOMINANCE: {
				this.distance = new SubvectorDominance();
				break;
			}
			case EPSILON_DOMINANCE: {
				this.distance = new EpsilonDominance();
				break;
			}
			case POTENTIAL_COVERAGE: {
				MultiCriteriaManager multi = ((MultiCriteriaManager<T>) this.goalsManager);
				this.distance = new PotentialCoverage(multi.getDependencies(), multi.getGraph());
				break;
			}
			case COVERAGE_DIVERSITY: {
				this.distance = new CoverageDiversity();
				break;
			}
		}
	}
}
