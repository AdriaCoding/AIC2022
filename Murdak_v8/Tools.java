package Murdak_v8;

import aic2022.user.Direction;
import aic2022.user.Location;
import aic2022.user.TileType;
import aic2022.user.UnitController;

import java.util.LinkedList;
import java.util.Queue;

public class Tools {

    UnitController uc;
    Data data;

    public Tools(UnitController _uc, Data _data) {
        this.uc = _uc;
        this.data = _data;
    }

    // Returns a random integer between 0 and n-1
    int randomInt(int n) {
        return (int) (Math.random() * n);
    }

    //Returns a random direction
    Direction randomDir() {
        int randomNum = randomInt(8);
        return data.dirs[randomNum];
    }

    Direction isAdjacent(Location loc, Location unit) {
        if (unit.x - loc.x == 1 && unit.x - loc.y == 0) return Direction.EAST;
        if (unit.x - loc.x == 0 && unit.x - loc.y == 1) return Direction.SOUTH;
        if (unit.x - loc.x == -1 && unit.x - loc.y == 0) return Direction.WEST;
        if (unit.x - loc.x == 0 && unit.x - loc.y == -1) return Direction.NORTH;
        if (unit.x - loc.x == 1 && unit.x - loc.y == 1) return Direction.SOUTHEAST;
        if (unit.x - loc.x == 1 && unit.x - loc.y == -1) return Direction.NORTHEAST;
        if (unit.x - loc.x == -1 && unit.x - loc.y == 1) return Direction.SOUTHWEST;
        if (unit.x - loc.x == -1 && unit.x - loc.y == -1) return Direction.NORTHWEST;
        else return Direction.ZERO;
    }
    ///--------------------------MAP INFO FUNCTIONS-----------------------------///
    /*
    Map is at most 80*80, so we divide each coordinate by 80, take the remainders and join
    them to get a unique code for each location, "1xxyy", where xx and yy are 79 at most.
    The code is the channel where we will store all info about each location we explore.
    To be able to get back our location from the code, we need to save the quotients of the prior
    divisions. As coordinates will be 1000 at most, 1000/80 = 12.5, so quotients will be between
    0 and 12.
    Since there will be 20 empty spaces between channel 1xx79 and channel 1x(x+1)00, we will store
    there the x and y quotients of locations from 1xx00 to 1xx80, thus we have to store 8 numbers
    for each channel.
    We can codify qx0, qy0, qx1, qy1, qx2, qy2, qx3, and qy3 as one integer using base 13.
    We save N = qx1 + qy1*13 + qx2*13^2 + ... + qx4*13^6 + qy4*13^7. (remember q < 13).
    To later get the height H = 3 quotients, qx3 and qy3, we just have to take
    (N/13^(2H))%13 and (N/12^(2H+1))%13
    In conclusion, we represent the map between Channels 10000 and 17999. C:
     */

    int _13pow(int i) {
        return (int) Math.pow(13, i);
    }

    int _10pow(int i) {
        return (int) Math.pow(10, i);
    }
    int encodeLoc(Location loc) {
        if (loc == null) return 0;
        int code_y = loc.y % 80, code_x = loc.x % 80;   // ,
        // we also save the quotients of these divisions
        if (uc.readOnSharedArray(code_y + code_x * 100) == 0) {
            int quotient_y = loc.y / 80, quotient_x = loc.x / 80;
            int q_Ch = 10080 + code_y / 4 + code_x * 100;
            int q_Height = code_y % 4;
            int current_value = uc.readOnSharedArray(q_Ch);
            if ((current_value / _13pow(2 * q_Height)) % 169 == 0) {
                current_value += quotient_x * _13pow(2 * q_Height) + quotient_y * _13pow(2 * q_Height + 1);
                uc.writeOnSharedArray(q_Ch, current_value);
            }
        }
        return 10000 + code_x * 100 + code_y; // "1xxyy"
    }

    Location decodeLoc(int code) {   // format "1xxyy"
        int code_y = code % 100, code_x = (code - 10000) / 100;
        int q_Ch = 10080 + code_y / 4 + code_x * 100;
        int q_Height = code_y % 4;
        int quotient_x = (uc.readOnSharedArray(q_Ch) / _13pow(2 * q_Height)) % 13;
        int quotient_y = (uc.readOnSharedArray(q_Ch) / _13pow(2 * q_Height + 1)) % 13;
        return new Location(quotient_x * 80 + code_x, quotient_y * 80 + code_y);
    }

    /*
    int encodeLoc(Location loc) {
        int x = loc.x; int y = loc.y;
        return x*10000 + y+1;
    }

    //Decrypt a location from an integer
    Location decodeLoc(int n) {
        return new Location(n/10000, n%10000-1);
    }
    */

    //-------TileType to int functions----------//

    int tileType_code(TileType tileType) {
        if (tileType.equals(TileType.PLAINS)) return 1;
        if (tileType.equals(TileType.FOREST)) return 2;
        if (tileType.equals(TileType.MOUNTAIN)) return 3;
        if (tileType.equals(TileType.SHRINE)) return 4;
        if (tileType.equals(TileType.WATER)) return 5;
        if (tileType.equals(TileType.DUNGEON)) return 6;
        if (tileType.equals(TileType.DUNGEON_ENTRANCE)) return 7;
        else return 0;
    }

    TileType getTileType(int full_code) {
        if (full_code % 10 == 1) return TileType.PLAINS;
        if (full_code % 10 == 2) return TileType.FOREST;
        if (full_code % 10 == 3) return TileType.MOUNTAIN;
        if (full_code % 10 == 4) return TileType.SHRINE;
        if (full_code % 10 == 5) return TileType.WATER;
        if (full_code % 10 == 6) return TileType.DUNGEON;
        if (full_code % 10 == 7) return TileType.DUNGEON_ENTRANCE;
        if (full_code % 10 == 8) return TileType.DUNGEON_ENTRANCE; //8 means dungeonEXIT
        else return null;
    }

    int dirCode(Direction d) {
        for (int i = 0; i < 8; ++i) {
            if (d.isEqual(data.dirs[i])) return i;
        }
        return 0;
    }


    //--------------------BFS FUNCTIONS--------------//

    boolean Visited(Location loc) {
        return uc.readOnSharedArray(encodeLoc(loc)) / 10 != 0;
    }

    Direction[] dirsBFS =
            {Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST,
                    Direction.NORTHWEST, Direction.SOUTHWEST, Direction.SOUTHEAST, Direction.NORTHEAST,
                    Direction.ZERO};

    int dirCodeBFS(Direction d) {
        if (d.isEqual(Direction.NORTH)) return 0;
        if (d.isEqual(Direction.WEST)) return 1;       //   4 0 7
        if (d.isEqual(Direction.SOUTH)) return 2;       //   1 8 3
        if (d.isEqual(Direction.EAST)) return 3;       //   5 2 6
        if (d.isEqual(Direction.NORTHWEST)) return 4;
        if (d.isEqual(Direction.SOUTHWEST)) return 5;
        if (d.isEqual(Direction.SOUTHEAST)) return 6;
        if (d.isEqual(Direction.NORTHEAST)) return 7;
        if (d.isEqual(Direction.ZERO)) return 8;

        uc.println("something went wrong in dirCode");

        return 0;
    }

    int angloid(Direction d1, Direction d2) {       /*assuming d1 is NORTH, then:
                                                          1 0 1
                                                          2   2
                                                          3 4 3         */
        if (d1.isEqual(d2)) return 0;
        if (d1.rotateRight().isEqual(d2) || d1.rotateLeft().isEqual(d2)) return 1;
        if (d1.rotateRight().rotateRight().isEqual(d2)) return 2;
        if (d1.rotateLeft().rotateLeft().isEqual(d2)) return 2;
        if (d1.opposite().rotateRight().isEqual(d2)) return 3;
        if (d1.opposite().rotateLeft().isEqual(d2)) return 3;
        if (d1.opposite().isEqual(d2)) return 4;

        uc.println("something went wrong in angloid");

        return 0;

    }

    boolean isValid(int code, int height, boolean dungeon) {
        //uc.println("H " + code + " " + uc.readOnSharedArray(code));
        int info = uc.readOnSharedArray(code);
        if (!dungeon) if(info == 0) return false;
        //uc.println("O " + info);
        TileType t = getTileType(info);
        if (t == null) return false;
        //uc.println("L " + t);

        if (t.equals(TileType.MOUNTAIN) | t.equals(TileType.WATER) | t.equals(TileType.DUNGEON)) {
            return false;
        }
        //uc.println("A");
        if(dungeon) return (uc.readOnSharedArray(code + 10000)/_10pow(height))%10 == 0;
        else return (info/_10pow(height))%10 == 0;
    }
    int[] adjacentCodes(int locCode) {  //1xxyy
        int[] adj = new int[8];
        int code_y = locCode % 100, code_x = (locCode - 10000) / 100;

        //els indexs estan permutats pq equivalgui a Direction.opposite()
        //      4 0 7          6 2 5
        //      1   3   -->    3   1
        //      5 2 6          7 0 4

        adj[2] = 10000 + code_x * 100 + (code_y+1)%80;               //NORTH
        adj[3] = 10000 + ((code_x-1)%80) * 100 + code_y;             //WEST
        adj[0] = 10000 + code_x * 100 + (code_y-1)%80;               //SOUTH
        adj[1] = 10000 + ((code_x+1)%80) * 100 + code_y;             //EAST
        adj[6] = 10000 + ((code_x-1)%80) * 100 + (code_y+1)%80;      //NORTHWEST
        adj[7] = 10000 + ((code_x-1)%80) * 100 + (code_y-1)%80;      //SOUTHWEST
        adj[4] = 10000 + ((code_x+1)%80) * 100 + (code_y-1)%80;      //SOUTHEAST
        adj[5] = 10000 + ((code_x+1)%80) * 100 + (code_y+1)%80;      //NORTHEAST

        return adj;
    }

    void baseBFS(int target, int BFSmemoryCh) {
        Queue<Integer> q = new LinkedList<>();
        int height = 1; if(decodeLoc(target) != data.allyBase) height = 5;

        if (uc.readOnSharedArray(target)/_10pow(height) == 0) {    //special treatment to target
            uc.writeOnSharedArray(target, uc.readOnSharedArray(target) + 9 * _10pow(height));
            q.add(target);
            if (height == 1) uc.println(" ** allybaseBFS target is at " + target + " **");
            else uc.println(" ** enemybaseBFS target is at " + target + " **");
        }
        int code = uc.readOnSharedArray(BFSmemoryCh);
        int j = BFSmemoryCh;
        while (code != 0){
            q.add(code);
            uc.writeOnSharedArray(j, 0);
            j += 1;
            code = uc.readOnSharedArray(j);
        }
       /* int energyThreshold = (q.size()+1)*150;
        boolean algohahecho = false;
        if(q.size() > 0){
            uc.println("Queue size is "+q.size() +", we get threshold of " + energyThreshold + ". Round = " + uc.getRound());
            algohahecho = true;
        }*/
        int workdone = 0;
        while (!q.isEmpty() && (uc.getEnergyLeft() > ((q.size()+7)*65)+1400)) {
            //int energy = uc.getEnergyLeft();
            int locCode = q.poll();
            int dist = Math.min(999, (uc.readOnSharedArray(locCode)/_10pow(height+1))%100 + 1);
            // Go to the adjacent cells
            int[] adjCode = adjacentCodes(locCode);
            for(int i = 1; i < 9; i++) {
                if (isValid(adjCode[i-1], height, false)) {
                    q.add(adjCode[i-1]);
                    uc.writeOnSharedArray(adjCode[i-1],
                            uc.readOnSharedArray(adjCode[i-1]) + (10*dist + i)*_10pow(height) );
                    //uc.println("        Adding direction " + dirsBFS[i-1] + " to location with code " + adjCode[i-1] + " at distance = "+ (uc.readOnSharedArray(adjCode[i-1])/_10pow(height+1))%100 + ", relative location " + ( target-adjCode[i-1]) +" when round = " + uc.getRound() + ". Energy left = " + uc.getEnergyLeft());

                }
            }
            ++workdone;
            //int remainder = energy - uc.getEnergyLeft();
            //uc.println("After consuming " + remainder + " energy we have "+ q.size()+ " locations queued and " + uc.getEnergyLeft() + " energy left.");
        }
        //if (q.size() > 0) uc.println("BaseBFS remaining queue size = " + q.size() + ".  Energy left = " + uc.getEnergyLeft() + ". Work done = " + workdone);
        while(!q.isEmpty()){
            if(uc.getEnergyLeft() < 45) uc.println("INSUFFICIENT BFS ENERGY THRESHOLD. Queue size = " + q.size() + ", round = " + uc.getRound());
            uc.writeOnSharedArray(BFSmemoryCh, q.poll());
            BFSmemoryCh += 1;
        }
        //if(algohahecho) uc.println("Remaining Energy is " + uc.getEnergyLeft());
    }
    void dungeonBFS(int target, int di) { // "di" stands for dungeon index
        Queue<Integer> q = new LinkedList<>();

        if ((uc.readOnSharedArray(target+10000)/_10pow(di))%10 == 0) {    //special treatment to target
            uc.writeOnSharedArray(target + 10000, uc.readOnSharedArray(target) + 9 * _10pow(di));
            q.add(target);
            uc.println(" ** " + di + "-DUNGEONBFS target is at " + target + " **");
        }
        int code = uc.readOnSharedArray(data.dungeonBFSCh[di]);
        int j = data.dungeonBFSCh[di];
        while (code != 0){
            q.add(code);
            uc.writeOnSharedArray(j, 0);
            j += 1;
            code = uc.readOnSharedArray(j);
        }
        int energyThreshold = (q.size()+1)*150;
        boolean algohahecho = false;
        if(q.size() > 0){
            uc.println("Queue size is "+q.size() +", we get threshold of " + energyThreshold + ". Round = " + uc.getRound());
            algohahecho = true;
        }
        int workdone = 0;
        while (!q.isEmpty() && (uc.getEnergyLeft() > ((q.size()+7)*50)+1000)) {
            int energy = uc.getEnergyLeft();
            int locCode = q.poll();
            // Go to the adjacent cells
            int[] adjCode = adjacentCodes(locCode);
            for(int i = 1; i < 9; i++) {
                if (isValid(adjCode[i-1], di, true)) {
                    q.add(adjCode[i - 1]);
                    uc.writeOnSharedArray(adjCode[i - 1] + 10000,
                            uc.readOnSharedArray(adjCode[i - 1] + 10000) + i * _10pow(di));
                    //uc.println("        Adding direction " + dirsBFS[i - 1] + " to location with code " + adjCode[i - 1] + ", relative location " + (target - adjCode[i - 1]) + " when round = " + uc.getRound() + ". Energy left = " + uc.getEnergyLeft());
                    //uc.println("        So at " + (adjCode[i - 1] + 10000) + " we write: " + uc.readOnSharedArray(adjCode[i - 1] + 10000));
                }
            }
            int remainder = energy - uc.getEnergyLeft();
            //uc.println("After consuming " + remainder + " energy we have "+ q.size()+ " locations queued and " + uc.getEnergyLeft() + " energy left.");
            ++workdone;
        }
        //if (q.size() > 0) uc.println("Queue size = " + q.size() + ".  Energy left = " + uc.getEnergyLeft());
        while(!q.isEmpty()){
            if(uc.getEnergyLeft() < 45) uc.println("INSUFFICIENT BFS ENERGY THRESHOLD. Queue size = " + q.size() + ", round = " + uc.getRound());
            uc.writeOnSharedArray(data.dungeonBFSCh[di], q.poll());
            data.dungeonBFSCh[di] += 1;
        }
        if(algohahecho) uc.println("Work done is " + workdone +" Remaining Energy after DUNGEONBFS is " + uc.getEnergyLeft());
    }
}