package edu.purdue.nodelink.ui;

import java.awt.Dimension;

import edu.purdue.nodelink.layout.Layout;
import edu.purdue.nodelink.visualization.NodeLinkGraph;
import edu.purdue.pivotlib.graph.Graph;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PLayer;

public class NodeLinkCanvas extends PCanvas {
	
	private static final long serialVersionUID = 1L;
	private PLayer nodeLayer, edgeLayer;
	private NodeLinkGraph nodeLinkGraph;

	public NodeLinkCanvas(int width, int height) {
        setPreferredSize(new Dimension(width, height));

        // Initialize, and create a layer for the edges (always underneath the nodes)
        nodeLayer = getLayer();
        edgeLayer = new PLayer();
        getRoot().addChild(edgeLayer);
        getCamera().addLayer(0, edgeLayer);
    }
	
	public void clear() { 
		nodeLinkGraph.clear();
	}
	
	public void setGraph(Graph g) { 
		
		if (nodeLinkGraph != null) { 
			nodeLayer.removeAllChildren();
			edgeLayer.removeAllChildren();
		}
		
		// Create the new graph
		nodeLinkGraph = new NodeLinkGraph(g);

		// Add the roots to the layers
        nodeLayer.addChild(nodeLinkGraph.getNodeRoot());
        edgeLayer.addChild(nodeLinkGraph.getEdgeRoot());
	}
	
	public void layout(Layout layout) { 
		if (nodeLinkGraph != null) { 
			layout.layout(nodeLinkGraph.getGraph(), getSize());
			nodeLinkGraph.update();
		}
	}
}
