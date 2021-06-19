package edu.purdue.nodelink.aggregation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.PriorityQueue;

public class GraphHierAggregation {
	
	@SuppressWarnings("unchecked")
	public static class NodePair implements Comparable { 
		private AggGraph.AggNode n1, n2;
		private double distance;
		public NodePair(AggGraph.AggNode n1, AggGraph.AggNode n2, double distance) { 
			this.n1 = n1;
			this.n2 = n2;
			this.distance = distance;
		}
		public int compareTo(Object o) {
			if (o instanceof NodePair) { 
				NodePair other = (NodePair) o;
				return distance < other.distance ? - 1 : distance > other.distance ? 1 : 0;
			}
			return 0;
		}
		public AggGraph.AggNode getNode1() { 
			return n1;
		}
		public AggGraph.AggNode getNode2() { 
			return n2;
		}
		public double getDistance() {
			return distance;
		}
	}
	
	private static PriorityQueue<NodePair> computeDistances(AggGraph graph, AggGraphDistance dist) { 
		PriorityQueue<NodePair> nodeDist = new PriorityQueue<NodePair>(); 
		for (int i = 0; i < graph.getAggregateCount() - 1; i++) {
			AggGraph.AggNode n1 = graph.getNode(i);
			for (int j = i + 1; j < graph.getAggregateCount(); j++) {
				AggGraph.AggNode n2 = graph.getNode(j);
				nodeDist.add(new NodePair(n1, n2, dist.getDistance(n1, n2)));
			}
		}	
		return nodeDist;
	}
	
	public static NodePair aggregateGraphStep(AggGraph graph, AggGraphDistance dist) { 
		PriorityQueue<NodePair> nodeDist = computeDistances(graph, dist); 
		return nodeDist.peek();
	}
	
	private static double calculateAggregateDiameter(AggGraph.AggNode node, AggGraphDistance dist) {
		double diameter = 0;
		for (int i = 0; i < node.getAggregateCount() - 1; i++) { 
			for (int j = i + 1; j < node.getAggregateCount(); j++) { 
				double currDist = dist.getDistance((AggGraph.AggNode) node.getAggregate(i), (AggGraph.AggNode) node.getAggregate(j));
				if (currDist > diameter) diameter = currDist;
			}
		}
		return diameter;
	}

	public static AggGraph.AggNode expandGraphStep(AggGraph graph, final AggGraphDistance dist) {
		double maxDiameter = Double.MIN_VALUE;
		AggGraph.AggNode maxNode = null;
		
		// Calculate distances for children of current aggregation
		for (int i = 0; i < graph.getAggregateCount(); i++) {
			AggGraph.AggNode node = graph.getNode(i);
			if (node.isLeaf() || node.getAggregateCount() < 2) continue;
			double currDiameter = calculateAggregateDiameter(node, dist);
			if (currDiameter > maxDiameter) { 
				maxDiameter = currDiameter;
				maxNode = node;
			}
		}
		
		return maxNode;
	}
	
	public static void aggregateGraph(AggGraph graph, AggGraphDistance dist) {

		// Get rid of any existing aggregation
		graph.expandAll();
		
		// Stop updating -- avoid multiple updates
		graph.freeze();
		
		// Calculate distances
		PriorityQueue<NodePair> nodeDist = computeDistances(graph, dist);
		
		// Keep aggregating until we have only a single top node
		while (!nodeDist.isEmpty()) { 
			
			// Get the node pair with the shortest distance
			NodePair np = nodeDist.poll();
			
			// Aggregate these two nodes
			ArrayList<AggGraph.AggNode> nodes = new ArrayList<AggGraph.AggNode>();
			nodes.add(np.getNode1());
			nodes.add(np.getNode2());
			AggGraph.AggNode superNode = graph.aggregate(nodes);
			
			// Mark all node pairs involving the old nodes
			ArrayList<NodePair> toBeDeleted = new ArrayList<NodePair>();
			for (Iterator<NodePair> i = nodeDist.iterator(); i.hasNext(); ) { 
				NodePair np2 = i.next();
				if (np2.getNode1() == np.getNode1() ||
					np2.getNode1() == np.getNode2() ||
					np2.getNode2() == np.getNode1() ||
					np2.getNode2() == np.getNode2()) { 
					toBeDeleted.add(np2);
				}
			}
			
			// Remove old pairs
			for (NodePair oldPair : toBeDeleted) {
				nodeDist.remove(oldPair);
			}
			
			// Compute new distances
			for (int i = 0; i < graph.getAggregateCount(); i++) { 
				AggGraph.AggNode currNode = graph.getNode(i);
				if (currNode == superNode) continue;
				nodeDist.add(new NodePair(superNode, currNode, dist.getDistance(superNode, currNode)));
			}
		}
		
		graph.thaw();
	}
}
