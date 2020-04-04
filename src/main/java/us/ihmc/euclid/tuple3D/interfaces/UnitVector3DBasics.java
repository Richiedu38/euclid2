package us.ihmc.euclid.tuple3D.interfaces;

/**
 * Write and read interface for 3 dimensional unit-length vector.
 * <p>
 * This unit vector shares the same API as a regular vector 3D while ensuring it is normalized when
 * accessing directly or indirectly its individual components, i.e. when invoking either
 * {@link #getX()}, {@link #getY()}, or {@link #getZ()}.
 * </p>
 * <p>
 * When the values of this vector are set to zero, the next time it is normalized it will be reset
 * to (1.0, 0.0, 0.0).
 * </p>
 * 
 * @author Sylvain Bertrand
 */
public interface UnitVector3DBasics extends UnitVector3DReadOnly, Vector3DBasics
{
   /**
    * {@inheritDoc}
    * <p>
    * Invoking this setter with a new value will result in marking this unit vector as dirty.
    * </p>
    */
   @Override
   void setX(double x);

   /**
    * {@inheritDoc}
    * <p>
    * Invoking this setter with a new value will result in marking this unit vector as dirty.
    * </p>
    */
   @Override
   void setY(double y);

   /**
    * {@inheritDoc}
    * <p>
    * Invoking this setter with a new value will result in marking this unit vector as dirty.
    * </p>
    */
   @Override
   void setZ(double z);

   /** {@inheritDoc} */
   @Override
   default double getX()
   {
      normalize();
      return getRawX();
   }

   /** {@inheritDoc} */
   @Override
   default double getY()
   {
      normalize();
      return getRawY();
   }

   /** {@inheritDoc} */
   @Override
   default double getZ()
   {
      normalize();
      return getRawZ();
   }

   /**
    * Marks this unit vector as dirty.
    * <p>
    * When a unit vector is marked as dirty, the next call to either {@link #getX()}, {@link #getY()},
    * or {@link #getZ()} will perform a normalization of this unit vector.
    * </p>
    */
   public void markAsDirty();

   /** {@inheritDoc} */
   @Override
   void negate();

   /** {@inheritDoc} */
   @Override
   void absolute();

   /** {@inheritDoc} */
   @Override
   void normalize();

   /**
    * Sets this unit vector to {@code other} while keeping track of the dirty flag property.
    * 
    * @param other the other unit vector. Not modified.
    */
   void set(UnitVector3DReadOnly other);

   /**
    * {@inheritDoc}
    * <p>
    * If the argument implements {@link UnitVector3DReadOnly}, a redirection to
    * {@link #set(UnitVector3DReadOnly)} is done.
    * </p>
    */
   @Override
   default void set(Tuple3DReadOnly tupleReadOnly)
   {
      if (tupleReadOnly instanceof UnitVector3DReadOnly)
         set((UnitVector3DReadOnly) tupleReadOnly);
      else
         Vector3DBasics.super.set(tupleReadOnly);
   }

   /**
    * {@inheritDoc}
    * <p>
    * If the argument implements {@link UnitVector3DReadOnly}, a redirection
    * {@link #set(UnitVector3DReadOnly)} is done.
    * </p>
    */
   @Override
   default void setAndNegate(Tuple3DReadOnly tupleReadOnly)
   {
      if (tupleReadOnly instanceof UnitVector3DReadOnly)
      {
         set((UnitVector3DReadOnly) tupleReadOnly);
         negate();
      }
      else
      {
         Vector3DBasics.super.setAndNegate(tupleReadOnly);
      }
   }

   /**
    * {@inheritDoc}
    * <p>
    * If the argument implements {@link UnitVector3DReadOnly}, a redirection
    * {@link #set(UnitVector3DReadOnly)} is done.
    * </p>
    */
   @Override
   default void setAndAbsolute(Tuple3DReadOnly other)
   {
      if (other instanceof UnitVector3DReadOnly)
      {
         set((UnitVector3DReadOnly) other);
         absolute();
      }
      else
      {
         Vector3DBasics.super.setAndAbsolute(other);
      }
   }

   /**
    * This method does nothing with a unit vector.
    */
   @Override
   default void scale(double scalar)
   {
   }

   /**
    * Redirection to {@link #add(Tuple3DReadOnly)} as a unit vector cannot be scaled.
    */
   @Override
   default void scaleAdd(double scalar, Tuple3DReadOnly other)
   {
      Vector3DBasics.super.add(other);
   }

   /**
    * Redirection to {@link #sub(Tuple3DReadOnly)} as a unit vector cannot be scaled.
    */
   @Override
   default void scaleSub(double scalar, Tuple3DReadOnly other)
   {
      Vector3DBasics.super.sub(other);
   }

   /**
    * This method does nothing with a unit vector as a unit vector cannot be scaled.
    * 
    * @return always {@code false};
    */
   @Override
   default boolean clipToMaxLength(double maxLength)
   {
      return false;
   }
}
