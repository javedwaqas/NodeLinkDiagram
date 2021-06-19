package edu.purdue.nodelink.layout;

import java.awt.Dimension;
import java.util.Random;

import edu.purdue.nodelink.visualization.NodeLinkGraph;
import edu.purdue.pivotlib.data.RealColumn;
import edu.purdue.pivotlib.graph.Graph;

public class RandomLayout implements Layout {
	
	public void layout(Graph graph, Dimension dim) { 
		Random random = new Random();

		RealColumn xCol = (RealColumn) graph.getVertexTable().getColumn(NodeLinkGraph.X_COLUMN);
		RealColumn yCol = (RealColumn) graph.getVertexTable().getColumn(NodeLinkGraph.Y_COLUMN);
		if (xCol == null || yCol == null) return;
		for (int i = 0; i < graph.getVertexCount(); i++) {
			float x = random.nextInt(dim.width);
	        float y = random.nextInt(dim.height);
			xCol.setValueAt(i, new Double(x));
			yCol.setValueAt(i, new Double(y));	        
		}
	}
}
