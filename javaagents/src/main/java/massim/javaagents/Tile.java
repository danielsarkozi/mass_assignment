package massim.javaagents;


public class Tile{

    public enum Type{
        EMPTY, OBSTACLE, ELEMENT, AGENT, DISPENSER
    }

    private Type type;
    private int x;
    private int y;

    public Tile( Type type, int x, int y ){
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public Type getType(){
        return this.type;
    }

	public int getX() {
		return this.x;
	}

    public int getY() {
		return this.y;
	}
}