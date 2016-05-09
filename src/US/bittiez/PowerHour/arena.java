package US.bittiez.PowerHour;

/**
 * Created by tadtaylor on 5/8/16.
 */
public class Arena {
    private int X, Y, Z;
    private String world;
    private String name;

    public int getX(){
        return X;
    }
    public int getY(){
        return Y;
    }
    public int getZ(){
        return Z;
    }
    public String getWorld(){
        return world;
    }
    public String getName(){
        return name;
    }


    public void setX(int x){
        X = x;
    }
    public void setY(int y){
        Y = y;
    }
    public void setZ(int z){
        Z = z;
    }
    public void setWorld(String world){
        this.world = world;
    }
    public void setName(String name){
        this.name = name;
    }

    public Arena(){

    }

    public Arena(int x, int y, int z, String world, String name){
        setX(x);
        setY(y);
        setZ(z);
        setWorld(world);
        setName(name);
    }

}
