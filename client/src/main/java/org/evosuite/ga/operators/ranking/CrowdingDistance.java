/**
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.ga.operators.ranking;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.comparators.SortByFitness;
import org.evosuite.testcase.TestChromosome;

/**
 * This class implements the classic Crowding Distance in NSGA-II
 *
 * @author Annibale Panichella
 */
public class CrowdingDistance<T extends Chromosome> extends SecondaryRanking<T> {

	private static final long serialVersionUID = 5700682318003298299L;

	/**
	 * Method used to assign the 'traditional' Crowding Distance.
	 * 
	 * @param front front of non-dominated solutions/tests
	 * @param set list of goals/targets (e.g., branches) to consider
	 */
	public void assignSecondaryRank(List<T> front, Set<FitnessFunction<T>> set)  {
		int size = front.size();

		if (size == 0)
			return;
		if (size == 1) {
			front.get(0).setDistance(Double.POSITIVE_INFINITY);
			return;
		}
		if (size == 2) {
			front.get(0).setDistance(Double.POSITIVE_INFINITY);
			front.get(1).setDistance(Double.POSITIVE_INFINITY);
			return;
		}

		for (int i = 0; i < size; i++)
			front.get(i).setDistance(0.0);

		double objetiveMaxn;
		double objetiveMinn;
		double distance;

		for (final FitnessFunction<?> ff : set) {
			// Sort the population by Fit n
			Collections.sort(front, new SortByFitness(ff, false));

			objetiveMinn = front.get(0).getFitness(ff);
			objetiveMaxn = front.get(front.size() - 1).getFitness(ff);

			// set crowding distance
			front.get(0).setDistance(Double.POSITIVE_INFINITY);
			front.get(size - 1).setDistance(Double.POSITIVE_INFINITY);

			for (int j = 1; j < size - 1; j++) {
				distance = front.get(j + 1).getFitness(ff) - front.get(j - 1).getFitness(ff);
				distance = distance / (objetiveMaxn - objetiveMinn);
				distance += front.get(j).getDistance();
				front.get(j).setDistance(distance);
			}
		}
	}


}
