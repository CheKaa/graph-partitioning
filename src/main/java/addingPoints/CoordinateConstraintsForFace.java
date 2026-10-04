package addingPoints;

import java.util.ArrayList;

import graph.Vertex;

public class CoordinateConstraintsForFace {
	private double maxX;
	private double minX;
	private double maxY;
	private double minY;

	public CoordinateConstraintsForFace(ArrayList<Vertex> verticesOfFace) {
		Vertex start = verticesOfFace.get(0);
		maxX = start.x;
		minX = start.x;
		maxY = start.y;
		minY = start.y;
		for (int i = 1; i < verticesOfFace.size(); i++) {
			Vertex curr = verticesOfFace.get(i);
			if (maxX < curr.x) maxX = curr.x;
			if (minX > curr.x) minX = curr.x;
			if (maxY < curr.y) maxY = curr.y;
			if (minY > curr.y) minY = curr.y;
		}
		
	}
	public double getMinX() {
		return minX;
	}
	public double getMaxX() {
		return maxX;
	}
	public double getMaxY() {
		return maxY;
	}
	public double getMinY() {
		return minY;
	}
}
