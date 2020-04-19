package massim.javaagents;

public class TaskElement {


        private int x;
        private int y;
        private String name;

        public TaskElement(int x, int y, String name){
            this.x = x;
            this.y = y;
            this.name = name;
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

        public void setX(int x){
            this.x = x;
        }

        public void setY(int y){
            this.y = y;
        }

        public void setName(String name){
            this.name = name;
        }
}
