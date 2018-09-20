package org.evosuite.ga.metaheuristics.mosa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.evosuite.Properties;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.comparators.OnlyCrowdingComparator;
import org.evosuite.ga.metaheuristics.mosa.structural.MultiCriteriatManager;
import org.evosuite.ga.metaheuristics.mosa.structural.StructuralGoalManager;
import org.evosuite.ga.operators.ranking.CrowdingDistance;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the DynaMOSA (Many Objective Sorting Algorithm) described in the TSE'17 paper ...
 * 
 * @author Annibale, Fitsum
 *
 * @param <T>
 */
public class DynaMOSA<T extends Chromosome> extends AbstractMOSA<T> {

	private static final long serialVersionUID = 146182080947267628L;

	private static final Logger logger = LoggerFactory.getLogger(DynaMOSA.class);

	/** Manager to determine the test goals to consider at each generation */
	protected StructuralGoalManager<T> goalsManager = null;

	protected CrowdingDistance<T> distance = new CrowdingDistance<T>();

	public DynaMOSA(ChromosomeFactory<T> factory) {
		super(factory);
	}

	/** {@inheritDoc} */
	@Override
	protected void evolve() {
		List<T> offspringPopulation = breedNextGeneration();

		// Create the union of parents and offSpring
		List<T> union = new ArrayList<T>(population.size()+offspringPopulation.size());
		union.addAll(population);
		union.addAll(offspringPopulation);

		// Ranking the union
		logger.debug("Union Size = {}", union.size());

		// Ranking the union using the best rank algorithm (modified version of the non dominated sorting algorithm
		this.rankingFunction.computeRankingAssignment(union, goalsManager.getCurrentGoals());

		// let's form the next population using "preference sorting and non-dominated sorting" on the
		// updated set of goals
		int remain = Math.max(Properties.POPULATION, this.rankingFunction.getSubfront(0).size());
		int index = 0;
		List<T> front = null;
		population.clear();

		// Obtain the next front
		front = this.rankingFunction.getSubfront(index);

		while ((remain > 0) && (remain >= front.size())) {
			// Assign crowding distance to individuals
			distance.fastEpsilonDominanceAssignment(front, goalsManager.getCurrentGoals());

			// Add the individuals of this front
			population.addAll(front);

			// Decrement remain
			remain = remain - front.size();

			// Obtain the next front
			index++;
			if (remain > 0) {
				front = this.rankingFunction.getSubfront(index);
			} // if
		} // while

		// Remain is less than front(index).size, insert only the best one
		if (remain > 0) { // front contains individuals to insert
			distance.fastEpsilonDominanceAssignment(front, goalsManager.getCurrentGoals());
			Collections.sort(front, new OnlyCrowdingComparator());
			for (int k = 0; k < remain; k++) {
				population.add(front.get(k));
			} // for

			remain = 0;
		} // if
		//for (T  p : population)
		//	logger.error("Rank {}, Distance {}", p.getRank(), p.getDistance());
		currentIteration++;
		logger.error("");
		//logger.error("N. fronts = {}", ranking.getNumberOfSubfronts());
		//logger.error("1* front size = {}", ranking.getSubfront(0).size());
		logger.error("Covered goals = {}", goalsManager.getCoveredGoals().size());
		logger.error("Current goals = {}", goalsManager.getCurrentGoals().size());
		logger.error("Uncovered goals = {}", goalsManager.getUncoveredGoals().size());
	}


	/** 
	 * This method computes the fitness scores only for the current goals
	 * @param c chromosome
	 */
	protected void calculateFitness(T c) {
		goalsManager.calculateFitness(c);
		notifyEvaluation(c);
	}

	/** 
	 * This method computes the fitness scores for all (covered and uncovered) goals
	 * @param c chromosome
	 */
	protected void completeCalculateFitness(T c) {
		for (FitnessFunction<T> fitnessFunction : this.goalsManager.getCoveredGoals().keySet()) {
			if (!c.getFitnessValues().containsKey(fitnessFunction))
				c.getFitness(fitnessFunction);
			//notifyEvaluation(c);
		}
		for (FitnessFunction<T> fitnessFunction : this.goalsManager.getCurrentGoals()) {
			if (!c.getFitnessValues().containsKey(fitnessFunction))
				c.getFitness(fitnessFunction);
			//notifyEvaluation(c);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void generateSolution() {
		logger.info("executing generateSolution function");

		goalsManager = new MultiCriteriatManager<T>(fitnessFunctions);

		LoggingUtils.getEvoLogger().info("\n Initial Number of Goals = "+goalsManager.getCurrentGoals().size());

		//initialize population
		if (population.isEmpty())
			initializePopulation();

		// update current goals
		calculateFitness();

		// Calculate dominance ranks and crowding distance
		this.rankingFunction.computeRankingAssignment(population, goalsManager.getCurrentGoals());

		for (int i = 0; i<this.rankingFunction.getNumberOfSubfronts(); i++){
			distance.fastEpsilonDominanceAssignment(this.rankingFunction.getSubfront(i), goalsManager.getCurrentGoals());
		}
		// next generations
		while (!isFinished() && goalsManager.getUncoveredGoals().size() > 0) {
			evolve();
			notifyIteration();
		}
		//completeCalculateFitness();
		notifySearchFinished();
	}

	protected void completeCalculateFitness() {
		logger.debug("Calculating fitness for " + population.size() + " individuals");
		for (T c : goalsManager.getCoveredGoals().values()){
			completeCalculateFitness(c);
		}
	}

	/** This method return the test goals covered by the test cases stored in the current archive **/
	public Set<FitnessFunction<T>> getCoveredGoals() {
		return goalsManager.getCoveredGoals().keySet();
	}

	protected List<T> getArchive() {
		//Set<T> tests = new HashSet<T>();
		//tests.addAll(goalsManager.getCoveredGoals().values());
		List<T> suite = new ArrayList<T>(goalsManager.getArchive());
		return suite;
	}

	protected List<T> getFinalTestSuite() {
		// trivial case where there are no branches to cover or the archive is empty
		List<T> archive = getArchive();
		if (archive.size() == 0){
			if (population.size() > 0) {
				ArrayList<T> list = new ArrayList<T>(population.size());
				list.add(population.get(population.size() - 1));
				return list;
			} else
				return archive;
		}
		//List<T>[] rank=this.nonDominatedSorting(archive);
		return archive;
	}

	@Override @SuppressWarnings("unchecked")
	public T getBestIndividual() {
		TestSuiteChromosome best = new TestSuiteChromosome();
		for (T test : getArchive()) {
			best.addTest((TestChromosome) test);
		}

		// compute overall fitness and coverage
		this.computeCoverageAndFitness(best);
		//suiteFitness.getFitness(best);
		return (T) best;
	}

	protected double numberOfCoveredTargets(){
		return this.goalsManager.getCoveredGoals().size();
	}

}
