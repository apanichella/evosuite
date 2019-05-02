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

    private static final Logger logger = LoggerFactory.getLogger(EuclideanDistance.class);

    @Override
    public void assignSecondaryRank(List<T> front, Set<FitnessFunction<T>> set){
        double[][] distances = new double[front.size()][front.size()];

        for (int i=0; i<front.size()-1; i++){
            TestChromosome tch1 =  (TestChromosome) front.get(i);
            ExecutionResult result1 = tch1.getLastExecutionResult();
            Map<Integer, Integer> frequency1 = result1.getTrace().getNoExecutionForConditionalNode();

            for (int j=i+1; j<front.size(); j++){
                TestChromosome tch2 =  (TestChromosome) front.get(j);
                ExecutionResult result2 = tch2.getLastExecutionResult();
                Map<Integer, Integer> frequency2 = result2.getTrace().getNoExecutionForConditionalNode();

                HashSet<Integer> union = new HashSet<>();
                union.addAll(frequency1.keySet());
                union.addAll(frequency2.keySet());

                for (Integer branch_id : union){
                    if (!frequency1.containsKey(branch_id))
                        distances[i][j] += Math.pow(frequency2.get(branch_id), 2);
                    else if (!frequency2.containsKey(branch_id))
                        distances[i][j] += Math.pow(frequency1.get(branch_id), 2);
                    else
                        distances[i][j] += Math.pow(frequency1.get(branch_id)-frequency2.get(branch_id), 2);
                }
                distances[j][i] = distances[i][j];
            }

        }
        for (int i=0; i<front.size(); i++){
            T t = front.get(i);
            double min = Double.MAX_VALUE;
            for (int j=0; j<front.size(); j++){
                if (i==j)
                    continue;

                min = Math.min(distances[i][j], min);
            }
            t.setDistance(min/(min+1));
        }
        //for (T test : front){
        //    logger.error("Test {} with distance {}", test.hashCode(), test.getDistance());
        //}
    }
}
