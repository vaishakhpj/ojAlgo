/*
 * Copyright 1997-2014 Optimatika (www.optimatika.se)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.ojalgo.array;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Iterator;

import org.ojalgo.access.Access1D;
import org.ojalgo.access.AccessUtils;
import org.ojalgo.access.Iterator1D;
import org.ojalgo.array.DenseArray.DenseFactory;
import org.ojalgo.array.SegmentedArray.SegmentedFactory;
import org.ojalgo.constant.PrimitiveMath;
import org.ojalgo.function.BinaryFunction;
import org.ojalgo.function.UnaryFunction;
import org.ojalgo.function.VoidFunction;
import org.ojalgo.scalar.BigScalar;
import org.ojalgo.scalar.ComplexNumber;
import org.ojalgo.scalar.PrimitiveScalar;
import org.ojalgo.scalar.RationalNumber;
import org.ojalgo.scalar.Scalar;
import org.ojalgo.type.TypeUtils;

/**
 * Sparse array - maps long to int.
 *
 * @author apete
 */
public final class SparseArray<N extends Number> extends BasicArray<N> {

    static abstract class SparseFactory<N extends Number> extends BasicFactory<N> {

        @Override
        final SparseFactory<N> getSparseFactory() {
            return this;
        }

        abstract SparseArray<N> make(long count);

        @Override
        final SparseArray<N> makeStructuredZero(final long... structure) {
            return this.make(AccessUtils.count(structure));
        }

        @Override
        final SparseArray<N> makeToBeFilled(final long... structure) {
            return this.make(AccessUtils.count(structure));
        }

    }

    private static final int INITIAL_CAPACITY = 7;

    static final SparseFactory<BigDecimal> BIG = new SparseFactory<BigDecimal>() {

        @Override
        DenseFactory<BigDecimal> getDenseFactory() {
            return BigArray.FACTORY;
        }

        @Override
        SegmentedFactory<BigDecimal> getSegmentedFactory() {
            return SegmentedArray.BIG;
        }

        @Override
        SparseArray<BigDecimal> make(final long count) {
            return SparseArray.makeBig(count);
        }

    };

    static final SparseFactory<ComplexNumber> COMPLEX = new SparseFactory<ComplexNumber>() {

        @Override
        DenseFactory<ComplexNumber> getDenseFactory() {
            return ComplexArray.FACTORY;
        }

        @Override
        SegmentedFactory<ComplexNumber> getSegmentedFactory() {
            return SegmentedArray.COMPLEX;
        }

        @Override
        SparseArray<ComplexNumber> make(final long count) {
            return SparseArray.makeComplex(count);
        }

    };

    static final SparseFactory<Double> PRIMITIVE = new SparseFactory<Double>() {

        @Override
        DenseFactory<Double> getDenseFactory() {
            return PrimitiveArray.FACTORY;
        }

        @Override
        SegmentedFactory<Double> getSegmentedFactory() {
            return SegmentedArray.PRIMITIVE;
        }

        @Override
        SparseArray<Double> make(final long count) {
            return SparseArray.makePrimitive(count);
        }

    };

    static final SparseFactory<RationalNumber> RATIONAL = new SparseFactory<RationalNumber>() {

        @Override
        DenseFactory<RationalNumber> getDenseFactory() {
            return RationalArray.FACTORY;
        }

        @Override
        SegmentedFactory<RationalNumber> getSegmentedFactory() {
            return SegmentedArray.RATIONAL;
        }

        @Override
        SparseArray<RationalNumber> make(final long count) {
            return SparseArray.makeRational(count);
        }

    };

    public static SparseArray<BigDecimal> makeBig(final long count) {
        return new SparseArray<>(count, new BigArray(INITIAL_CAPACITY), BigScalar.ZERO);
    }

    public static SparseArray<ComplexNumber> makeComplex(final long count) {
        return new SparseArray<>(count, new ComplexArray(INITIAL_CAPACITY), ComplexNumber.ZERO);
    }

    public static SparseArray<Double> makePrimitive(final long count) {
        return new SparseArray<>(count, new PrimitiveArray(INITIAL_CAPACITY), PrimitiveScalar.ZERO);
    }

    public static SparseArray<RationalNumber> makeRational(final long count) {
        return new SparseArray<>(count, new RationalArray(INITIAL_CAPACITY), RationalNumber.ZERO);
    }

    /**
     * The actual number of nonzwero elements
     */
    private int myActualLength = 0;
    private final long myCount;
    private long[] myIndices;
    private DenseArray<N> myValues;

    private final N myZeroNumber;
    private final Scalar<N> myZeroScalar;
    private final double myZeroValue;

    SparseArray(final long count, final DenseArray<N> values, final Scalar<N> zero) {

        super();

        myCount = count;

        myIndices = new long[values.size()];
        myValues = values;

        myZeroScalar = zero;
        myZeroNumber = zero.getNumber();
        myZeroValue = zero.doubleValue();
    }

    public final long count() {
        return myCount;
    }

    @Override
    public double doubleValue(final long index) {
        final int tmpIndex = this.index(index);
        if (tmpIndex >= 0) {
            return myValues.doubleValue(tmpIndex);
        } else {
            return myZeroValue;
        }
    }

    @Override
    public void fillAll(final N value) {

        if (TypeUtils.isZero(value.doubleValue())) {

            myValues.fillAll(myZeroNumber);

        } else {

            // Bad idea...

            final int tmpSize = (int) this.count();

            if (tmpSize != myIndices.length) {
                myIndices = AccessUtils.makeIncreasingRange(0L, tmpSize);
                myValues = myValues.newInstance(tmpSize);
                myActualLength = tmpSize;
            }

            myValues.fillAll(value);
        }
    }

    @Override
    public void fillRange(final long first, final long limit, final N value) {
        this.fill(first, limit, 1L, value);
    }

    @Override
    public N get(final long index) {
        final int tmpIndex = this.index(index);
        if (tmpIndex >= 0) {
            return myValues.get(tmpIndex);
        } else {
            return myZeroNumber;
        }
    }

    public boolean isAbsolute(final long index) {
        final int tmpIndex = this.index(index);
        if (tmpIndex >= 0) {
            return myValues.isAbsolute(tmpIndex);
        } else {
            return true;
        }
    }

    public boolean isInfinite(final long index) {
        final int tmpIndex = this.index(index);
        if (tmpIndex >= 0) {
            return myValues.isInfinite(tmpIndex);
        } else {
            return false;
        }
    }

    public boolean isNaN(final long index) {
        final int tmpIndex = this.index(index);
        if (tmpIndex >= 0) {
            return myValues.isNaN(tmpIndex);
        } else {
            return false;
        }
    }

    public boolean isPositive(final long index) {
        final int tmpIndex = this.index(index);
        if (tmpIndex >= 0) {
            return myValues.isPositive(tmpIndex);
        } else {
            return false;
        }
    }

    public boolean isReal(final long index) {
        final int tmpIndex = this.index(index);
        if (tmpIndex >= 0) {
            return myValues.isReal(tmpIndex);
        } else {
            return true;
        }
    }

    public boolean isZero(final long index) {
        final int tmpIndex = this.index(index);
        if (tmpIndex >= 0) {
            return myValues.isZero(tmpIndex);
        } else {
            return true;
        }
    }

    @Override
    public Iterator<N> iterator() {
        return new Iterator1D<>(this);
    }

    @Override
    public void set(final long index, final double value) {

        final int tmpIndex = this.index(index);

        if (tmpIndex >= 0) {
            // Existing value, just update

            // values[tmpIndex] = value;
            myValues.set(tmpIndex, value);

        } else {
            // Not existing value, insert new

            final long[] tmpOldIndeces = myIndices;

            final int tmpInsInd = -(tmpIndex + 1);

            if ((myActualLength + 1) <= tmpOldIndeces.length) {
                // No need to grow the backing arrays

                for (int i = myActualLength; i > tmpInsInd; i--) {
                    tmpOldIndeces[i] = tmpOldIndeces[i - 1];
                    // values[i] = values[i - 1];
                    myValues.set(i, myValues.doubleValue(i - 1));
                }
                tmpOldIndeces[tmpInsInd] = index;
                // values[tmpInsInd] = value;
                myValues.set(tmpInsInd, value);

                myActualLength++;

            } else {
                // Needs to grow the backing arrays

                final int tmpCapacity = tmpOldIndeces.length * 2;
                final long[] tmpIndices = new long[tmpCapacity];
                final DenseArray<N> tmpValues = myValues.newInstance(tmpCapacity);

                for (int i = 0; i < tmpInsInd; i++) {
                    tmpIndices[i] = tmpOldIndeces[i];
                    tmpValues.set(i, myValues.doubleValue(i));
                }
                tmpIndices[tmpInsInd] = index;
                tmpValues.set(tmpInsInd, value);
                for (int i = tmpInsInd; i < tmpOldIndeces.length; i++) {
                    tmpIndices[i + 1] = tmpOldIndeces[i];
                    tmpValues.set(i + 1, myValues.doubleValue(i));
                }
                for (int i = tmpOldIndeces.length + 1; i < tmpIndices.length; i++) {
                    tmpIndices[i] = Long.MAX_VALUE;
                }

                myIndices = tmpIndices;
                myValues = tmpValues;
                myActualLength++;
            }
        }
    }

    @Override
    public void set(final long index, final Number value) {

        final int tmpIndex = this.index(index);

        if (tmpIndex >= 0) {
            // Existing value, just update

            // values[tmpIndex] = value;
            myValues.set(tmpIndex, value);

        } else {
            // Not existing value, insert new

            final long[] tmpOldIndeces = this.myIndices;

            final int tmpInsInd = -(tmpIndex + 1);

            if ((myActualLength + 1) <= tmpOldIndeces.length) {
                // No need to grow the backing arrays

                for (int i = myActualLength; i > tmpInsInd; i--) {
                    tmpOldIndeces[i] = tmpOldIndeces[i - 1];
                    // values[i] = values[i - 1];
                    myValues.set(i, myValues.get(i - 1));
                }
                tmpOldIndeces[tmpInsInd] = index;
                // values[tmpInsInd] = value;
                myValues.set(tmpInsInd, value);

                myActualLength++;

            } else {
                // Needs to grow the backing arrays

                final int tmpCapacity = tmpOldIndeces.length * 2;
                final long[] tmpIndices = new long[tmpCapacity];
                final DenseArray<N> tmpValues = myValues.newInstance(tmpCapacity);

                for (int i = 0; i < tmpInsInd; i++) {
                    tmpIndices[i] = tmpOldIndeces[i];
                    // tmpValues[i] = values[i];
                    tmpValues.set(i, myValues.get(i));
                }
                tmpIndices[tmpInsInd] = index;
                // tmpValues[tmpInsInd] = value;
                tmpValues.set(tmpInsInd, value);
                for (int i = tmpInsInd; i < tmpOldIndeces.length; i++) {
                    tmpIndices[i + 1] = tmpOldIndeces[i];
                    // tmpValues[i + 1] = values[i];
                    tmpValues.set(i + 1, myValues.get(i));

                }
                for (int i = tmpOldIndeces.length + 1; i < tmpIndices.length; i++) {
                    tmpIndices[i] = Long.MAX_VALUE;
                }

                myIndices = tmpIndices;
                myValues = tmpValues;
                myActualLength++;
            }
        }
    }

    @Override
    protected void exchange(final long firstA, final long firstB, final long step, final long count) {

        if (this.isPrimitive()) {

            long tmpIndexA = firstA;
            long tmpIndexB = firstB;

            double tmpVal;

            for (long i = 0; i < count; i++) {

                tmpVal = this.doubleValue(tmpIndexA);
                this.set(tmpIndexA, this.doubleValue(tmpIndexB));
                this.set(tmpIndexB, tmpVal);

                tmpIndexA += step;
                tmpIndexB += step;
            }

        } else {

            long tmpIndexA = firstA;
            long tmpIndexB = firstB;

            N tmpVal;

            for (long i = 0; i < count; i++) {

                tmpVal = this.get(tmpIndexA);
                this.set(tmpIndexA, this.get(tmpIndexB));
                this.set(tmpIndexB, tmpVal);

                tmpIndexA += step;
                tmpIndexB += step;
            }
        }
    }

    @Override
    protected void fill(final long first, final long limit, final long step, final N value) {
        int tmpFirst = this.index(first);
        if (tmpFirst < 0) {
            tmpFirst = -tmpFirst + 1;
        }
        int tmpLimit = this.index(limit);
        if (tmpLimit < 0) {
            tmpLimit = -tmpLimit + 1;
        }
        if (this.isPrimitive()) {
            final double tmpValue = value.doubleValue();
            for (int i = tmpFirst; i < tmpLimit; i++) {
                myValues.set(i, tmpValue);
            }
        } else {
            for (int i = tmpFirst; i < tmpLimit; i++) {
                myValues.set(i, value);
            }
        }
    }

    @Override
    protected long indexOfLargest(final long first, final long limit, final long step) {

        double tmpVal = PrimitiveMath.ZERO;
        long retVal = Long.MIN_VALUE;

        for (int i = 0; i < myIndices.length; i++) {
            final long tmpIndex = myIndices[i];
            if ((tmpIndex >= first) && (tmpIndex < limit)) {
                if (((tmpIndex - first) % step) == 0L) {
                    if (myValues.doubleValue(i) > tmpVal) {
                        tmpVal = Math.abs(myValues.doubleValue(i));
                        retVal = tmpIndex;
                    }
                }
            }
        }

        return retVal;
    }

    @Override
    protected boolean isZeros(final long first, final long limit, final long step) {

        boolean retVal = true;

        for (int i = 0; retVal && (i < myIndices.length); i++) {
            final long tmpIndex = myIndices[i];
            if ((tmpIndex >= first) && (tmpIndex < limit)) {
                if (((tmpIndex - first) % step) == 0L) {
                    retVal &= myValues.isZero(i);
                }
            }
        }

        return retVal;
    }

    @Override
    protected void modify(final long first, final long limit, final long step, final Access1D<N> left, final BinaryFunction<N> function) {

        final double tmpZeroValue = function.invoke(PrimitiveMath.ZERO, PrimitiveMath.ZERO);

        if (TypeUtils.isZero(tmpZeroValue)) {

            for (int i = 0; i < myIndices.length; i++) {
                final long tmpIndex = myIndices[i];
                if ((tmpIndex >= first) && (tmpIndex < limit)) {
                    if (((tmpIndex - first) % step) == 0L) {
                        myValues.modify(i, left, function);
                    }
                }
            }

        } else {

            throw new IllegalArgumentException("SparseArray zero modification!");
        }
    }

    @Override
    protected void modify(final long first, final long limit, final long step, final BinaryFunction<N> function, final Access1D<N> right) {

        final double tmpZeroValue = function.invoke(PrimitiveMath.ZERO, PrimitiveMath.ZERO);

        if (TypeUtils.isZero(tmpZeroValue)) {

            for (int i = 0; i < myIndices.length; i++) {
                final long tmpIndex = myIndices[i];
                if ((tmpIndex >= first) && (tmpIndex < limit) && (((tmpIndex - first) % step) == 0L)) {
                    myValues.modify(i, function, right);
                }
            }

        } else {

            throw new IllegalArgumentException("SparseArray zero modification!");
        }
    }

    @Override
    protected void modify(final long first, final long limit, final long step, final UnaryFunction<N> function) {

        final double tmpZeroValue = function.invoke(PrimitiveMath.ZERO);

        if (TypeUtils.isZero(tmpZeroValue)) {

            for (int i = 0; i < myIndices.length; i++) {
                final long tmpIndex = myIndices[i];
                if ((tmpIndex >= first) && (tmpIndex < limit) && (((tmpIndex - first) % step) == 0L)) {
                    myValues.modify(i, function);
                }
            }

        } else {

            throw new IllegalArgumentException("SparseArray zero modification!");
        }
    }

    @Override
    protected Scalar<N> toScalar(final long index) {
        final int tmpIndex = this.index(index);
        if (tmpIndex >= 0) {
            return myValues.toScalar(tmpIndex);
        } else {
            return myZeroScalar;
        }
    }

    @Override
    protected void visit(final long first, final long limit, final long step, final VoidFunction<N> visitor) {
        for (int i = 0; i < myIndices.length; i++) {
            final long tmpIndex = myIndices[i];
            if ((tmpIndex >= first) && (tmpIndex < limit) && (((tmpIndex - first) % step) == 0L)) {
                myValues.visit(i, visitor);
            }
        }
    }

    final DenseArray<N> densify() {

        final DenseArray<N> retVal = myValues.newInstance((int) this.count());

        if (this.isPrimitive()) {
            for (int i = 0; i < myActualLength; i++) {
                retVal.set(myIndices[i], myValues.doubleValue(i));
            }
        } else {
            for (int i = 0; i < myActualLength; i++) {
                retVal.set(myIndices[i], myValues.get(i));
            }
        }

        return retVal;
    }

    final int index(final long index) {
        return Arrays.binarySearch(myIndices, 0, myActualLength, index);
    }

    @Override
    boolean isPrimitive() {
        return myValues.isPrimitive();
    }

}