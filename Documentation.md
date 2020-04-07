## MAS


### AgentMap

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
