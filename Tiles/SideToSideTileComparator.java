package Tiles;

import java.util.Comparator;

public class SideToSideTileComparator implements Comparator<SideToSideTile> {
    @Override
    public int compare(SideToSideTile sideToSideTile1, SideToSideTile sideToSideTile2) {
        return Comparator
                .comparingInt(SideToSideTile::getDistance)
                .thenComparingDouble(SideToSideTile::getWeight)
                .thenComparingInt(SideToSideTile::getX)
                .thenComparingInt(SideToSideTile::getY)
                .thenComparingInt(tile -> tile.getNeighbour().getX())
                .thenComparingInt(tile -> tile.getNeighbour().getY())
                .compare(sideToSideTile1, sideToSideTile2);
    }
}
