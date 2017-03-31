package us.ihmc.euclid.geometry.tools;

import static org.junit.Assert.*;
import static us.ihmc.euclid.geometry.tools.EuclidGeometryPolygonTools.*;
import static us.ihmc.euclid.geometry.tools.EuclidGeometryRandomTools.*;
import static us.ihmc.euclid.geometry.tools.EuclidGeometryTools.*;
import static us.ihmc.euclid.tools.EuclidCoreRandomTools.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import us.ihmc.euclid.tools.EuclidCoreRandomTools;
import us.ihmc.euclid.tools.EuclidCoreTestTools;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.Vector2D;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;

public class EuclidGeometryPolygonToolsTest
{
   private static final double SMALL_EPSILON = 1.0e-9;
   private static final double SMALLEST_EPSILON = 1.0e-12;
   private static final int ITERATIONS = 1000;

   private static interface ConvexHullAlgorithm
   {
      int process(List<? extends Point2DReadOnly> vertices, int numberOfVertices);
   }

   @Test
   public void testInPlaceGiftWrapConvexHull2D() throws Exception
   {
      Random random = new Random(3245345L);
      testConvexHullAlgorithm(random, (vertices, numberOfVertices) -> inPlaceGiftWrapConvexHull2D(vertices, numberOfVertices));
   }

   @Test
   public void testInPlaceGrahamScanConvexHull2D() throws Exception
   {
      Random random = new Random(5641651419L);
      testConvexHullAlgorithm(random, (vertices, numberOfVertices) -> inPlaceGrahamScanConvexHull2D(vertices, numberOfVertices));
   }

   @Test
   public void testCompareConvexHullAlgorithms() throws Exception
   {
      Random random = new Random(23454L);

      List<ConvexHullAlgorithm> algorithmsToTest = new ArrayList<>();
      algorithmsToTest.add((vertices, numberOfVertices) -> inPlaceGiftWrapConvexHull2D(vertices, numberOfVertices));
      algorithmsToTest.add((vertices, numberOfVertices) -> inPlaceGrahamScanConvexHull2D(vertices, numberOfVertices));

      for (int i = 0; i < ITERATIONS; i++)
      {
         int numberOfVertices = 100;
         List<? extends Point2DReadOnly> points = generateRandomPointCloud2D(random, 10.0, 10.0, numberOfVertices);
         List<List<? extends Point2DReadOnly>> pointsForEachAlgo = new ArrayList<>();
         while (pointsForEachAlgo.size() < algorithmsToTest.size())
            pointsForEachAlgo.add(new ArrayList<>(points));

         List<Integer> hullSizes = new ArrayList<>();

         for (int index = 0; index < algorithmsToTest.size(); index++)
            hullSizes.add(algorithmsToTest.get(index).process(pointsForEachAlgo.get(index), numberOfVertices));

         // Compare the different algorithms against the first one
         for (int algoIndex = 1; algoIndex < algorithmsToTest.size(); algoIndex++)
         {
            assertEquals(hullSizes.get(0), hullSizes.get(algoIndex));
            for (int vertexIndex = 0; vertexIndex < hullSizes.get(0); vertexIndex++)
               assertTrue(pointsForEachAlgo.get(0).get(vertexIndex) == pointsForEachAlgo.get(algoIndex).get(vertexIndex));
         }
      }
   }

   private static void testConvexHullAlgorithm(Random random, ConvexHullAlgorithm algorithmToTest) throws Exception
   {
      for (int i = 0; i < ITERATIONS; i++)
      { // Test simple features
         int numberOfPoints = 100;
         int numberOfPointsToProcess = random.nextInt(numberOfPoints - 1) + 1;
         List<Point2D> listToProcess = generateRandomPointCloud2D(random, 10.0, 10.0, numberOfPoints);
         List<Point2D> original = new ArrayList<>(listToProcess);

         int hullSize = algorithmToTest.process(listToProcess, numberOfPointsToProcess);
         // Test the given list does not get resized.
         assertEquals(numberOfPoints, listToProcess.size());
         // Test that the hull size is smaller or equal to the original number of points.
         assertTrue(hullSize <= numberOfPointsToProcess);
         // Test that the points in [numberOfPointsToProcess, numberOfPoints[ remain unchanged.
         for (int index = numberOfPointsToProcess; index < numberOfPoints; index++)
            assertTrue(original.get(index) == listToProcess.get(index));
         // Test that processing a twice time does not do anything.
         List<? extends Point2DReadOnly> reprocessedList = new ArrayList<>(listToProcess);
         int reprocessedHullSize = algorithmToTest.process(reprocessedList, numberOfPointsToProcess);
         assertEquals(hullSize, reprocessedHullSize);
         for (int index = 0; index < numberOfPoints; index++)
            assertTrue(listToProcess.get(index) == reprocessedList.get(index));

         // Test that with numberOfPointsToProcess = 1, the algorithm does not do anything 
         numberOfPointsToProcess = 1;
         listToProcess = new ArrayList<>(original);
         hullSize = algorithmToTest.process(listToProcess, numberOfPointsToProcess);
         assertEquals(numberOfPointsToProcess, hullSize);
         assertTrue(original.get(0) == listToProcess.get(0));

         // Test that with numberOfPointsToProcess = 2, the algorithm just reorders the two vertices to start with minXMaxYVertex 
         numberOfPointsToProcess = 2;
         listToProcess = new ArrayList<>(original);
         hullSize = algorithmToTest.process(listToProcess, numberOfPointsToProcess);
         assertEquals(numberOfPointsToProcess, hullSize);
         if (EuclidGeometryPolygonTools.findMinXMaxYVertexIndex(original, numberOfPointsToProcess) == 0)
         {
            assertTrue(original.get(0) == listToProcess.get(0));
            assertTrue(original.get(1) == listToProcess.get(1));
         }
         else
         {
            assertTrue(original.get(1) == listToProcess.get(0));
            assertTrue(original.get(0) == listToProcess.get(1));
         }
      }

      for (int i = 0; i < ITERATIONS; i++)
      { // Test with vertices that already form a convex hull make sure the algorithm just shift the points around so it starts with the min x, max y vertex.
         int numberOfPoints = 100;
         List<Point2D> vertices = generateRandomCircleBasedConvexPolygon2D(random, 10.0, 1.0, numberOfPoints);
         int startIndex = EuclidGeometryPolygonTools.findMinXMaxYVertexIndex(vertices, numberOfPoints);
         List<Point2D> convexHullVertices = new ArrayList<>();
         for (int index = 0; index < numberOfPoints; index++)
            convexHullVertices.add(vertices.get(wrap(index + startIndex, numberOfPoints)));

         List<? extends Point2DReadOnly> copy = new ArrayList<>(convexHullVertices);
         algorithmToTest.process(copy, copy.size());

         for (int index = 0; index < copy.size(); index++)
            assertTrue("Failed at index: " + index, copy.get(index) == convexHullVertices.get(index));
      }

      for (int i = 0; i < ITERATIONS; i++)
      { // Test that the resulting list is convex at every vertex
         int numberOfPoints = 100;
         List<? extends Point2DReadOnly> points = generateRandomPointCloud2D(random, 10.0, 10.0, numberOfPoints);
         int hullSize = algorithmToTest.process(points, numberOfPoints);
         for (int index = 0; index < hullSize; index++)
            assertTrue("Is not convex at vertex index: " + index, EuclidGeometryPolygonTools.isPolygon2DConvexAtVertex(index, points, hullSize, true));
      }

      for (int i = 0; i < ITERATIONS; i++)
      { // Test that Graham scan sorting algorithm does change the output of the already processed vertices.
         int numberOfPoints = 100;
         List<? extends Point2DReadOnly> processedList = generateRandomPointCloud2D(random, 10.0, 10.0, numberOfPoints);
         int hullSize = algorithmToTest.process(processedList, numberOfPoints);
         List<? extends Point2DReadOnly> sortedList = new ArrayList<>(processedList);
         EuclidGeometryPolygonTools.grahamScanAngleSort(sortedList, hullSize);
         for (int index = 0; index < hullSize; index++)
            assertTrue(processedList.get(index) == sortedList.get(index));
      }

      for (int i = 0; i < ITERATIONS; i++)
      { // Test that the sum of the angles from edge to edge is equal to 2*PI, ensuring that the resulting hull does not do several revolutions
         int numberOfPoints = 100;
         List<? extends Point2DReadOnly> processedList = generateRandomPointCloud2D(random, 10.0, 10.0, numberOfPoints);
         int hullSize = algorithmToTest.process(processedList, numberOfPoints);

         double sumOfAngles = 0.0;

         for (int index = 0; index < hullSize; index++)
         {
            Point2DReadOnly previousVertex = processedList.get(previous(index, hullSize));
            Point2DReadOnly vertex = processedList.get(index);
            Point2DReadOnly nextVertex = processedList.get(next(index, hullSize));
            Vector2D previousEdge = new Vector2D();
            previousEdge.sub(vertex, previousVertex);
            Vector2D nextEdge = new Vector2D();
            nextEdge.sub(nextVertex, vertex);
            sumOfAngles -= previousEdge.angle(nextEdge); // Because the vertices are clockwise ordered.
         }

         assertEquals(2.0 * Math.PI, sumOfAngles, SMALLEST_EPSILON);
      }

      for (int i = 0; i < ITERATIONS; i++)
      { // Test that duplicate vertices are removed from the hull
         int numberOfPoints = 100;
         List<Point2D> processedList = generateRandomPointCloud2D(random, 10.0, 10.0, numberOfPoints);
         int numberOfDuplicates = random.nextInt(50);
         while (numberOfDuplicates > 0)
         {
            int indexToDuplicate = random.nextInt(processedList.size());
            processedList.add(new Point2D(processedList.get(indexToDuplicate)));
            numberOfDuplicates--;
         }
         Collections.shuffle(processedList);

         int hullSize = algorithmToTest.process(processedList, numberOfPoints);
         // Assert there is no duplicate in [0, hullSize[
         for (int firstIndex = 0; firstIndex < hullSize; firstIndex++)
         {
            Point2DReadOnly first = processedList.get(firstIndex);

            for (int secondIndex = firstIndex + 1; secondIndex < hullSize; secondIndex++)
            {
               Point2DReadOnly second = processedList.get(secondIndex);
               assertFalse(duplicateMessage(processedList, hullSize, firstIndex, secondIndex), first.epsilonEquals(second, EuclidGeometryPolygonTools.EPSILON));
            }
         }
      }
   }

   private static String duplicateMessage(List<Point2D> processedList, int hullSize, int firstIndex, int secondIndex)
   {
      return "Found duplicate vertices (" + firstIndex + " and " + secondIndex + ") \n" + processedList.subList(0, hullSize);
   }

   @Test
   public void testGrahamScanAngleSort() throws Exception
   {
      Random random = new Random(324234L);

      for (int i = 0; i < ITERATIONS; i++)
      {
         int numberOfPoints = 10;
         List<Point2D> points = generateRandomPointCloud2D(random, 10.0, 10.0, numberOfPoints);
         List<Point2D> pointsCopy = new ArrayList<>(points);

         int minXMaxYIndex = EuclidGeometryPolygonTools.findMinXMaxYVertexIndex(points, points.size());
         Point2D minXMaxYVertex = points.get(minXMaxYIndex);

         Comparator<Point2DReadOnly> comparator = (vertex1, vertex2) -> grahamScanAngleCompare(minXMaxYVertex, vertex1, vertex2);
         Collections.sort(points, comparator);

         assertTrue(minXMaxYVertex == points.get(0));

         Point2D offset = new Point2D(minXMaxYVertex);
         points.forEach(v -> v.sub(offset));

         List<Double> angles = new ArrayList<>();
         // x and y are flipped on purpose as the comparison is based on the angle with respect to the y-axis.
         points.forEach(v -> angles.add(Math.atan2(v.getX(), v.getY())));

         for (int index = 1; index < numberOfPoints - 1; index++)
            assertTrue(angles.get(index) < angles.get(index + 1));

         EuclidGeometryPolygonTools.grahamScanAngleSort(pointsCopy, numberOfPoints);
         for (int index = 0; index < numberOfPoints; index++)
            assertTrue(points.get(index) == pointsCopy.get(index));
      }
   }

   @Test
   public void testComputeConvexPolygon2DArea() throws Exception
   {
      Random random = new Random(345345L);

      for (int i = 0; i < ITERATIONS; i++)
      {
         List<? extends Point2DReadOnly> convexPolygon2D = generateRandomPointCloud2D(random, 10.0, 10.0, 100);
         int hullSize = EuclidGeometryPolygonTools.inPlaceGrahamScanConvexHull2D(convexPolygon2D);
         boolean clockwiseOrdered = random.nextBoolean();
         if (!clockwiseOrdered)
            Collections.reverse(convexPolygon2D.subList(0, hullSize));

         Point2D average = new Point2D();
         convexPolygon2D.subList(0, hullSize).forEach(average::add);
         average.scale(1.0 / hullSize);

         double expectedArea = 0.0;
         for (int index = 0; index < hullSize; index++)
         {
            Point2DReadOnly vertex = convexPolygon2D.get(index);
            Point2DReadOnly nextVertex = convexPolygon2D.get(next(index, hullSize));
            expectedArea += triangleArea(average, vertex, nextVertex);
         }
         Point2D centroid = new Point2D();
         double actualArea = computeConvexPolyong2DArea(convexPolygon2D, hullSize, clockwiseOrdered, centroid);
         assertEquals(expectedArea, actualArea, SMALLEST_EPSILON);

         Point2D recomputedCentroid = new Point2D();

         for (int index = 0; index < hullSize; index++)
         {
            Point2DReadOnly previousVertex = convexPolygon2D.get(previous(index, hullSize));
            Point2DReadOnly vertex = convexPolygon2D.get(index);
            Point2DReadOnly nextVertex = convexPolygon2D.get(next(index, hullSize));

            double vertexWeight = 0.0;
            vertexWeight += triangleArea(centroid, vertex, previousVertex);
            vertexWeight += triangleArea(centroid, vertex, nextVertex);
            vertexWeight /= 2.0 * actualArea;
            recomputedCentroid.add(vertexWeight * vertex.getX(), vertexWeight * vertex.getY());
         }
         EuclidCoreTestTools.assertTuple2DEquals(recomputedCentroid, centroid, SMALLEST_EPSILON);
      }
   }

   @Test
   public void testEdgeNormal() throws Exception
   {
      Random random = new Random(234234L);

      for (int i = 0; i < ITERATIONS; i++)
      {
         List<? extends Point2DReadOnly> convexPolygon2D = generateRandomCircleBasedConvexPolygon2D(random, 10.0, 10.0, 100);
         int hullSize = EuclidGeometryPolygonTools.inPlaceGrahamScanConvexHull2D(convexPolygon2D);
         boolean clockwiseOrdered = random.nextBoolean();
         if (!clockwiseOrdered)
            Collections.reverse(convexPolygon2D.subList(0, hullSize));

         for (int edgeIndex = 0; edgeIndex < hullSize; edgeIndex++)
         {
            Point2DReadOnly edgeStart = convexPolygon2D.get(edgeIndex);
            Point2DReadOnly edgeEnd = convexPolygon2D.get(next(edgeIndex, hullSize));

            Vector2D edgeNormal = new Vector2D();
            EuclidGeometryPolygonTools.edgeNormal(edgeIndex, convexPolygon2D, hullSize, clockwiseOrdered, edgeNormal);

            Vector2D edgeDirection = new Vector2D();
            edgeDirection.sub(edgeEnd, edgeStart);
            assertEquals(0.0, edgeDirection.dot(edgeNormal), SMALLEST_EPSILON);

            Point2D pointInsidePolygon = new Point2D();
            pointInsidePolygon.scaleAdd(-1.0e-8, edgeNormal, edgeStart);
            assertTrue("Iteration: " + i + ", edgeIndex: " + edgeIndex,
                       isPoint2DInsideConvexPolygon2D(pointInsidePolygon, convexPolygon2D, hullSize, clockwiseOrdered, 0.0));

            Point2D pointOutsidePolygon = new Point2D();
            pointOutsidePolygon.scaleAdd(1.0e8, edgeNormal, edgeStart);
            assertFalse(isPoint2DInsideConvexPolygon2D(pointOutsidePolygon, convexPolygon2D, hullSize, clockwiseOrdered, 0.0));
         }
      }
   }

   @Test
   public void testIsPoint2DInsideConvexPolygon2D() throws Exception
   {
      Random random = new Random(324534L);

      for (int i = 0; i < ITERATIONS; i++)
      { // Test with epsilon == 0.0
         List<? extends Point2DReadOnly> convexPolygon2D = generateRandomCircleBasedConvexPolygon2D(random, 10.0, 10.0, 100);
         int hullSize = inPlaceGiftWrapConvexHull2D(convexPolygon2D);
         boolean clockwiseOrdered = random.nextBoolean();
         if (!clockwiseOrdered)
            Collections.reverse(convexPolygon2D.subList(0, hullSize));

         Point2D centroid = new Point2D();
         computeConvexPolyong2DArea(convexPolygon2D, hullSize, clockwiseOrdered, centroid);
         int vertexIndex = random.nextInt(hullSize);
         int nextVertexIndex = next(vertexIndex, hullSize);
         Point2DReadOnly vertex = convexPolygon2D.get(vertexIndex);
         Point2DReadOnly nextVertex = convexPolygon2D.get(nextVertexIndex);

         Point2D pointOnEdge = new Point2D();
         pointOnEdge.interpolate(vertex, nextVertex, random.nextDouble());

         double alphaOutside = EuclidCoreRandomTools.generateRandomDouble(random, 1.0, 3.0);
         Point2D outsidePoint = new Point2D();
         outsidePoint.interpolate(centroid, pointOnEdge, alphaOutside);
         assertFalse(isPoint2DInsideConvexPolygon2D(outsidePoint, convexPolygon2D, hullSize, clockwiseOrdered, 0));

         double alphaInside = EuclidCoreRandomTools.generateRandomDouble(random, 0.0, 1.0);
         Point2D insidePoint = new Point2D();
         insidePoint.interpolate(centroid, pointOnEdge, alphaInside);
         assertTrue(isPoint2DInsideConvexPolygon2D(insidePoint, convexPolygon2D, hullSize, clockwiseOrdered, 0));
      }

      for (int i = 0; i < ITERATIONS; i++)
      { // Test with epsilon > 0.0
         double epsilon = random.nextDouble();

         List<? extends Point2DReadOnly> convexPolygon2D = generateRandomCircleBasedConvexPolygon2D(random, 10.0, 10.0, 100);
         int hullSize = inPlaceGiftWrapConvexHull2D(convexPolygon2D);
         boolean clockwiseOrdered = random.nextBoolean();
         if (!clockwiseOrdered)
            Collections.reverse(convexPolygon2D.subList(0, hullSize));

         Point2D centroid = new Point2D();
         computeConvexPolyong2DArea(convexPolygon2D, hullSize, clockwiseOrdered, centroid);
         int vertexIndex = random.nextInt(hullSize);
         int nextVertexIndex = next(vertexIndex, hullSize);
         Point2DReadOnly vertex = convexPolygon2D.get(vertexIndex);
         Point2DReadOnly nextVertex = convexPolygon2D.get(nextVertexIndex);

         Point2D pointOnEdge = new Point2D();
         pointOnEdge.interpolate(vertex, nextVertex, random.nextDouble());

         double distanceOutside = EuclidCoreRandomTools.generateRandomDouble(random, 0.0, epsilon);
         Point2D outsidePoint = new Point2D();
         Vector2D orthogonal = new Vector2D();
         orthogonal.sub(nextVertex, vertex);
         orthogonal.normalize();
         orthogonal = perpendicularVector2D(orthogonal);
         if (!clockwiseOrdered)
            orthogonal.negate();

         outsidePoint.scaleAdd(distanceOutside, orthogonal, pointOnEdge);
         assertTrue(isPoint2DInsideConvexPolygon2D(outsidePoint, convexPolygon2D, hullSize, clockwiseOrdered, epsilon));

         distanceOutside = EuclidCoreRandomTools.generateRandomDouble(random, epsilon, epsilon + 1.0);
         outsidePoint.scaleAdd(distanceOutside, orthogonal, pointOnEdge);
         assertFalse(isPoint2DInsideConvexPolygon2D(outsidePoint, convexPolygon2D, hullSize, clockwiseOrdered, epsilon));

         double alphaInside = EuclidCoreRandomTools.generateRandomDouble(random, 0.0, 1.0);
         Point2D insidePoint = new Point2D();
         insidePoint.interpolate(centroid, pointOnEdge, alphaInside);
         assertTrue(isPoint2DInsideConvexPolygon2D(insidePoint, convexPolygon2D, hullSize, clockwiseOrdered, 0));
      }

      for (int i = 0; i < ITERATIONS; i++)
      { // Test with epsilon < 0.0
         double epsilon = -0.02; // Testing with a small value to avoid weird cases that are hard to deal with.

         List<? extends Point2DReadOnly> convexPolygon2D = generateRandomCircleBasedConvexPolygon2D(random, 10.0, 10.0, 100);
         int hullSize = inPlaceGiftWrapConvexHull2D(convexPolygon2D);
         boolean clockwiseOrdered = random.nextBoolean();
         if (!clockwiseOrdered)
            Collections.reverse(convexPolygon2D.subList(0, hullSize));

         Point2D centroid = new Point2D();
         computeConvexPolyong2DArea(convexPolygon2D, hullSize, clockwiseOrdered, centroid);
         int vertexIndex = random.nextInt(hullSize);
         int nextVertexIndex = next(vertexIndex, hullSize);
         Point2DReadOnly vertex = convexPolygon2D.get(vertexIndex);
         Point2DReadOnly nextVertex = convexPolygon2D.get(nextVertexIndex);

         Point2D pointOnEdge = new Point2D();
         pointOnEdge.interpolate(vertex, nextVertex, random.nextDouble());

         double alphaOutside = EuclidCoreRandomTools.generateRandomDouble(random, 1.0, 3.0);
         Point2D outsidePoint = new Point2D();
         outsidePoint.interpolate(centroid, pointOnEdge, alphaOutside);
         assertFalse(isPoint2DInsideConvexPolygon2D(outsidePoint, convexPolygon2D, hullSize, clockwiseOrdered, 0));

         double distanceInside = EuclidCoreRandomTools.generateRandomDouble(random, epsilon, 0.0);
         Vector2D orthogonal = new Vector2D();
         orthogonal.sub(nextVertex, vertex);
         orthogonal.normalize();
         orthogonal = perpendicularVector2D(orthogonal);
         if (!clockwiseOrdered)
            orthogonal.negate();

         outsidePoint.scaleAdd(distanceInside, orthogonal, pointOnEdge);
         assertFalse(isPoint2DInsideConvexPolygon2D(outsidePoint, convexPolygon2D, hullSize, clockwiseOrdered, epsilon));

         // Using the distance to the centroid as a max
         double distanceBetweenCentroidAndEdge = distanceFromPoint2DToLine2D(centroid, vertex, nextVertex);
         distanceInside = EuclidCoreRandomTools.generateRandomDouble(random, -distanceBetweenCentroidAndEdge, epsilon);
         Point2D insidePoint = new Point2D();
         insidePoint.scaleAdd(distanceInside, orthogonal, pointOnEdge);
         assertTrue(isPoint2DInsideConvexPolygon2D(insidePoint, convexPolygon2D, hullSize, clockwiseOrdered, 0));
      }
   }

   @Test
   public void testSignedDistanceFromPoint2DToConvexPolygon2D() throws Exception
   {
      {// Trivial case: Square
         int n = 4;
         List<Point2DReadOnly> clockwiseSquareVertices = new ArrayList<>();
         clockwiseSquareVertices.add(new Point2D(0.0, 1.0));
         clockwiseSquareVertices.add(new Point2D(1.0, 1.0));
         clockwiseSquareVertices.add(new Point2D(1.0, 0.0));
         clockwiseSquareVertices.add(new Point2D(0.0, 0.0));
         List<Point2DReadOnly> counterClockwiseSquareVertices = new ArrayList<>(clockwiseSquareVertices);
         Collections.reverse(counterClockwiseSquareVertices);
         double x, y;
         double expectedDistance, actualDistance;

         for (x = 0.0; x <= 1.0; x += 0.05)
         {
            y = 1.1;
            expectedDistance = 0.1;
            actualDistance = signedDistanceFromPoint2DToConvexPolygon2D(x, y, clockwiseSquareVertices, n, true);
            assertEquals(expectedDistance, actualDistance, SMALLEST_EPSILON);
            actualDistance = signedDistanceFromPoint2DToConvexPolygon2D(x, y, counterClockwiseSquareVertices, n, false);
            assertEquals(expectedDistance, actualDistance, SMALLEST_EPSILON);
         }

         for (x = 0.0; x <= 1.0; x += 0.05)
         {
            y = -0.1;
            expectedDistance = 0.1;
            actualDistance = signedDistanceFromPoint2DToConvexPolygon2D(x, y, clockwiseSquareVertices, n, true);
            assertEquals(expectedDistance, actualDistance, SMALLEST_EPSILON);
            actualDistance = signedDistanceFromPoint2DToConvexPolygon2D(x, y, counterClockwiseSquareVertices, n, false);
            assertEquals(expectedDistance, actualDistance, SMALLEST_EPSILON);
         }

         for (y = 0.0; y <= 1.0; y += 0.05)
         {
            x = 1.1;
            expectedDistance = 0.1;
            actualDistance = signedDistanceFromPoint2DToConvexPolygon2D(x, y, clockwiseSquareVertices, n, true);
            assertEquals(expectedDistance, actualDistance, SMALLEST_EPSILON);
            actualDistance = signedDistanceFromPoint2DToConvexPolygon2D(x, y, counterClockwiseSquareVertices, n, false);
            assertEquals(expectedDistance, actualDistance, SMALLEST_EPSILON);
         }

         for (y = 0.0; y <= 1.0; y += 0.05)
         {
            x = -0.1;
            expectedDistance = 0.1;
            actualDistance = signedDistanceFromPoint2DToConvexPolygon2D(x, y, clockwiseSquareVertices, n, true);
            assertEquals(expectedDistance, actualDistance, SMALLEST_EPSILON);
            actualDistance = signedDistanceFromPoint2DToConvexPolygon2D(x, y, counterClockwiseSquareVertices, n, false);
            assertEquals(expectedDistance, actualDistance, SMALLEST_EPSILON);
         }

         for (x = 0.1; x <= 0.9; x += 0.05)
         {
            y = 0.9;
            expectedDistance = -0.1;
            actualDistance = signedDistanceFromPoint2DToConvexPolygon2D(x, y, clockwiseSquareVertices, n, true);
            assertEquals(expectedDistance, actualDistance, SMALLEST_EPSILON);
            actualDistance = signedDistanceFromPoint2DToConvexPolygon2D(x, y, counterClockwiseSquareVertices, n, false);
            assertEquals(expectedDistance, actualDistance, SMALLEST_EPSILON);
         }

         for (x = 0.1; x <= 0.9; x += 0.05)
         {
            y = 0.1;
            expectedDistance = -0.1;
            actualDistance = signedDistanceFromPoint2DToConvexPolygon2D(x, y, clockwiseSquareVertices, n, true);
            assertEquals(expectedDistance, actualDistance, SMALLEST_EPSILON);
            actualDistance = signedDistanceFromPoint2DToConvexPolygon2D(x, y, counterClockwiseSquareVertices, n, false);
            assertEquals(expectedDistance, actualDistance, SMALLEST_EPSILON);
         }
      }

      // Non-trivial cases
      Random random = new Random(324234L);
      double expectedDistance, actualDistance;

      for (int i = 0; i < ITERATIONS; i++)
      {
         List<? extends Point2DReadOnly> convexPolygon2D = generateRandomCircleBasedConvexPolygon2D(random, 10.0, 10.0, 100);
         int hullSize = inPlaceGiftWrapConvexHull2D(convexPolygon2D);
         boolean clockwiseOrdered = random.nextBoolean();
         if (!clockwiseOrdered)
            Collections.reverse(convexPolygon2D.subList(0, hullSize));

         Point2D centroid = new Point2D();
         computeConvexPolyong2DArea(convexPolygon2D, hullSize, clockwiseOrdered, centroid);
         int vertexIndex = random.nextInt(hullSize);
         int nextVertexIndex = next(vertexIndex, hullSize);
         Point2DReadOnly vertex = convexPolygon2D.get(vertexIndex);
         Point2DReadOnly nextVertex = convexPolygon2D.get(nextVertexIndex);

         Point2D pointOnEdge = new Point2D();
         pointOnEdge.interpolate(vertex, nextVertex, random.nextDouble());

         double alphaOutside = EuclidCoreRandomTools.generateRandomDouble(random, 1.0, 3.0);
         Point2D outsidePoint = new Point2D();
         outsidePoint.interpolate(centroid, pointOnEdge, alphaOutside);
         expectedDistance = distanceFromPoint2DToLineSegment2D(outsidePoint, vertex, nextVertex);
         actualDistance = signedDistanceFromPoint2DToConvexPolygon2D(outsidePoint, convexPolygon2D, hullSize, clockwiseOrdered);
         assertEquals("EdgeLength = " + vertex.distance(nextVertex), expectedDistance, actualDistance, SMALLEST_EPSILON);

         double alphaInside = EuclidCoreRandomTools.generateRandomDouble(random, 0.0, 1.0);
         Point2D insidePoint = new Point2D();
         insidePoint.interpolate(centroid, pointOnEdge, alphaInside);

         expectedDistance = Double.POSITIVE_INFINITY;

         for (int j = 0; j < hullSize; j++)
         {
            Point2DReadOnly edgeStart = convexPolygon2D.get(j);
            Point2DReadOnly edgeEnd = convexPolygon2D.get(next(j, hullSize));
            expectedDistance = Math.min(expectedDistance, distanceFromPoint2DToLineSegment2D(insidePoint, edgeStart, edgeEnd));
         }

         expectedDistance = -expectedDistance;
         actualDistance = signedDistanceFromPoint2DToConvexPolygon2D(insidePoint, convexPolygon2D, hullSize, clockwiseOrdered);
         assertEquals("EdgeLength = " + vertex.distance(nextVertex), expectedDistance, actualDistance, SMALLEST_EPSILON);
      }
   }

   @Test
   public void testIntersectionBetweenLine2DAndConvexPolygon2D() throws Exception
   {
      Random random = new Random(234324L);

      for (int i = 0; i < ITERATIONS; i++)
      { // Setup: 2 intersections picked at random on the polygon from which the line can be built.
         List<? extends Point2DReadOnly> convexPolygon2D = generateRandomPointCloud2D(random, 10.0, 10.0, 100);
         int hullSize = inPlaceGrahamScanConvexHull2D(convexPolygon2D);
         boolean clockwiseOrdered = random.nextBoolean();
         if (!clockwiseOrdered)
            Collections.reverse(convexPolygon2D.subList(0, hullSize));

         int firstEdgeIndex = random.nextInt(hullSize);
         int secondEdgeIndex = wrap(random.nextInt(hullSize - 1) + firstEdgeIndex + 1, hullSize); // Making sure the two edges are different

         Point2D expectedFirstIntersection = new Point2D();
         {
            Point2DReadOnly vertex = convexPolygon2D.get(firstEdgeIndex);
            Point2DReadOnly nextVertex = convexPolygon2D.get(next(firstEdgeIndex, hullSize));
            expectedFirstIntersection.interpolate(vertex, nextVertex, random.nextDouble());
         }
         Point2D expectedSecondIntersection = new Point2D();
         {
            Point2DReadOnly vertex = convexPolygon2D.get(secondEdgeIndex);
            Point2DReadOnly nextVertex = convexPolygon2D.get(next(secondEdgeIndex, hullSize));
            expectedSecondIntersection.interpolate(vertex, nextVertex, random.nextDouble());
         }

         Point2D pointOnLine = new Point2D();
         pointOnLine.interpolate(expectedFirstIntersection, expectedSecondIntersection, EuclidCoreRandomTools.generateRandomDouble(random, 10.0));
         Vector2D lineDirection = new Vector2D();
         lineDirection.sub(expectedSecondIntersection, expectedFirstIntersection);
         lineDirection.normalize();
         lineDirection.scale(EuclidCoreRandomTools.generateRandomDouble(random, 10.0));

         Point2D actualFirstIntersection = new Point2D();
         Point2D actualSecondIntersection = new Point2D();
         int numberOfIntersections = intersectionBetweenLine2DAndConvexPolygon2D(pointOnLine, lineDirection, convexPolygon2D, hullSize, clockwiseOrdered,
                                                                                 actualFirstIntersection, actualSecondIntersection);
         assertEquals(2, numberOfIntersections);

         if (expectedFirstIntersection.distance(actualFirstIntersection) < expectedFirstIntersection.distance(actualSecondIntersection))
         {
            EuclidCoreTestTools.assertTuple2DEquals(expectedFirstIntersection, actualFirstIntersection, SMALL_EPSILON);
            EuclidCoreTestTools.assertTuple2DEquals(expectedSecondIntersection, actualSecondIntersection, SMALL_EPSILON);
         }
         else
         {
            EuclidCoreTestTools.assertTuple2DEquals(expectedFirstIntersection, actualSecondIntersection, SMALL_EPSILON);
            EuclidCoreTestTools.assertTuple2DEquals(expectedSecondIntersection, actualFirstIntersection, SMALL_EPSILON);
         }
      }

      for (int i = 0; i < ITERATIONS; i++)
      { // Setup: the line is collinear to an edge picked at random
         List<? extends Point2DReadOnly> convexPolygon2D = generateRandomPointCloud2D(random, 10.0, 10.0, 100);
         int hullSize = inPlaceGrahamScanConvexHull2D(convexPolygon2D);
         boolean clockwiseOrdered = random.nextBoolean();
         if (!clockwiseOrdered)
            Collections.reverse(convexPolygon2D.subList(0, hullSize));

         int edgeIndex = random.nextInt(hullSize);
         Point2DReadOnly vertex = convexPolygon2D.get(edgeIndex);
         Point2DReadOnly nextVertex = convexPolygon2D.get(next(edgeIndex, hullSize));

         Point2D pointOnLine = new Point2D();
         pointOnLine.interpolate(vertex, nextVertex, EuclidCoreRandomTools.generateRandomDouble(random, 10.0));
         Vector2D lineDirection = new Vector2D();
         lineDirection.sub(nextVertex, vertex);
         lineDirection.normalize();
         lineDirection.scale(EuclidCoreRandomTools.generateRandomDouble(random, 10.0));

         Point2D actualFirstIntersection = new Point2D();
         Point2D actualSecondIntersection = new Point2D();
         int numberOfIntersections = intersectionBetweenLine2DAndConvexPolygon2D(pointOnLine, lineDirection, convexPolygon2D, hullSize, clockwiseOrdered,
                                                                                 actualFirstIntersection, actualSecondIntersection);
         assertEquals("Iteration: " + i, 2, numberOfIntersections);

         if (vertex.distance(actualFirstIntersection) < vertex.distance(actualSecondIntersection))
         {
            EuclidCoreTestTools.assertTuple2DEquals(vertex, actualFirstIntersection, SMALL_EPSILON);
            EuclidCoreTestTools.assertTuple2DEquals(nextVertex, actualSecondIntersection, SMALL_EPSILON);
         }
         else
         {
            EuclidCoreTestTools.assertTuple2DEquals(vertex, actualSecondIntersection, SMALL_EPSILON);
            EuclidCoreTestTools.assertTuple2DEquals(nextVertex, actualFirstIntersection, SMALL_EPSILON);
         }
      }

      for (int i = 0; i < ITERATIONS; i++)
      { // (somewhat tricky) Setup: build a line that does NOT intersect with the polygon
         /* 
          * @formatter:off
          * - The goal is to build the query line such that it does not intersect with the polygon.
          * - Pick two successive vertices: v0 = convexPolygon2D.get(i) and vn1 = convexPolygon2D.get(i+1).
          * - Draw 2 lines going from the centroid through each vertex, they are called extrapolation lines.
          * - For the line going through v0, find the intersection v0Max with the line going through vn1 and vn2 = convexPolygon2D.get(i+2).
          *    The first point defining the query line should be between v0 and v0Max such that the line won't intersect with the edge (vn1, vn2).
          * - For the line going through vn1, find the intersection vn1Max with the line going through v0 and vp1 = convexPolygon2D.get(i-1).
          *    The second point defining the query line should be between vn1 and vn1Max such that the line won't intersect with the edge (v0, vp1).
          * @formatter:on
          */
         List<? extends Point2DReadOnly> convexPolygon2D = generateRandomPointCloud2D(random, 10.0, 10.0, 100);
         int hullSize = inPlaceGrahamScanConvexHull2D(convexPolygon2D);
         boolean clockwiseOrdered = random.nextBoolean();
         if (!clockwiseOrdered)
            Collections.reverse(convexPolygon2D.subList(0, hullSize));

         int v0Index = random.nextInt(hullSize);
         int vn1Index = next(v0Index, hullSize);
         int vn2Index = next(vn1Index, hullSize);
         int vp1Index = previous(v0Index, hullSize);

         Point2DReadOnly v0 = convexPolygon2D.get(v0Index);
         Point2DReadOnly vn1 = convexPolygon2D.get(vn1Index);
         Point2DReadOnly vn2 = convexPolygon2D.get(vn2Index);
         Point2DReadOnly vp1 = convexPolygon2D.get(vp1Index);

         Point2D centroid = new Point2D();
         computeConvexPolyong2DArea(convexPolygon2D, hullSize, clockwiseOrdered, centroid);

         Point2D v0Max = intersectionBetweenTwoLine2Ds(centroid, v0, vn1, vn2);
         Vector2D extrapolationDirection = new Vector2D();
         extrapolationDirection.sub(v0, centroid);

         if (!isPoint2DInFrontOfRay2D(v0Max, centroid, extrapolationDirection))
            v0Max.scaleAdd(10.0, extrapolationDirection, v0);

         Point2D vn1Max = intersectionBetweenTwoLine2Ds(centroid, vn1, v0, vp1);
         extrapolationDirection.sub(vn1, centroid);

         if (!isPoint2DInFrontOfRay2D(vn1Max, centroid, extrapolationDirection))
            vn1Max.scaleAdd(10.0, extrapolationDirection, vn1);

         Point2D firstExtrapolatedPoint = new Point2D();
         Point2D secondExtrapolatedPoint = new Point2D();

         firstExtrapolatedPoint.interpolate(v0, v0Max, generateRandomDouble(random, 0.0, 1.0));
         secondExtrapolatedPoint.interpolate(vn1, vn1Max, generateRandomDouble(random, 0.0, 1.0));

         Point2D pointOnLine = new Point2D();
         pointOnLine.interpolate(firstExtrapolatedPoint, secondExtrapolatedPoint, generateRandomDouble(random, 10.0));
         Vector2D lineDirection = new Vector2D();
         lineDirection.sub(secondExtrapolatedPoint, firstExtrapolatedPoint);
         lineDirection.normalize();
         lineDirection.scale(generateRandomDouble(random, 10.0));

         Point2D actualFirstIntersection = new Point2D();
         Point2D actualSecondIntersection = new Point2D();
         int numberOfIntersections = intersectionBetweenLine2DAndConvexPolygon2D(pointOnLine, lineDirection, convexPolygon2D, hullSize, clockwiseOrdered,
                                                                                 actualFirstIntersection, actualSecondIntersection);
         assertEquals("Iteration: " + i, 0, numberOfIntersections);
      }

      for (int i = 0; i < ITERATIONS; i++)
      { // Setup: intersection at a vertex
         List<? extends Point2DReadOnly> convexPolygon2D = generateRandomPointCloud2D(random, 10.0, 10.0, 100);
         int hullSize = inPlaceGrahamScanConvexHull2D(convexPolygon2D);
         boolean clockwiseOrdered = random.nextBoolean();
         if (!clockwiseOrdered)
            Collections.reverse(convexPolygon2D.subList(0, hullSize));

         int vertexIndex = random.nextInt(hullSize);
         Point2DReadOnly vertex = convexPolygon2D.get(vertexIndex);
         Point2DReadOnly nextVertex = convexPolygon2D.get(next(vertexIndex, hullSize));
         Point2DReadOnly previousVertex = convexPolygon2D.get(previous(vertexIndex, hullSize));

         Vector2D previousEdgeDirection = new Vector2D();
         previousEdgeDirection.sub(vertex, previousVertex);
         Vector2D nextEdgeDirection = new Vector2D();
         nextEdgeDirection.sub(nextVertex, vertex);

         // The line direction has to be between the direction of the previous and next edge.
         Vector2D lineDirection = new Vector2D();
         lineDirection.interpolate(previousEdgeDirection, nextEdgeDirection, generateRandomDouble(random, 0.0, 1.0));
         lineDirection.normalize();
         Point2D pointOnLine = new Point2D();
         pointOnLine.scaleAdd(generateRandomDouble(random, 10.0), lineDirection, vertex);
         lineDirection.scale(generateRandomDouble(random, 10.0));

         Point2D actualFirstIntersection = new Point2D();
         Point2D actualSecondIntersection = new Point2D();
         int numberOfIntersections = intersectionBetweenLine2DAndConvexPolygon2D(pointOnLine, lineDirection, convexPolygon2D, hullSize, clockwiseOrdered,
                                                                                 actualFirstIntersection, actualSecondIntersection);
         assertEquals("Iteration: " + i, 1, numberOfIntersections);
         EuclidCoreTestTools.assertTuple2DEquals(vertex, actualFirstIntersection, SMALL_EPSILON);
         EuclidCoreTestTools.assertTuple2DEquals(vertex, actualSecondIntersection, SMALL_EPSILON);
      }
   }

   @Test
   public void testIntersectionBetweenLineSegment2DAndConvexPolygon2D() throws Exception
   {
      Random random = new Random(234324L);

      for (int i = 0; i < ITERATIONS; i++)
      { // Setup: 2 intersections picked at random on the polygon from which the line segment can be built to test 0, 1, and 2 intersections just by moving the end points on the same line.
         List<? extends Point2DReadOnly> convexPolygon2D = generateRandomPointCloud2D(random, 10.0, 10.0, 100);
         int hullSize = inPlaceGrahamScanConvexHull2D(convexPolygon2D);
         boolean clockwiseOrdered = random.nextBoolean();
         if (!clockwiseOrdered)
            Collections.reverse(convexPolygon2D.subList(0, hullSize));

         int firstEdgeIndex = random.nextInt(hullSize);
         int secondEdgeIndex = wrap(random.nextInt(hullSize - 1) + firstEdgeIndex + 1, hullSize); // Making sure the two edges are different

         double alphaFirst = random.nextDouble();
         Point2D expectedFirstIntersection = new Point2D();
         {
            Point2DReadOnly vertex = convexPolygon2D.get(firstEdgeIndex);
            Point2DReadOnly nextVertex = convexPolygon2D.get(next(firstEdgeIndex, hullSize));
            expectedFirstIntersection.interpolate(vertex, nextVertex, alphaFirst);
         }
         double alphaSecond = random.nextDouble();
         Point2D expectedSecondIntersection = new Point2D();
         {
            Point2DReadOnly vertex = convexPolygon2D.get(secondEdgeIndex);
            Point2DReadOnly nextVertex = convexPolygon2D.get(next(secondEdgeIndex, hullSize));
            expectedSecondIntersection.interpolate(vertex, nextVertex, alphaSecond);
         }

         Vector2D lineDirection = new Vector2D();
         lineDirection.sub(expectedSecondIntersection, expectedFirstIntersection);
         lineDirection.normalize();

         Point2D actualFirstIntersection = new Point2D();
         Point2D actualSecondIntersection = new Point2D();
         Point2D lineSegmentStart = new Point2D();
         Point2D lineSegmentEnd = new Point2D();

         // Make the line-segment endpoints such that we have 2 intersections
         double alphaOutsideStart = generateRandomDouble(random, -10.0, -0.01);
         double alphaOutsideEnd = generateRandomDouble(random, 1.01, 10.0);
         lineSegmentStart.interpolate(expectedFirstIntersection, expectedSecondIntersection, alphaOutsideStart);
         lineSegmentEnd.interpolate(expectedFirstIntersection, expectedSecondIntersection, alphaOutsideEnd);

         int numberOfIntersections = intersectionBetweenLineSegment2DAndConvexPolygon2D(lineSegmentStart, lineSegmentEnd, convexPolygon2D, hullSize,
                                                                                        clockwiseOrdered, actualFirstIntersection, actualSecondIntersection);
         assertEquals(2, numberOfIntersections);

         if (expectedFirstIntersection.distance(actualFirstIntersection) < expectedFirstIntersection.distance(actualSecondIntersection))
         {
            EuclidCoreTestTools.assertTuple2DEquals(expectedFirstIntersection, actualFirstIntersection, SMALL_EPSILON);
            EuclidCoreTestTools.assertTuple2DEquals(expectedSecondIntersection, actualSecondIntersection, SMALL_EPSILON);
         }
         else
         {
            EuclidCoreTestTools.assertTuple2DEquals(expectedFirstIntersection, actualSecondIntersection, SMALL_EPSILON);
            EuclidCoreTestTools.assertTuple2DEquals(expectedSecondIntersection, actualFirstIntersection, SMALL_EPSILON);
         }

         // Make the line-segment endpoints such that we have 0 intersection
         lineSegmentStart.interpolate(expectedFirstIntersection, expectedSecondIntersection, generateRandomDouble(random, 0.0, 1.0));
         lineSegmentEnd.interpolate(expectedFirstIntersection, expectedSecondIntersection, generateRandomDouble(random, 0.0, 1.0));

         numberOfIntersections = intersectionBetweenLineSegment2DAndConvexPolygon2D(lineSegmentStart, lineSegmentEnd, convexPolygon2D, hullSize,
                                                                                    clockwiseOrdered, actualFirstIntersection, actualSecondIntersection);
         assertEquals(0, numberOfIntersections);

         // Make the line-segment endpoints such that we have 1 intersection (two ways to test)
         lineSegmentStart.interpolate(expectedFirstIntersection, expectedSecondIntersection, alphaOutsideStart);
         lineSegmentEnd.interpolate(expectedFirstIntersection, expectedSecondIntersection, generateRandomDouble(random, 0.0, 1.0));

         numberOfIntersections = intersectionBetweenLineSegment2DAndConvexPolygon2D(lineSegmentStart, lineSegmentEnd, convexPolygon2D, hullSize,
                                                                                    clockwiseOrdered, actualFirstIntersection, actualSecondIntersection);
         assertEquals(1, numberOfIntersections);

         if (actualFirstIntersection.distance(expectedFirstIntersection) < actualFirstIntersection.distance(expectedSecondIntersection))
            EuclidCoreTestTools.assertTuple2DEquals(expectedFirstIntersection, actualFirstIntersection, SMALL_EPSILON);
         else
            EuclidCoreTestTools.assertTuple2DEquals(expectedSecondIntersection, actualFirstIntersection, SMALL_EPSILON);

         lineSegmentStart.interpolate(expectedFirstIntersection, expectedSecondIntersection, generateRandomDouble(random, 0.0, 1.0));
         lineSegmentEnd.interpolate(expectedFirstIntersection, expectedSecondIntersection, alphaOutsideEnd);

         numberOfIntersections = intersectionBetweenLineSegment2DAndConvexPolygon2D(lineSegmentStart, lineSegmentEnd, convexPolygon2D, hullSize,
                                                                                    clockwiseOrdered, actualFirstIntersection, actualSecondIntersection);
         assertEquals(1, numberOfIntersections);

         if (actualFirstIntersection.distance(expectedFirstIntersection) < actualFirstIntersection.distance(expectedSecondIntersection))
            EuclidCoreTestTools.assertTuple2DEquals(expectedFirstIntersection, actualFirstIntersection, SMALL_EPSILON);
         else
            EuclidCoreTestTools.assertTuple2DEquals(expectedSecondIntersection, actualFirstIntersection, SMALL_EPSILON);
      }

      for (int i = 0; i < ITERATIONS; i++)
      { // Setup: the line is collinear to an edge picked at random
         List<? extends Point2DReadOnly> convexPolygon2D = generateRandomPointCloud2D(random, 10.0, 10.0, 100);
         int hullSize = inPlaceGrahamScanConvexHull2D(convexPolygon2D);
         boolean clockwiseOrdered = random.nextBoolean();
         if (!clockwiseOrdered)
            Collections.reverse(convexPolygon2D.subList(0, hullSize));

         int edgeIndex = random.nextInt(hullSize);
         Point2DReadOnly vertex = convexPolygon2D.get(edgeIndex);
         Point2DReadOnly nextVertex = convexPolygon2D.get(next(edgeIndex, hullSize));

         Point2D pointOnLine = new Point2D();
         pointOnLine.interpolate(vertex, nextVertex, EuclidCoreRandomTools.generateRandomDouble(random, 10.0));
         Vector2D lineDirection = new Vector2D();
         lineDirection.sub(nextVertex, vertex);
         lineDirection.normalize();
         lineDirection.scale(EuclidCoreRandomTools.generateRandomDouble(random, 10.0));

         Point2D actualFirstIntersection = new Point2D();
         Point2D actualSecondIntersection = new Point2D();

         Point2D lineSegmentStart = new Point2D();
         Point2D lineSegmentEnd = new Point2D();

         // Make the line-segment completely overlap the edge
         double alphaStart = generateRandomDouble(random, -10.0, -0.01);
         double alphaEnd = generateRandomDouble(random, 1.01, 10.0);
         lineSegmentStart.interpolate(vertex, nextVertex, alphaStart);
         lineSegmentEnd.interpolate(vertex, nextVertex, alphaEnd);

         int numberOfIntersections = intersectionBetweenLineSegment2DAndConvexPolygon2D(lineSegmentStart, lineSegmentEnd, convexPolygon2D, hullSize,
                                                                                        clockwiseOrdered, actualFirstIntersection, actualSecondIntersection);
         assertEquals("Iteration: " + i, 2, numberOfIntersections);

         if (vertex.distance(actualFirstIntersection) < vertex.distance(actualSecondIntersection))
         {
            EuclidCoreTestTools.assertTuple2DEquals(vertex, actualFirstIntersection, SMALL_EPSILON);
            EuclidCoreTestTools.assertTuple2DEquals(nextVertex, actualSecondIntersection, SMALL_EPSILON);
         }
         else
         {
            EuclidCoreTestTools.assertTuple2DEquals(vertex, actualSecondIntersection, SMALL_EPSILON);
            EuclidCoreTestTools.assertTuple2DEquals(nextVertex, actualFirstIntersection, SMALL_EPSILON);
         }

         // Make the line-segment not overlap the edge (two sides to test)
         lineSegmentStart.interpolate(vertex, nextVertex, generateRandomDouble(random, -10.0, 0.0));
         lineSegmentEnd.interpolate(vertex, nextVertex, generateRandomDouble(random, -10.0, 0.0));
         numberOfIntersections = intersectionBetweenLineSegment2DAndConvexPolygon2D(lineSegmentStart, lineSegmentEnd, convexPolygon2D, hullSize,
                                                                                    clockwiseOrdered, actualFirstIntersection, actualSecondIntersection);
         assertEquals("Iteration: " + i, 0, numberOfIntersections);

         lineSegmentStart.interpolate(vertex, nextVertex, generateRandomDouble(random, 1.0, 10.0));
         lineSegmentEnd.interpolate(vertex, nextVertex, generateRandomDouble(random, 1.0, 10.0));
         numberOfIntersections = intersectionBetweenLineSegment2DAndConvexPolygon2D(lineSegmentStart, lineSegmentEnd, convexPolygon2D, hullSize,
                                                                                    clockwiseOrdered, actualFirstIntersection, actualSecondIntersection);
         assertEquals("Iteration: " + i, 0, numberOfIntersections);

         // Make the line-segment partially overlap (two sides to test)
         lineSegmentStart.interpolate(vertex, nextVertex, generateRandomDouble(random, -10.0, 0.0));
         lineSegmentEnd.interpolate(vertex, nextVertex, generateRandomDouble(random, 0.0, 1.0));

         numberOfIntersections = intersectionBetweenLineSegment2DAndConvexPolygon2D(lineSegmentStart, lineSegmentEnd, convexPolygon2D, hullSize,
                                                                                    clockwiseOrdered, actualFirstIntersection, actualSecondIntersection);
         assertEquals("Iteration: " + i, 1, numberOfIntersections);

         if (actualFirstIntersection.distance(vertex) < actualFirstIntersection.distance(nextVertex))
            EuclidCoreTestTools.assertTuple2DEquals(vertex, actualFirstIntersection, SMALL_EPSILON);
         else
            EuclidCoreTestTools.assertTuple2DEquals(nextVertex, actualFirstIntersection, SMALL_EPSILON);

         // Test second side
         lineSegmentStart.interpolate(vertex, nextVertex, generateRandomDouble(random, 0.0, 1.0));
         lineSegmentEnd.interpolate(vertex, nextVertex, generateRandomDouble(random, 1.0, 10.0));

         numberOfIntersections = intersectionBetweenLineSegment2DAndConvexPolygon2D(lineSegmentStart, lineSegmentEnd, convexPolygon2D, hullSize,
                                                                                    clockwiseOrdered, actualFirstIntersection, actualSecondIntersection);
         assertEquals("Iteration: " + i, 1, numberOfIntersections);

         if (actualFirstIntersection.distance(vertex) < actualFirstIntersection.distance(nextVertex))
            EuclidCoreTestTools.assertTuple2DEquals(vertex, actualFirstIntersection, SMALL_EPSILON);
         else
            EuclidCoreTestTools.assertTuple2DEquals(nextVertex, actualFirstIntersection, SMALL_EPSILON);
      }
   }

   @Test
   public void testIntersectionBetweenRay2DAndConvexPolygon2D() throws Exception
   {
      Random random = new Random(43545L);

      for (int i = 0; i < ITERATIONS; i++)
      { // Test using intersectionBetweenLine2DAndConvexPolygon2D
         List<? extends Point2DReadOnly> convexPolygon2D = generateRandomPointCloud2D(random, 10.0, 10.0, 100);
         int hullSize = inPlaceGrahamScanConvexHull2D(convexPolygon2D);
         boolean clockwiseOrdered = random.nextBoolean();
         if (!clockwiseOrdered)
            Collections.reverse(convexPolygon2D.subList(0, hullSize));

         Point2D rayOrigin = generateRandomPoint2D(random, 10.0);
         Vector2D rayDirection = generateRandomVector2D(random, -10.0, 10.0);
         Point2D firstIntersectionWithLine = new Point2D();
         Point2D secondIntersectionWithLine = new Point2D();
         int expectedNumberOfIntersections = intersectionBetweenLine2DAndConvexPolygon2D(rayOrigin, rayDirection, convexPolygon2D, hullSize, clockwiseOrdered,
                                                                                         firstIntersectionWithLine, secondIntersectionWithLine);

         List<Point2D> expectedIntersections = new ArrayList<>();

         if (expectedNumberOfIntersections == 2 && isPoint2DInFrontOfRay2D(secondIntersectionWithLine, rayOrigin, rayDirection))
            expectedIntersections.add(secondIntersectionWithLine);
         if (expectedNumberOfIntersections >= 1 && isPoint2DInFrontOfRay2D(firstIntersectionWithLine, rayOrigin, rayDirection))
            expectedIntersections.add(firstIntersectionWithLine);
         expectedNumberOfIntersections = expectedIntersections.size();

         Point2D firstIntersectionWithRay = new Point2D();
         Point2D secondIntersectionWithRay = new Point2D();

         int actualNumberOfIntersections = intersectionBetweenRay2DAndConvexPolygon2D(rayOrigin, rayDirection, convexPolygon2D, hullSize, clockwiseOrdered,
                                                                                      firstIntersectionWithRay, secondIntersectionWithRay);
         assertEquals("Iteration: " + i, expectedNumberOfIntersections, actualNumberOfIntersections);

         if (expectedNumberOfIntersections == 2)
         {
            if (firstIntersectionWithLine.distance(expectedIntersections.get(0)) < firstIntersectionWithLine.distance(expectedIntersections.get(1)))
            {
               EuclidCoreTestTools.assertTuple2DEquals(firstIntersectionWithRay, expectedIntersections.get(0), EPSILON);
               EuclidCoreTestTools.assertTuple2DEquals(secondIntersectionWithRay, expectedIntersections.get(1), EPSILON);
            }
            else
            {
               EuclidCoreTestTools.assertTuple2DEquals(firstIntersectionWithRay, expectedIntersections.get(1), EPSILON);
               EuclidCoreTestTools.assertTuple2DEquals(secondIntersectionWithRay, expectedIntersections.get(0), EPSILON);
            }
         }

         if (expectedNumberOfIntersections == 1)
            EuclidCoreTestTools.assertTuple2DEquals(expectedIntersections.get(0), firstIntersectionWithRay, EPSILON);
      }
   }

   @Test
   public void testOrthogonalProjectionOnConvexPolygon2D() throws Exception
   {
      Random random = new Random(43545L);

      for (int i = 0; i < ITERATIONS; i++)
      { // Setup: Create point on an edge picked at random, shift it orthogonally toward the outside of the polygon
         List<? extends Point2DReadOnly> convexPolygon2D = generateRandomPointCloud2D(random, 10.0, 10.0, 100);
         int hullSize = inPlaceGrahamScanConvexHull2D(convexPolygon2D);
         boolean clockwiseOrdered = random.nextBoolean();
         if (!clockwiseOrdered)
            Collections.reverse(convexPolygon2D.subList(0, hullSize));

         int edgeIndex = random.nextInt(hullSize);
         Point2DReadOnly edgeStart = convexPolygon2D.get(edgeIndex);
         Point2DReadOnly edgeEnd = convexPolygon2D.get(next(edgeIndex, hullSize));

         Vector2D edgeNormal = new Vector2D();
         edgeNormal(edgeIndex, convexPolygon2D, hullSize, clockwiseOrdered, edgeNormal);

         Point2D expectedProjection = new Point2D();
         expectedProjection.interpolate(edgeStart, edgeEnd, random.nextDouble());

         Point2D pointToProject = new Point2D();
         pointToProject.scaleAdd(random.nextDouble(), edgeNormal, expectedProjection);

         Point2D actualProjection = new Point2D();
         boolean success = orthogonalProjectionOnConvexPolygon2D(pointToProject, convexPolygon2D, hullSize, clockwiseOrdered, actualProjection);
         assertTrue(success);
         EuclidCoreTestTools.assertTuple2DEquals(expectedProjection, actualProjection, EPSILON);
      }

      for (int i = 0; i < ITERATIONS; i++)
      { // Setup: pick a vertex at random an shift it outside the polygon such that the projection of the resulting point is the vertex.
         List<? extends Point2DReadOnly> convexPolygon2D = generateRandomPointCloud2D(random, 10.0, 10.0, 100);
         int hullSize = inPlaceGrahamScanConvexHull2D(convexPolygon2D);
         boolean clockwiseOrdered = random.nextBoolean();
         if (!clockwiseOrdered)
            Collections.reverse(convexPolygon2D.subList(0, hullSize));

         int vertexIndex = random.nextInt(hullSize);
         Point2DReadOnly vertex = convexPolygon2D.get(vertexIndex);

         Vector2D previousEdgeNormal = new Vector2D();
         Vector2D nextEdgeNormal = new Vector2D();
         Vector2D shiftDirection = new Vector2D();
         edgeNormal(previous(vertexIndex, hullSize), convexPolygon2D, hullSize, clockwiseOrdered, previousEdgeNormal);
         edgeNormal(vertexIndex, convexPolygon2D, hullSize, clockwiseOrdered, nextEdgeNormal);
         shiftDirection.interpolate(previousEdgeNormal, nextEdgeNormal, random.nextDouble());

         Point2D pointToProject = new Point2D();
         pointToProject.scaleAdd(random.nextDouble(), shiftDirection, vertex);

         Point2D acualProjection = new Point2D();
         boolean success = orthogonalProjectionOnConvexPolygon2D(pointToProject, convexPolygon2D, hullSize, clockwiseOrdered, acualProjection);
         assertTrue(success);
         EuclidCoreTestTools.assertTuple2DEquals("Iteration: " + i, vertex, acualProjection, EPSILON);
      }

      for (int i = 0; i < ITERATIONS; i++)
      { // Setup: ensure nothing happens if the query is inside the polygon.
         List<? extends Point2DReadOnly> convexPolygon2D = generateRandomPointCloud2D(random, 10.0, 10.0, 100);
         int hullSize = inPlaceGrahamScanConvexHull2D(convexPolygon2D);
         boolean clockwiseOrdered = random.nextBoolean();
         if (!clockwiseOrdered)
            Collections.reverse(convexPolygon2D.subList(0, hullSize));


         Point2D centroid = new Point2D();
         computeConvexPolyong2DArea(convexPolygon2D, hullSize, clockwiseOrdered, centroid);
         int vertexIndex = random.nextInt(hullSize);
         int nextVertexIndex = next(vertexIndex, hullSize);
         Point2DReadOnly vertex = convexPolygon2D.get(vertexIndex);
         Point2DReadOnly nextVertex = convexPolygon2D.get(nextVertexIndex);

         Point2D pointOnEdge = new Point2D();
         pointOnEdge.interpolate(vertex, nextVertex, random.nextDouble());

         Point2D pointInside = new Point2D();
         pointInside.interpolate(centroid, pointOnEdge, random.nextDouble());

         Point2D actualProjection = new Point2D(Double.NaN, Double.NaN);
         boolean success = orthogonalProjectionOnConvexPolygon2D(pointInside, convexPolygon2D, hullSize, clockwiseOrdered, actualProjection);
         assertFalse(success);
         EuclidCoreTestTools.assertTuple2DContainsOnlyNaN(actualProjection);
      }
   }

   @Test
   public void testRemove() throws Exception
   {
      Random random = new Random(35L);

      for (int i = 0; i < ITERATIONS; i++)
      {
         int numberOfPoints = 100;
         List<Integer> points = new ArrayList<>();
         for (int j = 0; j < numberOfPoints; j++)
            points.add(new Integer(j));

         List<Integer> pointsCopy = new ArrayList<>(points);
         int listSize = random.nextInt(numberOfPoints) + 1;

         int removeIndex = random.nextInt(listSize);
         Integer removedElement = pointsCopy.remove(removeIndex);

         EuclidGeometryPolygonTools.remove(points, removeIndex, listSize);

         assertTrue(points.get(listSize - 1) == removedElement);

         for (int j = removeIndex; j < listSize - 1; j++)
            assertTrue(points.get(j) == pointsCopy.get(j));

         for (int j = listSize; j < numberOfPoints; j++)
            assertTrue(points.get(j) == pointsCopy.get(j - 1));
      }
   }

   @Test
   public void testFindMinXMaxYVertexIndex() throws Exception
   {
      Random random = new Random(234234L);

      for (int i = 0; i < ITERATIONS; i++)
      { // Test with numberOfVerticess == list.size()
         int numberOfPoints = 100;
         List<Point2D> points = new ArrayList<>();
         double minX = EuclidCoreRandomTools.generateRandomDouble(random, 5.0);
         double minXMaxY = EuclidCoreRandomTools.generateRandomDouble(random, 5.0);

         for (int j = 0; j < numberOfPoints; j++)
         {
            double x;
            double y;

            if (random.nextDouble() < 0.15)
            {
               x = EuclidCoreRandomTools.generateRandomDouble(random, minX, minX + 10.0);
               y = EuclidCoreRandomTools.generateRandomDouble(random, 10.0);
            }
            else
            {
               x = minX;
               y = EuclidCoreRandomTools.generateRandomDouble(random, minXMaxY - 10.0, minXMaxY);
            }

            points.add(new Point2D(x, y));
         }

         int expectedMinXMaxYIndex = random.nextInt(numberOfPoints);
         points.set(expectedMinXMaxYIndex, new Point2D(minX, minXMaxY));

         int actualMinXMaxYIndex = EuclidGeometryPolygonTools.findMinXMaxYVertexIndex(points, numberOfPoints);

         assertEquals(expectedMinXMaxYIndex, actualMinXMaxYIndex);
      }

      for (int i = 0; i < ITERATIONS; i++)
      { // Test with numberOfVerticess != list.size()
         int numberOfPoints = 100;
         int listSize = numberOfPoints + random.nextInt(100);
         List<Point2D> points = new ArrayList<>();
         double minX = EuclidCoreRandomTools.generateRandomDouble(random, 5.0);
         double minXMaxY = EuclidCoreRandomTools.generateRandomDouble(random, 5.0);

         for (int j = 0; j < listSize; j++)
         {
            double x;
            double y;

            if (random.nextDouble() < 0.15)
            {
               x = EuclidCoreRandomTools.generateRandomDouble(random, minX, minX + 10.0);
               y = EuclidCoreRandomTools.generateRandomDouble(random, 10.0);
            }
            else
            {
               x = minX;
               y = EuclidCoreRandomTools.generateRandomDouble(random, minXMaxY - 10.0, minXMaxY);
            }

            points.add(new Point2D(x, y));
         }

         int expectedMinXMaxYIndex = random.nextInt(numberOfPoints);
         points.set(expectedMinXMaxYIndex, new Point2D(minX, minXMaxY));

         int actualMinXMaxYIndex = EuclidGeometryPolygonTools.findMinXMaxYVertexIndex(points, numberOfPoints);

         assertEquals(expectedMinXMaxYIndex, actualMinXMaxYIndex);
      }
   }
}
