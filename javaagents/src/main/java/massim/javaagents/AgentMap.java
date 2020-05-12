package massim.javaagents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import massim.javaagents.Coord;

import massim.javaagents.Tile.Type;

public class AgentMap {

    private int origoX;
    private int origoY;

    private int relativeX;
    private int relativeY;

    private int plannedX;
    private int plannedY;

    private List<String> encountered;
    private HashMap<Coord, Tile> gameMap;
    private HashMap<String, Coord> teamMates;
    private HashMap<String, String> connections = new HashMap<>();

    public AgentMap( ){
        this.origoX = 0;
        this.origoY = 0;
        this.relativeX = 0;
        this.relativeY = 0;
        this.plannedX = 0;
        this.plannedY = 0;
        this.gameMap = new HashMap<Coord, Tile>();
        this.teamMates = new HashMap<>();
        this.encountered = new ArrayList<>();
    }

    public void addEncountered( String name ){
        this.encountered.add(name);
    }

    public List<String> getEncountered(){
        return this.encountered;
    }

    public void addConnection( String connection, String name ){
        this.connections.put(connection, name);
    }

    public HashMap<String,String> getConnections(){
        return this.connections;
    }

    public void addTeamMate( String teamMate, Coord coord ){
        this.teamMates.put(teamMate, coord);
    }

    public HashMap<String, Coord> getTeamMates(){
        return this.teamMates;
    }

    public int getRelX(){
        return this.relativeX;
    }

    public int getRelY(){
        return this.relativeY;
    }

    public int getPlannedX(){
        return this.plannedX;
    }

    public int getPlannedY(){
        return this.plannedY;
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

    public void setPlannedX( int x ){
        this.plannedX = x;
    }

    public void setPlannedY( int y ){
        this.plannedY = y;
    }

    public void planStep( int x, int y ){
        this.plannedX = x;
        this.plannedY = y;
    }

    public void executePlan(){
        this.relativeX = this.plannedX;
        this.relativeY = this.plannedY;
    }

    public void addTile(Tile tile){
        this.gameMap.put(new Coord(tile.getX(), tile.getY()), tile);
    }

    public int getDistance( int destX, int destY ){
        return Math.abs(destX) + Math.abs(destY);
    }

    public int getDistanceFromSpawn( int destX, int destY ){
        return Math.abs(relativeX-destX) + Math.abs(relativeY-destY);
    }

    public Tile getClosestElement(Type type){
        List<Tile> ret_tiles = getTilesByType(type);
        if(ret_tiles.size() > 0 ){
            Tile ret_tile = ret_tiles.get(0);
            int min = getDistance(ret_tile.getX(), ret_tile.getX());
            for (Tile tile : getTilesByType(type)){
                if( getDistance(tile.getX(), tile.getX()) < min ){
                    min = getDistance(tile.getX(), tile.getX());
                    ret_tile = tile;
                }
            }
            return ret_tile;
        }else{
            return null;
        }
    }

    public Tile getClosestElement(Type type, String name){
        List<Tile> ret_tiles = getTilesByType(type, name);
        if(ret_tiles.size() > 0 ){
            Tile ret_tile = ret_tiles.get(0);
            int min = getDistance(ret_tile.getX(), ret_tile.getX());
            for (Tile tile : getTilesByType(type)){
                if( getDistance(tile.getX(), tile.getX()) < min && tile.getName().equals(name)){
                    min = getDistance(tile.getX(), tile.getX());
                    ret_tile = tile;
                }
            }
            return ret_tile;
        }else{
            return null;
        }
        
    }

    public Tile getTileByCoord( Coord coord ){
        return this.gameMap.get(coord);
    }

    public ArrayList<Tile> getTilesByType( Tile.Type type ){
        ArrayList<Tile> tileList = new ArrayList<>();
        for( Tile tile : this.gameMap.values() ){
            if(type == tile.getType()){
                tileList.add(tile);
            }
        }
        return tileList;
    }

    public ArrayList<Tile> getTilesByType( Tile.Type type, String name ){
        ArrayList<Tile> tileList = new ArrayList<>();
        for( Tile tile : this.gameMap.values() ){
            if(type == tile.getType() && name.equals(tile.getName())){
                tileList.add(tile);
            }
        }
        return tileList;
    }

    public void getAgentMap(){
        for(Tile tile : this.gameMap.values()){
            System.out.println( "(" + tile.getX() + ", " + tile.getY() + ") - " + tile.getType() );
        }
    }

    public boolean hasTile( Tile.Type type ){
        for( Tile tile : this.gameMap.values() ){
            if( type == tile.getType() ){
                return true;
            }
        }
        return false;
    }

    public boolean hasTile( Tile.Type type, String name ){
        for( Tile tile : this.gameMap.values() ){
            if( type == tile.getType() && name.equals(tile.getName()) ){
                return true;
            }
        }
        return false;
    }

}