package org.evosuite.ga.operators.ranking;

import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.testcase.TestChromosome;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CoverageDiversity<T extends Chromosome> extends SecondaryRanking<T> {

    private static final long serialVersionUID = -5899577241884258042L;

    @Override
    public void assignSecondaryRank(List<T> front, Set<FitnessFunction<T>> set){
        for (T t1 : front){
            TestChromosome tch1 =  (TestChromosome) t1;
            double difference = Double.MAX_VALUE;
            for(T t2 : front){
                if (t1 == t2)
                    continue;

                TestChromosome tch2 =  (TestChromosome) t2;

                Set set1 = new HashSet(tch1.getLastExecutionResult().getTrace().getCoveredLines());
                Set set2 = new HashSet(tch2.getLastExecutionResult().getTrace().getCoveredLines());

                set1.removeAll(set2);

                difference = Math.min(difference, set1.size());

            }
            tch1.setDistance(difference/(difference+1));
        }

    }
}
