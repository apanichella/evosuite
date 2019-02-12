package org.evosuite.ga.metaheuristics.mosa;

import com.examples.with.different.packagename.XMLElement2;
import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.evosuite.SystemTestBase;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.result.TestGenerationResult;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author Annibale Panichella
 *
 * System-level test for MOSA
 */
public class MOSASystemTest extends SystemTestBase {

    public TestGenerationResult setup(Properties.StoppingCondition sc, int budget, String cut){
        Properties.CRITERION = new Properties.Criterion[1];
        Properties.CRITERION[0] = Properties.Criterion.BRANCH;
        Properties.ALGORITHM = Properties.Algorithm.MOSA;
        Properties.STRATEGY = Properties.Strategy.MOSUITE;
        Properties.POPULATION = 50;
        Properties.ARCHIVE_TYPE = Properties.ArchiveType.COVERAGE;
        Properties.STOPPING_CONDITION = sc;
        Properties.SEARCH_BUDGET = budget;

        EvoSuite evosuite = new EvoSuite();

        String targetClass = cut;
        Properties.TARGET_CLASS = targetClass;

        String[] command = new String[] {"-generateMOSuite", "-class", targetClass};

        Object result = evosuite.parseCommandLine(command);
        Assert.assertNotNull(result);

        return getResult(result);
    }

    @Test
    public void testMOSAWithLimitedTime(){
        TestGenerationResult result = this.setup(Properties.StoppingCondition.MAXTIME, 20, XMLElement2.class.getCanonicalName());
        Assert.assertTrue(result.getGeneticAlgorithm() instanceof MOSA);

        MOSA mosa = (MOSA) result.getGeneticAlgorithm();
        System.out.println(mosa.budgetMonitor.getTime2MaxCoverage());
        Assert.assertTrue(mosa.budgetMonitor.getTime2MaxCoverage()<22000);
        Assert.assertTrue(mosa.budgetMonitor.getTime2MaxCoverage()>10000);

        int coveredTargets = coveredTargets(mosa);

        Assert.assertEquals(202, mosa.getFitnessFunctions().size());
        Assert.assertTrue(coveredTargets > 0);

        List<Chromosome> population = mosa.getBestIndividuals();
        for (Chromosome p : population) {
            Assert.assertEquals(1.0, p.getCoverage(), 0.000001);
        }
    }

    @Test
    public void testMOSAWithLimitedGenerations(){
        TestGenerationResult result = this.setup(Properties.StoppingCondition.MAXGENERATIONS, 20, XMLElement2.class.getCanonicalName());
        Assert.assertTrue(result.getGeneticAlgorithm() instanceof MOSA);

        MOSA mosa = (MOSA) result.getGeneticAlgorithm();
        Assert.assertEquals(19, mosa.getAge());

        int coveredTargets = coveredTargets(mosa);

        Assert.assertEquals(202, mosa.getFitnessFunctions().size());
        Assert.assertTrue(coveredTargets > 0);

        List<Chromosome> population = mosa.getBestIndividuals();
        for (Chromosome p : population) {
            Assert.assertEquals(1.0, p.getCoverage(), 0.000001);
        }
    }

    // ---- Utility methods

    protected TestGenerationResult getResult(Object result) {
        assert(result instanceof List);
        List<List<TestGenerationResult>> results = (List<List<TestGenerationResult>>)result;
        assert(results.size() == 1);
        //return results.iterator().next().getGeneticAlgorithm();
        return results.get(0).get(0);
    }

    public int coveredTargets(AbstractMOSA mosa){
        TestSuiteChromosome suite = (TestSuiteChromosome) mosa.getBestIndividuals().get(0);
        List<TestChromosome> tests = suite.getTestChromosomes();

        int coveredTargets = 0;
        for (Object f : mosa.getFitnessFunctions()){
            FitnessFunction target = (FitnessFunction) f;
            for (TestChromosome t : tests){
                if (t.getFitness(target) == 0) {
                    coveredTargets++;
                    break;
                }
            }
        }
        return coveredTargets;
    }

}