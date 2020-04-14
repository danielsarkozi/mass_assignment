package massim.javaagents.agents;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import eis.iilang.*;
import massim.javaagents.AgentMap;
import massim.javaagents.Coord;
import massim.javaagents.MailService;
import massim.javaagents.Tile;
import massim.javaagents.Tile.Type;

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
    private Coord targetBlock;

    Set<Direction> directions = new HashSet<Direction>();

    enum Objective {
        Discovery, GetMaterial, DropMaterial
    }

    enum Direction {
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
    public void handleMessage(final Percept message, final String sender) {
        // System.out.println(this.getName() + " Magellan message");
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

        updateCurrentObjective(percepts);

        say( "I'm in " + this.currentObjective + " phase");

        List<Percept> goalList = percepts.stream().filter(p -> p.getName().equals("goal"))
                .collect(Collectors.toList());
        List<Percept> obstacleList = percepts.stream().filter(p -> p.getName().equals("obstacle"))
                .collect(Collectors.toList());
        List<Percept> thingList = percepts.stream().filter(p -> p.getName().equals("thing"))
                .collect(Collectors.toList());

        updateMap(obstacleList, thingList, goalList);

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
            System.out.println("found a new task!");
            currentTask = taskList.get(0);
        }

        if (currentObjective == Objective.GetMaterial && (targetBlock = getNextBlock()) == null)
            currentObjective = Objective.Discovery;

        if (hasBlockForTask(currentTask))
            currentObjective = Objective.DropMaterial;
    }

    private Coord getNextBlock() {
        return null;
    }

    private Boolean hasBlockForTask(Percept task) {
        return false;
    }

    private Action stepDiscovery(List<Percept> obstacleList, List<Percept> thingList) {
        return new Action("move", this.getDirection(obstacleList, thingList));
    }

    private Action stepGetMaterial(List<Percept> obstacleList, List<Percept> thingList) {

        Tile goal = this.map.getClosestElement(Type.DISPENSER);
        if (this.map.getDistance(goal.getX(), goal.getX()) > 0) {
            return new Action("move", getRelativeDirectionToElem(this.map.getRelX(), this.map.getRelY(), goal.getX(), goal.getY(), obstacleList, thingList));
        } else {
            return new Action("attach",
                    getRelativeDirectionToElem(this.map.getRelX(), this.map.getRelY(), goal.getX(), goal.getY(), obstacleList, thingList));
        }
    }

    private Action stepDropMaterial(List<Percept> obstacleList, List<Percept> thingList) {

        Tile goal = this.map.getClosestElement(Type.GOAL);
        if (this.map.getDistance(goal.getX(), goal.getX()) > 0) {
            return new Action("move", getRelativeDirectionToElem(this.map.getRelX(), this.map.getRelY(), goal.getX(), goal.getY(), obstacleList, thingList));
        } else {
            return new Action("detach",getRelativeDirectionToElem(this.map.getRelX(), this.map.getRelY(), goal.getX(), goal.getY(), obstacleList, thingList));
        }
    }

    public void updateMap(List<Percept> obstacleList, List<Percept> thingList, List<Percept> goalList) {
        for (Percept p : obstacleList){
            int relX = this.map.getRelX() + getCoord(p.getClonedParameters().get(0));
            int relY = this.map.getRelY() + getCoord(p.getClonedParameters().get(1));
            Tile tile = new Tile( Tile.Type.OBSTACLE, relX, relY );
            this.map.addTile(tile);
        }

        for (Percept p : thingList){
            String type = p.getClonedParameters().get(2).toString();
            String name = p.getClonedParameters().get(3).toString();
            int relX = this.map.getRelX() + getCoord(p.getClonedParameters().get(0));
            int relY = this.map.getRelY() + getCoord(p.getClonedParameters().get(1));
            if( type.equals("dispenser") ){
                Tile tile = new Tile( Tile.Type.DISPENSER, relX, relY );
                tile.setName(name);
                this.map.addTile(tile);
            }
        }

        for (Percept p : goalList){
            int relX = this.map.getRelX() + getCoord(p.getClonedParameters().get(0));
            int relY = this.map.getRelY() + getCoord(p.getClonedParameters().get(1));
            Tile tile = new Tile( Tile.Type.GOAL, relX, relY );
            this.map.addTile(tile);
        }
    }



    public void updatePosition(int x, int y) {
        this.map.setRelX(x);
        this.map.setRelY(y);
    }

    public Identifier getDirection(List<Percept> obstacleList, List<Percept> thingList) {
        switch (this.currentObjective) {
            case Discovery:

                Set<Direction> bannedDirs = new HashSet<Direction>();
                Set<Direction> originalDirs = new HashSet<>(directions);

                banDirections(bannedDirs, thingList);
                banDirections(bannedDirs, obstacleList);
                if (!bannedDirs.contains(prevDirection)) {
                    return new Identifier(decyphDir(prevDirection));
                } else {
                    originalDirs.removeAll(bannedDirs);
                    int size = originalDirs.size();
                    if(size>0){
                        int item = new Random().nextInt(size);
                        int i = 0;
                        for (Direction dir : originalDirs) {
                            if (i == item){
                                this.prevDirection = dir;
                                return new Identifier(decyphDir(dir));
                            }
                            i++;
                        }
                    }else{
                        // agent is stuck
                        say("I am stuck! :(");
                        return new Identifier("n");
                    }
                }
            default:
                return new Identifier("n");
        }
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

    public Identifier getRelativeDirectionToElem(int selfX, int selfY, int elemX, int elemY, List<Percept> obstacleList, List<Percept> thingList) {

        Set<Direction> bannedDirs = new HashSet<Direction>();
        Set<Direction> originalDirs = new HashSet<>(directions);

        banDirections(bannedDirs, thingList);
        banDirections(bannedDirs, obstacleList);

        int distX;
        int distY;
        Direction propX;
        Direction propY;

        Direction retDir;

        if(selfX > elemX){
            distX = Math.abs(selfX-elemX);
            propX = Direction.W;
        }else{
            distX = Math.abs(elemX-selfX);
            propX = Direction.E;
        }

        if(selfY > elemY){
            distY = Math.abs(selfY-elemY);
            propY = Direction.N;
        }else{
            distY = Math.abs(elemY-selfY);
            propY = Direction.S;
        }

        if(distX>distY){
            retDir = propX;
        }else{
            retDir = propY;
        }

        return new Identifier(decyphDir(retDir));
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
                say("Let's go west!");
                updatePosition(this.map.getRelX() - 1, this.map.getRelY());
                return "w";
            case E:
                say("Let's go east!");
                updatePosition(this.map.getRelX() + 1, this.map.getRelY());
                return "e";
            case S:
                say("Let's go south!");
                updatePosition(this.map.getRelX(), this.map.getRelY() + 1);
                return "s";
            case N:
                say("Let's go north!");
                updatePosition(this.map.getRelX(), this.map.getRelY() - 1);
                return "n";
            default:
                return "Wrong direction!";
        }
    }
}
