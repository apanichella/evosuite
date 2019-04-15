package org.evosuite.ga.operators.ranking;

import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.testcase.TestChromosome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CoverageDiversity<T extends Chromosome> extends SecondaryRanking<T> {

    private static final long serialVersionUID = -5899577241884258042L;

    private static final Logger logger = LoggerFactory.getLogger(CoverageDiversity.class);

    @Override
    public void assignSecondaryRank(List<T> front, Set<FitnessFunction<T>> set){
        for (T t1 : front){
            TestChromosome tch1 =  (TestChromosome) t1;
            double difference = Double.MAX_VALUE;
            for(T t2 : front){
                if (t1 == t2)
                    continue;

                TestChromosome tch2 =  (TestChromosome) t2;

                Set set_diff = new HashSet(tch1.getLastExecutionResult().getTrace().getCoveredTrueBranches());
                set_diff.addAll(tch1.getLastExecutionResult().getTrace().getCoveredFalseBranches());
                set_diff.addAll(tch1.getLastExecutionResult().getTrace().getCoveredBranchlessMethods());

                Set union = new HashSet(tch2.getLastExecutionResult().getTrace().getCoveredTrueBranches());
                union.addAll(tch2.getLastExecutionResult().getTrace().getCoveredFalseBranches());
                union.addAll(tch2.getLastExecutionResult().getTrace().getCoveredBranchlessMethods());

                set_diff.removeAll(union);

                union.addAll(set_diff);

                difference = Math.min(difference, set_diff.size());
                //difference = difference / set.size();

            }
            tch1.setDistance(difference);
        }

        //for (T test : front){
        //    logger.error("Test {} with distance {}", test.hashCode(), test.getDistance());
        //}
    }
}
