package cn.gzten.jabu.util;

import org.junit.jupiter.api.Test;

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
}