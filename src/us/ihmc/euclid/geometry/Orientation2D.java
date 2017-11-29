package us.ihmc.euclid.geometry;

import us.ihmc.euclid.exceptions.NotAMatrix2DException;
import us.ihmc.euclid.geometry.interfaces.Orientation2DBasics;
import us.ihmc.euclid.geometry.tools.EuclidGeometryIOTools;
import us.ihmc.euclid.interfaces.GeometryObject;
import us.ihmc.euclid.tools.EuclidCoreTools;
import us.ihmc.euclid.tools.RotationMatrixTools;
import us.ihmc.euclid.transform.interfaces.Transform;
import us.ihmc.euclid.tuple2D.Vector2D;
import us.ihmc.euclid.tuple2D.interfaces.Tuple2DBasics;
import us.ihmc.euclid.tuple2D.interfaces.Tuple2DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DReadOnly;

/**
 * A {@code Orientation2D} represents an orientation in the XY-plane, i.e. the yaw angle about the
 * z-axis.
 */
public class Orientation2D implements Orientation2DBasics, GeometryObject<Orientation2D>
{
   /** The angle in radians about the z-axis. */
   private double yaw = 0.0;

   /** Vector used to transform {@code this} in {@link #applyTransform(Transform)}. */
   private final Vector2D xVector = new Vector2D(1.0, 0.0);

   /**
    * Creates a new orientation 2D initialized with its yaw angle to zero.
    */
   public Orientation2D()
   {
      setToZero();
   }

   /**
    * Creates a new orientation 2D and initializes its yaw angle to the given {@code yaw}.
    *
    * @param yaw the yaw angle used to initialize this.
    */
   public Orientation2D(double yaw)
   {
      setYaw(yaw);
   }

   /**
    * Creates a new orientation 2D and initializes it to {@code other}.
    *
    * @param other the other orientation 2D used to initialize this. Not modified.
    */
   public Orientation2D(Orientation2D other)
   {
      set(other);
   }

   /**
    * Sets this orientation 2D to the {@code other} orientation 2D.
    *
    * @param other the other orientation 2D. Not modified.
    */
   @Override
   public void set(Orientation2D other)
   {
      Orientation2DBasics.super.set(other);
   }

   /** {@inheritDoc} */
   @Override
   public boolean containsNaN()
   {
      return Double.isNaN(yaw);
   }

   /** {@inheritDoc} */
   @Override
   public void setToNaN()
   {
      yaw = Double.NaN;
   }

   /**
    * Sets the yaw angle of this orientation 2D to zero.
    */
   @Override
   public void setToZero()
   {
      yaw = 0.0;
   }

   /** {@inheritDoc} */
   @Override
   public void setYaw(double yaw)
   {
      this.yaw = EuclidCoreTools.trimAngleMinusPiToPi(yaw);
   }

   /** {@inheritDoc} */
   @Override
   public double getYaw()
   {
      return yaw;
   }

   /**
    * Performs a linear interpolation from {@code this} to {@code other} given the percentage
    * {@code alpha}.
    * <p>
    * this = (1.0 - alpha) * this + alpha * other
    * </p>
    *
    * @param other the other orientation 2D used for the interpolation. Not modified.
    * @param alpha the percentage used for the interpolation. A value of 0 will result in not
    *           modifying {@code this}, while a value of 1 is equivalent to setting {@code this} to
    *           {@code other}.
    */
   public void interpolate(Orientation2D other, double alpha)
   {
      interpolate(this, other, alpha);
   }

   /**
    * Performs a linear interpolation from {@code orientation1} to {@code orientation2} given the
    * percentage {@code alpha}.
    * <p>
    * this = (1.0 - alpha) * orientation1 + alpha * orientation2
    * </p>
    *
    * @param orientation1 the first orientation 2D used in the interpolation. Not modified.
    * @param orientation2 the second orientation 2D used in the interpolation. Not modified.
    * @param alpha the percentage to use for the interpolation. A value of 0 will result in setting
    *           {@code this} to {@code orientation1}, while a value of 1 is equivalent to setting
    *           {@code this} to {@code orientation2}.
    */
   public void interpolate(Orientation2D orientation1, Orientation2D orientation2, double alpha)
   {
      double deltaYaw = EuclidCoreTools.angleDifferenceMinusPiToPi(orientation2.yaw, orientation1.yaw);
      add(orientation1.yaw, alpha * deltaYaw);
   }

   /**
    * Transforms the given {@code tupleToTransform} by the rotation about the z-axis described by
    * this.
    *
    * <pre>
    * tupleToTransform = / cos(yaw) -sin(yaw) \ * tupleToTransform
    *                    \ sin(yaw)  cos(yaw) /
    * </pre>
    *
    * @param tupleToTransform the tuple to transform. Modified.
    */
   public void transform(Tuple2DBasics tupleToTransform)
   {
      transform(tupleToTransform, tupleToTransform);
   }

   /**
    * Transforms the given {@code tupleOriginal} by the rotation about the z-axis described by this
    * and stores the result in {@code tupleTransformed}.
    *
    * <pre>
    * tupleTransformed = / cos(yaw) -sin(yaw) \ * tupleOriginal
    *                    \ sin(yaw)  cos(yaw) /
    * </pre>
    *
    * @param tupleOriginal the tuple to be transformed. Not modified.
    * @param tupleTransformed the tuple in which the result is stored. Modified.
    */
   public void transform(Tuple2DReadOnly tupleOriginal, Tuple2DBasics tupleTransformed)
   {
      RotationMatrixTools.applyYawRotation(yaw, tupleOriginal, tupleTransformed);
   }

   /**
    * Transforms the given {@code tupleToTransform} by the rotation about the z-axis described by
    * this.
    *
    * <pre>
    *                    / cos(yaw) -sin(yaw) 0 \
    * tupleToTransform = | sin(yaw)  cos(yaw) 0 | * tupleToTransform
    *                    \    0         0     1 /
    * </pre>
    *
    * @param tupleToTransform the tuple to transform. Modified.
    */
   public void transform(Tuple3DBasics tupleToTransform)
   {
      transform(tupleToTransform, tupleToTransform);
   }

   /**
    * Transforms the given {@code tupleOriginal} by the rotation about the z-axis described by this
    * and stores the result in {@code tupleTransformed}.
    *
    * <pre>
    *                    / cos(yaw) -sin(yaw) 0 \
    * tupleTransformed = | sin(yaw)  cos(yaw) 0 | * tupleOriginal
    *                    \    0         0     1 /
    * </pre>
    *
    * @param tupleOriginal the tuple to be transformed. Not modified.
    * @param tupleTransformed the tuple in which the result is stored. Modified.
    */
   public void transform(Tuple3DReadOnly tupleOriginal, Tuple3DBasics tupleTransformed)
   {
      RotationMatrixTools.applyYawRotation(yaw, tupleOriginal, tupleTransformed);
   }

   /**
    * Performs the inverse of the transform to the given {@code tupleToTransform} by the rotation
    * about the z-axis described by this.
    *
    * <pre>
    * tupleToTransform = / cos(-yaw) -sin(-yaw) \ * tupleToTransform
    *                    \ sin(-yaw)  cos(-yaw) /
    * </pre>
    *
    * @param tupleToTransform the tuple to transform. Modified.
    */
   public void inverseTransform(Tuple2DBasics tupleToTransform)
   {
      inverseTransform(tupleToTransform, tupleToTransform);
   }

   /**
    * Performs the inverse of the transform to the given {@code tupleOriginal} by the rotation about
    * the z-axis described by this and stores the result in {@code tupleTransformed}.
    *
    * <pre>
    * tupleTransformed = / cos(-yaw) -sin(-yaw) \ * tupleOriginal
    *                    \ sin(-yaw)  cos(-yaw) /
    * </pre>
    *
    * @param tupleOriginal the tuple to be transformed. Not modified.
    * @param tupleTransformed the tuple in which the result is stored. Modified.
    */
   public void inverseTransform(Tuple2DReadOnly tupleOriginal, Tuple2DBasics tupleTransformed)
   {
      RotationMatrixTools.applyYawRotation(-yaw, tupleOriginal, tupleTransformed);
   }

   /**
    * Performs the inverse of the transform to the given {@code tupleToTransform} by the rotation
    * about the z-axis described by this.
    *
    * <pre>
    *                    / cos(-yaw) -sin(-yaw) 0 \
    * tupleToTransform = | sin(-yaw)  cos(-yaw) 0 | * tupleToTransform
    *                    \     0          0     1 /
    * </pre>
    *
    * @param tupleToTransform the tuple to transform. Modified.
    */
   public void inverseTransform(Tuple3DBasics tupleToTransform)
   {
      transform(tupleToTransform, tupleToTransform);
   }

   /**
    * Performs the inverse of the transform to the given {@code tupleOriginal} by the rotation about
    * the z-axis described by this and stores the result in {@code tupleTransformed}.
    *
    * <pre>
    *                    / cos(-yaw) -sin(-yaw) 0 \
    * tupleTransformed = | sin(-yaw)  cos(-yaw) 0 | * tupleOriginal
    *                    \     0          0     1 /
    * </pre>
    *
    * @param tupleOriginal the tuple to be transformed. Not modified.
    * @param tupleTransformed the tuple in which the result is stored. Modified.
    */
   public void inverseTransform(Tuple3DReadOnly tupleOriginal, Tuple3DBasics tupleTransformed)
   {
      RotationMatrixTools.applyYawRotation(-yaw, tupleOriginal, tupleTransformed);
   }

   /**
    * Transforms this orientation 2D by the given {@code transform}.
    * <p>
    * This is equivalent to extracting the yaw rotation part from the given transform and adding it
    * to this.
    * </p>
    *
    * @param transform the geometric transform to apply on this orientation 2D. Not modified.
    * @throws NotAMatrix2DException if the rotation part of {@code transform} is not a
    *            transformation in the XY plane.
    */
   @Override
   public void applyTransform(Transform transform)
   {
      xVector.set(1.0, 0.0);
      transform.transform(xVector);
      double deltaYaw = Math.atan2(xVector.getY(), xVector.getX());

      if (Double.isNaN(deltaYaw) || Double.isInfinite(deltaYaw))
         deltaYaw = 0.0;

      add(deltaYaw);
   }

   /**
    * Transforms this orientation 2D by the inverse of the given {@code transform}.
    * <p>
    * This is equivalent to extracting the yaw rotation part from the given transform and
    * subtracting it to this.
    * </p>
    *
    * @param transform the geometric transform to apply on this orientation 2D. Not modified.
    * @throws NotAMatrix2DException if the rotation part of {@code transform} is not a
    *            transformation in the XY plane.
    */
   @Override
   public void applyInverseTransform(Transform transform)
   {
      xVector.set(1.0, 0.0);
      transform.inverseTransform(xVector);
      double deltaYaw = Math.atan2(xVector.getY(), xVector.getX());

      if (Double.isNaN(deltaYaw) || Double.isInfinite(deltaYaw))
         deltaYaw = 0.0;

      add(deltaYaw);
   }

   /**
    * Tests if this orientation 2D is exactly equal to {@code other}.
    * <p>
    * Note that this method performs number comparison and not an angle comparison, such that:
    * -<i>pi</i> &ne; <i>pi</i>.
    * </p>
    *
    * @param other the other orientation 2D to compare against this. Not modified.
    * @return {@code true} if the two orientations are exactly equal, {@code false} otherwise.
    */
   public boolean equals(Orientation2D other)
   {
      if (other == null)
         return false;
      else
         return yaw == other.yaw;
   }

   /**
    * Tests if the given {@code object}'s class is the same as this, in which case the method
    * returns {@link #equals(Orientation2D)}, it returns {@code false} otherwise.
    *
    * @param object the object to compare against this. Not modified.
    * @return {@code true} if {@code object} and this are exactly equal, {@code false} otherwise.
    */
   @Override
   public boolean equals(Object obj)
   {
      try
      {
         return equals((Orientation2D) obj);
      }
      catch (ClassCastException e)
      {
         return false;
      }
   }

   /**
    * Tests if the yaw angle of this orientation is equal to an {@code epsilon} to the yaw of
    * {@code other}.
    * <p>
    * Note that this method performs number comparison and not an angle comparison, such that:
    * -<i>pi</i> &ne; <i>pi</i>.
    * </p>
    *
    * @param other the query. Not modified.
    * @param epsilon the tolerance to use.
    * @return {@code true} if the two orientations are equal, {@code false} otherwise.
    */
   @Override
   public boolean epsilonEquals(Orientation2D other, double epsilon)
   {
      return Orientation2DBasics.super.epsilonEquals(other, epsilon);
   }

   /**
    * Compares {@code this} to {@code other} to determine if the two orientations are geometrically
    * similar, i.e. the difference in yaw of {@code this} and {@code other} is less than or equal to
    * {@code epsilon}.
    *
    * @param other the orientation to compare to. Not modified.
    * @param epsilon the tolerance of the comparison.
    * @return {@code true} if the two orientations represent the same geometry, {@code false}
    *         otherwise.
    */
   @Override
   public boolean geometricallyEquals(Orientation2D other, double epsilon)
   {
      return Orientation2DBasics.super.geometricallyEquals(other, epsilon);
   }

   /**
    * Provides a {@code String} representation of this orientation 2D as follows:<br>
    * (0.123 )
    *
    * @return the {@code String} representing this orientation 2D.
    */
   @Override
   public String toString()
   {
      return EuclidGeometryIOTools.getOrientation2DString(this);
   }
}
