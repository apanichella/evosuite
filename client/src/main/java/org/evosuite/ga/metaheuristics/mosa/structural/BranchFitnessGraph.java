/**
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
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
package org.evosuite.ga.metaheuristics.mosa.structural;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.evosuite.Properties;
import org.evosuite.coverage.branch.Branch;
import org.evosuite.coverage.branch.BranchCoverageGoal;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.coverage.io.input.InputCoverageTestFitness;
import org.evosuite.coverage.io.output.OutputCoverageTestFitness;
import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.coverage.method.MethodCoverageTestFitness;
import org.evosuite.coverage.method.MethodNoExceptionCoverageTestFitness;
import org.evosuite.coverage.mutation.StrongMutationTestFitness;
import org.evosuite.coverage.mutation.WeakMutationTestFitness;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.graphs.cfg.ActualControlFlowGraph;
import org.evosuite.graphs.cfg.BasicBlock;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.utils.LoggingUtils;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.traverse.DepthFirstIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 
 * @author Annibale Panichella
 */
public class BranchFitnessGraph<T extends Chromosome, V extends FitnessFunction<T>> implements Serializable {

	private static final long serialVersionUID = -8020578778906420503L;
	
	private static final Logger logger = LoggerFactory.getLogger(BranchFitnessGraph.class);

	protected SimpleDirectedWeightedGraph<FitnessFunction<T>, DependencyEdge> graph = new SimpleDirectedWeightedGraph<FitnessFunction<T>, DependencyEdge>(DependencyEdge.class);
	
	protected Set<FitnessFunction<T>> rootBranches = new HashSet<FitnessFunction<T>>();

	@SuppressWarnings("unchecked")
	public BranchFitnessGraph(Collection<FitnessFunction<T>> goals){
		for (FitnessFunction<T> fitness : goals){
			graph.addVertex(fitness);
		}

		// derive dependencies among branches
		for (FitnessFunction<T> fitness : goals){
			if (! (fitness instanceof BranchCoverageTestFitness)) {
				continue;
			}
			Branch branch = ((BranchCoverageTestFitness) fitness).getBranch();
			if (branch==null){
				this.rootBranches.add(fitness); 
				continue;
			}

			if (branch.getInstruction().isRootBranchDependent()
					|| branch.getInstruction().isDirectlyControlDependentOn(null))
				this.rootBranches.add(fitness); 

			// see dependencies for all true/false branches
			ActualControlFlowGraph rcfg = branch.getInstruction().getActualCFG();
			Set<BasicBlock> visitedBlock = new HashSet<BasicBlock>();
			Set<BasicBlock> parents = lookForParent(branch.getInstruction().getBasicBlock(), rcfg, visitedBlock);
			for (BasicBlock bb : parents){
				Branch newB = extractBranch(bb);
				if (newB == null){
					this.rootBranches.add(fitness);
					continue;
				}
				
				BranchCoverageGoal goal = new BranchCoverageGoal(newB, true, newB.getClassName(), newB.getMethodName());
				BranchCoverageTestFitness newFitness = new BranchCoverageTestFitness(goal);
				addWeightedEdge((FitnessFunction<T>) newFitness, fitness);

				BranchCoverageGoal goal2 = new BranchCoverageGoal(newB, false, newB.getClassName(), newB.getMethodName());
				BranchCoverageTestFitness newfitness2 = new BranchCoverageTestFitness(goal2);
				addWeightedEdge((FitnessFunction<T>) newfitness2, fitness);
			}
		}
	}
	
	
	double determineWeight(FitnessFunction<T> target) {
		double weight = 1d;
		if (target instanceof StrongMutationTestFitness) {
			weight = 2d;
		} else if (target instanceof WeakMutationTestFitness) {
			weight = 1.5d;
		} else if (target instanceof OutputCoverageTestFitness) {
			weight = 0.75d;
		} else if (target instanceof InputCoverageTestFitness) {
			weight = 0.75d;
		} else if (target instanceof LineCoverageTestFitness) {
			weight = 0.5d;
		} else if (target instanceof MethodCoverageTestFitness || target instanceof MethodNoExceptionCoverageTestFitness) {
			weight = 0.5d;
		}
		logger.debug("Target: {} weight: {}", target.toString(), weight);
		return weight;
	}


	public Set<BasicBlock> lookForParent(BasicBlock block, ActualControlFlowGraph acfg, Set<BasicBlock> visitedBlock){
		Set<BasicBlock> realParent = new HashSet<BasicBlock>();
		Set<BasicBlock> parents = acfg.getParents(block);
		if (parents.size() == 0){
			realParent.add(block);
			return realParent;
		}
		for (BasicBlock bb : parents){
			if (visitedBlock.contains(bb))
				continue;
			visitedBlock.add(bb);
			if (containsBranches(bb))
				realParent.add(bb);
			else 
				realParent.addAll(lookForParent(bb, acfg, visitedBlock));
		}
		return realParent;
	}

	/**
	 * Utility method that verifies whether a basic block (@link {@link BasicBlock})
	 * contains a branch.
	 * @param block object of {@link BasicBlock}
	 * @return true or false depending on whether a branch is found
	 */
	public boolean containsBranches(BasicBlock block){
		for (BytecodeInstruction inst : block)
			if (inst.toBranch()!=null)
				return true;
		return false;
	}

	/**
	 * Utility method that extracts a branch ({@link Branch}) from a basic block 
	 * (@link {@link BasicBlock}).
	 * @param block object of {@link BasicBlock}
	 * @return an object of {@link Branch} representing the branch in the block
	 */
	public Branch extractBranch(BasicBlock block){
		for (BytecodeInstruction inst : block)
			if (inst.isBranch() || inst.isActualBranch())
				return inst.toBranch();
		return null;
	}
	
	public Set<FitnessFunction<T>> getRootBranches(){
		return this.rootBranches;
	}
	
	@SuppressWarnings("unchecked")
	public Set<FitnessFunction<T>> getStructuralChildren(FitnessFunction<T> parent){
		Set<DependencyEdge> outgoingEdges = this.graph.outgoingEdgesOf(parent);
		Set<FitnessFunction<T>> children = new HashSet<FitnessFunction<T>>();
		for (DependencyEdge edge : outgoingEdges){
			children.add((FitnessFunction<T>) edge.getTarget());
		}
		return children;
	}
	
	@SuppressWarnings("unchecked")
	public Set<FitnessFunction<T>> getStructuralParents(FitnessFunction<T> parent){
		Set<DependencyEdge> incomingEdges = this.graph.incomingEdgesOf(parent);
		Set<FitnessFunction<T>> parents = new HashSet<FitnessFunction<T>>();
		for (DependencyEdge edge : incomingEdges){
			parents.add((FitnessFunction<T>) edge.getSource());
		}
		return parents;
	}
	
	
	//////////// Fitsum: code below is added for providing additional support for managing ECDG
	
	public DependencyEdge addWeightedEdge(FitnessFunction<T> source, FitnessFunction<T> target, double weight) {
		DependencyEdge edge = graph.addEdge(source, target);
		graph.setEdgeWeight(edge, weight);
		return edge;
	}
	
	public DependencyEdge addWeightedEdge(FitnessFunction<T> source, FitnessFunction<T> target) {
		double weight = determineWeight(target);
		DependencyEdge edge = null;
		try {
			edge = graph.addEdge(source, target);
			graph.setEdgeWeight(edge, weight);
			return edge;
		} catch (java.lang.IllegalArgumentException e){
			graph.removeEdge(edge);
		}
		return edge;
	}
	
	public void exportGraphAsDot() {
		LoggingUtils.getEvoLogger().info("Dumping ECDG in DOT format ...");
		
		VertexNameProvider<FitnessFunction<T>> vertexIdProvider = new VertexNameProvider<FitnessFunction<T>>() {

			@Override
			public String getVertexName(FitnessFunction<T> vertex) {
				return "" + vertex.toString().hashCode();
			}
		};
		
		VertexNameProvider<FitnessFunction<T>> vertexLabelProvider = new VertexNameProvider<FitnessFunction<T>>() {

			@Override
			public String getVertexName(FitnessFunction<T> vertex) {
				return vertex.toString();
			}
		};
		EdgeNameProvider<DependencyEdge> edgeLabelProvider = new EdgeNameProvider<DependencyEdge>() {

			@Override
			public String getEdgeName(DependencyEdge edge) {
				return "" + graph.getEdgeWeight(edge);
			}
		};
		DOTExporter<FitnessFunction<T>, DependencyEdge> dotExporter = new DOTExporter<FitnessFunction<T>, DependencyEdge>(vertexIdProvider, vertexLabelProvider , edgeLabelProvider);
		Writer writer;
		try {
			writer = new FileWriter (Properties.TARGET_CLASS + ".ecdg.dot");
			dotExporter.export(writer, graph);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns all nodes of the graph reachable from the given node (not only direct children)
	 * @param parent
	 * @return set of all (recursively) reachable nodes from the given node.
	 */
	public Set<FitnessFunction<T>> getAllStructuralChildren(FitnessFunction<T> parent){
		DepthFirstIterator<FitnessFunction<T>, DependencyEdge> iterator = new DepthFirstIterator<FitnessFunction<T>, DependencyEdge>(graph, parent);
		Set<FitnessFunction<T>> children = new HashSet<FitnessFunction<T>>();
		while (iterator.hasNext()){
			children.add((FitnessFunction<T>) iterator.next());
		}
		return children;
	}
	
	
	/**
	 * Returns a count of all nodes of the graph reachable from the given node (not only direct children)
	 * @param parent
	 * @return count of all (recursively) reachable nodes from the given node.
	 */
	public double getAllStructuralChildrenCount(FitnessFunction<T> parent){
		DepthFirstIterator<FitnessFunction<T>, DependencyEdge> iterator = new DepthFirstIterator<FitnessFunction<T>, DependencyEdge>(graph, parent);
		Set<FitnessFunction<T>> children = new HashSet<FitnessFunction<T>>();
		while (iterator.hasNext()){
			children.add((FitnessFunction<T>) iterator.next());
		}
		return children.size();
	}
	
	/**
	 * Returns a "weighted" count of all (recursively) nodes. It basically returns a weighted sum. 
	 * @param parent
	 * @return weighted count (sum)
	 */
	public double getWeightedChildrenCount(FitnessFunction<T> parent){
		double weightedCount = 0d;
		DepthFirstIterator<FitnessFunction<T>, DependencyEdge> iterator = new DepthFirstIterator<FitnessFunction<T>, DependencyEdge>(graph, parent);
		while (iterator.hasNext()){
			FitnessFunction<T> node = iterator.next();
			Set<DependencyEdge> incomingEdges = graph.incomingEdgesOf(node);
			for (DependencyEdge edge : incomingEdges) {
				double w = graph.getEdgeWeight(edge);
				weightedCount += w;
			}
		}
		return weightedCount;
	}
}
