package edu.purdue.nodelink.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import edu.purdue.nodelink.layout.LinLogLayout;
import edu.purdue.nodelink.ui.NodeLinkCanvas;
import edu.purdue.pivotlib.graph.BasicGraph;
import edu.purdue.pivotlib.graph.Graph;
import edu.purdue.pivotlib.io.GraphMLReader;

public class NodeLinkApplication extends JFrame {
	
	private static final long serialVersionUID = 1L;
	private File file;
	private Graph graph = new BasicGraph("graph");
	private NodeLinkCanvas nodeLinkCanvas;
	
	public NodeLinkApplication(File file) {
		this.file = file;
		setTitle("Node-Link Graph Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        nodeLinkCanvas = new NodeLinkCanvas(1024, 768);
        getContentPane().add(nodeLinkCanvas);
        pack();
        setVisible(true);
        initialize();
	}

	public void initialize() {
		try {
			InputStream in = new FileInputStream(file);
			GraphMLReader reader = new GraphMLReader(in, "graph", graph);
			reader.load();
			
			System.err.println("Read graph '" + graph.getName() + "' with " + graph.getVertexCount() + " vertices and " + graph.getEdgeCount() + " edges.");
			
			// Create the graph
			nodeLinkCanvas.setGraph(graph);
			
			// Run a layout on the graph
			nodeLinkCanvas.layout(new LinLogLayout());
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		JFileChooser fileChooser = new JFileChooser(".");
        int ret = fileChooser.showOpenDialog(null);
        if (ret == JFileChooser.APPROVE_OPTION) {        	
    		new NodeLinkApplication(fileChooser.getSelectedFile());
        }		
	}
}
