package wormpex.data.util;

import java.util.Objects;

public class Pair {
    private String left;
    private String right;

    public Pair() {
    }

    public Pair(String left, String right) {
        this.left = left;
        this.right = right;
    }

    public static Pair of(String left, String right) {
        return new Pair(left, right);
    }

    public String getLeft() {
        return left;
    }

    public void setLeft(String left) {
        this.left = left;
    }

    public String getRight() {
        return right;
    }

    public void setRight(String right) {
        this.right = right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair pair = (Pair) o;
        return Objects.equals(left, pair.left) && Objects.equals(right, pair.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

    @Override
    public String toString() {
        return "Pair{" + "left='" + left + '\'' + ", right='" + right + '\'' + "}";
    }
}
