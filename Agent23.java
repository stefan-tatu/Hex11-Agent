import Tiles.BoardTile;
import Tiles.SideToSideTile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class Agent23 {
    public static String HOST = "127.0.0.1";
    public static int PORT = 1234;

    private Socket s;
    private PrintWriter out;
    private BufferedReader in;

    private static String colour = "R";
    private int turn = 0;
    private static int boardSize = 11;
    private int DEPTH = 2;
    private ExecutorService executorService;
    private static final int[][] openingMoves = {
            {1,0}, {2,0}, {3,0}, {5,0}, {6,0}, {7,0}, {8,0}, {9,0}, {10, 0}, {1,2}, {9,2}, {2, 5}, {9, 2}, {1, 8},
            {8, 5}, {9, 8}, {10, 10}, {0, 10}, {2, 10}, {3,10}, {4, 10}, {5, 10}, {7,10}, {8,10}, {10, 10}
    };

    private Map<String, Integer> shortestPathCache = new ConcurrentHashMap<>();

    private void Connect() throws UnknownHostException, IOException{
        s = new Socket(HOST, PORT);
        out = new PrintWriter(s.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    private String getMessage() throws IOException{
        return in.readLine();
    }

    private void sendMessage(String msg){
        out.print(msg); out.flush();
    }

    private void closeConnection() throws IOException{
        executorService.shutdown();
        s.close();
        out.close();
        in.close();
    }

    public void run(){
        // connect to the engine
        try{
            Connect();
        } catch (UnknownHostException e){
            System.out.println("ERROR: Host not found.");
            return;
        } catch (IOException e){
            System.out.println("ERROR: Could not establish I/O.");
            return;
        }

        while (true){
            // receive messages
            try{
                String msg = getMessage();
                boolean res = interpretMessage(msg);
                if (res == false) break;
            } catch (IOException e){
                System.out.println("ERROR: Could not establish I/O.");
                return;
            }
        }

        try{
            closeConnection();
        } catch (IOException e){
            System.out.println("ERROR: Connection was already closed.");
        }
    }

    private boolean interpretMessage(String s){
        turn++;

        String[] msg = s.strip().split(";");
        switch (msg[0]){
            case "START":
                boardSize = Integer.parseInt(msg[1]);
                colour = msg[2];
                if (colour.equals("R")){
                    // so sad ):
                    String board = "";
                    for (int i = 0; i < boardSize; i++){
                        String line = "";
                        for (int j = 0; j < boardSize; j++)
                            line += "0";
                        board += line;
                        if (i < boardSize - 1) board += ",";
                    }
                    makeMove(board);
                }
                break;

            case "CHANGE":
                if (msg[3].equals("END")) return false;
                if (msg[1].equals("SWAP")) colour = opp(colour);
                if (msg[3].equals(colour)) makeMove(msg[2]);
                break;

            default:
                return false;
        }

        return true;
    }

    private void makeMove(String board) {
        if (turn == 1) {
            int choiceIndex = new Random().nextInt(openingMoves.length);
            int[] choice = openingMoves[choiceIndex];
            sendMessage(choice[0] + "," + choice[1] + "\n");
            return;
        }

        String[] lines = board.split(",");
        if (turn == 2) {
            if (shouldSwap(lines)) {
                sendMessage("SWAP\n");
                return;
            }
        }

        ArrayList<int[]> choices = initialiseBoard(lines);

        if (!choices.isEmpty()) {
            int[] choice = minimax(choices, DEPTH, colour, createBoardState(lines));
            sendMessage(choice[0] + "," + choice[1] + "\n");
        }
    }

    private boolean shouldSwap(String[] lines) {
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                if (lines[i].charAt(j) == 'R') {
                    int finalI = i;
                    int finalJ = j;
                    if (Arrays.stream(openingMoves).anyMatch(move -> move[0] == finalI && move[1] == finalJ)) {
                        return true;
                    }
                    if ((i == 1 && (j >= 3 && j <= 5 || j == 9 || j == 10)) || (i == 9 && (j == 0 || j == 1
                            || (j >= 5 && j <= 7))) || (i >= 2 && j >= 1 && i <= 8 && j <= 9)) {
                        return true;
                    }

                }
            }
        }
        return false;
    }

    private ArrayList<int[]> initialiseBoard(String[] lines) {
        ArrayList<int[]> choices = new ArrayList<>();
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                if (lines[i].charAt(j) == '0') {
                    choices.add(new int[]{i, j});
                }
            }
        }
        return choices;
    }

    private BoardTile[][] createBoardState(String[] lines) {
        BoardTile[][] boardState = new BoardTile[boardSize][];
        for (int i = 0; i < boardSize; ++i) {
            BoardTile[] row = new BoardTile[boardSize];
            for (int j = 0; j < boardSize; j++) {
                BoardTile t = new BoardTile(i, j, Integer.MAX_VALUE, String.valueOf(lines[i].charAt(j)));
                row[j] = t;
            }
            boardState[i] = row;
        }
        return boardState;
    }

    private int[] minimax(ArrayList<int[]> choices, int depth, String colourParam, BoardTile[][] currentBoard) {
        int[] bestMove = null;
        final int[] maxi = {Integer.MIN_VALUE};

        List<Callable<int[]>> tasks = new ArrayList<>();

        for (int[] choice : choices) {
            final int[] currentChoice = choice;
            Callable<int[]> task = () -> {
                BoardTile[][] newBoardState = BoardUtil.copyCurrentBoard(currentBoard);
                newBoardState[currentChoice[0]][currentChoice[1]].setColour(colourParam);

                ArrayList<int[]> newChoices = BoardUtil.choicesOnCurrentBoard(newBoardState);

                int current_maxi = minimaxVal(newChoices, newBoardState, depth - 1, maxi[0], Integer.MAX_VALUE, opp(colourParam));

                if (current_maxi > maxi[0]) {
                    maxi[0] = current_maxi;
                    return currentChoice;
                } else {
                    return null;
                }
            };

            tasks.add(task);
        }

        try {
            List<Future<int[]>> results = executorService.invokeAll(tasks);

            for (Future<int[]> result : results) {
                if (result != null) {
                    int[] move = result.get();
                    if (move != null) {
                        bestMove = move;
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return bestMove;
    }

    private int minimaxVal(ArrayList<int[]> choices, BoardTile[][] currentBoard, int depth, int alpha, int beta,
                           String currentColour) {

        if (depth == 0) {
            return sideToSideDistanceBoardEvaluation(currentBoard, colour);
        }

        if (currentColour.equals(colour)) {
            alpha = Integer.MIN_VALUE;
            for (int[] choice : choices) {
                if (alpha < beta) {
                    alpha = getAlpha(currentBoard, depth, alpha, beta, currentColour, choice);
                } else {
                    break;
                }
            }
            return alpha;
        } else {
            beta = Integer.MAX_VALUE;
            for (int[] choice : choices) {
                if (alpha < beta) {
                    beta = getBeta(currentBoard, depth, alpha, beta, currentColour, choice);
                } else {
                    break;
                }
            }
            return beta;
        }
    }

    private int getAlpha(BoardTile[][] boardState, int depth, int alpha, int beta, String currentColour, int[] choice) {
        makeMove(choice, currentColour, boardState);
        ArrayList<int[]> newChoices = BoardUtil.choicesOnCurrentBoard(boardState);
        alpha = Math.max(alpha, minimaxVal(newChoices, boardState, depth - 1, alpha, beta, opp(currentColour)));
        undoMove(choice, boardState);
        return alpha;
    }

    private int getBeta(BoardTile[][] boardState, int depth, int alpha, int beta, String currentColour, int[] choice) {
        makeMove(choice, currentColour, boardState);
        ArrayList<int[]> newChoices = BoardUtil.choicesOnCurrentBoard(boardState);
        beta = Math.min(beta, minimaxVal(newChoices, boardState, depth - 1, alpha, beta, opp(currentColour)));
        undoMove(choice, boardState);
        return beta;
    }

    private void makeMove(int[] choice, String currentColour, BoardTile[][] boardState) {
        boardState[choice[0]][choice[1]].setColour(currentColour);
    }

    private void undoMove(int[] choice, BoardTile[][] boardState) {
        boardState[choice[0]][choice[1]].setColour("0");
    }

    private int sideToSideDistanceBoardEvaluation(BoardTile[][] currentBoard, String currentColour) {
        int[] redSource = {-1, -1};
        int[] redDestination = {-3, -3};
        int[] blueSource = {-2, -2};
        int[] blueDestination = {-4, -4};

        int shortestPathRed = shortestPathCached(redSource, redDestination,
                BoardUtil.copyCurrentBoard(currentBoard), "R");
        int shortestPathBlue = shortestPathCached(blueSource, blueDestination,
                BoardUtil.copyCurrentBoard(currentBoard), "B");

        if (shortestPathRed == 0 && currentColour.equals("R") || shortestPathBlue == 0 &&
                currentColour.equals("B")) {
            return Integer.MAX_VALUE;
        } else if (shortestPathRed == 0 && currentColour.equals("B") || shortestPathBlue == 0 &&
                currentColour.equals("R")) {
            return Integer.MIN_VALUE;
        }

        int redScore = sideToSideDistance(BoardUtil.copyCurrentBoard(currentBoard), "R");
        int blueScore = sideToSideDistance(BoardUtil.copyCurrentBoard(currentBoard), "B");
        redScore += shortestPathRed;
        blueScore += shortestPathBlue;

        int value = redScore - blueScore;
        value = ("R".equals(currentColour)) ? -value : value;

        if (redScore == Integer.MAX_VALUE || blueScore == Integer.MAX_VALUE) {
            value = (value < 0) ? boardPositionValue(currentBoard) - 100 : boardPositionValue(currentBoard) + 100;
        }

        return value;
    }

    private int sideToSideDistance(BoardTile[][] currentBoard, String currentColour) {
        PriorityQueue<SideToSideTile> priorityQueue = BoardUtil.initialisePriorityQueue(currentColour, boardSize);
        BoardTile[][] bestNeighbors = BoardUtil.initialiseBoard(boardSize);
        BoardTile[][] distancesGrid = BoardUtil.initialiseBoard(boardSize);

        BoardTile bestOppositeTile = new BoardTile(-1, -1, Integer.MAX_VALUE, "N");
        Set<BoardTile> searchedTiles = new HashSet<>();

        boolean isConnected = false;
        int row;
        int col;
        int distance = -1;

        while (!priorityQueue.isEmpty()) {
            SideToSideTile parent = priorityQueue.remove();
            distance = parent.getDistance();
            int weight = parent.getWeight();
            row = parent.getX();
            col = parent.getY();
            BoardTile neighbour = parent.getNeighbour();
            if (weight == 0) {
                if (bestOppositeTile.getColour().equals("N")) {
                    bestOppositeTile = neighbour;
                } else if (bestOppositeTile.getX() != neighbour.getX() || bestOppositeTile.getY() != neighbour.getY()) {
                    isConnected = true;
                    break;
                }
            }
            int[] directionX = {-1, 0, 1, 1, 0, -1};
            int[] directionY = {0, -1, -1, 0, 1, 1};

            for (int i = 0; i < 6; ++i) {
                int new_row = row + directionX[i];
                int new_col = col + directionY[i];
                int boardValue;
                BoardTile tile;

                if (BoardUtil.isSafe(new_row, new_col, boardSize)) {
                    boardValue = BoardUtil.getBoardValueAtPosition(new_row, new_col, currentBoard);
                    tile = BoardUtil.getBoardTileFromCoords(new_row, new_col, currentBoard);
                } else {
                    continue;
                }

                if (boardValue == 0 && !searchedTiles.contains(tile)) {
                    if (bestNeighbors[new_row][new_col].getColour().equals("N")) {
                        bestNeighbors[new_row][new_col] = neighbour;
                    } else if (bestNeighbors[new_row][new_col].getX() != neighbour.getX() ||
                            bestNeighbors[new_row][new_col].getY() != neighbour.getY()) {
                        searchedTiles.add(tile);
                        if (currentColour.equals("B")) {
                            priorityQueue.add(new SideToSideTile(new_row, new_col, distance + 1, new_col,
                                    BoardUtil.getBoardTileFromCoords(new_row, new_col, currentBoard)));
                        } else {
                            priorityQueue.add(new SideToSideTile(new_row, new_col, distance + 1, new_row,
                                    BoardUtil.getBoardTileFromCoords(new_row, new_col, currentBoard)));
                        }
                        distancesGrid[new_row][new_col].setDistance(distance + 1);
                    }
                } else if (((boardValue == 1 && currentColour.equals("B")) ||
                        (boardValue == -1 && currentColour.equals("R"))) && !searchedTiles.contains(tile)) {
                    if (bestNeighbors[new_row][new_col].getColour().equals("N")) {
                        bestNeighbors[new_row][new_col] = neighbour;
                        if (currentColour.equals("B")) {
                            priorityQueue.add(new SideToSideTile(new_row, new_col, distance, new_col, neighbour));
                        } else {
                            priorityQueue.add(new SideToSideTile(new_row, new_col, distance, new_row, neighbour));
                        }
                    } else if (bestNeighbors[new_row][new_col].getX() != neighbour.getX() ||
                            bestNeighbors[new_row][new_col].getY() != neighbour.getY()) {
                        searchedTiles.add(tile);
                        bestNeighbors[new_row][new_col] = neighbour;
                        if (currentColour.equals("B")) {
                            priorityQueue.add(new SideToSideTile(new_row, new_col, distance, new_col, neighbour));
                        } else {
                            priorityQueue.add(new SideToSideTile(new_row, new_col, distance, new_row, neighbour));
                        }
                        distancesGrid[new_row][new_col].setDistance(distance + 1);
                    }
                }
            }
        }

        return isConnected ? distance : Integer.MAX_VALUE;
    }

    private int boardPositionValue(BoardTile[][] currentBoard) {
        int[] redSource = {-1, -1};
        int[] redDestination = {-3, -3};
        int[] blueSource = {-2, -2};
        int[] blueDestination = {-4, -4};

        int redScore = shortestPathCached(redSource, redDestination,
                BoardUtil.copyCurrentBoard(currentBoard), "R");
        int blueScore = shortestPathCached(blueSource, blueDestination,
                BoardUtil.copyCurrentBoard(currentBoard), "B");

        return colour.equals("B") ? redScore - blueScore : blueScore - redScore;
    }

    private int shortestPathCached(int[] source, int[] dest, BoardTile[][] currentBoard, String currentColour) {
        String cacheKey = Arrays.toString(source) + Arrays.toString(dest) + currentColour;
        return shortestPathCache.computeIfAbsent(cacheKey,
                key -> shortestPath(source, dest, currentBoard, currentColour));
    }

    private int shortestPath(int[] source, int[] dest, BoardTile[][] currentBoard, String currentColour) {
        PriorityQueue<BoardTile> priorityQueue = new PriorityQueue<>(Comparator.comparingInt(BoardTile::getDistance));
        BoardTile sourceTile = new BoardTile(source[0], source[1], 0, currentColour);
        priorityQueue.add(sourceTile);
        Set<BoardTile> visitedTiles = new HashSet<>();

        String opponentColour = opp(currentColour);

        while (!priorityQueue.isEmpty()) {
            BoardTile currentTile = priorityQueue.remove();
            visitedTiles.add(currentTile);

            if (currentTile.getX() == dest[0] && currentTile.getY() == dest[1]) {
                return currentTile.getDistance();
            }

            visitNeighbours(currentBoard, currentColour, priorityQueue, visitedTiles, opponentColour, currentTile);
        }
        return Integer.MAX_VALUE;
    }

    private void visitNeighbours(BoardTile[][] currentBoard, String currentColour,
                                 PriorityQueue<BoardTile> priorityQueue, Set<BoardTile> visitedBoardTiles,
                                 String opponentColour, BoardTile currentTile) {
        for (BoardTile neighbor : getNeighbours(currentTile, currentBoard, currentColour)) {
            if (shouldSkipTile(visitedBoardTiles, priorityQueue, currentTile, neighbor, opponentColour)) {
                continue;
            }

            int distance = currentTile.getColour().equals(neighbor.getColour()) ? 0 : 1;
            int newDistance = distance + currentTile.getDistance();

            if (!visitedBoardTiles.contains(neighbor) && !priorityQueue.contains(neighbor)) {
                neighbor.setColour(currentTile.getColour());
                neighbor.setDistance(newDistance);
                priorityQueue.add(neighbor);
            }
        }
    }

    private boolean shouldSkipTile(Set<BoardTile> visitedBoardTiles, PriorityQueue<BoardTile> priorityQueue,
                                   BoardTile currentTile, BoardTile neighbor, String opponentColour) {
        return visitedBoardTiles.contains(neighbor) || priorityQueue.contains(neighbor) ||
                currentTile.getColour().equals(opponentColour);
    }

    private ArrayList<BoardTile> getNeighbours(BoardTile boardTile, BoardTile[][] currentBoard, String currentColour) {
        ArrayList<BoardTile> neighbours = new ArrayList<>();
        int x = boardTile.getX();
        int y = boardTile.getY();

        switch (x) {
            case -1:
                addRowToNeighbours(0, currentBoard, neighbours);
                break;
            case -2:
                addColumnToNeighbours(currentBoard.length - 1, currentBoard, neighbours);
                break;
            case -3:
                addRowToNeighbours(currentBoard.length - 1, currentBoard, neighbours);
                break;
            case -4:
                addColumnToNeighbours(0, currentBoard, neighbours);
                break;
            default:
                addTileIfExists(x - 1, y, currentBoard, neighbours);
                addTileIfExists(x + 1, y, currentBoard, neighbours);
                addTileIfExists(x, y - 1, currentBoard, neighbours);
                addTileIfExists(x, y + 1, currentBoard, neighbours);

                if (x == currentBoard.length - 1 && currentColour.equals("R")) {
                    neighbours.add(new BoardTile(-3, -3, Integer.MAX_VALUE, "R"));
                }
                if (y == 0 && currentColour.equals("B")) {
                    neighbours.add(new BoardTile(-4, -4, Integer.MAX_VALUE, "B"));
                }
                break;
        }

        return neighbours;
    }

    private void addTileIfExists(int x, int y, BoardTile[][] currentBoard, List<BoardTile> neighbours) {
        if (x >= 0 && x < currentBoard.length && y >= 0 && y < currentBoard[x].length) {
            neighbours.add(BoardUtil.getBoardTileFromCoords(x, y, currentBoard));
        }
    }

    private void addRowToNeighbours(int row, BoardTile[][] currentBoard, List<BoardTile> neighbours) {
        for (int i = 0; i < currentBoard.length; i++) {
            neighbours.add(BoardUtil.getBoardTileFromCoords(row, i, currentBoard));
        }
    }

    private void addColumnToNeighbours(int column, BoardTile[][] currentBoard, List<BoardTile> neighbours) {
        for (int i = 0; i < currentBoard[column].length; i++) {
            neighbours.add(BoardUtil.getBoardTileFromCoords(i, column, currentBoard));
        }
    }

    public static String opp(String c){
        if (c.equals("R")) return "B";
        if (c.equals("B")) return "R";
        return "None";
    }

    public static void main(String args[]){
        Agent23 agent = new Agent23();
        agent.run();
    }
}
