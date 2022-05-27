package us.ihmc.euclid.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static us.ihmc.euclid.EuclidTestConstants.ITERATIONS;

import java.util.Random;

import org.ejml.data.DMatrix;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.junit.jupiter.api.Test;

import us.ihmc.euclid.axisAngle.AxisAngle;
import us.ihmc.euclid.matrix.Matrix3D;
import us.ihmc.euclid.matrix.RotationMatrix;
import us.ihmc.euclid.orientation.interfaces.Orientation3DBasics;
import us.ihmc.euclid.tools.EuclidCoreRandomTools;
import us.ihmc.euclid.tools.EuclidCoreTestTools;
import us.ihmc.euclid.transform.interfaces.RigidBodyTransformBasics;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DReadOnly;
import us.ihmc.euclid.tuple4D.Quaternion;

public abstract class RigidBodyTransformBasicsTest<T extends RigidBodyTransformBasics> extends TransformTest<T>
{
   private static final double EPS = 1.0e-14;

   @Override
   public abstract T createRandomTransform(Random random);

   @Override
   public abstract T createRandomTransform2D(Random random);

   public abstract T copy(T original);

   public abstract T identity();

   @Test
   public void testMultiply() throws Exception
   {
      Random random = new Random(465416L);

      // Test against invert
      for (int i = 0; i < ITERATIONS; i++)
      {
         T transform = createRandomTransform(random);
         T inverse = copy(transform);
         inverse.invert();

         assertTrue(transform.hasRotation());
         assertTrue(transform.hasTranslation());
         transform.multiply(inverse);
         assertFalse(transform.hasRotation());
         assertFalse(transform.hasTranslation());

         EuclidCoreTestTools.assertRigidBodyTransformGeometricallyEquals(transform, identity(), EPS);
      }

      // Test against EJML
      for (int i = 0; i < ITERATIONS; i++)
      {
         T t1 = createRandomTransform(random);
         T t2 = createRandomTransform(random);
         checkMultiplyAgainstEJML(t1, t2);
      }

      
      // Try different combinations with/without translation/rotation
      for (int i = 0; i < ITERATIONS; i++)
      {  
         // t1: random rotation + random translation
         // t2: zero   rotation + random translation
         T t1 = createRandomTransform(random);
         T t2 = createRandomTransform(random);
         t2.setRotationToZero();
         
         System.out.println("t1 = " + t1.toString() +"\nt2 = " + t2.toString());
         
         checkMultiplyAgainstEJML(t1, t2);
         assertTrue(t1.hasRotation());
         assertTrue(t1.hasTranslation());
         t1.multiply(t2);
         assertTrue(t1.hasRotation());
         assertTrue(t1.hasTranslation());
         
         // t1: zero   rotation + random translation
         // t2: random rotation + random translation
         t1 = createRandomTransform(random);
         t2 = createRandomTransform(random);
         t1.setRotationToZero();         
         
         System.out.println("t1 = " + t1.toString() +"\nt2 = " + t2.toString());
         
         checkMultiplyAgainstEJML(t1, t2);
         assertFalse(t1.hasRotation());
         assertTrue(t1.hasTranslation());
         t1.multiply(t2);
         assertTrue(t1.hasRotation());
         assertTrue(t1.hasTranslation());

         // t1: random rotation + random translation
         // t2: random rotation + zero   translation
         t1 = createRandomTransform(random);
         t2 = createRandomTransform(random);
         t2.setTranslationToZero();
         
         System.out.println("t1 = " + t1.toString() +"\nt2 = " + t2.toString());
         
         checkMultiplyAgainstEJML(t1, t2);
         assertTrue(t1.hasRotation());
         assertTrue(t1.hasTranslation());
         t1.multiply(t2);
         assertTrue(t1.hasRotation());
         assertTrue(t1.hasTranslation());

         // t1: random rotation + zero   translation
         // t2: random rotation + random translation
         t1 = createRandomTransform(random);
         t1.setTranslationToZero();
         t2 = createRandomTransform(random);
         
         checkMultiplyAgainstEJML(t1, t2);
         assertTrue(t1.hasRotation());
         assertFalse(t1.hasTranslation());
         t1.multiply(t2);
         assertTrue(t1.hasRotation());
         assertTrue(t1.hasTranslation());
         
         // t1: zero rotation + random translation
         // t2: zero rotation + random translation
         t1 = createRandomTransform(random);
         t2 = createRandomTransform(random);
         t1.setRotationToZero();
         t2.setRotationToZero();
         checkMultiplyAgainstEJML(t1, t2);
         assertFalse(t1.hasRotation());
         assertTrue(t1.hasTranslation());
         t1.multiply(t2);
         assertFalse(t1.hasRotation());
         assertTrue(t1.hasTranslation());

         // t1: random rotation + zero translation
         // t2: random rotation + zero translation
         t1 = createRandomTransform(random);
         t2 = createRandomTransform(random);
         
         System.out.println("t1 = " + t1.toString() +"\nt2 = " + t2.toString());
         t1.setTranslationToZero();
         t2.setTranslationToZero();
         checkMultiplyAgainstEJML(t1, t2);
         assertTrue(t1.hasRotation());
         assertFalse(t1.hasTranslation());
         t1.multiply(t2);
         assertTrue(t1.hasRotation());
         assertFalse(t1.hasTranslation());

         // t1: random rotation + random translation
         // t2: random rotation + random translation
         t1 = createRandomTransform(random);
         t2 = createRandomTransform(random);
         
         System.out.println("t1 = " + t1.toString() +"\nt2 = " + t2.toString());
         t2.getRotation().set(t1.getRotation());
         t2.invertRotation();
         checkMultiplyAgainstEJML(t1, t2);
         assertTrue(t1.hasRotation());
         assertTrue(t1.hasTranslation());
         t1.multiply(t2);
         assertFalse(t1.hasRotation());
         assertTrue(t1.hasTranslation());

         
         // t1: random rotation + random translation
         // t2: random rotation + random translation
         t1 = createRandomTransform(random);
         t2 = createRandomTransform(random);         
         Vector3D negateTranslation = new Vector3D(t1.getTranslation());
         negateTranslation.negate();
         t1.inverseTransform(negateTranslation);
         t2.getTranslation().set(negateTranslation);
         checkMultiplyAgainstEJML(t1, t2);
         assertTrue(t1.hasRotation());
         assertTrue(t1.hasTranslation());
         t1.multiply(t2);
         assertTrue(t1.hasRotation());
         assertFalse(t1.hasTranslation());
      }
   }
   
   @Test // moved from non-basic
   public void testResetRotation() throws Exception
   {
      Random random = new Random(42353L);
      T original = createRandomTransform(random);
      T transform = copy(original);

      assertTrue(transform.hasRotation());
      transform.setRotationToZero();
      assertFalse(transform.hasRotation());

      assertTrue(transform.getTranslation().equals(original.getTranslation()));
      assertTrue(transform.getRotation().equals(identity().getRotation()));
   }
   
   @Test // moved from non-basic
   public void testResetTranslation() throws Exception
   {
      Random random = new Random(42353L);
      T original = createRandomTransform(random);
      T transform = copy(original);

      assertTrue(transform.hasTranslation());
      transform.setTranslationToZero();
      assertFalse(transform.hasTranslation());

      assertTrue(transform.getTranslationX() == 0.0);
      assertTrue(transform.getTranslationY() == 0.0);
      assertTrue(transform.getTranslationZ() == 0.0);
      assertTrue(transform.getRotation().equals(original.getRotation()));      
   }
   
   @Test // moved from non-basic
   public void testNormalizeRotationPart() throws Exception
   {
      Random random = new Random(42353L);
      T original = createRandomTransform(random);
      T transform = copy(original);

      double corruptionFactor = 0.1;
      
      RotationMatrix rotationPart = new RotationMatrix();
//      original.get(rotationPart, null);
      original.getRotation().get(rotationPart);
      double m00 = rotationPart.getM00() + corruptionFactor * random.nextDouble();
      double m01 = rotationPart.getM01() + corruptionFactor * random.nextDouble();
      double m02 = rotationPart.getM02() + corruptionFactor * random.nextDouble();
      double m10 = rotationPart.getM10() + corruptionFactor * random.nextDouble();
      double m11 = rotationPart.getM11() + corruptionFactor * random.nextDouble();
      double m12 = rotationPart.getM12() + corruptionFactor * random.nextDouble();
      double m20 = rotationPart.getM20() + corruptionFactor * random.nextDouble();
      double m21 = rotationPart.getM21() + corruptionFactor * random.nextDouble();
      double m22 = rotationPart.getM22() + corruptionFactor * random.nextDouble();
      
      transform.getRotation().setRotationMatrix(m00, m01, m02, m10, m11, m12, m20, m21, m22);
      transform.normalizeRotationPart();

      Matrix3D rotation = new Matrix3D();
      Vector3D vector1 = new Vector3D();
      Vector3D vector2 = new Vector3D();

      rotation.set(transform.getRotation());

      // Test that each row & column vectors are unit-length
      for (int j = 0; j < 3; j++)
      {
         rotation.getRow(j, vector1);
         assertEquals(1.0, vector1.length(), EPS);

         rotation.getColumn(j, vector1);
         assertEquals(1.0, vector1.length(), EPS);
      }

      // Test that each pair of rows and each pair of columns are orthogonal
      for (int j = 0; j < 3; j++)
      {
         rotation.getRow(j, vector1);
         rotation.getRow((j + 1) % 3, vector2);
         assertEquals(0.0, vector1.dot(vector2), EPS);

         rotation.getColumn(j, vector1);
         rotation.getColumn((j + 1) % 3, vector2);
         assertEquals(0.0, vector1.dot(vector2), EPS);
      }

      corruptionFactor = 0.9e-12;
      m00 = 1.0 + EuclidCoreRandomTools.nextDouble(random, corruptionFactor);
      m01 = 0.0 + EuclidCoreRandomTools.nextDouble(random, corruptionFactor);
      m02 = 0.0 + EuclidCoreRandomTools.nextDouble(random, corruptionFactor);
      m10 = 0.0 + EuclidCoreRandomTools.nextDouble(random, corruptionFactor);
      m11 = 1.0 + EuclidCoreRandomTools.nextDouble(random, corruptionFactor);
      m12 = 0.0 + EuclidCoreRandomTools.nextDouble(random, corruptionFactor);
      m20 = 0.0 + EuclidCoreRandomTools.nextDouble(random, corruptionFactor);
      m21 = 0.0 + EuclidCoreRandomTools.nextDouble(random, corruptionFactor);
      m22 = 1.0 + EuclidCoreRandomTools.nextDouble(random, corruptionFactor);
      transform.getRotation().setRotationMatrix(m00, m01, m02, m10, m11, m12, m20, m21, m22);
      assertFalse(transform.hasRotation());
      
      // TODO: Should really compare using 'equal', but for now can't find a way.
      assertTrue(transform.getRotation().geometricallyEquals(identity().getRotation(), corruptionFactor));
      
      transform.normalizeRotationPart();
      assertTrue(transform.getRotation().equals(identity().getRotation()));
   }
   
   @Test // moved from non-basic
   public void testSetToZero() throws Exception
   {
      Random random = new Random(2342L);
      T transform = createRandomTransform(random);
      assertTrue(transform.hasRotation());
      assertTrue(transform.hasTranslation());
      
      assertFalse(transform.equals(identity()));

      transform.setToZero();
      assertFalse(transform.hasRotation());
      assertFalse(transform.hasTranslation());
      
      assertTrue(transform.equals(identity()));
   }
   
   @Test // moved from non-basic
   public void testSetToNaN() throws Exception
   {
      Random random = new Random(2342L);
      T transform = createRandomTransform(random);
      assertTrue(transform.hasRotation());
      assertTrue(transform.hasTranslation());
      
      assertFalse(transform.containsNaN());
      
      transform.setToNaN();
      assertTrue(transform.hasRotation());
      assertTrue(transform.hasTranslation());
      
      // TODO: What it actually needs to check -> all the elements are NaN
      assertTrue(transform.getRotation().containsNaN());
      assertTrue(transform.getTranslation().containsNaN());
   }
   
   @Test // moved from non-basic
   public void testContainsNaN() throws Exception
   {
      Random random = new Random(143234);
      T transform = createRandomTransform(random);
      transform.getRotation().setRotationMatrix(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
      transform.getTranslation().set(0.0, 0.0, 0.0);
      assertFalse(transform.containsNaN());
      
      transform.getRotation().setRotationMatrix(Double.NaN, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
      assertTrue(transform.containsNaN());
      
      transform.getRotation().setRotationMatrix(0.0, Double.NaN, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
      assertTrue(transform.containsNaN());
      
      transform.getRotation().setRotationMatrix(0.0, 0.0, Double.NaN, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
      assertTrue(transform.containsNaN());
      
      transform.getRotation().setRotationMatrix(0.0, 0.0, 0.0, Double.NaN, 0.0, 0.0, 0.0, 0.0, 0.0);
      assertTrue(transform.containsNaN());
      
      transform.getRotation().setRotationMatrix(0.0, 0.0, 0.0, 0.0, Double.NaN, 0.0, 0.0, 0.0, 0.0);
      assertTrue(transform.containsNaN());
      
      transform.getRotation().setRotationMatrix(0.0, 0.0, 0.0, 0.0, 0.0, Double.NaN, 0.0, 0.0, 0.0);
      assertTrue(transform.containsNaN());
      
      transform.getRotation().setRotationMatrix(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.NaN, 0.0, 0.0);
      assertTrue(transform.containsNaN());
      
      transform.getRotation().setRotationMatrix(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.NaN, 0.0);
      assertTrue(transform.containsNaN());
      
      transform.getRotation().setRotationMatrix(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.NaN);
      assertTrue(transform.containsNaN());
      
      transform.getRotation().setRotationMatrix(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
      transform.getTranslation().setX(Double.NaN);
      transform.getTranslation().setY(0.0);
      transform.getTranslation().setZ(0.0);
      assertTrue(transform.containsNaN());
      
      transform.getTranslation().setX(0.0);
      transform.getTranslation().setY(Double.NaN);
      transform.getTranslation().setZ(0.0);
      assertTrue(transform.containsNaN());
      
      transform.getTranslation().setX(0.0);
      transform.getTranslation().setY(0.0);
      transform.getTranslation().setZ(Double.NaN);
      assertTrue(transform.containsNaN());
   }
   
   @Test // moved from non-basic
   public void testAppendOrientation() throws Exception
   {
      Random random = new Random(46575);

      for (int i = 0; i < ITERATIONS; i++)
      {
         T original = createRandomTransform(random);
         Orientation3DBasics orientation = EuclidCoreRandomTools.nextOrientation3D(random);
         T orientationTransform = createRandomTransform(random);
         orientationTransform.getRotation().set(orientation);
         orientationTransform.getTranslation().set(0.0,0.0,0.0);
         
         T expected = copy(original);
         expected.multiply(orientationTransform);
         
         T actual = copy(original);
         actual.appendOrientation(orientation);
         
         // TODO: Can't find a way to give tolerance (eps) to the condition.
         assertTrue(expected.equals(actual));
         
      }
   }
   
   @Test // moved from non-basic
   public void testAppendYawPitchRoll() throws Exception
   {
      Random random = new Random(35454L);

      RigidBodyTransform expected = new RigidBodyTransform();
      RigidBodyTransform actual = new RigidBodyTransform();

      for (int i = 0; i < ITERATIONS; i++)
      { // appendYawRotation(double yaw)
         RotationMatrix expectedRotation = new RotationMatrix();

         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         double yaw = EuclidCoreRandomTools.nextDouble(random, Math.PI);
         expectedRotation.set(original.getRotation());
         expectedRotation.appendYawRotation(yaw);
         expected.set(expectedRotation, original.getTranslation());
         assertTrue(expected.hasRotation());

         actual.set(original);
         assertTrue(actual.hasRotation());
         actual.appendYawRotation(yaw);
         assertTrue(actual.hasRotation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         original.setToZero();
         yaw = EuclidCoreRandomTools.nextDouble(random, Math.PI);
         actual.set(original);
         assertFalse(actual.hasRotation());
         actual.appendYawRotation(yaw);
         assertTrue(actual.hasRotation());

         original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         yaw = 0.0;
         actual.set(original);
         assertTrue(actual.hasRotation());
         actual.appendYawRotation(yaw);
         assertTrue(actual.hasRotation());

         yaw = EuclidCoreRandomTools.nextDouble(random, Math.PI);
         original.getRotation().setToYawOrientation(-yaw);
         actual.set(original);
         assertTrue(actual.hasRotation());
         actual.appendYawRotation(yaw);
         assertFalse(actual.hasRotation());
      }

      for (int i = 0; i < ITERATIONS; i++)
      { // appendPitchRotation(double pitch)
         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RotationMatrix expectedRotation = new RotationMatrix(original.getRotation());
         double pitch = EuclidCoreRandomTools.nextDouble(random, Math.PI);
         expectedRotation.appendPitchRotation(pitch);
         expected.set(expectedRotation, original.getTranslation());
         assertTrue(expected.hasRotation());

         actual.set(original);
         assertTrue(actual.hasRotation());
         actual.appendPitchRotation(pitch);
         assertTrue(actual.hasRotation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         original.setToZero();
         pitch = EuclidCoreRandomTools.nextDouble(random, Math.PI);
         actual.set(original);
         assertFalse(actual.hasRotation());
         actual.appendPitchRotation(pitch);
         assertTrue(actual.hasRotation());

         original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         pitch = 0.0;
         actual.set(original);
         assertTrue(actual.hasRotation());
         actual.appendPitchRotation(pitch);
         assertTrue(actual.hasRotation());

         pitch = EuclidCoreRandomTools.nextDouble(random, Math.PI);
         original.getRotation().setToPitchOrientation(-pitch);
         actual.set(original);
         assertTrue(actual.hasRotation());
         actual.appendPitchRotation(pitch);
         assertFalse(actual.hasRotation());
      }

      for (int i = 0; i < ITERATIONS; i++)
      { // appendRollRotation(double roll)
         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RotationMatrix expectedRotation = new RotationMatrix(original.getRotation());
         double roll = EuclidCoreRandomTools.nextDouble(random, Math.PI);
         expectedRotation.appendRollRotation(roll);
         expected.set(expectedRotation, original.getTranslation());
         assertTrue(expected.hasRotation());

         actual.set(original);
         assertTrue(actual.hasRotation());
         actual.appendRollRotation(roll);
         assertTrue(actual.hasRotation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         original.setToZero();
         roll = EuclidCoreRandomTools.nextDouble(random, Math.PI);
         actual.set(original);
         assertFalse(actual.hasRotation());
         actual.appendRollRotation(roll);
         assertTrue(actual.hasRotation());

         original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         roll = 0.0;
         actual.set(original);
         assertTrue(actual.hasRotation());
         actual.appendRollRotation(roll);
         assertTrue(actual.hasRotation());

         roll = EuclidCoreRandomTools.nextDouble(random, Math.PI);
         original.getRotation().setToRollOrientation(-roll);
         actual.set(original);
         assertTrue(actual.hasRotation());
         actual.appendRollRotation(roll);
         assertFalse(actual.hasRotation());
      }
   }
   
   @Test // moved from non-basic
   public void testPrependTranslation() throws Exception
   {
      Random random = new Random(35454L);

      RigidBodyTransform expected = new RigidBodyTransform();
      RigidBodyTransform actual = new RigidBodyTransform();

      for (int i = 0; i < ITERATIONS; i++)
      { // prependTranslation(double x, double y, double z)
         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform translationTransform = new RigidBodyTransform();
         double x = EuclidCoreRandomTools.nextDouble(random, -10.0, 10.0);
         double z = EuclidCoreRandomTools.nextDouble(random, -10.0, 10.0);
         double y = EuclidCoreRandomTools.nextDouble(random, -10.0, 10.0);
         translationTransform.getTranslation().set(x, y, z);
         expected.set(original);
         assertTrue(expected.hasTranslation());
         expected.preMultiply(translationTransform);
         assertTrue(expected.hasTranslation());

         actual.set(original);
         assertTrue(actual.hasTranslation());
         actual.prependTranslation(x, y, z);
         assertTrue(actual.hasTranslation());

         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         original.setToZero();
         actual.set(original);
         assertFalse(actual.hasTranslation());
         actual.prependTranslation(x, y, z);
         assertTrue(actual.hasTranslation());

         original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         actual.set(original);
         assertTrue(actual.hasTranslation());
         actual.prependTranslation(0.0, 0.0, 0.0);
         assertTrue(actual.hasTranslation());

         original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         Vector3D negateTranslation = new Vector3D(original.getTranslation());
         negateTranslation.negate();
         actual.set(original);
         assertTrue(actual.hasTranslation());
         actual.prependTranslation(negateTranslation.getX(), negateTranslation.getY(), negateTranslation.getZ());
         assertFalse(actual.hasTranslation());
      }

      for (int i = 0; i < ITERATIONS; i++)
      { // prependTranslation(Tuple3DReadOnly translation)
         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform translationTransform = new RigidBodyTransform();
         Tuple3DReadOnly translation = EuclidCoreRandomTools.nextPoint3D(random, 10.0, 10.0, 10.0);
         translationTransform.getTranslation().set(translation);
         expected.set(original);
         assertTrue(expected.hasTranslation());
         expected.preMultiply(translationTransform);
         assertTrue(expected.hasTranslation());

         actual.set(original);
         assertTrue(actual.hasTranslation());
         actual.prependTranslation(translation);
         assertTrue(actual.hasTranslation());

         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         original.setToZero();
         actual.set(original);
         assertFalse(actual.hasTranslation());
         actual.prependTranslation(translation);
         assertTrue(actual.hasTranslation());

         original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         actual.set(original);
         assertTrue(actual.hasTranslation());
         actual.prependTranslation(0.0, 0.0, 0.0);
         assertTrue(actual.hasTranslation());

         original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         Vector3D negateTranslation = new Vector3D(original.getTranslation());
         negateTranslation.negate();
         actual.set(original);
         assertTrue(actual.hasTranslation());
         actual.prependTranslation(negateTranslation);
         assertFalse(actual.hasTranslation());
      }
   }
   @Test // moved from non-basic
   public void testPrependOrientation() throws Exception
   {
      Random random = new Random(3456);

      for (int i = 0; i < ITERATIONS; i++)
      {
         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         Orientation3DBasics orientation = EuclidCoreRandomTools.nextOrientation3D(random);
         RigidBodyTransform orientationTransform = new RigidBodyTransform(orientation, new Vector3D());

         RigidBodyTransform expected = new RigidBodyTransform();
         expected.set(original);
         expected.preMultiply(orientationTransform);

         RigidBodyTransform actual = new RigidBodyTransform();
         actual.set(original);
         actual.prependOrientation(orientation);

         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);
      }
   }
   
   @Test // moved from non-basic
   public void testPrependYawPitchRoll() throws Exception
   {
      Random random = new Random(35454L);

      RigidBodyTransform expected = new RigidBodyTransform();
      RigidBodyTransform actual = new RigidBodyTransform();

      for (int i = 0; i < ITERATIONS; i++)
      { // prependYawRotation(double yaw)
         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform yawTransform = new RigidBodyTransform();
         double yaw = EuclidCoreRandomTools.nextDouble(random, Math.PI);
         yawTransform.getRotation().setToYawOrientation(yaw);
         expected.set(original);
         assertTrue(expected.hasRotation());
         expected.preMultiply(yawTransform);
         assertTrue(expected.hasRotation());

         actual.set(original);
         assertTrue(actual.hasRotation());
         actual.prependYawRotation(yaw);
         assertTrue(actual.hasRotation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         original.setToZero();
         yaw = EuclidCoreRandomTools.nextDouble(random, Math.PI);
         actual.set(original);
         assertFalse(actual.hasRotation());
         actual.prependYawRotation(yaw);
         assertTrue(actual.hasRotation());

         original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         yaw = 0.0;
         actual.set(original);
         assertTrue(actual.hasRotation());
         actual.prependYawRotation(yaw);
         assertTrue(actual.hasRotation());

         yaw = EuclidCoreRandomTools.nextDouble(random, Math.PI);
         original.getRotation().setToYawOrientation(-yaw);
         actual.set(original);
         assertTrue(actual.hasRotation());
         actual.prependYawRotation(yaw);
         assertFalse(actual.hasRotation());
      }

      for (int i = 0; i < ITERATIONS; i++)
      { // prependPitchRotation(double pitch)
         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform pitchTransform = new RigidBodyTransform();
         double pitch = EuclidCoreRandomTools.nextDouble(random, Math.PI);
         pitchTransform.getRotation().setToPitchOrientation(pitch);
         expected.set(original);
         expected.preMultiply(pitchTransform);
         assertTrue(expected.hasRotation());

         actual.set(original);
         assertTrue(actual.hasRotation());
         actual.prependPitchRotation(pitch);
         assertTrue(actual.hasRotation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         original.setToZero();
         pitch = EuclidCoreRandomTools.nextDouble(random, Math.PI);
         actual.set(original);
         assertFalse(actual.hasRotation());
         actual.prependPitchRotation(pitch);
         assertTrue(actual.hasRotation());

         original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         pitch = 0.0;
         actual.set(original);
         assertTrue(actual.hasRotation());
         actual.prependPitchRotation(pitch);
         assertTrue(actual.hasRotation());

         pitch = EuclidCoreRandomTools.nextDouble(random, Math.PI);
         original.getRotation().setToPitchOrientation(-pitch);
         actual.set(original);
         assertTrue(actual.hasRotation());
         actual.prependPitchRotation(pitch);
         assertFalse(actual.hasRotation());
      }

      for (int i = 0; i < ITERATIONS; i++)
      { // prependRollRotation(double roll)
         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform rollTransform = new RigidBodyTransform();
         double roll = EuclidCoreRandomTools.nextDouble(random, Math.PI);
         rollTransform.getRotation().setToRollOrientation(roll);
         expected.set(original);
         assertTrue(expected.hasRotation());
         expected.preMultiply(rollTransform);
         assertTrue(expected.hasRotation());

         actual.set(original);
         assertTrue(actual.hasRotation());
         actual.prependRollRotation(roll);
         assertTrue(actual.hasRotation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         original.setToZero();
         roll = EuclidCoreRandomTools.nextDouble(random, Math.PI);
         actual.set(original);
         assertFalse(actual.hasRotation());
         actual.prependRollRotation(roll);
         assertTrue(actual.hasRotation());

         original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         roll = 0.0;
         actual.set(original);
         assertTrue(actual.hasRotation());
         actual.prependRollRotation(roll);
         assertTrue(actual.hasRotation());

         roll = EuclidCoreRandomTools.nextDouble(random, Math.PI);
         original.getRotation().setToRollOrientation(-roll);
         actual.set(original);
         assertTrue(actual.hasRotation());
         actual.prependRollRotation(roll);
         assertFalse(actual.hasRotation());
      }
   }
   
   @Test // moved from non-basic
   public void testSetRotationAndZeroTranslation() throws Exception
   {
      Random random = new Random(2342L);
      RotationMatrix expectedRotation = EuclidCoreRandomTools.nextRotationMatrix(random);

      { // Test setRotationAndZeroTranslation(AxisAngleReadOnly axisAngle)
         RigidBodyTransform actualTransform = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         AxisAngle axisAngle = new AxisAngle(expectedRotation);
         actualTransform.setRotationAndZeroTranslation(axisAngle);
         assertTrue(actualTransform.hasRotation());
         assertFalse(actualTransform.hasTranslation());
         EuclidCoreTestTools.assertMatrix3DEquals(expectedRotation, actualTransform.getRotation(), 0.0);
         EuclidCoreTestTools.assertTuple3DIsSetToZero(actualTransform.getTranslation());

         actualTransform.setRotationAndZeroTranslation(new AxisAngle(EuclidCoreRandomTools.nextVector3DWithFixedLength(random, 1.0), 0.0));
         assertFalse(actualTransform.hasRotation());
      }

      { // Test setRotationAndZeroTranslation(DMatrix matrix)
         RigidBodyTransform actualTransform = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         DMatrix denseMatrix = new DMatrixRMaj(3, 3);
         expectedRotation.get(denseMatrix);
         actualTransform.setRotationAndZeroTranslation(denseMatrix);
         assertTrue(actualTransform.hasRotation());
         assertFalse(actualTransform.hasTranslation());
         EuclidCoreTestTools.assertMatrix3DEquals(expectedRotation, actualTransform.getRotation(), 0.0);
         EuclidCoreTestTools.assertTuple3DIsSetToZero(actualTransform.getTranslation());

         actualTransform.setRotationAndZeroTranslation(CommonOps_DDRM.identity(3));
         assertFalse(actualTransform.hasRotation());
      }

      { // Test setRotationAndZeroTranslation(QuaternionReadOnly quaternion)
         RigidBodyTransform actualTransform = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         Quaternion quaternion = new Quaternion(expectedRotation);
         actualTransform.setRotationAndZeroTranslation(quaternion);
         assertTrue(actualTransform.hasRotation());
         assertFalse(actualTransform.hasTranslation());
         EuclidCoreTestTools.assertMatrix3DEquals(expectedRotation, actualTransform.getRotation(), EPS);
         EuclidCoreTestTools.assertTuple3DIsSetToZero(actualTransform.getTranslation());

         actualTransform.setRotationAndZeroTranslation(new Quaternion());
         assertFalse(actualTransform.hasRotation());
      }

      { // Test setRotationAndZeroTranslation(Matrix3DReadOnly rotationMatrix)
         RigidBodyTransform actualTransform = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         actualTransform.setRotationAndZeroTranslation(expectedRotation);
         assertTrue(actualTransform.hasRotation());
         assertFalse(actualTransform.hasTranslation());
         EuclidCoreTestTools.assertMatrix3DEquals(expectedRotation, actualTransform.getRotation(), 0.0);
         EuclidCoreTestTools.assertTuple3DIsSetToZero(actualTransform.getTranslation());

         actualTransform.setRotationAndZeroTranslation(new RotationMatrix());
         assertFalse(actualTransform.hasRotation());
      }

      { // Test setRotation(Vector3DReadOnly rotationVector)
         Vector3D rotationVector = EuclidCoreRandomTools.nextRotationVector(random);
         RigidBodyTransform actualTransform = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         actualTransform.setRotationAndZeroTranslation(rotationVector);
         expectedRotation.setRotationVector(rotationVector);
         assertTrue(actualTransform.hasRotation());
         assertFalse(actualTransform.hasTranslation());
         EuclidCoreTestTools.assertMatrix3DEquals(expectedRotation, actualTransform.getRotation(), 0.0);
         EuclidCoreTestTools.assertTuple3DIsSetToZero(actualTransform.getTranslation());

         actualTransform.getRotation().setRotationVector(new Vector3D());
         assertFalse(actualTransform.hasRotation());
      }
   }
   
   @Test // moved from non-basic
   public void testMultiplyWithQuaternionBasedTransform() throws Exception
   {
      Random random = new Random(465416L);

      // Test against multiply(RigidBodyTransform)
      for (int i = 0; i < ITERATIONS; i++)
      {
         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform expected = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform actual = EuclidCoreRandomTools.nextRigidBodyTransform(random);

         RigidBodyTransform multipliedWithRigidBody = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         QuaternionBasedTransform multipliedWith = new QuaternionBasedTransform(multipliedWithRigidBody);

         expected.set(original);
         expected.multiply(multipliedWithRigidBody);
         actual.set(original);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.multiply(multipliedWith);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         multipliedWith.set(actual);
         multipliedWith.invert();
         actual.multiply(multipliedWith);
         assertFalse(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         expected.setToZero();
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);
      }
   }

   @Test // moved from non-basic
   public void testMultiplyWithAffineTransform() throws Exception
   {
      Random random = new Random(465416L);

      // Test against multiply(RigidBodyTransform)
      for (int i = 0; i < ITERATIONS; i++)
      {
         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform expected = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform actual = EuclidCoreRandomTools.nextRigidBodyTransform(random);

         RigidBodyTransform multipliedWithRigidBody = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         AffineTransform multipliedWith = new AffineTransform(multipliedWithRigidBody);
         multipliedWith.appendScale(EuclidCoreRandomTools.nextVector3D(random, 0.0, 10.0));

         expected.set(original);
         expected.multiply(multipliedWithRigidBody);
         actual.set(original);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.multiply(multipliedWith);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         multipliedWithRigidBody.set(actual);
         multipliedWithRigidBody.invert();
         actual.multiply(new AffineTransform(multipliedWithRigidBody));
         assertFalse(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         expected.setToZero();
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);
      }
   }
   
   @Test // moved from non-basic
   public void testMultiplyInvertThis() throws Exception
   {
      Random random = new Random(465416L);

      // Test against multiply(RigidBodyTransform)
      for (int i = 0; i < ITERATIONS; i++)
      {
         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform expected = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform actual = EuclidCoreRandomTools.nextRigidBodyTransform(random);

         RigidBodyTransform multipliedWith = EuclidCoreRandomTools.nextRigidBodyTransform(random);

         expected.set(original);
         expected.invert();
         expected.multiply(multipliedWith);
         actual.set(original);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.multiplyInvertThis(multipliedWith);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         actual.multiplyInvertThis(new RigidBodyTransform(actual));
         assertFalse(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         expected.setToZero();
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);
      }

      // Try different combinations with/without translation/rotation
      for (int i = 0; i < ITERATIONS; i++)
      {
         RigidBodyTransform expected = new RigidBodyTransform();
         RigidBodyTransform actual = new RigidBodyTransform();

         RigidBodyTransform t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         RigidBodyTransform t2 = new RigidBodyTransform(new Quaternion(), EuclidCoreRandomTools.nextVector3D(random));
         expected.setAndInvert(t1);
         expected.multiply(t2);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.multiplyInvertThis(t2);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(new Quaternion(), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         expected.setAndInvert(t1);
         expected.multiply(t2);
         actual.set(t1);
         assertFalse(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.multiplyInvertThis(t2);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), new Vector3D());
         expected.setAndInvert(t1);
         expected.multiply(t2);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.multiplyInvertThis(t2);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), new Vector3D());
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         expected.setAndInvert(t1);
         expected.multiply(t2);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         actual.multiplyInvertThis(t2);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(new RotationMatrix(), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(new RotationMatrix(), EuclidCoreRandomTools.nextVector3D(random));
         expected.setAndInvert(t1);
         expected.multiply(t2);
         actual.set(t1);
         assertFalse(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.multiplyInvertThis(t2);
         assertFalse(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), new Vector3D());
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), new Vector3D());
         expected.setAndInvert(t1);
         expected.multiply(t2);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         actual.multiplyInvertThis(t2);
         assertTrue(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(t1.getRotation(), EuclidCoreRandomTools.nextVector3D(random));
         expected.setAndInvert(t1);
         expected.multiply(t2);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.multiplyInvertThis(t2);
         assertFalse(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), t1.getTranslation());
         expected.setAndInvert(t1);
         expected.multiply(t2);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.multiplyInvertThis(t2);
         assertTrue(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);
      }
   }
   
   @Test // moved from non-basic
   public void testMultiplyInvertOther() throws Exception
   {
      Random random = new Random(465416L);

      // Test against multiply(RigidBodyTransform)
      for (int i = 0; i < ITERATIONS; i++)
      {
         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform expected = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform actual = EuclidCoreRandomTools.nextRigidBodyTransform(random);

         RigidBodyTransform multipliedWith = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform inverseOfMultipliedWith = new RigidBodyTransform(multipliedWith);
         inverseOfMultipliedWith.invert();

         expected.set(original);
         expected.multiply(inverseOfMultipliedWith);
         actual.set(original);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.multiplyInvertOther(multipliedWith);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         actual.multiplyInvertOther(new RigidBodyTransform(actual));
         assertFalse(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         expected.setToZero();
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);
      }

      // Try different combinations with/without translation/rotation
      for (int i = 0; i < ITERATIONS; i++)
      {
         RigidBodyTransform expected = new RigidBodyTransform();
         RigidBodyTransform actual = new RigidBodyTransform();

         RigidBodyTransform t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         RigidBodyTransform t2 = new RigidBodyTransform(new Quaternion(), EuclidCoreRandomTools.nextVector3D(random));
         RigidBodyTransform t2Inverse = new RigidBodyTransform();
         t2Inverse.setAndInvert(t2);
         expected.set(t1);
         expected.multiply(t2Inverse);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.multiplyInvertOther(t2);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(new Quaternion(), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         t2Inverse.setAndInvert(t2);
         expected.set(t1);
         expected.multiply(t2Inverse);
         actual.set(t1);
         assertFalse(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.multiplyInvertOther(t2);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), new Vector3D());
         t2Inverse.setAndInvert(t2);
         expected.set(t1);
         expected.multiply(t2Inverse);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.multiplyInvertOther(t2);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), new Vector3D());
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         t2Inverse.setAndInvert(t2);
         expected.set(t1);
         expected.multiply(t2Inverse);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         actual.multiplyInvertOther(t2);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(new RotationMatrix(), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(new RotationMatrix(), EuclidCoreRandomTools.nextVector3D(random));
         t2Inverse.setAndInvert(t2);
         expected.set(t1);
         expected.multiply(t2Inverse);
         actual.set(t1);
         assertFalse(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.multiplyInvertOther(t2);
         assertFalse(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), new Vector3D());
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), new Vector3D());
         t2Inverse.setAndInvert(t2);
         expected.set(t1);
         expected.multiply(t2Inverse);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         actual.multiplyInvertOther(t2);
         assertTrue(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(t1.getRotation(), EuclidCoreRandomTools.nextVector3D(random));
         t2Inverse.setAndInvert(t2);
         expected.set(t1);
         expected.multiply(t2Inverse);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.multiplyInvertOther(t2);
         assertFalse(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         Vector3D negateTranslation = new Vector3D(t1.getTranslation());
         t1.inverseTransform(negateTranslation);
         t2.transform(negateTranslation);
         t2.getTranslation().set(negateTranslation);
         t2Inverse.setAndInvert(t2);
         expected.set(t1);
         expected.multiply(t2Inverse);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.multiplyInvertOther(t2);
         assertTrue(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);
      }
   }
   
   @Test //moved from non-basic
   public void testMultiplyInvertThisWithQuaternionBasedTransform() throws Exception
   {
      Random random = new Random(465416L);

      // Test against multiply(RigidBodyTransform)
      for (int i = 0; i < ITERATIONS; i++)
      {
         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform expected = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform actual = EuclidCoreRandomTools.nextRigidBodyTransform(random);

         RigidBodyTransform multipliedWithRigidBody = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         QuaternionBasedTransform multipliedWith = new QuaternionBasedTransform(multipliedWithRigidBody);

         expected.set(original);
         expected.invert();
         expected.multiply(multipliedWithRigidBody);
         actual.set(original);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.multiplyInvertThis(multipliedWith);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         actual.multiplyInvertThis(new QuaternionBasedTransform(actual));
         assertFalse(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         expected.setToZero();
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);
      }
   }
   
   @Test //moved from non-basic
   public void testMultiplyInvertOtherWithQuaternionBasedTransform() throws Exception
   {
      Random random = new Random(465416L);

      // Test against multiply(RigidBodyTransform)
      for (int i = 0; i < ITERATIONS; i++)
      {
         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform expected = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform actual = EuclidCoreRandomTools.nextRigidBodyTransform(random);

         RigidBodyTransform multipliedWithRigidBody = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform inverseOfMultipliedWithRigidBody = new RigidBodyTransform(multipliedWithRigidBody);
         inverseOfMultipliedWithRigidBody.invert();
         QuaternionBasedTransform multipliedWith = new QuaternionBasedTransform(multipliedWithRigidBody);

         expected.set(original);
         expected.multiply(inverseOfMultipliedWithRigidBody);
         actual.set(original);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.multiplyInvertOther(multipliedWith);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         actual.multiplyInvertOther(new QuaternionBasedTransform(actual));
         assertFalse(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         expected.setToZero();
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);
      }
   }
   
   @Test //moved from non-basic
   public void testMultiplyInvertThisWithAffineTransform() throws Exception
   {
      Random random = new Random(465416L);

      // Test against multiply(RigidBodyTransform)
      for (int i = 0; i < ITERATIONS; i++)
      {
         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform expected = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform actual = EuclidCoreRandomTools.nextRigidBodyTransform(random);

         RigidBodyTransform multipliedWithRigidBody = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         AffineTransform multipliedWith = new AffineTransform(multipliedWithRigidBody);
         multipliedWith.appendScale(EuclidCoreRandomTools.nextVector3D(random, 0.0, 10.0));

         expected.set(original);
         expected.invert();
         expected.multiply(multipliedWithRigidBody);
         actual.set(original);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.multiplyInvertThis(multipliedWith);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         actual.multiplyInvertThis(new AffineTransform(actual));
         assertFalse(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         expected.setToZero();
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);
      }
   }
   
   @Test //moved from non-basic
   public void testMultiplyInvertOtherWithAffineTransform() throws Exception
   {
      Random random = new Random(465416L);

      // Test against multiply(RigidBodyTransform)
      for (int i = 0; i < ITERATIONS; i++)
      {
         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform expected = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform actual = EuclidCoreRandomTools.nextRigidBodyTransform(random);

         RigidBodyTransform multipliedWithRigidBody = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform inverseOfMultipliedWithRigidBody = new RigidBodyTransform(multipliedWithRigidBody);
         inverseOfMultipliedWithRigidBody.invert();
         AffineTransform multipliedWith = new AffineTransform(multipliedWithRigidBody);
         multipliedWith.appendScale(EuclidCoreRandomTools.nextVector3D(random, 0.0, 10.0));

         expected.set(original);
         expected.multiply(inverseOfMultipliedWithRigidBody);
         actual.set(original);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.multiplyInvertOther(multipliedWith);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         actual.multiplyInvertOther(new AffineTransform(actual));
         assertFalse(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         expected.setToZero();
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);
      }
   }
   
   @Test //moved from non-basic
   public void testPreMultiply() throws Exception
   {
      Random random = new Random(465416L);

      // Test against invert
      for (int i = 0; i < ITERATIONS; i++)
      {
         RigidBodyTransform transform = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform inverse = new RigidBodyTransform(transform);
         inverse.invert();

         assertTrue(transform.hasRotation());
         assertTrue(transform.hasTranslation());
         transform.preMultiply(inverse);
         assertFalse(transform.hasRotation());
         assertFalse(transform.hasTranslation());

         for (int row = 0; row < 4; row++)
         {
            for (int column = 0; column < 4; column++)
            {
               if (row == column)
                  assertEquals(transform.getElement(row, column), 1.0, EPS);
               else
                  assertEquals(transform.getElement(row, column), 0.0, EPS);
            }
         }
      }

      // Test against EJML
      for (int i = 0; i < ITERATIONS; i++)
      {
         RigidBodyTransform t1 = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform t2 = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform t3 = new RigidBodyTransform(t2);
         t3.preMultiply(t1);

         DMatrixRMaj m1 = new DMatrixRMaj(4, 4);
         DMatrixRMaj m2 = new DMatrixRMaj(4, 4);
         DMatrixRMaj m3 = new DMatrixRMaj(4, 4);
         t1.get(m1);
         t2.get(m2);
         CommonOps_DDRM.mult(m1, m2, m3);

         for (int row = 0; row < 4; row++)
            for (int column = 0; column < 4; column++)
               assertEquals(m3.get(row, column), t3.getElement(row, column), EPS);
      }

      // Try different combinations with/without translation/rotation
      for (int i = 0; i < ITERATIONS; i++)
      {
         RigidBodyTransform expected = new RigidBodyTransform();
         RigidBodyTransform actual = new RigidBodyTransform();

         RigidBodyTransform t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         RigidBodyTransform t2 = new RigidBodyTransform(new Quaternion(), EuclidCoreRandomTools.nextVector3D(random));
         expected.set(t2);
         expected.multiply(t1);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.preMultiply(t2);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(new Quaternion(), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         expected.set(t2);
         expected.multiply(t1);
         actual.set(t1);
         assertFalse(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.preMultiply(t2);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), new Vector3D());
         expected.set(t2);
         expected.multiply(t1);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.preMultiply(t2);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), new Vector3D());
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         expected.set(t2);
         expected.multiply(t1);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         actual.preMultiply(t2);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(new RotationMatrix(), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(new RotationMatrix(), EuclidCoreRandomTools.nextVector3D(random));
         expected.set(t2);
         expected.multiply(t1);
         actual.set(t1);
         assertFalse(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.preMultiply(t2);
         assertFalse(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), new Vector3D());
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), new Vector3D());
         expected.set(t2);
         expected.multiply(t1);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         actual.preMultiply(t2);
         assertTrue(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         t1.getRotation().set(t2.getRotation());
         t1.invertRotation();
         expected.set(t2);
         expected.multiply(t1);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.preMultiply(t2);
         assertFalse(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         Vector3D negateTranslation = new Vector3D(t2.getTranslation());
         negateTranslation.negate();
         t2.inverseTransform(negateTranslation);
         t1.getTranslation().set(negateTranslation);
         expected.set(t2);
         expected.multiply(t1);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.preMultiply(t2);
         assertTrue(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);
      }
   }
   
   @Test //moved from non-basic
   public void testPreMultiplyWithQuaternionBasedTransform() throws Exception
   {
      Random random = new Random(465416L);

      // Test against multiply(RigidBodyTransform)
      for (int i = 0; i < ITERATIONS; i++)
      {
         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform expected = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform actual = EuclidCoreRandomTools.nextRigidBodyTransform(random);

         RigidBodyTransform multipliedWithRigidBody = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         QuaternionBasedTransform multipliedWith = new QuaternionBasedTransform(multipliedWithRigidBody);

         expected.set(original);
         expected.preMultiply(multipliedWithRigidBody);
         actual.set(original);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.preMultiply(multipliedWith);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         multipliedWith.set(actual);
         multipliedWith.invert();
         actual.preMultiply(multipliedWith);
         assertFalse(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         expected.setToZero();
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);
      }
   }

   @Test //moved from non-basic
   public void testPreMultiplyWithAffineTransform() throws Exception
   {
      Random random = new Random(465416L);

      // Test against multiply(RigidBodyTransform)
      for (int i = 0; i < ITERATIONS; i++)
      {
         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform expected = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform actual = EuclidCoreRandomTools.nextRigidBodyTransform(random);

         RigidBodyTransform multipliedWithRigidBody = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         AffineTransform multipliedWith = new AffineTransform(multipliedWithRigidBody);
         multipliedWith.appendScale(EuclidCoreRandomTools.nextVector3D(random, 0.0, 10.0));

         expected.set(original);
         expected.preMultiply(multipliedWithRigidBody);
         actual.set(original);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.preMultiply(multipliedWith);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         multipliedWithRigidBody.set(actual);
         multipliedWithRigidBody.invert();
         actual.preMultiply(new AffineTransform(multipliedWithRigidBody));
         assertFalse(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         expected.setToZero();
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);
      }
   }

   @Test //moved from non-basic
   public void testPreMultiplyInvertThis() throws Exception
   {
      Random random = new Random(465416L);

      // Test against multiply(RigidBodyTransform)
      for (int i = 0; i < ITERATIONS; i++)
      {
         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform expected = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform actual = EuclidCoreRandomTools.nextRigidBodyTransform(random);

         RigidBodyTransform multipliedWith = EuclidCoreRandomTools.nextRigidBodyTransform(random);

         expected.set(original);
         expected.invert();
         expected.preMultiply(multipliedWith);
         actual.set(original);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.preMultiplyInvertThis(multipliedWith);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         actual.preMultiplyInvertThis(new RigidBodyTransform(actual));
         assertFalse(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         expected.setToZero();
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);
      }

      // Try different combinations with/without translation/rotation
      for (int i = 0; i < ITERATIONS; i++)
      {
         RigidBodyTransform expected = new RigidBodyTransform();
         RigidBodyTransform actual = new RigidBodyTransform();

         RigidBodyTransform t1 = new RigidBodyTransform(new Quaternion(), EuclidCoreRandomTools.nextVector3D(random));
         RigidBodyTransform t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         expected.set(t2);
         expected.multiplyInvertOther(t1);
         actual.set(t1);
         assertFalse(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.preMultiplyInvertThis(t2);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(new Quaternion(), EuclidCoreRandomTools.nextVector3D(random));
         expected.set(t2);
         expected.multiplyInvertOther(t1);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.preMultiplyInvertThis(t2);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), new Vector3D());
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         expected.set(t2);
         expected.multiplyInvertOther(t1);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         actual.preMultiplyInvertThis(t2);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), new Vector3D());
         expected.set(t2);
         expected.multiplyInvertOther(t1);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.preMultiplyInvertThis(t2);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(new RotationMatrix(), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(new RotationMatrix(), EuclidCoreRandomTools.nextVector3D(random));
         expected.set(t2);
         expected.multiplyInvertOther(t1);
         actual.set(t1);
         assertFalse(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.preMultiplyInvertThis(t2);
         assertFalse(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), new Vector3D());
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), new Vector3D());
         expected.set(t2);
         expected.multiplyInvertOther(t1);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         actual.preMultiplyInvertThis(t2);
         assertTrue(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         t1.getRotation().set(t2.getRotation());
         expected.set(t2);
         expected.multiplyInvertOther(t1);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.preMultiplyInvertThis(t2);
         assertFalse(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         Vector3D negateTranslation = new Vector3D(t2.getTranslation());
         t2.inverseTransform(negateTranslation);
         t1.transform(negateTranslation);
         t1.getTranslation().set(negateTranslation);
         expected.set(t2);
         expected.multiplyInvertOther(t1);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.preMultiplyInvertThis(t2);
         assertTrue(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);
      }
   }

   @Test //moved from non-basic
   public void testPreMultiplyInvertOther() throws Exception
   {
      Random random = new Random(465416L);

      // Test against multiply(RigidBodyTransform)
      for (int i = 0; i < ITERATIONS; i++)
      {
         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform expected = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform actual = EuclidCoreRandomTools.nextRigidBodyTransform(random);

         RigidBodyTransform multipliedWith = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform inverseOfMultipliedWith = new RigidBodyTransform(multipliedWith);
         inverseOfMultipliedWith.invert();

         expected.set(original);
         expected.preMultiply(inverseOfMultipliedWith);
         actual.set(original);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.preMultiplyInvertOther(multipliedWith);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         actual.preMultiplyInvertOther(new RigidBodyTransform(actual));
         assertFalse(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         expected.setToZero();
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);
      }

      // Try different combinations with/without translation/rotation
      for (int i = 0; i < ITERATIONS; i++)
      {
         RigidBodyTransform expected = new RigidBodyTransform();
         RigidBodyTransform actual = new RigidBodyTransform();

         RigidBodyTransform t1 = new RigidBodyTransform(new Quaternion(), EuclidCoreRandomTools.nextVector3D(random));
         RigidBodyTransform t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         expected.set(t2);
         expected.multiplyInvertThis(t1);
         actual.set(t1);
         assertFalse(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.preMultiplyInvertOther(t2);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(new Quaternion(), EuclidCoreRandomTools.nextVector3D(random));
         expected.set(t2);
         expected.multiplyInvertThis(t1);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.preMultiplyInvertOther(t2);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), new Vector3D());
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         expected.set(t2);
         expected.multiplyInvertThis(t1);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         actual.preMultiplyInvertOther(t2);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), new Vector3D());
         expected.set(t2);
         expected.multiplyInvertThis(t1);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.preMultiplyInvertOther(t2);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(new RotationMatrix(), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(new RotationMatrix(), EuclidCoreRandomTools.nextVector3D(random));
         expected.set(t2);
         expected.multiplyInvertThis(t1);
         actual.set(t1);
         assertFalse(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.preMultiplyInvertOther(t2);
         assertFalse(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), new Vector3D());
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), new Vector3D());
         expected.set(t2);
         expected.multiplyInvertThis(t1);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         actual.preMultiplyInvertOther(t2);
         assertTrue(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         t1.getRotation().set(t2.getRotation());
         expected.set(t2);
         expected.multiplyInvertThis(t1);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.preMultiplyInvertOther(t2);
         assertFalse(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         t1 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         t2 = new RigidBodyTransform(EuclidCoreRandomTools.nextQuaternion(random), EuclidCoreRandomTools.nextVector3D(random));
         t1.getTranslation().set(t2.getTranslation());
         expected.set(t2);
         expected.multiplyInvertThis(t1);
         actual.set(t1);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.preMultiplyInvertOther(t2);
         assertTrue(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);
      }
   }

   @Test //moved from non-basic
   public void testPreMultiplyInvertThisWithQuaternionBasedTransform() throws Exception
   {
      Random random = new Random(465416L);

      // Test against multiply(RigidBodyTransform)
      for (int i = 0; i < ITERATIONS; i++)
      {
         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform expected = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform actual = EuclidCoreRandomTools.nextRigidBodyTransform(random);

         RigidBodyTransform multipliedWithRigidBody = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         QuaternionBasedTransform multipliedWith = new QuaternionBasedTransform(multipliedWithRigidBody);

         expected.set(original);
         expected.invert();
         expected.preMultiply(multipliedWithRigidBody);
         actual.set(original);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         actual.preMultiplyInvertThis(multipliedWith);
         assertTrue(actual.hasRotation());
         assertTrue(actual.hasTranslation());
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         actual.preMultiplyInvertThis(new QuaternionBasedTransform(actual));
         assertFalse(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         expected.setToZero();
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

      }
   }

   @Test //moved from non-basic
   public void testPreMultiplyInvertOtherWithQuaternionBasedTransform() throws Exception
   {
      Random random = new Random(465416L);

      // Test against multiply(RigidBodyTransform)
      for (int i = 0; i < ITERATIONS; i++)
      {
         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform expected = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform actual = EuclidCoreRandomTools.nextRigidBodyTransform(random);

         RigidBodyTransform multipliedWithRigidBody = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform inverseOfMultipliedWithRigidBody = new RigidBodyTransform(multipliedWithRigidBody);
         inverseOfMultipliedWithRigidBody.invert();
         QuaternionBasedTransform multipliedWith = new QuaternionBasedTransform(multipliedWithRigidBody);

         expected.set(original);
         expected.preMultiply(inverseOfMultipliedWithRigidBody);
         actual.set(original);
         actual.preMultiplyInvertOther(multipliedWith);
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         actual.preMultiplyInvertOther(new QuaternionBasedTransform(actual));
         assertFalse(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         expected.setToZero();
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);
      }
   }

   @Test //moved from non-basic
   public void testPreMultiplyInvertThisWithAffineTransform() throws Exception
   {
      Random random = new Random(465416L);

      // Test against multiply(RigidBodyTransform)
      for (int i = 0; i < ITERATIONS; i++)
      {
         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform expected = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform actual = EuclidCoreRandomTools.nextRigidBodyTransform(random);

         RigidBodyTransform multipliedWithRigidBody = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         AffineTransform multipliedWith = new AffineTransform(multipliedWithRigidBody);
         multipliedWith.appendScale(EuclidCoreRandomTools.nextVector3D(random, 0.0, 10.0));

         expected.set(original);
         expected.invert();
         expected.preMultiply(multipliedWithRigidBody);
         actual.set(original);
         actual.preMultiplyInvertThis(multipliedWith);
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);

         actual.preMultiplyInvertThis(new AffineTransform(actual));
         assertFalse(actual.hasRotation());
         assertFalse(actual.hasTranslation());
         expected.setToZero();
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);
      }
   }
   
   @Test //moved from non-basic
   public void testPreMultiplyInvertOtherWithAffineTransform() throws Exception
   {
      Random random = new Random(465416L);

      // Test against multiply(RigidBodyTransform)
      for (int i = 0; i < ITERATIONS; i++)
      {
         RigidBodyTransform original = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform expected = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform actual = EuclidCoreRandomTools.nextRigidBodyTransform(random);

         RigidBodyTransform multipliedWithRigidBody = EuclidCoreRandomTools.nextRigidBodyTransform(random);
         RigidBodyTransform inverseOfMultipliedWithRigidBody = new RigidBodyTransform(multipliedWithRigidBody);
         inverseOfMultipliedWithRigidBody.invert();
         AffineTransform multipliedWith = new AffineTransform(multipliedWithRigidBody);
         multipliedWith.appendScale(EuclidCoreRandomTools.nextVector3D(random, 0.0, 10.0));

         expected.set(original);
         expected.preMultiply(inverseOfMultipliedWithRigidBody);
         actual.set(original);
         actual.preMultiplyInvertOther(multipliedWith);
         EuclidCoreTestTools.assertRigidBodyTransformEquals(expected, actual, EPS);
      }
   }

   private void checkMultiplyAgainstEJML(T t1, T t2)
   {
      T actual_t3 = copy(t1);
      actual_t3.multiply(t2);

      DMatrixRMaj m1 = CommonOps_DDRM.identity(4);
      DMatrixRMaj m2 = CommonOps_DDRM.identity(4);
      DMatrixRMaj m3 = new DMatrixRMaj(4, 4);

      // Pack t1 into m1
      Matrix3D r1 = new Matrix3D();
      t1.getRotation().get(r1);
      r1.get(m1);
      t1.getTranslation().get(0, 3, m1);

      // Pack t2 into m2
      Matrix3D r2 = new Matrix3D();
      t2.getRotation().get(r2);
      r2.get(m2);
      t2.getTranslation().get(0, 3, m2);

      CommonOps_DDRM.mult(m1, m2, m3);

      T expected_t3 = identity();
      expected_t3.getRotation().setRotationMatrix(m3.get(0, 0), m3.get(0, 1), m3.get(0, 2),
                                                  m3.get(1, 0), m3.get(1, 1), m3.get(1, 2),
                                                  m3.get(2, 0), m3.get(2, 1), m3.get(2, 2));
      expected_t3.getTranslation().set(0, 3, m3);

      EuclidCoreTestTools.assertRigidBodyTransformGeometricallyEquals(expected_t3, actual_t3, EPS);
   }

}