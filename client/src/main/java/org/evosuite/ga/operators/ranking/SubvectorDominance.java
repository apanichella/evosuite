package org.evosuite.ga.operators.ranking;

import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.testcase.TestChromosome;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * This class implements a variant of the crowding distance named "subvector-dominance-assignment"
 * proposed by K\"{o}ppen and Yoshida in :
 * [1] Mario K\"{o}ppen and Kaori Yoshida, "Substitute Distance Assignments in NSGA-II for handling Many-objective
 * Optimization Problems", Evolutionary Multi-Criterion Optimization, Volume 4403 of the series Lecture Notes
 * in Computer Science pp 727-741.
 *
 * @author Annibale Panichella
 */

public class SubvectorDominance<T extends Chromosome> extends SecondaryRanking<T> {

    private static final long serialVersionUID = 3982285667605041037L;

    /**
     * @param front front of non-dominated solutions/tests
     * @param set set of goals/targets (e.g., branches) to consider
     */
    @Override
    public void assignSecondaryRank(List<T> front, Set<FitnessFunction<T>> set) {
        int size = front.size();
        if (front.size() == 1){
            front.get(0).setDistance(Double.POSITIVE_INFINITY);
            return;
        }

        for (int i = 0; i < size; i++)
            front.get(i).setDistance(Double.MAX_VALUE);

        int dominate1, dominate2;
        for (int i = 0; i<front.size()-1; i++){
            T p1 = front.get(i);
            for (int j = i+1; j<front.size(); j++){
                T p2 = front.get(j);
                dominate1 = 0;
                dominate2 = 0;
                for (final FitnessFunction<T> ff : set) {
                    double value1 = p1.getFitness(ff);
                    double value2 = p2.getFitness(ff);
                    if (value1 < value2)
                        dominate1++;
                    else if (value1 > value2)
                        dominate2++;
                }
                p1.setDistance(Math.min(dominate1, p1.getDistance()));
                p2.setDistance(Math.min(dominate2, p2.getDistance()));
            }
        }
    }
}
