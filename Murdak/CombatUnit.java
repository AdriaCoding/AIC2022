package Murdak;

import aic2022.user.*;

public class CombatUnit {

    UnitController uc;
    Data data;
    Tools tools;

    void report() {
        reportMyself();
        //reportEnemies();
        //reportEnvironment();
    }

    // placeholder, this is overridden for each different unit
    void reportMyself() {
        // Report to the Comm Channel
        uc.writeOnSharedArray(data.unitReportCh, uc.readOnSharedArray(data.unitReportCh)+1);
        // Reset Next Slot
        uc.writeOnSharedArray(data.unitResetCh, 0);
    }

    public int targetPriority(UnitInfo unit) {
        if(unit.getType() == UnitType.ASSASSIN)     return 7;
        if(unit.getType() == UnitType.MAGE)         return 6;
        if(unit.getType() == UnitType.EXPLORER)     return 5;
        if(unit.getType() == UnitType.RANGER)       return 4;
        if(unit.getType() == UnitType.BARBARIAN)    return 3;
        if(unit.getType() == UnitType.KNIGHT)       return 2;
        if(unit.getType() == UnitType.BASE)         return 1;

        return 0;
    }

}
