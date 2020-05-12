package massim.javaagents.agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
    private AgentMap map;
    private Objective currentObjective;
    private Direction prevDirection;
    private Percept currentTask;
    private TaskElement targetBlock;
    private TaskElement attachedBlock;
    private ArrayList<TaskElement> agentTasks = new ArrayList<TaskElement>();
    private int vision;
    
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
        
        }
    }


    /**
     * Sends a percept as a message to the given agent.
     * The receiver agent may fetch the message the next time it is stepped.
     * @param message the message to deliver
     * @param receiver the receiving agent
     * @param sender the agent sending the message
     */
    protected void sendMessage(Percept message, String receiver, String sender){
        // if(mqSender == null)
        //     return;
        
        // parseExecutor.submit(() -> {
        //     Message msg = new Message(CONTENT_TYPE_PATH, GsonInstance.getInstance().toJson(positions, POSITION_LIST_TYPE));
        //     mqSender.sendMessage(msg);
        // });
        // parseExecutor.submit(()->{

        // })
        Agent recipient = register.get(to);
        
        if(recipient == null) {
            logger.warning("Cannot deliver message to " + to + "; unknown target,");
        }
        else{
            recipient.handleMessage(message, from);
        }
        mailbox.sendMessage(message, receiver, sender);
    }

    /**
     * Broadcasts a message to the entire team.
     * @param message the message to broadcast
     * @param sender the agent sending the message
     */
    void broadcast(Percept message, String sender){
        //AgentContainer agentContainer
        // brodcast
        // if(agentContainer.getMqSender() == null)
        //     return;

        // AgentContainerMessage agentContainerMessage = new AgentContainerMessage(agentContainer);
        // parseExecutor.submit(() -> {
        //     Message msg = new Message(CONTENT_TYPE_AGENT_CONTAINER, GsonInstance.getInstance().toJson(agentContainerMessage));
        //     agentContainer.getMqSender().sendMessage(msg);
        // });
        agentsByTeam.get(teamForAgent.get(sender)).stream()
                .map(Agent::getName).filter(ag -> !ag.equals(sender)).forEach(ag -> sendMessage(message, ag, sender));
                //mailbox.broadcast(message, sender);
    }

    /**
     * Prints a message to std out prefixed with the agent's name.
     * @param message the message to say
     */
    void say(String message){
        System.out.println("[ " + name + " ]  " + message);
    }

    @Override
    public Action step() {
        // System.out.println(this.getName() + " Magellan step");
        final List<Percept> percepts = getPercepts();
        percepts.stream().filter(p -> p.getName().equals("step")).findAny().ifPresent(p -> {
            final Parameter param = p.getParameters().getFirst();
            if (param instanceof Identifier)
                say("Step " + ((Identifier) param).getValue());
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
                return stepGetMaterial(obstacleList, thingList);
            case DropMaterial:
                return stepDropMaterial(obstacleList, thingList);
        }

        return new Action("move", new Identifier("n"));
    }

    private void updateCurrentObjective(List<Percept> percepts) {
        List<Percept> taskList = percepts.stream().filter(p -> p.getName().equals("task")).collect(Collectors.toList());

        if (taskList.isEmpty()) {
            currentTask = null;
            currentObjective = Objective.Discovery;

        } else if (!taskList.contains(currentTask)) {

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
                            this.targetBlock = this.agentTasks.get(0);
                            this.targetBlock = this.agentTasks.remove(0);
                        }
                        break;
                    }
                }
            }
        }

        /*
        if (currentObjective == Objective.GetMaterial && false) 
            currentObjective = Objective.Discovery;
         
        if (hasBlockForTask(currentTask))
            currentObjective = Objective.DropMaterial;
        */
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
        Identifier dir = this.getDirection(obstacleList, thingList);
        moveAgent(dir.toString());
        return new Action("move", dir);
    }

    private Action stepGetMaterial(List<Percept> obstacleList, List<Percept> thingList) {

        Tile goal = this.map.getClosestElement(Type.DISPENSER, this.targetBlock.getName());
        if (this.map.getDistanceFromSpawn(goal.getX(), goal.getY()) > 1) {
            Identifier dir = getRelativeIdentifierToElem(this.map.getRelX(), this.map.getRelY(), goal.getX(),
                    goal.getY(), obstacleList, thingList);
            moveAgent(dir.toString());
            return new Action("move", dir);
        } else {
            this.currentObjective = Objective.DropMaterial;
            Direction dir = getRelativeDirectionToElem(this.map.getRelX(), this.map.getRelY(), goal.getX(), goal.getY());
            attachedBlock = new TaskElement(targetBlock);
            attachedBlock.setIsAttached(true);
            attachedBlock.setDirection(dir);
            say("I grab " + targetBlock.getName() + " block");
            return new Action("attach", getRelativeIdentifierToElem(this.map.getRelX(), this.map.getRelY(), goal.getX(),
                    goal.getY(), obstacleList, thingList));
            /*Identifier dir = getRelativeDirectionToElem(this.map.getRelX(), this.map.getRelY(), goal.getX(),
                goal.getY(), obstacleList, thingList);
            attachedBlocks.put(dir.toString(), goal);
            return new Action("attach", dir);*/
        }
    }

    private Action stepDropMaterial(List<Percept> obstacleList, List<Percept> thingList) {

        Tile goal = this.map.getClosestElement(Type.GOAL);
        if (this.map.getDistanceFromSpawn(goal.getX(), goal.getY()) > 1) {
            Identifier dir = getRelativeIdentifierToElem(this.map.getRelX(), this.map.getRelY(), goal.getX(),
                    goal.getY(), obstacleList, thingList);
            moveAgent(dir.toString());
            return new Action("move", dir);
        } else {
            if(agentTasks.size() > 0){
                targetBlock = agentTasks.remove(0);
                this.currentTask = null;
            }
            say("I drop " + targetBlock.getName() + " block");
            return new Action("detach", getRelativeIdentifierToElem(this.map.getRelX(), this.map.getRelY(), goal.getX(),
                    goal.getY(), obstacleList, thingList));
        }
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

    public void updatePosition(int x, int y) {
        this.map.setRelX(x);
        this.map.setRelY(y);
    }

    public Coord calculateAttachedCoords(){
        switch(attachedBlock.getDirection()){
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

    public Identifier getDirection(List<Percept> obstacleList, List<Percept> thingList) {

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

        banDirections(bannedDirs, obstacleList);

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
                updatePosition(this.map.getRelX() - 1, this.map.getRelY());
                break;
            case E:
                say("Let's go east!");
                updatePosition(this.map.getRelX() + 1, this.map.getRelY());
                break;
            case S:
                say("Let's go south!");
                updatePosition(this.map.getRelX(), this.map.getRelY() + 1);
                break;
            case N:
                say("Let's go north!");
                updatePosition(this.map.getRelX(), this.map.getRelY() - 1);
                break;
        }
    }

    public void moveAgent(String dir) {
        switch (dir) {
            case "w":
                say("Let's go west!");
                updatePosition(this.map.getRelX() - 1, this.map.getRelY());
                break;
            case "e":
                say("Let's go east!");
                updatePosition(this.map.getRelX() + 1, this.map.getRelY());
                break;
            case "s":
                say("Let's go south!");
                updatePosition(this.map.getRelX(), this.map.getRelY() + 1);
                break;
            case "n":
                say("Let's go north!");
                updatePosition(this.map.getRelX(), this.map.getRelY() - 1);
                break;
        }
    }
}
