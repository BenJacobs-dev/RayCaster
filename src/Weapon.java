import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;

public class Weapon extends Rectangle {

    public int timeStep, shootStartTime;
    public boolean moving;
    public Player player;
    public double prevPosX, prevPosY;

    public Weapon(Player playerIn, double width, double height, double x, double y, ImagePattern img) {
        super(width, height, img);
        setX(x);
        setY(y);
        timeStep = 1;
        shootStartTime = -1000000;
        moving = false;
        player = playerIn;
    }

    public void shoot() {
        shootStartTime = timeStep;
    }

    public void animateShoot(int frames) {
        // // Spin animation
        // int timeElapsed = timeStep - shootStartTime;
        // if(timeElapsed <= frames){
        //     setRotate(timeElapsed*(360/frames));
        // }
        int timeElapsed = timeStep - shootStartTime;
        if(timeElapsed <= 3){
            setRotate(20*timeElapsed);
        }
        else if (timeElapsed <= frames){
            setRotate(-5*timeElapsed+75);
        }

    }

    public void animateBob(int frames) {
        // Bob animation
        if(player.width != prevPosX || player.height != prevPosY){
            setTranslateY(10*Math.sin(timeStep*Math.PI*0.125));
        }
    }

    public void animate(){
        int frames = 15;
        animateShoot(frames);
        animateBob(frames);
        timeStep++;
        prevPosX = player.width;
        prevPosY = player.height;
    }

}
