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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.purdue.pivotlib.data.Column;
import edu.purdue.pivotlib.data.IntColumn;
import edu.purdue.pivotlib.data.StringColumn;
import edu.purdue.pivotlib.data.Table;
import edu.purdue.pivotlib.graph.BasicGraph;
import edu.purdue.pivotlib.graph.Graph;

/**
 * Balanced aggregation graph---only supports balanced split and merge 
 * operations (allowing it to rely on an aggregation hierarchy for the
 * edges as well).  NOTE! This class cannot be used if you change the
 * order of split and merge operations! 
 * 
 * @author Niklas Elmqvist
 */
public class BalancedAggGraph implements Graph {

	/// Property for when the current aggregation hierarchy has changed
	public static final String PROPERTY_AGGREGATION = "aggregation";

	/// Property for when the current order has changed
	public static final String PROPERTY_ORDER = "order";

    private static int nodeCounter = 0;
	private static int edgeCounter = 0;

	@SuppressWarnings("unchecked")
	private static class Item implements Comparable { 
		int index;
		Comparable item;
		public Item(int index, Comparable item) { 
			this.index = index;
			this.item = item;
		}
		public int getIndex() { 
			return index;
		}
		public int compareTo(Object o) {
			if (o instanceof Item) {
				Item i = (Item) o;
				return item.compareTo(i.item);
			}
			else return 0;
		}	
	}
	
	public class AggNode extends BasicAggregate<Integer> {
		
		private int id;
		private ArrayList<AggEdge> outEdges = new ArrayList<AggEdge>();
		private ArrayList<AggEdge> inEdges = new ArrayList<AggEdge>();

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
			
			Hashtable<AggNode, ArrayList<AggEdge>> inEdgeTable = new Hashtable<AggNode, ArrayList<AggEdge>>();
			Hashtable<AggNode, ArrayList<AggEdge>> outEdgeTable = new Hashtable<AggNode, ArrayList<AggEdge>>();
			
			// Step through all nodes that should be aggregated
			for (AggNode node : nodes) {
				
				// Add the aggregate
				addAggregate(node);
				
				// Separate all out edges that should be aggregated 
				for (AggEdge outEdge : node.outEdges) {
					AggNode key = nodes.contains(outEdge.getDst()) ? this : outEdge.getDst(); 
					separateAggEdge(key, outEdge, outEdgeTable);
				}
				
				// Now do the same for all in edges 
				for (AggEdge inEdge : node.inEdges) {
					AggNode key = nodes.contains(inEdge.getSrc()) ? this : inEdge.getSrc(); 
					separateAggEdge(key, inEdge, inEdgeTable);
				}
			}
			
			// Now build the new aggregated out-edges
			Enumeration<AggNode> outKeys = outEdgeTable.keys();
			while (outKeys.hasMoreElements()) { 
				AggNode dst = outKeys.nextElement();				
				ArrayList<AggEdge> edgeList = outEdgeTable.get(dst);
				AggEdge edge = new AggEdge(edgeList, this, dst);
					
				// Add the edge itself
				addOutEdge(edge);
					
				// Update the connected nodes
				if (dst != this) { 
					for (AggEdge subEdge : edgeList) {
						dst.inEdges.remove(subEdge);
					}
					dst.addInEdge(edge);
				}
			}

			// Then the new aggregated in-edges
			Enumeration<AggNode> inKeys = inEdgeTable.keys();
			while (inKeys.hasMoreElements()) { 
				AggNode src = inKeys.nextElement();
				ArrayList<AggEdge> edgeList = inEdgeTable.get(src);
				AggEdge edge = new AggEdge(edgeList, src, this);
				
				// Add the edge itself
				addInEdge(edge);
				
				// Update the connected nodes
				if (src != this) { 
					for (AggEdge subEdge : edgeList) { 
						src.outEdges.remove(subEdge);
					}
					src.addOutEdge(edge);
				}
			}
		}
		
		private void separateAggEdge(AggNode key, AggEdge edge, Hashtable<AggNode, ArrayList<AggEdge>> edgeTable) { 
			if (!edgeTable.containsKey(key)) {
				ArrayList<AggEdge> edgeList = new ArrayList<AggEdge>();
				edgeTable.put(key, edgeList);
			}
			ArrayList<AggEdge> edgeList = edgeTable.get(key);
			edgeList.add(edge);			
		}
		
		public int getId() { 
			return id;
		}
		
		public void expandEdges() {
			
			// Operation does not make sense if there are no aggregates
			if (isLeaf()) return;
			
			// Repoint the in-edges 
			for (AggEdge edge : inEdges) {

				// Remove this edge from the source node
				edge.getSrc().outEdges.remove(edge);
				
				// Step through the aggregated edges and restore them				
				for (int i = 0; i < edge.getAggregateCount(); i++) {
					AggEdge subEdge = (AggEdge) edge.getAggregate(i);
					subEdge.getSrc().addOutEdge(subEdge);
				}
			}

			// Repoint the out-edges
			for (AggEdge edge : outEdges) {
				edge.getDst().inEdges.remove(edge);
			}
		}
		
		public int getOutEdgeCount() { 
			return outEdges.size();
		}
		
		public int getInEdgeCount() { 
			return inEdges.size();
		}
		
		public ArrayList<AggEdge> getAllInEdges() {
			return inEdges;
		}
		
		public ArrayList<AggEdge> getAllOutEdges() {
			return outEdges;
		}
		
		public void addOutEdge(AggEdge edge) {
			if (outEdges.contains(edge)) return;
			outEdges.add(edge);
		}

		public void addInEdge(AggEdge edge) { 
			if (inEdges.contains(edge)) return;
			inEdges.add(edge);
		}
		
		public AggEdge getConnectingEdge(AggNode node) { 
			for (AggEdge edge : outEdges) {
				if (edge.getDst().containsAggregate(node)) return edge;
			}
			return null;
		}
		
		public boolean connectedTo(AggNode node) {
			return getConnectingEdge(node) != null;
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
	
	public class AggEdge extends BasicAggregate<Integer> {
		
		private int id;
		private AggNode src, dst;
		
		public AggEdge(AggNode src, AggNode dst) {
			this.id = edgeCounter++;
			this.src = src;
			this.dst = dst;			
		}
		
		public AggEdge(Collection<AggEdge> edges, AggNode src, AggNode dst) {
			this.id = edgeCounter++;
			for (AggEdge edge : edges) { 
				addAggregate(edge);
			}
			this.src = src;
			this.dst = dst;
		}
		
		public int getId() { 
			return this.id;
		}
		
		public AggNode getSrc() {
			return this.src;
		}
		
		public AggNode getDst() { 
			return this.dst;
		}
	}
	
	private Graph graph;
	private ArrayList<AggNode> nodes = new ArrayList<AggNode>();
	private Hashtable<Integer, AggEdge> edges = new Hashtable<Integer, AggEdge>();
	private PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);
	
	public BalancedAggGraph() { 
		graph = new BasicGraph("graph");
	}
	
	public BalancedAggGraph(Graph graph) {
		this.graph = graph;
		if (!graph.isDirected()) graph.expandUndirected();
		createBaseHierarchy();
	}
	
	private void updateEdgeTable(int from, int to, int row) {
		int edgeIndex = from * graph.getEdgeCount() + to;
		if (!edges.containsKey(edgeIndex)) {
			AggEdge edge = new AggEdge(nodes.get(from), nodes.get(to));
			edges.put(edgeIndex, edge);
		}
		AggEdge edge = edges.get(edgeIndex);
		edge.addItem(row);
		nodes.get(from).addOutEdge(edge);
		nodes.get(to).addInEdge(edge);
	}
	
	public List<Integer> mapRowsToAggNodes(List<Integer> rowNumbers) {
		Set<AggNode> visited = new HashSet<AggNode>();
		ArrayList<Integer> nodeIndices = new ArrayList<Integer>();
		for (Integer row : rowNumbers) { 
			
			// Search for this row (inefficient, but uncommonly called)
			for (int index = 0; index < nodes.size(); index++) {
				AggNode node = nodes.get(index);
				if (!visited.contains(node)) {
					if (node.containsItem(row)) {
						nodeIndices.add(index);
						visited.add(node);
						break;
					}
				}
			}
		}
		return nodeIndices;
	}
	
	public int mapRowToAggNode(int rowNumber) {
		for (int index = 0; index < nodes.size(); index++) {
			AggNode node = nodes.get(index);
			if (node.containsItem(rowNumber))
				return index;
		}
		return -1; // probably curled rows
	}
	
	public void permutate(List<Integer> permutation) {
		Set<AggNode> visited = new HashSet<AggNode>();
		ArrayList<AggNode> newNodeList = new ArrayList<AggNode>();
		for (Integer i : permutation) {
			if (i < 0 || i >= nodes.size()) continue;
			newNodeList.add(nodes.get(i));
			visited.add(nodes.get(i));
		}
		
		nodes = newNodeList;
		firePropertyChange(PROPERTY_ORDER, null, permutation);
	}
	
	private void createBaseHierarchy() {
		
		// Create aggregate nodes for all atoms 
		for (int index = 0; index < graph.getVertexCount(); index++) {
			AggNode node = new AggNode(index);
			nodes.add(node);
		}
		
		// Create edges (aggregate over same from and to nodes)
		for (int index = 0; index < graph.getEdgeCount(); index++) {
			
			int from = graph.getFromColumn().getIntValueAt(index);
			int to = graph.getToColumn().getIntValueAt(index);
			
			updateEdgeTable(from, to, index);			
		}
	}
	
	public int expand(int index) {

		ArrayList<AggNode> afterNodes = new ArrayList<AggNode>();
		AggNode node = nodes.get(index);
		
		if (node.isLeaf()) return 1;
		
		nodes.remove(index);
		
		for (int i = 0; i < node.getAggregateCount(); i++) {
			AggNode child = (AggNode) node.getAggregate(i);
			nodes.add(index + i, child);
			afterNodes.add(child);
		}
		node.expandEdges();
		
		ArrayList<AggNode> beforeNodes = new ArrayList<AggNode>();
		beforeNodes.add(node);
		
		firePropertyChange(PROPERTY_AGGREGATION, beforeNodes, afterNodes);

		return node.getAggregateCount();
	}

	public int expand(AggNode node) {

		ArrayList<AggNode> afterNodes = new ArrayList<AggNode>();
//		AggNode node = nodes.get(index);
		
		if (node.isLeaf()) return 1;
		
		nodes.remove(node);
		
		for (int i = 0; i < node.getAggregateCount(); i++) {
			AggNode child = (AggNode) node.getAggregate(i);
			nodes.add(/*index + i,*/ child);
			afterNodes.add(child);
		}
		node.expandEdges();
		
		ArrayList<AggNode> beforeNodes = new ArrayList<AggNode>();
		beforeNodes.add(node);
		
		firePropertyChange(PROPERTY_AGGREGATION, beforeNodes, afterNodes);

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
		
		firePropertyChange(PROPERTY_AGGREGATION, beforeNodes, afterNodes);
		
		return superNode;
	}

//	public AggNode aggregate(Collection<Integer> indices) {
//
//		ArrayList<AggNode> beforeNodes = new ArrayList<AggNode>();
//		for (Integer i : indices) {
//			beforeNodes.add(nodes.get(i));
//			System.err.println("- " + i);
//		}
//		AggNode superNode = new AggNode(beforeNodes);
//		nodes.removeAll(beforeNodes);
//		nodes.add(superNode);
//		
//		ArrayList<AggNode> afterNodes = new ArrayList<AggNode>();
//		afterNodes.add(superNode);
//		
//		firePropertyChange(PROPERTY_AGGREGATION, beforeNodes, afterNodes);
//		
//		return superNode;
//	}
	
	public AggNode aggregate(Collection<AggNode> subNodes) {

		AggNode superNode = new AggNode(subNodes);
		nodes.removeAll(subNodes);
		nodes.add(superNode);
		
		ArrayList<AggNode> afterNodes = new ArrayList<AggNode>();
		afterNodes.add(superNode);
		
		firePropertyChange(PROPERTY_AGGREGATION, subNodes, afterNodes);
		
		return superNode;
	}

	public void checkIntegrity() { 
		for (Iterator<AggNode> i = nodes.iterator(); i.hasNext(); ) { 
			AggNode currNode = i.next();
			for (Iterator<AggEdge> j = currNode.outEdges.iterator(); j.hasNext(); ) {
				AggEdge currEdge = j.next();
				if (currEdge.src != currNode) { 
					System.err.println("INTEGRITY: not same src!");
				}
				if (!nodes.contains(currEdge.getSrc())) {
					System.err.println("INTEGRITY: out - src not in current node aggregation");
				}
				if (!nodes.contains(currEdge.getDst())) {
					System.err.println("INTEGRITY: out - dst not in current node aggregation");
				}
			}
			for (Iterator<AggEdge> j = currNode.inEdges.iterator(); j.hasNext(); ) {
				AggEdge currEdge = j.next();
				if (currEdge.dst != currNode) { 
					System.err.println("INTEGRITY: not same dst!");
				}
				if (!nodes.contains(currEdge.getSrc())) {
					System.err.println("INTEGRITY: in - src not in current node aggregation");
				}
				if (!nodes.contains(currEdge.getDst())) {
					System.err.println("INTEGRITY: in - dst not in current node aggregation");
				}
			}			
		}	
	}

	public void expandAll() { 
		boolean flat; 
		do {
			flat = true;
			for (int i = 0; i < getAggregateCount(); i++) { 
				AggNode n = getNode(i);
				if (!n.isLeaf()) {					
					expand(i);
					flat = false;
					break;
				}
			}
		}
		while (!flat);		
	}
	
	public int cullEmptyNodes() {
		ArrayList<AggNode> cullList = new ArrayList<AggNode>();
		for (AggNode node : nodes) {
			if (node.getInEdgeCount() == 0 && node.getOutEdgeCount() == 0) { 
				cullList.add(node);
			}
		}
		nodes.removeAll(cullList);
		return cullList.size();
	}
	
	public AggNode getNode(int index) { 
		return nodes.get(index);
	}
	
	public AggEdge getEdge(int row) {
		int from = graph.getFromColumn().getIntValueAt(row);
		int to = graph.getToColumn().getIntValueAt(row);
		
		AggNode fromNode = getNode(this.mapRowToAggNode(from));
		AggNode toNode = getNode(this.mapRowToAggNode(to));
		
		if (fromNode == null || toNode == null)
			System.err.println("AggGraph.getEdge(): row, from, to:" + row + "," + from + "," + to);
		
		AggEdge edge = fromNode.getConnectingEdge(toNode);
		
		if (edge == null)
			System.err.println("AggGraph.getEdge(): row, from, to:" + row + "," + from + "," + to);
		
		return edge;
	}
	
	public ArrayList<AggNode> getNodes() {
		return nodes;
	}
	
	public Iterator<AggEdge> getEdges() {
		HashSet<AggEdge> edges = new HashSet<AggEdge>();
		for (AggNode node: nodes) {
			edges.addAll(node.getAllOutEdges());
			edges.addAll(node.getAllInEdges());			
		}
		return edges.iterator();
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
		
	private int getRowNumber(AggNode node) { 
		if (node.getItemCount() != 0) {
			return node.getItem(0);
		}
		else if (node.getAggregateCount() != 0) {
			for (int i = 0; i < node.getAggregateCount(); i++) {
				AggNode child = (AggNode) node.getAggregate(i);
				int ret = getRowNumber(child);
				if (ret != -1) return ret;
			}
		}
		return -1;
	}
	
	@SuppressWarnings("unchecked")
	public void sortNodes(Column c, boolean reverse) { 
		
		// Create the dummy flat list from the aggregation
		ArrayList<Item> items = new ArrayList<Item>();
		for (int i = 0; i < nodes.size(); i++) {
			int row = getRowNumber(getNode(i));			
			if (c instanceof StringColumn) { 
				items.add(new Item(i, c.getStringValueAt(row)));
			}
			else { 
				items.add(new Item(i, c.getRealValueAt(row)));
			}			
		}

		// Sort the dummy list
		Collections.sort(items);
		if (reverse) {
			Collections.reverse(items);
		}
		
		// Extract the indices
		ArrayList<Integer> permutation = new ArrayList<Integer>();
		for (Item i : items) { 
			permutation.add(i.getIndex());
		}
		
		// Finally, permutate the list
		permutate(permutation);
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
	
 	public void print() { 
 		for (AggNode node : nodes) {
			System.err.println("#" + node.getId() + ": " + node);

			for (AggEdge edge : node.outEdges) { 
				System.err.println("   -> #" + edge.getDst().getId() + ": " + edge.getDst());
			}
		}
	}
	
	public int addEdge(int from, int to) {
		int edge = graph.addEdge(from, to);
		updateEdgeTable(from, to, edge);
		return edge;
	}

	public int addVertex() {
		int vertex = graph.addVertex();
		AggNode node = new AggNode(vertex);
		nodes.add(node);
		return vertex;
	}

	public void clear() {
		graph.clear();
		nodes.clear();
		edges.clear();
	}

	public void expandUndirected() {
		graph.expandUndirected();
		nodes.clear();
		edges.clear();
		createBaseHierarchy();
	}

	public int getEdgeColumnCount() {
		return graph.getEdgeColumnCount();
	}

	public Table getEdgeTable() {
		return graph.getEdgeTable();
	}

	public int getEdgeCount() {
		return graph.getEdgeCount();
	}

	public IntColumn getFromColumn() {
		return graph.getFromColumn();
	}

	public String getName() {
		return graph.getName();
	}

	public IntColumn getToColumn() {
		return graph.getToColumn();
	}

	public StringColumn getVertexColumn() {
		return graph.getVertexColumn();
	}

	public int getVertexColumnCount() {
		return graph.getVertexColumnCount();
	}

	public Table getVertexTable() {
		return graph.getVertexTable();
	}

	public int getVertexCount() {
		return graph.getVertexCount();
	}

	public boolean isDirected() {
		return graph.isDirected();
	}

	public void setDirected(boolean directed) {
		graph.setDirected(directed);
		
	}

	public void setName(String name) {
		graph.setName(name);
	}
	
	public final static void main(String[] args) { 
		
		BalancedAggGraph agg = new BalancedAggGraph();
		agg.addVertex(); // 0
		agg.addVertex(); // 1
		agg.addVertex(); // 2
		agg.addVertex(); // 4
		
		agg.addEdge(0, 1);
		agg.addEdge(0, 2);
		agg.addEdge(1, 2);
		agg.addEdge(1, 3);
		agg.addEdge(2, 3);
		
		agg.expandUndirected();
		
		AggNode n0 = agg.getNode(0); 
		AggNode n1 = agg.getNode(1); 
		AggNode n2 = agg.getNode(2); 
		AggNode n3 = agg.getNode(3);

		ArrayList<AggNode> indexList = new ArrayList<AggNode>();
		indexList.add(n1);
		indexList.add(n2);
		AggNode n4 = agg.aggregate(indexList);

		agg.print();
		
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
		agg.print();

		// Aggregation test
//		@SuppressWarnings("unused")
//		AggNode i1 = agg.aggregate(2, 2);
//		@SuppressWarnings("unused")
//		AggNode i2 = agg.aggregate(0, 2);
////		@SuppressWarnings("unused")
////		AggNode i3 = agg.aggregate(0, 2);
//		agg.expand(i2);
//		agg.expand(i1);
//		agg.checkIntegrity();
////		agg.print();
	}
}
