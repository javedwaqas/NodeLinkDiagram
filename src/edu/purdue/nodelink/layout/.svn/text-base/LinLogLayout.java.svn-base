//Copyright (C) 2005 Andreas Noack
//
//This library is free software; you can redistribute it and/or
//modify it under the terms of the GNU Lesser General Public
//License as published by the Free Software Foundation; either
//version 2.1 of the License, or (at your option) any later version.
//
//This library is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//Lesser General Public License for more details.
//
//You should have received a copy of the GNU Lesser General Public
//License along with this library; if not, write to the Free Software
//Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA 
package edu.purdue.nodelink.layout;

import java.awt.Dimension;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;

import edu.purdue.nodelink.visualization.NodeLinkGraph;
import edu.purdue.pivotlib.data.IntColumn;
import edu.purdue.pivotlib.data.RealColumn;
import edu.purdue.pivotlib.data.StringColumn;
import edu.purdue.pivotlib.data.Table;
import edu.purdue.pivotlib.graph.Graph;

/**
 * Simple program for computing graph layouts (positions of the nodes of a graph
 * in two- or three-dimensional space) for visualization and knowledge discovery.
 * Reads a graph from a file, computes a layout, writes the layout to a file,
 * and displays the layout in a dialog.
 * The program can be used to identify clusters (groups of densely connected 
 * nodes) in graphs, like groups of friends or collaborators in social networks,
 * related documents in hyperlink structures (e.g. web graphs),
 * cohesive subsystems in software models, etc.
 * With a change of a parameter in the <code>main</code> method,
 * it can also compute classical "nice" (i.e. readable) force-directed layouts.
 * The program is intended as a demo of the use of its core layouter class 
 * <code>MinimizerBarnesHut</code>.  See this class for details about layouts.
 * 
 * @author Andreas Noack (an@informatik.tu-cottbus.de)
 * @version 28.09.2005
 */
public class LinLogLayout implements Layout {
	
	private static Map<String,Map<String,Float>> convertGraph(Graph g) {
		
		Map<String,Map<String,Float>> result = new TreeMap<String,Map<String,Float>>();
		Table vertexTable = g.getVertexTable();
		StringColumn idColumn = (StringColumn) vertexTable.getColumn("id");
		for (int i = 0; i< g.getVertexCount();i++) {
			String source = idColumn.getStringValueAt(i);
			if (result.get(source) == null) {
				result.put(source, new HashMap<String,Float>());
								
			}
		}
		
		Table edgeTable = g.getEdgeTable();
		IntColumn fromColumn = (IntColumn) edgeTable.getColumn("from");
		IntColumn toColumn = (IntColumn) edgeTable.getColumn("to");
		for (int i = 0; i < g.getEdgeCount(); i++) {
			int from = fromColumn.getIntValueAt(i);
			int to = toColumn.getIntValueAt(i);
			String source = idColumn.getStringValueAt(from);
			String target = idColumn.getStringValueAt(to);
			float weight = 1.0f;
			result.get(source).put(target, weight);
		}
		return result;
	}
	
	/**
	 * Returns a symmetric version of the given graph.
	 * A graph is symmetric if and only if for each pair of nodes,
	 * the weight of the edge from the first to the second node
	 * equals the weight of the edge from the second to the first node.
	 * Here the symmetric version is obtained by adding to each edge weight
	 * the weight of the inverse edge.
	 * 
	 * @param graph  possibly unsymmetric graph.
	 * @return symmetric version of the given graph.
	 */
	private static Map<String,Map<String,Float>> makeSymmetricGraph
			(Map<String,Map<String,Float>> graph) {
		Map<String,Map<String,Float>> result = new HashMap<String,Map<String,Float>>();
		for (String source : graph.keySet()) {
			if (result.get(source) == null) result.put(source, new HashMap<String,Float>());
			for (String target : graph.get(source).keySet()) {
				float weight = graph.get(source).get(target);
				float revWeight = 0.0f;
				if (graph.get(target) != null && graph.get(target).get(source) != null) {
					revWeight = graph.get(target).get(source);
				}
				if (result.get(source) == null) result.put(source, new HashMap<String,Float>());
				result.get(source).put(target, weight+revWeight);
				if (result.get(target) == null) result.put(target, new HashMap<String,Float>());
				result.get(target).put(source, weight+revWeight);
			}			
		}
		return result;
	}
	
	/**
	 * Returns a map from each node of the given graph 
	 * to a unique number from 0 to (number of nodes minus 1). 
	 * 
	 * @param graph the graph.
	 * @return map from each node of the given graph 
	 *         to a unique number from 0 to (number of nodes minus 1).
	 */
	private static Map<String,Integer> makeIds(Map<String,Map<String,Float>> graph) {
		Map<String,Integer> result = new HashMap<String,Integer>();
		int cnt = 0;
		for (String node : graph.keySet()) {
			result.put(node, cnt);
			cnt++;
		}
		return result;
	}
	
	/**
	 * Returns a map from each node of the given graph 
	 * to a random initial position in three-dimensional space. 
	 * 
	 * @param graph the graph.
	 * @return map from each node of the given graph 
	 * 		   to a random initial position in three-dimensional space.
	 */
	private static float[][] makeInitialPositions(Map<String,Map<String,Float>> graph) {
		float[][] result = new float[graph.size()][3];
		for (int i = 0; i < result.length; i++) {
			result[i][0] = (float)Math.random() - 0.5f;
			result[i][1] = (float)Math.random() - 0.5f;
			result[i][2] = 0.0f; // set to 0.0f for 2D layouts,
			                     // and to a random number for 3D.
		}
		return result;
	}
	
	/**
	 * Converts the edge weights of the given graph into the adjacency list 
	 * format expected by <code>MinimizerBarnesHut</code>.
	 * 
	 * @param graph    the graph.
	 * @param nodeToId unique ids of the graph nodes.
	 * @return array of adjacency lists for <code>MinimizerBarnesHut</code>.
	 */
	private static float[][] makeAttrWeights
			(Map<String,Map<String,Float>> graph, Map<String,Integer> nodeToId) {
		float[][] result = new float[graph.size()][];
		for (String source : graph.keySet()) {
			int sourceId = nodeToId.get(source);
			result[sourceId] = new float[graph.get(source).size()];
			int cnt = 0;
			for (String target : graph.get(source).keySet()) {
				result[sourceId][cnt] = graph.get(source).get(target);
				cnt++;
			}
		}
		return result;
	}
	
	/**
	 * Converts the edges of the given graph into the adjacency list 
	 * format expected by <code>MinimizerBarnesHut</code>.
	 * 
	 * @param graph    the graph.
	 * @param nodeToId unique ids of the graph nodes.
	 * @return array of adjacency lists for <code>MinimizerBarnesHut</code>.
	 */
	private static int[][] makeAttrIndexes
			(Map<String,Map<String,Float>> graph, Map<String,Integer> nodeToId) {
		int[][] result = new int[graph.size()][];
		for (String source : graph.keySet()) {
			int sourceId = nodeToId.get(source);
			result[sourceId] = new int[graph.get(source).size()];
			int cnt = 0;
			for (String target : graph.get(source).keySet()) {
				result[sourceId][cnt] = nodeToId.get(target);
				cnt++;
			}
		}
		return result;
	}

	/**
	 * Computes the repulsion weights of the nodes  
	 * for <code>MinimizerBarnesHut</code>.
	 * In the edge repulsion LinLog energy model, the repulsion weight
	 * of each node is its degree, i.e. the sum of the weights of its edges.
	 * 
	 * @param graph    the graph.
	 * @param nodeToId unique ids of the graph nodes.
	 * @return array of repulsion weights for <code>MinimizerBarnesHut</code>.
	 */
	private static float[] makeRepuWeights
			(Map<String,Map<String,Float>> graph, Map<String,Integer> nodeToId) {
		float[] result = new float[graph.size()];
		for (String source : graph.keySet()) {
			int sourceId = nodeToId.get(source);
			result[sourceId] = 0.0f;
			for (Float weight : graph.get(source).values()) {
				result[sourceId] += weight;
			}
		}
		return result;
	}

	/**
	 * Converts the array of node positions from <code>MinimizerBarnesHut</code>
	 * into a map from each node to its position.
	 * 
	 * @param positions array of node positions.
	 * @param nodeToId unique ids of the graph nodes.
	 * @return map from each node to its positions.
	 */
	private static Map<String,float[]> convertPositions
			(float[][] positions, Map<String,Integer> nodeToId) {
		Map<String,float[]> result = new HashMap<String,float[]>();
		for (String node : nodeToId.keySet()) {
			result.put(node, positions[nodeToId.get(node)]);
		}
		return result;
	}
	
	/**
	 * Computes a map from each node to the diameter of the circle
	 * that represents the node in the visualization.
	 * Here the square root of the degree (the total weight of the edges)
	 * of the node is used as diameter, thus the area of the circle
	 * is proportional to the degree.  
	 * 
	 * @param graph the graph.
	 * @return map from each node to its diameter in the visualization.
	 */
	@SuppressWarnings("unused")
	private static Map<String,Float> computeDiameters(Map<String,Map<String,Float>> graph) {
		Map<String,Float> result = new HashMap<String,Float>();
		for (String source : graph.keySet()) {
			float degree = 0.0f;
			for (Float weight : graph.get(source).values()) degree += weight;
			// degree = 100;
			result.put(source, (float)Math.sqrt(degree));
		}
		return result;
	}
	
	public void layout(Graph g, Dimension dim) { 		
    	Map<String,Map<String,Float>> graph = LinLogLayout.convertGraph(g);
    	graph = LinLogLayout.makeSymmetricGraph(graph);
   		Map<String,Integer> nodeToId = LinLogLayout.makeIds(graph);
   		float[][] positions = LinLogLayout.makeInitialPositions(graph);

   		// Now run the minimizer
		MinimizerBarnesHut minimizer = new MinimizerBarnesHut(
				LinLogLayout.makeAttrIndexes(graph, nodeToId),
				LinLogLayout.makeAttrWeights(graph, nodeToId), 
				LinLogLayout.makeRepuWeights(graph, nodeToId),
				3.0f, -1.0f, 0.01f, positions);
		minimizer.minimizeEnergy(100);
		
		Map<String, float[]> nodeToPosition = LinLogLayout.convertPositions(positions, nodeToId);
		
		// Find extremes of layout
		float minX = Float.MAX_VALUE; float maxX = -Float.MAX_VALUE;
		float minY = Float.MAX_VALUE; float maxY = -Float.MAX_VALUE;
		
		for (String node : nodeToPosition.keySet()) {
			float[] position = nodeToPosition.get(node);
			float diameter = 0; //nodeToDiameter.get(node);
			minX = Math.min(minX, position[0] - diameter / 2);
			maxX = Math.max(maxX, position[0] + diameter / 2);
			minY = Math.min(minY, position[1] - diameter / 2);
			maxY = Math.max(maxY, position[1] + diameter / 2);
		}
		
		float moveX = -minX;
		float moveY = -minY;
		float scale = Math.min((float) dim.getWidth() / (maxX - minX), (float) dim.getHeight() / (maxY-minY));
		
		Table vertexTable = g.getVertexTable();
		StringColumn idColumn = (StringColumn) vertexTable.getColumn("id");
		
		// Set the node positions 
		RealColumn xCol = (RealColumn) g.getVertexTable().getColumn(NodeLinkGraph.X_COLUMN);
		RealColumn yCol = (RealColumn) g.getVertexTable().getColumn(NodeLinkGraph.Y_COLUMN);
		if (xCol == null || yCol == null) return;
		for (int i = 0; i < g.getVertexCount(); i++) { 
			float x = ((nodeToPosition.get(idColumn.getStringValueAt(i))[0] + moveX) * scale);
			float y = ((nodeToPosition.get(idColumn.getStringValueAt(i))[1] + moveY) * scale);
			xCol.setValueAt(i, new Double(x));
			yCol.setValueAt(i, new Double(y));
		}
	}
}
