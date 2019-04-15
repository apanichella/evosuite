package org.evosuite.ga.operators.ranking;

import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.metaheuristics.mosa.AbstractMOSA;
import org.evosuite.ga.metaheuristics.mosa.structural.BranchFitnessGraph;
import org.evosuite.ga.metaheuristics.mosa.structural.MultiCriteriaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

public class PotentialCoverage<T extends Chromosome> extends SecondaryRanking<T> {

    private static final long serialVersionUID = -833503379499088402L;

    private Map<BranchCoverageTestFitness, Set<FitnessFunction<T>>> dependecies;

    private BranchFitnessGraph graph;

    private static final Logger logger = LoggerFactory.getLogger(PotentialCoverage.class);

    public PotentialCoverage(Map<BranchCoverageTestFitness, Set<FitnessFunction<T>>> pDependencies, BranchFitnessGraph pGraph){
        this.dependecies = pDependencies;
        this.graph = pGraph;
        //for (BranchCoverageTestFitness key : pDependencies.keySet()){
        //    Set<FitnessFunction<T>> targets = pDependencies.get(key);
        //    int nBranches = 0;
        //    for (FitnessFunction<T> target : targets){
        //        if (target instanceof BranchCoverageTestFitness)
        //            nBranches++;
        //    }
        //    this.dependecies.put(key, nBranches);
        //}
        //for (BranchCoverageTestFitness key : graph.
    }

    @Override
    public void assignSecondaryRank(List<T> front, Set<FitnessFunction<T>> set){
        double dependentTargets;

        for (T t : front){
            t.setDistance(0);
        }

        double value;
        for (final FitnessFunction<T> ff : set) {

            if (!(ff instanceof BranchCoverageTestFitness)) {
                continue;
            }

            double min = Double.POSITIVE_INFINITY;
            List<T> min_test = new ArrayList<>();
            for (T test : front) {
                value = test.getFitness(ff);
                if (value == min) {
                    min_test.add(test);
                } if (value < min) {
                    min = value;
                    min_test.clear();
                    min_test.add(test);
                }
            }
            //logger.error("{} value {}", ff, min);
            for (T t : min_test) {
                dependentTargets = this.dependecies.get(ff).size();
                dependentTargets += this.graph.getStructuralChildren(ff).size();
                t.setDistance(t.getDistance() + dependentTargets);

            }
        }
        //for (T test : front){
        //    logger.error("Test {} with distance {}", test.hashCode(), test.getDistance());
        //}
    }
}
