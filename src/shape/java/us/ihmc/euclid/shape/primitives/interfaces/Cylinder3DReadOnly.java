package us.ihmc.euclid.shape.primitives.interfaces;

import us.ihmc.euclid.geometry.interfaces.BoundingBox3DBasics;
import us.ihmc.euclid.geometry.interfaces.Line3DReadOnly;
import us.ihmc.euclid.geometry.tools.EuclidGeometryTools;
import us.ihmc.euclid.shape.tools.EuclidShapeTools;
import us.ihmc.euclid.tools.EuclidCoreTools;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.UnitVector3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;

/**
 * Read-only interface for a cylinder 3D.
 * <p>
 * A cylinder 3D is represented by its length, its radius, the position of its center, and its axis
 * of revolution.
 * </p>
 *
 * @author Sylvain Bertrand
 */
public interface Cylinder3DReadOnly extends Shape3DReadOnly
{
   /**
    * Gets the length of this cylinder.
    *
    * @return the value of the length.
    */
   double getLength();

   /**
    * Gets the half-length of this cylinder.
    *
    * @return the half-length.
    */
   default double getHalfLength()
   {
      return 0.5 * getLength();
   }

   /**
    * Gets the radius of this cylinder.
    *
    * @return the value of the radius.
    */
   double getRadius();

   /**
    * Gets the read-only reference of the position of this cylinder center.
    *
    * @return the position of this cylinder.
    */
   Point3DReadOnly getPosition();

   /**
    * Gets the read-only reference of this cylinder axis of revolution.
    *
    * @return the axis of this cylinder.
    */
   UnitVector3DReadOnly getAxis();

   /**
    * {@inheritDoc}
    * <p>
    * Note that the centroid is also the position of this cylinder.
    * </p>
    */
   @Override
   default Point3DReadOnly getCentroid()
   {
      return getPosition();
   }

   /** {@inheritDoc} */
   @Override
   default double getVolume()
   {
      return EuclidShapeTools.cylinderVolume(getRadius(), getLength());
   }

   /**
    * Gets the read-only reference to the center of the top cap.
    * <p>
    * WARNING: The default implementation of this method generates garbage.
    * </p>
    *
    * @return the top center.
    */
   default Point3DReadOnly getTopCenter()
   {
      Point3D topCenter = new Point3D();
      topCenter.scaleAdd(getHalfLength(), getAxis(), getPosition());
      return topCenter;
   }

   /**
    * Gets the read-only reference to the center of the bottom cap.
    * <p>
    * WARNING: The default implementation of this method generates garbage.
    * </p>
    *
    * @return the bottom center.
    */
   default Point3DReadOnly getBottomCenter()
   {
      Point3D bottomCenter = new Point3D();
      bottomCenter.scaleAdd(-getHalfLength(), getAxis(), getPosition());
      return bottomCenter;
   }

   /** {@inheritDoc} */
   @Override
   default boolean containsNaN()
   {
      return getPosition().containsNaN() || getAxis().containsNaN() || Double.isNaN(getLength()) || Double.isNaN(getRadius());
   }

   /** {@inheritDoc} */
   @Override
   default boolean evaluatePoint3DCollision(Point3DReadOnly pointToCheck, Point3DBasics closestPointOnSurfaceToPack, Vector3DBasics normalAtClosestPointToPack)
   {
      return EuclidShapeTools.evaluatePoint3DCylinder3DCollision(pointToCheck,
                                                                 getPosition(),
                                                                 getAxis(),
                                                                 getLength(),
                                                                 getRadius(),
                                                                 closestPointOnSurfaceToPack,
                                                                 normalAtClosestPointToPack) <= 0.0;
   }

   /** {@inheritDoc} */
   @Override
   default boolean getSupportingVertex(Vector3DReadOnly supportDirection, Point3DBasics supportingVertexToPack)
   {
      EuclidShapeTools.supportingVertexCylinder3D(supportDirection, getPosition(), getAxis(), getLength(), getRadius(), supportingVertexToPack);
      return true;
   }

   /** {@inheritDoc} */
   @Override
   default double signedDistance(Point3DReadOnly point)
   {
      return EuclidShapeTools.signedDistanceBetweenPoint3DAndCylinder3D(point, getPosition(), getAxis(), getLength(), getRadius());
   }

   /** {@inheritDoc} */
   @Override
   default boolean isPointInside(Point3DReadOnly query, double epsilon)
   {
      return EuclidShapeTools.isPoint3DInsideCylinder3D(query, getPosition(), getAxis(), getLength(), getRadius(), epsilon);
   }

   /** {@inheritDoc} */
   @Override
   default boolean orthogonalProjection(Point3DReadOnly pointToProject, Point3DBasics projectionToPack)
   {
      return EuclidShapeTools.orthogonalProjectionOntoCylinder3D(pointToProject, getPosition(), getAxis(), getLength(), getRadius(), projectionToPack);
   }

   /**
    * Computes the coordinates of the possible intersections between a line and this cylinder.
    * <p>
    * In the case the line and this cylinder do not intersect, this method returns {@code 0} and
    * {@code firstIntersectionToPack} and {@code secondIntersectionToPack} remain unmodified.
    * </p>
    *
    * @param line                     the line expressed in world coordinates that may intersect this
    *                                 cylinder. Not modified.
    * @param firstIntersectionToPack  the coordinate in world of the first intersection. Can be
    *                                 {@code null}. Modified.
    * @param secondIntersectionToPack the coordinate in world of the second intersection. Can be
    *                                 {@code null}. Modified.
    * @return the number of intersections between the line and this cylinder. It is either equal to 0,
    *         1, or 2.
    */
   default int intersectionWith(Line3DReadOnly line, Point3DBasics firstIntersectionToPack, Point3DBasics secondIntersectionToPack)
   {
      return intersectionWith(line.getPoint(), line.getDirection(), firstIntersectionToPack, secondIntersectionToPack);
   }

   /**
    * Computes the coordinates of the possible intersections between a line and this cylinder.
    * <p>
    * In the case the line and this cylinder do not intersect, this method returns {@code 0} and
    * {@code firstIntersectionToPack} and {@code secondIntersectionToPack} remain unmodified.
    * </p>
    *
    * @param pointOnLine              a point expressed in world located on the infinitely long line.
    *                                 Not modified.
    * @param lineDirection            the direction expressed in world of the line. Not modified.
    * @param firstIntersectionToPack  the coordinate in world of the first intersection. Can be
    *                                 {@code null}. Modified.
    * @param secondIntersectionToPack the coordinate in world of the second intersection. Can be
    *                                 {@code null}. Modified.
    * @return the number of intersections between the line and this cylinder. It is either equal to 0,
    *         1, or 2.
    */
   default int intersectionWith(Point3DReadOnly pointOnLine, Vector3DReadOnly lineDirection, Point3DBasics firstIntersectionToPack,
                                Point3DBasics secondIntersectionToPack)
   {
      return EuclidGeometryTools.intersectionBetweenLine3DAndCylinder3D(getLength(),
                                                                        getRadius(),
                                                                        getPosition(),
                                                                        getAxis(),
                                                                        pointOnLine,
                                                                        lineDirection,
                                                                        firstIntersectionToPack,
                                                                        secondIntersectionToPack);
   }

   /** {@inheritDoc} */
   @Override
   default void getBoundingBox(BoundingBox3DBasics boundingBoxToPack)
   {
      EuclidShapeTools.boundingBoxCylinder3D(getPosition(), getAxis(), getLength(), getRadius(), boundingBoxToPack);
   }

   /** {@inheritDoc} */
   @Override
   default boolean isConvex()
   {
      return true;
   }

   /** {@inheritDoc} */
   @Override
   default boolean isPrimitive()
   {
      return true;
   }

   /** {@inheritDoc} */
   @Override
   default boolean isDefinedByPose()
   {
      return false;
   }

   /**
    * Returns {@code null} as this shape is not defined by a pose.
    */
   @Override
   default Shape3DPoseReadOnly getPose()
   {
      return null;
   }

   @Override
   Cylinder3DBasics copy();

   /**
    * Tests on a per component basis if this cylinder and {@code other} are equal to an
    * {@code epsilon}.
    *
    * @param other   the other cylinder to compare against this. Not modified.
    * @param epsilon tolerance to use when comparing each component.
    * @return {@code true} if the two cylinders are equal component-wise, {@code false} otherwise.
    */
   default boolean epsilonEquals(Cylinder3DReadOnly other, double epsilon)
   {
      return EuclidCoreTools.epsilonEquals(getLength(), other.getLength(), epsilon) && EuclidCoreTools.epsilonEquals(getRadius(), other.getRadius(), epsilon)
            && getPosition().epsilonEquals(other.getPosition(), epsilon) && other.getAxis().epsilonEquals(other.getAxis(), epsilon);
   }

   /**
    * Compares {@code this} and {@code other} to determine if the two cylinders are geometrically
    * similar.
    *
    * @param other   the cylinder to compare to. Not modified.
    * @param epsilon the tolerance of the comparison.
    * @return {@code true} if the cylinders represent the same geometry, {@code false} otherwise.
    */
   default boolean geometricallyEquals(Cylinder3DReadOnly other, double epsilon)
   {
      if (Math.abs(getRadius() - other.getRadius()) > epsilon || Math.abs(getLength() - other.getLength()) > epsilon)
         return false;

      if (!getPosition().geometricallyEquals(other.getPosition(), epsilon))
         return false;

      return EuclidGeometryTools.areVector3DsParallel(getAxis(), other.getAxis(), epsilon);
   }

   /**
    * Tests on a per component basis, if this cylinder 3D is exactly equal to {@code other}.
    *
    * @param other the other cylinder 3D to compare against this. Not modified.
    * @return {@code true} if the two cylinders are exactly equal component-wise, {@code false}
    *         otherwise.
    */
   default boolean equals(Cylinder3DReadOnly other)
   {
      if (other == this)
      {
         return true;
      }
      else if (other == null)
      {
         return false;
      }
      else
      {
         if (getLength() != other.getLength())
            return false;
         if (getRadius() != other.getRadius())
            return false;
         if (!getPosition().equals(other.getPosition()))
            return false;
         if (!getAxis().equals(other.getAxis()))
            return false;
         return true;
      }
   }
}
