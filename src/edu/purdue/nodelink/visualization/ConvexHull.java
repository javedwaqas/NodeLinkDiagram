/* ------------------------------------------------------------------
 * Blob.java
 * 
 * Created 2008-03-07 by Niklas Elmqvist <elm@lri.fr>.
 * Adapted to Pivot 2009-06-12 by Niklas Elmqvist <elm@purdue.edu>.
 * ------------------------------------------------------------------
 */
package edu.purdue.nodelink.visualization;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ConvexHull {

	private static class Line {
		private Point2D p1, p2;		
		public Line(Point2D p1, Point2D p2) {
			this.p1 = p1;
			this.p2 = p2;
		}
		public int relativeCCW(Point2D point) {
			return Line.relativeCCW(p1.getX(), p1.getY(), p2.getX(), p2.getY(), point.getX(), point.getY());
		}
		public static int relativeCCW(double x1, double y1, double x2, double y2, double px, double py) {
			
			// Coincident points
			if ((x1 == x2 && y1 == y2) || (x1 == px && y1 == py)) return 0;
			 
			// Translate to the origin
			x2 -= x1;
			y2 -= y1;
			px -= x1;
			py -= y1;
			 
			double slope2 = y2 / x2;
			double slopep = py / px;
			if (slope2 == slopep || (x2 == 0 && px == 0)) {
				return 0;
			}
			if (x2 >= 0 && slope2 >= 0) {
				return px >= 0 // Quadrant 1.
				 	? (slope2 > slopep ? 1 : -1)
				 	: (slope2 < slopep ? 1 : -1);
			}
			if (y2 > 0) {
				return px < 0 // Quadrant 2.
					? (slope2 > slopep ? 1 : -1)
					: (slope2 < slopep ? 1 : -1);
			}
			if (slope2 >= 0.0) {
				return px >= 0 // Quadrant 3.
				 	? (slope2 < slopep ? 1 : -1)
				 	: (slope2 > slopep ? 1 : -1);
			}
			return px < 0 // Quadrant 4.
			 	? (slope2 < slopep ? 1 : -1)
			 	: (slope2 > slopep ? 1 : -1);
		}
	}
	
	private ArrayList<Point2D> points = new ArrayList<Point2D>();	
	private ArrayList<Point2D> hull = new ArrayList<Point2D>();	
	
	public ConvexHull() {}
	
	public void clear() { 
		points.clear();
		hull.clear();
	}
	
	public void addPoint(Point2D point) { 
		points.add(point);
	}
	
	public ArrayList<Point2D> getHull() {
		return hull;
	}
	
	public Point2D[] getPath() {
		Point2D[] ps = new Point2D[hull.size() + 1];
		for (int i = 0; i < hull.size(); i++) {
			ps[i] = hull.get(i);
		}
		ps[hull.size()] = ps[0];
		return ps;
	}

	public void computeHull() {
		// Gift-wrapping convex hull computation (Jarvis) - O(nh)
	
		// Clear the old hull
		hull.clear();
			
		// Sort by y coordinate
		Collections.sort(points, new Comparator<Point2D> () {
			public int compare(Point2D p1, Point2D p2) {
				return p1.getY() < p2.getY() ? -1 : p1.getY() > p2.getY() ? 1 : 0; 
			}					
		});
			
		// Start with the extreme y-value---it is known to be on the hull
		hull.add(points.get(0));
		boolean addedVertex = true;
		while (addedVertex) {
			
			// Get the current point (A)
			Point2D currPoint = hull.get(hull.size() - 1);
			
			// Find a point B so that all points are to the left of AB
			Point2D newPoint = null;
			addedVertex = false;
			for (int i = 0; i < points.size(); i++) {
				
				// Construct the line AB
				Point2D nextPoint = points.get(i);
				if (currPoint == nextPoint) continue;
				Line line = new Line(currPoint, nextPoint);
				
				// Test remaining points w.r.t. this line 
				boolean allLeft = true;
				for (int j = 0; j < points.size(); j++) {
					Point2D testPoint = points.get(j);
					if (testPoint == currPoint || testPoint == nextPoint) continue;
					if (line.relativeCCW(testPoint) == -1) {
						allLeft = false;
						break;
					}
				}
				
				// Did they all fall to the left of the point?						
				if (allLeft) {
					newPoint = nextPoint;
					break;
				}
			}
			
			// Store the new point
			if (newPoint != null) {
				if (hull.contains(newPoint)) break;
				hull.add(newPoint);
				addedVertex = true;
			}
		}
	}
}
