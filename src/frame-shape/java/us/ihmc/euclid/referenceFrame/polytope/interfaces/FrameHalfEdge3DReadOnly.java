package us.ihmc.euclid.referenceFrame.polytope.interfaces;

import us.ihmc.euclid.referenceFrame.exceptions.ReferenceFrameMismatchException;
import us.ihmc.euclid.referenceFrame.interfaces.FrameLineSegment3DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint3DReadOnly;
import us.ihmc.euclid.shape.convexPolytope.interfaces.HalfEdge3DReadOnly;

/**
 * Read-only interface for a half-edge 3D that belongs to a convex polytope 3D expressed in a given
 * reference frame.
 * <p>
 * This is part of a Doubly Connected Edge List data structure
 * <a href="https://en.wikipedia.org/wiki/Doubly_connected_edge_list"> link</a>.
 * </p>
 *
 * @author Sylvain Bertrand
 */
public interface FrameHalfEdge3DReadOnly extends HalfEdge3DReadOnly, FrameLineSegment3DReadOnly
{
   /** {@inheritDoc} */
   @Override
   FrameVertex3DReadOnly getOrigin();

   /** {@inheritDoc} */
   @Override
   FrameVertex3DReadOnly getDestination();

   /** {@inheritDoc} */
   @Override
   default FramePoint3DReadOnly getFirstEndpoint()
   {
      return getOrigin();
   }

   /** {@inheritDoc} */
   @Override
   default FramePoint3DReadOnly getSecondEndpoint()
   {
      return getDestination();
   }

   /** {@inheritDoc} */
   @Override
   FrameHalfEdge3DReadOnly getTwin();

   /** {@inheritDoc} */
   @Override
   FrameHalfEdge3DReadOnly getNext();

   /** {@inheritDoc} */
   @Override
   FrameHalfEdge3DReadOnly getPrevious();

   /**
    * Computes the minimum distance between a given point and the infinitely long line supporting this
    * half-edge.
    *
    * @param point the location of the query. Not modified.
    * @return the distance from the query to the support line.
    * @throws ReferenceFrameMismatchException if the argument is not expressed in the same reference
    *                                         frame as {@code this}.
    */
   default double distanceFromSupportLine(FramePoint3DReadOnly point)
   {
      checkReferenceFrameMatch(point);
      return HalfEdge3DReadOnly.super.distanceFromSupportLine(point);
   }

   /**
    * Tests on a per component basis, if this half-edge 3D is exactly equal to {@code other}.
    * <p>
    * If the two half-edges have different frames, this method returns {@code false}.
    * </p>
    *
    * @param other the other half-edge 3D to compare against this. Not modified.
    * @return {@code true} if the two half-edges are exactly equal component-wise and are expressed in
    *         the same reference frame, {@code false} otherwise.
    */
   default boolean equals(FrameHalfEdge3DReadOnly other)
   {
      if (other == this)
         return true;
      else if (other == null)
         return false;
      else if (getOrigin() == null != (other.getOrigin() == null))
         return false;
      else if (getDestination() == null != (other.getDestination() == null))
         return false;
      else
         return FrameLineSegment3DReadOnly.super.equals(other);
   }
}
