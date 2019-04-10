package org.evosuite.ga.operators.ranking;

import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.metaheuristics.mosa.AbstractMOSA;
import org.evosuite.ga.metaheuristics.mosa.structural.MultiCriteriaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PotentialCoverage<T extends Chromosome> extends SecondaryRanking<T> {

    private static final long serialVersionUID = -833503379499088402L;

    private Map<BranchCoverageTestFitness, Set<FitnessFunction<T>>> dependecies;

    private static final Logger logger = LoggerFactory.getLogger(PotentialCoverage.class);

    public PotentialCoverage(Map<BranchCoverageTestFitness, Set<FitnessFunction<T>>> pDepencencies){
        this.dependecies = pDepencencies;
    }

    @Override
    public void assignSecondaryRank(List<T> front, Set<FitnessFunction<T>> set){
        for (T t : front){
            double dependentBranches = 0;
            for (FitnessFunction<T> f : set){
                if (f instanceof BranchCoverageTestFitness){
                    if (t.getFitness(f) < 1){
                        dependentBranches += this.dependecies.get(f).size();
                    }
                }
            }
            t.setDistance(dependentBranches/(dependentBranches+1));
            //logger.error("Test {} with distance {}", t.hashCode(), t.getDistance());
        }
    }
}
