package edu.purdue.nodelink.aggregation;

import java.awt.geom.Point2D;
import java.util.Hashtable;

import edu.purdue.nodelink.visualization.AggNodeLinkGraph;
import edu.purdue.pivotlib.data.RealColumn;

public class MidPointDistance implements AggGraphDistance {

	private AggGraph graph;
	private Hashtable<AggGraph.AggNode, Point2D> positions = new Hashtable<AggGraph.AggNode, Point2D>();

	public MidPointDistance(AggGraph graph) {
		this.graph = graph;
	}
	
	private Point2D findNodePosition(AggGraph.AggNode node) {
		
		// Create the position if it does not exist 
		if (!positions.containsKey(node)) { 
			
			// Is it a leaf?
			if (node.isLeaf()) { 
		        RealColumn xCol = (RealColumn) graph.getGraph().getVertexTable().getColumn(AggNodeLinkGraph.X_COLUMN);
		        RealColumn yCol = (RealColumn) graph.getGraph().getVertexTable().getColumn(AggNodeLinkGraph.Y_COLUMN);		
		        Integer item = node.getItem(0);
		        double x = xCol.getRealValueAt(item);
		        double y = yCol.getRealValueAt(item);
		        positions.put(node, new Point2D.Double(x, y));
			}
			// It is an aggregate---use the midpoint
			else { 
				AggGraph.AggNode n1 = (AggGraph.AggNode) node.getAggregate(0);
				AggGraph.AggNode n2 = (AggGraph.AggNode) node.getAggregate(1);
				Point2D p1 = findNodePosition(n1);
				Point2D p2 = findNodePosition(n2);
				Point2D center = new Point2D.Double(
						(p1.getX() + p2.getX()) / 2.0,
						(p1.getY() + p2.getY()) / 2.0);
				positions.put(node, center);				
			}
		}
		
		return positions.get(node);
	}
	
	public double getDistance(AggGraph.AggNode n1, AggGraph.AggNode n2) {
		Point2D p1 = findNodePosition(n1);
		Point2D p2 = findNodePosition(n2);
		return p1.distance(p2);
	}

}
