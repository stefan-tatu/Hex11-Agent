package Tiles;


public class SideToSideTile {
    private int x;
    private int y;
    private int distance;
    private int weight;
    BoardTile neighbour;

    public SideToSideTile(int x, int y, int distance, int weight, BoardTile neighbour) {
        this.x = x;
        this.y = y;
        this.distance = distance;
        this.weight = weight;
        this.neighbour = neighbour;
    }

    public int getX() {
        return this.x;
    }
    public int getY() {
        return this.y;
    }
    public int getDistance() {
        return this.distance;
    }
    public int getWeight() {
        return this.weight;
    }
    public BoardTile getNeighbour() {
        return this.neighbour;
    }

}