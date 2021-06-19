package edu.purdue.nodelink.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import edu.purdue.nodelink.aggregation.AggGraph;
import edu.purdue.nodelink.aggregation.GraphHierAggregation;
import edu.purdue.nodelink.aggregation.MidPointDistance;
import edu.purdue.nodelink.layout.LinLogLayout;
import edu.purdue.nodelink.ui.AggNodeLinkCanvas;
import edu.purdue.pivotlib.graph.BasicGraph;
import edu.purdue.pivotlib.graph.Graph;
import edu.purdue.pivotlib.io.GraphMLReader;

public class AggNodeLinkApplication extends JFrame {
	
	private static final long serialVersionUID = 1L;
	private AggNodeLinkCanvas nodeLinkCanvas;
	private File file;
	
	public AggNodeLinkApplication(File file) {
		this.file = file;
		setTitle("Node-Link Graph Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        nodeLinkCanvas = new AggNodeLinkCanvas(1024, 768);
        getContentPane().add(nodeLinkCanvas);
        pack();
        setVisible(true);
        initialize();
	}

	public void initialize() {
		try {
			InputStream in = new FileInputStream(file);
			Graph graph = new BasicGraph("graph"); 
			GraphMLReader reader = new GraphMLReader(in, "graph", graph);
			reader.load();
			
			System.err.println("Read graph '" + graph.getName() + "' with " + graph.getVertexCount() + " vertices and " + graph.getEdgeCount() + " edges.");
			graph.expandUndirected();
						
			// Create the graph
			AggGraph agg = new AggGraph(graph);
			nodeLinkCanvas.setGraph(agg);
						
			// Run a layout on the graph
			nodeLinkCanvas.layout(new LinLogLayout());
			
			// Run the hierarchical aggregation process
			GraphHierAggregation.aggregateGraph(agg, new MidPointDistance(agg));
//			nodeLinkCanvas.update();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		JFileChooser fileChooser = new JFileChooser(".");
        int ret = fileChooser.showOpenDialog(null);
        if (ret == JFileChooser.APPROVE_OPTION) {        	
    		new AggNodeLinkApplication(fileChooser.getSelectedFile());
        }		
	}
}
