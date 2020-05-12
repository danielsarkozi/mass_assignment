package massim.javaagents.agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import eis.iilang.*;
import massim.javaagents.AgentMap;
import massim.javaagents.Coord;
import massim.javaagents.MailService;
import massim.javaagents.Tile;
import massim.javaagents.Tile.Type;
import massim.javaagents.TaskElement;

public class MagellanAgent extends Agent {

    /**
     * Constructor.
     * 
     * @param name    the agent's name
     * @param mailbox the mail facility
     */
    public final String MOVE = "move";
    public final String ATTACH = "attach";
    public final String DETACH = "detach";
    public final String ROTATE = "rotate";
    public final String CONNECT = "connect";
    public final String REQUEST = "request";
    public final String SUBMIT = "submit";
    public final String CLEAR = "clear";
    public final String DISCONNECT = "disconnect";
    public final String SKIP = "skip";

    private AgentMap map;
    private Objective currentObjective;
    private Direction prevDirection;
    private Percept currentTask;
    private TaskElement targetBlock = null;
    private TaskElement attachedBlock;
    private ArrayList<TaskElement> agentTasks = new ArrayList<TaskElement>();
    private int vision;
    private Action nextAction = null;
    
    private Map<String, Tile> attachedBlocks = new HashMap<String, Tile>();

    Set<Direction> directions = new HashSet<Direction>();

    enum Objective {
        Discovery, GetMaterial, DropMaterial
    }

    public enum Direction {
        N, E, W, S, OTHER
    }

    public MagellanAgent(final String name, final MailService mailbox) {
        super(name, mailbox);
        this.map = new AgentMap();
        this.currentObjective = Objective.Discovery;
        this.prevDirection = Direction.N;
        directions.add(Direction.N);
        directions.add(Direction.W);
        directions.add(Direction.E);
        directions.add(Direction.S);
    }

    @Override
    public void handlePercept(final Percept percept) {
        // System.out.println(this.getName() + " Magellan percept");
    }

    @Override
    public void handleMessage(Percept message, String sender) {
        System.out.println("asd");
    }

    /**
     * Sends a percept as a message to the given agent.
     * The receiver agent may fetch the message the next time it is stepped.
     * @param message the message to deliver
     * @param receiver the receiving agent
     * @param sender the agent sending the message
     */
    protected void sendMessage(Percept message, String receiver, String sender){
        mailbox.sendMessage(message, receiver, sender);
    }

    /**
     * Broadcasts a message to the entire team.
     * @param message the message to broadcast
     * @param sender the agent sending the message
     */
    void broadcast(Percept message, String sender){
        mailbox.broadcast(message, sender);
    }

    /**
     * Prints a message to std out prefixed with the agent's name.
     * @param message the message to say
     */
    void say(String message){
        System.out.println("[ " + this.getName() + " ]  " + message);
    }

    @Override
    public Action step() {
        if (nextAction != null)
        {
            Action act = nextAction;
            nextAction = null;
            return act;
        }
        // System.out.println(this.getName() + " Magellan step");
        final List<Percept> percepts = getPercepts();
        percepts.stream().filter(p -> p.getName().equals("step")).findAny().ifPresent(p -> {
            final Parameter param = p.getParameters().getFirst();
            if (param instanceof Identifier)
                say("Step " + ((Identifier) param).getValue());
        });

        percepts.stream().filter(p -> p.getName().equals("lastActionResult")).findAny().ifPresent(p -> {
            String param = p.getParameters().getFirst().toString();
            if (param.equals("success"))
                this.map.executePlan();
        });

        List<Percept> goalList = percepts.stream().filter(p -> p.getName().equals("goal")).collect(Collectors.toList());
        List<Percept> obstacleList = percepts.stream().filter(p -> p.getName().equals("obstacle")).collect(Collectors.toList());
        List<Percept> thingList = percepts.stream().filter(p -> p.getName().equals("thing")).collect(Collectors.toList());

        for(Percept p : percepts){
            if(p.getName().equals("vision")){
                vision = Integer.parseInt(p.getParameters().get(0).toString());
            }
        }

        updateMap(obstacleList, thingList, goalList);
        updateCurrentObjective(percepts);

        say("I'm in " + this.currentObjective + " phase");

        switch (currentObjective) {
            case Discovery:
                return stepDiscovery(obstacleList, thingList);
            case GetMaterial:
                return executeStep(obstacleList, thingList, Type.DISPENSER, ATTACH);
            case DropMaterial:
                return executeStep(obstacleList, thingList, Type.GOAL, DETACH);
        }

        return new Action(MOVE, new Identifier("n"));
    }

    private void shareMap(String name){

    }

    private void shareCoordinates(String name){
        LinkedList<Parameter> paramList = new LinkedList<>();
        Parameter x = new Numeral(this.map.getRelX());
        Parameter y = new Numeral(this.map.getRelY());
        paramList.add(x);
        paramList.add(y);
        Percept message = new Percept("coords", paramList);
        sendMessage(message, name, this.getName());
    }

    private void updateCurrentObjective(List<Percept> percepts) {
        List<Percept> taskList = percepts.stream().filter(p -> p.getName().equals("task")).collect(Collectors.toList());

        if (taskList.isEmpty()) {
            currentTask = null;
            currentObjective = Objective.Discovery;

        } else if (currentTask == null || !taskList.contains(currentTask)) {

            say("Found a new task!");
            for( Percept task : taskList ){

                if(task.getClonedParameters().size() >= 3){

                    boolean acceptTask = true;
                    ArrayList<TaskElement> tempList = new ArrayList<TaskElement>();

                    if(!map.hasTile(Tile.Type.GOAL)){
                        acceptTask = false;
                    }
                    
                    for( Parameter param : (ParameterList) task.getClonedParameters().get(3)){
                        Function f = (Function) param;
                        int taskX = Integer.parseInt(f.getParameters().get(0).toString());
                        int taskY = Integer.parseInt(f.getParameters().get(1).toString());
                        String taskName = f.getParameters().get(2).toString();

                        TaskElement te = new TaskElement(taskX, taskY, taskName);
                        tempList.add(te);

                        if(!map.hasTile(Tile.Type.DISPENSER, taskName)){
                            acceptTask = false;
                            break;
                        }
                    }
    
                    if(acceptTask){
                        this.currentObjective = Objective.GetMaterial;
                        currentTask = task;
                        this.agentTasks = new ArrayList<TaskElement>(tempList);
                        if( this.agentTasks.size() > 0 ){
                            //this.targetBlock = this.agentTasks.get(0);
                            this.targetBlock = this.agentTasks.remove(0);
                        }
                        break;
                    }
                }
            }
        }
    }

    private Tile getTargetTile() {
        int minDist = 0;
        Tile closestTile = null;
        Set<String> blockType = new HashSet<>();
        for (TaskElement te : agentTasks)
        {
            blockType.add(te.getName());
        }

        for (Tile tile : map.getTilesByType(Tile.Type.DISPENSER))
        {
            if (blockType.contains(tile.getName()))
            {
                int dist = new Coord(map.getRelX(), map.getRelY()).distanceTo(new Coord(tile.getX(), tile.getY()));
                if (closestTile == null || dist < minDist)
                {
                    minDist = dist;
                    closestTile = tile;
                }
            }
        }
        return closestTile;
    }

    private Boolean shouldGoToGoal() {
        for (Map.Entry<String, Tile> pair : attachedBlocks.entrySet())
        {
            for (TaskElement te : agentTasks)
            {
                if (te.getName().equals(pair.getValue().getName()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private Action stepDiscovery(List<Percept> obstacleList, List<Percept> thingList) {
        Identifier dir = this.discover(obstacleList, thingList);
        moveAgent(dir.toString());
        return new Action(MOVE, dir);
    }

    /*
    public Action stepGetMaterial( List<Percept> obstacleList, List<Percept> thingList ){
        System.out.println("GET MATERIALLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLL");
        Tile goal = this.map.getClosestElement(Type.DISPENSER, this.targetBlock.getName());
        if (goal == null)
            return stepDiscovery(obstacleList, thingList);
        if (this.map.getDistanceFromSpawn(goal.getX(), goal.getY()) > 1) {
            Identifier dir = getRelativeIdentifierToElem(this.map.getRelX(), this.map.getRelY(), goal.getX(),
                    goal.getY(), obstacleList, thingList);
            moveAgent(dir.toString());
            return new Action(MOVE, dir);
        } else {
            this.currentObjective = Objective.DropMaterial;
            Direction dir = getRelativeDirectionToElem(this.map.getRelX(), this.map.getRelY(), goal.getX(), goal.getY());
            attachedBlock = new TaskElement(targetBlock);
            attachedBlock.setIsAttached(true);
            attachedBlock.setDirection(dir);
            agentTasks.add(attachedBlock);
            attachedBlocks.put(dir.toString(), goal);
            say("I grab " + targetBlock.getName() + " block");
            nextAction = new Action(ATTACH, getRelativeIdentifierToElem(this.map.getRelX(), this.map.getRelY(), goal.getX(),
                    goal.getY(), obstacleList, thingList));
            return new Action("request", new Identifier(decyphDir(dir)));
        }
    }
    */

    /*
    public Action dropMaterial(List<Percept> obstacleList, List<Percept> thingList){
        Tile goal = this.map.getClosestElement(Type.GOAL);
        if (goal == null)
            return stepDiscovery(obstacleList, thingList);
        if (this.map.getDistanceFromSpawn(goal.getX(), goal.getY()) > 1) {
            Identifier dir = getRelativeIdentifierToElem(this.map.getRelX(), this.map.getRelY(), goal.getX(),
                    goal.getY(), obstacleList, thingList);
            moveAgent(dir.toString());
            return new Action(MOVE, dir);
        } else {
            if(agentTasks.size() > 0){
                targetBlock = agentTasks.remove(0);
                this.currentTask = null;
            }
            say("I drop " + targetBlock.getName() + " block");
            return new Action(DETACH, new Identifier(decyphDir(targetBlock.getDirection())));
    }
    */

    public Action executeStep( List<Percept> obstacleList, List<Percept> thingList, Type type, String operation ){

        Tile tile = null;

        if( type == Type.DISPENSER ){
            tile = this.map.getClosestElement(Type.DISPENSER, this.targetBlock.getName());
        }else if( type == Type.GOAL ){
            tile = this.map.getClosestElement(Type.GOAL);
        }

        if ( tile == null){
            this.currentObjective = Objective.Discovery;
            return stepDiscovery(obstacleList, thingList);
        } else if (this.map.getDistanceFromSpawn(tile.getX(), tile.getY()) > 3) {
            Identifier dir = getRelativeIdentifierToElem(this.map.getRelX(), this.map.getRelY(), tile.getX(),
                    tile.getY(), obstacleList, thingList);
            moveAgent(dir.toString());
            return new Action(MOVE, dir);
        } else {

            int x = 0;
            int y = 0;
            boolean b = true;

            for (Percept percept : thingList){
                if( percept.getParameters().get(2).toString().equals("dispenser") && percept.getParameters().get(3).toString().equals(tile.getName()) ){
                    x = Integer.parseInt(percept.getParameters().get(0).toString());
                    y = Integer.parseInt(percept.getParameters().get(1).toString());
                    b = false;
                }
            }

            if(b){
                this.currentObjective = Objective.Discovery;
                return new Action(MOVE, discover(obstacleList, thingList));
            }

            if (this.map.getDistance(x, y) > 1){
                Identifier id = getRelativeIdentifierToElem(0, 0, x, y, obstacleList, thingList);;
                if( id != null ){
                    return new Action(MOVE, id);
                }
                return new Action(MOVE, new Identifier("Error"));
            }else{
                this.currentObjective = Objective.DropMaterial;
                Direction dir = getRelativeDirectionToElem(this.map.getRelX(), this.map.getRelY(), tile.getX(), tile.getY());
                attachedBlock = new TaskElement(targetBlock);
                attachedBlock.setIsAttached(true);
                attachedBlock.setDirection(dir);
                if(operation.equals(ATTACH)){
                    say("I grab " + targetBlock.getName() + " block");
                    nextAction = new Action(operation, new Identifier(decyphDir(dir));
                    return new Action("request", new Identifier(decyphDir(dir)));
                }else if(operation.equals(DETACH)){
                    return new Action( operation, getRelativeIdentifierToElem(this.map.getRelX(), this.map.getRelY(), tile.getX(),
                        tile.getY(), obstacleList, thingList));
                }else{
                    return new Action(MOVE, new Identifier("Error"));
                }
            }
        }
    }

    public Identifier getIdentifierByPercept( List<Percept> thingList, List<Percept> obstacleList, Tile tile){

        for (Percept percept : thingList){
            if( percept.getParameters().get(3).toString().equals("dispenser") && percept.getParameters().get(4).toString().equals(tile.getName()) ){
                int x = Integer.parseInt(percept.getParameters().get(0).toString());
                int y = Integer.parseInt(percept.getParameters().get(1).toString());
                return getRelativeIdentifierToElem(0, 0, x, y, obstacleList, thingList);
            }
        }

        return null;
    }

    public void updateMap(List<Percept> obstacleList, List<Percept> thingList, List<Percept> goalList) {
        ArrayList<Coord> visionList = new ArrayList<>();
        for(int i = -vision; i <= vision; i++){
            for(int j = -vision; j <= vision; j++){
                Coord c = new Coord(i, j);
                visionList.add(c);
            }
        }

        for (Percept p : obstacleList) {
            int x = getCoord(p.getClonedParameters().get(0));
            int y = getCoord(p.getClonedParameters().get(1));

            visionList.remove(new Coord(x, y));

            int relX = this.map.getRelX() + x;
            int relY = this.map.getRelY() + y;
            Tile tile = new Tile(Tile.Type.OBSTACLE, relX, relY);
            this.map.addTile(tile);
        }

        for (Percept p : thingList) {
            int x = getCoord(p.getClonedParameters().get(0));
            int y = getCoord(p.getClonedParameters().get(1));

            String type = p.getClonedParameters().get(2).toString();
            String name = p.getClonedParameters().get(3).toString();
            int relX = this.map.getRelX() + x;
            int relY = this.map.getRelY() + y;
            if (type.equals("dispenser")) {
                visionList.remove(new Coord(x, y));
                Tile tile = new Tile(Tile.Type.DISPENSER, relX, relY);
                tile.setName(name);
                this.map.addTile(tile);
            }else if (type.equals("entity")){
                meetTeamMate( p.getSource() );
            }
        }

        for (Percept p : goalList) {
            int x = getCoord(p.getClonedParameters().get(0));
            int y = getCoord(p.getClonedParameters().get(1));

            visionList.remove(new Coord(x, y));

            int relX = this.map.getRelX() + getCoord(p.getClonedParameters().get(0));
            int relY = this.map.getRelY() + getCoord(p.getClonedParameters().get(1));
            Tile tile = new Tile(Tile.Type.GOAL, relX, relY);
            this.map.addTile(tile);
        }

        for(Coord c : visionList){
            int relX = this.map.getRelX() + c.getX();
            int relY = this.map.getRelY() + c.getY();
            Tile tile = new Tile(Tile.Type.EMPTY, relX, relY);
            this.map.addTile(tile);
        }
    }

    private void meetTeamMate( String name ){
        if(!this.map.getTeamMates().contains(name)){
            shareCoordinates(name);
            this.map.addTeamMate(name);
        }
    }

    public void updatePosition(int x, int y) {
        this.map.setRelX(x);
        this.map.setRelY(y);
    }

    public Coord calculateAttachedCoords(){
        switch(attachedBlock.discover()){
            case W:
                return new Coord(this.map.getRelX()-1, this.map.getRelY());
            case E:
                return new Coord(this.map.getRelX()+1, this.map.getRelY());
            case S:
                return new Coord(this.map.getRelX(), this.map.getRelY()+1);
            case N:
                return new Coord(this.map.getRelX(), this.map.getRelY()-1);
            default:
                return null;
        }
    }

    public Identifier discover(List<Percept> obstacleList, List<Percept> thingList) {

        Set<Direction> bannedDirs = new HashSet<Direction>();
        Set<Direction> originalDirs = new HashSet<>(directions);

        banDirections(bannedDirs, thingList);
        banDirections(bannedDirs, obstacleList);
        if (!bannedDirs.contains(prevDirection)) {
            return new Identifier(decyphDir(prevDirection));
        } else {
            originalDirs.removeAll(bannedDirs);
            int size = originalDirs.size();
            if (size > 0) {
                int item = new Random().nextInt(size);
                int i = 0;
                for (Direction dir : originalDirs) {
                    if (i == item) {
                        this.prevDirection = dir;
                        return new Identifier(decyphDir(dir));
                    }
                    i++;
                }
            } else {
                // agent is stuck
                say("I am stuck! :(");
                return new Identifier("Error");
            }
        }

        return new Identifier("Error");
    }


    public void banDirections(Set<Direction> bannedDirs, List<Percept> elemList) {
        for (Percept elem : elemList) {
            int x = getCoord(elem.getClonedParameters().get(0));
            int y = getCoord(elem.getClonedParameters().get(1));
            if (this.map.getDistance(x, y) == 1) {
                switch (getDirToElem(x, y)) {
                    case W:
                        bannedDirs.add(Direction.W);
                        break;
                    case E:
                        bannedDirs.add(Direction.E);
                        break;
                    case S:
                        bannedDirs.add(Direction.S);
                        break;
                    case N:
                        bannedDirs.add(Direction.N);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public Identifier getRelativeIdentifierToElem(int selfX, int selfY, int elemX, int elemY, List<Percept> obstacleList,
            List<Percept> thingList) {
        return new Identifier(decyphDir(getRelativeDirectionToElem(selfX, selfY, elemX, elemY, obstacleList, thingList)));
    }

    public Direction getRelativeDirectionToElem(int selfX, int selfY, int elemX, int elemY, List<Percept> obstacleList, List<Percept> thingList){
        int distX;
        int distY;
        Direction propX;
        Direction propY;

        Direction retDir;

        Set<Direction> bannedDirs = new HashSet<Direction>();
        Set<Direction> originalDirs = new HashSet<>(directions);

        List<Percept> entityList = new ArrayList<>();
        for( Percept thing : thingList){
            if( thing.getParameters().get(2).equals("entity") )
                entityList.add(thing);
        }

        banDirections(bannedDirs, obstacleList);
        banDirections(bannedDirs, entityList);

        if (selfX > elemX) {
            distX = Math.abs(selfX - elemX);
            propX = Direction.W;
        } else {
            distX = Math.abs(elemX - selfX);
            propX = Direction.E;
        }

        if (selfY > elemY) {
            distY = Math.abs(selfY - elemY);
            propY = Direction.N;
        } else {
            distY = Math.abs(elemY - selfY);
            propY = Direction.S;
        }

        if (distX > distY) {
            retDir = propX;
        } else {
            retDir = propY;
        }

        if(!bannedDirs.contains(retDir)){
            return retDir;
        }else{
            originalDirs.removeAll(bannedDirs);
            int size = originalDirs.size();
            if (size > 0) {
                int item = new Random().nextInt(size);
                int i = 0;
                for (Direction dir : originalDirs) {
                    if (i == item) {
                        this.prevDirection = dir;
                        return dir;
                    }
                    i++;
                }
            } else {
                // agent is stuck
                say("I am stuck! :(");
                return Direction.N;
            }
        }
        say("I am stuck! :(");
        return Direction.N;
    }

    public Direction getRelativeDirectionToElem(int selfX, int selfY, int elemX, int elemY){
        int distX;
        int distY;
        Direction propX;
        Direction propY;

        Direction retDir;

        if (selfX > elemX) {
            distX = Math.abs(selfX - elemX);
            propX = Direction.W;
        } else {
            distX = Math.abs(elemX - selfX);
            propX = Direction.E;
        }

        if (selfY > elemY) {
            distY = Math.abs(selfY - elemY);
            propY = Direction.N;
        } else {
            distY = Math.abs(elemY - selfY);
            propY = Direction.S;
        }

        if (distX > distY) {
            retDir = propX;
        } else {
            retDir = propY;
        }

        return retDir;

    }

    public int getCoord(Object p) {
        return Integer.parseInt(p.toString());
    }

    public Direction getDirToElem(int x, int y) {
        if (x == 0 && y < 0) {
            return Direction.N;
        } else if (x == 0 && y > 0) {
            return Direction.S;
        } else if (x < 0 && y == 0) {
            return Direction.W;
        } else if (x > 0 && y == 0) {
            return Direction.E;
        }
        return Direction.OTHER;
    }

    public String decyphDir(Direction dir) {
        switch (dir) {
            case W:
                return "w";
            case E:
                return "e";
            case S:
                return "s";
            case N:
                return "n";
            default:
                return "Wrong direction!";
        }
    }

    public void moveAgent(Direction dir) {
        switch (dir) {
            case W:
                say("Let's go west!");
                this.map.planStep(this.map.getRelX() - 1, this.map.getRelY());
                break;
            case E:
                say("Let's go east!");
                this.map.planStep(this.map.getRelX() + 1, this.map.getRelY());
                break;
            case S:
                say("Let's go south!");
                this.map.planStep(this.map.getRelX(), this.map.getRelY() + 1);
                break;
            case N:
                say("Let's go north!");
                this.map.planStep(this.map.getRelX(), this.map.getRelY() - 1);
                break;
        }
    }

    public void moveAgent(String dir) {
        switch (dir) {
            case "w":
                say("Let's go west!");
                this.map.planStep(this.map.getRelX() - 1, this.map.getRelY());
                break;
            case "e":
                say("Let's go east!");
                this.map.planStep(this.map.getRelX() + 1, this.map.getRelY());
                break;
            case "s":
                say("Let's go south!");
                this.map.planStep(this.map.getRelX(), this.map.getRelY() + 1);
                break;
            case "n":
                say("Let's go north!");
                this.map.planStep(this.map.getRelX(), this.map.getRelY() - 1);
                break;
        }
    }
}
