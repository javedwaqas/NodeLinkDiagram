package edu.purdue.nodelink.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import edu.purdue.nodelink.aggregation.AggGraph;
import edu.purdue.nodelink.layout.Layout;
import edu.purdue.nodelink.visualization.AggNodeLinkGraph;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PPanEventHandler;
import edu.umd.cs.piccolo.nodes.PPath;

public class AggNodeLinkCanvas extends PCanvas implements PropertyChangeListener {
	
	private static final long serialVersionUID = 1L;
	private PLayer nodeLayer, edgeLayer;
	private AggNodeLinkGraph nodeLinkGraph;
	
	private class AggControlTool implements KeyListener {
		public void keyPressed(KeyEvent e) {}
		public void keyReleased(KeyEvent e) {}
		public void keyTyped(KeyEvent e) {
			if (Character.isDigit(e.getKeyChar())) { 
				int level = e.getKeyChar() - '0';
				if (level == 0) level = 10;
				nodeLinkGraph.setVisibleItemRatio(level / 10.0);
			}			
		}
	}
	
	@SuppressWarnings("unused")
	private class LassoTool extends PBasicInputEventHandler {
		private BasicStroke stroke = new BasicStroke(2.0f);
		private PPath selection;
		private ArrayList<Point2D> positions = new ArrayList<Point2D>();
		private boolean dragging = false;
		private PPanEventHandler panHandler = new PPanEventHandler();
		
		public void mousePressed(PInputEvent event) {
			if (event.getButton() == MouseEvent.BUTTON2 || (event.isControlDown() && event.getButton() == MouseEvent.BUTTON1)) { 
				positions.clear();
				positions.add(event.getPosition());
				updateSelection(nodeLinkGraph.getNodeRoot());
				dragging = true;
				event.setHandled(true);
			}
			else { 
				panHandler.mousePressed(event);
			}
		}		
		public void mouseDragged(PInputEvent event) { 
			if (dragging) {
				positions.add(event.getPosition());
				updateSelection(nodeLinkGraph.getNodeRoot());
				event.setHandled(true);
			}
			else { 
				panHandler.mouseDragged(event);
			}
		}
		public void mouseReleased(PInputEvent event) {
			if (dragging) { 
				ArrayList<PNode> selected = findSelectedObjects(nodeLinkGraph.getNodeRoot());
				System.err.println(" agg " + selected + ", " + selected.size());
				if (selected.size() > 1) {
					nodeLinkGraph.aggregate(selected);
				}
				nodeLinkGraph.getNodeRoot().removeChild(selection);
				dragging = false;
				event.setHandled(true);
			}
			else { 
				panHandler.mouseReleased(event);
			}
		}
		private void updateSelection(PNode node) { 
			if (node.indexOfChild(selection) != -1) {
				node.removeChild(selection);
			}
			selection = PPath.createPolyline(positions.toArray(new Point2D[positions.size()]));
			selection.setStroke(stroke);
			selection.setPaint(new Color(0.8f, 0.8f, 0.8f, 0.5f));
			selection.setStrokePaint(Color.lightGray);
			node.addChild(selection);
		}
		private ArrayList<PNode> findSelectedObjects(PNode node) {
			ArrayList<PNode> selected = new ArrayList<PNode>();
			Area selectionArea = new Area(selection.getPathReference());

			// Step through all of the PNodes
			for (int i = 0; i < node.getChildrenCount(); i++) { 
				PNode child = node.getChild(i);
				if (child == selection) continue;
				if (intersectsNode(child, selectionArea)) { 
					selected.add(child);
				}
			}
			return selected;
		}
		private boolean intersectsNode(PNode node, Area selectionArea) {
			
			// Base case: This node
			if (node instanceof PPath) { 

				// Compute areas
				PPath childPath = (PPath) node;
				Area childArea = new Area(childPath.getPathReference());
				
				// Transform the child
				double scale = node.getScale();
				childArea.transform(AffineTransform.getTranslateInstance(node.getOffset().getX(), node.getOffset().getY()));
				childArea.transform(AffineTransform.getScaleInstance(scale, scale));
				
				// Intersect them
				childArea.intersect(selectionArea);
				if (!childArea.isEmpty()) return true;				
			}
			
			// Recursive case: Step through children
			for (int i = 0; i < node.getChildrenCount(); i++) { 
				PNode child = node.getChild(i);
				if (intersectsNode(child, selectionArea)) return true;
			}
			return false;
		}
	}

	public AggNodeLinkCanvas(int width, int height) {
        setPreferredSize(new Dimension(width, height));

        // Initialize, and create a layer for the edges (always underneath the nodes)
        nodeLayer = getLayer();
        edgeLayer = new PLayer();
        getRoot().addChild(edgeLayer);
        getCamera().addLayer(0, edgeLayer);
        
        // Add a mouse wheel listener
        addMouseWheelListener(new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (nodeLinkGraph == null) return;
				if (e.getWheelRotation() < 0) {
					nodeLinkGraph.drillDown(1);
				}
				else { 
					nodeLinkGraph.rollUp(1);
				}
			} 
        });
        
//        setPanEventHandler(null);
//        setZoomEventHandler(null);
        
        // Add a lasso tool for aggregation
//        addInputEventListener(new LassoTool());
        addKeyListener(new AggControlTool());
    }
	
	public void clear() { 
		nodeLinkGraph.clear();
	}
	
	public void setGraph(AggGraph graph) { 
		
		if (nodeLinkGraph != null) { 
			nodeLayer.removeAllChildren();
			edgeLayer.removeAllChildren();
			nodeLinkGraph.getGraph().removePropertyChangeListener(this);
		}
		
		// Create the new graph
		nodeLinkGraph = new AggNodeLinkGraph(graph);
		graph.addPropertyChangeListener(this);

		// Add the roots to the layers
        nodeLayer.addChild(nodeLinkGraph.getNodeRoot());
        edgeLayer.addChild(nodeLinkGraph.getEdgeRoot());
	}
	
	public void layout(Layout layout) { 
		if (nodeLinkGraph != null) { 
			layout.layout(nodeLinkGraph.getGraph().getGraph(), getSize());
		}
	}

	public void propertyChange(PropertyChangeEvent evt) {
		repaint();
	}
}
