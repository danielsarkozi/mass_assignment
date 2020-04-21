package massim.javaagents;

import massim.javaagents.agents.MagellanAgent;

public class TaskElement {


        private int x;
        private int y;
        private String name;
        private boolean isAttached;
        private MagellanAgent.Direction attachedDir;

        public TaskElement(int x, int y, String name){
            this.x = x;
            this.y = y;
            this.name = name;
            this.isAttached = false;
            this.attachedDir = null;
        }

        public TaskElement( TaskElement te ){
            this.x = te.getX();
            this.y = te.getY();
            this.name = te.getName();
            this.isAttached = te.getIsAttached();
            this.attachedDir = te.getDirection();
        }

        public int getX(){
            return this.x;
        }

        public int getY(){
            return this.y;
        }

        public String getName(){
            return this.name;
        }

        public boolean getIsAttached(){
            return this.isAttached;
        }

        public MagellanAgent.Direction getDirection(){
            return this.attachedDir;
        }

        public void setX(int x){
            this.x = x;
        }

        public void setY(int y){
            this.y = y;
        }

        public void setName(String name){
            this.name = name;
        }

        public void setIsAttached(boolean isAttached){
            this.isAttached = isAttached;
        }

        public void setDirection(MagellanAgent.Direction attachedDir){
            this.attachedDir = attachedDir;
        }
}
