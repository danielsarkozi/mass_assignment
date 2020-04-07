package massim.javaagents;

import java.util.ArrayList;

public class AgentMap {

    private int origoX;
    private int origoY;

    private int relativeX;
    private int relativeY;

    private ArrayList<Tile> gameMap;

    public AgentMap( ){
        this.origoX = 0;
        this.origoY = 0;
        this.gameMap = new ArrayList<Tile>();
    }

    public int getRelX(){
        return this.relativeX;
    }

    public int getRelY(){
        return this.relativeY;
    }

    public int getOrigoX(){
        return this.origoX;
    }

    public int getOrigoY(){
        return this.origoY;
    }

    public void setRelX( int x ){
        this.relativeX = x;
    }

    public void setRelY(int y){
        this.relativeY = y;
    }

    public void addTile(Tile tile){
        this.gameMap.add(tile);
    }

    public int getDistance( int destX, int destY ){
        return Math.abs(relativeX-destX) + Math.abs(relativeY-destY);
    }

}