package massim.javaagents;


public class Tile{

    public enum Type{
        EMPTY, OBSTACLE, GOAL, AGENT, DISPENSER
    }

    private Type type;
    private int x;
    private int y;
    private String name;

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
    
    public String getName(){
        return this.name;
    }

    public void setName( String name ){
        this.name = name;
    }
}