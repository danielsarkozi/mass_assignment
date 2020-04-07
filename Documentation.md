## MAS


### AgentMap

AgentMap is used to represent the surroundings discovered so far by the agent. Each agent is equiped with an AgentMap which comes with a coordinate system unique to the robot and is based on the agent's spawn point. 

```java
    private int origoX;
    private int origoY;

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

X and Y are the two point in our 2D System that defined as a list( **enum** ):

called at:
```java
        private ArrayList<Tile> gameMap;
```

and the enum class is Defined like below:
```java
    public class Tile{

        public enum Type{
            EMPTY, OBSTACLE, THING
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
> In general, there are two type of Task: 

### Basic Agent Task:
```java
    public void handlePercept(Percept percept) {
        System.out.println( this.getName() + " Basic percept" );
    }

    public void handleMessage(Percept message, String sender) {
        System.out.println( this.getName() + " Basic message" );
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


