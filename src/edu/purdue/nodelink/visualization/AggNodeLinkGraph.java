package edu.purdue.nodelink.visualization;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import edu.purdue.nodelink.aggregation.AggGraph;
import edu.purdue.nodelink.aggregation.GraphHierAggregation;
import edu.purdue.nodelink.aggregation.MidPointDistance;
import edu.purdue.pivotlib.data.RealColumn;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.nodes.PPath;

public class AggNodeLinkGraph implements PropertyChangeListener {
	
	public static final String X_COLUMN = "#X";
	public static final String Y_COLUMN = "#Y";
	
	public static final double NODE_RADIUS = 5.0;
	
	private AggGraph agg;
	private Hashtable<AggGraph.AggNode, PNode> nodes = new Hashtable<AggGraph.AggNode, PNode>();
	private Hashtable<PNode, AggGraph.AggNode> reverseNodes = new Hashtable<PNode, AggGraph.AggNode>();
	private Hashtable<AggGraph.AggNode, Point2D> layout = new Hashtable<AggGraph.AggNode, Point2D>();
	private PNode nodeRoot = new PNode(), edgeRoot = new PNode();
		
	public AggNodeLinkGraph(AggGraph graph) {
		this.agg = graph;
		this.agg.addPropertyChangeListener(this);
		
		RealColumn xCol = new RealColumn(X_COLUMN, true);
		RealColumn yCol = new RealColumn(Y_COLUMN, true);
		xCol.ensureCapacity(graph.getVertexCount());
		yCol.ensureCapacity(graph.getVertexCount());
		graph.getGraph().getVertexTable().addColumn(xCol);		
		graph.getGraph().getVertexTable().addColumn(yCol);
		
		// Initialize layout
		for (int i = 0; i < graph.getAggregateCount(); i++) { 
			AggGraph.AggNode node = graph.getNode(i);
			layout.put(node, new Point2D.Double(0, 0));
		}
		
		// Build the graph 
		buildGraph();
	}
	
	private void buildGraph() {
		// Add all the nodes
		for (int i = 0; i < agg.getAggregateCount(); i++) { 
			AggGraph.AggNode node = agg.getNode(i);
			addNode(node);
		}
		
		// Add all the edges
		for (int i = 0; i < agg.getAggregateCount(); i++) { 
			AggGraph.AggNode node = agg.getNode(i);
			for (AggGraph.AggNode neighbor : node.getNeighbors()) {
				if (node == neighbor) continue;
				addEdge(node, neighbor);
			}
		}
	}
	
	public void aggregate(Collection<PNode> nodes) {
		ArrayList<AggGraph.AggNode> aggNodes = new ArrayList<AggGraph.AggNode>();
		for (PNode node : nodes) {
			aggNodes.add(reverseNodes.get(node));
		}
		agg.aggregate(aggNodes);
	}
	
	public void rollUp(int numLevels) {
		
		agg.freeze();		
		
		for (int i = 0; i < numLevels; i++) {
			
			// Decide which node pair to aggregate
			GraphHierAggregation.NodePair np = GraphHierAggregation.aggregateGraphStep(agg, new MidPointDistance(agg));
			if (np == null) continue;
			
			// Aggregate the nodes
			ArrayList<AggGraph.AggNode> nodes = new ArrayList<AggGraph.AggNode>(); 
			nodes.add(np.getNode1());
			nodes.add(np.getNode2());
			agg.aggregate(nodes);
		}
		agg.thaw();
	}

	public void drillDown(int numLevels) {
		agg.freeze();
		for (int i = 0; i < numLevels; i++) {
			
			// Decide which node pair to expand
			AggGraph.AggNode node = GraphHierAggregation.expandGraphStep(agg, new MidPointDistance(agg));
			if (node == null) continue;
			
			// Expand that node
			agg.expand(node);			
		}
		agg.thaw();
	}

	public void expand(AggGraph.AggNode node) { 
		agg.expand(node);
	}
	
	public AggGraph getGraph() {
		return agg;
	}
	
	public PNode getNodeRoot() { 
		return nodeRoot;
	}
	
	public PNode getEdgeRoot() { 
		return edgeRoot;
	}
	
	public void clear() { 
		nodeRoot.removeAllChildren();
		edgeRoot.removeAllChildren();
		nodes.clear();
		reverseNodes.clear();
	}
	
	private PPath addNode(AggGraph.AggNode n) {

		PPath node;
		
        RealColumn xCol = (RealColumn) agg.getGraph().getVertexTable().getColumn(X_COLUMN);
        RealColumn yCol = (RealColumn) agg.getGraph().getVertexTable().getColumn(Y_COLUMN);
		
		// Is this a leaf?
		if (n.isLeaf()) { 
			node = PPath.createEllipse(0, 0, (float) (2 * NODE_RADIUS), (float) (2 * NODE_RADIUS));
	        node.setChildrenPickable(false);
	        
	        Integer item = n.getItem(0);
	        double x = xCol.getRealValueAt(item);
	        double y = yCol.getRealValueAt(item);
	        
	        node.setOffset(x, y);
		}
		// Create a convex hull for the aggregated node
		else {	
			
			// Compute the convex hull
			ConvexHull hull = new ConvexHull();
			Collection<Integer> items = n.getAllItems();
			for (Integer item : items) {
				
				// Find the center point
		        double x = xCol.getRealValueAt(item);
		        double y = yCol.getRealValueAt(item);
		        
		        // Expand the hull in all compass directions
		        hull.addPoint(new Point2D.Double(x - NODE_RADIUS, y));
		        hull.addPoint(new Point2D.Double(x + NODE_RADIUS, y));
		        hull.addPoint(new Point2D.Double(x, y - NODE_RADIUS));
		        hull.addPoint(new Point2D.Double(x, y + NODE_RADIUS));
			}
			hull.computeHull();
			
			// Create the Piccolo node
			node = PPath.createPolyline(hull.getPath());
	        node.setChildrenPickable(false);
	        
	        node.setPaint(new Color(1.0f, 0.0f, 1.0f));
		}
		
		// Save the node
        nodeRoot.addChild(node);
		nodes.put(n, node);
		reverseNodes.put(node, n);

		node.addInputEventListener(new PBasicInputEventHandler() {
			public void mouseClicked(PInputEvent event) {
				if (event.getButton() == MouseEvent.BUTTON1) { 
					expand(reverseNodes.get(event.getPickedNode()));
				}
			}
		});

        return node;
	}
	
	public void setVisibleItemRatio(double ratio) { 
		int numItems = (int) Math.round(ratio * agg.getGraph().getVertexCount());
		if (numItems < 1) numItems = 1;
		
		agg.freeze();
		while (agg.getAggregateCount() != numItems) { 

			// If we have too many, aggregate
			if (agg.getAggregateCount() > numItems) {

				// Decide which node pair to aggregate
				GraphHierAggregation.NodePair np = GraphHierAggregation.aggregateGraphStep(agg, new MidPointDistance(agg));
				if (np == null) break;
				
				// Aggregate the nodes
				ArrayList<AggGraph.AggNode> nodes = new ArrayList<AggGraph.AggNode>(); 
				nodes.add(np.getNode1());
				nodes.add(np.getNode2());
				agg.aggregate(nodes);
			}
			else { 
				// Decide which node pair to expand
				AggGraph.AggNode node = GraphHierAggregation.expandGraphStep(agg, new MidPointDistance(agg));
				if (node == null) break;
				
				// Expand that node
				agg.expand(node);			
			}
		}
		agg.thaw();
	}
	
	private void addEdge(AggGraph.AggNode from, AggGraph.AggNode to) {
        PPath edge = new PPath();
		PNode node1 = nodes.get(from);
		PNode node2 = nodes.get(to);
		Point2D start = node1.getFullBoundsReference().getCenter2D();
		Point2D end = node2.getFullBoundsReference().getCenter2D();
		edge.moveTo((float) start.getX(), (float) start.getY());
		edge.lineTo((float) end.getX(), (float) end.getY());
		edgeRoot.addChild(edge);
	}

	public void propertyChange(PropertyChangeEvent e) {
		// Clear the graph 
		nodeRoot.removeAllChildren();
		edgeRoot.removeAllChildren();
		
		// Rebuild the graph
		buildGraph();
	}
}
