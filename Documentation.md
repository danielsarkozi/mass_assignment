## MAS


### AgentMap

AgentMap is used to represent the surroundings discovered so far by the agent. Each agent is equiped with an AgentMap which comes with a coordinate system unique to the robot and is based on the agent's spawn point. 

```java
    // Current coordinates relative to the spawn point.
    private int relativeX;
    private int relativeY;
```

To get current point of the **agent** (2D). at the following method we calculate the location
```java
    public AgentMap( ){
        this.origoX = 0;
        this.origoY = 0;
        this.gameMap = new ArrayList<Tile>();
    }
```
Location is calculate base on the distance the agent has from the initial points.
this Distance is calculated in form that followed below.

```java
public int getDistance( int destX, int destY ){
        return Math.abs(relativeX-destX) + Math.abs(relativeY-destY);
    }
```

Every tile on the map encountered by the agent is stored in a list:

```java
        private ArrayList<Tile> gameMap;
```

The Tile class represents each segment of the map, and can have various types assigned to it, these include:
EMPTY, OBSTACLE, ELEMENT, AGENT, DISPENSER as defined in the MAS scenario documentation.
```java
    public class Tile{

        public enum Type{
            EMPTY, OBSTACLE, ELEMENT, AGENT, DISPENSER
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
```

## Task of each agent
> In general, there are three type of Task: 

### Basic Agent Task:
```java
    public void handlePercept(Percept percept) {
        System.out.println( this.getName() + " Basic percept" );
    }

    public void handleMessage(Percept message, String sender) {
        System.out.println( this.getName() + " Basic message" );
    }
    
    @Override
    public Action step() {
        //Lots of action
    }
```

The two method that came in top, are use for the basic agent activity.
The Agent called ** Magellan ** and the main source file of the agent is ```MagellanAgent.java```.
### Magellan Agent Task:
This stage of the project divided to the following phases:
* Discovery ( also update the map).
> when and agent finish the discovering map it change the phase to complete the tasks.
* Get Material.
* Drop The Material.

### AgentMap
In this file we have an ```update``` function that responsible to locate the different object and update the map. this function is running at the first phase which is discovery.


### MagellanAgent.java 
In this part we looking at the specific method that helps to get the tile of each target which means targeted blocks.
first we have the set that includes the set of the information of the block types.
then we have special loop that's go through the type of the block and then in another one it through the map and get the location of the targeted block. 



```java
private Tile getTargetTile() 
        int minDist = 0;
        Tile closestTile = null;
        Set<String> blockType = new HashSet<>();
        for (TaskElement te : agentTask)
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

```

### should go to the goal or not
At this method agent consider the blocks is good or not base on the block types and the task.
It will return true then agent must go for the block.


```java
private Boolean shouldGoToGoal() {
        for (Map.Entry<String, Tile> pair : attachedBlocks.entrySet())
        {
            for (TaskElement te : agentTask)
            {
                if (te.getName().equals(pair.getValue().getName()))
                {
                    return true;
                }
            }
        }
        return false;	        return false;
}	    
```
For Compeleting the task we need the following methods:
* getClosestElement
* getIsAttached
* discover
By the method we mentioned agents are able to accomplish the tasks.

```java
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
```

At this method the agent calculate how they can attach the block, as know from the scenario each agent can attach four blocks to itself. that's why we have the four direction for this purpose.
> At this stage, agents can get the distance from the block it needs and if the block is the correct one agent will discover the path to it and will attached it to itself.

### Messaging Service include the full code:

```java
public final class Message {

    // The thing That Agent(Magellan) are sharing with each other.
    // As we mentioned in the presentation one of the thing that agents are share with each other is path to the block.

    public static final String CONTENT_TYPE_RESET = "reset";
    public static final String CONTENT_TYPE_PERCEPT = "percept";
    public static final String CONTENT_TYPE_AUTH_AGENTS = "authenticatedAgents";
    public static final String CONTENT_TYPE_LOCATION = "location";
    public static final String CONTENT_TYPE_PATH = "path";

    // This is needed to serialize List<MapPercept> using Gson
    public static final Type MAP_PERCEPT_LIST_TYPE = new TypeToken<Collection<MapPercept>>(){}.getType();
    public static final Type MAP_AUTH_MAP_TYPE = new TypeToken<List<Position>>(){}.getType();
    public static final String CONTENT_TYPE_NEW_STEP = "newStep";
    public static final Type POSITION_LIST_TYPE = new TypeToken<List<Position>>(){}.getType();
    private static final ExecutorService parseExecutor = Executors.newSingleThreadExecutor();
    public static final String CONTENT_TYPE_AGENT_CONTAINER = "agent_update";

    private String contentType;
    private String messageBody;

    private Message(String contentType, String messageBody) {
        // content type means(path, percept, authenticated agent which is the agent that asign to the block and etc.)
        this.contentType = contentType;
        this.messageBody = messageBody;
    }

    public static void createAndSendPathMessage(MQSender mqSender, List<Position> positions) {
        if(mqSender == null)
            return;

        parseExecutor.submit(() -> {
            Message msg = new Message(CONTENT_TYPE_PATH, GsonInstance.getInstance().toJson(positions, POSITION_LIST_TYPE));
            mqSender.sendMessage(msg);
        });
    }

    public String getContentType() {
        return contentType;
    }

    public String getMessageBody() {
        return messageBody;
    }


    public static void createAndSendAgentContainerMessage(AgentContainer agentContainer) {
        if(agentContainer.getMqSender() == null)
            return;

        AgentContainerMessage agentContainerMessage = new AgentContainerMessage(agentContainer);
        parseExecutor.submit(() -> {
            Message msg = new Message(CONTENT_TYPE_AGENT_CONTAINER, GsonInstance.getInstance().toJson(agentContainerMessage));
            agentContainer.getMqSender().sendMessage(msg);
        });
    }

    public static void createAndSendNewStepMessage(MQSender mqSender, long step) {
        if(mqSender == null)
            return;

        parseExecutor.submit(() -> {
            Message msg = new Message(CONTENT_TYPE_NEW_STEP, String.valueOf(step));
            mqSender.sendMessage(msg);
        });
    }

    public static void createAndSendResetMessage(MQSender mqSender) {
        if(mqSender == null)
            return;

        parseExecutor.submit(() -> {
            Message msg = new Message(CONTENT_TYPE_RESET, "");
            mqSender.sendMessage(msg);
        });
    }

    public static void createAndSendAuthenticatedMessage(MQSender mqSender, List<AuthenticatedAgent> authenticatedContainers) {

        if(mqSender == null)
            return;

        parseExecutor.submit(() -> {
            Map<Position, String> agentPositions = new HashMap<>();
            for(AuthenticatedAgent authenticatedAgent : authenticatedContainers)
            {
                Position translation = authenticatedAgent.getTranslationValue();
                Position translatedPosition = authenticatedAgent.getAgentContainer().getCurrentLocation().add(translation);
                agentPositions.put(translatedPosition, authenticatedAgent.getAgentContainer().getAgentName());
            }

            Message msg = new Message(CONTENT_TYPE_AUTH_AGENTS, GsonInstance.getInstance().toJson(agentPositions.keySet(), MAP_AUTH_MAP_TYPE));
            mqSender.sendMessage(msg);
        });
    }

    public static void createAndSendPerceptMessage(MQSender mqSender, AgentLocation location, Collection<MapPercept> mapPercept) {

        if (mapPercept == null || mqSender == null || location == null)
            return;

        parseExecutor.submit(() -> {
            Message locMsg = new Message(CONTENT_TYPE_LOCATION, location.toJsonString());
            mqSender.sendMessage(locMsg);

            Message msg = new Message(CONTENT_TYPE_PERCEPT, GsonInstance.getInstance().toJson(mapPercept, MAP_PERCEPT_LIST_TYPE));
            mqSender.sendMessage(msg);
        });
    }
}
```
