package us.ihmc.euclid.shape.convexPolytope;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import us.ihmc.euclid.geometry.tools.EuclidGeometryTools;
import us.ihmc.euclid.shape.convexPolytope.interfaces.Vertex3DReadOnly;
import us.ihmc.euclid.shape.convexPolytope.tools.EuclidPolytopeTestTools;
import us.ihmc.euclid.shape.convexPolytope.tools.IcoSphereFactory;
import us.ihmc.euclid.shape.convexPolytope.tools.IcoSphereFactory.GeometryMesh3D;
import us.ihmc.euclid.shape.tools.EuclidShapeRandomTools;
import us.ihmc.euclid.testSuite.EuclidTestSuite;
import us.ihmc.euclid.tools.EuclidCoreRandomTools;
import us.ihmc.euclid.tools.EuclidCoreTestTools;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;

public class ConvexPolytope3DTest
{
   private static final int ITERATIONS = EuclidTestSuite.ITERATIONS;
   private static final double EPSILON = 1.0e-12;

   @Test
   public void testConstrustingAPolytope()
   {
      Random random = new Random(4533543);

      ConvexPolytope3D polytope = new ConvexPolytope3D();

      // Testing properties for empty polytope.
      assertEquals(0, polytope.getNumberOfVertices());
      assertEquals(0, polytope.getNumberOfEdges());
      assertEquals(0, polytope.getNumberOfFaces());

      List<Point3D> pointsAdded = new ArrayList<>();

      // Testing properties for single vertex polytope.
      Point3D firstVertex = EuclidCoreRandomTools.nextPoint3D(random);
      polytope.addVertex(firstVertex, 0.0);
      pointsAdded.add(firstVertex);

      assertEquals(1, polytope.getNumberOfVertices());
      assertEquals(0, polytope.getNumberOfEdges());
      assertEquals(1, polytope.getNumberOfFaces());
      EuclidCoreTestTools.assertTuple3DEquals(firstVertex, polytope.getCentroid(), EPSILON);

      for (int vertexIndex = 0; vertexIndex < pointsAdded.size(); vertexIndex++)
      {
         EuclidCoreTestTools.assertTuple3DEquals(pointsAdded.get(vertexIndex), polytope.getVertex(vertexIndex), EPSILON);
      }

      // Assert that adding the same point twice does not change anything
      polytope.addVertex(firstVertex, 0.0);
      assertEquals(1, polytope.getNumberOfVertices());
      assertEquals(0, polytope.getNumberOfEdges());
      assertEquals(1, polytope.getNumberOfFaces());

      // Testing properties for single edge polytope.
      Point3D secondVertex = EuclidCoreRandomTools.nextPoint3D(random);
      polytope.addVertex(secondVertex, 0.0);
      pointsAdded.add(secondVertex);

      assertEquals(2, polytope.getNumberOfVertices());
      assertEquals(1, polytope.getNumberOfEdges());
      assertEquals(1, polytope.getNumberOfFaces());
      EuclidCoreTestTools.assertTuple3DEquals(EuclidGeometryTools.averagePoint3Ds(pointsAdded), polytope.getCentroid(), EPSILON);

      for (int vertexIndex = 0; vertexIndex < pointsAdded.size(); vertexIndex++)
      {
         EuclidCoreTestTools.assertTuple3DEquals(pointsAdded.get(vertexIndex), polytope.getVertex(vertexIndex), EPSILON);
      }

      for (int i = 0; i < ITERATIONS; i++)
      { // Assert that adding a point that is on the edge does not change anything.
         Point3D pointInside = EuclidShapeRandomTools.nextWeightedAverage(random, pointsAdded);
         polytope.addVertex(pointInside, 1.0e-12);

         assertEquals(2, polytope.getNumberOfVertices());
         assertEquals(1, polytope.getNumberOfEdges());
         assertEquals(1, polytope.getNumberOfFaces());
      }

      // Testing properties for single triangle face polytope
      Point3D thirdVertex = EuclidCoreRandomTools.nextPoint3D(random);
      polytope.addVertex(thirdVertex, 0.0);
      pointsAdded.add(thirdVertex);

      assertEquals(3, polytope.getNumberOfVertices());
      //      assertEquals(3, polytope.getNumberOfEdges()); // FIXME
      assertEquals(1, polytope.getNumberOfFaces());
      Point3D expectedCentroid = EuclidGeometryTools.averagePoint3Ds(pointsAdded);
      EuclidCoreTestTools.assertTuple3DEquals(expectedCentroid, polytope.getCentroid(), EPSILON);

      for (int vertexIndex = 0; vertexIndex < pointsAdded.size(); vertexIndex++)
      {
         EuclidCoreTestTools.assertTuple3DEquals(pointsAdded.get(vertexIndex), polytope.getVertex(vertexIndex), EPSILON);
      }

      EuclidCoreTestTools.assertTuple3DEquals(expectedCentroid, polytope.getFace(0).getCentroid(), EPSILON);
      Vector3D expectedNormal = EuclidGeometryTools.normal3DFromThreePoint3Ds(firstVertex, secondVertex, thirdVertex);
      if (expectedNormal.dot(polytope.getFace(0).getNormal()) < 0.0)
         expectedNormal.negate();
      EuclidCoreTestTools.assertTuple3DEquals(expectedNormal, polytope.getFace(0).getNormal(), EPSILON);

      for (int i = 0; i < ITERATIONS; i++)
      { // Assert that adding a point that is on the face does not change anything.
         Point3D pointInside = EuclidShapeRandomTools.nextWeightedAverage(random, pointsAdded);
         polytope.addVertex(pointInside, 1.0e-12);

         assertEquals(3, polytope.getNumberOfVertices());
         //      assertEquals(3, polytope.getNumberOfEdges()); // FIXME
         assertEquals(1, polytope.getNumberOfFaces());
      }

      // We finally have an usual polytope: a tetrahedron
      Point3D fourthVertex = EuclidCoreRandomTools.nextPoint3D(random);
      polytope.addVertex(fourthVertex, 0.0);
      pointsAdded.add(fourthVertex);

      assertEquals(4, polytope.getNumberOfVertices());
      assertEquals(6, polytope.getNumberOfEdges());
      assertEquals(4, polytope.getNumberOfFaces());
      expectedCentroid = EuclidGeometryTools.averagePoint3Ds(pointsAdded);
      EuclidCoreTestTools.assertTuple3DEquals(expectedCentroid, polytope.getCentroid(), EPSILON);

      for (int vertexIndex = 0; vertexIndex < polytope.getVertices().size(); vertexIndex++)
      {
         Vertex3D vertex = polytope.getVertex(vertexIndex);
         // Assert that each vertex is unique
         assertTrue(polytope.getVertices().stream().noneMatch(otherVertex -> (otherVertex != vertex && otherVertex.epsilonEquals(vertex, EPSILON))));
         // Assert that all vertex are from the points added
         assertTrue(pointsAdded.stream().anyMatch(point -> point.epsilonEquals(vertex, EPSILON)));

         for (HalfEdge3D edge : vertex.getAssociatedEdges())
            assertTrue(edge.getOrigin() == vertex);
      }

      for (int edgeIndex = 0; edgeIndex < polytope.getEdges().size(); edgeIndex++)
      {
         HalfEdge3D edge = polytope.getEdge(edgeIndex);
         // Assert that each vertex is unique
         assertTrue(polytope.getEdges().stream().noneMatch(otherEdge -> (otherEdge != edge && otherEdge.epsilonEquals(edge, EPSILON))));
         // Assert that all vertex are from the points added
         assertTrue(pointsAdded.stream().anyMatch(point -> point.epsilonEquals(edge.getOrigin(), EPSILON)));
         assertTrue(pointsAdded.stream().anyMatch(point -> point.epsilonEquals(edge.getDestination(), EPSILON)));

         EuclidPolytopeTestTools.assertVertex3DEquals(edge.getOrigin(), edge.getTwinEdge().getDestination(), EPSILON);
         EuclidPolytopeTestTools.assertVertex3DEquals(edge.getDestination(), edge.getTwinEdge().getOrigin(), EPSILON);

         EuclidPolytopeTestTools.assertVertex3DEquals(edge.getOrigin(), edge.getPreviousEdge().getDestination(), EPSILON);
         EuclidPolytopeTestTools.assertVertex3DEquals(edge.getDestination(), edge.getNextEdge().getOrigin(), EPSILON);

         EuclidPolytopeTestTools.assertVertex3DEquals(edge.getOrigin(), edge.getPreviousEdge().getTwinEdge().getOrigin(), EPSILON);
         EuclidPolytopeTestTools.assertVertex3DEquals(edge.getDestination(), edge.getNextEdge().getTwinEdge().getDestination(), EPSILON);

         EuclidPolytopeTestTools.assertVertex3DEquals(edge.getOrigin(), edge.getTwinEdge().getNextEdge().getOrigin(), EPSILON);
         EuclidPolytopeTestTools.assertVertex3DEquals(edge.getDestination(), edge.getTwinEdge().getPreviousEdge().getDestination(), EPSILON);

         assertTrue(edge.getTwinEdge().getTwinEdge() == edge);
         assertTrue(edge.getNextEdge().getPreviousEdge() == edge);
         assertTrue(edge.getPreviousEdge().getNextEdge() == edge);
      }

      for (int faceIndex = 0; faceIndex < polytope.getFaces().size(); faceIndex++)
      {
         Face3D face = polytope.getFace(faceIndex);
         assertTrue(polytope.getFaces().stream().noneMatch(otherFace -> (otherFace != face && otherFace.epsilonEquals(face, EPSILON))));
         // Assert that all vertex are from the points added
         for (HalfEdge3D edge : face.getEdges())
         {
            assertTrue(pointsAdded.stream().anyMatch(point -> point.epsilonEquals(edge.getOrigin(), EPSILON)));
            assertTrue(pointsAdded.stream().anyMatch(point -> point.epsilonEquals(edge.getDestination(), EPSILON)));
         }
      }

      for (int i = 0; i < ITERATIONS; i++)
      { // Assert that adding a point that is inside the polytope does not change anything.
         Point3D pointInside = EuclidShapeRandomTools.nextWeightedAverage(random, pointsAdded);

         assertTrue(polytope.isInteriorPoint(pointInside, EPSILON));

         polytope.addVertex(pointInside, 0.0);

         assertEquals(4, polytope.getNumberOfVertices());
         assertEquals(6, polytope.getNumberOfEdges());
         assertEquals(4, polytope.getNumberOfFaces());
      }
   }

   @SuppressWarnings("unlikely-arg-type")
   @Test
   void testTetrahedron() throws Exception
   {
      Point3D top = new Point3D(0.0, 0.0, 1.0);
      Point3D bottomP0 = new Point3D(-0.5, -0.5, 0.0);
      Point3D bottomP1 = new Point3D(0.5, -0.5, 0.0);
      Point3D bottomP2 = new Point3D(0.0, 0.5, 0.0);

      ConvexPolytope3D convexPolytope3D = new ConvexPolytope3D();
      convexPolytope3D.addVertex(bottomP0, 0.0);
      convexPolytope3D.addVertex(bottomP1, 0.0);
      convexPolytope3D.addVertex(bottomP2, 0.0);
      convexPolytope3D.addVertex(top, 0.0);

      assertTrue(convexPolytope3D.getVertices().contains(top));
      assertTrue(convexPolytope3D.getVertices().contains(bottomP0));
      assertTrue(convexPolytope3D.getVertices().contains(bottomP1));
      assertTrue(convexPolytope3D.getVertices().contains(bottomP2));

      assertEquals(4, convexPolytope3D.getNumberOfVertices());
      assertEquals(6, convexPolytope3D.getNumberOfEdges());
      assertEquals(4, convexPolytope3D.getNumberOfFaces());

      Vector3D bottomNormal = new Vector3D(0.0, 0.0, -1.0);
      Vector3D sideYMinusNormal = EuclidGeometryTools.normal3DFromThreePoint3Ds(top, bottomP0, bottomP1);
      if (sideYMinusNormal.getY() > 0.0)
         sideYMinusNormal.negate();
      Vector3D sideXMinusNormal = EuclidGeometryTools.normal3DFromThreePoint3Ds(top, bottomP0, bottomP2);
      if (sideXMinusNormal.getX() > 0.0)
         sideXMinusNormal.negate();
      Vector3D sideXPlusNormal = new Vector3D(sideXMinusNormal);
      sideXPlusNormal.setX(-sideXPlusNormal.getX());

      for (int faceIndex = 0; faceIndex < 4; faceIndex++)
      {
         Face3D face = convexPolytope3D.getFace(faceIndex);
         Vector3D normal = face.getNormal();

         if (normal.epsilonEquals(bottomNormal, EPSILON))
         {
            assertTrue(convexPolytope3D.getVertices().contains(bottomP0));
            assertTrue(convexPolytope3D.getVertices().contains(bottomP1));
            assertTrue(convexPolytope3D.getVertices().contains(bottomP2));
         }
         else if (normal.epsilonEquals(sideYMinusNormal, EPSILON))
         {
            assertTrue(convexPolytope3D.getVertices().contains(top));
            assertTrue(convexPolytope3D.getVertices().contains(bottomP0));
            assertTrue(convexPolytope3D.getVertices().contains(bottomP1));
         }
         else if (normal.epsilonEquals(sideXMinusNormal, EPSILON))
         {
            assertTrue(convexPolytope3D.getVertices().contains(top));
            assertTrue(convexPolytope3D.getVertices().contains(bottomP0));
            assertTrue(convexPolytope3D.getVertices().contains(bottomP2));
         }
         else if (normal.epsilonEquals(sideXPlusNormal, EPSILON))
         {
            assertTrue(convexPolytope3D.getVertices().contains(top));
            assertTrue(convexPolytope3D.getVertices().contains(bottomP0));
            assertTrue(convexPolytope3D.getVertices().contains(bottomP1));
         }
         else
         {
            fail("Unexpected face normal: " + normal);
         }
      }
   }

   @Test
   void testUnitLengthCube() throws Exception
   {
      Point3D bottomP0 = new Point3D(-0.5, -0.5, 0.0);
      Point3D bottomP1 = new Point3D(-0.5, 0.5, 0.0);
      Point3D bottomP2 = new Point3D(0.5, 0.5, 0.0);
      Point3D bottomP3 = new Point3D(0.5, -0.5, 0.0);

      Point3D topP0 = new Point3D(-0.5, -0.5, 1.0);
      Point3D topP1 = new Point3D(-0.5, 0.5, 1.0);
      Point3D topP2 = new Point3D(0.5, 0.5, 1.0);
      Point3D topP3 = new Point3D(0.5, -0.5, 1.0);

      double buildEpsilon = 1.0e-10;
      ConvexPolytope3D convexPolytope3D = new ConvexPolytope3D();
      convexPolytope3D.addVertex(bottomP0, buildEpsilon);
      convexPolytope3D.addVertex(bottomP1, buildEpsilon);
      convexPolytope3D.addVertex(bottomP2, buildEpsilon);
      convexPolytope3D.addVertex(bottomP3, buildEpsilon);

      convexPolytope3D.addVertex(topP0, buildEpsilon);
      convexPolytope3D.addVertex(topP1, buildEpsilon);
      convexPolytope3D.addVertex(topP2, buildEpsilon);
      convexPolytope3D.addVertex(topP3, buildEpsilon);
   }

   @Test
   void testConstructIcosahedron() throws Exception
   {
      Random random = new Random(23423);
      GeometryMesh3D icosahedron = IcoSphereFactory.newIcoSphere(0);

      for (int i = 0; i < ITERATIONS; i++)
      {
         ConvexPolytope3D convexPolytope3D = new ConvexPolytope3D();
         List<Point3D> shuffledVertices = new ArrayList<>(icosahedron.getVertices());
         Collections.shuffle(shuffledVertices, random);
         
         shuffledVertices.forEach(vertex -> convexPolytope3D.addVertex(vertex, 1.0e-10));
         
         // https://en.wikipedia.org/wiki/Icosahedron
         assertEquals(12, convexPolytope3D.getNumberOfVertices());
         assertEquals(30, convexPolytope3D.getNumberOfEdges());
         assertEquals(20, convexPolytope3D.getNumberOfFaces());
         
         for (Vertex3DReadOnly vertex : convexPolytope3D.getVertices())
         {
            assertTrue(icosahedron.getVertices().stream().anyMatch(p -> p.epsilonEquals(vertex, EPSILON)));
         }
         
         for (Face3D face : convexPolytope3D.getFaces())
         {
            assertEquals(3, face.getNumberOfEdges());
            
            Vector3D normalDirectionGuess = new Vector3D();
            normalDirectionGuess.sub(face.getCentroid(), convexPolytope3D.getCentroid());
            assertTrue(normalDirectionGuess.dot(face.getNormal()) > 0.0);
            
            Vertex3D a = face.getVertex(0);
            Vertex3D b = face.getVertex(1);
            Vertex3D c = face.getVertex(2);
            assertTrue(icosahedron.getAllTriangles().stream().anyMatch(triangle -> triangle.geometryEquals(a, b, c, EPSILON)));
         }

         for (HalfEdge3D edge : convexPolytope3D.getEdges())
         {
            assertNotNull(edge.getTwinEdge());
            Vertex3D a0 = edge.getOrigin();
            Vertex3D b0 = edge.getDestination();
            Vertex3D a1 = edge.getTwinEdge().getDestination();
            Vertex3D b1 = edge.getTwinEdge().getOrigin();

            assertTrue(a0 == a1);
            assertTrue(b0 == b1);

            assertTrue(edge.getOrigin().getAssociatedEdges().contains(edge));
         }
      }
   }

   @Test
   void testGetFaceContainingPointClosestTo() throws Exception
   {
      Random random = new Random(34656);

      { // Testing with a tetrahedron
         Point3D top = new Point3D(0.0, 0.0, 1.0);
         Point3D bottomP0 = new Point3D(-0.5, -0.5, 0.0);
         Point3D bottomP1 = new Point3D(0.5, -0.5, 0.0);
         Point3D bottomP2 = new Point3D(0.0, 0.5, 0.0);

         ConvexPolytope3D convexPolytope3D = new ConvexPolytope3D();
         convexPolytope3D.addVertex(bottomP0, 0.0);
         convexPolytope3D.addVertex(bottomP1, 0.0);
         convexPolytope3D.addVertex(bottomP2, 0.0);
         convexPolytope3D.addVertex(top, 0.0);

         for (int i = 0; i < ITERATIONS; i++)
         {
            for (int faceIndex = 0; faceIndex < 4; faceIndex++)
            {
               Face3D face = convexPolytope3D.getFace(faceIndex);
               Point3D pointOnFace = EuclidShapeRandomTools.nextPoint3DInTriangle(random, face.getVertex(0), face.getVertex(1), face.getVertex(2));
               assertTrue(face == convexPolytope3D.getFaceContainingPointClosestTo(pointOnFace));

               Point3D pointOutside = new Point3D();
               pointOutside.scaleAdd(EuclidCoreRandomTools.nextDouble(random, 0.0, 10.0), face.getNormal(), pointOnFace);
               assertTrue(face == convexPolytope3D.getFaceContainingPointClosestTo(pointOutside));
            }

            // TODO need to add couple with point: closest to an edge, closest to a vertex
         }
      }
      // TODO need to add tests with more complicated polytope
   }

   @Test
   void testGetVisibleFaces() throws Exception
   {
      Random random = new Random(43656);

      { // Testing with a tetrahedron
         Point3D top = new Point3D(0.0, 0.0, 1.0);
         Point3D bottomP0 = new Point3D(-0.5, -0.5, 0.0);
         Point3D bottomP1 = new Point3D(0.5, -0.5, 0.0);
         Point3D bottomP2 = new Point3D(0.0, 0.5, 0.0);

         ConvexPolytope3D convexPolytope3D = new ConvexPolytope3D();
         convexPolytope3D.addVertex(bottomP0, 0.0);
         convexPolytope3D.addVertex(bottomP1, 0.0);
         convexPolytope3D.addVertex(bottomP2, 0.0);
         convexPolytope3D.addVertex(top, 0.0);

         for (int i = 0; i < ITERATIONS; i++)
         {
            for (int faceIndex = 0; faceIndex < 4; faceIndex++)
            { // Expecting only 1 visible face
               Face3D face = convexPolytope3D.getFace(faceIndex);
               Point3D pointOnFace = EuclidShapeRandomTools.nextPoint3DInTriangle(random, face.getVertex(0), face.getVertex(1), face.getVertex(2));
               Point3D pointOutside = new Point3D();
               pointOutside.scaleAdd(EuclidCoreRandomTools.nextDouble(random, 0.0, 10.0), face.getNormal(), pointOnFace);

               List<Face3D> actualVisibleFaces = new ArrayList<>();
               Face3D leastVisibleFace = convexPolytope3D.getVisibleFaces(actualVisibleFaces, pointOutside, 0.0);

               assertEquals(1, actualVisibleFaces.size());
               assertTrue(face == actualVisibleFaces.get(0));
               assertTrue(face == leastVisibleFace);
            }

            for (int faceIndex = 0; faceIndex < 4; faceIndex++)
            { // Expecting only 2 visible faces
               Face3D firstFace = convexPolytope3D.getFace(faceIndex);
               HalfEdge3D edge = firstFace.getEdge(random.nextInt(3));
               Face3D secondFace = edge.getTwinEdge().getFace();

               Vector3D edgeNormal = new Vector3D();
               edgeNormal.interpolate(firstFace.getNormal(), secondFace.getNormal(), 0.5);
               edgeNormal.normalize();

               Point3D pointOnEdge = new Point3D();
               pointOnEdge.interpolate(edge.getOrigin(), edge.getDestination(), EuclidCoreRandomTools.nextDouble(random, 0.0, 1.0));

               { // Case #1: the firstFace is the most visible
                  Vector3D directionLimit = new Vector3D(); // Represents the limit before secondFace becomes invisible.
                  directionLimit.cross(edge.getDirection(false), secondFace.getNormal());
                  directionLimit.normalize();
                  assertTrue(directionLimit.dot(firstFace.getNormal()) > 0.0); // This is only to ensure that we've constructed the limit such that it is on the firstFace side.

                  Vector3D extractionDirection = new Vector3D();
                  extractionDirection.interpolate(directionLimit, edgeNormal, EuclidCoreRandomTools.nextDouble(random, 0.0, 1.0));
                  extractionDirection.normalize();

                  Point3D pointOutside = new Point3D();
                  pointOutside.scaleAdd(EuclidCoreRandomTools.nextDouble(random, 0.0, 10.0), extractionDirection, pointOnEdge);

                  List<Face3D> actualVisibleFaces = new ArrayList<>();
                  Face3D leastVisibleFace = convexPolytope3D.getVisibleFaces(actualVisibleFaces, pointOutside, 0.0);

                  assertEquals(2, actualVisibleFaces.size());
                  assertTrue(actualVisibleFaces.contains(firstFace));
                  assertTrue(actualVisibleFaces.contains(secondFace));
                  assertTrue(secondFace == leastVisibleFace);
               }

               { // Case #2: the secondFace is the most visible (redundant test)
                  Vector3D directionLimit = new Vector3D(); // Represents the limit before firstFace becomes invisible.
                  directionLimit.cross(firstFace.getNormal(), edge.getDirection(false));
                  directionLimit.normalize();
                  assertTrue(directionLimit.dot(secondFace.getNormal()) > 0.0); // This is only to ensure that we've constructed the limit such that it is on the secondFace side.

                  Vector3D extractionDirection = new Vector3D();
                  extractionDirection.interpolate(directionLimit, edgeNormal, EuclidCoreRandomTools.nextDouble(random, 0.0, 1.0));
                  extractionDirection.normalize();

                  Point3D pointOutside = new Point3D();
                  pointOutside.scaleAdd(EuclidCoreRandomTools.nextDouble(random, 0.0, 10.0), extractionDirection, pointOnEdge);

                  List<Face3D> actualVisibleFaces = new ArrayList<>();
                  Face3D leastVisibleFace = convexPolytope3D.getVisibleFaces(actualVisibleFaces, pointOutside, 0.0);

                  assertEquals(2, actualVisibleFaces.size());
                  assertTrue(actualVisibleFaces.contains(firstFace));
                  assertTrue(actualVisibleFaces.contains(secondFace));
                  assertTrue(firstFace == leastVisibleFace);
               }
            }

            for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++)
            { // Expecting 3 visible faces
               Vertex3D vertex = convexPolytope3D.getVertex(vertexIndex);

               Face3D firstFace = vertex.getAssociatedEdge(0).getFace();
               Face3D secondFace = vertex.getAssociatedEdge(1).getFace();
               Face3D thirdFace = vertex.getAssociatedEdge(2).getFace();
               assertTrue(firstFace != secondFace);
               assertTrue(secondFace != thirdFace);
               assertTrue(thirdFace != firstFace);

               Vector3D vertexNormal = new Vector3D();
               vertexNormal.setAndScale(1.0 / 3.0, firstFace.getNormal());
               vertexNormal.scaleAdd(1.0 / 3.0, secondFace.getNormal(), vertexNormal);
               vertexNormal.scaleAdd(1.0 / 3.0, thirdFace.getNormal(), vertexNormal);
               vertexNormal.normalize();

               Point3D equidistantPoint = new Point3D();
               equidistantPoint.scaleAdd(EuclidCoreRandomTools.nextDouble(random, 0.0, 10.0), vertexNormal, vertex);

               { // Assert that when extruding using vertexNormal, the resulting point is equidistant from the three faces.
                  double firstDistance = firstFace.distance(equidistantPoint);
                  double secondDistance = secondFace.distance(equidistantPoint);
                  double thirdDistance = thirdFace.distance(equidistantPoint);
                  assertEquals(firstDistance, secondDistance, EPSILON);
                  assertEquals(secondDistance, thirdDistance, EPSILON);
                  assertEquals(thirdDistance, firstDistance, EPSILON);
               }

               { // Case #1: Shifting equidistantPoint slightly (not too much or the face won't be visible) away from firstFace so it is the least visible face.
                 // TODO The construction used here is not reliable, the least visible face 
                  Vector3D oppositeEdgeNormal = new Vector3D();
                  oppositeEdgeNormal.interpolate(secondFace.getNormal(), thirdFace.getNormal(), 0.5);
                  oppositeEdgeNormal.normalize();

                  Vector3D limitDirection = new Vector3D();
                  limitDirection.cross(firstFace.getNormal(), oppositeEdgeNormal);
                  limitDirection.normalize();
                  limitDirection.cross(limitDirection, firstFace.getNormal());
                  limitDirection.normalize();
                  assertEquals(0.0, limitDirection.dot(firstFace.getNormal()), EPSILON);
                  assertTrue(oppositeEdgeNormal.dot(limitDirection) > 0.0);

                  Vector3D extrusionDirection = new Vector3D();
                  extrusionDirection.interpolate(limitDirection, vertexNormal, EuclidCoreRandomTools.nextDouble(random, 0.001, 0.999));

                  Point3D pointOutside = new Point3D();
                  pointOutside.scaleAdd(EuclidCoreRandomTools.nextDouble(random, 0.0, 1.0), extrusionDirection, vertex);

                  List<Face3D> actualVisibleFaces = new ArrayList<>();
                  Face3D leastVisibleFace = convexPolytope3D.getVisibleFaces(actualVisibleFaces, pointOutside, 0.0);

                  assertEquals(3, actualVisibleFaces.size());
                  assertTrue(actualVisibleFaces.contains(firstFace));
                  assertTrue(actualVisibleFaces.contains(secondFace));
                  assertTrue(actualVisibleFaces.contains(thirdFace));
                  // Problem with the construction.
//                  String errorMessage = "Iteration: " + i + ", vertex index: " + vertexIndex;
//                  assertTrue(firstFace == leastVisibleFace, errorMessage);
               }

            }
         }
      }
   }
}
