package massim.javaagents;

public class Coord {
    public Coord(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public Coord() {}

    public int getX()
    {
        return x;
    }

    public int getY()
    {
        return y;
    }

    public int distanceTo(Coord coord)
    {
        return Math.abs(coord.x - x) + Math.abs(coord.y - y);
    }

    private int x;
    private int y;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coord)) return false;
        Coord key = (Coord) o;
        return x == key.x && y == key.y;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        return result;
    }
}