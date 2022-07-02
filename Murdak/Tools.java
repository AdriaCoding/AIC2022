package Murdak;

import aic2022.user.*;

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

}
