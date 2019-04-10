package org.evosuite.ga.operators.ranking;

import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.testcase.TestChromosome;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class implements a "fast" version of the variant of the crowding distance named "epsilon-dominance-assignment"
 * proposed by K\"{o}ppen and Yoshida in :
 * [1] Mario K\"{o}ppen and Kaori Yoshida, "Substitute Distance Assignments in NSGA-II for handling Many-objective
 * Optimization Problems", Evolutionary Multi-Criterion Optimization, Volume 4403 of the series Lecture Notes
 * in Computer Science pp 727-741.
 *
 * @author Annibale Panichella
 */

public class EpsilonDominance<T extends Chromosome> extends SecondaryRanking<T> {

    private static final long serialVersionUID = 4070641831294605290L;

    /**
     *  @param front front of non-dominated solutions/tests
     *  @param set set of goals/targets (e.g., branches) to consider
     */
    @Override
    public void assignSecondaryRank(List<T> front, Set<FitnessFunction<T>> set) {
        double value;
        for (T test : front){
            test.setDistance(0);
        }

        for (final FitnessFunction<T> ff : set) {
            double min = Double.POSITIVE_INFINITY;
            List<T> minSet = new ArrayList<T>(front.size());
            double max = 0;
            for (T test : front){
                value = test.getFitness(ff);
                if (value < min){
                    min = value;
                    minSet.clear();
                    minSet.add(test);
                } else if (value == min)
                    minSet.add(test);

                if (value > max){
                    max = value;
                }
            }

            //if (max == min)
            //	continue;

            for (T test : minSet){
                double numer = (front.size() - minSet.size());
                double demon = front.size();
                test.setDistance(Math.max(test.getDistance(), numer/demon));
            }
        }
    }
}
