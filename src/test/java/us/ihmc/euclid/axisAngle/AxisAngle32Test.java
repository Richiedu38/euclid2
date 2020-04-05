package us.ihmc.euclid.axisAngle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static us.ihmc.euclid.EuclidTestConstants.ITERATIONS;

import java.util.Random;

import org.junit.jupiter.api.Test;

import us.ihmc.euclid.axisAngle.interfaces.AxisAngleReadOnly;
import us.ihmc.euclid.matrix.RotationMatrix;
import us.ihmc.euclid.rotationConversion.AxisAngleConversion;
import us.ihmc.euclid.tools.EuclidCoreRandomTools;
import us.ihmc.euclid.tools.EuclidCoreTestTools;
import us.ihmc.euclid.tuple3D.UnitVector3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.euclid.yawPitchRoll.YawPitchRoll;

public class AxisAngle32Test extends AxisAngleBasicsTest<AxisAngle32>
{
   public static final double EPS = 1e-6;

   @Test
   public void testAxisAngle32()
   {
      Random random = new Random(613615L);
      AxisAngle32 axisAngle = new AxisAngle32(), expected;
      { // Test AxisAngle32()
         assertTrue(axisAngle.getX() == 1.0);
         assertTrue(axisAngle.getY() == 0.0);
         assertTrue(axisAngle.getZ() == 0.0);
         assertTrue(axisAngle.getAngle() == 0.0);
      }

      { // Test AxisAngle32(AxisAngleBasics other)
         for (int i = 0; i < ITERATIONS; i++)
         {
            axisAngle = expected = EuclidCoreRandomTools.nextAxisAngle32(random);

            AxisAngle32 axisAngle2 = new AxisAngle32(axisAngle);

            EuclidCoreTestTools.assertAxisAngleEquals(axisAngle, axisAngle2, EPS);
            EuclidCoreTestTools.assertAxisAngleEquals(axisAngle, expected, EPS);
         }
      }

      { // Test AxisAngle32(float x, float y, float z, float angle)
         for (int i = 0; i < ITERATIONS; i++)
         {
            expected = EuclidCoreRandomTools.nextAxisAngle32(random);
            axisAngle = new AxisAngle32(expected.getX32(), expected.getY32(), expected.getZ32(), expected.getAngle32());

            EuclidCoreTestTools.assertAxisAngleEquals(axisAngle, expected, EPS);
         }
      }

      { // Test AxisAngle32(float[] axisAngleArray)
         for (int i = 0; i < ITERATIONS; i++)
         {
            expected = EuclidCoreRandomTools.nextAxisAngle32(random);
            float[] axisAngleArray;
            float[] axisAngleArrayCopy;
            axisAngleArray = axisAngleArrayCopy = new float[] {expected.getX32(), expected.getY32(), expected.getZ32(), expected.getAngle32()};

            axisAngle = new AxisAngle32(axisAngleArray);

            EuclidCoreTestTools.assertAxisAngleEquals(axisAngle, expected, EPS);

            for (int j = 0; j < axisAngleArray.length; j++)
               assertTrue(axisAngleArray[j] == axisAngleArrayCopy[j]);
         }
      }

      { // Test AxisAngle32(VectorBasics axis, float angle)
         for (int i = 0; i < ITERATIONS; i++)
         {
            UnitVector3D vectorAxis = EuclidCoreRandomTools.nextUnitVector3D(random);

            float angle = random.nextFloat();

            axisAngle = new AxisAngle32(vectorAxis, angle);

            assertEquals(axisAngle.getX(), vectorAxis.getX(), EPS);
            assertEquals(axisAngle.getY(), vectorAxis.getY(), EPS);
            assertEquals(axisAngle.getZ(), vectorAxis.getZ(), EPS);
            assertEquals(axisAngle.getAngle(), angle, EPS);
         }
      }

      { // Test AxisAngle32(QuaternionBasics quaternion)
         for (int i = 0; i < ITERATIONS; i++)
         {
            Quaternion quaternion, quaternionCopy;
            quaternion = quaternionCopy = EuclidCoreRandomTools.nextQuaternion(random);

            axisAngle = new AxisAngle32(quaternion);
            AxisAngle32 expectedAxisAngle32 = new AxisAngle32();
            AxisAngleConversion.convertQuaternionToAxisAngle(quaternion, expectedAxisAngle32);

            EuclidCoreTestTools.assertAxisAngleEquals(axisAngle, expectedAxisAngle32, EPS);
            EuclidCoreTestTools.assertQuaternionEquals(quaternion, quaternionCopy, EPS);
         }
      }

      { // Test AxisAngle32(RotationMatrix rotationMatrix)
         for (int i = 0; i < ITERATIONS; i++)
         {
            RotationMatrix matrix, matrixCopy;
            matrix = matrixCopy = EuclidCoreRandomTools.nextRotationMatrix(random);

            float angle;
            float angleCopy;
            angle = angleCopy = random.nextFloat();

            axisAngle = new AxisAngle32(matrix);
            AxisAngle32 expectedAxisAngle32 = new AxisAngle32();
            AxisAngleConversion.convertMatrixToAxisAngle(matrix, expectedAxisAngle32);

            EuclidCoreTestTools.assertAxisAngleEquals(axisAngle, expectedAxisAngle32, EPS);

            assertTrue(angle == angleCopy);

            EuclidCoreTestTools.assertMatrix3DEquals(matrix, matrixCopy, EPS);
         }
      }

      { // Test AxisAngle32(VectorBasics rotationVector)
         for (int i = 0; i < ITERATIONS; i++)
         {
            Vector3D rotationVector, rotationVectorCopy;
            rotationVector = rotationVectorCopy = EuclidCoreRandomTools.nextRotationVector(random);

            axisAngle = new AxisAngle32(rotationVector);
            AxisAngle32 expectedAxisAngle32 = new AxisAngle32();
            AxisAngleConversion.convertRotationVectorToAxisAngle(rotationVector, expectedAxisAngle32);

            EuclidCoreTestTools.assertAxisAngleEquals(axisAngle, expectedAxisAngle32, EPS);
            EuclidCoreTestTools.assertRotationVectorGeometricallyEquals(rotationVector, rotationVectorCopy, EPS);
         }
      }

      { // Test AxisAngle(double yaw, double pitch, double roll)
         for (int i = 0; i < ITERATIONS; i++)
         {
            YawPitchRoll yawPitchRoll = EuclidCoreRandomTools.nextYawPitchRoll(random);

            axisAngle = new AxisAngle32(yawPitchRoll.getYaw(), yawPitchRoll.getPitch(), yawPitchRoll.getRoll());
            AxisAngle32 expectedAxisAngle = new AxisAngle32();
            AxisAngleConversion.convertYawPitchRollToAxisAngle(yawPitchRoll.getYaw(), yawPitchRoll.getPitch(), yawPitchRoll.getRoll(), expectedAxisAngle);

            EuclidCoreTestTools.assertAxisAngleEquals(axisAngle, expectedAxisAngle, EPS);
         }
      }
   }

   @Test
   public void testHashCode() throws Exception
   {
      Random random = new Random(621541L);
      AxisAngle32 axisAngle = EuclidCoreRandomTools.nextAxisAngle32(random);

      int newHashCode, previousHashCode;
      newHashCode = axisAngle.hashCode();
      assertEquals(newHashCode, axisAngle.hashCode());

      previousHashCode = axisAngle.hashCode();

      for (int i = 0; i < ITERATIONS; i++)
      {
         axisAngle.setElement(random.nextInt(4), random.nextFloat());
         newHashCode = axisAngle.hashCode();
         assertNotEquals(newHashCode, previousHashCode);
         previousHashCode = newHashCode;
      }
   }

   @Override
   @Test
   public void testGeometricallyEquals() throws Exception
   {
      super.testGeometricallyEquals();

      Random random = new Random(35454L);

      AxisAngle32 aabA;
      AxisAngle32 aabB;

      for (int i = 0; i < ITERATIONS; ++i)
      {
         double epsilon = random.nextDouble();
         aabA = EuclidCoreRandomTools.nextAxisAngle32(random);
         AxisAngle aa = new AxisAngle(EuclidCoreRandomTools.nextVector3DWithFixedLength(random, 1.0), epsilon + random.nextDouble() - random.nextDouble());

         aabB = new AxisAngle32(aa);
         aabB.preMultiply(aabA);

         if (((AxisAngleReadOnly) aabA).geometricallyEquals(aabB, epsilon))
         {
            assertTrue(aabA.geometricallyEquals(aabB, epsilon));
         }
         else
         {
            assertFalse(aabA.geometricallyEquals(aabB, epsilon));
         }
      }
   }

   @Override
   public AxisAngle32 createEmptyAxisAngle()
   {
      return new AxisAngle32();
   }

   @Override
   public AxisAngle32 createAxisAngle(Vector3DReadOnly axis, double angle)
   {
      return new AxisAngle32(axis, (float) angle);
   }

   @Override
   public AxisAngle32 createAxisAngle(double ux, double uy, double uz, double angle)
   {
      return new AxisAngle32((float) ux, (float) uy, (float) uz, (float) angle);
   }

   @Override
   public AxisAngle32 createRandomAxisAngle(Random random)
   {
      return EuclidCoreRandomTools.nextAxisAngle32(random);
   }

   @Override
   public double getEpsilon()
   {
      return EPS;
   }

   @Override
   public double getSmallestEpsilon()
   {
      return 1.0e-6;
   }
}
