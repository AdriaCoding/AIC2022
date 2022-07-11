package Murdak_v5;

import aic2022.user.Direction;
import aic2022.user.Location;
import aic2022.user.TileType;
import aic2022.user.UnitController;

import java.util.LinkedList;
import java.util.Queue;

public class Tools {

    UnitController uc;
    Data data;

    public Tools (UnitController _uc, Data _data) {
        this.uc = _uc;
        this.data = _data;
    }

    // Returns a random integer between 0 and n-1
    int randomInt(int n) {
        return (int)(Math.random()*n);
    }

    //Returns a random direction
    Direction randomDir() {
        int randomNum = randomInt(8);
        return data.dirs[randomNum];
    }

    Direction isAdjacent (Location loc, Location unit){
        if(unit.x-loc.x ==  1 && unit.x-loc.y ==  0) return Direction.EAST;
        if(unit.x-loc.x ==  0 && unit.x-loc.y ==  1) return Direction.SOUTH;
        if(unit.x-loc.x == -1 && unit.x-loc.y ==  0) return Direction.WEST;
        if(unit.x-loc.x ==  0 && unit.x-loc.y == -1) return Direction.NORTH;
        if(unit.x-loc.x ==  1 && unit.x-loc.y ==  1) return Direction.SOUTHEAST;
        if(unit.x-loc.x ==  1 && unit.x-loc.y == -1) return Direction.NORTHEAST;
        if(unit.x-loc.x == -1 && unit.x-loc.y ==  1) return Direction.SOUTHWEST;
        if(unit.x-loc.x == -1 && unit.x-loc.y == -1) return Direction.NORTHWEST;
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

    int _13pow(int i){
        return (int) Math.pow(13,i);
    }

    int encodeLoc(Location loc) {
        int code_y = loc.y%80, code_x = loc.x%80 ;   // ,
        // we also save the quotients of these divisions
        if (uc.readOnSharedArray(code_y + code_x*100) == 0){
            int quotient_y = loc.y/80, quotient_x = loc.x/80;
            int q_Ch = 10080 + code_y/4 + code_x*100;
            int q_Height = code_y%4;
            int current_value = uc.readOnSharedArray(q_Ch);
            if ((current_value/ _13pow(2*q_Height))%169 == 0){
                current_value += quotient_x* _13pow(2*q_Height) + quotient_y* _13pow(2*q_Height+1);
                uc.writeOnSharedArray(q_Ch,current_value);
            }
        }
        return 10000 + code_x * 100 + code_y; // "1xxyy"
    }
    Location decodeLoc(int code){   // format "1xxyy"
        int code_y = code%100, code_x = (code - 10000)/100;
        int q_Ch = 10080 + code_y/4 + code_x*100;
        int q_Height = code_y%4;
        int quotient_x = (uc.readOnSharedArray(q_Ch)/ _13pow( 2*q_Height ))%13;
        int quotient_y = (uc.readOnSharedArray(q_Ch)/ _13pow(2*q_Height+1))%13;
        return new Location(quotient_x*80 + code_x, quotient_y*80 + code_y);
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

    int tileType_code (TileType tileType){
        if (tileType == TileType.PLAINS) return 1;
        if (tileType == TileType.FOREST) return 2;
        if (tileType == TileType.MOUNTAIN) return 3;
        if (tileType == TileType.SHRINE) return 4;
        if (tileType == TileType.WATER) return 5;
        if (tileType == TileType.DUNGEON) return 6;
        if (tileType == TileType.DUNGEON_ENTRANCE) return 7;
        else return 0;
    }

    TileType getTileType (int full_code){
        if (full_code % 10 == 1) return TileType.PLAINS;
        if (full_code % 10 == 2) return TileType.FOREST;
        if (full_code % 10 == 3) return TileType.MOUNTAIN;
        if (full_code % 10 == 4) return TileType.SHRINE;
        if (full_code % 10 == 5) return TileType.WATER;
        if (full_code % 10 == 6) return TileType.DUNGEON;
        if (full_code % 10 == 7) return TileType.DUNGEON_ENTRANCE;
        if (full_code % 10 == 8) return TileType.DUNGEON_ENTRANCE; //8 means dungeonEXIT
        else return TileType.DUNGEON_ENTRANCE; // dunno
    }

    int dirCode(Direction d){
        for(int i = 0; i < 8; ++i){
            if (d.isEqual(data.dirs[i]) )return i;
        }
        return 0;
    }


    //--------------------BFS FUNCTIONS--------------//

    boolean Visited (Location loc){
        return uc.readOnSharedArray(encodeLoc(loc)) / 10 != 0;
    }

    Direction[] dirsBFS =
            {Direction.NORTH,     Direction.WEST,      Direction.SOUTH,     Direction.EAST,
             Direction.NORTHWEST, Direction.SOUTHWEST, Direction.SOUTHEAST, Direction.NORTHEAST};

    int dirCodeBFS (Direction d){
        //TODO arreglar los ==
        if (d == Direction.NORTH)       return 0;
        if (d == Direction.WEST)        return 1;       //   4 0 7
        if (d == Direction.SOUTH)       return 2;       //   1 8 3
        if (d == Direction.EAST)        return 3;       //   5 2 6
        if (d == Direction.NORTHWEST)   return 4;
        if (d == Direction.SOUTHWEST)   return 5;
        if (d == Direction.SOUTHEAST)   return 6;
        if (d == Direction.NORTHEAST)   return 7;
        if (d == Direction.ZERO)        return 8;

        uc.println("something went wrong in dirCode");

        return 0;
    }

    int angloid (Direction d1, Direction d2){       /*assuming d1 is NORTH, then:
                                                          1 0 1
                                                          2   2
                                                          3 4 3         */
        if (d1.isEqual(d2) )                                               return 0;
        if (d1.rotateRight().isEqual(d2) || d1.rotateLeft().isEqual(d2) )  return 1;
        if (d1.rotateRight().rotateRight().isEqual(d2) )                   return 2;
        if (d1.rotateLeft().rotateLeft().isEqual(d2) )                     return 2;
        if (d1.opposite().rotateRight().isEqual(d2) )                      return 3;
        if (d1.opposite().rotateLeft().isEqual(d2) )                       return 3;
        if (d1.opposite().isEqual(d2) )                                    return 4;

        uc.println("something went wrong in angloid");

        return 0;

    }

    boolean isValid(Location loc){
        if(uc.isOutOfMap(loc)) return false;
        int info = uc.readOnSharedArray(encodeLoc(loc));
        if(info == 0) return false;
        if(getTileType(info) == TileType.MOUNTAIN) return false;
        if(getTileType(info) == TileType.WATER) return false;
        if(getTileType(info) == TileType.DUNGEON) return false;
        return info / 10 == 0;
    }

    void BFS(Location start) {
        Queue<Location> q = new LinkedList<>();

        // Mark the starting cell as visited
        // and push it into the queue
        uc.writeOnSharedArray(encodeLoc(start), uc.readOnSharedArray(encodeLoc(start)) + 9*10 );
        q.add(start);
        while (!q.isEmpty() & uc.getEnergyLeft() > 100) {
            Location loc = q.peek();
            q.remove();

            // Go to the adjacent cells
            for(int i = 1; i < 10; i++) {
                Location adjLoc = loc.add(data.dirs[i-1]);
                if (isValid(adjLoc)) {
                    q.add(adjLoc);
                    uc.println("Adding to queue (" + adjLoc.x+ " " + adjLoc.y + ") \n");
                    uc.writeOnSharedArray(encodeLoc(adjLoc),
                            uc.readOnSharedArray(encodeLoc(adjLoc)) + i*10 );
            }
            }
        }

        //TODO empty the queue
    }

}
