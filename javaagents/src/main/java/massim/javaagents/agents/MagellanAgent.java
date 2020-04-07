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

        List<Percept> goalList = percepts.stream().filter(p -> p.getName().equals("goal")).collect(Collectors.toList());

        updateCurrentObjective(percepts);

        switch (currentObjective) {
            case Discovery:
                return stepDiscovery(percepts);
            case GetMaterial:
                return stepGetMaterial(percepts);
            case DropMaterial:
                return stepDropMaterial(percepts);
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

    private Action stepDiscovery(List<Percept> percepts) {
        List<Percept> obstacleList = percepts.stream().filter(p -> p.getName().equals("obstacle"))
                .collect(Collectors.toList());
        List<Percept> thingList = percepts.stream().filter(p -> p.getName().equals("thing"))
                .collect(Collectors.toList());

        return new Action("move", this.getDirection(obstacleList, thingList));
    }

    private Action stepGetMaterial(List<Percept> percepts) {
        List<Percept> obstacleList = percepts.stream().filter(p -> p.getName().equals("obstacle"))
                .collect(Collectors.toList());
        List<Percept> thingList = percepts.stream().filter(p -> p.getName().equals("thing"))
                .collect(Collectors.toList());

        Tile goal = this.map.getClosestElement(Type.ELEMENT);
        if (this.map.getDistance(goal.getX(), goal.getX()) > 0) {
            return new Action("move", getRelativeDirectionToElem(this.map.getRelX(), this.map.getRelY(), goal.getX(), goal.getY(), obstacleList, thingList));
        } else {
            return new Action("attach",
                    getRelativeDirectionToElem(this.map.getRelX(), this.map.getRelY(), goal.getX(), goal.getY(), obstacleList, thingList));
        }
    }

    private Action stepDropMaterial(List<Percept> percepts) {
        List<Percept> obstacleList = percepts.stream().filter(p -> p.getName().equals("obstacle"))
                .collect(Collectors.toList());
        List<Percept> thingList = percepts.stream().filter(p -> p.getName().equals("thing"))
                .collect(Collectors.toList());

        Tile goal = this.map.getClosestElement(Type.DISPENSER);
        if (this.map.getDistance(goal.getX(), goal.getX()) > 0) {
            return new Action("move", getRelativeDirectionToElem(this.map.getRelX(), this.map.getRelY(), goal.getX(), goal.getY(), obstacleList, thingList));
        } else {
            return new Action("detach",
                    getRelativeDirectionToElem(this.map.getRelX(), this.map.getRelY(), goal.getX(), goal.getY(), obstacleList, thingList));
        }
    }

    public void updateMap() {

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
                    int item = new Random().nextInt(size);
                    int i = 0;
                    for (Direction dir : originalDirs) {
                        if (i == item)
                            return new Identifier(decyphDir(dir));
                        i++;
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
        /*
        if (!bannedDirs.contains(retDir)) {
            return new Identifier(decyphDir(retDir));
        } else {
            originalDirs.removeAll(bannedDirs);
            int size = originalDirs.size();
            int item = new Random().nextInt(size);
            int i = 0;
            for (Direction dir : originalDirs) {
                if (i == item)
                    return new Identifier(decyphDir(dir));
                i++;
            }
        }
        return null;
        */
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
