package geometry;

import static java.lang.Double.max;
import static java.lang.Math.abs;

import java.util.ArrayList;
import java.util.List;

import graph.Point;

public class SizeEstimator {

    public static<T extends Point> double findDiameter(List<T> vertices) {
        List<T> hull = SizeEstimator.findConvexHull(vertices);
        double diameter = 0.0;
        int n = hull.size();
        if (n == 1) return 0;
        if (n == 2) return hull.get(0).getLength(hull.get(0));
        int k = 1;
        while (abs(SizeEstimator.leftTurn(hull.get(n-1), hull.get(0), hull.get((k+1) % n))) > abs(SizeEstimator.leftTurn(hull.get(n-1), hull.get(0), hull.get(k)))) {
            k++;
        }
        for (int i = 0, j = k; i <= k && j < n; i++) {
            diameter = max(diameter, hull.get(i).getLength(hull.get(j)));
            while (j < n && abs(SizeEstimator.leftTurn(hull.get(i), hull.get((i + 1) % n), hull.get((j + 1) % n))) > abs(SizeEstimator.leftTurn(hull.get(i), hull.get((i + 1) % n), hull.get(j)))) {
                diameter = max(diameter, hull.get(i).getLength(hull.get((j + 1) % n)));
                j++;
            }
        }
        return diameter;
    }

    public static<T extends Point> Point findMinEnclosingCircleCenter(List<T> vertices) {
        double sumX = 0.0;
        double sumY = 0.0;
    
        for (T vertex : vertices) {
            sumX += vertex.x;
            sumY += vertex.y;
        }
    
        double centerX = sumX / vertices.size();
        double centerY = sumY / vertices.size();
    
        return new Point(centerX, centerY);
    }

    public static <T extends Point> double findRadius(List<T> vertices) {
        if (vertices.size() < 2) {
            return 0.0;
        } else if (vertices.size() == 2) {
            T a = vertices.get(0);
            T b = vertices.get(1);
            return a.coordinateDiff(b).norm() / 2.0;
        }
    
        Point center = findMinEnclosingCircleCenter(vertices);
        double maxRadius = 0.0;
    
        for (T vertex : vertices) {
            double distance = vertex.coordinateDiff(center).norm();
            if (distance > maxRadius) {
                maxRadius = distance;
            }
        }
    
        return maxRadius;
    }

    public static double leftTurn(Point a, Point b, Point c) {
        return (c.x - a.x) * (b.y - a.y) - (c.y - a.y) * (b.x - a.x);
    }

    public static<T extends Point> List<T> findConvexHull(List<T> vertices) {
        T finalInitVertex = getInitVertex(vertices);
    
        vertices.sort((a, b) -> {
            Point coorDistA = finalInitVertex.coordinateDiff(a);
            Point coorDistB = finalInitVertex.coordinateDiff(b);
    
            double angleA = Math.atan2(coorDistA.y, coorDistA.x);
            double angleB = Math.atan2(coorDistB.y, coorDistB.x);
    
            int cmp = Double.compare(angleA, angleB);
            if (cmp != 0) return cmp;
    
            double distanceA = coorDistA.normSq();
            double distanceB = coorDistB.normSq();
            return Double.compare(distanceA, distanceB);    
        });
    
        List<T> hull = new ArrayList<>();
    
        for (T vertex : vertices) {
            while (hull.size() >= 2) {
    
                double crossProduct = getCrossProduct(vertex, hull);
    
                if (crossProduct <= 0) {
                    hull.remove(hull.size() - 1);
                } else {
                    break;
                }
            }
            hull.add(vertex);
        }
    
        return hull;
    }

    public static<T extends Point> T getInitVertex(List<T> vertices) {
        if (vertices.size() < 3) {
            throw new IllegalArgumentException("Convex hull calculation requires at least 3 points");
        }
    
        T initVertex = vertices.get(0);
        for (T vertex : vertices) {
            if (vertex.x < initVertex.x ||
                    (vertex.x == initVertex.x &&
                            vertex.y < initVertex.y)) {
                initVertex = vertex;
            }
        }
    
        return initVertex;
    }

    public static<T extends Point> double getCrossProduct(T vertex, List<T> hull) {
        T last = hull.get(hull.size() - 1);
        T secondLast = hull.get(hull.size() - 2);
    
        Point lastVec = secondLast.coordinateDiff(last);
        Point newVec = last.coordinateDiff(vertex);
    
        return lastVec.x * newVec.y - lastVec.y * newVec.x;
    }
    
}
