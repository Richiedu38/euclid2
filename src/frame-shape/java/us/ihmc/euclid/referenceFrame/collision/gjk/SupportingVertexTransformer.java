package us.ihmc.euclid.referenceFrame.collision.gjk;

import us.ihmc.euclid.shape.collision.interfaces.SupportingVertexHolder;
import us.ihmc.euclid.transform.interfaces.RigidBodyTransformReadOnly;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;

/**
 * Wrapper around a {@link SupportingVertexHolder} for handling queries being from a different
 * frame.
 * <p>
 * This wrapper essentially interpose the a transform between every query and the original
 * supporting vertex holder.
 * </p>
 *
 * @author Sylvain Bertrand
 */
public class SupportingVertexTransformer implements SupportingVertexHolder
{
   private final Vector3D localSupportDirection = new Vector3D();
   private SupportingVertexHolder original;
   private RigidBodyTransformReadOnly transform;

   /**
    * Sets the supporting vertex holder to be wrapped and the transform to use.
    *
    * @param original  the supporting vertex holder to wrap.
    * @param transform the transform to interpose between queries and {@code original}.
    */
   public void initialize(SupportingVertexHolder original, RigidBodyTransformReadOnly transform)
   {
      this.original = original;
      this.transform = transform;
   }

   /** {@inheritDoc} */
   @Override
   public boolean getSupportingVertex(Vector3DReadOnly supportDirection, Point3DBasics supportingVertexToPack)
   {
      transform.inverseTransform(supportDirection, localSupportDirection);
      boolean success = original.getSupportingVertex(localSupportDirection, supportingVertexToPack);
      if (success)
         transform.transform(supportingVertexToPack);
      return success;
   }
}