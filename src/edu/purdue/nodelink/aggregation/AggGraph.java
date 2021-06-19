/* ------------------------------------------------------------------
 * AggGraph.java
 * 
 * Created 2009-01-20 by Niklas Elmqvist <elm@purdue.edu>.
 * ------------------------------------------------------------------
 */
package edu.purdue.nodelink.aggregation;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;

import edu.purdue.pivotlib.graph.BasicGraph;
import edu.purdue.pivotlib.graph.Graph;

public class AggGraph {

	/// Property for when the current aggregation hierarchy has changed
	public static final String PROPERTY_AGGREGATION = "aggregation";

	/// Property for when the current order has changed
	public static final String PROPERTY_ORDER = "order";

    private static int nodeCounter = 0;

	public class AggNode extends BasicAggregate<Integer> {
		
		private int id;
		private ArrayList<AggNode> neighbors = new ArrayList<AggNode>();

		/**
		 * Base aggregation node constructor.  Use this for creating the leaf aggregates that only contain items. 
		 * @param index integer reference (row number) to the original node.
		 */
		public AggNode(int index) {
			this.id = nodeCounter++;
			addItem(index);
		}
		
		/**
		 * Aggregation node constructor.  Use this for creating an aggregated node and for aggregating edges correspondingly.
		 * @param nodes list of nodes to aggregate.
		 */
		public AggNode(Collection<AggNode> nodes) {
			this.id = nodeCounter++;

			// Step through all nodes that should be aggregated
			for (AggNode node : nodes) {
				
				// Add the aggregate
				addAggregate(node);
				
				// Update my neighbor's neighbors
				for (AggNode neighbor : node.neighbors) {
					
					// Self-references cause concurrent modification of the neighbor list
					if (neighbor == node) continue;
					
					// Remove all references to the old node
					while (neighbor.neighbors.contains(node)) {  
						neighbor.neighbors.remove(node);
					}

					// Need to add edges to the neighbors for the supernode
					if (!neighbors.contains(neighbor)) { 
						neighbors.add(neighbor);
					}
					
					// Make sure the neighbor knows about us too 
					if (!neighbor.neighbors.contains(this)) { 
						neighbor.neighbors.add(this);
					}
				}
			}
		}
		
		public int getId() { 
			return id;
		}
		
		public boolean connectedTo(AggNode node) {
			Collection<Integer> currItems = getAllItems();
			Collection<Integer> compItems = node.getAllItems();
			for (Integer currItem : currItems) { 
				for (Integer compItem : compItems) { 
					if (connected(currItem, compItem)) return true;
				}
			}
			return false;
		}
		
		public Collection<AggNode> getNeighbors() { 
			return neighbors;
		}
		
		private void rebuildEdges() {
			
			// Rebuild edges for all child nodes
			for (int i = 0; i < getAggregateCount(); i++) { 
				AggNode childNode = (AggNode) getAggregate(i);
				childNode.neighbors.clear();
				Collection<Integer> childItems = childNode.getAllItems();
				for (Integer baseItem : childItems) {
					if (!edgeTable.containsKey(baseItem)) continue;
					ArrayList<Integer> baseNeighbors = edgeTable.get(baseItem);
					for (int j = 0; j < nodes.size(); j++) { 
						AggNode compNode = getNode(j);
						Collection<Integer> compItems = compNode.getAllItems();
						for (Integer compItem : compItems) {
							if (baseNeighbors.contains(compItem)) {
								if (!childNode.neighbors.contains(compNode)) { 
									childNode.neighbors.add(compNode);
								}
								break;
							}
						}
					}
				}
			}
			
			// Fix other nodes referencing the old aggregate
			for (int i = 0; i < nodes.size(); i++) {
				AggNode currNode = getNode(i);
				if (currNode.neighbors.contains(this)) {
					
					// Remove reference to old aggregate
					currNode.neighbors.remove(this);
					
					// Figure out which child it should refer to
					Collection<Integer> currItems = currNode.getAllItems();
					for (Integer currItem : currItems) {
						if (!edgeTable.containsKey(currItem)) continue;
						ArrayList<Integer> currNeighbors = edgeTable.get(currItem);
						for (int j = 0; j < getAggregateCount(); j++) {
							AggNode childNode = (AggNode) getAggregate(j);						
							Collection<Integer> childItems = childNode.getAllItems();
							for (Integer childItem : childItems) {
								if (currNeighbors.contains(childItem)) {
									if (!currNeighbors.contains(childNode)) { 
										childNode.neighbors.add(childNode);
									}
									break;
								}
							}
						}
					}
				}
			}
		}

		public void addLeaves(ArrayList<AggNode> leaves) { 
			if (isLeaf()) leaves.add(this);
			else {
				for (int i = 0; i < getAggregateCount(); i++) {
					AggNode child = (AggNode) getAggregate(i);
					child.addLeaves(nodes);
				}
			}
		}

		public String toString() {
			StringBuffer sbuf = new StringBuffer();
			sbuf.append("[");
			for (Integer i : getAllItems()) {
				sbuf.append(i + ", ");
			}
			sbuf.append("]");
			return sbuf.toString();
		}
	}
	
	private Graph graph;
	private boolean fireChanges = true;
	private ArrayList<AggNode> nodes = new ArrayList<AggNode>();
	private Hashtable<Integer, ArrayList<Integer>> edgeTable = new Hashtable<Integer, ArrayList<Integer>>(); 
	private PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);
	
	public AggGraph(Graph graph) {
		this.graph = graph;
		if (!graph.isDirected()) graph.expandUndirected();
		createBaseHierarchy();
	}
	
	private void createBaseHierarchy() {
		
		// Clear the node list
		nodes.clear();
		edgeTable.clear();
		
		// Create aggregate nodes for all atoms 
		for (int index = 0; index < graph.getVertexCount(); index++) {
			AggNode node = new AggNode(index);
			nodes.add(node);
		}
		
		// Create edges (aggregate over same from and to nodes)
		for (int index = 0; index < graph.getEdgeCount(); index++) {
			
			// Add the edge
			int from = graph.getFromColumn().getIntValueAt(index);
			int to = graph.getToColumn().getIntValueAt(index);
			addEdge(from, to);
			
			// Update the edge table (only for base edges, not for aggregation)
			if (!edgeTable.containsKey(from)) {
				edgeTable.put(from, new ArrayList<Integer>());
			}
			ArrayList<Integer> neighbors = edgeTable.get(from);
			if (!neighbors.contains(to)) { 
				neighbors.add(to);
			}
		}
	}
	
	public boolean connected(int src, int dst) {
		if (!edgeTable.containsKey(src)) return false;
		ArrayList<Integer> neighbors = edgeTable.get(src);
		if (!neighbors.contains(dst)) return false;
		return true;
	}
	
	public void freeze() { 
		this.fireChanges = false;
	}
	
	public void thaw() { 
		this.fireChanges = true;
		firePropertyChange(PROPERTY_AGGREGATION, null, null);
	}
	
	public int expand(int index) {
		if (index >= nodes.size()) return 0;
		ArrayList<AggNode> afterNodes = new ArrayList<AggNode>();
		AggNode node = nodes.get(index);
		
		if (node.isLeaf()) return 1;
		
		nodes.remove(index);
		
		for (int i = 0; i < node.getAggregateCount(); i++) {
			AggNode child = (AggNode) node.getAggregate(i);
			nodes.add(index + i, child);
			afterNodes.add(child);
		}
		node.rebuildEdges();
		
		ArrayList<AggNode> beforeNodes = new ArrayList<AggNode>();
		beforeNodes.add(node);
		
		if (fireChanges) firePropertyChange(PROPERTY_AGGREGATION, beforeNodes, afterNodes);

		return node.getAggregateCount();
	}

	public int expand(AggNode node) {
		if (node.isLeaf()) return 1;
		if (!nodes.contains(node)) return 0;
		
		ArrayList<AggNode> afterNodes = new ArrayList<AggNode>();
		
		nodes.remove(node);
		
		for (int i = 0; i < node.getAggregateCount(); i++) {
			AggNode child = (AggNode) node.getAggregate(i);
			nodes.add(child);
			afterNodes.add(child);
		}
		node.rebuildEdges();
		
		ArrayList<AggNode> beforeNodes = new ArrayList<AggNode>();
		beforeNodes.add(node);
		
		if (fireChanges) firePropertyChange(PROPERTY_AGGREGATION, beforeNodes, afterNodes);

		return node.getAggregateCount();
	}
	
	public AggNode aggregate(int start, int length) {

		ArrayList<AggNode> beforeNodes = new ArrayList<AggNode>();
		for (int i = 0; i < length; i++) { 
			beforeNodes.add(nodes.get(start + i));
		}
		AggNode superNode = new AggNode(beforeNodes);
		nodes.removeAll(beforeNodes);
		nodes.add(start, superNode);
		
		ArrayList<AggNode> afterNodes = new ArrayList<AggNode>();
		afterNodes.add(superNode);
		
		if (fireChanges) firePropertyChange(PROPERTY_AGGREGATION, beforeNodes, afterNodes);
		
		return superNode;
	}

	public AggNode aggregate(Collection<AggNode> subNodes) {

		AggNode superNode = new AggNode(subNodes);
		nodes.removeAll(subNodes);
		nodes.add(superNode);
		
		ArrayList<AggNode> afterNodes = new ArrayList<AggNode>();
		afterNodes.add(superNode);
		
		if (fireChanges) firePropertyChange(PROPERTY_AGGREGATION, subNodes, afterNodes);
		
		return superNode;
	}

	public void checkIntegrity() { 
		for (Iterator<AggNode> i = nodes.iterator(); i.hasNext(); ) { 
			AggNode currNode = i.next();
			for (AggNode neighbor : currNode.neighbors) { 
				if (!nodes.contains(neighbor)) { 
					System.err.println("INTEGRITY: neighbor list not updated for node " + currNode);
				}
			}
		}	
	}

	public void expandAll() {
		createBaseHierarchy();
	}
	
	public AggNode getNode(int index) { 
		return nodes.get(index);
	}
	
	public ArrayList<AggNode> getNodes() {
		return nodes;
	}
	
	public int getNodeIndexOf(AggNode node) {
		return nodes.indexOf(node);
	}
	
	public int getAggregateCount() { 
		return nodes.size();
	}
	
	public Graph getGraph() { 
		return graph;
	}
		
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertySupport.addPropertyChangeListener(listener);
	}
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) { 
		propertySupport.addPropertyChangeListener(propertyName, listener);
	}
	public void firePropertyChange(String propertyName, Object oldValue, Object newValue) { 
		propertySupport.firePropertyChange(propertyName, oldValue, newValue);
	}
	public PropertyChangeListener[] getPropertyChangeListeners() {
		return propertySupport.getPropertyChangeListeners();
	}
	public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) { 
		return propertySupport.getPropertyChangeListeners(propertyName);
	}
	public boolean hasListeners(String propertyName) {
		return propertySupport.hasListeners(propertyName);
	}
	public void removePropertyChangeListener(PropertyChangeListener listener) { 
		propertySupport.removePropertyChangeListener(listener);
	}
	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) { 
		propertySupport.removePropertyChangeListener(propertyName, listener);		
	}
	
	private void addEdge(int from, int to) {
		AggNode fromNode = nodes.get(from);
		AggNode toNode = nodes.get(to);
		if (!fromNode.neighbors.contains(toNode)) { 
			fromNode.neighbors.add(toNode);
		}
	}

	public void clear() {
		graph.clear();
		nodes.clear();
	}

	public boolean isDirected() {
		return graph.isDirected();
	}
	
	public int getVertexCount() { 
		return graph.getVertexCount();
	}

	public int getEdgeCount() { 
		return graph.getEdgeCount();
	}
	
//	public void fireAggregationChange() { 
//		firePropertyChange(AggGraph.PROPERTY_AGGREGATION, null, null);
//	}
//
	public final static void main(String[] args) { 
		
		Graph graph = new BasicGraph("graph");
		graph.setDirected(false);
		graph.addVertex(); // 0
		graph.addVertex(); // 1
		graph.addVertex(); // 2
		graph.addVertex(); // 3
		
		graph.addEdge(0, 1);
		graph.addEdge(0, 2);
		graph.addEdge(1, 2);
		graph.addEdge(1, 3);
		graph.addEdge(2, 3);

		System.err.println("Graph with " + graph.getVertexCount() + " nodes and " + graph.getEdgeCount() + " edges.");
		
		AggGraph agg = new AggGraph(graph);
		
		AggNode n0 = agg.getNode(0); 
		AggNode n1 = agg.getNode(1); 
		AggNode n2 = agg.getNode(2); 
		AggNode n3 = agg.getNode(3);

		ArrayList<AggNode> indexList = new ArrayList<AggNode>();
		indexList.add(n1);
		indexList.add(n2);
		AggNode n4 = agg.aggregate(indexList);
		agg.checkIntegrity();
		
		System.err.println("All false:");
		System.err.println("0 connected to 0? (false) " + n0.connectedTo(n0));
		System.err.println("0 connected to 3? (false) " + n0.connectedTo(n3));
		System.err.println("3 connected to 0? (false) " + n3.connectedTo(n0));
		System.err.println("3 connected to 3? (false) " + n3.connectedTo(n3));
		
		System.err.println("All true:");
		System.err.println("0 connected to 1? (true) " + n0.connectedTo(n1));
		System.err.println("0 connected to 2? (true) " + n0.connectedTo(n2));
		System.err.println("0 connected to 4? (true) " + n0.connectedTo(n4));
		System.err.println("3 connected to 4? (true) " + n3.connectedTo(n4));
		System.err.println("4 connected to 0? (true) " + n4.connectedTo(n0));
		System.err.println("4 connected to 3? (true) " + n4.connectedTo(n3));
		System.err.println("4 connected to 1? (true) " + n4.connectedTo(n1));
		System.err.println("4 connected to 2? (true) " + n4.connectedTo(n2));
		System.err.println("4 connected to 4? (true) " + n4.connectedTo(n4));

		agg.expand(1);

		// Aggregation test
		agg.expandAll();
		
		AggNode i1 = agg.aggregate(2, 2);
		AggNode i2 = agg.aggregate(0, 2);
		
		agg.expand(i1);
		agg.expand(i2);
		agg.checkIntegrity();
	}
}
