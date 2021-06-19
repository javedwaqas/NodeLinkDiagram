package edu.purdue.nodelink.visualization;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import edu.purdue.pivotlib.data.IntColumn;
import edu.purdue.pivotlib.data.RealColumn;
import edu.purdue.pivotlib.data.StringColumn;
import edu.purdue.pivotlib.data.Table;
import edu.purdue.pivotlib.graph.Graph;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PDragEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;

public class NodeLinkGraph {
	
	public static final String X_COLUMN = "#X";
	public static final String Y_COLUMN = "#Y";
	
	private Graph graph;
	private PNode nodeRoot = new PNode(), edgeRoot = new PNode();
	private DragEventHandler dragEventHandler = new DragEventHandler();
	
	private class DragEventHandler extends PDragEventHandler {
        public void mouseEntered(PInputEvent e) {
            super.mouseEntered(e);
            if (e.getButton() == MouseEvent.NOBUTTON) {
                e.getPickedNode().setPaint(Color.RED);
            }
        }
        
        public void mouseExited(PInputEvent e) {
            super.mouseExited(e);
            if (e.getButton() == MouseEvent.NOBUTTON) {
                e.getPickedNode().setPaint(Color.WHITE);
            }
        }
        
        protected void startDrag(PInputEvent e) {
            super.startDrag(e);
            e.setHandled(true);
            e.getPickedNode().moveToFront();
        }
        
		@SuppressWarnings("unchecked")
		protected void drag(PInputEvent e) {
            super.drag(e);
            
            ArrayList<PPath> edges = (ArrayList<PPath>) e.getPickedNode().getAttribute("edges");
            for (int i = 0; i < edges.size(); i++) {
                updateEdge((PPath) edges.get(i));
            }
        }	
	}
	
	public NodeLinkGraph(Graph graph) {
		this.graph = graph;
		
		RealColumn xCol = new RealColumn(X_COLUMN, true);
		RealColumn yCol = new RealColumn(Y_COLUMN, true);
		xCol.ensureCapacity(graph.getVertexCount());
		yCol.ensureCapacity(graph.getVertexCount());
		graph.getVertexTable().addColumn(xCol);		
		graph.getVertexTable().addColumn(yCol);

		Table vertexTable = graph.getVertexTable();
		StringColumn idColumn = (StringColumn) vertexTable.getColumn("id");
		for (int i = 0; i < vertexTable.getRowCount(); i++) { 
			addNode(idColumn.getStringValueAt(i));
		}
		
		Table edgeTable = graph.getEdgeTable();
		IntColumn fromColumn = (IntColumn) edgeTable.getColumn("from");
		IntColumn toColumn = (IntColumn) edgeTable.getColumn("to");
		for (int i = 0; i < edgeTable.getRowCount(); i++) {
			int from = fromColumn.getIntValueAt(i); 
			int to = toColumn.getIntValueAt(i);
			addEdge(from, to);
		}
	} 
	
	public PDragEventHandler getDragEventHandler() { 
		return dragEventHandler;
	}
	
	public Graph getGraph() {
		return graph;
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
	}
	
	public void update() { 

		// Retrieve the position columns
		RealColumn xCol = (RealColumn) graph.getVertexTable().getColumn(X_COLUMN);
		RealColumn yCol = (RealColumn) graph.getVertexTable().getColumn(Y_COLUMN);
		
		// Update node positions
		for (int i = 0; i < graph.getVertexCount(); i++) {
			double x = xCol.getRealValueAt(i);
			double y = yCol.getRealValueAt(i);
			nodeRoot.getChild(i).setOffset(x, y);
		}
		
		// Update edge positions
		for (int i = 0; i < graph.getEdgeCount(); i++) {
			PNode edge = edgeRoot.getChild(i);
			updateEdge((PPath) edge);
		}		
	}
	
	private PPath addNode(String name) {
		PPath node = PPath.createEllipse(0, 0, 20, 20);
        node.addAttribute("edges", new ArrayList<PPath>());
        node.addAttribute("name", name);
        PText text = new PText(name);
        text.translate(0, -15);
        node.addChild(text);
        node.setChildrenPickable(false);
        nodeRoot.addChild(node);
        return node;
	}
	
	@SuppressWarnings("unchecked")
	private void addEdge(int n1, int n2) {
		PNode node1 = nodeRoot.getChild(n1);
        PNode node2 = nodeRoot.getChild(n2);

        PPath edge = new PPath();
        ((ArrayList<PPath>) node1.getAttribute("edges")).add(edge);
        ((ArrayList<PPath>) node2.getAttribute("edges")).add(edge);
        edge.addAttribute("nodes", new ArrayList<PNode>());
        ((ArrayList<PNode>) edge.getAttribute("nodes")).add(node1);
        ((ArrayList<PNode>) edge.getAttribute("nodes")).add(node2);
        edgeRoot.addChild(edge);
        updateEdge(edge);    
	}
    
    @SuppressWarnings("unchecked")
	private void updateEdge(PPath edge) {
        PNode node1 = (PNode) ((ArrayList<PNode>) edge.getAttribute("nodes")).get(0);
        PNode node2 = (PNode) ((ArrayList<PNode>) edge.getAttribute("nodes")).get(1);
        Point2D start = node1.getFullBoundsReference().getCenter2D();
        Point2D end = node2.getFullBoundsReference().getCenter2D();
        edge.reset();
        edge.moveTo((float) start.getX(), (float) start.getY());
        edge.lineTo((float) end.getX(), (float) end.getY());
    }
}
