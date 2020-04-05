package us.ihmc.euclid.shape.primitives;

import us.ihmc.euclid.Axis3D;
import us.ihmc.euclid.interfaces.GeometryObject;
import us.ihmc.euclid.shape.primitives.interfaces.Cylinder3DBasics;
import us.ihmc.euclid.shape.primitives.interfaces.Cylinder3DReadOnly;
import us.ihmc.euclid.shape.tools.EuclidShapeIOTools;
import us.ihmc.euclid.tools.EuclidCoreFactories;
import us.ihmc.euclid.tools.EuclidHashCodeTools;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.UnitVector3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.UnitVector3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;

/**
 * Implementation of a cylinder 3D.
 * <p>
 * A cylinder 3D is represented by its length, its radius, the position of its center, and its axis
 * of revolution.
 * </p>
 *
 * @author Sylvain Bertrand
 */
public class Cylinder3D implements Cylinder3DBasics, GeometryObject<Cylinder3D>
{
   /** Position of this cylinder's center. */
   private final Point3D position = new Point3D();
   /** Axis of revolution of this cylinder. */
   private final UnitVector3D axis = new UnitVector3D(Axis3D.Z);

   /** Radius of the cylinder part. */
   private double radius;
   /**
    * Overall length of the cylinder, i.e. the top face is at {@code 0.5 * length} and the bottom face
    * at {@code - 0.5 * length}.
    */
   private double length;
   /** This cylinder half-length. */
   private double halfLength;

   /** Position of the top cap center linked to this capsule properties. */
   private final Point3DReadOnly topCenter = EuclidCoreFactories.newLinkedPoint3DReadOnly(() -> halfLength * axis.getX() + position.getX(),
                                                                                          () -> halfLength * axis.getY() + position.getY(),
                                                                                          () -> halfLength * axis.getZ() + position.getZ());
   /** Position of the bottom cap center linked to this capsule properties. */
   private final Point3DReadOnly bottomCenter = EuclidCoreFactories.newLinkedPoint3DReadOnly(() -> -halfLength * axis.getX() + position.getX(),
                                                                                             () -> -halfLength * axis.getY() + position.getY(),
                                                                                             () -> -halfLength * axis.getZ() + position.getZ());

   /**
    * Creates a new cylinder with length of {@code 1}, radius of {@code 0.5}, and axis along the
    * z-axis.
    */
   public Cylinder3D()
   {
      this(1.0, 0.5);
   }

   /**
    * Creates a new cylinder 3D which axis is along the z-axis and initializes its length and radius.
    *
    * @param length the length of the cylinder.
    * @param radius the radius of the cylinder.
    * @throws IllegalArgumentException if either {@code length} or {@code radius} is negative.
    */
   public Cylinder3D(double length, double radius)
   {
      setSize(length, radius);
   }

   /**
    * Creates a new cylinder 3D and initializes its pose and size.
    *
    * @param position the position of the center. Not modified.
    * @param axis     the axis of revolution. Not modified.
    * @param length   the length of this cylinder.
    * @param radius   the radius of this cylinder.
    * @throws IllegalArgumentException if {@code length} or {@code radius} is negative.
    */
   public Cylinder3D(Point3DReadOnly position, Vector3DReadOnly axis, double length, double radius)
   {
      set(position, axis, length, radius);
   }

   /**
    * Creates a new cylinder 3D identical to {@code other}.
    *
    * @param other the other cylinder to copy. Not modified.
    */
   public Cylinder3D(Cylinder3DReadOnly other)
   {
      set(other);
   }

   /**
    * Copies the {@code other} cylinder data into {@code this}.
    *
    * @param other the other cylinder to copy. Not modified.
    */
   @Override
   public void set(Cylinder3D other)
   {
      Cylinder3DBasics.super.set(other);
   }

   /**
    * Sets the radius of this cylinder.
    *
    * @param radius the new radius for this cylinder.
    * @throws IllegalArgumentException if {@code radius} is negative.
    */
   @Override
   public void setRadius(double radius)
   {
      if (radius < 0.0)
         throw new IllegalArgumentException("The radius of a Cylinder3D cannot be negative: " + radius);
      this.radius = radius;
   }

   /**
    * Sets the length of this cylinder.
    *
    * @param length the cylinder length along the z-axis.
    * @throws IllegalArgumentException if {@code length} is negative.
    */
   @Override
   public void setLength(double length)
   {
      if (length < 0.0)
         throw new IllegalArgumentException("The length of a Cylinder3D cannot be negative: " + length);
      this.length = length;
      halfLength = 0.5 * length;
   }

   /**
    * Gets the radius of this cylinder.
    *
    * @return the value of the radius.
    */
   @Override
   public double getRadius()
   {
      return radius;
   }

   /**
    * Gets the length of this cylinder.
    *
    * @return the value of the length.
    */
   @Override
   public double getLength()
   {
      return length;
   }

   @Override
   public double getHalfLength()
   {
      return halfLength;
   }

   /** {@inheritDoc} */
   @Override
   public Point3DBasics getPosition()
   {
      return position;
   }

   /** {@inheritDoc} */
   @Override
   public UnitVector3DBasics getAxis()
   {
      return axis;
   }

   /** {@inheritDoc} */
   @Override
   public Point3DReadOnly getTopCenter()
   {
      return topCenter;
   }

   /** {@inheritDoc} */
   @Override
   public Point3DReadOnly getBottomCenter()
   {
      return bottomCenter;
   }

   @Override
   public Cylinder3D copy()
   {
      return new Cylinder3D(this);
   }

   /**
    * Tests on a per component basis if {@code other} and {@code this} are equal to an {@code epsilon}.
    *
    * @param other   the other cylinder to compare against this. Not modified.
    * @param epsilon tolerance to use when comparing each component.
    * @return {@code true} if the two cylinders are equal component-wise, {@code false} otherwise.
    */
   @Override
   public boolean epsilonEquals(Cylinder3D other, double epsilon)
   {
      return Cylinder3DBasics.super.epsilonEquals(other, epsilon);
   }

   /**
    * Compares {@code this} and {@code other} to determine if the two cylinders are geometrically
    * similar.
    *
    * @param other   the cylinder to compare to. Not modified.
    * @param epsilon the tolerance of the comparison.
    * @return {@code true} if the cylinders represent the same geometry, {@code false} otherwise.
    */
   @Override
   public boolean geometricallyEquals(Cylinder3D other, double epsilon)
   {
      return Cylinder3DBasics.super.geometricallyEquals(other, epsilon);
   }

   /**
    * Tests if the given {@code object}'s class is the same as this, in which case the method returns
    * {@link #equals(Cylinder3DReadOnly)}, it returns {@code false} otherwise.
    *
    * @param object the object to compare against this. Not modified.
    * @return {@code true} if {@code object} and this are exactly equal, {@code false} otherwise.
    */
   @Override
   public boolean equals(Object object)
   {
      if (object instanceof Cylinder3DReadOnly)
         return Cylinder3DBasics.super.equals((Cylinder3DReadOnly) object);
      else
         return false;
   }

   /**
    * Calculates and returns a hash code value from the value of each component of this cylinder 3D.
    *
    * @return the hash code value for this cylinder 3D.
    */
   @Override
   public int hashCode()
   {
      long hash = 1L;
      hash = EuclidHashCodeTools.toLongHashCode(length, radius);
      hash = EuclidHashCodeTools.combineHashCode(hash, EuclidHashCodeTools.toLongHashCode(position, axis));
      return EuclidHashCodeTools.toIntHashCode(hash);
   }

   /**
    * Provides a {@code String} representation of this cylinder 3D as follows:<br>
    * Cylinder 3D: [position: (-0.362, -0.617, 0.066 ), axis: ( 0.634, -0.551, -0.543 ), length: 0.170,
    * radius: 0.906]
    *
    * @return the {@code String} representing this cylinder 3D.
    */
   @Override
   public String toString()
   {
      return EuclidShapeIOTools.getCylinder3DString(this);
   }
}
