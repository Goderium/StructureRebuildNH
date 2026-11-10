package com.Goderium.StructureRebuildNH.util;

/** Inclusive world-coordinate volume. */
public final class Bounds {
    public final int minX, minY, minZ, maxX, maxY, maxZ;
    public Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
    }
    public static Bounds point(int x, int y, int z) { return new Bounds(x, y, z, x, y, z); }
    public Bounds union(Bounds b) {
        return new Bounds(Math.min(minX, b.minX), Math.min(minY, b.minY), Math.min(minZ, b.minZ),
            Math.max(maxX, b.maxX), Math.max(maxY, b.maxY), Math.max(maxZ, b.maxZ));
    }
    /** One enclosing cuboid, including empty space between individual structure pieces. */
    public static Bounds enclosing(Iterable<Bounds> pieces) {
        Bounds result = null;
        for (Bounds piece : pieces) result = result == null ? piece : result.union(piece);
        return result;
    }
    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }
    public boolean intersects(Bounds b) {
        return maxX >= b.minX && minX <= b.maxX && maxY >= b.minY && minY <= b.maxY
            && maxZ >= b.minZ && minZ <= b.maxZ;
    }
    public long volume() {
        return Math.multiplyExact(Math.multiplyExact((long) maxX - minX + 1, (long) maxY - minY + 1),
            (long) maxZ - minZ + 1);
    }
    @Override public String toString() {
        return "[" + minX + "," + minY + "," + minZ + "]..[" + maxX + "," + maxY + "," + maxZ + "]";
    }
    @Override public boolean equals(Object o) {
        if (!(o instanceof Bounds)) return false;
        Bounds b = (Bounds) o;
        return minX == b.minX && minY == b.minY && minZ == b.minZ && maxX == b.maxX && maxY == b.maxY && maxZ == b.maxZ;
    }
    @Override public int hashCode() { return (((minX * 31 + minY) * 31 + minZ) * 31 + maxX) * 31 * 31 + maxY * 31 + maxZ; }
}
