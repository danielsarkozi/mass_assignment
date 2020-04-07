package massim.javaagents;

import java.util.ArrayList;
import java.util.List;

import massim.javaagents.Tile.Type;;

public class AgentMap {

    private int origoX;
    private int origoY;

    private int relativeX;
    private int relativeY;

    private ArrayList<Tile> gameMap;

    public AgentMap( ){
        this.origoX = 0;
        this.origoY = 0;
        this.relativeX = 0;
        this.relativeY = 0;
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

    public List<Tile> getStoredElementsByType(Type type){
        List<Tile> ret = new ArrayList<Tile>();

        for( Tile tile : this.gameMap ){
            if(tile.getType() == type){
                ret.add(tile);
            }
        }

        return ret;
    }

    public Tile getClosestElement(Type tipe){
        Tile ret_tile = getStoredElementsByType(tipe).get(0);
        int min = getDistance(ret_tile.getX(), ret_tile.getX());
        for (Tile tile : getStoredElementsByType(tipe)){
            if( getDistance(tile.getX(), tile.getX()) < min ){
                min = getDistance(tile.getX(), tile.getX());
                ret_tile = tile;
            }
        }
        return ret_tile;
    }

}