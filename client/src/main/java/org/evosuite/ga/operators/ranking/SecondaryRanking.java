package org.evosuite.ga.operators.ranking;

import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public abstract class SecondaryRanking<T extends Chromosome> implements Serializable {

    private static final long serialVersionUID = 2442065831094890675L;

    public abstract void assignSecondaryRank(List<T> front, Set<FitnessFunction<T>> set);
}
