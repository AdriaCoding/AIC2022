package Murdak_v3;

import aic2022.user.Direction;
import aic2022.user.Location;
import aic2022.user.TileType;
import aic2022.user.UnitController;

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

    //-------TileType to int functions----------//

    int tileType_code (TileType tileType){
        if (tileType == TileType.PLAINS) return 1;
        if (tileType == TileType.FOREST) return 2;
        if (tileType == TileType.MOUNTAIN) return 3;
        if (tileType == TileType.SHRINE) return 4;
        if (tileType == TileType.WATER) return 5;
        if (tileType == TileType.DUNGEON) return 6;
        if (tileType == TileType.DUNGEON_ENTRANCE) return 7;
        else return 8;
    }

    TileType getTileType (int full_code){
        if (full_code % 10 == 1) return TileType.PLAINS;
        if (full_code % 10 == 2) return TileType.FOREST;
        if (full_code % 10 == 3) return TileType.MOUNTAIN;
        if (full_code % 10 == 4) return TileType.SHRINE;
        if (full_code % 10 == 5) return TileType.WATER;
        if (full_code % 10 == 6) return TileType.DUNGEON;
        if (full_code % 10 == 7) return TileType.DUNGEON_ENTRANCE;
        else return TileType.PLAINS;
    }

}
