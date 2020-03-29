package us.ihmc.euclid.referenceFrame;

import static us.ihmc.euclid.EuclidTestConstants.ITERATIONS;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import us.ihmc.euclid.EuclidTestConstants;
import us.ihmc.euclid.geometry.Pose2D;
import us.ihmc.euclid.geometry.interfaces.Pose2DReadOnly;
import us.ihmc.euclid.geometry.tools.EuclidGeometryRandomTools;
import us.ihmc.euclid.geometry.tools.EuclidGeometryTestTools;
import us.ihmc.euclid.referenceFrame.api.EuclidFrameAPIDefaultConfiguration;
import us.ihmc.euclid.referenceFrame.api.EuclidFrameAPITester;
import us.ihmc.euclid.referenceFrame.api.FrameTypeCopier;
import us.ihmc.euclid.referenceFrame.api.MethodSignature;
import us.ihmc.euclid.referenceFrame.api.RandomFramelessTypeBuilder;
import us.ihmc.euclid.referenceFrame.interfaces.FixedFramePose2DBasics;
import us.ihmc.euclid.referenceFrame.interfaces.FramePose2DReadOnly;
import us.ihmc.euclid.referenceFrame.tools.EuclidFrameRandomTools;

public class FramePose2DTest extends FramePose2DReadOnlyTest<FramePose2D>
{
   public static final double EPSILON = 1.0e-15;

   @Override
   public FramePose2D createFramePose(ReferenceFrame referenceFrame, Pose2DReadOnly pose)
   {
      return new FramePose2D(referenceFrame, pose);
   }

   @Test
   public void testConsistencyWithPose2D()
   {
      FrameTypeCopier frameTypeBuilder = (frame, pose) -> createFramePose(frame, (Pose2DReadOnly) pose);
      RandomFramelessTypeBuilder framelessTypeBuilder = EuclidGeometryRandomTools::nextPose2D;
      Predicate<Method> methodFilter = m -> !m.getName().equals("hashCode") && !m.getName().equals("epsilonEquals");
      EuclidFrameAPITester tester = new EuclidFrameAPITester(new EuclidFrameAPIDefaultConfiguration());
      tester.assertFrameMethodsOfFrameHolderPreserveFunctionality(frameTypeBuilder,
                                                                  framelessTypeBuilder,
                                                                  methodFilter,
                                                                  EuclidTestConstants.API_FUNCTIONALITY_TEST_ITERATIONS);
   }

   @Override
   @Test
   public void testOverloading() throws Exception
   {
      super.testOverloading();
      List<MethodSignature> signaturesToIgnore = new ArrayList<>();
      signaturesToIgnore.add(new MethodSignature("set", Pose2D.class));
      signaturesToIgnore.add(new MethodSignature("equals", Pose2D.class));
      signaturesToIgnore.add(new MethodSignature("epsilonEquals", Pose2D.class, Double.TYPE));
      signaturesToIgnore.add(new MethodSignature("geometricallyEquals", Pose2D.class, Double.TYPE));
      Predicate<Method> methodFilter = EuclidFrameAPITester.methodFilterFromSignature(signaturesToIgnore);

      EuclidFrameAPITester tester = new EuclidFrameAPITester(new EuclidFrameAPIDefaultConfiguration());
      tester.assertOverloadingWithFrameObjects(FramePose2D.class, Pose2D.class, true, 1, methodFilter);
   }

   @Test
   public void testSetMatchingFrame() throws Exception
   {
      EuclidFrameAPITester tester = new EuclidFrameAPITester(new EuclidFrameAPIDefaultConfiguration());
      tester.assertSetMatchingFramePreserveFunctionality(EuclidFrameRandomTools::nextFramePose2D, EuclidTestConstants.API_FUNCTIONALITY_TEST_ITERATIONS);

      Random random = new Random(544354);

      for (int i = 0; i < ITERATIONS; i++)
      {
         ReferenceFrame sourceFrame = EuclidFrameRandomTools.nextReferenceFrame(random, true);
         ReferenceFrame destinationFrame = EuclidFrameRandomTools.nextReferenceFrame(random, true);

         FramePose2DReadOnly source = EuclidFrameRandomTools.nextFramePose2D(random, sourceFrame);
         FixedFramePose2DBasics actual = EuclidFrameRandomTools.nextFramePose2D(random, destinationFrame);

         actual.setMatchingFrame(source);

         FramePose2D expected = new FramePose2D(source);
         expected.changeFrame(destinationFrame);

         EuclidGeometryTestTools.assertPose2DEquals(expected, actual, EPSILON);
      }
   }

   @Test
   public void testSetIncludingFrame()
   {
      EuclidFrameAPITester tester = new EuclidFrameAPITester(new EuclidFrameAPIDefaultConfiguration());
      tester.assertSetIncludingFramePreserveFunctionality(EuclidFrameRandomTools::nextFramePoint2D, EuclidTestConstants.API_FUNCTIONALITY_TEST_ITERATIONS);
   }
}
