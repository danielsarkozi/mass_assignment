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

    private int x;
    private int y;
}