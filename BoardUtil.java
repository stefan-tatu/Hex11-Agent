import Tiles.BoardTile;
import Tiles.SideToSideTile;
import Tiles.SideToSideTileComparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;

public class BoardUtil {

    public static BoardTile[][] copyCurrentBoard(BoardTile[][] currentBoard) {
        int boardSize = currentBoard.length;
        BoardTile[][] destination = new BoardTile[boardSize][];
        for (int i = 0; i < boardSize; ++i) {
            destination[i] = Arrays.copyOf(currentBoard[i], boardSize);
            for (int j = 0; j < boardSize; ++j) {
                destination[i][j] = currentBoard[i][j].copyBoardTile();
            }
        }
        return destination;
    }

    public static ArrayList<int[]> choicesOnCurrentBoard(BoardTile[][] currentBoard) {
        int boardSize = currentBoard.length;

        ArrayList<int[]> choices = new ArrayList<>();
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                if ("0".equals(currentBoard[i][j].getColour())) {
                    int[] c = {i, j};
                    choices.add(c);
                }
            }
        }
        return choices;
    }

    public static PriorityQueue<SideToSideTile> initialisePriorityQueue(String playerColour, int boardSize) {
        PriorityQueue<SideToSideTile> priorityQueue = new PriorityQueue<>((boardSize * boardSize) + 4,
                new SideToSideTileComparator());

        for (int i = -1; i < boardSize; ++i) {
            int x = (playerColour.equals("B")) ? i : boardSize;
            int y = (playerColour.equals("B")) ? boardSize : i;
            priorityQueue.add(new SideToSideTile(x, y, 0, boardSize, new BoardTile(x, y,
                    Integer.MAX_VALUE, "0")));
        }

        return priorityQueue;
    }

    public static BoardTile[][] initialiseBoard(int boardSize) {
        BoardTile[][] board = new BoardTile[boardSize][boardSize];

        for (int i = 0; i < boardSize; ++i) {
            for (int j = 0; j < boardSize; ++j) {
                board[i][j] = new BoardTile(-1, -1, Integer.MAX_VALUE, "N");
            }
        }

        return board;
    }

    public static int getBoardValueAtPosition(int x, int y, BoardTile[][] boardStateParam) {
        String tileColour = boardStateParam[x][y].getColour();
        return "B".equals(tileColour) ? 1 : ("R".equals(tileColour) ? -1 : 0);
    }

    public static boolean isSafe(int x, int y, int boardSize) {
        return x >= 0 && y >= 0 && x < boardSize && y < boardSize;
    }

    public static BoardTile getBoardTileFromCoords(int x, int y, BoardTile[][] boardState){
        return boardState[x][y];
    }

}
