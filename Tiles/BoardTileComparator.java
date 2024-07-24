package Tiles;

import java.util.Comparator;

public class BoardTileComparator implements Comparator<BoardTile> {
    @Override public int compare(BoardTile tile1, BoardTile tile2) {
        if (tile1.getDistance() < tile2.getDistance())
            return -1;

        if (tile1.getDistance() > tile2.getDistance())
            return 1;

        return 0;
    }
}