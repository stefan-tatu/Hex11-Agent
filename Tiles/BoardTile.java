package Tiles;

public class BoardTile {
    private int x;
    private int y;
    private String colour;

    private int distance;

    public BoardTile(int x, int y, int distance, String colour) {
        this.x = x;
        this.y = y;
        this.distance = distance;
        this.colour = colour;
    }

    public int getX(){
        return this.x;
    }
    public int getY(){
        return this.y;
    }
    public int getDistance(){
        return this.distance;
    }
    public String getColour(){
        return this.colour;
    }
    public void setDistance(int d) {
        this.distance = d;
    }
    public void setColour(String c) {
        this.colour = c;
    }
    public void setX(int x) {
        this.x = x;
    }
    public void setY(int y) {
        this.y = y;
    }

    public void setValues(int x, int y, int distance, String colour) {
        this.x = x;
        this.y = y;
        this.distance = distance;
        this.colour = colour;
    }

    public BoardTile copyBoardTile(){
        return new BoardTile(this.x, this.y, this.distance, this.colour);
    }
}
