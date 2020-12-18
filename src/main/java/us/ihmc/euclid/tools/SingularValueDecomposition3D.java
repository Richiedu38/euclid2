package us.ihmc.euclid.tools;

import us.ihmc.euclid.exceptions.SingularMatrixException;
import us.ihmc.euclid.matrix.Matrix3D;
import us.ihmc.euclid.matrix.interfaces.Matrix3DBasics;
import us.ihmc.euclid.matrix.interfaces.Matrix3DReadOnly;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.euclid.tuple4D.interfaces.QuaternionBasics;

/**
 * Calculator for performing singular value decomposition of a 3D matrix <tt>A</tt> as:
 *
 * <pre>
 * A = U W V<sup>T</sup>
 * </pre>
 * <p>
 * The algorithm is based on the paper: <i>Computing the Singular Value Decomposition of 3 x 3
 * matrices with minimal branching and elementary floating point operations</i>.
 * </p>
 * <p>
 * The main particularity of the algorithm used in this calculator is the guarantee for <tt>U</tt>
 * and <tt>V</tt> to represent pure rotations and they are computed directly as quaternions. To
 * permit that, the sign of the third singular value, i.e. W<sub>3,3</sub>, matches the sign the
 * determinant of <tt>A</tt>.
 * </p>
 *
 * @author Sylvain Bertrand
 */
public class SingularValueDecomposition3D
{
   static final double gamma = 3.0 + 2.0 * EuclidCoreTools.squareRoot(2.0);
   static final double cosPiOverEight = EuclidCoreTools.cos(Math.PI / 8.0);
   static final double sinPiOverEight = EuclidCoreTools.sin(Math.PI / 8.0);
   static final double sqrtTwoOverTwo = EuclidCoreTools.squareRoot(2.0) / 2.0;

   private final Matrix3D temp = new Matrix3D();
   private final SVD3DOutput output = new SVD3DOutput();

   private int maxIterations = 25;
   private double tolerance = 1.0e-13;
   private boolean sortDescendingOrder = true;

   private int iterations = -1;

   /**
    * Creates a new calculator ready to be used.
    */
   public SingularValueDecomposition3D()
   {
   }

   /**
    * Sets the maximum number of iterations for the first stage of the decomposition.
    *
    * @param maxIterations the new maximum number of iterations for the next decompositions. Default
    *                      value is {@code 25}.
    */
   public void setMaxIterations(int maxIterations)
   {
      this.maxIterations = maxIterations;
   }

   /**
    * Sets the tolerance used internally, lower value means higher accuracy but more iterations.
    *
    * @param tolerance the new tolerance to used for the next decompositions. Default value
    *                  {@code 1.0e-12}.
    */
   public void setTolerance(double tolerance)
   {
      this.tolerance = tolerance;
   }

   /**
    * Specifies whether the singular values should be sorted in descending order, i.e.:
    * 
    * <pre>
    * W.getX() &geq; W.getY() &geq; |W.getZ()|
    * </pre>
    * 
    * Note that regardless of the state of this flag, the two first singular values are always positive
    * and the sign of the third singular value is the same as the determinant of the matrix being
    * decomposed.
    * 
    * @param sortDescendingOrder {@code true} for sorting the singular values in descending,
    *                            {@code false} for skipping sorting. Default value {@code true}.
    */
   public void setSortDescendingOrder(boolean sortDescendingOrder)
   {
      this.sortDescendingOrder = sortDescendingOrder;
   }

   /**
    * Performs a decomposition of the given matrix {@code A} into <tt>U W V<sup>T</sup></tt>.
    * <ul>
    * <li><tt>U</tt> and <tt>V</tt> are rotations stored as quaternions,
    * <li><tt>W</tt> is a vector storing the three singular values.
    * </p>
    *
    * @param A the matrix to be decomposed. Not modified.
    * @return whether the algorithm succeeded or not.
    */
   public boolean decompose(Matrix3DReadOnly A)
   {
      double scale = A.maxAbsElement();
      double a00 = A.getM00() / scale;
      double a01 = A.getM01() / scale;
      double a02 = A.getM02() / scale;
      double a10 = A.getM10() / scale;
      double a11 = A.getM11() / scale;
      double a12 = A.getM12() / scale;
      double a20 = A.getM20() / scale;
      double a21 = A.getM21() / scale;
      double a22 = A.getM22() / scale;
      if (!computeV(a00, a01, a02, a10, a11, a12, a20, a21, a22))
         return false;
      computeUW(a00, a01, a02, a10, a11, a12, a20, a21, a22);
      output.W.scale(scale);
      return true;
   }

   /**
    * First stage of the algorithm described in section <i>2 Symmetric eigenanalysis</i>
    *
    * @param a00 element of the matrix to decompose.
    * @param a01 element of the matrix to decompose.
    * @param a02 element of the matrix to decompose.
    * @param a10 element of the matrix to decompose.
    * @param a11 element of the matrix to decompose.
    * @param a12 element of the matrix to decompose.
    * @param a20 element of the matrix to decompose.
    * @param a21 element of the matrix to decompose.
    * @param a22 element of the matrix to decompose.
    * @return whether the algorithm succeeded or not.
    */
   private boolean computeV(double a00, double a01, double a02, double a10, double a11, double a12, double a20, double a21, double a22)
   {
      Matrix3D S = temp;
      S.set(a00, a01, a02, a10, a11, a12, a20, a21, a22);
      S.multiplyInner();
      iterations = computeV(S, output.V, maxIterations, tolerance);
      return iterations < maxIterations;
   }

   static int computeV(Matrix3DBasics S, QuaternionBasics V, int maxIterations, double tolerance)
   {
      int iteration = 0;

      V.setToZero();

      for (; iteration < maxIterations; iteration++)
      {
         // Find the element off-diagonal with the max absolute value. Section 2.1
         double a_01_abs = Math.abs(S.getM01());
         double a_02_abs = Math.abs(S.getM02());
         double a_12_abs = Math.abs(S.getM12());

         if (a_01_abs > a_02_abs)
         {
            if (a_01_abs > a_12_abs)
            {
               if (a_01_abs <= tolerance * Math.abs(S.getM00()) * Math.abs(S.getM11()))
                  break;
               approxGivensQuaternion(0, 1, S, V);
            }
            else
            {
               if (a_12_abs <= tolerance * Math.abs(S.getM11()) * Math.abs(S.getM22()))
                  break;
               approxGivensQuaternion(1, 2, S, V);
            }
         }
         else
         {
            if (a_02_abs > a_12_abs)
            {
               if (a_02_abs <= tolerance * Math.abs(S.getM00()) * Math.abs(S.getM22()))
                  break;
               approxGivensQuaternion(0, 2, S, V);
            }
            else
            {
               if (a_12_abs <= tolerance * Math.abs(S.getM11()) * Math.abs(S.getM22()))
                  break;
               approxGivensQuaternion(1, 2, S, V);
            }
         }
      }

      if (iteration > 0)
         V.normalize();

      return iteration;
   }

   /**
    * Performs one Jacobi iteration given the coordinate (p, q) of the off-diagonal element in
    * {@code S} to cancel.
    * <p>
    * The method computes the givens quaternion which is used to both update {@code S} and {@code Q}.
    * </p>
    */
   static void approxGivensQuaternion(int p, int q, Matrix3DBasics SToUpdate, QuaternionBasics QToUpdate)
   {
      // Compute (ch, sh) as described in Algorithm 2.
      double s_pp, s_pq, s_qq;

      if (p == 0)
      {
         if (q == 1)
         {
            s_pp = SToUpdate.getM00();
            s_pq = SToUpdate.getM01();
            s_qq = SToUpdate.getM11();
         }
         else
         {
            s_pp = SToUpdate.getM00();
            s_pq = SToUpdate.getM02();
            s_qq = SToUpdate.getM22();
         }
      }
      else
      {
         s_pp = SToUpdate.getM11();
         s_pq = SToUpdate.getM12();
         s_qq = SToUpdate.getM22();
      }

      double ch = 2.0 * (s_pp - s_qq);
      double sh = s_pq;

      if (gamma * sh * sh < ch * ch)
      {
         /*
          * TODO The square root does not need to be accurate. In the paper, the authors suggest using the
          * SSE RSQRTPS, need to look into it.
          */
         double omega = 1.0 / EuclidCoreTools.squareRoot(ch * ch + sh * sh);
         ch *= omega;
         sh *= omega;
      }
      else
      {
         ch = cosPiOverEight;
         sh = sinPiOverEight;
      }

      // Based on (p, q), we identify the rotation axis.
      if (p == 0)
      {
         if (q == 1)
         { // Rotation along the z-axis
            prependGivensQuaternionZ(ch, sh, QToUpdate);
            applyJacobiGivensRotationZ(ch, sh, SToUpdate);
         }
         else
         { // Rotation along the y-axis
            prependGivensQuaternionY(ch, sh, QToUpdate);
            applyJacobiGivensRotationY(ch, sh, SToUpdate);
         }
      }
      else
      { // Rotation along the x-axis
         prependGivensQuaternionX(ch, sh, QToUpdate);
         applyJacobiGivensRotationX(ch, sh, SToUpdate);
      }
   }

   /**
    * Second and third stages of the algorithm described in section <i>3 Sorting the singular
    * values</i> and <i>4 Computation of the factors U and &Sigma;</i>
    *
    * @param a00 element of the matrix to decompose.
    * @param a01 element of the matrix to decompose.
    * @param a02 element of the matrix to decompose.
    * @param a10 element of the matrix to decompose.
    * @param a11 element of the matrix to decompose.
    * @param a12 element of the matrix to decompose.
    * @param a20 element of the matrix to decompose.
    * @param a21 element of the matrix to decompose.
    * @param a22 element of the matrix to decompose.
    */
   private void computeUW(double a00, double a01, double a02, double a10, double a11, double a12, double a20, double a21, double a22)
   {
      Matrix3D B = temp;
      // B = A V
      computeB(a00, a01, a02, a10, a11, a12, a20, a21, a22, output.V, B);
      // Sorting the columns of B as described in Algorithm 3
      if (sortDescendingOrder)
         sortBColumns(B, output.V);
      output.U.setToZero();

      boolean isUquatInitialized = false;

      // Test each off-diagonal element of B to decide whether to cancel it or not.
      if (!EuclidCoreTools.isZero(B.getM10(), tolerance * Math.abs(B.getM00()) * Math.abs(B.getM11())) || B.getM00() < 0.0)
      {
         qrGivensQuaternion(1, 0, B, output.U, tolerance);
         isUquatInitialized = true;
      }

      if (!EuclidCoreTools.isZero(B.getM20(), tolerance * Math.abs(B.getM22()) * Math.abs(B.getM00())) || B.getM11() < 0.0)
      {
         qrGivensQuaternion(2, 0, B, output.U, tolerance);
         isUquatInitialized = true;
      }

      if (!EuclidCoreTools.isZero(B.getM21(), tolerance * Math.abs(B.getM22()) * Math.abs(B.getM11())) || B.getM11() < 0.0)
      {
         qrGivensQuaternion(2, 1, B, output.U, tolerance);
         isUquatInitialized = true;
      }

      output.W.set(B.getM00(), B.getM11(), B.getM22());

      if (isUquatInitialized)
         output.U.normalize();
   }

   /**
    * Computes B as follows: {@code B = A V}.
    *
    * @param a00 element of the matrix to decompose.
    * @param a01 element of the matrix to decompose.
    * @param a02 element of the matrix to decompose.
    * @param a10 element of the matrix to decompose.
    * @param a11 element of the matrix to decompose.
    * @param a12 element of the matrix to decompose.
    * @param a20 element of the matrix to decompose.
    * @param a21 element of the matrix to decompose.
    * @param a22 element of the matrix to decompose.
    */
   private void computeB(double a00, double a01, double a02, double a10, double a11, double a12, double a20, double a21, double a22, Quaternion V,
                         Matrix3D BToPack)
   {
      double qx = V.getX();
      double qy = V.getY();
      double qz = V.getZ();
      double qs = V.getS();

      double yy2 = 2.0 * qy * qy;
      double zz2 = 2.0 * qz * qz;
      double xx2 = 2.0 * qx * qx;
      double xy2 = 2.0 * qx * qy;
      double sz2 = 2.0 * qs * qz;
      double xz2 = 2.0 * qx * qz;
      double sy2 = 2.0 * qs * qy;
      double yz2 = 2.0 * qy * qz;
      double sx2 = 2.0 * qs * qx;

      double m00 = 1.0 - yy2 - zz2;
      double m01 = xy2 - sz2;
      double m02 = xz2 + sy2;
      double m10 = xy2 + sz2;
      double m11 = 1.0 - xx2 - zz2;
      double m12 = yz2 - sx2;
      double m20 = xz2 - sy2;
      double m21 = yz2 + sx2;
      double m22 = 1.0 - xx2 - yy2;

      double b00 = a00 * m00 + a01 * m10 + a02 * m20;
      double b01 = a00 * m01 + a01 * m11 + a02 * m21;
      double b02 = a00 * m02 + a01 * m12 + a02 * m22;
      double b10 = a10 * m00 + a11 * m10 + a12 * m20;
      double b11 = a10 * m01 + a11 * m11 + a12 * m21;
      double b12 = a10 * m02 + a11 * m12 + a12 * m22;
      double b20 = a20 * m00 + a21 * m10 + a22 * m20;
      double b21 = a20 * m01 + a21 * m11 + a22 * m21;
      double b22 = a20 * m02 + a21 * m12 + a22 * m22;
      BToPack.set(b00, b01, b02, b10, b11, b12, b20, b21, b22);
   }

   /**
    * Performs one QR iteration given the entry (p, q).
    */
   private static void qrGivensQuaternion(int p, int q, Matrix3DBasics B, QuaternionBasics UToUpdate, double epsilon)
   {
      // Compute (ch, sh) as described in Algorithm 4.
      double a1 = B.getElement(q, q);
      double a2 = B.getElement(p, q);

      double rho = EuclidCoreTools.squareRoot(a1 * a1 + a2 * a2);
      double ch;
      double sh;

      if (a1 < 0.0)
      {
         ch = rho > epsilon ? a2 : 0.0;
         sh = -a1 + Math.max(rho, epsilon);
      }
      else
      {
         ch = a1 + Math.max(rho, epsilon);
         sh = rho > epsilon ? a2 : 0.0;

      }

      double omega = 1.0 / EuclidCoreTools.squareRoot(ch * ch + sh * sh);
      ch *= omega;
      sh *= omega;

      // Based on (p, q), we identify the rotation axis.
      if (q == 0)
      {
         if (p == 1)
         { // Rotation along the z-axis
            appendGivensQuaternionZ(ch, sh, UToUpdate);
            applyQRGivensRotationZ(ch, sh, B);
         }
         else // p == 2
         { // Rotation along the y-axis
            appendGivensQuaternionY(ch, sh, UToUpdate);
            applyQRGivensRotationY(ch, sh, B);
         }
      }
      else
      { // Rotation along the x-axis
         appendGivensQuaternionX(ch, sh, UToUpdate);
         applyQRGivensRotationX(ch, sh, B);
      }
   }

   /**
    * Computes the givens rotation, see equation (12), when p=1 and q=2 and applies it to {@code S},
    * i.e. <tt>S = Q<sup>T</sup> S Q</tt>.
    */
   static void applyJacobiGivensRotationX(double ch, double sh, Matrix3DBasics S)
   {
      double ch2 = ch * ch;
      double sh2 = sh * sh;

      double diag_a = ch2 + sh2;
      double diag_b = (ch2 - sh2) / diag_a;
      double off_diag = 2.0 * ch * sh / diag_a;

      double s00 = S.getM00();
      double s11 = S.getM11();
      double s22 = S.getM22();
      double s01 = S.getM01();
      double s02 = S.getM02();
      double s12 = S.getM12();

      double qTs11 = diag_b * s11 + off_diag * s12;
      double qTs22 = diag_b * s22 - off_diag * s12;
      double qTs12 = diag_b * s12 + off_diag * s22;
      double qTs21 = diag_b * s12 - off_diag * s11;

      double qTsq11 = qTs11 * diag_b + qTs12 * off_diag;
      double qTsq22 = qTs22 * diag_b - qTs21 * off_diag;
      double qTsq01 = s01 * diag_b + s02 * off_diag;
      double qTsq02 = s02 * diag_b - s01 * off_diag;
      double qTsq12 = qTs12 * diag_b - qTs11 * off_diag;
      S.set(s00, qTsq01, qTsq02, qTsq01, qTsq11, qTsq12, qTsq02, qTsq12, qTsq22);
   }

   /**
    * Computes the givens rotation, see equation (12), when p=0 and q=2 and applies it to {@code S},
    * i.e. <tt>S = Q<sup>T</sup> S Q</tt>.
    */
   static void applyJacobiGivensRotationY(double ch, double sh, Matrix3DBasics S)
   {
      double ch2 = ch * ch;
      double sh2 = sh * sh;

      double diag_a = ch2 + sh2;
      double diag_b = (ch2 - sh2) / diag_a;
      double off_diag = 2.0 * ch * sh / diag_a;

      double s00 = S.getM00();
      double s11 = S.getM11();
      double s22 = S.getM22();
      double s01 = S.getM01();
      double s02 = S.getM02();
      double s12 = S.getM12();

      double qTs00 = diag_b * s00 + off_diag * s02;
      double qTs01 = diag_b * s01 + off_diag * s12;
      double qTs02 = diag_b * s02 + off_diag * s22;
      double qTs20 = diag_b * s02 - off_diag * s00;
      double qTs22 = diag_b * s22 - off_diag * s02;

      double qTsq00 = diag_b * qTs00 + off_diag * qTs02;
      double qTsq02 = diag_b * qTs02 - off_diag * qTs00;
      double qTsq12 = diag_b * s12 - off_diag * s01;
      double qTsq22 = diag_b * qTs22 - off_diag * qTs20;
      S.set(qTsq00, qTs01, qTsq02, qTs01, s11, qTsq12, qTsq02, qTsq12, qTsq22);
   }

   /**
    * Computes the givens rotation, see equation (12), when p=0 and q=1 and applies it to {@code S},
    * i.e. <tt>S = Q<sup>T</sup> S Q</tt>.
    */
   static void applyJacobiGivensRotationZ(double ch, double sh, Matrix3DBasics S)
   {
      double ch2 = ch * ch;
      double sh2 = sh * sh;

      double diag_a = ch2 + sh2;
      double diag_b = (ch2 - sh2) / diag_a;
      double off_diag = 2.0 * ch * sh / diag_a;

      double s00 = S.getM00();
      double s11 = S.getM11();
      double s22 = S.getM22();
      double s01 = S.getM01();
      double s02 = S.getM02();
      double s12 = S.getM12();

      double qTs00 = diag_b * s00 + off_diag * s01;
      double qTs01 = diag_b * s01 + off_diag * s11;
      double qTs02 = diag_b * s02 + off_diag * s12;
      double qTs10 = diag_b * s01 - off_diag * s00;
      double qTs11 = diag_b * s11 - off_diag * s01;
      double qTs12 = diag_b * s12 - off_diag * s02;
      double qTsq00 = diag_b * qTs00 + off_diag * qTs01;
      double qTsq01 = diag_b * qTs01 - off_diag * qTs00;
      double qTsq11 = diag_b * qTs11 - off_diag * qTs10;
      S.set(qTsq00, qTsq01, qTs02, qTsq01, qTsq11, qTs12, qTs02, qTs12, s22);
   }

   /**
    * Prepends the givens rotation around the x-axis to {@code B}.
    */
   private static void applyQRGivensRotationX(double ch, double sh, Matrix3DBasics BToUpdate)
   {
      double ch2 = ch * ch;
      double sh2 = sh * sh;

      double diag_a = ch2 + sh2;
      double diag_b = (ch2 - sh2) / diag_a;
      double off_diag = 2.0 * ch * sh / diag_a;
      double b10 = diag_b * BToUpdate.getM10() + off_diag * BToUpdate.getM20();
      double b11 = diag_b * BToUpdate.getM11() + off_diag * BToUpdate.getM21();
      double b12 = diag_b * BToUpdate.getM12() + off_diag * BToUpdate.getM22();
      double b20 = diag_b * BToUpdate.getM20() - off_diag * BToUpdate.getM10();
      double b21 = diag_b * BToUpdate.getM21() - off_diag * BToUpdate.getM11();
      double b22 = diag_b * BToUpdate.getM22() - off_diag * BToUpdate.getM12();
      BToUpdate.set(BToUpdate.getM00(), BToUpdate.getM01(), BToUpdate.getM02(), b10, b11, b12, b20, b21, b22);
   }

   /**
    * Prepends the givens rotation around the y-axis to {@code B}.
    */
   private static void applyQRGivensRotationY(double ch, double sh, Matrix3DBasics BToUpdate)
   {
      double ch2 = ch * ch;
      double sh2 = sh * sh;

      double diag_a = ch2 + sh2;
      double diag_b = (ch2 - sh2) / diag_a;
      double off_diag = 2.0 * ch * sh / diag_a;
      double b00 = diag_b * BToUpdate.getM00() + off_diag * BToUpdate.getM20();
      double b01 = diag_b * BToUpdate.getM01() + off_diag * BToUpdate.getM21();
      double b02 = diag_b * BToUpdate.getM02() + off_diag * BToUpdate.getM22();
      double b20 = diag_b * BToUpdate.getM20() - off_diag * BToUpdate.getM00();
      double b21 = diag_b * BToUpdate.getM21() - off_diag * BToUpdate.getM01();
      double b22 = diag_b * BToUpdate.getM22() - off_diag * BToUpdate.getM02();
      BToUpdate.set(b00, b01, b02, BToUpdate.getM10(), BToUpdate.getM11(), BToUpdate.getM12(), b20, b21, b22);
   }

   /**
    * Prepends the givens rotation around the z-axis to {@code B}.
    */
   private static void applyQRGivensRotationZ(double ch, double sh, Matrix3DBasics BToUpdate)
   {
      double ch2 = ch * ch;
      double sh2 = sh * sh;

      double diag_a = ch2 + sh2;
      double diag_b = (ch2 - sh2) / diag_a;
      double off_diag = 2.0 * ch * sh / diag_a;
      double b00 = diag_b * BToUpdate.getM00() + off_diag * BToUpdate.getM10();
      double b01 = diag_b * BToUpdate.getM01() + off_diag * BToUpdate.getM11();
      double b02 = diag_b * BToUpdate.getM02() + off_diag * BToUpdate.getM12();
      double b10 = diag_b * BToUpdate.getM10() - off_diag * BToUpdate.getM00();
      double b11 = diag_b * BToUpdate.getM11() - off_diag * BToUpdate.getM01();
      double b12 = diag_b * BToUpdate.getM12() - off_diag * BToUpdate.getM02();
      BToUpdate.set(b00, b01, b02, b10, b11, b12, BToUpdate.getM20(), BToUpdate.getM21(), BToUpdate.getM22());
   }

   /**
    * Prepends the givens quaternion (sh, 0, 0, ch) (when p=1 and q=2) to {@code V}.
    */
   private static void prependGivensQuaternionX(double ch, double sh, QuaternionBasics V)
   {
      double vx = V.getX();
      double vy = V.getY();
      double vz = V.getZ();
      double vs = V.getS();
      V.setUnsafe(vx * ch + vs * sh, vy * ch + vz * sh, vz * ch - vy * sh, vs * ch - vx * sh);
   }

   /**
    * Prepends the givens quaternion (0, sh, 0, ch) (when p=0 and q=2) to {@code V}.
    */
   private static void prependGivensQuaternionY(double ch, double sh, QuaternionBasics V)
   {
      double vx = V.getX();
      double vy = V.getY();
      double vz = V.getZ();
      double vs = V.getS();
      V.setUnsafe(vz * sh + vx * ch, vy * ch - vs * sh, vz * ch - vx * sh, vs * ch + vy * sh);
   }

   /**
    * Prepends the givens quaternion (0, 0, sh, ch) (when p=0 and q=1) to {@code V}.
    */
   private static void prependGivensQuaternionZ(double ch, double sh, QuaternionBasics V)
   {
      double vx = V.getX();
      double vy = V.getY();
      double vz = V.getZ();
      double vs = V.getS();
      V.setUnsafe(vx * ch + vy * sh, vy * ch - vx * sh, vz * ch + vs * sh, vs * ch - vz * sh);
   }

   /**
    * Appends the givens quaternion (sh, 0, 0, ch) (when p=1 and q=2) to {@code U}.
    */
   private static void appendGivensQuaternionX(double ch, double sh, QuaternionBasics U)
   {
      double ux = U.getX();
      double uy = U.getY();
      double uz = U.getZ();
      double us = U.getS();
      U.setUnsafe(us * sh + ux * ch, uy * ch + uz * sh, uz * ch - uy * sh, us * ch - ux * sh);
   }

   /**
    * Appends the givens quaternion (0, sh, 0, ch) (when p=0 and q=2) to {@code U}.
    */
   private static void appendGivensQuaternionY(double ch, double sh, QuaternionBasics U)
   {
      double ux = U.getX();
      double uy = U.getY();
      double uz = U.getZ();
      double us = U.getS();
      U.setUnsafe(ux * ch + uz * sh, uy * ch - us * sh, uz * ch - ux * sh, us * ch + uy * sh);
   }

   /**
    * Appends the givens quaternion (0, 0, sh, ch) (when p=0 and q=1) to {@code U}.
    */
   private static void appendGivensQuaternionZ(double ch, double sh, QuaternionBasics U)
   {
      double ux = U.getX();
      double uy = U.getY();
      double uz = U.getZ();
      double us = U.getS();
      U.setUnsafe(ux * ch + uy * sh, uy * ch - ux * sh, us * sh + uz * ch, us * ch - uz * sh);
   }

   /**
    * Implements the Algorithms 3 that sorts the columns of B in descending order, and updates V such
    * that the equality {@code B = A V} is preserved.
    */
   static void sortBColumns(Matrix3DBasics B, QuaternionBasics V)
   {
      double rho0 = EuclidCoreTools.normSquared(B.getM00(), B.getM10(), B.getM20());
      double rho1 = EuclidCoreTools.normSquared(B.getM01(), B.getM11(), B.getM21());
      double rho2 = EuclidCoreTools.normSquared(B.getM02(), B.getM12(), B.getM22());

      double qx = V.getX();
      double qy = V.getY();
      double qz = V.getZ();
      double qs = V.getS();

      // @formatter:off
      if (rho0 >= rho1)
      {
         if (rho0 >= rho2)
         {
            if (rho1 >= rho2)
            { // 0 > 1 > 2
              // Do nothing
            }
            else
            { // 0 > 2 > 1
               B.set(B.getM00(), B.getM02(), -B.getM01(),
                     B.getM10(), B.getM12(), -B.getM11(),
                     B.getM20(), B.getM22(), -B.getM21());
               V.setUnsafe(sqrtTwoOverTwo * (qs + qx),
                           sqrtTwoOverTwo * (qy + qz),
                           sqrtTwoOverTwo * (qz - qy),
                           sqrtTwoOverTwo * (qs - qx));
            }
         }
         else
         { // 2 > 0 > 1
            B.set(B.getM02(), B.getM00(), B.getM01(),
                  B.getM12(), B.getM10(), B.getM11(),
                  B.getM22(), B.getM20(), B.getM21());
            V.setUnsafe(0.5 * (-qs + qx - qy + qz),
                        0.5 * (-qs + qx + qy - qz),
                        0.5 * (-qs - qx + qy + qz),
                        0.5 * ( qs + qx + qy + qz));
         }
      }
      else
      {
         if (rho1 >= rho2)
         {
            if (rho0 >= rho2)
            { // 1 > 0 > 2
               B.set(B.getM01(), -B.getM00(), B.getM02(),
                     B.getM11(), -B.getM10(), B.getM12(),
                     B.getM21(), -B.getM20(), B.getM22());
               V.setUnsafe(sqrtTwoOverTwo * (qx + qy),
                           sqrtTwoOverTwo * (qy - qx),
                           sqrtTwoOverTwo * (qs + qz),
                           sqrtTwoOverTwo * (qs - qz));
            }
            else
            { // 1 > 2 > 0
               B.set(B.getM01(), B.getM02(), B.getM00(),
                     B.getM11(), B.getM12(), B.getM10(),
                     B.getM21(), B.getM22(), B.getM20());
               V.setUnsafe(0.5 * (qs + qx + qy - qz),
                           0.5 * (qs - qx + qy + qz),
                           0.5 * (qs + qx - qy + qz),
                           0.5 * (qs - qx - qy - qz));
            }
         }
         else
         { // 2 > 1 > 0
            B.set(B.getM02(), B.getM01(), -B.getM00(),
                  B.getM12(), B.getM11(), -B.getM10(),
                  B.getM22(), B.getM21(), -B.getM20());
            V.setUnsafe(sqrtTwoOverTwo * (qx + qz),
                        sqrtTwoOverTwo * (qy - qs),
                        sqrtTwoOverTwo * (qz - qx),
                        sqrtTwoOverTwo * (qs + qy));
         }
      }
      // @formatter:on
   }

   /**
    * Returns the output of this algorithm packaged as {@link SVD3DOutput}. It is updated every time a
    * decomposition is performed.
    *
    * @return the output the packaged output of this algorithm.
    */
   public SVD3DOutput getOutput()
   {
      return output;
   }

   /**
    * Returns the left side rotation of the decomposition.
    *
    * @return a quaternion representing the left side rotation of the decomposition.
    */
   public Quaternion getU()
   {
      return output.getU();
   }

   /**
    * Returns the vector containing the singular values in descending order.
    * <p>
    * Note that the last singular value may be negative. This allows for {@code U} and {@code V} to be
    * pure rotations.
    * </p>
    *
    * @return the singular values in descending order.
    */
   public Vector3D getW()
   {
      return output.getW();
   }

   /**
    * Returns the diagonal matrix containing the singular values in descending order.
    * <p>
    * Note that the last singular value may be negative. This allows for {@code U} and {@code V} to be
    * pure rotations.
    * </p>
    *
    * @param W the matrix in which to store diagonal matrix with the singular values. If {@code null},
    *          a new matrix is created and returned.
    * @return the diagonal matrix containing the singular values in descending order.
    */
   public Matrix3DBasics getW(Matrix3DBasics W)
   {
      return output.getW(W);
   }

   /**
    * Returns the right side rotation of the decomposition.
    *
    * @return a quaternion representing the right side rotation of the decomposition.
    */
   public Quaternion getV()
   {
      return output.getV();
   }

   /**
    * Returns the tolerance used by this calculator.
    *
    * @return the tolerance used by this calculator.
    */
   public double getTolerance()
   {
      return tolerance;
   }

   /**
    * Returns the maximum number of iterations allowed for the decomposition.
    *
    * @return the maximum number of iterations allowed for the decomposition.
    */
   public int getMaxIterations()
   {
      return maxIterations;
   }

   /**
    * Returns the number of iterations taken in the last decomposition.
    *
    * @return the number of iterations taken in the last decomposition.
    */
   public int getIterations()
   {
      return iterations;
   }

   /**
    * Returns whether this calculator is sorting the singular values in descending order.
    * 
    * @return whether this calculator is sorting the singular values in descending order.
    */
   public boolean getSortDescendingOrder()
   {
      return sortDescendingOrder;
   }

   /**
    * Class used to package the result of the decomposition.
    *
    * @author Sylvain Bertrand
    */
   public static class SVD3DOutput
   {
      private final Quaternion U = new Quaternion();
      private final Vector3D W = new Vector3D();
      private final Quaternion V = new Quaternion();

      /**
       * Performs a deep copy of {@code other} into {@code this}.
       *
       * @param other the other output to copy. Not modified.
       */
      public void set(SVD3DOutput other)
      {
         U.set(other.U);
         W.set(other.W);
         V.set(other.V);
      }

      /**
       * Resets {@code U} and {@code V} to neutral quaternions and {@code W} to (1, 1, 1).
       */
      public void setIdentity()
      {
         U.setToZero();
         W.set(1.0, 1.0, 1.0);
         V.setToZero();
      }

      /**
       * Resets {@code U} and {@code V} to neutral quaternions and {@code W} to (0, 0, 0).
       */
      public void setToZero()
      {
         U.setToZero();
         W.setToZero();
         V.setToZero();
      }

      /**
       * Sets {@code U}, {@code W}, and {@code V} to NaN.
       */
      public void setToNaN()
      {
         U.setToNaN();
         W.setToNaN();
         V.setToNaN();
      }

      /**
       * Performs the following operation: <tt>(U W V<sup>T</sup>)<sup>T</sup><tt> such that:
       * <ul>
       * <li><tt>U<sup>new</sup> = V<sup>old</sup></tt>
       * <li><tt>V<sup>new</sup> = U<sup>old</sup></tt>
       * </ul>
       */
      public void transpose()
      {
         double vx = V.getX();
         double vy = V.getY();
         double vz = V.getZ();
         double vs = V.getS();
         V.set(U);
         U.setUnsafe(vx, vy, vz, vs);
      }

      /**
       * Performs the following operation: <tt>(U W V<sup>T</sup>)<sup>-1</sup><tt> such that:
       * <ul>
       * <li><tt>U<sup>new</sup> = V<sup>old</sup></tt>
       * <li><tt>W<sub>i</sub><sup>new</sup> = 1/W<sub>i</sub><sup>old</sup></tt> &forall;i&in;[0;2]
       * <li><tt>V<sup>new</sup> = U<sup>old</sup></tt>
       * </ul>
       */
      public void invert()
      {
         if (W.getX() < Matrix3DTools.EPS_INVERT || W.getY() < Matrix3DTools.EPS_INVERT || Math.abs(W.getZ()) < Matrix3DTools.EPS_INVERT)
            throw new SingularMatrixException(W.getX(), 0, 0, 0, W.getY(), 0, 0, 0, W.getZ());
         transpose();
         W.setX(1.0 / W.getX());
         W.setY(1.0 / W.getY());
         W.setZ(1.0 / W.getZ());
      }

      /**
       * Returns the left side rotation of the decomposition.
       *
       * @return a quaternion representing the left side rotation of the decomposition.
       */
      public Quaternion getU()
      {
         return U;
      }

      /**
       * The vector containing the singular values in descending order.
       * <p>
       * Note that the last singular value may be negative. This allows for {@code U} and {@code V} to be
       * pure rotations.
       * </p>
       *
       * @return the singular values in descending order.
       */
      public Vector3D getW()
      {
         return W;
      }

      /**
       * The diagonal matrix containing the singular values in descending order.
       * <p>
       * Note that the last singular value may be negative. This allows for {@code U} and {@code V} to be
       * pure rotations.
       * </p>
       *
       * @param W the matrix in which to store diagonal matrix with the singular values. If {@code null},
       *          a new matrix is created and returned.
       * @return the diagonal matrix containing the singular values in descending order.
       */
      public Matrix3DBasics getW(Matrix3DBasics W)
      {
         if (W == null)
            W = new Matrix3D();
         W.setToDiagonal(this.W);
         return W;
      }

      /**
       * Returns the right side rotation of the decomposition.
       *
       * @return a quaternion representing the right side rotation of the decomposition.
       */
      public Quaternion getV()
      {
         return V;
      }

      @Override
      public String toString()
      {
         return "U = " + U + ", W = " + W + ", V = " + V;
      }
   }
}
