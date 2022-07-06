package Murdak_v3;

import aic2022.user.Direction;
import aic2022.user.Location;
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

    int encodeLocation(int x, int y) {
        return x*10000 + y+1;
    }

    //Decrypt a location from an integer
    Location decodeLocation(int n) {
        return new Location(n/10000, n%10000-1);
    }


}
