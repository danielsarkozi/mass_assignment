package massim.javaagents;


public class Tile{

    public enum Type{
        EMPTY, OBSTACLE, THING
    }

    private Type type;
    int x;
    int y;

    public Tile( Type type, int x, int y ){
        this.type = type;
        this.x = x;
        this.y = y;
    }

    


}