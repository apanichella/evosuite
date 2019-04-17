package org.evosuite.ga.operators.ranking;

import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class EuclideanDistance<T extends Chromosome> extends SecondaryRanking<T> {

    private static final long serialVersionUID = 7955426596562779202L;

    private static final Logger logger = LoggerFactory.getLogger(CoverageDiversity.class);

    @Override
    public void assignSecondaryRank(List<T> front, Set<FitnessFunction<T>> set){
        double distance;

        for (T t1 : front){
            TestChromosome tch1 =  (TestChromosome) t1;
            ExecutionResult result1 = tch1.getLastExecutionResult();
            Map<Integer, Integer> frequency1 = result1.getTrace().getNoExecutionForConditionalNode();
            double difference = Double.MAX_VALUE;
            for(T t2 : front){
                if (t1 == t2)
                    continue;

                TestChromosome tch2 =  (TestChromosome) t2;
                ExecutionResult result2 = tch2.getLastExecutionResult();
                Map<Integer, Integer> frequency2 = result2.getTrace().getNoExecutionForConditionalNode();

                distance = 0;

                HashSet<Integer> union = new HashSet<>();
                union.addAll(frequency1.keySet());
                union.addAll(frequency2.keySet());

                for (Integer branch_id : union){
                    if (!frequency1.containsKey(branch_id))
                        distance += Math.pow(frequency2.get(branch_id), 2);
                    else if (!frequency2.containsKey(branch_id))
                        distance += Math.pow(frequency1.get(branch_id), 2);
                    else
                        distance += Math.pow(frequency1.get(branch_id)-frequency2.get(branch_id), 2);
                }

                difference = Math.min(difference, distance);
                difference = difference / set.size();

            }
            tch1.setDistance(difference);
        }

        //for (T test : front){
        //    logger.error("Test {} with distance {}", test.hashCode(), test.getDistance());
        //}
    }
}
