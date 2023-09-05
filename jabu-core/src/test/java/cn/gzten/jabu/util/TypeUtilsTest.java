package cn.gzten.jabu.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class TypeUtilsTest {

    @Test
    void isLong() {
        // test the TypeUtils.isLong
        assertTrue(TypeUtils.isLong(Long.class));
        assertTrue(TypeUtils.isLong(long.class));
        assertFalse(TypeUtils.isLong(Integer.class));
        assertFalse(TypeUtils.isLong(int.class));
        assertFalse(TypeUtils.isLong(Double.class));
    }

    @Test
    void isInt() {
        // test the TypeUtils.isInt
        assertTrue(TypeUtils.isInt(Integer.class));
        assertTrue(TypeUtils.isInt(int.class));
        assertFalse(TypeUtils.isInt(Long.class));
        assertFalse(TypeUtils.isInt(long.class));
        assertFalse(TypeUtils.isInt(Double.class));
    }

    @Test
    void isShort() {
        // test the TypeUtils.isShort
        assertTrue(TypeUtils.isShort(Short.class));
        assertTrue(TypeUtils.isShort(short.class));
        assertFalse(TypeUtils.isShort(Long.class));
        assertFalse(TypeUtils.isShort(long.class));
        assertFalse(TypeUtils.isShort(Double.class));
    }

    @Test
    void isByte() {
        // test the TypeUtils.isByte
        assertTrue(TypeUtils.isByte(Byte.class));
        assertTrue(TypeUtils.isByte(byte.class));
        assertFalse(TypeUtils.isByte(Long.class));
        assertFalse(TypeUtils.isByte(long.class));
        assertFalse(TypeUtils.isByte(Double.class));
    }

    @Test
    void isDouble() {
        // test the TypeUtils.isDouble
        assertTrue(TypeUtils.isDouble(Double.class));
        assertTrue(TypeUtils.isDouble(double.class));
        assertFalse(TypeUtils.isDouble(Long.class));
        assertFalse(TypeUtils.isDouble(long.class));
        assertFalse(TypeUtils.isDouble(Integer.class));
        assertFalse(TypeUtils.isDouble(int.class));
    }

    @Test
    void isFloat() {
        // test the TypeUtils.isFloat
        assertTrue(TypeUtils.isFloat(Float.class));
        assertTrue(TypeUtils.isFloat(float.class));
        assertFalse(TypeUtils.isFloat(Long.class));
        assertFalse(TypeUtils.isFloat(long.class));
        assertFalse(TypeUtils.isFloat(Integer.class));
        assertFalse(TypeUtils.isFloat(int.class));
    }

    /**
     * write test cases for TypeUtils.convertBasicTypes, to cover all the logic branches.
     */
    @Test
    void testConvertBasicTypes() {
        assertEquals(1, TypeUtils.convertBasicTypes("1", Integer.class));
        assertEquals(1, TypeUtils.convertBasicTypes("1", int.class));
        assertEquals(1L, TypeUtils.convertBasicTypes("1", Long.class));
        assertEquals(1L, TypeUtils.convertBasicTypes("1", long.class));
        assertEquals(1.0, TypeUtils.convertBasicTypes("1", Double.class));
        assertEquals(1.0, TypeUtils.convertBasicTypes("1", double.class));
        assertEquals(true, TypeUtils.convertBasicTypes("true", Boolean.class));
        assertEquals(true, TypeUtils.convertBasicTypes("true", boolean.class));
        assertEquals("hey", TypeUtils.convertBasicTypes("hey", String.class));
        assertEquals(new BigDecimal("1.23"), TypeUtils.convertBasicTypes("1.23", BigDecimal.class));
        assertEquals(new BigInteger("123"), TypeUtils.convertBasicTypes("123", BigInteger.class));

        assertEquals(null, TypeUtils.convertBasicTypes(null, String.class));
        assertEquals(null, TypeUtils.convertBasicTypes(null, Integer.class));
        assertEquals(null, TypeUtils.convertBasicTypes(null, Long.class));
        assertEquals(null, TypeUtils.convertBasicTypes(null, Double.class));
        assertEquals(null, TypeUtils.convertBasicTypes(null, Boolean.class));

        assertEquals("", TypeUtils.convertBasicTypes("", String.class));
        assertEquals(null, TypeUtils.convertBasicTypes("", Integer.class));
        assertEquals(null, TypeUtils.convertBasicTypes("", Long.class));
        assertEquals(null, TypeUtils.convertBasicTypes("", Double.class));
        assertEquals(null, TypeUtils.convertBasicTypes("", Boolean.class));

        assertEquals("1.0", TypeUtils.convertBasicTypes("1.0", String.class));
        assertThrows(NumberFormatException.class, () -> TypeUtils.convertBasicTypes("1.0", Integer.class));
        assertThrows(NumberFormatException.class, () -> TypeUtils.convertBasicTypes("1.0", Long.class));
        assertEquals(1.0d, TypeUtils.convertBasicTypes("1.0", Double.class));

        assertEquals(1, TypeUtils.convertBasicTypes(1L, int.class));

        Long x = 1L;
        System.out.println(x instanceof Number);
    }

}